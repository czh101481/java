package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fenglin.springboottest.common.Coin;
import com.fenglin.springboottest.entity.BagItem;
import com.fenglin.springboottest.entity.Item;
import com.fenglin.springboottest.entity.Trade;
import com.fenglin.springboottest.entity.Wallet;
import com.fenglin.springboottest.mapper.BagItemMapper;
import com.fenglin.springboottest.mapper.ItemMapper;
import com.fenglin.springboottest.mapper.TradeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 魔族修行 · 鬼市（玩家间交易）
 *
 * 规则：
 *   1) 挂单即从卖家乾坤袋预扣物品，避免同一件物品被重复挂出；
 *   2) 撤单原物退回卖家乾坤袋；
 *   3) 成交时买家付全款，卖家得 95%（5% 为鬼市焚香钱，系统回收）；
 *   4) 抢单用「UPDATE ... WHERE status=0」原子改状态，保证一件只卖一次。
 *
 * 事务：buy 全程 @Transactional，任一步失败（余额不足等）整体回滚，
 *      挂单状态不会被误置为已售。
 */
@Service
public class TradeService {

    /** 业务拒绝：由控制器转成 Result.error(400, msg) */
    public static class Deny extends RuntimeException {
        public Deny(String msg) {
            super(msg);
        }
    }

    @Autowired
    private TradeMapper tradeMapper;

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private BagItemMapper bagItemMapper;

    @Autowired
    private ShopService shopService;

    @Autowired
    private CoinService coinService;

    // ==================== 浏览 ====================

    /** 鬼市在售 */
    public List<Map<String, Object>> onSale() {
        return decorate(tradeMapper.listOnSale(120));
    }

    /** 我的挂单（含已售 / 已撤） */
    public List<Map<String, Object>> mine(Long userId) {
        return decorate(tradeMapper.listBySeller(userId, 60));
    }

