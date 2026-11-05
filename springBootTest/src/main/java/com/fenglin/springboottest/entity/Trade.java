package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 鬼市挂单，对应表 demon_trade
 *
 * status：0=在售 1=已售 2=已撤
 * 上架即从卖家乾坤袋预扣，撤单原物退回，成交则入买家袋。
 */
@Data
@TableName("demon_trade")
public class Trade {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sellerId;

    private Long itemId;

    private Integer qty;

    /** 单价（按 currency 计价） */
    private Integer price;

    /** coin=魔晶 / jade=血玉 */
    private String currency;

    /** 0=在售 1=已售 2=已撤 */
    private Integer status;

    private Long buyerId;

    private LocalDateTime createdAt;

    private LocalDateTime soldAt;
}
