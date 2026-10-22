package com.fenglin.springboottest.vo;

import lombok.Data;

import java.util.List;

/**
 * 检索结果分组（按内容类型）
 */
@Data
public class SearchGroupVO {

    private String type;

    private String typeName;

    private long total;

    private List<SearchItemVO> items;
}
