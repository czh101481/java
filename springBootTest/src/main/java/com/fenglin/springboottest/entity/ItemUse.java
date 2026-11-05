package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 道具每日使用量，对应表 demon_item_use
 * 唯一键 (user_id, item_id, use_date)：一人一物一天一行，跨日自动另起一行。
 * 与「每日限购」(demon_purchase) 相互独立：限购管「买」，本表管「用」。
 */
@Data
@TableName("demon_item_use")
public class ItemUse {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long itemId;

    private LocalDate useDate;

    /** 当日已取用件数 */
    private Integer qty;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
