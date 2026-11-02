package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秘境实体，对应表 demon_trial
 *
 * 语义：此处并非典籍词条，而是一处<strong>可进入、可结算、有产出与凶险</strong>的域外秘境。
 * 每个秘境自带开启境界、入场消耗、每日可入次数、产出区间与机缘/凶险概率。
 */
@Data
@TableName("demon_trial")
public class Trial {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 秘境名 */
    private String title;

    /** 所在域（如：北境魔原） */
    private String region;

    /** 开启所需境界 */
    private String realmReq;

    /** 凶险：低/中/高/绝 */
    private String difficulty;

    /** 守关妖魔（可为空） */
    private String boss;

    /** 入场消耗煞气 */
    private Integer costExp;

    /** 每日可入次数 */
    private Integer dailyLimit;

    /** 产出煞气下限 */
    private Integer rewardMin;

    /** 产出煞气上限 */
    private Integer rewardMax;

    /** 凶险概率（百分比整数，20 = 20% 负伤） */
    private Integer riskRate;

    /** 机缘概率（百分比整数，15 = 15% 得宝，所得翻倍） */
    private Integer treasureRate;

    /** 简介 */
    private String summary;

    /** 配图相对路径（/images/trial/trial-N.jpg，静态资源） */
    private String image;

    private LocalDateTime createdAt;
}
