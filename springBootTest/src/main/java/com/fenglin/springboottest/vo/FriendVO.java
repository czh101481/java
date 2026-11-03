package com.fenglin.springboottest.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 好友视图：对方修者信息 + 最近一条消息预览 + 未读数
 */
@Data
public class FriendVO {

    /** 好友（对方）修者 id */
    private Long userId;

    /** 道号 */
    private String name;

    /** 魔种（血煞/幽冥/噬魂/虚妄） */
    private String realm;

    /** 境界（引煞→无上） */
    private String cultivation;

    /** 最近一条消息预览（心得卡片显示标题） */
    private String lastMessage;

    /** 最近一条消息类型 */
    private Integer lastMsgType;

    /** 最近一条消息 id（前端增量轮询用） */
    private Long lastMsgId;

    /** 来自该好友的未读消息数 */
    private Integer unread;

    /** 最近一条消息时间 */
    private LocalDateTime lastTime;
}
