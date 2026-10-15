-- =====================================================================
-- 龙渊认证 · 建库建表脚本
-- ---------------------------------------------------------------------
-- 后端：D:\workSpace\springBootTest   （Spring Boot 3 + MyBatis-Plus + MySQL 8）
-- 前端：dragon-register-vue           （Vue 3 + Vite）
-- 库名：dragon_auth   账号：root / 123456   端口：3306
--
-- 执行方式（任选其一）：
--   1) 命令行（推荐）：
--      "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" --default-character-set=utf8mb4 -uroot -p123456 < db\init.sql
--   2) IDEA 自带的 Terminal：mysql -uroot -p123456 < db/init.sql
--   3) Navicat / DataGrip / IDEA Database 工具里打开本文件直接运行
--
-- 特点：本脚本可重复执行（幂等），不会删除已有数据。
--       若确需清库重建，把下面 CREATE TABLE 换成 DROP TABLE IF EXISTS + CREATE TABLE 即可。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `dragon_auth`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `dragon_auth`;

-- ---------------------------------------------------------------------
-- 用户表
--   · 密码只以「盐 + PBKDF2WithHmacSHA256 摘要」形式存储，绝不存明文
--   · uk_username / uk_email 用的 utf8mb4_general_ci 排序规则天然忽略大小写，
--     与后端「用户名 / 邮箱查重忽略大小写」的规则一致
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tb_user`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`      VARCHAR(20)  NOT NULL COMMENT '名号（3-20 字符）',
    `email`         VARCHAR(100) NOT NULL COMMENT '邮箱',
    `password_hash` VARCHAR(128) NOT NULL COMMENT 'PBKDF2WithHmacSHA256 摘要，hex 64 位',
    `salt`          VARCHAR(64)  NOT NULL COMMENT '随机盐，hex 32 位',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='龙渊用户表';

-- ---------------------------------------------------------------------
-- 演示账号（可选，方便注册前先试登录）
--   名号：longyuan      邮箱：demo@dragon.com      秘钥：12345678
--   salt / password_hash 由 PBKDF2WithHmacSHA256（120000 轮 / 16 字节盐 / 256 位摘要）算出，
--   与后端 PasswordUtil、旧版 backend/RegisterServer.java 算法完全一致。
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `tb_user` (`username`, `email`, `password_hash`, `salt`)
VALUES ('longyuan',
        'demo@dragon.com',
        'a880084d2b43b32f7d5c964c62eb687b2513fcfd1051d73244f10ae3ead99f8c',
        '9f2c7a1e4b8d3506c1a2e7f04b9d6c83');

-- ---------------------------------------------------------------------
-- 执行结果自检
-- ---------------------------------------------------------------------
SELECT COUNT(*) AS 用户总数 FROM `tb_user`;
SELECT `id`, `username`, `email`, `created_at` FROM `tb_user` ORDER BY `id`;
