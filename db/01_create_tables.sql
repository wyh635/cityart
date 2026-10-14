-- =============================================================================
-- 艺览 ciTY · 首页与详情页 —— 建表 SQL（共 4 张表）
-- =============================================================================
-- 包含：
--   1. banner           首页轮播图
--   2. gallery          美术馆（首页热门美术馆 + 美术馆详情页）
--   3. exhibition       展览（首页热门展览 + 展览详情页 + 场馆内展览列表）
--   4. exhibition_work  作品集（展览详情页「作品集」区块，关联 exhibition.id）
--
-- 表关系：
--   gallery (1) ──< exhibition (N)          exhibition.gallery_id
--   exhibition (1) ──< exhibition_work (N)  exhibition_work.exhibition_id
--
-- 说明：
--   · 「艺览智荐」区块由算法接口直接返回，不落库，因此无对应表
--   · 票价为单一定价，故不设票种表，price / total_stock / sold_count 均在 exhibition
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
