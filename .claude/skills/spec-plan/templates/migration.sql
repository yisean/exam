-- migration-<YYYY>-<特性名>.sql
-- 落点：docs/ops/install/migration-<YYYY>-<特性名>.sql
-- 对应设计文档（docs/engineering/design/…-NNN-*）的「数据 ER 模型」，与之逐字段一致。
-- 安全性：<能否重复执行幂等 / 加字段是否阻塞大表 / 索引是否在线创建>

-- ========== UP（正向） ==========

-- 示例：新增预约名额表（与 ER 的 EXAM_BOOKING 对应）
-- 时间戳规约：业务表带 create_time + update_time（命名沿用项目惯例）
-- 字段长度规约：业务最大字符数为唯一真值，与设计标注、后端校验、前端 maxLength 一致。
--             下例为 MySQL（utf8mb4 按字符，varchar(N)=N 字符）；非 MySQL 按目标库声明语义
--             （SQL Server nvarchar、Oracle varchar2(N CHAR)、达梦 LENGTH_IN_CHAR…见 constitution #9）。
--             生成 SQL 前若项目未记录用哪种库，先问用户确认。
CREATE TABLE IF NOT EXISTS exam_booking (
  id           BIGINT       PRIMARY KEY AUTO_INCREMENT,
  slot_id      BIGINT       NOT NULL,
  depart_id    BIGINT       NOT NULL,
  booker       VARCHAR(32)  COMMENT '预约人姓名，最多 32 字符',
  create_time  DATETIME     NOT NULL,
  update_time  DATETIME     NULL,
  UNIQUE KEY uk_slot_depart (slot_id, depart_id)   -- 防同部门重复占名额
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 示例：给已有表加字段
-- ALTER TABLE exam_time_slot ADD COLUMN open_type INT NOT NULL DEFAULT 0;

-- ========== DOWN（回滚） ==========
-- 规约：up 建/改了什么，down 逆序撤什么（先建后删、注意外键依赖顺序）。
-- 务必注明会丢什么数据、是否需先备份——结构能回滚，数据不一定能。
-- 无 schema 变更（纯数据/逻辑改动）时写：无。

-- 示例（撤销上面的 up；DROP 会连数据一起删，生产回滚前先备份 exam_booking）
DROP TABLE IF EXISTS exam_booking;
-- ALTER TABLE exam_time_slot DROP COLUMN open_type;
