package com.fenglin.springboottest.vo;

import lombok.Data;

import java.util.List;

/**
 * 首页概览：各内容模块的数量与精选条目
 */
@Data
public class OverviewVO {

    private long artCount;

    private long trialCount;

    private long debateCount;

    private long codexCount;

    /** 功法阁精选（最新 3 条） */
    private List<SearchItemVO> artFeatured;

    /** 历练录精选 */
    private List<SearchItemVO> trialFeatured;

    /** 宗门论道精选 */
    private List<SearchItemVO> debateFeatured;

    /** 魔典秘录精选 */
    private List<SearchItemVO> codexFeatured;
}
