package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.dto.LoginDTO;
import com.fenglin.springboottest.dto.RegisterDTO;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.service.AuthService;
import com.fenglin.springboottest.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 魔族修行 · 认证接口
 * 基底址：/api/auth
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /** 入魔道（登录） POST /api/auth/login  body: { name, password } */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO dto) {
        return authService.login(dto);
    }

    /** 开辟魔渊（注册） POST /api/auth/register  body: { name, email, realm, password } */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterDTO dto) {
        return authService.register(dto);
    }

    /** 持令问询（当前修者） GET /api/auth/me  header: Authorization: Bearer <token> */
    @GetMapping("/me")
    public Result<DemonUser> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }
        return authService.me(token);
    }
}
