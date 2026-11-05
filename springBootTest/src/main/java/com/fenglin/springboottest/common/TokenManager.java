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

    /**
     * 销毁某修者的其余令牌，保留当前这一条（改密令后让其他设备/浏览器掉线）。
     *
     * @return 实际销毁的令牌数量
     */
    public int removeOthersOfUser(Long userId, String keepToken) {
        if (userId == null) {
            return 0;
        }
        int removed = 0;
        for (Map.Entry<String, Long> e : tokenUserMap.entrySet()) {
            if (userId.equals(e.getValue()) && !e.getKey().equals(keepToken)) {
                if (tokenUserMap.remove(e.getKey()) != null) {
                    removed++;
                }
            }
        }
        return removed;
    }

    /** 当前有效令牌数（在线量观测用） */
    public int onlineCount() {
        return tokenUserMap.size();
    }
}
