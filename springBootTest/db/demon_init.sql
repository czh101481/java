-- =====================================================================
-- 魔族修行 · 数据库初始化脚本
-- 后端：springBootTest（Spring Boot 3 + MyBatis-Plus + MySQL 8）
-- 前端：魔族修行登录 Vue 项目（Vue 3 + Vite）
-- 库名：demon_db    账号：root / 123456    端口：3306
--
-- 执行方式（任选其一）：
--   1) 命令行：        mysql -uroot -p123456 < db/demon_init.sql
--   2) IDEA Terminal： mysql -uroot -p123456 < db/demon_init.sql
--   3) IDEA / Navicat / DataGrip 的 Database 工具中直接打开本文件运行
--
-- 说明：本脚本幂等，可重复执行，不会删除已有数据。
-- =====================================================================

-- 创建数据库（若不存在）
CREATE DATABASE IF NOT EXISTS demon_db
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

-- 使用数据库
USE demon_db;

-- ---------------------------------------------------------------------
-- 魔族修者表
--   · 字段与后端实体 com.fenglin.springboottest.entity.DemonUser 一一对应
--   · 密码字段为演示明文（与前端约定一致），生产环境务必加密存储
--   · uk_name / uk_email 保证道号、邮箱唯一，注册时后端会做查重
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS demon_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT            COMMENT '主键',
  name        VARCHAR(50)  NOT NULL                          COMMENT '修者道号',
  email       VARCHAR(100) NOT NULL                          COMMENT '灵识印记（邮箱）',
  realm       VARCHAR(20)  NOT NULL                          COMMENT '魔种：血煞/幽冥/噬魂/虚妄',
  password    VARCHAR(100) NOT NULL                          COMMENT '护法密令（演示为明文，生产请加密）',
  created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP          COMMENT '入籍时间',
  updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP
                          ON UPDATE CURRENT_TIMESTAMP        COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_name  (name),
  UNIQUE KEY uk_email (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '魔族修者表';

-- ---------------------------------------------------------------------
-- 可选：预置一个演示修者，方便不注册也能先试登录
--   道号：mo ZuShi（魔祖）   邮箱：demo@demon.com   密令：123456
-- ---------------------------------------------------------------------
INSERT IGNORE INTO demon_user (name, email, realm, password)
VALUES ('魔祖', 'demo@demon.com', '血煞', '123456');
