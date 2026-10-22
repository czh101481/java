package com.fenglin.springboottest.dto;

import lombok.Data;

/**
 * 开辟魔渊（注册）请求体：{ name, email, realm, password }
 */
@Data
public class RegisterDTO {
    private String name;
    private String email;
    private String realm;
    private String password;
}
