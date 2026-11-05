package com.fenglin.springboottest.common;

/**
 * 魔族修行 · 货币体系常量与产出规则
 *
 * 双币制：
 *   coin = 魔晶（日常软通货）：签到、参悟、论道、切磋、秘境产出，用于购买寻常物品
 *   jade = 血玉（稀有硬通货）：连签满七日、秘境机缘产出，用于兑换珍品
 *
 * 调整产出只需改本类常量（无需动业务代码）。
 */
public final class Coin {

    private Coin() {
    }

    /** 魔晶 */
    public static final String COIN = "coin";
    /** 血玉 */
    public static final String JADE = "jade";

    public static final String COIN_NAME = "魔晶";
    public static final String JADE_NAME = "血玉";

    // ---------------- 签到产出 ----------------
    /** 连签第 1 日基础魔晶 */
    public static final int SIGN_COIN_BASE = 20;
    /** 每多连签一日加成的魔晶 */
    public static final int SIGN_COIN_STEP = 5;
    /** 单日魔晶封顶 */
    public static final int SIGN_COIN_CAP = 60;
    /** 每连签满七日额外血玉（v1 由 1 调为 2，补日活侧的硬通货来源） */
    public static final int SIGN_JADE_BONUS = 2;

    // ---------------- 玩法产出 ----------------
    /** 每参悟一篇所得魔晶 */
    public static final int EXPLORE_COIN = 3;
    /** 论道答对一题所得魔晶 */
    public static final int QUIZ_COIN = 8;
    /** 切磋获胜一场所得魔晶 */
    public static final int SPAR_COIN = 15;
    /** 秘境产出魔晶 = 所得煞气 / 该系数（下限 SECRET_COIN_MIN） */
    public static final int SECRET_COIN_RATE = 10;
    public static final int SECRET_COIN_MIN = 5;
    /** 秘境触发「机缘」额外血玉（v1 由 1 调为 2） */
    public static final int SECRET_JADE_TREASURE = 2;

    // ---------------- 鬼市（玩家间交易） ----------------
    /** 鬼市抽成百分比：成交额的 5% 由系统回收（焚香钱） */
    public static final int TRADE_FEE_PERCENT = 5;
    /** 每人同时挂单上限 */
    public static final int TRADE_MAX_OPEN = 10;
    /** 单件挂单价格上限 */
    public static final int TRADE_MAX_PRICE = 99999;
    /** 富甲榜折算：1 血玉 = 100 魔晶 */
    public static final int JADE_TO_COIN = 100;

    /** 折扣价：percent 为应付百分比（如 80 即八折），至少 1 */
    public static int discountPrice(int base, int percent) {
        if (base <= 0) {
            return base;
        }
        if (percent <= 0 || percent >= 100) {
            return base;
        }
        return Math.max(1, (int) Math.round(base * percent / 100.0));
    }

    /** 鬼市抽成金额（向下取整） */
    public static int tradeFee(int amount) {
        return Math.max(0, amount) * TRADE_FEE_PERCENT / 100;
    }

    // ---------------- 流水事由文案 ----------------
    public static String reasonText(String reason) {
        if (reason == null) {
            return "魔晶流转";
        }
        switch (reason) {
            case "sign":
                return "每日焚香";
            case "explore":
                return "参悟典籍";
            case "quiz":
                return "论道得胜";
            case "spar":
                return "切磋获胜";
            case "secret":
                return "秘境探索";
            case "buy":
                return "藏宝阁购宝";
            case "item":
                return "开囊所得";
            case "trade":
                return "鬼市易物";
            case "art":
                return "功法购得";
            case "den":
                return "魔窟产出";
            case "den_up":
                return "魔窟扩建";
            case "den_up_refund":
                return "扩建退款";
            case "admin":
                return "魔门赏赐";
            default:
                return "魔晶流转";
        }
    }

    /** 签到魔晶：随连签天数递增，封顶 SIGN_COIN_CAP */
    public static int signCoin(int streak) {
        int gain = SIGN_COIN_BASE + Math.max(0, Math.min(streak, 7) - 1) * SIGN_COIN_STEP;
        return Math.min(SIGN_COIN_CAP, gain);
    }

    /** 秘境魔晶：按所得煞气换算，至少 SECRET_COIN_MIN */
    public static int secretCoin(int expGain) {
        return Math.max(SECRET_COIN_MIN, Math.max(0, expGain) / SECRET_COIN_RATE);
    }
}
