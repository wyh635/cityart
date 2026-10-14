-- =============================================================================
-- 艺览 ciTY · 全量建表 SQL（共 8 张表）
-- =============================================================================
-- 包含：
--   1. banner               首页轮播图
--   2. gallery              美术馆（首页热门 + 美术馆列表 / 详情页）
--   3. exhibition           展览（首页热门 + 全部展览列表 + 展览详情页）
--   4. exhibition_work      作品集（展览详情页「作品集」区块）
--   5. ticket_type          票种（成人票 / 老人票 / 学生票 / 儿童票，各自独立价格与库存）
--   6. exhibition_calendar  展览日历库存（按日期限流，只放售票的若干天）
--   7. exhibition_order     订单（含票种快照、二维码、支付超时、核销信息）
--   8. favorite             收藏（展览 / 美术馆 多态）
--
-- 说明：本脚本只含业务表。登录模块的 `user` / `admin` 见
--       《01登录模块数据库建表方案.md》，本目录的 02_alter_existing.sql 会对
--       `user` 做增量扩展（加 city、gender）。
--
-- 表关系：
--   gallery    (1) ──< exhibition (N)           exhibition.gallery_id
--   exhibition (1) ──< exhibition_work (N)      exhibition_work.exhibition_id
--   exhibition (1) ──< ticket_type (N)          ticket_type.exhibition_id
--   exhibition (1) ──< exhibition_calendar (N)  exhibition_calendar.exhibition_id
--   exhibition (1) ──< exhibition_order (N)     exhibition_order.exhibition_id
--   ticket_type(1) ──< exhibition_order (N)     exhibition_order.ticket_type_id
--   user       (1) ──< exhibition_order (N)     exhibition_order.user_id
--   user       (1) ──< favorite (N)             favorite.user_id
--
-- 设计约定：
--   · 「艺览智荐」区块由算法接口直接返回，不落库，因此无对应表
--   · 展出状态（正在/即将/往期）由 start_date / end_date 实时判断，不落库
--   · **多票种**：价格与库存以 ticket_type 表为准
--     exhibition.price / total_stock / sold_count 保留，仅用于列表页展示票价
--   · **日历只放指定日期**：exhibition_calendar 里没有记录的日期 = 不发售
--   · **一单一码**：一个订单一个二维码，不建 ticket 表；核销按整单
--   · **库存两维**：下单时 ticket_type 与 exhibition_calendar 都要扣减
--   · 支付超时 30 分钟，超时自动取消并释放库存
--   · 剩余库存 = total_stock - sold_count
--
-- 字符集与引擎：InnoDB / utf8mb4 / utf8mb4_unicode_ci
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `city_art`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE `city_art`;


