package com.fenglin.springboottest.common;

import java.util.List;
import java.util.Map;

/**
 * 魔族修行 · 功法阶位与修炼进度常量（硬性设定，改动即改平衡）
 *
 * 阶位：黄 → 玄 → 地 → 天 → 仙。阶位决定上限、威力、反噬与市价。
 * 形态：完整（可修至无上） / 残片（价 = 完整 × 0.35，最高止步大成，威能折损）。
 * 进度：入门 → 小成 → 大成 → 圆满 → 入道 → 无上（每部功法各自推进）。
 *
 * 价格恒定（v1 数值平衡后，血玉计价三阶下调，详见 db/BALANCE.md）：
 *   黄 完整 100 魔晶 / 残片 35      玄 完整 800 魔晶 / 残片 280
 *   地 完整 40 血玉 / 残片 14       天 完整 150 血玉 / 残片 53
 *   仙 完整 600 血玉 / 残片 210
 */
public final class ArtRank {

    private ArtRank() {
    }

    /** 阶位由低至高 */
    public static final List<String> GRADES = List.of("黄", "玄", "地", "天", "仙");

    /** 阶位定位（详情/后台说明用） */
    public static final Map<String, String> GRADE_DESC = Map.of(
            "黄", "凡尘俗法·底层通用",
            "玄", "宗门常备·中坚功法",
            "地", "镇族镇宗·核心功法",
            "天", "绝世秘传·大宗顶级",
            "仙", "仙道本源·天地至宝");

    /** 各阶位重数上限：黄 4 · 玄 6 · 地 7 · 天 10 · 仙 10 */
    public static final Map<String, Integer> MAX_LAYER = Map.of(
            "黄", 4, "玄", 6, "地", 7, "天", 10, "仙", 10);

