package com.fenglin.springboottest.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fenglin.springboottest.common.Coin;
import com.fenglin.springboottest.common.DenRule;
import com.fenglin.springboottest.common.Realm;
import com.fenglin.springboottest.entity.Den;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.entity.Wallet;
import com.fenglin.springboottest.mapper.DenMapper;
import com.fenglin.springboottest.mapper.DemonUserMapper;
import com.fenglin.springboottest.mapper.WalletMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 魔族修行 · 魔窟（洞府挂机产出）业务
 *
 * 要点：
 *   1) 累积以 last_collect 为起点按秒折算，超过 CAP_HOURS（单次存量）封顶；
 *   2) 另按 collect_date / collected_sec 记当日已结算秒数，一天最多结算
 *      DAILY_CAP_HOURS 小时，跨日自动清零——收两次也拿不到双倍；
 *   3) 收取用「乐观锁」防止连点双收：UPDATE ... WHERE last_collect = 旧值；
 *   4) 血玉走千分位（jade_carry）累积，避免低级时 floor 归零；
 *   5) 煞气直接进累计修为（demon_user.exp，原子自增），心魔高时聚煞阵降效。
 */
@Service
public class DenService {

    @Autowired
    private DenMapper denMapper;

    @Autowired
    private DemonUserMapper userMapper;

    @Autowired
    private CoinService coinService;

    /** 后台总览只读钱囊，不触发自动开户 */
    @Autowired
    private WalletMapper walletMapper;

    /** 取魔窟；不存在则开辟（并发下靠唯一键兜底） */
    public Den denOf(Long userId) {
        if (userId == null) {
            return null;
        }
        Den d = denMapper.selectOne(new LambdaQueryWrapper<Den>().eq(Den::getUserId, userId));
        if (d != null) {
            return d;
        }
        Den fresh = new Den();
        fresh.setUserId(userId);
        fresh.setShaLevel(0);
        fresh.setCoinLevel(0);
        fresh.setJadeLevel(0);
        fresh.setJadeCarry(0);
        fresh.setLastCollect(LocalDateTime.now());
        fresh.setCollectDate(LocalDate.now());
        fresh.setCollectedSec(0);
        try {
            denMapper.insert(fresh);
            return fresh;
        } catch (DuplicateKeyException e) {
            return denMapper.selectOne(new LambdaQueryWrapper<Den>().eq(Den::getUserId, userId));
        }
    }

    // ==================== 待收取结算（纯计算，不写库） ====================

    private static int clampInt(long v) {
        return (int) Math.max(0, Math.min(Integer.MAX_VALUE, v));
    }

    private Map<String, Object> computePending(Den d, DemonUser user) {
        int heart = user.getHeartDemon() == null ? 0 : user.getHeartDemon();
        int shaLv = DenRule.level(d.getShaLevel());
        int coinLv = DenRule.level(d.getCoinLevel());
        int jadeLv = DenRule.level(d.getJadeLevel());

        long capSec = DenRule.CAP_HOURS * 3600L;
        long dailyCapSec = DenRule.DAILY_CAP_HOURS * 3600L;

        // 当日已结算秒数：记账日期不是今天则视为 0（跨日自动清零）
        LocalDate today = LocalDate.now();
        int collectedToday = 0;
        if (today.equals(d.getCollectDate()) && d.getCollectedSec() != null) {
            collectedToday = Math.max(0, d.getCollectedSec());
        }
        long dailyLeft = Math.max(0L, dailyCapSec - collectedToday);

        long elapsed = java.time.Duration.between(
                d.getLastCollect() == null ? LocalDateTime.now() : d.getLastCollect(),
                LocalDateTime.now()).getSeconds();
        if (elapsed < 0) {
            elapsed = 0;
        }
        // 三重约束：实际经过时长、单次存量上限、当日剩余额度
        long eff = Math.min(Math.min(elapsed, capSec), dailyLeft);

        // 心魔惩罚只作用于聚煞阵
        int shaPerHour = DenRule.shaPerHour(shaLv, heart);
        int shaPending = clampInt(shaPerHour * eff / 3600L);
        int coinPending = clampInt(DenRule.coinPerHour(coinLv) * eff / 3600L);
        long jadeMilli = (long) DenRule.jadeMilliPerHour(jadeLv) * eff / 3600L
                + (d.getJadeCarry() == null ? 0 : d.getJadeCarry());
        int jadePending = (int) (jadeMilli / 1000);
        int jadeRemain = (int) (jadeMilli % 1000);

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("elapsedSec", elapsed);
        m.put("capSec", capSec);
        m.put("capped", elapsed >= capSec);
        m.put("effSec", eff);
        m.put("dailyCapHours", DenRule.DAILY_CAP_HOURS);
        m.put("dailyCapSec", dailyCapSec);
        m.put("collectedSecToday", collectedToday);
        m.put("dailyLeftSec", dailyLeft);
        m.put("dailyExhausted", dailyLeft <= 0);
        m.put("shaPending", shaPending);
        m.put("coinPending", coinPending);
        m.put("jadePending", jadePending);
        m.put("jadeRemain", jadeRemain);
        m.put("jadeCarry", d.getJadeCarry() == null ? 0 : d.getJadeCarry());
        m.put("shaPerHour", shaPerHour);
        m.put("coinPerHour", DenRule.coinPerHour(coinLv));
        m.put("jadeMilliPerHour", DenRule.jadeMilliPerHour(jadeLv));
        return m;
    }

