package com.fenglin.springboottest.dto;

import lombok.Data;

/**
 * 修者切磋请求体：对手道号
 */
@Data
public class SparringDTO {

    /** 切磋对手道号 */
    private String targetName;
}
