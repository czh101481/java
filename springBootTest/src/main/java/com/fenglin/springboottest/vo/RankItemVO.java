package com.fenglin.springboottest.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 魔门排行条目：排名 / 道号 / 境界 / 位阶 / 称号 / 煞气 / 是否本人（GET /api/home/rank）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankItemVO {

    /** 排名（1 起） */
    private int rank;

    /** 修者道号 */
    private String name;

    /** 头像：预设 key（blood/moon…）或上传图片路径（/uploads/avatars/xxx.png），空则用道号首字 */
    private String avatar;

    /** 境界名 */
    private String realmName;

    /** 境界位阶 */
    private int index;

    /** 境界称号 */
    private String title;

    /** 煞气修为 */
    private int exp;

    /** 是否为当前持契修者本人 */
    private boolean me;
}
