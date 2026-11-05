package com.fenglin.springboottest.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 魔族修行 · 境界体系（引煞 → 无上，共十境）
 *
 * 数据源优先级：
 *   1) 数据库表 demon_realm（启动时由 RealmConfigLoader 加载覆盖）
 *   2) 下方内置默认十境（表不存在 / 表空 / 数据非法时自动回退）
 *
 * 因此调整境界只需改表（改完重启生效），无需再动 Java 与前端数组。
 */
public final class Realm {

    private Realm() {
    }

    /** 内置默认十境之名（索引即位阶）—— 表数据缺失或非法时回退到此处 */
    private static final List<String> DEFAULT_NAMES = List.of(
            "引煞", "凝煞", "魔卒", "魔尉", "魔将", "血煞", "魔尊", "魔祖", "魔圣", "无上");

    /** 内置默认十境之称 */
    private static final List<String> DEFAULT_TITLES = List.of(
            "初入魔门", "煞气凝形", "魔门行伍", "统御百夫", "坐镇一方", "威震魔域", "魔道至尊",
            "万法归魔", "圣魔临世", "无上魔道");

    /** 内置默认十境之述 */
    private static final List<String> DEFAULT_DESCS = List.of(
            "引煞入体，初踏魔途",
            "煞气凝形，根基渐固",
            "位列行伍，可入秘境",
            "百夫之长，可窥魔典",
            "坐镇一方，执掌典册",
            "煞名远播，威震群魔",
            "魔威盖世，群魔俯首",
            "一念通天，统御群修",
            "魔道通玄，圣威昭昭",
            "超然物外，万法皆空");

    /**
     * 内置默认冲击煞气（索引即目标位阶）
     *
     * 重要语义：自 v2 起煞气为「累计总修为」，冲境只判达标、**不再扣除**。
     * 因此下表是「晋升该境所需的累计煞气总量」，而非一次性的消耗值。
     */
    private static final List<Integer> DEFAULT_THRESHOLDS = List.of(
            100, 400, 1200, 3000, 6000, 10000, 16000, 24000, 35000, 50000);

    /** 十境之名（索引即位阶）—— 运行时值，可被 demon_realm 表覆盖 */
    public static volatile List<String> NAMES = DEFAULT_NAMES;

    /** 十境之称 —— 运行时值，可被 demon_realm 表覆盖 */
    public static volatile List<String> TITLES = DEFAULT_TITLES;

    /** 十境之述 —— 运行时值，可被 demon_realm 表覆盖 */
    public static volatile List<String> DESCS = DEFAULT_DESCS;

    /** 冲击各境界所需煞气（索引即目标位阶）—— 运行时值，可被 demon_realm 表覆盖 */
    public static volatile List<Integer> THRESHOLDS = DEFAULT_THRESHOLDS;

    /** 十境配置是否已由 demon_realm 表覆盖（false = 仍用内置默认值） */
    private static volatile boolean loadedFromTable = false;

    /**
     * 各功能模块所需最低境界位阶（与 demon_realm 表描述、魔枢典册「典册封禁之律」一致）：
     * art 引煞（无门槛）/ trial 魔卒 / debate 魔卒 / codex 魔尉 / archive 魔将
     */
    public static final Map<String, Integer> FEATURE_REQUIREMENT = Map.of(
            "art", 0,
            "trial", 2,
            "debate", 2,
            "codex", 3,
            "archive", 4);

    /** 每日参悟上限（篇） */
    public static final int DAILY_EXPLORE_LIMIT = 10;

    // ==================== 境界分重（每境九重） ====================

    /** 每境重数：引煞一重 ~ 九重，九重圆满方可冲下一境 */
    public static final int LAYERS = 9;

    /** 重数中文名（索引 = 重数 - 1） */
    public static final List<String> LAYER_NAMES = List.of(
            "一", "二", "三", "四", "五", "六", "七", "八", "九");

    /** 「无上」境无下一境，九重之后仍可继续积累——每重额外跨度（煞气） */
    public static final int LAST_REALM_LAYER_SPAN = 2000;

    /**
     * 进入某境所需煞气（引煞为 0，其余取其阈值）。
     * 与 {@link #thresholdOf(int)} 的区别：thresholdOf 是「冲击该境所需」，引煞无冲击动作。
     */
    public static int entryExp(int index) {
        if (index <= 0) {
            return 0;
        }
        return thresholdOf(index);
    }

    /** 下一境门槛；已至无上时按最后一重跨度外推 */
    public static int nextRealmExp(int index) {
        if (index >= NAMES.size() - 1) {
            return entryExp(index) + LAST_REALM_LAYER_SPAN * LAYERS;
        }
        return thresholdOf(index + 1);
    }