    // ==================== 状态 ====================

    public Map<String, Object> state(DemonUser user) {
        Den d = denOf(user.getId());
        Map<String, Object> p = computePending(d, user);
        Wallet w = coinService.walletOf(user.getId());
        int realmIndex = Realm.indexOf(user.getCultivation());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("levels", Map.of(
                "sha", DenRule.level(d.getShaLevel()),
                "coin", DenRule.level(d.getCoinLevel()),
                "jade", DenRule.level(d.getJadeLevel())));
        data.put("maxLevel", DenRule.MAX_LEVEL);
        data.put("capHours", DenRule.CAP_HOURS);
        data.put("dailyCapHours", DenRule.DAILY_CAP_HOURS);
        // 每级基准产量下发给前端，避免页面里再抄一份硬编码数值
        data.put("shaPerLevel", DenRule.SHA_PER_HOUR);
        data.put("coinPerLevel", DenRule.COIN_PER_HOUR);
        data.put("jadeMilliPerLevel", DenRule.JADE_MILLI_PER_HOUR);
        data.put("lastCollect", d.getLastCollect());
        data.put("serverTime", LocalDateTime.now());
        data.put("heartDemon", user.getHeartDemon() == null ? 0 : user.getHeartDemon());
        data.put("shaPenalty", DenRule.shaPenalty(user.getHeartDemon() == null ? 0 : user.getHeartDemon()));
        data.put("realmIndex", realmIndex);
        data.put("coin", w == null ? 0 : w.getCoin());
        data.put("jade", w == null ? 0 : w.getJade());
        data.putAll(p);

        data.put("facilities", facilities(d, realmIndex));
        return data;
    }

    /** 三座设施的展示信息：等级 / 产量 / 升级费用 / 门槛 */
    private Map<String, Object> facilities(Den d, int realmIndex) {
        Map<String, Object> fs = new LinkedHashMap<>();
        fs.put("sha", facility("sha", DenRule.level(d.getShaLevel()), null));
        fs.put("coin", facility("coin", DenRule.level(d.getCoinLevel()), null));
        fs.put("jade", facility("jade", DenRule.level(d.getJadeLevel()), realmIndex));
        return fs;
    }

