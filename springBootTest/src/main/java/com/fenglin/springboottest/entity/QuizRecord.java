package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论道答题记录，对应表 demon_quiz_record
 * 唯一键 (user_id, debate_id)：同一修者对同一论题只能作答一次，防止反复刷煞气
 */
@Data
@TableName("demon_quiz_record")
public class QuizRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作答修者 id */
    private Long userId;

    /** 论题 id（demon_debate.id） */
    private Long debateId;

    /** 是否答对：1 是 / 0 否 */
    private Integer correct;

    /** 本次结算的煞气变动（可为负） */
    private Integer awarded;

    private LocalDateTime createdAt;
}
