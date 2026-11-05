package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 魔窟（洞府挂机产出），对应表 demon_den（每人一行，首次访问自动开辟）
 */
@Data
@TableName("demon_den")
public class Den {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 修者 id */
    private Long userId;

    /** 聚煞阵等级（0 = 未开辟） */
    private Integer shaLevel;

    /** 魔晶矿等级 */
    private Integer coinLevel;

    /** 血玉髓脉等级 */
    private Integer jadeLevel;

    /** 血玉千分位进位（1 血玉 = 1000，避免低级时取整归零） */
    private Integer jadeCarry;

    /** 上次收取时间（累积起点） */
    private LocalDateTime lastCollect;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
