package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 魔友关系（含好友申请与已成好友，单表双向存储）
 * 约定：status 0=申请中 1=好友 2=已拒绝/已删除
 * 唯一键 (user_id, friend_id) 保证任一方向上最多一条关系记录
 */
@Data
@TableName("demon_friend")
public class Friend {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发起人（申请方） */
    private Long userId;

    /** 对方 */
    private Long friendId;

    /** 0=申请中 1=好友 2=已拒绝/已删除 */
    private Integer status;

    /** 申请留言 */
    private String message;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime handledAt;
}
