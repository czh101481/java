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
            "位列行伍，可入历练",
            "百夫之长，可窥魔典",
            "坐镇一方，执掌典册",
            "煞名远播，威震群魔",
            "魔威盖世，群魔俯首",
            "一念通天，统御群修",
            "魔道通玄，圣威昭昭",
            "超然物外，万法皆空");

    /** 内置默认冲击煞气（索引即目标位阶） */
    private static final List<Integer> DEFAULT_THRESHOLDS = List.of(
            100, 300, 800, 1500, 2400, 3800, 8000, 12000, 15000, 20000);

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

    /** 各功能模块所需最低境界位阶：art 0 / trial 1 / debate 2 / codex 3 / archive 4 */
    public static final Map<String, Integer> FEATURE_REQUIREMENT = Map.of(
            "art", 0,
            "trial", 1,
            "debate", 2,
            "codex", 3,
            "archive", 4);

    /** 每日参悟上限（篇） */
    public static final int DAILY_EXPLORE_LIMIT = 10;

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

    /** 境界不足提示文案（%s 为所需境界名） */
    public static final String DENY_MSG = "修为不足，需修至「%s」境方可入内。";

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