    private Map<String, Object> facility(String type, int lv, Integer realmIndex) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("type", type);
        f.put("level", lv);
        f.put("maxLevel", DenRule.MAX_LEVEL);
        String[] cost = DenRule.upgradeCost(type, lv);
        boolean jade = Coin.JADE.equals(cost[0]);
        f.put("costCurrency", cost[0]);
        f.put("costCurrencyName", jade ? Coin.JADE_NAME : Coin.COIN_NAME);
        f.put("costAmount", Integer.parseInt(cost[1]));
        if (DenRule.TYPE_JADE.equals(type) && realmIndex != null) {
            f.put("realmRequired", DenRule.JADE_REALM_INDEX);
            f.put("realmRequiredName", Realm.name(DenRule.JADE_REALM_INDEX));
            f.put("realmLocked", realmIndex < DenRule.JADE_REALM_INDEX);
        }
        return f;
    }

    // ==================== 收取 ====================

    public Map<String, Object> collect(DemonUser user) {
        Den d = denOf(user.getId());
        LocalDateTime old = d.getLastCollect();
        Map<String, Object> p = computePending(d, user);
        int sha = (int) p.get("shaPending");
        int coin = (int) p.get("coinPending");
        int jade = (int) p.get("jadePending");
        int remain = (int) p.get("jadeRemain");

        if (Boolean.TRUE.equals(p.get("dailyExhausted"))) {
            return Map.of("ok", false, "msg", "今日洞府之利已尽，明日再来。");
        }
        if (sha <= 0 && coin <= 0 && jade <= 0) {
            return Map.of("ok", false, "msg", "魔窟之中尚无可取之物，稍候片刻再来。");
        }

        LocalDateTime now = LocalDateTime.now();
        int collectedToday = ((Number) p.get("collectedSecToday")).intValue();
        long eff = ((Number) p.get("effSec")).longValue();

        // 乐观锁：仅当 last_collect 仍为结算起点时才入账（防连点双收）
        UpdateWrapper<Den> uw = new UpdateWrapper<>();
        uw.eq("user_id", user.getId())
                .eq("last_collect", old)
                .set("last_collect", now)
                .set("jade_carry", remain)
                .set("collect_date", java.sql.Date.valueOf(now.toLocalDate()))
                .set("collected_sec", collectedToday + (int) eff);
        int rows = denMapper.update(null, uw);
        if (rows == 0) {
            return Map.of("ok", false, "msg", "收取太急，魔气尚未凝成，稍候片刻。");
        }

        // 煞气 → 累计修为（原子自增）
        if (sha > 0) {
            userMapper.update(null, new UpdateWrapper<DemonUser>()
                    .eq("id", user.getId())
                    .setSql("exp = exp + " + sha));
        }
        Wallet w = coinService.walletOf(user.getId());
        if (coin > 0) {
            w = coinService.earn(user.getId(), Coin.COIN, coin, "den", "den", null);
        }
        if (jade > 0) {
            w = coinService.earn(user.getId(), Coin.JADE, jade, "den", "den", null);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ok", true);
        data.put("shaGain", sha);
        data.put("coinGain", coin);
        data.put("jadeGain", jade);
        data.put("coin", w == null ? 0 : w.getCoin());
        data.put("jade", w == null ? 0 : w.getJade());
        data.put("msg", String.format("魔窟气机入囊：煞气 +%d、魔晶 +%d、血玉 +%d。", sha, coin, jade));
        data.putAll(state(userMapper.selectById(user.getId())));
        return data;
    }

    // ==================== 升级 ====================

    public Map<String, Object> upgrade(DemonUser user, String type) {
        if (!DenRule.validType(type)) {
            return Map.of("ok", false, "msg", "魔窟之中并无此物。");
        }
        int realmIndex = Realm.indexOf(user.getCultivation());
        if (DenRule.TYPE_JADE.equals(type) && realmIndex < DenRule.JADE_REALM_INDEX) {
            return Map.of("ok", false, "msg", String.format(
                    "血玉髓脉尚封于岩层深处，需至「%s」境方可开辟。", Realm.name(DenRule.JADE_REALM_INDEX)));
        }

        Den d = denOf(user.getId());
        String col = DenRule.TYPE_SHA.equals(type) ? "sha_level"
                : (DenRule.TYPE_COIN.equals(type) ? "coin_level" : "jade_level");
        int cur = DenRule.level(DenRule.TYPE_SHA.equals(type) ? d.getShaLevel()
                : (DenRule.TYPE_COIN.equals(type) ? d.getCoinLevel() : d.getJadeLevel()));
        if (cur >= DenRule.MAX_LEVEL) {
            return Map.of("ok", false, "msg", "此设施已臻圆满，无法再扩建。");
        }

        String[] cost = DenRule.upgradeCost(type, cur);
        String currency = cost[0];
        int amount = Integer.parseInt(cost[1]);

        // 先扣款（余额不足返回 null）
        String currencyName = Coin.JADE.equals(currency) ? Coin.JADE_NAME : Coin.COIN_NAME;
        Wallet before = coinService.walletOf(user.getId());
        Wallet after = coinService.spend(user.getId(), currency, amount, "den_up", "den", null);
        if (after == null) {
            int have = before == null ? 0 : (Coin.JADE.equals(currency) ? before.getJade() : before.getCoin());
            return Map.of("ok", false, "msg", String.format("%s不足：扩建需 %d %s，囊中只有 %d。",
                    currencyName, amount, currencyName, Math.max(0, have)));
        }

        // 条件自增：仅当等级未变时 +1（并发扩建兜底）
        UpdateWrapper<Den> uw = new UpdateWrapper<>();
        uw.eq("user_id", user.getId())
                .eq(col, cur)
            .setSql(col + " = " + col + " + 1");
        int rows = denMapper.update(null, uw);
        if (rows == 0) {
            // 并发下他人已升级：退还费用
            coinService.earn(user.getId(), currency, amount, "den_up_refund", "den", null);
            return Map.of("ok", false, "msg", "手快了些，此设施刚刚扩建过。");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("ok", true);
        data.put("type", type);
        data.put("level", cur + 1);
        data.put("coin", after.getCoin());
        data.put("jade", after.getJade());
        data.put("msg", String.format("魔窟气机涌动，%s已扩建至 %d 级。",
                facilityName(type), cur + 1));
        data.putAll(state(userMapper.selectById(user.getId())));
        return data;
    }

    // ==================== 后台管理：只读总览 / 处置 ====================

    /**
     * 后台总览：全部修者的洞府状态与待收预估。
     * 纯只读——不调用 denOf / walletOf，避免后台"看一眼"就自动开辟洞府或开户。
     */
    public List<Map<String, Object>> adminOverview(String kw) {
        LambdaQueryWrapper<DemonUser> qw = new LambdaQueryWrapper<>();
        if (kw != null && !kw.trim().isEmpty()) {
            qw.like(DemonUser::getName, kw.trim());
        }
        qw.orderByDesc(DemonUser::getId);
        List<DemonUser> users = userMapper.selectList(qw);

        List<Map<String, Object>> out = new ArrayList<>();
        for (DemonUser u : users) {
            Den d = denMapper.selectOne(
                    new LambdaQueryWrapper<Den>().eq(Den::getUserId, u.getId()));
            Den calc = d;
            if (calc == null) {
                // 未开辟者按「三设施 0 级、此刻为起点」计算，待收恒为 0
                calc = new Den();
                calc.setUserId(u.getId());
                calc.setShaLevel(0);
                calc.setCoinLevel(0);
                calc.setJadeLevel(0);
                calc.setJadeCarry(0);
                calc.setLastCollect(LocalDateTime.now());
            }
            Map<String, Object> p = computePending(calc, u);
            Wallet w = walletMapper.selectOne(
                    new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, u.getId()));

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());                 // 供后台通用表格作为修改主键
            m.put("name", u.getName());
            m.put("cultivation", u.getCultivation());
            m.put("exp", u.getExp() == null ? 0 : u.getExp());
            m.put("heartDemon", u.getHeartDemon() == null ? 0 : u.getHeartDemon());
            m.put("coin", w == null || w.getCoin() == null ? 0 : w.getCoin());
            m.put("jade", w == null || w.getJade() == null ? 0 : w.getJade());
            m.put("denId", d == null ? null : d.getId());
            m.put("opened", d != null);
            m.put("shaLevel", DenRule.level(calc.getShaLevel()));
            m.put("coinLevel", DenRule.level(calc.getCoinLevel()));
            m.put("jadeLevel", DenRule.level(calc.getJadeLevel()));
            m.put("jadeCarry", calc.getJadeCarry() == null ? 0 : calc.getJadeCarry());
            m.put("lastCollect", d == null ? null : d.getLastCollect());
            long elapsedSec = ((Number) p.get("elapsedSec")).longValue();
            m.put("elapsedHours", Math.round(elapsedSec / 360.0) / 10.0);
            m.put("capped", p.get("capped"));
            m.put("collectedSecToday", p.get("collectedSecToday"));
            m.put("dailyLeftSec", p.get("dailyLeftSec"));
            m.put("dailyExhausted", p.get("dailyExhausted"));
            m.put("shaPending", p.get("shaPending"));
            m.put("coinPending", p.get("coinPending"));
            m.put("jadePending", p.get("jadePending"));
            m.put("jadeRemain", p.get("jadeRemain"));
            m.put("shaPerHour", p.get("shaPerHour"));
            m.put("coinPerHour", p.get("coinPerHour"));
            m.put("jadeMilliPerHour", p.get("jadeMilliPerHour"));
            out.add(m);
        }
        // 已开辟洞府者优先，其余按修者 id 倒序——后台打开即可见有数据的洞府
        out.sort((a, b) -> {
            int ao = Boolean.TRUE.equals(a.get("opened")) ? 0 : 1;
            int bo = Boolean.TRUE.equals(b.get("opened")) ? 0 : 1;
            if (ao != bo) {
                return ao - bo;
            }
            return Long.compare(((Number) b.get("id")).longValue(),
                    ((Number) a.get("id")).longValue());
        });
        return out;
    }

    /**
     * 后台处置：校订三设施等级 / 血玉千分位 / 回拨累积起点；
     * 或代修者收取（复用玩家端 collect 的乐观锁结算）、拆毁洞府（删行，下次访问自动重建）。
     * 返回 ok / msg，由 Controller 决定 success 或 error。
     */
    public Map<String, Object> adminAdjust(Long userId, Map<String, Object> patch) {
        DemonUser user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            return Map.of("ok", false, "msg", "魔门之中并无此修者。");
        }
        Map<String, Object> p = patch == null ? Map.of() : patch;
        String action = adminStr(p.get("action"));
        if (action.isEmpty()) {
            action = "none";
        }

        if ("reset".equals(action)) {
            denMapper.delete(new LambdaQueryWrapper<Den>().eq(Den::getUserId, userId));
            return Map.of("ok", true, "msg", "洞府已拆毁，三设施归零；修者下次进入时重新开辟。");
        }
        if ("collect".equals(action)) {
            Map<String, Object> r = collect(user);
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("ok", Boolean.TRUE.equals(r.get("ok")));
            res.put("msg", r.get("msg"));
            return res;
        }
        if (!"none".equals(action)) {
            return Map.of("ok", false, "msg", "未知的魔窟处置方式。");
        }

        Integer sha = adminInt(p.get("shaLevel"));
        Integer coinLv = adminInt(p.get("coinLevel"));
        Integer jadeLv = adminInt(p.get("jadeLevel"));
        Integer carry = adminInt(p.get("jadeCarry"));
        Integer back = adminInt(p.get("backHours"));
        if (outOfRange(sha, DenRule.MAX_LEVEL)
                || outOfRange(coinLv, DenRule.MAX_LEVEL)
                || outOfRange(jadeLv, DenRule.MAX_LEVEL)) {
            return Map.of("ok", false, "msg", "设施等级只可为 0～" + DenRule.MAX_LEVEL + "。");
        }
        if (outOfRange(carry, 999)) {
            return Map.of("ok", false, "msg", "血玉千分位只可为 0～999。");
        }
        if (outOfRange(back, 720)) {
            return Map.of("ok", false, "msg", "回拨小时只可为 0～720。");
        }

        Den d = denMapper.selectOne(new LambdaQueryWrapper<Den>().eq(Den::getUserId, userId));
        boolean exists = d != null;
        if (!exists) {
            d = new Den();
            d.setUserId(userId);
            d.setShaLevel(0);
            d.setCoinLevel(0);
            d.setJadeLevel(0);
            d.setJadeCarry(0);
            d.setLastCollect(LocalDateTime.now());
            d.setCollectDate(LocalDate.now());
            d.setCollectedSec(0);
        }
        if (sha != null) {
            d.setShaLevel(sha);
        }
        if (coinLv != null) {
            d.setCoinLevel(coinLv);
        }
        if (jadeLv != null) {
            d.setJadeLevel(jadeLv);
        }
        if (carry != null) {
            d.setJadeCarry(carry);
        }
        if (back != null && back > 0) {
            d.setLastCollect(LocalDateTime.now().minusHours(back));
            // 回拨视为重新开放当日额度，便于演示 / 补发（否则会被每日封顶挡住）
            d.setCollectDate(LocalDate.now());
            d.setCollectedSec(0);
        }
        if (exists) {
            denMapper.updateById(d);
        } else {
            denMapper.insert(d);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("ok", true);
        res.put("msg", String.format("洞府已校订：聚煞阵 %d 级 / 魔晶矿 %d 级 / 血玉髓脉 %d 级。",
                d.getShaLevel(), d.getCoinLevel(), d.getJadeLevel()));
        res.put("shaLevel", d.getShaLevel());
        res.put("coinLevel", d.getCoinLevel());
        res.put("jadeLevel", d.getJadeLevel());
        res.put("jadeCarry", d.getJadeCarry());
        res.put("lastCollect", d.getLastCollect());
        return res;
    }

    private static boolean outOfRange(Integer v, int max) {
        return v != null && (v < 0 || v > max);
    }

    /** 后台 number 输入可能是字符串或 JSON 数字；空串视为「不修改」 */
    private static Integer adminInt(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number n) {
            return n.intValue();
        }
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String adminStr(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    public static String facilityName(String type) {
        if (DenRule.TYPE_SHA.equals(type)) {
            return "聚煞阵";
        }
        if (DenRule.TYPE_COIN.equals(type)) {
            return "魔晶矿";
        }
        return "血玉髓脉";
    }
}
