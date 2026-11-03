package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.TokenManager;
import com.fenglin.springboottest.service.SocialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 魔族修行 · 社交接口（好友 + 传音）
 * 基址：/api/social
 * 全部接口需持魔契（Bearer 令牌），无效返回 401。
 */
@RestController
@RequestMapping("/api/social")
public class SocialController {

    @Autowired
    private SocialService socialService;

    @Autowired
    private TokenManager tokenManager;

    /** 由 Authorization 头解析当前修者 id（无效返回 null） */
    private Long uid(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return tokenManager.getUserId(authorization.substring(7).trim());
    }

    /** 递出道友契约 POST /api/social/friend/apply { toName, message } */
    @PostMapping("/friend/apply")
    public Result<?> apply(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        Long me = uid(authorization);
        if (me == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        String toName = body == null ? null : (String) body.get("toName");
        Object msg = body == null ? null : body.get("message");
        return socialService.apply(me, toName, msg == null ? null : String.valueOf(msg));
    }

    /** 待处理请求（呈予我） GET /api/social/friend/requests */
    @GetMapping("/friend/requests")
    public Result<?> requests(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long me = uid(authorization);
        if (me == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        return socialService.listRequests(me);
    }

    /** 处理请求 POST /api/social/friend/handle { applyId, action: accept|reject } */
    @PostMapping("/friend/handle")
    public Result<?> handle(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        Long me = uid(authorization);
        if (me == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        Object idObj = body == null ? null : body.get("applyId");
        Long applyId = idObj == null ? null : Long.valueOf(idObj.toString());
        Object act = body == null ? null : body.get("action");
        return socialService.handle(me, applyId, act == null ? null : String.valueOf(act));
    }

    /** 我的道友列表 GET /api/social/friend/list */
    @GetMapping("/friend/list")
    public Result<?> friends(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long me = uid(authorization);
        if (me == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        return socialService.listFriends(me);
    }

    /** 解除道友 DELETE /api/social/friend/{friendId} */
    @DeleteMapping("/friend/{friendId}")
    public Result<?> removeFriend(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long friendId) {
        Long me = uid(authorization);
        if (me == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        return socialService.removeFriend(me, friendId);
    }

    /** 搜索修者（按道号，排除自己） GET /api/social/user/search?kw= */
    @GetMapping("/user/search")
    public Result<?> search(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "kw", required = false, defaultValue = "") String kw) {
        Long me = uid(authorization);
        if (me == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        return socialService.searchUsers(me, kw);
    }

    /** 会话历史（增量 since + 自动标记已读） GET /api/social/message/history?friendId=&since= */
    @GetMapping("/message/history")
    public Result<?> history(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "friendId", required = false) Long friendId,
            @RequestParam(value = "since", required = false) Long since) {
        Long me = uid(authorization);
        if (me == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        return socialService.history(me, friendId, since);
    }

    /** 发送传音 POST /api/social/message/send { toId, content, msgType, refType, refId, refTitle, refSummary } */
    @PostMapping("/message/send")
    public Result<?> send(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        Long me = uid(authorization);
        if (me == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        Object toObj = body == null ? null : body.get("toId");
        Long toId = toObj == null ? null : Long.valueOf(toObj.toString());
        String content = body == null ? null : (String) body.get("content");
        Object typeObj = body == null ? null : body.get("msgType");
        Integer msgType = typeObj == null ? 0 : Integer.valueOf(typeObj.toString());
        String refType = body == null ? null : (String) body.get("refType");
        Object refIdObj = body == null ? null : body.get("refId");
        Long refId = refIdObj == null ? null : Long.valueOf(refIdObj.toString());
        String refTitle = body == null ? null : (String) body.get("refTitle");
        String refSummary = body == null ? null : (String) body.get("refSummary");
        return socialService.send(me, toId, content, msgType, refType, refId, refTitle, refSummary);
    }

    /** 未读统计 GET /api/social/message/unread */
    @GetMapping("/message/unread")
    public Result<?> unread(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long me = uid(authorization);
        if (me == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        return socialService.unread(me);
    }
}
