package com.fenglin.springboottest.dto;

import lombok.Data;

/**
 * 入魔道（登录）请求体：{ name, password }
 */
@Data
public class LoginDTO {
    private String name;
    private String password;
}
