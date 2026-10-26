package com.fenglin.springboottest.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 境界阶梯节点：位阶 / 境界名 / 称号 / 描述
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RealmNodeVO {

    /** 位阶（0 = 引煞） */
    private int index;

    /** 境界名 */
    private String name;

    /** 境界称号 */
    private String title;

    /** 境界描述 */
    private String desc;
}
