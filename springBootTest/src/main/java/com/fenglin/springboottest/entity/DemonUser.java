package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 魔族修者实体，对应表 demon_user
 */
@Data
@TableName("demon_user")
public class DemonUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 修者道号 */
    private String name;

    /** 灵识印记（邮箱） */
    private String email;

    /** 魔种：血煞 / 幽冥 / 噬魂 / 虚妄 */
    private String realm;

    /** 护法密令（演示为明文，生产请加密） */
    private String password;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
