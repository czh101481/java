package com.fenglin.springboottest.common;

import java.util.List;
import java.util.Map;

/**
 * 魔族修行 · 境界体系（引煞 → 魔圣，共十境）
 * 与前端 REALM_NAMES / REALM_THRESHOLDS / TYPE_REQ 保持一致
 */
public final class Realm {

    private Realm() {
    }

    /** 十境之名（索引即位阶） */
    public static final List<String> NAMES = List.of(
            "引煞", "凝煞", "魔卒", "魔尉", "魔将", "血煞", "魔尊", "魔祖", "无上", "魔圣");

    /** 十境之称 */
    public static final List<String> TITLES = List.of(
            "初入魔门", "煞气凝形", "魔门行伍", "统御百夫", "坐镇一方", "威震魔域", "魔道至尊",
            "万法归魔", "无上魔道", "圣魔临世");

    /** 十境之述 */
    public static final List<String> DESCS = List.of(
            "引煞入体，初踏魔途",
            "煞气凝形，根基渐固",
            "位列行伍，可入历练",
            "百夫之长，可窥魔典",
            "坐镇一方，执掌典册",
            "煞名远播，威震群魔",
            "魔威盖世，群魔俯首",
            "一念通天，统御群修",
            "超然物外，万法皆空",
            "魔道通玄，圣威昭昭");

    /** 冲击各境界所需煞气（索引即目标位阶；引煞为起始境，无人需冲击） */
    public static final List<Integer> THRESHOLDS = List.of(
            100, 300, 800, 1500, 2400, 3800, 8000, 12000, 15000, 20000);

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
