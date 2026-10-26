package com.fenglin.springboottest.vo;

import lombok.Data;

import java.util.List;

/**
 * 境界阶梯全量：七境节点 + 各功能模块门槛（GET /api/home/realms）
 */
@Data
public class RealmLadderVO {

    /** 七境阶梯（升序） */
    private List<RealmNodeVO> ladder;

    /** 各功能模块所需境界（按门槛升序） */
    private List<RealmFeatureVO> features;
}
