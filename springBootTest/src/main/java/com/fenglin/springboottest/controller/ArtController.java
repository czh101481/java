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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 魔族修行 · 功法阁（完整 / 残片 · 六重进度）
 * 基底址：/api/art
 *
 *   购得  POST /api/art/buy/{artId}?form=0|1   → 0 完整 / 1 残片（按阶位定价扣款）
 *   补全  POST /api/art/complete/{artId}       → 残片付差价转完整
 *   修炼  POST /api/art/cultivate/{artId}      → 每日 3 课，耗煞气涨熟练
 *   顿悟  POST /api/art/insight/{artId}        → 圆满 +「悟道机缘」→ 入道
 *   破境  POST /api/art/transcend/{artId}      → 入道 +「死战悟道」→ 无上
 *   已习  GET  /api/art/mine                   → 我的功法与进度
 */
@RestController
@RequestMapping("/api/art")
public class ArtController {

    @Autowired
    private ArtService artService;

    @Autowired
    private TokenManager tokenManager;

    /** 购得功法（form 缺省为完整） */
    @PostMapping("/buy/{artId}")
    public Result<?> buy(@RequestHeader(value = "Authorization", required = false) String authorization,
                         @PathVariable Long artId,
                         @RequestParam(value = "form", required = false, defaultValue = "0") int form) {
        Long uid = uid(authorization);
        if (uid == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        try {
            return Result.success(artService.buy(uid, artId, form));
        } catch (ArtService.Deny e) {
            return Result.error(400, e.getMessage());
        }
    }

    /** 残片补全为完整功法 */
    @PostMapping("/complete/{artId}")
    public Result<?> complete(@RequestHeader(value = "Authorization", required = false) String authorization,
                              @PathVariable Long artId) {
        return run(authorization, () -> artService.complete(uid(authorization), artId));
    }

    /** 修炼一课 */
    @PostMapping("/cultivate/{artId}")
    public Result<?> cultivate(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable Long artId) {
        return run(authorization, () -> artService.cultivate(uid(authorization), artId));
    }

    /** 圆满 → 入道 */
    @PostMapping("/insight/{artId}")
    public Result<?> insight(@RequestHeader(value = "Authorization", required = false) String authorization,
                             @PathVariable Long artId) {
        return run(authorization, () -> artService.insight(uid(authorization), artId));
    }

    /** 入道 → 无上 */
    @PostMapping("/transcend/{artId}")
    public Result<?> transcend(@RequestHeader(value = "Authorization", required = false) String authorization,
                               @PathVariable Long artId) {
        return run(authorization, () -> artService.transcend(uid(authorization), artId));
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

    // ==================== 工具 ====================

    private interface Action {
        Map<String, Object> run();
    }

    private Result<?> run(String authorization, Action action) {
        if (uid(authorization) == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        try {
            return Result.success(action.run());
        } catch (ArtService.Deny e) {
            return Result.error(400, e.getMessage());
        }
    }

    private Long uid(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return tokenManager.getUserId(authorization.substring(7).trim());
    }
}
