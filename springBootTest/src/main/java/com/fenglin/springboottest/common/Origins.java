package com.fenglin.springboottest.common;

import com.fenglin.springboottest.entity.OriginConfig;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 魔族修行 · 魔种天赋体系（血煞 / 幽冥 / 噬魂 / 虚妄）
 *
 * 数值优先读 demon_origin 表（由 OriginConfigLoader 启动时装载），
 * 表缺失 / 表空 / 数据非法时自动回退到本类内置默认值，保证玩法不会因配置问题瘫痪。
 *
 * 与 Realm 的设计保持一致：静态持有 + 可被表覆盖 + 可热重载。
 */
public final class Origins {

    private Origins() {
    }

    /** 单个魔种的天赋数值 */
    public static final class Bonus {

        /** 魔种名，与 demon_user.realm 对应 */
        private final String origin;
        /** 天赋名 */
        private final String title;
        /** 天赋描述 */
        private final String description;
        /** 参悟煞气加成倍率（0.5 = +50%） */
        private final double exploreBonus;
        /** 切磋战力加成倍率（0.12 = +12%） */
        private final double sparPowerBonus;
        /** 切磋胜利额外奖励倍率（1.0 = 奖励翻倍） */
        private final double sparRewardBonus;
        /** 参悟顿悟（双倍）概率（0.25 = 25%） */
        private final double insightChance;

        public Bonus(String origin, String title, String description,
                     double exploreBonus, double sparPowerBonus,
                     double sparRewardBonus, double insightChance) {
            this.origin = origin;
            this.title = title;
            this.description = description;
            this.exploreBonus = exploreBonus;
            this.sparPowerBonus = sparPowerBonus;
            this.sparRewardBonus = sparRewardBonus;
            this.insightChance = insightChance;
        }

        public String getOrigin() {
            return origin;
        }

        public String getTitle() {
            return title;
        }

        public String getDescription() {
            return description;
        }

        public double getExploreBonus() {
            return exploreBonus;
        }

        public double getSparPowerBonus() {
            return sparPowerBonus;
        }

        public double getSparRewardBonus() {
            return sparRewardBonus;
        }

        public double getInsightChance() {
            return insightChance;
        }

        /** 是否携带任何实际加成 */
        public boolean hasAnyBonus() {
            return exploreBonus > 0 || sparPowerBonus > 0 || sparRewardBonus > 0 || insightChance > 0;
        }
    }

    /** 凡体：未匹配到魔种时的中性天赋（无任何加成） */
    public static final Bonus NEUTRAL =
            new Bonus("凡体", "无特殊天赋", "未觉醒魔种，无额外加成。", 0, 0, 0, 0);

    /** 内置默认四系（与 db/demon_gameplay.sql 种子一致） */
    private static Map<String, Bonus> defaults() {
        Map<String, Bonus> m = new LinkedHashMap<>();
        m.put("血煞", new Bonus("血煞", "战意滔天",
                "血煞入体，愈战愈勇。与人切磋时战力大涨，正面硬撼无人可挡。", 0, 0.12, 0, 0));
        m.put("幽冥", new Bonus("幽冥", "幽息吞吐",
                "幽冥之体，静默纳煞。参悟魔域所得煞气远胜他人，进境最速。", 0.50, 0, 0, 0));
        m.put("噬魂", new Bonus("噬魂", "夺魂摄魄",
                "噬魂之术，胜则食髓。切磋得胜时可将败者煞气一并吞下，掠夺倍增。", 0, 0, 1.00, 0));
        m.put("虚妄", new Bonus("虚妄", "虚妄顿悟",
                "虚妄之心，真假难辨。参悟时偶得顿悟，煞气瞬间翻倍。", 0, 0, 0, 0.25));
        return java.util.Collections.unmodifiableMap(m);
    }

    /** 默认天赋表 */
    public static final Map<String, Bonus> DEFAULT_ORIGINS = defaults();

    /** 当前生效的天赋表（可由 demon_origin 表覆盖） */
    private static volatile Map<String, Bonus> current = DEFAULT_ORIGINS;

    /** 是否已从表中加载（诊断用） */
    private static volatile boolean loadedFromTable = false;

    /**
     * 取某魔种的天赋；未匹配（含历史脏数据 / 空魔种）返回 NEUTRAL
     */
    public static Bonus of(String origin) {
        if (origin == null || origin.isBlank()) {
            return NEUTRAL;
        }
        Bonus b = current.get(origin.trim());
        return b == null ? NEUTRAL : b;
    }

    /** 全部魔种天赋（按 sort 顺序） */
    public static Collection<Bonus> all() {
        return new ArrayList<>(current.values());
    }

    /** 用表数据覆盖当前天赋（由 OriginConfigLoader 调用） */
    public static void applyTable(List<OriginConfig> rows) {
        if (rows == null || rows.isEmpty()) {
            resetToDefaults();
            return;
        }
        Map<String, Bonus> m = new LinkedHashMap<>();
        for (OriginConfig r : rows) {
            if (r == null || r.getOrigin() == null || r.getOrigin().isBlank()) {
                continue;
            }
            m.put(r.getOrigin().trim(), new Bonus(
                    r.getOrigin().trim(),
                    r.getTitle() == null ? r.getOrigin().trim() : r.getTitle(),
                    r.getDescription() == null ? "" : r.getDescription(),
                    dbl(r.getExploreBonus()),
                    dbl(r.getSparPowerBonus()),
                    dbl(r.getSparRewardBonus()),
                    dbl(r.getInsightChance())));
        }
        if (m.isEmpty()) {
            resetToDefaults();
            return;
        }
        current = java.util.Collections.unmodifiableMap(m);
        loadedFromTable = true;
    }

    /** 回退为内置默认 */
    public static void resetToDefaults() {
        current = DEFAULT_ORIGINS;
        loadedFromTable = false;
    }

    /** 是否已从 demon_origin 表加载 */
    public static boolean isLoadedFromTable() {
        return loadedFromTable;
    }

    private static double dbl(BigDecimal v) {
        return v == null ? 0d : v.doubleValue();
    }
}
