package com.fenglin.springboottest.dto;

import lombok.Data;

/**
 * 论道作答请求体：论题 id + 所选选项（A/B/C）
 */
@Data
public class QuizAnswerDTO {

    /** 论题 id */
    private Long id;

    /** 所选选项（A/B/C，大小写不敏感） */
    private String choice;
}
