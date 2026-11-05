package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.TokenManager;
import com.fenglin.springboottest.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 魔族修行 · 鬼市（玩家间交易）接口
 * 基底址：/api/trade
 *
 *   在售    GET  /api/trade/list          → 鬼市挂牌（匿名可浏览）
 *   我的    GET  /api/trade/mine          → 我挂的单（需持魔契）
 *   寄售    POST /api/trade/sell          {itemId, qty, price, currency}
 *   撤单    POST /api/trade/cancel        {tradeId}
 *   买入    POST /api/trade/buy           {tradeId}
 */
@RestController
@RequestMapping("/api/trade")
public class TradeController {

    @Autowired
    private TradeService tradeService;

    @Autowired
    private TokenManager tokenManager;

    @GetMapping("/list")
    public Result<?> list() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", tradeService.onSale());
        return Result.success(data);
    }

    @GetMapping("/mine")
    public Result<?> mine(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long uid = uid(authorization);
        if (uid == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", tradeService.mine(uid));
        return Result.success(data);
    }

    @PostMapping("/sell")
    public Result<?> sell(@RequestHeader(value = "Authorization", required = false) String authorization,
                          @RequestBody(required = false) Map<String, Object> body) {
        Long uid = uid(authorization);
        if (uid == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        try {
            return Result.success(tradeService.sell(uid,
                    longOf(body == null ? null : body.get("itemId")),
                    intOf(body == null ? null : body.get("qty"), 1),
                    intOf(body == null ? null : body.get("price"), 1),
                    body == null ? null : String.valueOf(body.get("currency"))));
        } catch (TradeService.Deny e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/cancel")
    public Result<?> cancel(@RequestHeader(value = "Authorization", required = false) String authorization,
                            @RequestBody(required = false) Map<String, Object> body) {
        Long uid = uid(authorization);
        if (uid == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        try {
            return Result.success(tradeService.cancel(uid, longOf(body == null ? null : body.get("tradeId"))));
        } catch (TradeService.Deny e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/buy")
    public Result<?> buy(@RequestHeader(value = "Authorization", required = false) String authorization,
                         @RequestBody(required = false) Map<String, Object> body) {
        Long uid = uid(authorization);
        if (uid == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        try {
            return Result.success(tradeService.buy(uid, longOf(body == null ? null : body.get("tradeId"))));
        } catch (TradeService.Deny e) {
            return Result.error(400, e.getMessage());
        }
    }

    // ==================== 工具 ====================

    private Long uid(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return tokenManager.getUserId(authorization.substring(7).trim());
    }

    private Long longOf(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int intOf(Object v, int def) {
        if (v == null) {
            return def;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
