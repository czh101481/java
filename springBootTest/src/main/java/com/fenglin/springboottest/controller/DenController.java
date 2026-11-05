package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.TokenManager;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.mapper.DemonUserMapper;
import com.fenglin.springboottest.service.DenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 魔族修行 · 魔窟（洞府挂机产出）
 * 基地址：/api/den
 *
 *   状态  GET  /api/den            → 三设施等级 / 每时产量 / 待收取 / 升级费用
 *   收取  POST /api/den/collect    → 结算累积（煞气入修为，魔晶/血玉入钱囊，12h 封顶）
 *   扩建  POST /api/den/upgrade    → ?type=sha|coin|jade 设施升级（扣魔晶或血玉）
 */
@RestController
@RequestMapping("/api/den")
public class DenController {

    @Autowired
    private DenService denService;

    @Autowired
    private DemonUserMapper userMapper;

    @Autowired
    private TokenManager tokenManager;

    @GetMapping
    public Result<?> state(@RequestHeader(value = "Authorization", required = false) String authorization) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        return Result.success(denService.state(user));
    }

    @PostMapping("/collect")
    public Result<?> collect(@RequestHeader(value = "Authorization", required = false) String authorization) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        java.util.Map<String, Object> data = denService.collect(user);
        if (Boolean.FALSE.equals(data.get("ok"))) {
            return Result.error(400, String.valueOf(data.get("msg")));
        }
        return Result.success(data);
    }

    @PostMapping("/upgrade")
    public Result<?> upgrade(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("type") String type) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        java.util.Map<String, Object> data = denService.upgrade(user, type);
        if (Boolean.FALSE.equals(data.get("ok"))) {
            return Result.error(400, String.valueOf(data.get("msg")));
        }
        return Result.success(data);
    }

    private DemonUser currentUser(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        Long uid = tokenManager.getUserId(authorization.substring(7).trim());
        return uid == null ? null : userMapper.selectById(uid);
    }
}
