package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.common.TokenManager;
import com.fenglin.springboottest.service.CoinService;
import com.fenglin.springboottest.service.ShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 魔族修行 · 货币与藏宝阁接口
 * 基底址：/api/coin
 *
 *   钱囊      GET  /api/coin/wallet        → 余额 + 今日收支 + 累计（需持魔契）
 *   流水      GET  /api/coin/logs?limit=30 → 收支明细（需持魔契）
 *   藏宝阁    GET  /api/coin/shop          → 在售物品（匿名可浏览，登录后带限购与持有）
 *   购买      POST /api/coin/buy           {itemId, qty}（需持魔契）
 *   乾坤袋    GET  /api/coin/bag           → 背包（需持魔契）
 *   使用      POST /api/coin/use           {itemId, qty}（需持魔契）
 */
@RestController
@RequestMapping("/api/coin")
public class CoinController {

    @Autowired
    private CoinService coinService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private TokenManager tokenManager;

    /** 钱囊总览 */
    @GetMapping("/wallet")
    public Result<?> wallet(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long uid = uid(authorization);
        if (uid == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        return Result.success(coinService.overview(uid));
    }

    /** 收支流水 */
    @GetMapping("/logs")
    public Result<?> logs(@RequestHeader(value = "Authorization", required = false) String authorization,
                          @RequestParam(value = "limit", defaultValue = "30") int limit) {
        Long uid = uid(authorization);
        if (uid == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("list", coinService.logs(uid, limit));
        return Result.success(data);
    }

    /** 藏宝阁（匿名可看） */
    @GetMapping("/shop")
    public Result<?> shop(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long uid = uid(authorization);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", shopService.shop(uid));
        data.put("logged", uid != null);
        return Result.success(data);
    }

    /** 购买 */
    @PostMapping("/buy")
    public Result<?> buy(@RequestHeader(value = "Authorization", required = false) String authorization,
                         @RequestBody(required = false) Map<String, Object> body) {
        Long uid = uid(authorization);
        if (uid == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        Long itemId = longOf(body == null ? null : body.get("itemId"));
        int qty = intOf(body == null ? null : body.get("qty"), 1);
        try {
            return Result.success(shopService.buy(uid, itemId, qty));
        } catch (ShopService.Deny e) {
            return Result.error(400, e.getMessage());
        }
    }

    /** 乾坤袋 */
    @GetMapping("/bag")
    public Result<?> bag(@RequestHeader(value = "Authorization", required = false) String authorization) {
        Long uid = uid(authorization);
        if (uid == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", shopService.bag(uid));
        return Result.success(data);
    }

    /** 使用道具 */
    @PostMapping("/use")
    public Result<?> use(@RequestHeader(value = "Authorization", required = false) String authorization,
                         @RequestBody(required = false) Map<String, Object> body) {
        Long uid = uid(authorization);
        if (uid == null) {
            return Result.error(401, "未持魔契，请先入魔道。");
        }
        Long itemId = longOf(body == null ? null : body.get("itemId"));
        int qty = intOf(body == null ? null : body.get("qty"), 1);
        try {
            return Result.success(shopService.use(uid, itemId, qty));
        } catch (ShopService.Deny e) {
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
