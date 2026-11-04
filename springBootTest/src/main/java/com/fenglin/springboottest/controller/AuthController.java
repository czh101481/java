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

    /** 更换头像 POST /api/auth/avatar  body: { avatar }  header: Authorization: Bearer <token> */
    @PostMapping("/avatar")
    public Result<DemonUser> updateAvatar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody java.util.Map<String, Object> body) {
        String token = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }
        Object av = body == null ? null : body.get("avatar");
        return authService.updateAvatar(token, av == null ? null : String.valueOf(av));
    }

    /** 上传自定义头像 POST /api/auth/avatar/upload  multipart: file  header: Authorization: Bearer <token> */
    @PostMapping("/avatar/upload")
    public Result<DemonUser> uploadAvatar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String token = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
        }
        return authService.uploadAvatar(token, file);
    }
}