    /** 当前境每重所需煞气跨度（区间等分九份） */
    public static double layerSpan(int index) {
        int lo = entryExp(index);
        int hi = nextRealmExp(index);
        return Math.max(1.0, (hi - lo) / (double) LAYERS);
    }

    /**
     * 由累计煞气推算当前重数（1~9）。
     * 语义：重数只用于展示与冲境门槛校验，冲境仍需累计煞气达下一境阈值。
     */
    public static int layerOf(int exp, int index) {
        int lo = entryExp(index);
        if (exp <= lo) {
            return 1;
        }
        double span = layerSpan(index);
        int k = (int) Math.floor((exp - lo) / span) + 1;
        return Math.max(1, Math.min(LAYERS, k));
    }

    /** 该重起始煞气（含） */
    public static int layerStartExp(int index, int layer) {
        int lo = entryExp(index);
        return (int) Math.round(lo + (clampLayer(layer) - 1) * layerSpan(index));
    }

    /** 下一重起始煞气（九重时为下一境门槛） */
    public static int layerNextExp(int index, int layer) {
        if (layer >= LAYERS) {
            return nextRealmExp(index);
        }
        return layerStartExp(index, layer + 1);
    }

    /** 重数中文名（越界按一重/九重收敛） */
    public static String layerName(int layer) {
        return LAYER_NAMES.get(clampLayer(layer) - 1);
    }

    /** 重数收敛到 1~9 */
    public static int clampLayer(int layer) {
        return Math.max(1, Math.min(LAYERS, layer));
    }

    /** 累计重数（跨境累计，用于数值成长）：境阶×9 + 重数-1 */
    public static int totalLayer(int index, int layer) {
        return index * LAYERS + (clampLayer(layer) - 1);
    }

    /** 境界折算值（含重数小数部分），如 凝煞境·三重 = 1 + 2/9 */
    public static double realmLevel(int index, int layer) {
        return index + (clampLayer(layer) - 1) / (double) LAYERS;
    }

    /** 是否九重圆满（可冲境） */
    public static boolean isFullLayer(int layer) {
        return clampLayer(layer) >= LAYERS;
    }

    /** 每日切磋上限（场，跨日清零） */
    public static final int DAILY_SPAR_LIMIT = 5;

    /** 每日论道答题上限（题，跨日清零）—— 防止反复作答同一论题刷煞气 */
    public static final int DAILY_QUIZ_LIMIT = 10;

    /** 每参悟一篇所得煞气 */
    public static final int EXPLORE_GAIN = 5;

    /** 论道答对所得煞气 */
    public static final int QUIZ_GAIN = 10;

    /** 论道答错所失煞气 */
    public static final int QUIZ_LOSS = 5;

    /** 切磋胜者所得煞气 */
    public static final int SPAR_WIN = 15;

    /** 切磋败者所失煞气 */
    public static final int SPAR_LOSS = 10;

    // ==================== v2 玩法数值 ====================

    /** 战力公式中每一重境界的权重：战力 = 境阶 × 本值 + 煞气。
     *  取 8000 是为了让「境界」真正压过「囤煞气」——跨一重需 8000 煞气优势才追得平，
     *  从而杜绝旧版「突破反而变弱、不如囤气霸榜」的畸形最优解。 */
    public static final int POWER_PER_REALM = 8000;

    /** 切磋天运波动上限（0 ~ 本值随机） */
    public static final int SPAR_RANDOM = 600;

    /** 每日签到基础煞气 */
    public static final int SIGN_BASE = 10;

    /** 连签每多一天额外煞气 */
    public static final int SIGN_STEP = 5;

    /** 连签加成封顶天数（第 7 天起不再递增） */
    public static final int SIGN_STREAK_CAP = 7;

    /** 连满 7 天的额外赏赐 */
    public static final int SIGN_BONUS_7 = 50;

    /** 参悟触发机缘奇遇的概率（0.12 = 12%） */
    public static final double CHANCE_RATE = 0.12;

    /** 研读一篇典籍所得煞气 */
    public static final int READ_GAIN = 8;

    /** 每日研读上限（篇，超出不再给煞气） */
    public static final int DAILY_READ_LIMIT = 5;

    /** 冲境（渡劫）失败概率（0.20 = 20%） */
    public static final double BREAK_FAIL_RATE = 0.20;

    /** 渡劫失败扣损：当前煞气的比例 */
    public static final double BREAK_FAIL_LOSS_RATE = 0.10;

    /** 渡劫失败扣损上限 */
    public static final int BREAK_FAIL_LOSS_CAP = 500;

    /** 连败达此次数后本次渡劫必定成功（保底，避免卡死劝退） */
    public static final int BREAK_FAIL_GUARANTEE = 2;

