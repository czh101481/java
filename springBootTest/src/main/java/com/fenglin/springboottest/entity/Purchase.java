package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 藏宝阁购买记录，对应表 demon_purchase（用于每日限购统计与消费审计）
 */
@Data
@TableName("demon_purchase")
public class Purchase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long itemId;

    private Integer qty;

    /** coin / jade */
    private String payCurrency;

    /** 总价 */
    private Integer payAmount;

    private LocalDate buyDate;

    private LocalDateTime createdAt;
}
