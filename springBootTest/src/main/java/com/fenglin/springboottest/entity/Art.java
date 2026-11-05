package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 功法阁实体，对应表 demon_art
 *
 * 分重体系：黄 1-4 重 · 玄 1-6 重 · 地 1-7 重 · 天 1-10 重 · 仙 1-10 重。
 * 同一 series（系列）内须按重递进习得；习得后按 bonus_* 提升修者数值。
 */
@Data
@TableName("demon_art")
public class Art {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 功法名 */
    private String name;

    /** 类别：攻伐/护体/幻术/炼魂/御兽 */
    private String category;

    /** 品阶：黄/玄/地/天/仙 */
    private String grade;

    /** 功法重数（不得超过品阶上限：黄4 玄6 地7 天10 仙10） */
    private Integer layer;

    /** 功法系列（同系列须按重递进购买；为空则互不相关） */
    private String series;

    /** 售价·魔晶（与 priceJade 同时为 0 表示免费） */
    private Integer priceCoin;

    /** 售价·血玉（>0 时以此计价，忽略魔晶） */
    private Integer priceJade;

    /** 1=在售 0=下架 */
    private Integer status;

    /** 攻伐加成 */
    private Integer bonusAtk;

    /** 护体加成 */
    private Integer bonusDef;

    /** 气血加成 */
    private Integer bonusHp;

    /** 悟性加成 */
    private Integer bonusInsight;

    /** 身法加成 */
    private Integer bonusAgility;

    /** 所需境界 */
    private String realmReq;

    /** 简介 */
    private String summary;

    /** 配图相对路径（/images/art/art-N.jpg，静态资源） */
    private String image;

    /** 正文 */
    private String content;

    private LocalDateTime createdAt;
}
