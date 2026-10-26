-- 魔族修行 · demon_user 升级脚本（修行体系：境界/煞气/每日参悟）
-- 幂等说明：列已存在会报 Duplicate column，可忽略
USE demon_db;

ALTER TABLE demon_user
  ADD COLUMN cultivation     VARCHAR(20) DEFAULT '引煞' COMMENT '修为境界：引煞/凝煞/魔卒/魔尉/魔将/血煞/魔祖' AFTER realm,
  ADD COLUMN exp             INT         DEFAULT 0      COMMENT '煞气修为' AFTER cultivation,
  ADD COLUMN exp_date        DATE        DEFAULT NULL   COMMENT '最后参悟日期（跨日清零）' AFTER exp,
  ADD COLUMN exp_count_today INT         DEFAULT 0      COMMENT '当日已参悟次数' AFTER exp_date;

-- 演示账号：引煞起步，煞气 80（够冲击凝煞境演示）
UPDATE demon_user SET cultivation='引煞', exp=80, exp_date=NULL, exp_count_today=0
WHERE email='demo@demon.com';
