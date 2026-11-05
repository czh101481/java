package com.fenglin.springboottest.dto;

import lombok.Data;

/**
 * 改密令请求体：{ oldPassword, newPassword }
 * 供 POST /api/auth/password 使用。
 */
@Data
public class ChangePasswordDTO {
    private String oldPassword;
    private String newPassword;
}