-- =============================================================================
-- 1. banner —— 首页轮播图
-- =============================================================================
DROP TABLE IF EXISTS `banner`;
CREATE TABLE `banner`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `image_url`   VARCHAR(500)    NOT NULL                COMMENT '轮播大图地址（文案烧入图中）',
    `title`       VARCHAR(100)    DEFAULT ''              COMMENT '标题，仅后台识别使用，页面不展示',
    `sort`        INT             NOT NULL DEFAULT 0      COMMENT '排序，数字越大越靠前',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',

    PRIMARY KEY (`id`),
    KEY `idx_sort` (`sort`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='首页轮播图';


-- =============================================================================
-- 2. gallery —— 美术馆
-- =============================================================================
DROP TABLE IF EXISTS `gallery`;
CREATE TABLE `gallery`
(
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`             VARCHAR(100)    NOT NULL                COMMENT '美术馆名称',
    `cover_image`      VARCHAR(255)    DEFAULT NULL            COMMENT '封面图（唯一图片，首页卡片与详情页共用）',
    `address`          VARCHAR(255)    NOT NULL                COMMENT '详细地址（已含城市与区县，如：南京市玄武区长江路333号（总统府对面））',
    `city`             VARCHAR(50)     NOT NULL                COMMENT '所在城市，用于筛选',
    `intro`            TEXT            DEFAULT NULL            COMMENT '展馆简介长文',
    `open_time`        VARCHAR(50)     DEFAULT NULL            COMMENT '开放时间，如：周二到周日 8:30-18:30',
    `exhibition_count` INT             NOT NULL DEFAULT 0      COMMENT '当前在展数量（冗余字段，首页卡片直接读取，需应用层或定时任务同步）',
    `is_hot`           TINYINT         NOT NULL DEFAULT 0      COMMENT '是否热门：0-否 1-是（首页「热门美术馆」按此筛选）',
    `status`           TINYINT         NOT NULL DEFAULT 0      COMMENT '状态：0-正常营业 1-闭馆维护',
    `type`             INT             NOT NULL DEFAULT 1      COMMENT '美术馆类型：1-综合类 2-当代类 3-古典类 4-雕塑类 5-摄影类（美术馆列表页筛选用）',
    `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',

    PRIMARY KEY (`id`),
    KEY `idx_city` (`city`),
    KEY `idx_hot` (`is_hot`, `exhibition_count`),
    KEY `idx_type` (`type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='美术馆信息表';


-- =============================================================================
-- 3. exhibition —— 展览
-- =============================================================================
DROP TABLE IF EXISTS `exhibition`;
CREATE TABLE `exhibition`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `gallery_id`    BIGINT UNSIGNED NOT NULL                COMMENT '所属美术馆ID，关联 gallery.id',
    `title`         VARCHAR(200)    NOT NULL                COMMENT '展览标题',
    `poster_image`  VARCHAR(255)    NOT NULL                COMMENT '封面海报图（首页卡片、场馆内展览卡片共用）',
    `images`        VARCHAR(1000)   DEFAULT NULL            COMMENT '展览实景图，多张用英文逗号分隔（详情页「展览实景」区块）',
    `start_date`    DATE            NOT NULL                COMMENT '开展日期',
    `end_date`      DATE            NOT NULL                COMMENT '闭展日期',
    `open_time`     VARCHAR(50)     DEFAULT NULL            COMMENT '展览开放时间，如：周二到周日 8:30-18:30',
    `description`   TEXT            DEFAULT NULL            COMMENT '展览简介长文（详情页「展览简介」区块）',
    `is_hot`        TINYINT         NOT NULL DEFAULT 0      COMMENT '是否热门：0-否 1-是（首页「当前热门展览」按此筛选）',
    `sort_order`    INT             NOT NULL DEFAULT 0      COMMENT '排序权重，数字越大越靠前',
    `type`          INT             NOT NULL DEFAULT 1      COMMENT '展览分类：1-综合展览 2-当代展览 3-古典展览 4-雕塑展览 5-摄影展览',
    `price`         DECIMAL(10, 2)  DEFAULT NULL            COMMENT '票价（单一定价，元）',
    `total_stock`   INT             NOT NULL DEFAULT 0      COMMENT '总库存（票数上限）',
    `sold_count`    INT             NOT NULL DEFAULT 0      COMMENT '已售数量（下单 +quantity，退款/取消 -quantity）',
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',

    PRIMARY KEY (`id`),
    KEY `idx_gallery` (`gallery_id`),
    KEY `idx_dates` (`start_date`, `end_date`),
    KEY `idx_hot` (`is_hot`, `sort_order`),
    KEY `idx_type` (`type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='展览信息表';


-- =============================================================================
-- 4. exhibition_work —— 作品集
-- =============================================================================
DROP TABLE IF EXISTS `exhibition_work`;
CREATE TABLE `exhibition_work`
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
-- 5. ticket_type —— 票种（多票种，各自独立价格与库存）
-- =============================================================================
DROP TABLE IF EXISTS `ticket_type`;
CREATE TABLE `ticket_type`
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
-- 6. exhibition_calendar —— 展览日历库存（只放售票的日期）
-- =============================================================================
DROP TABLE IF EXISTS `exhibition_calendar`;
CREATE TABLE `exhibition_calendar`
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
-- 7. exhibition_order —— 订单（一单一码）
-- =============================================================================
DROP TABLE IF EXISTS `exhibition_order`;
CREATE TABLE `exhibition_order`
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
-- 8. favorite —— 收藏（展览 / 美术馆 多态）
-- =============================================================================
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite`
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
