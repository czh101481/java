package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 修行境界配置，对应表 demon_realm
 * 启动时由 RealmConfigLoader 加载并覆盖 Realm 内置十境
 */
@Data
@TableName("demon_realm")
public class RealmConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 位阶（0 起，0 = 引煞，依次递增） */
    private Integer idx;

    /** 境界名（写入 demon_user.cultivation） */
    private String name;

    /** 境界称号 */
    private String title;

    /** 境界描述 */
    private String description;

    /** 冲击本位阶所需煞气 */
    private Integer threshold;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
