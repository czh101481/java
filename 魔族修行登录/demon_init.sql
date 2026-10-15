-- ============================================================
-- 魔族修行 · 数据库初始化脚本
-- 说明：本地 MySQL，用户名 root，密码 123456
-- 使用方式：在 MySQL 客户端（命令行 / IDEA / Navicat）执行本文件，
--          或执行：mysql -uroot -p123456 < demon_init.sql
-- ============================================================

-- 创建数据库（若不存在）
CREATE DATABASE IF NOT EXISTS demon_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

-- 使用数据库
USE demon_db;

-- 魔族修者表
CREATE TABLE IF NOT EXISTS demon_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  name        VARCHAR(50)  NOT NULL                COMMENT '修者道号',
  email       VARCHAR(100) NOT NULL                COMMENT '灵识印记（邮箱）',
  realm       VARCHAR(20)  NOT NULL                COMMENT '魔种：血煞/幽冥/噬魂/虚妄',
  password    VARCHAR(100) NOT NULL                COMMENT '护法密令（演示为明文，生产请加密）',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP                COMMENT '入籍时间',
  updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_name  (name),
  UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='魔族修者表';
