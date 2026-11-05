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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 魔族修行 · 魔窟（洞府挂机产出）业务
 *
 * 要点：
 *   1) 累积以 last_collect 为起点按秒折算，超过 CAP_HOURS 封顶；
 *   2) 收取用「乐观锁」防止连点双收：UPDATE ... WHERE last_collect = 旧值；
 *   3) 血玉走千分位（jade_carry）累积，避免低级时 floor 归零；
 *   4) 煞气直接进累计修为（demon_user.exp，原子自增），心魔高时聚煞阵降效。
 */
@Service
public class DenService {

    @Autowired
    private DenMapper denMapper;

    @Autowired
    private DemonUserMapper userMapper;

    @Autowired
    private CoinService coinService;

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
        long elapsed = java.time.Duration.between(
                d.getLastCollect() == null ? LocalDateTime.now() : d.getLastCollect(),
                LocalDateTime.now()).getSeconds();
        if (elapsed < 0) {
            elapsed = 0;
        }
        long eff = Math.min(elapsed, capSec);

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

        if (sha <= 0 && coin <= 0 && jade <= 0) {
            return Map.of("ok", false, "msg", "魔窟之中尚无可取之物，稍候片刻再来。");
        }

        // 乐观锁：仅当 last_collect 仍为结算起点时才入账（防连点双收）
        UpdateWrapper<Den> uw = new UpdateWrapper<>();
        uw.eq("user_id", user.getId())
                .eq("last_collect", old)
                .set("last_collect", LocalDateTime.now())
                .set("jade_carry", remain);
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
