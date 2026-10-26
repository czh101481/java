package com.fenglin.springboottest.vo;

import lombok.Data;

/**
 * 修者境界状态：境界 / 位阶 / 进度 / 修为 / 下一境阈值 / 今日参悟（GET /api/home/realm）
 */
@Data
public class RealmStatusVO {

    /** 修者道号 */
    private String name;

    /** 当前境界名 */
    private String cultivation;

    /** 当前位阶（0 = 引煞） */
    private int index;

    /** 总境数（7） */
    private int total;

    /** 修行进度百分比 */
    private int percent;

    /** 下一境界名（已至魔祖为 null） */
    private String next;

    /** 当前煞气修为 */
    private int exp;

    /** 冲击下一境所需煞气（已至魔祖为 0） */
    private int nextExp;

    /** 是否可冲击下一境 */
    private boolean canBreakthrough;

    /** 今日已参悟篇数 */
    private int exploreCount;

    /** 每日参悟上限 */
    private int exploreLimit;
}
