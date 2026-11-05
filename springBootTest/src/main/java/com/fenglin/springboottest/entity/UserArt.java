package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 修者已习得功法，对应表 demon_user_art
 * 唯一键 (user_id, art_id)：同一部功法不可重复习得
 */
@Data
@TableName("demon_user_art")
public class UserArt {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 修者 id */
    private Long userId;

    /** 功法 id（demon_art.id） */
    private Long artId;

    /** 形态：0=完整功法 1=残片 */
    private Integer form;

    /** 修炼进度：1 入门 / 2 小成 / 3 大成 / 4 圆满 / 5 入道 / 6 无上 */
    private Integer stage;

    /** 本重熟练度（满则进境） */
    private Integer prof;

    /** 最后修炼日期（每日课数跨日清零） */
    private java.time.LocalDate trainDate;

    /** 当日已修炼课数 */
    private Integer trainCount;

    private LocalDateTime createdAt;
}
