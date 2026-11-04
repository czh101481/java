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

    /** 修为境界：引煞 / 凝煞 / 魔卒 / 魔尉 / 魔将 / 血煞 / 魔尊 / 魔祖 / 魔圣 / 无上 */
    private String cultivation;

    /** 煞气修为：浏览魔域内容积累，攒足可冲击下一境 */
    private Integer exp;

    /** 最后参悟日期（每日参悟上限跨日清零用） */
    private java.time.LocalDate expDate;

    /** 当日已参悟次数 */
    private Integer expCountToday;

    /** 最后切磋日期（每日切磋上限跨日清零用） */
    private java.time.LocalDate sparDate;

    /** 当日已切磋次数 */
    private Integer sparCountToday;

    /** 最后论道日期（每日答题上限跨日清零用） */
    private java.time.LocalDate quizDate;

    /** 当日已答论题数 */
    private Integer quizCountToday;

    // ==================== v2 玩法字段 ====================

    /** 最后签到日期 */
    private java.time.LocalDate signDate;

    /** 连续签到天数（断签归 1） */
    private Integer signStreak;

    /** 累计签到天数 */
    private Integer signTotal;

    /** 切磋胜场 */
    private Integer sparWin;

    /** 切磋负场 */
    private Integer sparLose;

    /** 当前连续渡劫失败次数（达上限后必成，成功即清零） */
    private Integer breakFail;

    /** 转生次数（第几世） */
    private Integer reincarnate;

    /** 最后研读日期（每日研读上限跨日清零用） */
    private java.time.LocalDate readDate;

    /** 当日已研读篇数 */
    private Integer readCountToday;

    /** 护法密令（演示为明文，生产请加密） */
    private String password;

    /** 权限：1 默认权限 / 0 管理员权限（仅 0 可见后台入口并可登录后台） */
    private Integer permission;

    /** 头像代号：前端预设头像的 key（如 blood/ghost/…），为空则回退道号首字 */
    private String avatar;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
