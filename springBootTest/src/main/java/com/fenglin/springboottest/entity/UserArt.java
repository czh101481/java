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

    private LocalDateTime createdAt;
}
