package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 入魔道 · 新手状态（demon_newbie），一人一行。
 *
 * started_at 是「新兵期」起点而非注册时间：存量账号首次访问时懒补发开局礼包，
 * 补发当刻才开始计 14 天，避免老号一进来就没有任何加成。
 */
@Data
@TableName("demon_newbie")
public class Newbie {

    /** 主键即 user_id，非自增 */
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    /** 开局礼包是否已发（1 = 已发） */
    private Integer starterDone;

    /** 新兵期起点（领礼包时间） */
    private LocalDateTime startedAt;

    /** 七日礼已领到第几日（0~7，第 1 日即开局礼包） */
    private Integer loginDay;

    /** 上次领取七日礼的日期，防同日重复 */
    private LocalDate lastClaimDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
