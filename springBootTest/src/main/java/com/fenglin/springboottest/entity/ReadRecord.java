package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 修者研读记录，对应表 demon_read_record
 * 唯一键 (user_id, module, ref_id, read_date)：同一篇每日一次，跨日可再读；
 * 当日超额研读由 DailyDecay 递减收益，图鉴仍按 DISTINCT(module, ref_id) 统计。
 */
@Data
@TableName("demon_read_record")
public class ReadRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 研读修者 id */
    private Long userId;

    /** 所属模块：art / trial / debate / codex / archive */
    private String module;

    /** 条目 id（各内容表主键） */
    private Long refId;

    /** 研读日期（跨日重置，唯一键的一部分） */
    private LocalDate readDate;

    private LocalDateTime createdAt;
}
