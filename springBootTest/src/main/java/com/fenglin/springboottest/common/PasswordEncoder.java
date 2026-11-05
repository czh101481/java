package com.fenglin.springboottest.common;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 魔族修行 · 密令编码器
 *
 * 新写入一律 PBKDF2WithHmacSHA256 + 随机盐，格式：
 *   pbkdf2$&lt;iterations&gt;$&lt;saltBase64&gt;$&lt;hashBase64&gt;
 * 单串约 83 字符，可存于 demon_user.password varchar(100)。
 *
 * 兼容历史数据：种子账号仍是 6 位明文。isEncoded 判前缀，matches 对未编码串走明文比对，
 * AuthService.verifyPassword 在登录命中后自动把明文升级为哈希，存量用户无感。
 */
public final class PasswordEncoder {

    private static final String PREFIX = "pbkdf2$";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordEncoder() {
    }

    /** 是否已是本编码器产出的哈希串 */
    public static boolean isEncoded(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            return false;
        }
        String[] parts = stored.split("\\$", -1);
        return parts.length == 4 && !parts[1].isEmpty() && !parts[2].isEmpty() && !parts[3].isEmpty();
    }

    /** 生成新哈希（每次盐不同，同一密令两次结果也不同） */
    public static String encode(String raw) {
        if (raw == null) {
            return null;
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(raw, salt, ITERATIONS);
        return PREFIX + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    /** 校验密令：哈希串走 PBKDF2，历史明文走常量时间比对 */
    public static boolean matches(String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        if (!isEncoded(stored)) {
            return MessageDigest.isEqual(
                    raw.getBytes(StandardCharsets.UTF_8),
                    stored.getBytes(StandardCharsets.UTF_8));
        }
        try {
            String[] parts = stored.split("\\$", -1);
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            return MessageDigest.isEqual(derive(raw, salt, iterations), expected);
        } catch (RuntimeException e) {
            // 哈希串损坏时退回明文比对，避免账号彻底锁死
            return MessageDigest.isEqual(
                    raw.getBytes(StandardCharsets.UTF_8),
                    stored.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static byte[] derive(String raw, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(raw.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("密令编码失败：" + ALGORITHM, e);
        } finally {
            spec.clearPassword();
        }
    }
}
