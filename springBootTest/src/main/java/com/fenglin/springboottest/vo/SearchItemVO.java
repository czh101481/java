package com.fenglin.springboottest.vo;

import lombok.Data;

/**
 * 统一检索条目（跨 4 张内容表）
 */
@Data
public class SearchItemVO {

    /** 类型：art / trial / debate / codex */
    private String type;

    /** 类型名：功法阁 / 历练录 / 宗门论道 / 魔典秘录 */
    private String typeName;

    private Long id;

    /** 标题（功法名 / 历练名 / 论题 / 典籍名） */
    private String title;

    /** 简介 */
    private String summary;

    /** 附加信息：功法=品阶·境界；历练=地域·凶险；论道=论者；魔典=卷册 */
    private String extra;
}
