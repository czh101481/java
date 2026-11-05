package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 货币流水，对应表 demon_coin_log（正数入账、负数支出，每笔留痕）
 */
@Data
@TableName("demon_coin_log")
public class CoinLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** coin=魔晶 / jade=血玉 */
    private String currency;

    /** 变动值：正为入账，负为支出 */
    private Integer amount;

    /** 变动后余额（快照） */
    private Integer balance;

    /** 来源/去向代码：sign/explore/quiz/spar/secret/buy/item */
    private String reason;

    /** 人话说明 */
    private String reasonText;

    private String refType;

    private Long refId;

    private LocalDateTime createdAt;
}
