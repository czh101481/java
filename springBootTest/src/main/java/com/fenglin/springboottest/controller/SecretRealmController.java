package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.TokenManager;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.mapper.DemonUserMapper;
import com.fenglin.springboottest.service.SecretRealmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 魔族修行 · 域外秘境接口
 * 基底址：/api/home/secret
 *
 * 秘境是副本而非典籍：可进入、可结算、有次数与产出。
 *   列表   GET  /api/home/secret/list        → 各秘境门槛与今日剩余（匿名亦可浏览，但不可入）
 *   进入   POST /api/home/secret/enter/{id}  → 结算一次探索（需持魔契）
 *   战报   GET  /api/home/secret/log         → 本人秘境履历（需持魔契）
 */
@RestController
@RequestMapping("/api/home/secret")
public class SecretRealmController {

    @Autowired
    private SecretRealmService secretRealmService;

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private DemonUserMapper userMapper;

    /** 秘境一览：含开启境界 / 凶险 / 入场消耗 / 今日剩余 / 可否进入 */
    @GetMapping("/list")
    public Result<?> list(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        DemonUser user = currentUser(authorization);
        List<Map<String, Object>> list = secretRealmService.list(user == null ? guest() : user);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("logged", user != null);
        return Result.success(data);
    }

    /** 进入秘境：结算一次探索，返回战报 */
    @org.springframework.web.bind.annotation.PostMapping("/enter/{id}")
    public Result<?> enter(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        try {
            return Result.success(secretRealmService.enter(user, id));
        } catch (SecretRealmService.Deny e) {
            return Result.error(400, e.getMessage());
        }
    }

    /** 我的秘境履历 */
    @GetMapping("/log")
    public Result<?> log(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("list", secretRealmService.log(user.getId(), limit));
        return Result.success(data);
    }

    /** 未登录时的空壳修者：仅用于渲染列表（境界引煞、煞气 0，故一切秘境皆不可入） */
    private DemonUser guest() {
        DemonUser u = new DemonUser();
        u.setId(0L);
        u.setExp(0);
        return u;
    }

    /** 由 Authorization 头解析当前修者（无效返回 null） */
    private DemonUser currentUser(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        Long userId = tokenManager.getUserId(authorization.substring(7).trim());
        return userId == null ? null : userMapper.selectById(userId);
    }
}
