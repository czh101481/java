package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fenglin.springboottest.common.Coin;
import com.fenglin.springboottest.entity.BagItem;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.entity.Item;
import com.fenglin.springboottest.entity.Purchase;
import com.fenglin.springboottest.entity.Wallet;
import com.fenglin.springboottest.mapper.BagItemMapper;
import com.fenglin.springboottest.mapper.DemonUserMapper;
import com.fenglin.springboottest.mapper.ItemMapper;
import com.fenglin.springboottest.mapper.PurchaseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 魔族修行 · 藏宝阁与乾坤袋业务
 *
 * 购买流程：校验在售 → 校验限购 → 扣币 → 入袋 → 记购买流水
 * 使用流程：校验持有 → 扣袋 → 结算效果（加煞气 / 加参悟上限 / 开囊得魔晶）
 */
@Service
public class ShopService {

    /** 业务拒绝：由控制器转成 Result.error(400, msg) */
    public static class Deny extends RuntimeException {
        public Deny(String msg) {
            super(msg);
        }
    }

    @Autowired
    private ItemMapper itemMapper;

    @Autowired
    private BagItemMapper bagItemMapper;

    @Autowired
    private PurchaseMapper purchaseMapper;

    @Autowired
    private DemonUserMapper userMapper;

    @Autowired
    private CoinService coinService;

    // ==================== 藏宝阁 ====================

