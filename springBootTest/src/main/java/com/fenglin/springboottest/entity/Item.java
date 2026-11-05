package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 藏宝阁物品定义，对应表 demon_item
 *
 * effectType：exp=加煞气 / explore=今日参悟上限+N / coin=立即得魔晶
 */
@Data
@TableName("demon_item")
public class Item {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 物品代号（唯一） */
    private String code;

    private String name;

    /** 图标字（单字） */
    private String icon;

    /** 丹药 / 符箓 / 奇物 / 珍品 */
    private String category;

    /** 物品说明（desc 是关键字，列名为 desc_text） */
    private String descText;

    /** 魔晶价 */
    private Integer priceCoin;

    /** 血玉价 */
    private Integer priceJade;

    private String effectType;

    private Integer effectValue;

    /** 每日限购，0=不限 */
    private Integer dailyLimit;

    /** 1=在售 0=下架 */
    private Integer status;

    private Integer sort;

    private LocalDateTime createdAt;
}
