package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 乾坤袋（背包），对应表 demon_bag（每人每物一行，数量累加）
 */
@Data
@TableName("demon_bag")
public class BagItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long itemId;

    private Integer qty;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
