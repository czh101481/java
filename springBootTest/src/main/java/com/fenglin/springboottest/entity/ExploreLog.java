package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 参悟日志，对应表 demon_explore_log
 * 每次「参悟此篇 / 随机参悟」成功后落一条，用于：
 *   1. 每篇内容每日仅可参悟一次（防重复刷同一篇）
 *   2. 每日参悟总上限（与 Realm.DAILY_EXPLORE_LIMIT 一致）
 *   3. 回看今日参悟了哪些篇章
 */
@Data
@TableName("demon_explore_log")
public class ExploreLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 参悟者 id */
    private Long userId;

    /** 内容类型：art 功法阁 / codex 魔典秘录 / archive 魔枢典册 */
    private String contentType;

    /** 内容 id */
    private Long contentId;

    /** 内容标题快照（内容改名后日志仍可读） */
    private String title;

    /** 本次所得煞气 */
    private Integer gain;

    private LocalDateTime createdAt;
}
