package com.fenglin.springboottest.common;

/**
 * 魔族修行 · 入魔道（新手期）数值规则
 *
 * 设计前提：v1 的 895 煞 / 627 晶是「满级洞府 + 日活全打满」的稳态口径，
 * 新玩家开局 0 煞 / 0 晶 / 0 玉 / 引煞境，且论道、秘境、研读、典册全被境界封禁，
 * 血玉髓脉又要 2 玉 + 凝煞 —— 形成「没有产出 → 解锁不了玩法」的双重死锁。
 *
 * 因此本层只提供：① 一次性开局资粮；② 七日一次性登录礼；③ 有期限的加成倍率。
 * 全部不计入稳态日产，到期自动回落，故 db/BALANCE.md 第二节数字不变。
 * 改数值只需改本类，改后同步 db/BALANCE.md 第九节。
 */
public final class NewbieRule {

    private NewbieRule() {
    }

    /** 新兵期长度（自领取开局礼包之日起算，到期所有倍率自动回落 1.0） */
    public static final int BOOST_DAYS = 14;

    /** 新兵期：焚香 / 参悟所得煞气倍率 */
    public static final double EXP_MULT = 1.5;

    /** 新兵期：魔窟三设施每小时产量倍率 */
    public static final double DEN_MULT = 2.0;

    /** 新兵期：渡劫失败概率倍率（减半）与失败时煞气溃散倍率（减半） */
    public static final double BREAK_FAIL_MULT = 0.5;

    /** 七日礼总天数（第 1 日即开局礼包） */
    public static final int DAILY_DAYS = 7;

    /**
     * 逐日赏赐 { 煞气, 魔晶, 血玉 }，索引 = 第几日 - 1。
     * 七日合计：煞气 430 / 魔晶 950 / 血玉 9（全部为一次性，不进稳态日产）。
     */
    private static final int[][] DAY_REWARDS = {
            {120, 200, 3},   // 第 1 日 · 开局礼包：够开两座洞府 + 一部黄阶功法 + 开髓脉
            {30, 100, 0},    // 第 2 日
            {50, 0, 1},      // 第 3 日
            {0, 150, 0},     // 第 4 日
            {80, 0, 2},      // 第 5 日
            {0, 200, 0},     // 第 6 日
            {150, 300, 3}    // 第 7 日 · 足以自择一部黄阶功法
    };

    /** 第 n 日赏赐（越界返回全 0）；返回副本，调用方改不动常量表 */
    public static int[] rewardOf(int day) {
        if (day < 1 || day > DAILY_DAYS) {
            return new int[]{0, 0, 0};
        }
        return DAY_REWARDS[day - 1].clone();
    }

    /** 第 n 日赏赐的中文短文（用于消息与前端展示） */
    public static String rewardText(int day) {
        int[] r = rewardOf(day);
        StringBuilder sb = new StringBuilder();
        if (r[0] > 0) {
            sb.append("煞气 +").append(r[0]);
        }
        if (r[1] > 0) {
            if (sb.length() > 0) {
                sb.append("、");
            }
            sb.append("魔晶 +").append(r[1]);
        }
        if (r[2] > 0) {
            if (sb.length() > 0) {
                sb.append("、");
            }
            sb.append("血玉 +").append(r[2]);
        }
        return sb.length() == 0 ? "无" : sb.toString();
    }

    /** 七日礼合计，用于 BALANCE.md 与后台核对 */
    public static int[] totalRewards() {
        int exp = 0;
        int coin = 0;
        int jade = 0;
        for (int d = 1; d <= DAILY_DAYS; d++) {
            int[] r = rewardOf(d);
            exp += r[0];
            coin += r[1];
            jade += r[2];
        }
        return new int[]{exp, coin, jade};
    }
}
