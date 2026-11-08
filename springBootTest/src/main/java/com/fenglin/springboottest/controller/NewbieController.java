package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.TokenManager;
import com.fenglin.springboottest.entity.DemonUser;
import com.fenglin.springboottest.mapper.DemonUserMapper;
import com.fenglin.springboottest.service.NewbieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 魔族修行 · 入魔道（新手期）
 * 基地址：/api/newbie
 *
 *   GET  /api/newbie/state → 开局礼包状态、七日礼进度、新兵期剩余天数与倍率
 *   POST /api/newbie/claim → 领取下一日之礼（同日不可重复）
 *
 * 注意：state 对「尚无新手记录的存量账号」会懒补发开局礼包并开启 14 天新兵期，
 * 这是唯一有副作用的读接口，故玩法结算点一律调用 NewbieService 的纯读方法。
 * 数值口径见 common/NewbieRule.java 与 db/BALANCE.md 第九节。
 */
@RestController
@RequestMapping("/api/newbie")
public class NewbieController {

    @Autowired
    private NewbieService newbieService;

    @Autowired
    private DemonUserMapper userMapper;

    @Autowired
    private TokenManager tokenManager;

    @GetMapping("/state")
    public Result<?> state(@RequestHeader(value = "Authorization", required = false) String authorization) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        return Result.success(newbieService.state(user.getId()));
    }

    @PostMapping("/claim")
    public Result<?> claim(@RequestHeader(value = "Authorization", required = false) String authorization) {
        DemonUser user = currentUser(authorization);
        if (user == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        Map<String, Object> r = newbieService.claimDaily(user.getId());
        if (!Boolean.TRUE.equals(r.get("ok"))) {
            return Result.error(400, String.valueOf(r.get("msg")));
        }
        return Result.success(r);
    }

    private DemonUser currentUser(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        Long uid = tokenManager.getUserId(authorization.substring(7).trim());
        return uid == null ? null : userMapper.selectById(uid);
    }
}