    /** 每习得一部功法带来的参悟加成（0.05 = +5%），最多叠加 ART_BONUS_CAP 部 */
    public static final double ART_BONUS = 0.05;

    /** 功法参悟加成叠加上限（部） */
    public static final int ART_BONUS_CAP = 4;

    /** 每一世转生带来的永久参悟加成（0.10 = +10%） */
    public static final double REINCARNATE_BONUS = 0.10;

    /** 转世加成封顶（世数）：最多按 10 世计，即参悟/秘境倍率最高 +100% */
    public static final int REINCARNATE_CAP = 10;

    /** 境界不足提示文案（%s 为所需境界名） */
    public static final String DENY_MSG = "修为不足，需修至「%s」境方可入内。";

    // ==================== 秘境（demon_trial）玩法 ====================

    /** 秘境缺省每日可入次数（表内 daily_limit 未设时取此值） */
    public static final int REALM_DAILY_DEFAULT = 3;

    /** 秘境负伤：所获煞气的折损比例（0.5 = 半数不保） */
    public static final double REALM_RISK_LOSS = 0.5;

    /** 秘境负伤折损上限（避免一次亏空过大） */
    public static final int REALM_RISK_CAP = 300;

    /** 秘境机缘：所得煞气翻倍数 */
    public static final double REALM_TREASURE_MULTI = 2.0;

    /** 是否已从 demon_realm 表加载配置（诊断用） */
    public static boolean isLoadedFromTable() {
        return loadedFromTable;
    }

    /** 恢复为内置默认十境（表加载失败或需重置时调用） */
    public static synchronized void resetToDefaults() {
        NAMES = DEFAULT_NAMES;
        TITLES = DEFAULT_TITLES;
        DESCS = DEFAULT_DESCS;
        THRESHOLDS = DEFAULT_THRESHOLDS;
        loadedFromTable = false;
    }

    /**
     * 用 demon_realm 表的数据覆盖内置十境。
     * 校验不通过时抛异常，调用方应捕获并保留内置默认值，避免启动失败。
     *
     * @param names      境界名（索引即位阶，必须从低到高连续）
     * @param titles     境界称号（不足补空串）
     * @param descs      境界描述（不足补空串）
     * @param thresholds 冲击各地阶所需煞气（不足补 0）
     */
    public static synchronized void applyTable(List<String> names, List<String> titles,
                                               List<String> descs, List<Integer> thresholds) {
        if (names == null || names.size() < 2) {
            throw new IllegalArgumentException("境界数量不足（至少 2 境）");
        }
        if (new HashSet<>(names).size() != names.size()) {
            throw new IllegalArgumentException("境界名存在重复");
        }
        for (String n : names) {
            if (n == null || n.isBlank()) {
                throw new IllegalArgumentException("存在空的境界名");
            }
        }

        NAMES = List.copyOf(names);
        TITLES = List.copyOf(padStrings(titles, names.size()));
        DESCS = List.copyOf(padStrings(descs, names.size()));
        THRESHOLDS = List.copyOf(padThresholds(thresholds, names.size()));
        loadedFromTable = true;
    }

    /** 补齐称号/描述长度，缺失补空串 */
    private static List<String> padStrings(List<String> src, int size) {
        List<String> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String v = (src != null && i < src.size()) ? src.get(i) : null;
            out.add(v == null ? "" : v);
        }
        return out;
    }

    /** 补齐阈值长度，缺失补 0 */
    private static List<Integer> padThresholds(List<Integer> src, int size) {
        List<Integer> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Integer v = (src != null && i < src.size()) ? src.get(i) : null;
            out.add(v == null ? 0 : v);
        }
        return out;
    }

    /** 境界名 → 位阶（未知境界视为引煞 0） */
    public static int indexOf(String name) {
        if (name == null) {
            return 0;
        }
        int i = NAMES.indexOf(name);
        return i >= 0 ? i : 0;
    }

    /** 位阶 → 境界名（越界视为引煞） */
    public static String name(int index) {
        if (index < 0 || index >= NAMES.size()) {
            return NAMES.get(0);
        }
        return NAMES.get(index);
    }

    /** 冲击目标位阶所需煞气 */
    public static int thresholdOf(int targetIndex) {
        if (targetIndex < 0 || targetIndex >= THRESHOLDS.size()) {
            return 0;
        }
        return THRESHOLDS.get(targetIndex);
    }

    /** 当前境界位阶是否达到某模块所需门槛 */
    public static boolean satisfied(String cultivation, String type) {
        return indexOf(cultivation) >= requirementOf(type);
    }

    /** 某模块所需最低境界位阶（未知模块视为 0 即公开） */
    public static int requirementOf(String type) {
        return FEATURE_REQUIREMENT.getOrDefault(type, 0);
    }
}
