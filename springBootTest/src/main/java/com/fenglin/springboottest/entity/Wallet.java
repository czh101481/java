package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 修者钱囊，对应表 demon_wallet（每人一行，首次访问自动开户）
 */
@Data
@TableName("demon_wallet")
public class Wallet {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 修者 id */
    private Long userId;

    /** 魔晶余额（日常通货） */
    private Integer coin;

    /** 血玉余额（稀有通货） */
    private Integer jade;

    /** 累计获得魔晶 */
    private Long earnedCoin;

    /** 累计消耗魔晶 */
    private Long spentCoin;

    /** 累计获得血玉 */
    private Long earnedJade;

    /** 累计消耗血玉 */
    private Long spentJade;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
