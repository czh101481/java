package com.fenglin.springboottest.common;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 魔契令牌管理（演示用，内存存储）
 * 说明：本实现为内存令牌表 token -> userId，服务重启即失效。
 *      生产环境请替换为 JWT（jjwt）等可校验、可过期、分布式的方案。
 */
@Component
public class TokenManager {

    private final Map<String, Long> tokenUserMap = new ConcurrentHashMap<>();

    /** 为某修者生成魔契令牌 */
    public String createToken(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenUserMap.put(token, userId);
        return token;
    }

    /** 凭令牌取修者 id，无效返回 null */
    public Long getUserId(String token) {
        return tokenUserMap.get(token);
    }

    /** 销毁令牌（断魔契） */
    public void remove(String token) {
        if (token != null) {
            tokenUserMap.remove(token);
        }
    }
}
