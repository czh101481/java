package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 魔种天赋配置，对应表 demon_origin
 * 四系魔种（血煞 / 幽冥 / 噬魂 / 虚妄）各自的成长偏向，由 Origins 装载后有玩法生效
 */
@Data
@TableName("demon_origin")
public class OriginConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 魔种名（与 demon_user.realm 对应） */
    private String origin;

    /** 天赋名 */
    private String title;

    /** 天赋描述 */
    private String description;

    /** 参悟煞气加成倍率，0.50 = +50% */
    private BigDecimal exploreBonus;

    /** 切磋战力加成倍率，0.12 = +12% */
    private BigDecimal sparPowerBonus;

    /** 切磋胜利额外奖励倍率，1.00 = 奖励翻倍 */
    private BigDecimal sparRewardBonus;

    /** 参悟顿悟（双倍）概率，0.25 = 25% */
    private BigDecimal insightChance;

    /** 展示顺序 */
    private Integer sort;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
