-- =============================================================================
-- 艺览 ciTY · 增量变更脚本（把现有库调整为本项目最终设计）
-- =============================================================================
-- 适用：已有 city_art 库，且已按旧文档建过 banner / gallery / exhibition。
-- 特性：**可重复执行**（内部用存储过程判断列是否存在，安全幂等）。
-- 行为：只增删列，不删表、不动数据。
--
-- 设计约定回顾：
--   · 展期状态由 start_date / end_date 与 CURDATE() 实时判断，不落库
--   · 票价单一定价，price / total_stock / sold_count 均在 exhibition
--   · 「艺览智荐」由算法接口返回，不落库
-- =============================================================================

USE `city_art`;


-- =============================================================================
-- 0. 辅助存储过程：判断列是否存在
-- =============================================================================
DROP PROCEDURE IF EXISTS `sp_add_column_if_absent`;
DROP PROCEDURE IF EXISTS `sp_drop_column_if_exists`;

DELIMITER $$

CREATE PROCEDURE `sp_add_column_if_absent`(IN p_table VARCHAR(64),
                                           IN p_column VARCHAR(64),
                                           IN p_definition TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1
                   FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = p_table
                     AND column_name = p_column) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE `sp_drop_column_if_exists`(IN p_table VARCHAR(64), IN p_column VARCHAR(64))
BEGIN
    IF EXISTS (SELECT 1
               FROM information_schema.columns
               WHERE table_schema = DATABASE()
                 AND table_name = p_table
                 AND column_name = p_column) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` DROP COLUMN `', p_column, '`');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;


-- =============================================================================
-- 1. banner：补 update_time
-- =============================================================================
CALL sp_add_column_if_absent('banner', 'update_time',
                             'DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''最近更新时间''');


-- =============================================================================
-- 2. gallery：本设计不需要 district、phone；确保 open_time、is_hot、type 存在
--    （若已按旧文档建过，open_time / type 会已存在，这里会跳过）
-- =============================================================================
CALL sp_add_column_if_absent('gallery', 'open_time',
                             'VARCHAR(50) DEFAULT NULL COMMENT ''开放时间''');

CALL sp_add_column_if_absent('gallery', 'is_hot',
                             'TINYINT NOT NULL DEFAULT 0 COMMENT ''是否热门：0-否 1-是（首页热门美术馆按此筛选）''');

-- 美术馆类型：美术馆列表页「美术馆类型」筛选用
CALL sp_add_column_if_absent('gallery', 'type',
                             'INT NOT NULL DEFAULT 1 COMMENT ''美术馆类型：1-综合类 2-当代类 3-古典类 4-雕塑类 5-摄影类''');

-- 以下两列按最新设计为「不需要」，删除（会连带删除列内数据）
CALL sp_drop_column_if_exists('gallery', 'phone');
CALL sp_drop_column_if_exists('gallery', 'district');

-- 类型筛选索引（可选，列表页按类型筛选时生效）
SET @idx_exists = (SELECT COUNT(*)
                   FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'gallery'
                     AND index_name = 'idx_type');
SET @ddl = IF(@idx_exists = 0,
              'ALTER TABLE `gallery` ADD KEY `idx_type` (`type`)',
              'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- =============================================================================
-- 3. exhibition：删除 subtitle；确保 images、open_time 存在
-- =============================================================================
CALL sp_add_column_if_absent('exhibition', 'images',
                             'VARCHAR(1000) DEFAULT NULL COMMENT ''展览实景图，多张用英文逗号分隔''');

CALL sp_add_column_if_absent('exhibition', 'open_time',
                             'VARCHAR(50) DEFAULT NULL COMMENT ''展览开放时间''');

CALL sp_drop_column_if_exists('exhibition', 'subtitle');

-- 统一展览分类的注释与默认值（1-综合展览 2-当代展览 3-古典展览 4-雕塑展览 5-摄影展览）
-- 若旧表 default 是 0，这里改为 1；已存在的 0 值记录请按业务需要手工订正
ALTER TABLE `exhibition`
    MODIFY COLUMN `type` INT NOT NULL DEFAULT 1
        COMMENT '展览分类：1-综合展览 2-当代展览 3-古典展览 4-雕塑展览 5-摄影展览';

-- 展览分类筛选索引（全部展览列表页按展览类型筛选时生效）
SET @idx_exists = (SELECT COUNT(*)
                   FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'exhibition'
                     AND index_name = 'idx_type');
SET @ddl = IF(@idx_exists = 0,
              'ALTER TABLE `exhibition` ADD KEY `idx_type` (`type`)',
              'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- =============================================================================
-- 4. exhibition_work：新建作品集表
-- =============================================================================
CREATE TABLE IF NOT EXISTS `exhibition_work`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `exhibition_id` BIGINT UNSIGNED NOT NULL                COMMENT '所属展览ID，关联 exhibition.id',
    `name`          VARCHAR(200)    NOT NULL                COMMENT '作品名称',
    `author`        VARCHAR(100)    DEFAULT NULL            COMMENT '作者姓名',
    `image`         VARCHAR(500)    DEFAULT NULL            COMMENT '作品图片地址',
    `description`   TEXT            DEFAULT NULL            COMMENT '作品简介',
    `sort`          INT             NOT NULL DEFAULT 0      COMMENT '排序，数字越大越靠前',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',

    PRIMARY KEY (`id`),
    KEY `idx_exhibition` (`exhibition_id`, `sort`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='展览作品集表';


-- =============================================================================
-- 5. 清理辅助存储过程
-- =============================================================================
DROP PROCEDURE IF EXISTS `sp_add_column_if_absent`;
DROP PROCEDURE IF EXISTS `sp_drop_column_if_exists`;


-- =============================================================================
-- 6. 校验
-- =============================================================================
-- 本设计下的目标结构：
--   banner     : id, image_url, title, sort, create_time, update_time
--   gallery    : id, name, cover_image, address, city, intro, open_time,
--                exhibition_count, is_hot, status, type, create_time, update_time
--   exhibition : id, gallery_id, title, poster_image, images, start_date, end_date,
--                open_time, description, is_hot, sort_order, type,
--                price, total_stock, sold_count, create_time, update_time
--   exhibition_work : id, exhibition_id, name, author, image, description,
--                     sort, create_time, update_time
--
-- 执行后请核对：
--   DESC banner;  DESC gallery;  DESC exhibition;  DESC exhibition_work;
