package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 功法阁实体，对应表 demon_art
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

    /** 所需境界 */
    private String realmReq;

    /** 简介 */
    private String summary;

    /** 正文 */
    private String content;

    private LocalDateTime createdAt;
}
