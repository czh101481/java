package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 秘境探索日志，对应表 demon_realm_log
 * 每次「进入秘境」结算后落一条，用于：每日次数统计、战报回看、秘境履历
 */
@Data
@TableName("demon_realm_log")
public class RealmLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 探索者 id */
    private Long userId;

    /** 所入秘境 id */
    private Long trialId;

    /** 秘境名（快照，秘境改名后战报仍可读） */
    private String title;

    /** 结果：treasure 机缘 / success 功成 / injured 负伤 */
    private String result;

    /** 事件描述（一句话战报） */
    private String detail;

    /** 本次所得煞气（已含机缘翻倍、已扣负伤，未扣入场消耗） */
    private Integer expGain;

    /** 入场消耗煞气 */
    private Integer cost;

    /** 负伤折损煞气 */
    private Integer injury;

    /** 机缘所得宝物名（无则为空串） */
    private String treasure;

    private LocalDateTime createdAt;
}
