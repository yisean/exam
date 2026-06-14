-- ----------------------------
-- 迁移脚本：简答题与人工阅卷（plan 005）
-- 对应设计 docs/engineering/design/2026-06-14-005-feat-subjective-question-manual-grading-design.md 的「数据 ER 模型」。
-- 题型编码（el_qu.qu_type / el_paper_qu.qu_type）：1单选 2多选 3判断 4简答 5不定项 6综合题(父)
-- 适用：MySQL 5.7 / 8.x（utf8mb4，varchar(N) 按字符）；在已部署的 yf-exam-lite 库上执行。
-- 安全性：以「列不存在才添加」方式幂等执行，可重复运行；仅加 1 列，不改存量结构、不动数据。
--         采用与 migration-2026-综合题与不定项.sql 一致的 information_schema + PREPARE 写法，
--         不使用 DELIMITER/存储过程，兼容 JDBC、迁移工具等单语句执行环境。
-- 回滚：见文末 DOWN 注释（默认不执行）。
--
-- 重要前置说明（经核验，本特性无需添加）：
--   基线库 数据库脚本.sql 中以下列已存在，简答与阅卷直接复用，本脚本不重复添加：
--     el_exam_repo.saq_count / saq_score        （组卷：简答数量 / 每题分值）
--     el_paper.obj_score / subj_score / user_score / has_saq （客观分 / 主观分 / 总分 / 是否含简答）
--     el_paper_qu.answer / actual_score          （考生作答 / 实得分）
--   客观分汇总口径 sumObjective=qu_type IN(1,2,3,5)、主观分 sumSubjective=qu_type=4 已在 Mapper 写好，
--   无需 DDL（详见设计 §详细设计）。
-- ----------------------------

SET NAMES utf8mb4;

-- ========== UP（正向） ==========

-- el_paper_qu 新增「阅卷点评」列（考生可见）；本特性唯一新增列。
-- 长度规约：comment 最大 200 字符（utf8mb4 按字符），前端 maxlength=200、后端按字符校验，三处对齐。
SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='el_paper_qu' AND COLUMN_NAME='comment');
SET @s := IF(@c=0,
  'ALTER TABLE `el_paper_qu` ADD COLUMN `comment` varchar(200) NULL DEFAULT NULL COMMENT ''阅卷点评（简答题，考生可见）''',
  'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ========== DOWN（回滚） ==========
-- 撤销上面的 UP；DROP COLUMN 会丢失已录入的阅卷点评文本，生产回滚前请先备份 el_paper_qu。
-- 结构可回滚，点评数据不可恢复。
-- ALTER TABLE `el_paper_qu` DROP COLUMN `comment`;
