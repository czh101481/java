package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自定义头像图片本体，对应表 demon_avatar_file。
 *
 * 图片以 base64 存库，随 demon_db.sql 全量快照一起迁移；
 * demon_user.avatar 仍只存 /uploads/avatars/xxx.jpg 路径（与旧数据完全兼容）。
 * 磁盘上的同名文件降级为本机备份，取图时库优先、文件兜底。
 */
@Data
@TableName("demon_avatar_file")
public class AvatarFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 形如 /uploads/avatars/2_1793779685246.jpg，唯一 */
    private String filePath;

    /** image/jpeg / image/png ... */
    private String mime;

    /** 图片原始字节的 base64 编码 */
    private String imgBase64;

    private LocalDateTime createdAt;
}
