package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.TokenManager;
import com.fenglin.springboottest.service.ArtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 魔族修行 · 功法阁（分重 · 货币购得）
 * 基底址：/api/art
 *
 *   购得  POST /api/art/buy/{artId} → 校验重数/境界/前置重后扣魔晶或血玉并入库（需持魔契）
 *   已习  GET  /api/art/mine        → 我所习得的功法与数值（需持魔契）
 */
@RestController
@RequestMapping("/api/art")
public class ArtController {

    @Autowired
    private ArtService artService;

    @Autowired
    private TokenManager tokenManager;

    /** 购得功法 */
    @PostMapping("/buy/{artId}")
    public Result<?> buy(@RequestHeader(value = "Authorization", required = false) String authorization,
                         @PathVariable Long artId) {
        Long uid = uid(authorization);
        if (uid == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        try {
            return Result.success(artService.buy(uid, artId));
        } catch (ArtService.Deny e) {
            return Result.error(400, e.getMessage());
        }
    }

    /** 我已习得的功法 */
    @GetMapping("/mine")
    public Result<?> mine(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long uid = uid(authorization);
        if (uid == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", artService.mine(uid));
        return Result.success(data);
    }

    private Long uid(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return tokenManager.getUserId(authorization.substring(7).trim());
    }
}
