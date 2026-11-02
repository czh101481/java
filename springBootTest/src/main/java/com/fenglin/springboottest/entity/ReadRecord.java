package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 修者研读记录，对应表 demon_read_record
 * 唯一键 (user_id, module, ref_id)：同一篇典籍首次研读才给煞气，并据此统计图鉴收集度
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

    private LocalDateTime createdAt;
}
