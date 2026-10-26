package com.fenglin.springboottest.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 论道问答：论题信息 + 三个选项（不下发答案）（GET /api/home/quiz）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DebateQuizVO {

    /** 论题 id */
    private Long id;

    /** 论题 */
    private String title;

    /** 论者 */
    private String speaker;

    /** 摘要 */
    private String summary;

    /** 三个选项（A/B/C 顺序） */
    private List<String> options;
}
