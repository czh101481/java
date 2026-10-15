package com.fenglin.springboottest.vo;

import lombok.Data;

import java.util.List;

/**
 * 跨表检索结果
 */
@Data
public class SearchResultVO {

    /** 检索关键字 */
    private String keyword;

    /** 命中总数 */
    private long total;

    /** 按类型分组的结果（仅含非空分组） */
    private List<SearchGroupVO> groups;
}
