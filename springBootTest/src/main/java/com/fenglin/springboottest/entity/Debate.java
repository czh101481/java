package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 宗门论道实体，对应表 demon_debate
 */
@Data
@TableName("demon_debate")
public class Debate {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 论题 */
    private String title;

    /** 论者 */
    private String speaker;

    /** 摘要 */
    private String summary;

    /** 选项 A */
    private String optionA;

    /** 选项 B */
    private String optionB;

    /** 选项 C */
    private String optionC;

    /** 正确选项 A/B/C（不下发前端） */
    private String answer;

    /** 论道解析 */
    private String analysis;

    private LocalDateTime createdAt;
}
