package com.fenglin.springboottest.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fenglin.springboottest.entity.AvatarFile;
import com.fenglin.springboottest.mapper.AvatarFileMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 自定义头像取图：GET /uploads/avatars/xxx.jpg
 *
 * 图片本体存在 demon_avatar_file（随 demon_db.sql 快照迁移），本接口
 * **库优先、磁盘兜底**：换电脑后只要导入快照，即使 uploads/ 目录为空，
 * 英杰榜 / 道牌 / 传音里的头像也能正常显示。
 *
 * 前端把图片地址改成同源相对路径（不再拼 localhost:8081），
 * 因此从别的电脑访问同一套服务时也不会指向访问者自己的机器。
 */
@RestController
public class AvatarFileController {

    /** 只允许 /uploads/avatars/ 下的单级文件名，杜绝路径穿越 */
    private static final Pattern SAFE_PATH =
            Pattern.compile("^/uploads/avatars/[A-Za-z0-9_.-]+$");

    @Autowired
    private AvatarFileMapper avatarFileMapper;

    @GetMapping("/uploads/**")
    public ResponseEntity<byte[]> serve(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        String path = (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx))
                ? uri.substring(ctx.length()) : uri;
        // 去掉可能的 URL 参数（getRequestURI 不含 query，这里只做保险）
        int q = path.indexOf('?');
        if (q >= 0) {
            path = path.substring(0, q);
        }
        if (!SAFE_PATH.matcher(path).matches()) {
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = null;
        String mime = guessMime(path);

        // ① 库（权威副本）
        try {
            AvatarFile row = avatarFileMapper.selectOne(
                    new LambdaQueryWrapper<AvatarFile>()
                            .eq(AvatarFile::getFilePath, path)
                            .last("LIMIT 1"));
            if (row != null && row.getImgBase64() != null) {
                bytes = Base64.getDecoder().decode(row.getImgBase64());
                if (row.getMime() != null && row.getMime().startsWith("image/")) {
                    mime = row.getMime();
                }
            }
        } catch (Exception ignored) {
            /* 库里这张坏了就退回磁盘 */
        }

        // ② 磁盘（本机备份，兼容尚未迁移的历史图片）
        if (bytes == null || bytes.length == 0) {
            try {
                Path base = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
                Path file = base.resolve(path.replaceFirst("^/+", "")).normalize();
                if (file.startsWith(base.resolve("uploads")) && Files.isRegularFile(file)) {
                    bytes = Files.readAllBytes(file);
                }
            } catch (IOException ignored) {
                /* 文件不存在即可 */
            }
        }

        if (bytes == null || bytes.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mime))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .body(bytes);
    }

    private static String guessMime(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".bmp")) {
            return "image/bmp";
        }
        return "image/jpeg";
    }
}
