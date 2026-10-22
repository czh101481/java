package com.fenglin.springboottest.vo;

import lombok.Data;

/**
 * 魔枢典册 · 分类卡片（资料库入口）
 */
@Data
public class ArchiveCategoryVO {

    /** 分类键：lineage / path / law / ancient */
    private String key;

    /** 分类名：魔族谱系 / 魔修道途 / 魔域法则 / 上古秘录 */
    private String name;

    /** 一句话说明 */
    private String desc;

    /** 图标字符 */
    private String icon;

    /** 封面图相对路径（复用魔典秘录配图） */
    private String cover;

    /** 该分类条目数 */
    private Long count;

    /** 量词：脉 / 篇 / 条 / 卷 */
    private String unit;
}
