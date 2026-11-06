package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 道途成就定义，对应表 demon_achievement（后台可 CRUD）
 *
 * 成就只给称号 / 徽记 / 道途分期，不发任何资源，
 * 故不影响 db/BALANCE.md 的每日产出预算。
 */
@Data
@TableName("demon_achievement")
public class Achievement {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 成就代号（唯一） */
    private String code;

    private String name;

    /** 徽记单字 */
    private String icon;

    /** 分类：道途 / 修行 / 日课 / 论道 / 图鉴 / 洞府 / 财富 / 交游 / 终极 */
    private String category;

    private String subtitle;

    private String description;

    /** 判定类型，取值见 AchievementService.COND_TYPES */
    private String condType;

    /** 判定阈值 */
    private Integer condValue;

    /** 纯荣誉称号，不发资源 */
    private String titleReward;

    /** 道途分期：初入 / 登堂 / 小成 / 大成 / 圆满 / 无上 */
    private String stage;

    private Integer sortOrder;

    /** 1=启用 0=停用 */
    private Integer status;

    private LocalDateTime createdAt;
}
