package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 魔典秘录实体，对应表 demon_codex
 */
@Data
@TableName("demon_codex")
public class Codex {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 典籍名 */
    private String title;

    /** 卷册 */
    private String volume;

    /** 摘要 */
    private String summary;

    private LocalDateTime createdAt;
}
