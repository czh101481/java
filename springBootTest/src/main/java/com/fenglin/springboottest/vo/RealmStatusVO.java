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

    /** 总境数（10） */
    private int total;

    /** 修行进度百分比 */
    private int percent;

    /** 下一境界名（已至无上为 null） */
    private String next;

    /** 当前煞气修为 */
    private int exp;

    /** 冲击下一境所需煞气（已至无上为 0） */
    private int nextExp;

    /** 是否可冲击下一境 */
    private boolean canBreakthrough;

    /** 今日已参悟篇数 */
    private int exploreCount;

    /** 每日参悟上限 */
    private int exploreLimit;

    /** 今日已切磋场数 */
    private int sparCount;

    /** 每日切磋上限 */
    private int sparLimit;

    // ==================== v2 玩法状态 ====================

    /** 今日是否已签到 */
    private boolean signSigned;

    /** 连续签到天数 */
    private int signStreak;

    /** 累计签到天数 */
    private int signTotal;

    /** 今日签到可领煞气（已签为 0） */
    private int signReward;

    /** 切磋胜场 */
    private int sparWin;

    /** 切磋负场 */
    private int sparLose;

    /** 转生次数（第几世） */
    private int reincarnate;

    /** 今日已研读篇数 */
    private int readCount;

    /** 每日研读上限 */
    private int readLimit;

    /** 图鉴收集度百分比 */
    private int collectionPercent;
}
