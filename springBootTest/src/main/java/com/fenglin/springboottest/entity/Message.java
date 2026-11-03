package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 传音私信
 * msg_type: 0=文字 1=修炼心得卡片（ref_type/ref_id/ref_title/ref_summary 携带被分享的功法/秘录/典籍）
 */
@Data
@TableName("demon_message")
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fromId;

    private Long toId;

    private String content;

    /** 0=文字 1=修炼心得卡片 */
    private Integer msgType;

    /** 心得卡片关联内容类型：art/codex/archive */
    private String refType;

    /** 心得卡片关联内容 id */
    private Long refId;

    /** 心得卡片标题 */
    private String refTitle;

    /** 心得卡片摘要 */
    private String refSummary;

    /** 0=未读 1=已读（read 是 MySQL 保留字，需反引号转义） */
    @TableField("`read`")
    private Integer read;

    private LocalDateTime createdAt;
}