    /** 在售物品一览：含今日已购、剩余可购、背包持有、是否买得起 */
    public List<Map<String, Object>> shop(Long userId) {
        List<Item> items = itemMapper.selectList(new LambdaQueryWrapper<Item>()
                .eq(Item::getStatus, 1)
                .orderByAsc(Item::getSort)
                .orderByAsc(Item::getId));
        Wallet w = userId == null ? null : coinService.walletOf(userId);
        int coin = w == null || w.getCoin() == null ? 0 : w.getCoin();
        int jade = w == null || w.getJade() == null ? 0 : w.getJade();

        Map<Long, Integer> boughtToday = userId == null ? new HashMap<>() : boughtToday(userId);
        Map<Long, Integer> held = userId == null ? new HashMap<>() : heldMap(userId);

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Item it : items) {
            Map<String, Object> m = itemMap(it);
            int bought = boughtToday.getOrDefault(it.getId(), 0);
            int limit = it.getDailyLimit() == null ? 0 : it.getDailyLimit();
            boolean useJade = it.getPriceJade() != null && it.getPriceJade() > 0;
            int basePrice = nz(useJade ? it.getPriceJade() : it.getPriceCoin());
            int disc = activeDiscount(it, now);
            int price = disc > 0 ? Coin.discountPrice(basePrice, disc) : basePrice;
            m.put("boughtToday", bought);
            m.put("dailyLimit", limit);
            m.put("remainToday", limit <= 0 ? -1 : Math.max(0, limit - bought));
            m.put("soldOut", limit > 0 && bought >= limit);
            m.put("held", held.getOrDefault(it.getId(), 0));
            m.put("payCurrency", useJade ? Coin.JADE : Coin.COIN);
            m.put("price", price);
            m.put("originalPrice", basePrice);
            m.put("discountPercent", disc);
            m.put("discounting", disc > 0);
            m.put("discountEnd", it.getDiscountEnd());
            m.put("affordable", price <= (useJade ? jade : coin));
            list.add(m);
        }
        return list;
    }

    // ==================== 限时折扣 ====================

    /** 此刻生效的折扣百分比（1-99）；0 表示未打折 */
    public static int activeDiscount(Item it, LocalDateTime now) {
        if (it == null) {
            return 0;
        }
        Integer p = it.getDiscountPercent();
        if (p == null || p <= 0 || p >= 100) {
            return 0;
        }
        if (it.getDiscountStart() != null && now.isBefore(it.getDiscountStart())) {
            return 0;
        }
        if (it.getDiscountEnd() != null && now.isAfter(it.getDiscountEnd())) {
            return 0;
        }
        return p;
    }

    /** 实付单价（已含折扣）；jade=true 取血玉价 */
    public static int payPrice(Item it, boolean jade, LocalDateTime now) {
        int base = nz(jade ? it.getPriceJade() : it.getPriceCoin());
        int p = activeDiscount(it, now);
        return p > 0 ? Coin.discountPrice(base, p) : base;
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }


    /** 购买：qty 件，扣币入袋 */
    public Map<String, Object> buy(Long userId, Long itemId, int qty) {
        if (itemId == null) {
            throw new Deny("未指明欲购何物。");
        }
        int n = Math.max(1, Math.min(qty, 99));
        Item it = itemMapper.selectById(itemId);
        if (it == null || it.getStatus() == null || it.getStatus() != 1) {
            throw new Deny("藏宝阁中并无此物。");
        }
        int limit = it.getDailyLimit() == null ? 0 : it.getDailyLimit();
        if (limit > 0) {
            int bought = boughtToday(userId).getOrDefault(itemId, 0);
            if (bought + n > limit) {
                throw new Deny(String.format("「%s」每日限购 %d 件，今日已购 %d 件。",
                        it.getName(), limit, bought));
            }
        }

        boolean useJade = it.getPriceJade() != null && it.getPriceJade() > 0;
        int unit = payPrice(it, useJade, LocalDateTime.now());
        int total = unit * n;
        int disc = activeDiscount(it, LocalDateTime.now());
        if (total > 0) {
            Wallet after = coinService.spend(userId,
                    useJade ? Coin.JADE : Coin.COIN, total, "buy", "item", itemId);
            if (after == null) {
                throw new Deny(String.format("%s不足，需 %d，尚缺 %d。",
                        useJade ? Coin.JADE_NAME : Coin.COIN_NAME, total,
                        total - balanceOf(userId, useJade)));
            }
        }

        addToBag(userId, itemId, n);

        Purchase p = new Purchase();
        p.setUserId(userId);
        p.setItemId(itemId);
        p.setQty(n);
        p.setPayCurrency(useJade ? Coin.JADE : Coin.COIN);
        p.setPayAmount(total);
        p.setBuyDate(LocalDate.now());
        purchaseMapper.insert(p);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("item", itemMap(it));
        data.put("qty", n);
        data.put("payCurrency", useJade ? Coin.JADE : Coin.COIN);
        data.put("payAmount", total);
        data.put("discountPercent", disc);
        data.put("wallet", coinService.overview(userId));
        data.put("bagQty", heldOf(userId, itemId));
        data.put("msg", disc > 0
                ? String.format("趁折扣购得「%s」×%d，耗 %d %s，已入乾坤袋。",
                        it.getName(), n, total, useJade ? Coin.JADE_NAME : Coin.COIN_NAME)
                : String.format("购得「%s」×%d，已入乾坤袋。", it.getName(), n));
        return data;
    }

    private int balanceOf(Long userId, boolean jade) {
        Wallet w = coinService.walletOf(userId);
        if (w == null) {
            return 0;
        }
        return jade ? (w.getJade() == null ? 0 : w.getJade()) : (w.getCoin() == null ? 0 : w.getCoin());
    }

    // ==================== 乾坤袋 ====================

    /** 背包列表（只列还有剩余的） */
    public List<Map<String, Object>> bag(Long userId) {
        List<BagItem> rows = bagItemMapper.selectList(new LambdaQueryWrapper<BagItem>()
                .eq(BagItem::getUserId, userId)
                .gt(BagItem::getQty, 0)
                .orderByDesc(BagItem::getUpdatedAt));
        List<Map<String, Object>> list = new ArrayList<>();
        for (BagItem b : rows) {
            Item it = itemMapper.selectById(b.getItemId());
            if (it == null) {
                continue;
            }
            Map<String, Object> m = itemMap(it);
            m.put("qty", b.getQty());
            m.put("effectText", effectText(it, b.getQty()));
            list.add(m);
        }
        return list;
    }

    /** 使用道具：扣袋 + 结算效果 */
    public Map<String, Object> use(Long userId, Long itemId, int qty) {
        if (itemId == null) {
            throw new Deny("未指明所用何物。");
        }
        int n = Math.max(1, Math.min(qty, 99));
        Item it = itemMapper.selectById(itemId);
        if (it == null) {
            throw new Deny("乾坤袋中并无此物。");
        }
        int held = heldOf(userId, itemId);
        if (held < n) {
            throw new Deny(String.format("「%s」仅余 %d 件，不足 %d 件。", it.getName(), held, n));
        }

        // 扣背包：带 qty >= n 条件，防并发超用
        UpdateWrapper<BagItem> uw = new UpdateWrapper<>();
        uw.eq("user_id", userId).eq("item_id", itemId).ge("qty", n)
          .setSql("qty = qty - " + n);
        int rows = bagItemMapper.update(null, uw);
        if (rows == 0) {
            throw new Deny("乾坤袋已空，无法取用。");
        }

        String type = it.getEffectType() == null ? "exp" : it.getEffectType();
        int value = (it.getEffectValue() == null ? 0 : it.getEffectValue()) * n;
        int newExp = -1;
        String effectText;

        if ("explore".equals(type)) {
            applyExploreBonus(userId, value);
            effectText = String.format("今日参悟上限 +%d", value);
        } else if ("coin".equals(type)) {
            coinService.earn(userId, Coin.COIN, value, "item", "item", itemId);
            effectText = String.format("获得 %s +%d", Coin.COIN_NAME, value);
        } else {
            UpdateWrapper<DemonUser> uu = new UpdateWrapper<>();
            uu.eq("id", userId).setSql("exp = exp + " + value);
            userMapper.update(null, uu);
            DemonUser after = userMapper.selectById(userId);
            newExp = after == null || after.getExp() == null ? 0 : after.getExp();
            effectText = String.format("煞气 +%d", value);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("item", itemMap(it));
        data.put("qty", n);
        data.put("effectType", type);
        data.put("effectValue", value);
        data.put("effectText", effectText);
        data.put("exp", newExp);
        data.put("bagQty", heldOf(userId, itemId));
        data.put("wallet", coinService.overview(userId));
        data.put("msg", String.format("服下「%s」×%d，%s。", it.getName(), n, effectText));
        return data;
    }

    /** 今日参悟上限加成（跨日自动清零） */
    private void applyExploreBonus(Long userId, int add) {
        DemonUser u = userMapper.selectById(userId);
        if (u == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        int base = 0;
        if (u.getExploreBonusDate() != null && today.equals(u.getExploreBonusDate())
                && u.getExploreBonusToday() != null) {
            base = u.getExploreBonusToday();
        }
        UpdateWrapper<DemonUser> uw = new UpdateWrapper<>();
        uw.eq("id", userId)
          .set("explore_bonus_today", base + add)
          .set("explore_bonus_date", today);
        userMapper.update(null, uw);
    }

    // ==================== 内部工具 ====================

    public void addToBag(Long userId, Long itemId, int n) {
        UpdateWrapper<BagItem> uw = new UpdateWrapper<>();
        uw.eq("user_id", userId).eq("item_id", itemId).setSql("qty = qty + " + n);
        int rows = bagItemMapper.update(null, uw);
        if (rows > 0) {
            return;
        }
        BagItem b = new BagItem();
        b.setUserId(userId);
        b.setItemId(itemId);
        b.setQty(n);
        try {
            bagItemMapper.insert(b);
        } catch (DuplicateKeyException e) {
            UpdateWrapper<BagItem> retry = new UpdateWrapper<>();
            retry.eq("user_id", userId).eq("item_id", itemId).setSql("qty = qty + " + n);
            bagItemMapper.update(null, retry);
        }
    }

    private int heldOf(Long userId, Long itemId) {
        BagItem b = bagItemMapper.selectOne(new LambdaQueryWrapper<BagItem>()
                .eq(BagItem::getUserId, userId)
                .eq(BagItem::getItemId, itemId));
        return b == null || b.getQty() == null ? 0 : b.getQty();
    }

    /** 用户今日各物品已购数量 */
    private Map<Long, Integer> boughtToday(Long userId) {
        QueryWrapper<Purchase> qw = new QueryWrapper<>();
        qw.select("item_id", "SUM(qty) AS qty")
          .eq("user_id", userId)
          .eq("buy_date", LocalDate.now())
          .groupBy("item_id");
        List<Map<String, Object>> rows = purchaseMapper.selectMaps(qw);
        Map<Long, Integer> m = new HashMap<>();
        for (Map<String, Object> r : rows) {
            Object id = r.get("item_id");
            Object qty = r.get("qty");
            if (id instanceof Number && qty instanceof Number) {
                m.put(((Number) id).longValue(), ((Number) qty).intValue());
            }
        }
        return m;
    }

    /** 用户背包持有数映射 */
    private Map<Long, Integer> heldMap(Long userId) {
        List<BagItem> rows = bagItemMapper.selectList(new LambdaQueryWrapper<BagItem>()
                .eq(BagItem::getUserId, userId));
        Map<Long, Integer> m = new HashMap<>();
        for (BagItem b : rows) {
            m.put(b.getItemId(), b.getQty() == null ? 0 : b.getQty());
        }
        return m;
    }

    private Map<String, Object> itemMap(Item it) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", it.getId());
        m.put("code", it.getCode());
        m.put("name", it.getName());
        m.put("icon", it.getIcon());
        m.put("category", it.getCategory());
        m.put("desc", it.getDescText());
        m.put("priceCoin", it.getPriceCoin());
        m.put("priceJade", it.getPriceJade());
        m.put("dailyLimit", it.getDailyLimit());
        m.put("discountPercent", activeDiscount(it, LocalDateTime.now()));
        m.put("discountEnd", it.getDiscountEnd());
        m.put("effectType", it.getEffectType());
        m.put("effectValue", it.getEffectValue());
        m.put("effectText", effectText(it, 1));
        return m;
    }

    private String effectText(Item it, int qty) {
        String type = it.getEffectType() == null ? "exp" : it.getEffectType();
        int v = (it.getEffectValue() == null ? 0 : it.getEffectValue()) * Math.max(1, qty);
        if ("explore".equals(type)) {
            return "今日参悟上限 +" + v;
        }
        if ("coin".equals(type)) {
            return "获得" + Coin.COIN_NAME + " +" + v;
        }
        return "煞气 +" + v;
    }
}