    /** 重数中文：1 → 一 …… 10 → 十 */
    private static final String[] LAYER_CN = {
            "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};

    /** 该阶位的重数上限（未知阶位按黄阶 4 重） */
    public static int maxLayerOf(String grade) {
        return MAX_LAYER.getOrDefault(nvl(grade), 4);
    }

    /** 重数文案：3 → 「三重」 */
    public static String layerName(int layer) {
        int i = Math.max(1, Math.min(LAYER_CN.length, layer)) - 1;
        return LAYER_CN[i] + "重";
    }

    /** 完整功法价（魔晶）：黄、玄以魔晶计价 */
    private static final Map<String, Integer> FULL_COIN = Map.of("黄", 100, "玄", 800);

    /** 完整功法价（血玉）：地、天、仙以血玉计价（v1：50/300/2000 → 40/150/600） */
    private static final Map<String, Integer> FULL_JADE = Map.of("地", 40, "天", 150, "仙", 600);

    /** 残片价 = 完整价 × 0.35（硬性规则） */
    public static final double FRAG_RATIO = 0.35;

    /** 残片威能折损：40%–60% 折损，取中值 45%，即保留 55% 威力 */
    public static final double FRAG_WEAKEN = 0.55;

    /** 残片最高可修至「大成」（stage = 3） */
    public static final int FRAG_MAX_STAGE = 3;

    /** 六重进度名（下标 + 1 = stage） */
    public static final String[] STAGES = {"入门", "小成", "大成", "圆满", "入道", "无上"};

    /** 各重战力/数值系数：入门 0.30 → 无上 1.80（无上战力跨阶） */
    public static final double[] STAGE_FACTOR = {0.30, 0.55, 0.80, 1.00, 1.30, 1.80};

    /** 入门→小成 / 小成→大成 / 大成→圆满 所需熟练度（圆满之后靠机缘，不再靠日课） */
    public static final int[] STAGE_NEED = {100, 200, 400, 0, 0, 0};

    /** 每部功法每日可修炼课数 */
    public static final int TRAIN_DAILY = 3;

    /** 每课消耗煞气 */
    public static final int TRAIN_COST_EXP = 10;

    /** 完整功法每课熟练度 */
    public static final int TRAIN_PROF_FULL = 10;

    /** 残片每课熟练度（残缺滞涩，进境减半） */
    public static final int TRAIN_PROF_FRAG = 5;

    /** 心魔上限 */
    public static final int HEART_MAX = 100;

    /** 修残篇魔功每课滋生心魔（反噬翻倍） */
    public static final int HEART_FRAG_TRAIN = 2;

    /** 完整魔功入道后每课「以煞养心」，心魔消退 */
    public static final int HEART_CALM = 1;

    /** 心魔 ≥ 此值不可冲境（心魔劫） */
    public static final int HEART_GATE = 60;

    /** 冲境成功渡劫洗心，心魔下降 */
    public static final int HEART_AFTER_BREAK = 10;

    /** 秘境「机缘」掉落「悟道机缘」的概率（%）：为圆满功法留一条入道之路 */
    public static final int INSIGHT_DROP_RATE = 35;

    // ==================== 价格 ====================

    /** 是否为血玉计价的阶位 */
    public static boolean isJade(String grade) {
        return FULL_JADE.containsKey(nvl(grade));
    }

    /** 计价货币代号：coin / jade */
    public static String currencyOf(String grade) {
        return isJade(grade) ? Coin.JADE : Coin.COIN;
    }

    /** 计价货币名 */
    public static String currencyName(String grade) {
        return isJade(grade) ? Coin.JADE_NAME : Coin.COIN_NAME;
    }

    /** 完整功法价（未知阶位按黄阶） */
    public static int fullPrice(String grade) {
        String g = nvl(grade);
        Integer coin = FULL_COIN.get(g);
        if (coin != null) {
            return coin;
        }
        Integer jade = FULL_JADE.get(g);
        return jade == null ? FULL_COIN.get("黄") : jade;
    }

    /** 残片价（完整 × 0.35，四舍五入，至少 1） */
    public static int fragPrice(String grade) {
        return Math.max(1, (int) Math.round(fullPrice(grade) * FRAG_RATIO));
    }

    /** 按形态取价：form 1 = 残片 */
    public static int priceOf(String grade, int form) {
        return form == 1 ? fragPrice(grade) : fullPrice(grade);
    }

    /** 售价文案：「完整 100 魔晶」/「残片 35 魔晶」 */
    public static String priceText(String grade, int form) {
        return String.format("%s %d %s", formName(form), priceOf(grade, form), currencyName(grade));
    }

    // ==================== 形态 / 进度 ====================

    public static String formName(int form) {
        return form == 1 ? "残片" : "完整";
    }

    /** stage 1..6 → 中文重名 */
    public static String stageName(int stage) {
        int i = Math.max(1, Math.min(STAGES.length, stage)) - 1;
        return STAGES[i];
    }

    /** stage 对应的数值系数 */
    public static double stageFactor(int stage) {
        int i = Math.max(1, Math.min(STAGES.length, stage)) - 1;
        return STAGE_FACTOR[i];
    }

    /** 某重修炼至下一重所需熟练度（圆满及以上返回 0） */
    public static int needOf(int stage) {
        if (stage < 1 || stage >= STAGE_NEED.length + 1) {
            return 0;
        }
        return STAGE_NEED[stage - 1];
    }

    /** 形态 + 重数 的最终数值系数（残片再乘折损） */
    public static double factorOf(int form, int stage) {
        double f = stageFactor(stage);
        return form == 1 ? f * FRAG_WEAKEN : f;
    }

    /** 是否可再行日课：残片止步大成；圆满须机缘；完整入道/无上可「以煞养心」 */
    public static boolean trainable(int form, int stage) {
        if (stage == 4) {
            return false;
        }
        if (form == 1) {
            return stage < FRAG_MAX_STAGE;
        }
        return true;
    }

    // ==================== 心魔 ====================

    /** 心魔段位文案 */
    public static String heartState(int heart) {
        if (heart >= 80) {
            return "魔焰焚心";
        }
        if (heart >= ArtRank.HEART_GATE) {
            return "心魔缠身";
        }
        if (heart >= 40) {
            return "心绪躁动";
        }
        if (heart >= 20) {
            return "心湖微澜";
        }
        return "心湖无波";
    }

    /** 心魔对战力的折损系数：≥80 打七五折，≥60 打九折 */
    public static double heartPenalty(int heart) {
        if (heart >= 80) {
            return 0.75;
        }
        if (heart >= HEART_GATE) {
            return 0.90;
        }
        return 1.0;
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }
}
