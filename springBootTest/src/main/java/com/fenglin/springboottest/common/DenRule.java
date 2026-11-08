package com.fenglin.springboottest.common;

/**
 * 魔族修行 · 魔窟（洞府挂机产出）数值规则
 *
 * 三座设施，各自 0~MAX_LEVEL 级（0 = 未开辟）：
 *   聚煞阵   → 煞气（直接计入累计修为 demon_user.exp）
 *   魔晶矿   → 魔晶（入钱囊）
 *   血玉髓脉 → 血玉（入钱囊，按千分位累积进位，避免低级取整归零）
 *
 * 定位（v1 数值平衡后改为「日活为本、洞府为底」）：
 *   满级每日封顶 12 小时约得 煞气 240 / 魔晶 240 / 血玉 12，
 *   低于焚香+参悟+论道+切磋+研读+秘境的日活总收益（约 655 煞气 / 387 魔晶）。
 *
 * 每日结算封顶：CAP_HOURS 是「单次未收存量」上限，DAILY_CAP_HOURS 是「一天累计可结算时长」
 *   上限。二者分开记录，故一天收两次也不会拿到 24 小时的产出。详见 db/BALANCE.md。
 *
 * 调整产出只需改本类常量（无需动业务代码），改后请同步 db/BALANCE.md。
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

    /** 离线累积存量上限（小时）：单次未收取最多积这么多，超过部分不再累积 */
    public static final int CAP_HOURS = 12;

    /** 每日累计结算上限（小时）：一天内无论收取几次，最多只按此时长结算（跨日自动清零） */
    public static final int DAILY_CAP_HOURS = 12;

    // ---------------- 每小时产量 ----------------

    /** 聚煞阵：煞气/小时 = SHA_PER_HOUR × 等级（满级 20/h，每日封顶 240） */
    public static final int SHA_PER_HOUR = 2;

    /** 魔晶矿：魔晶/小时 = COIN_PER_HOUR × 等级（满级 20/h，每日封顶 240） */
    public static final int COIN_PER_HOUR = 2;

    /** 血玉髓脉：血玉千分位/小时 = JADE_MILLI_PER_HOUR × 等级
     *  （满级 1000‰/h = 1 血玉/h，每日封顶 12 血玉） */
    public static final int JADE_MILLI_PER_HOUR = 100;

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
    /** 血玉髓脉：血玉 = ceil(L² / UPGRADE_JADE_HALF)（至少 1；v1 由 2 调为 4，降低增产门槛） */
    public static final int UPGRADE_JADE_HALF = 4;

    /** 血玉髓脉所需最低境界位阶。
     *  v1.2 由「凝煞(1)」放宽为「引煞(0)」：旧律要 2 玉才能开凿产玉设施，
     *  而新号前 7 日拿不到任何血玉，形成死锁；开局礼包已直接给 3 玉。 */
    public static final int JADE_REALM_INDEX = 0;

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
