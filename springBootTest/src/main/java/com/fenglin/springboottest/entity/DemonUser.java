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

    /** 修为境界：引煞 / 凝煞 / 魔卒 / 魔尉 / 魔将 / 血煞 / 魔祖 */
    private String cultivation;

    /** 煞气修为：浏览魔域内容积累，攒足可冲击下一境 */
    private Integer exp;

    /** 最后参悟日期（每日参悟上限跨日清零用） */
    private java.time.LocalDate expDate;

    /** 当日已参悟次数 */
    private Integer expCountToday;

    /** 护法密令（演示为明文，生产请加密） */
    private String password;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
