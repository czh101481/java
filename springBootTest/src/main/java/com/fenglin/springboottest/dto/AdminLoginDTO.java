package com.fenglin.springboottest.dto;

import lombok.Data;

/**
 * 后台管理登录请求体：管理员账号 + 密码
 */
@Data
public class AdminLoginDTO {

    /** 管理员账号 */
    private String username;

    /** 管理员密码 */
    private String password;
}
