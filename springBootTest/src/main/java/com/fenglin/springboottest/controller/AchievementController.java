package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.TokenManager;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.mapper.DemonUserMapper;
import com.fenglin.springboottest.service.AchievementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 魔族修行 · 道途成就 + 今日修行
 * 基地址：/api/achievement
 *
 *   成就墙 GET /api/achievement/list  → 全部成就进度、自动铭刻、分期汇总、下一目标、已获称号
 *   今日课 GET /api/achievement/daily → 七课完成度（焚香/参悟/论道/切磋/研读/秘境/洞府收取）
 *
 * 成就纯荣誉，不发任何资源；判定口径见 AchievementService 与 db/BALANCE.md 第八节。
 */
@RestController
@RequestMapping("/api/achievement")
public class AchievementController {

    @Autowired
    private AchievementService achievementService;

    @Autowired
    private DemonUserMapper userMapper;

    @Autowired
    private TokenManager tokenManager;

    @GetMapping("/list")
    public Result<?> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        return Result.success(achievementService.overview(user));
    }

    @GetMapping("/daily")
    public Result<?> daily(@RequestHeader(value = "Authorization", required = false) String authorization) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        return Result.success(achievementService.daily(user));
    }

    private DemonUser currentUser(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        Long uid = tokenManager.getUserId(authorization.substring(7).trim());
        return uid == null ? null : userMapper.selectById(uid);
    }
}
