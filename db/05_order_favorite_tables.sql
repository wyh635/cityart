-- =============================================================================
-- 艺览 ciTY · 票务与个人中心 —— 增量变更脚本（已有库时使用）
-- =============================================================================
-- 用途：数据库里已经有 banner / gallery / exhibition / exhibition_work，
--       现在补上票务与个人中心所需的 4 张新表，并给 user 表加 2 个字段。
--
-- 特性：**可重复执行**（列用存储过程判断，表用 CREATE TABLE IF NOT EXISTS）。
--
-- 前置：如果你还没跑过 02_alter_existing.sql，请先跑那个（它会补齐
--       gallery.is_hot / exhibition.images 等列）。
-- =============================================================================

USE `city_art`;


-- =============================================================================
-- 0. 辅助存储过程：判断列是否存在
-- =============================================================================
DROP PROCEDURE IF EXISTS `sp_add_column_if_absent`;

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

DELIMITER ;


-- =============================================================================
-- 1. user：新增 city（所在城市）、gender（性别）
--    用于「编辑个人资料」页：昵称 / 性别 / 所在城市 / 手机号
-- =============================================================================
CALL sp_add_column_if_absent('user', 'city',
                             'VARCHAR(50) DEFAULT NULL COMMENT ''用户所在城市，如：南京''');

CALL sp_add_column_if_absent('user', 'gender',
                             'TINYINT NOT NULL DEFAULT 0 COMMENT ''性别：0-未知 1-男 2-女''');


