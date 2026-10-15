package com.fenglin.springboottest.vo;

import com.fenglin.springboottest.entity.DemonUser;
import lombok.Data;

/**
 * 登录成功返回：魔契令牌 + 修者信息
 */
@Data
public class LoginVO {
    private String token;
    private DemonUser user;

    public LoginVO(String token, DemonUser user) {
        this.token = token;
        this.user = user;
    }
}
