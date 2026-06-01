-- ----------------------------
-- 迁移脚本：考试助理与预约考试（plan 001）
-- 新增三张表 + sys_config 提前可见时长列 + assistant 角色种子
-- 适用：MySQL 5.7 / 8.x；在已部署的 yf-exam-lite 库上执行
-- ----------------------------

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 考试时间段 el_exam_time_slot
--    把开放方式 + 时间窗口下沉到段级（R1/R2/R4）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `el_exam_time_slot` (
  `id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'ID',
  `exam_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '考试ID',
  `open_type` int NOT NULL DEFAULT '1' COMMENT '开放类型：1不限人员 2指定部门 3预约考试',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `max_depart` int NOT NULL DEFAULT '0' COMMENT '可预约部门数上限（仅预约型有效）',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `exam_id` (`exam_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考试时间段';

-- ----------------------------
-- 2. 时间段-部门 el_exam_slot_depart
--    指定部门(可考) 与 预约型免约部门 共表，以 depart_type 区分（R4b/R4d）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `el_exam_slot_depart` (
  `id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'ID',
  `slot_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '时间段ID',
  `depart_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '部门ID',
  `depart_type` int NOT NULL DEFAULT '1' COMMENT '部门类型：1指定部门(可考) 2预约型免约部门',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `slot_id` (`slot_id`) USING BTREE,
  KEY `depart_id` (`depart_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考试时间段部门';

-- ----------------------------
-- 3. 预约记录 el_exam_booking
--    (exam_id, depart_id) 唯一：每部门每场考试只能预约一个时段（R7）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `el_exam_booking` (
  `id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'ID',
  `exam_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '考试ID',
  `slot_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '时间段ID',
  `depart_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '预约的部门ID',
  `user_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '预约人（考试助理）',
  `create_time` datetime DEFAULT NULL COMMENT '预约时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `exam_depart` (`exam_id`,`depart_id`) USING BTREE,
  KEY `slot_id` (`slot_id`) USING BTREE,
  KEY `depart_id` (`depart_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='考试预约记录';

-- ----------------------------
-- 4. 系统配置：提前可见时长（分钟）R4c
--    幂等：列不存在才添加
-- ----------------------------
SET @col_exists := (
  SELECT COUNT(1) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_config'
    AND COLUMN_NAME = 'advance_visible_minutes'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `sys_config` ADD COLUMN `advance_visible_minutes` int NULL DEFAULT 0 COMMENT ''考试提前可见时长(分钟)''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 5. 种子：考试助理角色
-- ----------------------------
INSERT INTO `sys_role` (`id`, `role_name`)
SELECT 'assistant', '考试助理'
WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `id` = 'assistant');

SET FOREIGN_KEY_CHECKS = 1;
