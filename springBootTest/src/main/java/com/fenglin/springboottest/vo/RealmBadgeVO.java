package com.fenglin.springboottest.vo;

import lombok.Data;

import java.util.List;

/**
 * 个人道牌（勋章墙）：修者信息 + 十境勋章 + 修行数据总览（GET /api/home/badges）
 */
@Data
public class RealmBadgeVO {

    /** 修者道号 */
    private String name;

    /** 当前境界名 */
    private String cultivation;

    /** 当前位阶 */
    private int index;

    /** 境界称号 */
    private String title;

    /** 当前煞气修为 */
    private int exp;

    /** 已解锁殿堂数 */
    private int unlockCount;

    /** 殿堂总数 */
    private int totalHalls;

    /** 今日已参悟篇数 */
    private int exploreCount;

    /** 每日参悟上限 */
    private int exploreLimit;

    /** 十境勋章（升序） */
    private List<MedalVO> medallions;

    /**
     * 单枚勋章：位阶 / 境界名 / 称号 / 是否已点亮
     */
    @Data
    public static class MedalVO {

        /** 位阶 */
        private int index;

        /** 境界名 */
        private String name;

        /** 境界称号 */
        private String title;

        /** 是否已点亮（位阶达到即点亮） */
        private boolean unlocked;

        public MedalVO() {
        }

        public MedalVO(int index, String name, String title, boolean unlocked) {
            this.index = index;
            this.name = name;
            this.title = title;
            this.unlocked = unlocked;
        }
    }
}
