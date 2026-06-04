-- ----------------------------
-- 迁移脚本：综合题与不定项题型（plan 004）
-- 复用 el_qu / el_qu_answer 的父子关系承载综合题，无新表；
-- el_qu 增父子/排序/子题分值列；el_exam_repo 增不定项与综合题组卷列；
-- el_paper_qu 增父题列以在试卷内分组综合题子题。
-- 题型编码（el_qu.qu_type / el_paper_qu.qu_type）：1单选 2多选 3判断 4主观 5不定项 6综合题(父)
-- 适用：MySQL 5.7 / 8.x；在已部署的 yf-exam-lite 库上执行
-- 安全性：全部以「列不存在才添加」方式幂等执行，可重复运行；仅加列、不改存量数据。
-- 回滚：见文末 ROLLBACK 注释（默认不执行）。
-- ----------------------------

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 通用：幂等加列存储过程（执行后删除，避免污染）
DROP PROCEDURE IF EXISTS `pa_add_col`;
DELIMITER $$
CREATE PROCEDURE `pa_add_col`(IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl_tail VARCHAR(512))
BEGIN
  IF (SELECT COUNT(1) FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col) = 0 THEN
    SET @ddl := CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `', col, '` ', ddl_tail);
    PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
  END IF;
END$$
DELIMITER ;

-- ----------------------------
-- 1. el_qu：综合题父子结构 + 子题分值（R3/R4/R5/R6）
--    parent_id 为空＝顶层题；综合题子题 parent_id 指向综合题(qu_type=6)，子题 qu_type ∈ {1,2,3,5}
-- ----------------------------
CALL pa_add_col('el_qu', 'parent_id', "varchar(64) NULL DEFAULT NULL COMMENT '父题ID（综合题子题指向综合题父题；普通题/综合题父题为空）'");
CALL pa_add_col('el_qu', 'sort',      "int NOT NULL DEFAULT 0 COMMENT '同一父题下子题排序'");
CALL pa_add_col('el_qu', 'score',     "int NULL DEFAULT 0 COMMENT '子题分值（仅综合题子题使用；普通题分值由组卷配置决定）'");

-- parent_id 查询索引（按父题取 5 个子题）
SET @idx_exists := (SELECT COUNT(1) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'el_qu' AND INDEX_NAME = 'idx_qu_parent');
SET @ddl := IF(@idx_exists = 0, 'ALTER TABLE `el_qu` ADD KEY `idx_qu_parent` (`parent_id`)', 'SELECT 1');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ----------------------------
-- 2. el_exam_repo：组卷新增不定项（数量/分值）与综合题（数量）（R8）
--    综合题分值随题（=5 个子题分值之和），故不设 composite_score 列。
-- ----------------------------
CALL pa_add_col('el_exam_repo', 'uncertain_count', "int NOT NULL DEFAULT 0 COMMENT '不定项题数量'");
CALL pa_add_col('el_exam_repo', 'uncertain_score', "int NOT NULL DEFAULT 0 COMMENT '不定项题每题分值'");
CALL pa_add_col('el_exam_repo', 'composite_count', "int NOT NULL DEFAULT 0 COMMENT '综合题数量（整题抽取，分值随题）'");

-- ----------------------------
-- 3. el_paper_qu：试卷内综合题父子分组（R9/R11/R13）
--    综合题子题的 parent_id 指向同卷综合题父题的 el_paper_qu.id；普通题为空。
-- ----------------------------
CALL pa_add_col('el_paper_qu', 'parent_id', "varchar(64) NULL DEFAULT NULL COMMENT '父题在本试卷中的 el_paper_qu.id（综合题子题用；普通题为空）'");

DROP PROCEDURE IF EXISTS `pa_add_col`;
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
