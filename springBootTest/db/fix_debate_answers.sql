-- ============================================================
-- 魔族修行 · 论道答案分布校正
-- 问题：20 道题中 17 道答案为 B，玩家无脑选 B 即可稳赚，题目形同虚设。
-- 做法：交换正确项与干扰项的位置，使 A/B/C 分布趋于均衡（约 7/7/6），
--       选项语义与解析均不变，只调整「正确项落在哪个字母」。
-- 幂等：脚本以 answer 当前值为前提，重复执行会再次交换 —— 仅执行一次。
-- ============================================================
USE demon_db;

-- id 9 煞气何来：B → A（交换 A/B）
SET @a := (SELECT option_a FROM demon_debate WHERE id = 9);
SET @b := (SELECT option_b FROM demon_debate WHERE id = 9);
UPDATE demon_debate SET option_a = @b, option_b = @a, answer = 'A' WHERE id = 9;

-- id 10 境界压制之虚实：B → C（交换 B/C）
SET @b := (SELECT option_b FROM demon_debate WHERE id = 10);
SET @c := (SELECT option_c FROM demon_debate WHERE id = 10);
UPDATE demon_debate SET option_b = @c, option_c = @b, answer = 'C' WHERE id = 10;

-- id 11 四系魔种之辨：B → A
SET @a := (SELECT option_a FROM demon_debate WHERE id = 11);
SET @b := (SELECT option_b FROM demon_debate WHERE id = 11);
UPDATE demon_debate SET option_a = @b, option_b = @a, answer = 'A' WHERE id = 11;

-- id 13 切磋是否夺人道行：B → A
SET @a := (SELECT option_a FROM demon_debate WHERE id = 13);
SET @b := (SELECT option_b FROM demon_debate WHERE id = 13);
UPDATE demon_debate SET option_a = @b, option_b = @a, answer = 'A' WHERE id = 13;

-- id 14 境界与功法：B → C
SET @b := (SELECT option_b FROM demon_debate WHERE id = 14);
SET @c := (SELECT option_c FROM demon_debate WHERE id = 14);
UPDATE demon_debate SET option_b = @c, option_c = @b, answer = 'C' WHERE id = 14;

-- id 15 历练之所求：C → A（交换 A/C）
SET @a := (SELECT option_a FROM demon_debate WHERE id = 15);
SET @c := (SELECT option_c FROM demon_debate WHERE id = 15);
UPDATE demon_debate SET option_a = @c, option_c = @a, answer = 'A' WHERE id = 15;

-- id 16 魔籍为何留名：B → A
SET @a := (SELECT option_a FROM demon_debate WHERE id = 16);
SET @b := (SELECT option_b FROM demon_debate WHERE id = 16);
UPDATE demon_debate SET option_a = @b, option_b = @a, answer = 'A' WHERE id = 16;

-- id 18 无上何解：B → A
SET @a := (SELECT option_a FROM demon_debate WHERE id = 18);
SET @b := (SELECT option_b FROM demon_debate WHERE id = 18);
UPDATE demon_debate SET option_a = @b, option_b = @a, answer = 'A' WHERE id = 18;

-- id 20 道号与真我：保持 A 不变

-- 校验：答案分布
SELECT answer, COUNT(*) AS 题数 FROM demon_debate GROUP BY answer ORDER BY answer;
