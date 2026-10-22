package com.fenglin.springboottest.vo;

import lombok.Data;

/**
 * 魔门公告视图对象
 */
@Data
public class AnnouncementVO {
    private Long id;
    private String title;
    private String content;
    private String level;   // 通告 / 警示 / 喜讯 / 秘辛
    private String createdAt;
}
