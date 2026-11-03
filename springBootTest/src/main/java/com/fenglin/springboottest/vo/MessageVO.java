package com.fenglin.springboottest.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 私信视图：消息本体 + 是否本人发出标记
 */
@Data
public class MessageVO {

    private Long id;
    private Long fromId;
    private Long toId;
    private String content;
    private Integer msgType;
    private String refType;
    private Long refId;
    private String refTitle;
    private String refSummary;
    private Integer read;
    private LocalDateTime createdAt;

    /** 是否本人发出（前端区分左右气泡） */
    private boolean mine;
}
