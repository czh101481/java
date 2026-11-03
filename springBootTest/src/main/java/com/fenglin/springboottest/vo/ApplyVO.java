package com.fenglin.springboottest.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友申请视图：呈予本人的待处理契约（含申请人信息）
 */
@Data
public class ApplyVO {

    /** demon_friend.id（处理时回传） */
    private Long applyId;

    /** 申请人修者 id */
    private Long fromId;

    /** 申请人道号 */
    private String name;

    /** 魔种 */
    private String realm;

    /** 境界 */
    private String cultivation;

    /** 留言 */
    private String message;

    private LocalDateTime createdAt;
}
