package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 历练录实体，对应表 demon_trial
 */
@Data
@TableName("demon_trial")
public class Trial {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 历练名 */
    private String title;

    /** 地域 */
    private String region;

    /** 所需境界 */
    private String realmReq;

    /** 凶险：低/中/高/绝 */
    private String difficulty;

    /** 简介 */
    private String summary;

    private LocalDateTime createdAt;
}
