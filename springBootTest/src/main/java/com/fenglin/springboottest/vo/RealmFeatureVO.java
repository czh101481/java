package com.fenglin.springboottest.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 功能模块境界门槛：模块 / 展示名 / 所需位阶 / 所需境界名
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RealmFeatureVO {

    /** 模块 type（art / trial / debate / codex / archive） */
    private String type;

    /** 模块展示名（功法阁 / 历练录 / 宗门论道 / 魔典秘录 / 魔枢典册） */
    private String name;

    /** 所需最低境界位阶 */
    private int requirement;

    /** 所需境界名 */
    private String realmName;
}
