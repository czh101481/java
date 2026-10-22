package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 魔枢典册（典籍库）实体，对应表 demon_archive
 * 四大分类：lineage 魔族谱系 / path 魔修道途 / law 魔域法则 / ancient 上古秘录
 */
@Data
@TableName("demon_archive")
public class Archive {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类键：lineage / path / law / ancient */
    private String category;

    /** 条目名 */
    private String title;

    /** 副题（血脉 / 境界跨度 / 铁律 / 上古纪年） */
    private String subtitle;

    /** 一句话提要 */
    private String summary;

    /** 正文（含换行，前端需 white-space: pre-line） */
    private String content;

    /** 同分类内排序，小者在前 */
    private Integer sortOrder;

    private LocalDateTime createdAt;
}