    private List<Map<String, Object>> decorate(List<Map<String, Object>> rows) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> m = new LinkedHashMap<>(r);
            int price = num(r.get("price"), 0);
            int qty = num(r.get("qty"), 1);
            int total = price * qty;
            boolean jade = Coin.JADE.equals(String.valueOf(r.get("currency")));
            int status = num(r.get("status"), 0);
            int fee = Coin.tradeFee(total);
            m.put("total", total);
            m.put("currencyName", jade ? Coin.JADE_NAME : Coin.COIN_NAME);
            m.put("fee", fee);
            m.put("sellerNet", total <= 0 ? 0 : Math.max(1, total - fee));
            m.put("statusText", status == 1 ? "已售出" : (status == 2 ? "已撤回" : "在售"));
            m.put("effectText", effectText(str(r.get("effectType")), num(r.get("effectValue"), 0), 1));
            list.add(m);
        }
        return list;
    }

    private String effectText(String type, int value, int qty) {
        int v = value * Math.max(1, qty);
        if ("explore".equals(type)) {
            return "今日参悟上限 +" + v;
        }
        if ("coin".equals(type)) {
            return "获得" + Coin.COIN_NAME + " +" + v;
        }
        return "煞气 +" + v;
    }

    // ==================== 挂单 ====================

    /** 寄售：从乾坤袋预扣 qty 件，按 price 单价挂出 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> sell(Long userId, Long itemId, int qty, int price, String currency) {
        if (itemId == null) {
            throw new Deny("未指明要寄售何物。");
        }
        Item it = itemMapper.selectById(itemId);
        if (it == null) {
            throw new Deny("魔门之中并无此物。");
        }
        int n = Math.max(1, Math.min(qty, 99));
        int p = Math.max(1, Math.min(price, Coin.TRADE_MAX_PRICE));
        String cur = Coin.JADE.equals(currency) ? Coin.JADE : Coin.COIN;

        long open = tradeMapper.selectCount(new LambdaQueryWrapper<Trade>()
                .eq(Trade::getSellerId, userId)
                .eq(Trade::getStatus, 0));
        if (open >= Coin.TRADE_MAX_OPEN) {
            throw new Deny(String.format("鬼市同时挂单不可超过 %d 单，先撤几单再来。", Coin.TRADE_MAX_OPEN));
        }

        // 预扣：带 qty >= n 条件，防并发超卖
        UpdateWrapper<BagItem> uw = new UpdateWrapper<>();
        uw.eq("user_id", userId).eq("item_id", itemId).ge("qty", n).setSql("qty = qty - " + n);
        if (bagItemMapper.update(null, uw) == 0) {
            throw new Deny(String.format("乾坤袋中「%s」不足 %d 件。", it.getName(), n));
        }

        Trade t = new Trade();
        t.setSellerId(userId);
        t.setItemId(itemId);
        t.setQty(n);
        t.setPrice(p);
        t.setCurrency(cur);
        t.setStatus(0);
        t.setCreatedAt(LocalDateTime.now());
        tradeMapper.insert(t);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tradeId", t.getId());
        data.put("itemName", it.getName());
        data.put("qty", n);
        data.put("price", p);
        data.put("total", p * n);
        data.put("currencyName", Coin.JADE.equals(cur) ? Coin.JADE_NAME : Coin.COIN_NAME);
        data.put("bagQty", heldOf(userId, itemId));
        data.put("msg", String.format("「%s」×%d 已挂入鬼市，单价 %d %s。",
                it.getName(), n, p, Coin.JADE.equals(cur) ? Coin.JADE_NAME : Coin.COIN_NAME));
        return data;
    }

    /** 撤单：状态 0 → 2，原物退回乾坤袋 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancel(Long userId, Long tradeId) {
        Trade t = tradeId == null ? null : tradeMapper.selectById(tradeId);
        if (t == null) {
            throw new Deny("鬼市之中并无此单。");
        }
        if (!userId.equals(t.getSellerId())) {
            throw new Deny("此单非你之物，不可撤回。");
        }
        if (t.getStatus() == null || t.getStatus() != 0) {
            throw new Deny("此单已不可撤回。");
        }
        UpdateWrapper<Trade> uw = new UpdateWrapper<>();
        uw.eq("id", tradeId).eq("status", 0)
          .set("status", 2).set("sold_at", LocalDateTime.now());
        if (tradeMapper.update(null, uw) == 0) {
            throw new Deny("此单刚被他人买走，已无法撤回。");
        }
        shopService.addToBag(userId, t.getItemId(), t.getQty() == null ? 1 : t.getQty());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tradeId", tradeId);
        data.put("bagQty", heldOf(userId, t.getItemId()));
        data.put("msg", "挂单已撤回，原物退回乾坤袋。");
        return data;
    }

    // ==================== 成交 ====================

    /** 买入：原子抢单 → 扣买家 → 结算给卖家（扣 5% 抽成）→ 物品入买家袋 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> buy(Long userId, Long tradeId) {
        Trade t = tradeId == null ? null : tradeMapper.selectById(tradeId);
        if (t == null) {
            throw new Deny("鬼市之中并无此单。");
        }
        if (userId.equals(t.getSellerId())) {
            throw new Deny("自家挂的单，何必买？撤单即可取回。");
        }
        if (t.getStatus() == null || t.getStatus() != 0) {
            throw new Deny("此单已被买走或已撤回。");
        }
        Item it = itemMapper.selectById(t.getItemId());
        if (it == null) {
            throw new Deny("此物已不存于魔门。");
        }

        int qty = t.getQty() == null ? 1 : t.getQty();
        int total = (t.getPrice() == null ? 0 : t.getPrice()) * qty;
        boolean jade = Coin.JADE.equals(t.getCurrency());

        // 抢单：谁先把 status 由 0 改成 1，谁得此物
        UpdateWrapper<Trade> claim = new UpdateWrapper<>();
        claim.eq("id", t.getId()).eq("status", 0)
             .set("status", 1).set("buyer_id", userId).set("sold_at", LocalDateTime.now());
        if (tradeMapper.update(null, claim) == 0) {
            throw new Deny("慢了一步，此单已被他人买走。");
        }

        if (total > 0) {
            Wallet after = coinService.spend(userId, t.getCurrency(), total, "trade", "trade", t.getId());
            if (after == null) {
                int have = balanceOf(userId, jade);
                throw new Deny(String.format("%s不足，需 %d，尚缺 %d。",
                        jade ? Coin.JADE_NAME : Coin.COIN_NAME, total, total - have));
            }
        }

        int fee = Coin.tradeFee(total);
        int net = total <= 0 ? 0 : Math.max(1, total - fee);
        if (net > 0) {
            coinService.earn(t.getSellerId(), t.getCurrency(), net, "trade", "trade", t.getId());
        }
        shopService.addToBag(userId, t.getItemId(), qty);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tradeId", t.getId());
        data.put("itemName", it.getName());
        data.put("qty", qty);
        data.put("total", total);
        data.put("fee", fee);
        data.put("sellerNet", net);
        data.put("currencyName", jade ? Coin.JADE_NAME : Coin.COIN_NAME);
        data.put("wallet", coinService.overview(userId));
        data.put("bagQty", heldOf(userId, t.getItemId()));
        data.put("msg", String.format("已从鬼市购得「%s」×%d，耗 %d %s。",
                it.getName(), qty, total, jade ? Coin.JADE_NAME : Coin.COIN_NAME));
        return data;
    }

    // ==================== 工具 ====================

    private int heldOf(Long userId, Long itemId) {
        BagItem b = bagItemMapper.selectOne(new LambdaQueryWrapper<BagItem>()
                .eq(BagItem::getUserId, userId)
                .eq(BagItem::getItemId, itemId));
        return b == null || b.getQty() == null ? 0 : b.getQty();
    }

    private int balanceOf(Long userId, boolean jade) {
        Wallet w = coinService.walletOf(userId);
        if (w == null) {
            return 0;
        }
        return jade ? (w.getJade() == null ? 0 : w.getJade()) : (w.getCoin() == null ? 0 : w.getCoin());
    }

    private static int num(Object v, int def) {
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    private static String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
