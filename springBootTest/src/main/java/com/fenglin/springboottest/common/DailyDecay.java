package com.fenglin.springboottest.common;

/**
 * 魔族修行 · 每日循环玩法的「收益递减」曲线（v1.1）
 *
 * 为什么需要它：论道题库只有 20 题、研读是「全库首次一次性」，旧版一旦答完
 * 就再无进项，v1 数值预算表里的「论道 100 煞 / 研读 40 煞」只在前两日成立。
 * 改为跨日可重复后，需要一条软上限，防止一天内把同一批内容刷穿。
 *
 * 口径（用户定案「满额不变 + 超额递减」）：
 *   - 当日第 1 ~ fullLimit 次：**100%**，与 v1 预算表完全一致；
 *   - 其后一档（长度 ceil(fullLimit/2)）：**50%**；
 *   - 再后一档：**25%**；
 *   - 再往后：**0%**（仍可操作，但无赏，且给出提示）。
 *   次数每日跨子夜归零，故「每日打满」成为长期稳态而非一次性内容。
 *
 * 论道 fullLimit = Realm.DAILY_QUIZ_LIMIT(10) → 1-10 全额 / 11-15 半额 / 16-20 四分之一；
 * 研读 fullLimit = Realm.DAILY_READ_LIMIT(5) → 1-5 全额 / 6-8 半额 / 9-11 四分之一 / 12+ 无赏。
 *
 * 调整曲线只需改本类；各玩法的满额次数仍由 Realm 常量定义。
 */
public final class DailyDecay {

    private DailyDecay() {
    }

    /** 满额之后每个衰减档的长度 */
    private static int band(int fullLimit) {
        return Math.max(1, (fullLimit + 1) / 2);
    }

    /** 第 nth 次（从 1 起）的收益百分比：100 / 50 / 25 / 0 */
    public static int percent(int nth, int fullLimit) {
        if (fullLimit <= 0 || nth <= fullLimit) {
            return 100;
        }
        int over = nth - fullLimit;
        int b = band(fullLimit);
        if (over <= b) {
            return 50;
        }
        if (over <= b * 2) {
            return 25;
        }
        return 0;
    }

    /** 按档位结算基础收益（四舍五入，不产生负数） */
    public static int apply(int base, int nth, int fullLimit) {
        return (int) Math.round(Math.max(0, base) * percent(nth, fullLimit) / 100.0);
    }

    /** 该次是否处于满额段 */
    public static boolean isFull(int nth, int fullLimit) {
        return percent(nth, fullLimit) == 100;
    }

    /** 档位中文名，供玩家提示文案 */
    public static String label(int nth, int fullLimit) {
        switch (percent(nth, fullLimit)) {
            case 100:
                return "全额";
            case 50:
                return "半额";
            case 25:
                return "四分之一";
            default:
                return "无赏";
        }
    }
}
