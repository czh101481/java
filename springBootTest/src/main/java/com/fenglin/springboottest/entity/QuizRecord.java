package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 论道答题记录，对应表 demon_quiz_record
 * 唯一键 (user_id, debate_id, quiz_date)：同一修者对同一论题每日一次，跨日可再辩；
 * 当日超额作答由 DailyDecay 递减收益，论道榜按 DISTINCT debate_id 统计精通题数。
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

    /** 作答日期（跨日重置，唯一键的一部分） */
    private LocalDate quizDate;

    /** 是否答对：1 是 / 0 否 */
    private Integer correct;

    /** 本次结算的煞气变动（可为负） */
    private Integer awarded;

    private LocalDateTime createdAt;
}
