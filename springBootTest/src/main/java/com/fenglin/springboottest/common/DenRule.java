package com.fenglin.springboottest.common;

/**
 * 魔族修行 · 魔窟（洞府挂机产出）数值规则
 *
 * 三座设施，各自 0~MAX_LEVEL 级（0 = 未开辟）：
 *   聚煞阵   → 煞气（直接计入累计修为 demon_user.exp）
 *   魔晶矿   → 魔晶（入钱囊）
 *   血玉髓脉 → 血玉（入钱囊，按千分位累积进位，避免低级取整归零）
 *
 * 定位（用户选定「挂机为主」强度）：
 *   满级 12 小时约得 煞气 360 / 魔晶 360 / 血玉 4~5，
 *   超过签到+参悟+论道+切磋的日活总收益（约 100~200 煞气）。
 *
 * 调整产出只需改本类常量（无需动业务代码）。
 */
public final class DenRule {

    private DenRule() {
    }

    /** 设施类型代码（与 demon_den 列、前端 type 参数一一对应） */
    public static final String TYPE_SHA = "sha";
    public static final String TYPE_COIN = "coin";
    public static final String TYPE_JADE = "jade";

    /** 设施等级上限 */
    public static final int MAX_LEVEL = 10;

    /** 离线累积存量上限（小时）：超过部分不再累积，鼓励收取 */
    public static final int CAP_HOURS = 12;

    // ---------------- 每小时产量 ----------------

    /** 聚煞阵：煞气/小时 = SHA_PER_HOUR × 等级（满级 30/h，12h 满 360） */
    public static final int SHA_PER_HOUR = 3;

    /** 魔晶矿：魔晶/小时 = COIN_PER_HOUR × 等级（满级 30/h，12h 满 360） */
    public static final int COIN_PER_HOUR = 3;

    /** 血玉髓脉：血玉千分位/小时 = JADE_MILLI_PER_HOUR × 等级
     *  （满级 400‰/h = 0.4 血玉/h，12h 约 4.8 血玉） */
    public static final int JADE_MILLI_PER_HOUR = 40;

    // ---------------- 心魔影响 ----------------

    /** 心魔 ≥ 此值时聚煞阵效率打 SHA_PENALTY_MID 折（心魔缠身，魔窟阴晴不定） */
    public static final int HEART_GATE_MID = 60;
    public static final double SHA_PENALTY_MID = 0.8;
    /** 心魔 ≥ 此值时聚煞阵效率打 SHA_PENALTY_HIGH 折 */
    public static final int HEART_GATE_HIGH = 80;
    public static final double SHA_PENALTY_HIGH = 0.6;

    // ---------------- 开辟费用（0 → 1 级） ----------------

    /** 开辟聚煞阵所需魔晶 */
    public static final int OPEN_SHA_COIN = 30;
    /** 开辟魔晶矿所需魔晶 */
    public static final int OPEN_COIN_COIN = 30;
    /** 开辟血玉髓脉所需血玉（另需凝煞境） */
    public static final int OPEN_JADE_JADE = 2;

    // ---------------- 升级费用（L → L+1，L 为当前等级） ----------------

    /** 聚煞阵 / 魔晶矿：魔晶 = UPGRADE_COIN_FACTOR × L² */
    public static final int UPGRADE_COIN_FACTOR = 30;
    /** 血玉髓脉：血玉 = ceil(L² / 2)（至少 1） */
    public static final int UPGRADE_JADE_HALF = 2;

    /** 血玉髓脉所需最低境界位阶（凝煞）；聚煞阵 / 魔晶矿无门槛 */
    public static final int JADE_REALM_INDEX = 1;

    // ==================== 计算工具 ====================

    /** 设施类型是否合法 */
    public static boolean validType(String type) {
        return TYPE_SHA.equals(type) || TYPE_COIN.equals(type) || TYPE_JADE.equals(type);
    }

    /** 当前等级（null → 0） */
    public static int level(Integer lv) {
        return lv == null || lv < 0 ? 0 : Math.min(lv, MAX_LEVEL);
    }

    /** 聚煞阵每小时煞气（含心魔惩罚系数） */
    public static int shaPerHour(int lv, int heartDemon) {
        return (int) Math.floor(SHA_PER_HOUR * lv * shaPenalty(heartDemon));
    }

    /** 魔晶矿每小时魔晶 */
    public static int coinPerHour(int lv) {
        return COIN_PER_HOUR * lv;
    }

    /** 血玉髓脉每小时血玉千分位 */
    public static int jadeMilliPerHour(int lv) {
        return JADE_MILLI_PER_HOUR * lv;
    }

    /** 心魔对聚煞阵的效率系数 */
    public static double shaPenalty(int heartDemon) {
        int hd = heartDemon;
        if (hd >= HEART_GATE_HIGH) {
            return SHA_PENALTY_HIGH;
        }
        if (hd >= HEART_GATE_MID) {
            return SHA_PENALTY_MID;
        }
        return 1.0;
    }

    /** 升级 / 开辟费用：type → {currency, amount} */
    public static String[] upgradeCost(String type, int curLevel) {
        if (TYPE_JADE.equals(type)) {
            if (curLevel <= 0) {
                return new String[]{Coin.JADE, String.valueOf(OPEN_JADE_JADE)};
            }
            return new String[]{Coin.JADE, String.valueOf(Math.max(1, (curLevel * curLevel + UPGRADE_JADE_HALF - 1) / UPGRADE_JADE_HALF))};
        }
        int amount = curLevel <= 0
                ? (TYPE_SHA.equals(type) ? OPEN_SHA_COIN : OPEN_COIN_COIN)
                : UPGRADE_COIN_FACTOR * curLevel * curLevel;
        return new String[]{Coin.COIN, String.valueOf(amount)};
    }
}
