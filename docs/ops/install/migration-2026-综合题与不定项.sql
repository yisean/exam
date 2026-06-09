-- ----------------------------
-- 迁移脚本：综合题与不定项题型（plan 004）
-- 复用 el_qu / el_qu_answer 的父子关系承载综合题，无新表；
-- el_qu 增父子/排序/子题分值列；el_exam_repo 增不定项与综合题组卷列；
-- el_paper_qu 增父题列以在试卷内分组综合题子题。
-- 题型编码（el_qu.qu_type / el_paper_qu.qu_type）：1单选 2多选 3判断 4主观 5不定项 6综合题(父)
-- 适用：MySQL 5.7 / 8.x；在已部署的 yf-exam-lite 库上执行
-- 安全性：全部以「列/索引不存在才添加」方式幂等执行，可重复运行；仅加列不改存量表结构。
--         采用与 migration-2026-预约考试.sql 一致的 information_schema + PREPARE 写法，
--         不使用 DELIMITER/存储过程，兼容 JDBC、迁移工具等单语句执行环境。
-- 回滚：见文末 ROLLBACK 注释（默认不执行）。
-- ----------------------------

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. el_qu：综合题父子结构 + 子题分值（R3/R4/R5/R6）
--    parent_id 为空＝顶层题；综合题子题 parent_id 指向综合题(qu_type=6)，子题 qu_type ∈ {1,2,3,5}
-- ----------------------------
SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='el_qu' AND COLUMN_NAME='parent_id');
SET @s := IF(@c=0, 'ALTER TABLE `el_qu` ADD COLUMN `parent_id` varchar(64) NULL DEFAULT NULL COMMENT ''父题ID（综合题子题指向综合题父题；普通题/综合题父题为空）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='el_qu' AND COLUMN_NAME='sort');
SET @s := IF(@c=0, 'ALTER TABLE `el_qu` ADD COLUMN `sort` int NOT NULL DEFAULT 0 COMMENT ''同一父题下子题排序''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='el_qu' AND COLUMN_NAME='score');
SET @s := IF(@c=0, 'ALTER TABLE `el_qu` ADD COLUMN `score` int NULL DEFAULT 0 COMMENT ''子题分值（仅综合题子题使用；普通题分值由组卷配置决定）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- parent_id 查询索引（按父题取5个子题）
SET @c := (SELECT COUNT(1) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='el_qu' AND INDEX_NAME='idx_qu_parent');
SET @s := IF(@c=0, 'ALTER TABLE `el_qu` ADD KEY `idx_qu_parent` (`parent_id`)', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 2. el_exam_repo：组卷新增不定项（数量/分值）与综合题（数量）（R8）
--    综合题分值随题（=5个子题分值之和），故不设 composite_score 列。
-- ----------------------------
SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='el_exam_repo' AND COLUMN_NAME='uncertain_count');
SET @s := IF(@c=0, 'ALTER TABLE `el_exam_repo` ADD COLUMN `uncertain_count` int NOT NULL DEFAULT 0 COMMENT ''不定项题数量''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='el_exam_repo' AND COLUMN_NAME='uncertain_score');
SET @s := IF(@c=0, 'ALTER TABLE `el_exam_repo` ADD COLUMN `uncertain_score` int NOT NULL DEFAULT 0 COMMENT ''不定项题每题分值''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='el_exam_repo' AND COLUMN_NAME='composite_count');
SET @s := IF(@c=0, 'ALTER TABLE `el_exam_repo` ADD COLUMN `composite_count` int NOT NULL DEFAULT 0 COMMENT ''综合题数量（整题抽取，分值随题）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 3. el_paper_qu：试卷内综合题父子分组（R9/R11/R13）
--    综合题子题的 parent_id 指向同卷综合题父题的 el_paper_qu.id；普通题为空。
-- ----------------------------
SET @c := (SELECT COUNT(1) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='el_paper_qu' AND COLUMN_NAME='parent_id');
SET @s := IF(@c=0, 'ALTER TABLE `el_paper_qu` ADD COLUMN `parent_id` varchar(64) NULL DEFAULT NULL COMMENT ''父题在本试卷中的 el_paper_qu.id（综合题子题用；普通题为空）''', 'SELECT 1');
PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 4. 兼容性数据修正（解决部署窗口问题）★必须执行★
--    旧逻辑下 el_paper_qu.actual_score 在建卷时即被置为「满分」，判分正确性仅靠 is_right 过滤；
--    新逻辑改为 sumObjective 直接对 actual_score 求和（不再看 is_right），故升级时仍「进行中」(state=0)
--    的在考试卷，其客观题 actual_score 仍是满分，交卷会被全部计满分。
--    这里把进行中试卷的客观题(1单选/2多选/3判断)的 actual_score 按已判定的 is_right 回填为实得分，
--    与新口径一致；不定项(5)、综合题(6)为本次新增，旧卷不存在，无需处理。
--    幂等：重复执行结果一致。已交卷(state<>0)的历史成绩已由旧 sumObjective 算定，不在此调整。
-- ----------------------------
UPDATE `el_paper_qu` pq
JOIN `el_paper` p ON pq.paper_id = p.id
SET pq.actual_score = (CASE WHEN pq.is_right = 1 THEN pq.score ELSE 0 END)
WHERE p.state = 0
  AND pq.qu_type IN (1, 2, 3);

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- 说明：题型编码无需 DDL（qu_type 已为 int）；不定项=5、综合题父=6 为约定值。
-- 客观分汇总口径（PaperQuMapper.sumObjective）由代码改为 qu_type IN (1,2,3,5)，不在本脚本内。
-- ----------------------------

-- ----------------------------
-- ROLLBACK（如需回滚，手动执行；会丢失综合题/不定项相关数据）：
-- ALTER TABLE `el_qu`        DROP COLUMN `parent_id`, DROP COLUMN `sort`, DROP COLUMN `score`, DROP KEY `idx_qu_parent`;
-- ALTER TABLE `el_exam_repo` DROP COLUMN `uncertain_count`, DROP COLUMN `uncertain_score`, DROP COLUMN `composite_count`;
-- ALTER TABLE `el_paper_qu`  DROP COLUMN `parent_id`;
-- ----------------------------
