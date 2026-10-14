-- =============================================================================
-- 艺览 ciTY · 回滚脚本（把结构还原到变更之前）
-- =============================================================================
-- 用途：执行 02_alter_existing.sql 之后如果发现设计要改，用本脚本还原结构。
--
-- ⚠️ 适用范围：**只回滚 02_alter_existing.sql 的变更**。
--    05_order_favorite_tables.sql（票务 4 张表）与 07_admin_tables.sql（后台字段）
--    没有对应的回滚脚本 —— 它们都是「只增不减」的变更，需要回滚时请手工处理，
--    或直接重新执行 01_create_tables.sql 重建库（注意会清空数据）。
--
-- ⚠️ 本脚本【不可重复执行】，且会删除 exhibition_work 表内的全部数据。
-- ⚠️ 回滚会丢失以下列的数据：banner.update_time、gallery.open_time/is_hot、
--    exhibition.images/open_time。执行前请先备份。
--
-- 备份建议：mysqldump -uroot -p city_art > city_art_backup_$(date +%Y%m%d).sql
-- =============================================================================

USE `city_art`;


-- =============================================================================
-- 1. 删除新增的 exhibition_work 表
-- =============================================================================
DROP TABLE IF EXISTS `exhibition_work`;


-- =============================================================================
-- 2. exhibition：还原 subtitle，删除 images、open_time
-- =============================================================================
ALTER TABLE `exhibition`
    DROP COLUMN `images`,
    DROP COLUMN `open_time`,
    DROP KEY `idx_hot`,
    ADD COLUMN `subtitle` VARCHAR(255) DEFAULT NULL COMMENT '副标题/英文标题' AFTER `title`;


-- =============================================================================
-- 3. gallery：还原 district、type，删除 open_time、is_hot
-- =============================================================================
ALTER TABLE `gallery`
    DROP COLUMN `open_time`,
    DROP COLUMN `is_hot`,
    DROP KEY `idx_hot`,
    ADD COLUMN `district` VARCHAR(50) DEFAULT NULL COMMENT '所在区/县' AFTER `city`,
    ADD COLUMN `type` INT NOT NULL DEFAULT 1 COMMENT '美术馆类型：1-综合 2-当代 3-古典 4-雕塑 5-摄影' AFTER `status`;


-- =============================================================================
-- 4. banner：删除 update_time
-- =============================================================================
ALTER TABLE `banner`
    DROP COLUMN `update_time`;