-- =============================================================================
-- 2. ticket_type —— 票种（多票种，各自独立价格与库存）
-- =============================================================================
CREATE TABLE IF NOT EXISTS `ticket_type`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '票种主键ID',
    `exhibition_id`  BIGINT UNSIGNED NOT NULL                COMMENT '所属展览ID，关联 exhibition.id',
    `name`           VARCHAR(50)     NOT NULL                COMMENT '票种名称：成人票 / 老人票 / 学生票 / 儿童票',
    `price`          DECIMAL(10, 2)  NOT NULL DEFAULT 0.00   COMMENT '该票种单价（元）',
    `total_stock`    INT             NOT NULL DEFAULT 0      COMMENT '总库存',
    `sold_count`     INT             NOT NULL DEFAULT 0      COMMENT '已售数量（下单 +quantity，超时取消/退款 -quantity）',
    `limit_per_user` INT             NOT NULL DEFAULT 5      COMMENT '每人限购数量（原型：每人至多可购五张）',
    `sort`           INT             NOT NULL DEFAULT 0      COMMENT '排序，数字越大越靠前',
    `status`         TINYINT         NOT NULL DEFAULT 0      COMMENT '状态：0-在售 1-停售',
    `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exhibition_name` (`exhibition_id`, `name`),
    KEY `idx_exhibition_status` (`exhibition_id`, `status`, `sort`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='展览票种表';


-- =============================================================================
-- 3. exhibition_calendar —— 展览日历库存（只放售票的日期）
-- =============================================================================
CREATE TABLE IF NOT EXISTS `exhibition_calendar`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `exhibition_id` BIGINT UNSIGNED NOT NULL                COMMENT '展览ID，关联 exhibition.id',
    `calendar_date` DATE            NOT NULL                COMMENT '可预约日期',
    `total_stock`   INT             NOT NULL DEFAULT 0      COMMENT '当日总放票数',
    `sold_count`    INT             NOT NULL DEFAULT 0      COMMENT '当日已售数量',
    `status`        TINYINT         NOT NULL DEFAULT 0      COMMENT '状态：0-可预约 1-已约满/闭馆',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_exhibition_date` (`exhibition_id`, `calendar_date`),
    KEY `idx_date` (`calendar_date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='展览日历库存表（只放售票的日期）';


-- =============================================================================
-- 4. exhibition_order —— 订单（一单一码）
-- =============================================================================
CREATE TABLE IF NOT EXISTS `exhibition_order`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '订单主键ID',
    `order_no`        VARCHAR(32)     NOT NULL                COMMENT '订单号（业务唯一号）',
    `user_id`         BIGINT UNSIGNED NOT NULL                COMMENT '下单用户ID，关联 user.id',
    `exhibition_id`   BIGINT UNSIGNED NOT NULL                COMMENT '展览ID，关联 exhibition.id',
    `gallery_id`      BIGINT UNSIGNED NOT NULL                COMMENT '美术馆ID，关联 gallery.id（冗余，票夹显示「观展场馆」）',
    `ticket_type_id`  BIGINT UNSIGNED NOT NULL                COMMENT '票种ID，关联 ticket_type.id',
    `ticket_name`     VARCHAR(50)     NOT NULL                COMMENT '票种名称快照，如：成人票',
    `price`           DECIMAL(10, 2)  NOT NULL DEFAULT 0.00   COMMENT '成交单价快照（元），防后台改价影响历史订单',
    `quantity`        INT             NOT NULL DEFAULT 1      COMMENT '购票数量',
    `total_amount`    DECIMAL(10, 2)  NOT NULL DEFAULT 0.00   COMMENT '订单总金额 = price * quantity',
    `status`          TINYINT         NOT NULL DEFAULT 1      COMMENT '订单状态：1-待支付 2-已支付 3-已取消 4-已退款 5-已完成（已核销）',
    `visit_date`      DATE            NOT NULL                COMMENT '预约观展日期',
    `qr_code`         VARCHAR(500)    DEFAULT NULL            COMMENT '电子票二维码（一单一码，支付成功后生成）',
    `use_time`        DATETIME        DEFAULT NULL            COMMENT '核销入场时间（看展记录的「入场时间」）',
    `expire_time`     DATETIME        DEFAULT NULL            COMMENT '电子票有效期（支付成功后生成，票夹显示「有效-N天后过期」）',
    `contact_name`    VARCHAR(50)     DEFAULT NULL            COMMENT '联系人姓名',
    `contact_phone`   VARCHAR(11)     DEFAULT NULL            COMMENT '联系人手机号',
    `pay_type`        TINYINT         DEFAULT NULL            COMMENT '支付方式：1-微信支付 2-支付宝',
    `pay_time`        DATETIME        DEFAULT NULL            COMMENT '支付成功时间',
    `pay_expire_time` DATETIME        DEFAULT NULL            COMMENT '支付超时时间 = 下单时间 + 30 分钟',
    `cancel_time`     DATETIME        DEFAULT NULL            COMMENT '取消时间',
    `cancel_reason`   VARCHAR(255)    DEFAULT NULL            COMMENT '取消原因',
    `refund_status`   TINYINT         NOT NULL DEFAULT 0      COMMENT '退款状态：0-无退款 1-退款中 2-已退款 3-退款驳回',
    `refund_amount`   DECIMAL(10, 2)  DEFAULT NULL            COMMENT '实际退款金额',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_status_time` (`user_id`, `status`, `create_time`),
    KEY `idx_user_use` (`user_id`, `use_time`),
    KEY `idx_exhibition_date` (`exhibition_id`, `visit_date`),
    KEY `idx_pay_expire` (`status`, `pay_expire_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='展览购票订单表';


-- =============================================================================
-- 5. favorite —— 收藏（展览 / 美术馆 多态）
-- =============================================================================
CREATE TABLE IF NOT EXISTS `favorite`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '收藏主键ID',
    `user_id`     BIGINT UNSIGNED NOT NULL                COMMENT '用户ID，关联 user.id',
    `target_type` TINYINT         NOT NULL                COMMENT '收藏类型：1-展览 2-美术馆',
    `target_id`   BIGINT UNSIGNED NOT NULL                COMMENT '收藏对象ID（type=1 指 exhibition.id，=2 指 gallery.id）',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),
    KEY `idx_target` (`target_type`, `target_id`),
    KEY `idx_user_time` (`user_id`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户收藏表（展览/美术馆多态）';


-- =============================================================================
-- 6. 清理辅助存储过程
-- =============================================================================
DROP PROCEDURE IF EXISTS `sp_add_column_if_absent`;


-- =============================================================================
-- 7. 校验
-- =============================================================================
-- 执行后应新增 4 张表、user 表新增 2 列：
--   SHOW TABLES;
--   DESC user;              -- 应有 city、gender
--   DESC ticket_type;
--   DESC exhibition_calendar;
--   DESC exhibition_order;
--   DESC favorite;
