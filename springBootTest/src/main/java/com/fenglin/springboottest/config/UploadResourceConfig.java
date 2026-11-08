package com.fenglin.springboottest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 上传资源映射（历史配置，保留占位）。
 *
 * v2 起自定义头像图片本体已存入 demon_avatar_file，并由
 * {@link com.fenglin.springboottest.controller.AvatarFileController}
 * 的 GET /uploads/** 统一提供（库优先、磁盘兜底），因此这里
 * **不再注册 /uploads/** 静态资源映射**：否则同一路径同时存在
 * 静态资源 Handler 与 Controller，行为取决于 HandlerMapping 优先级，
 * 容易出现「换电脑有文件没库 / 有库没文件」表现不一致的问题。
 *
 * 磁盘 uploads/ 目录仍保留写入，仅作为本机备份，不参与对外取图路由。
 */
@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {
    // intentionally empty：取图逻辑全部在 AvatarFileController
}
