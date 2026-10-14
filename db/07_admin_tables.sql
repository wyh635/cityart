-- =============================================================================
-- 艺览 ciTY · 后台管理系统（B 端）—— 建表与字段扩展 SQL
-- =============================================================================
-- 对应后台页面：
--   1. 数据看板        今日到馆总人数 / 本周售票总量 / 本周总营业额 / 展览排行
--   2. 展览管理-展览列表 展览ID / 展览名称 / 所属场馆 / 展期 / 票价 / 当日库存（含「已售完」）
--   3. 展览管理-新增展览 展览名称 / 所属展馆 / 开展闭展日期 / 4 种票价 / 每日票数上限 / 展览海报
--   4. 展览管理-新增展馆 展馆名称 / 所在城市 / 开馆时间 / 闭馆日期 / 展馆海报 / 展馆简历
--   5. 票务数据        统计日期 / 展览名称 / 当日出售票数 / 当日核销票数 / 当日未核销票数 / 单日营收
--   6. 扫码核销        扫码核销 / 订单号核销 + 核销记录（核销时间/订单编号/展览名称/购票人/核销方式）
--   7. 个人中心        头像 / 昵称 / 城市 / 编辑个人资料 / 退出登录
--
-- -----------------------------------------------------------------------------
-- 结论：后台**不新建业务大表**，主要是给现有表补字段 + 新建 3 张辅助表
-- -----------------------------------------------------------------------------
--   新建：city（城市字典）、notification（管理员通知）、gallery（后台新增时用同一张表）
--   补字段：exhibition（编号、每日票数上限）、gallery（开馆/闭馆时间）、
--           exhibition_order（核销方式、核销人）、admin（头像、城市）
--
--   数据看板 / 票务数据 的统计**全部从 exhibition_order 实时聚合**，不建统计表。
--   核销记录**直接从 exhibition_order 查**（一单一码，一单一条核销记录），不建核销表。
--
-- 字符集与引擎：InnoDB / utf8mb4 / utf8mb4_unicode_ci
-- 可重复执行：表用 CREATE TABLE IF NOT EXISTS，列用存储过程判断
-- =============================================================================

USE `city_art`;


-- =============================================================================
-- 0. 辅助存储过程：判断列是否存在（幂等加列）
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
-- 1. city —— 城市字典
-- =============================================================================
-- 后台多处的「城市」下拉（数据看板 / 展览列表 / 扫码核销 / 票务数据）都读本表。
-- 之前 C 端是 `SELECT DISTINCT city FROM gallery` 兜的，后台需要独立维护（含未开馆城市）。
CREATE TABLE IF NOT EXISTS `city`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        VARCHAR(50)     NOT NULL                COMMENT '城市名称，如：南京',
    `code`        VARCHAR(20)     DEFAULT NULL            COMMENT '城市行政编码（对接定位/第三方时用）',
    `sort`        INT             NOT NULL DEFAULT 0      COMMENT '排序，数字越大越靠前',
    `status`      TINYINT         NOT NULL DEFAULT 0      COMMENT '状态：0-启用 1-停用',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_status_sort` (`status`, `sort`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='城市字典表';


-- =============================================================================
-- 2. notification —— 管理员通知（后台右上角铃铛 + 未读角标）
-- =============================================================================
-- 原型右上角有铃铛图标与红色未读数字，本表承载通知内容与已读状态。
CREATE TABLE IF NOT EXISTS `notification`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `admin_id`    BIGINT UNSIGNED DEFAULT NULL            COMMENT '接收管理员ID，关联 admin.id；NULL 表示全员通知',
    `type`        TINYINT         NOT NULL DEFAULT 1      COMMENT '通知类型：1-系统 2-订单 3-退款 4-票务告警',
    `title`       VARCHAR(100)    NOT NULL                COMMENT '通知标题',
    `content`     VARCHAR(500)    DEFAULT NULL            COMMENT '通知内容',
    `link_url`    VARCHAR(255)    DEFAULT NULL            COMMENT '点击跳转的前端路由',
    `is_read`     TINYINT         NOT NULL DEFAULT 0      COMMENT '是否已读：0-未读 1-已读',
    `read_time`   DATETIME        DEFAULT NULL            COMMENT '已读时间',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近更新时间',

    PRIMARY KEY (`id`),
    KEY `idx_admin_read` (`admin_id`, `is_read`, `create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='后台管理员通知表';


-- =============================================================================
-- 3. 给现有表补字段
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 3.1 exhibition：展览编号 + 每日票数上限
-- ---------------------------------------------------------------------------
-- 展览编号：后台列表显示的「NJ001」，运营可读的编号，与自增 id 区分
-- 每日票数上限：新增展览页的「每日票数上限」，创建展览时按展期批量生成
--              exhibition_calendar 的每日记录（total_stock = 该值）
CALL sp_add_column_if_absent('exhibition', 'exhibition_code',
                             'VARCHAR(32) DEFAULT NULL COMMENT ''展览编号，如 NJ001（后台展示用）''');

CALL sp_add_column_if_absent('exhibition', 'daily_stock_limit',
                             'INT NOT NULL DEFAULT 0 COMMENT ''每日票数上限（0 表示不限制），建展览时据此生成日历库存''');

-- 上下架状态：后台「展览列表」的「全部状态」筛选、以及上下架操作都依赖它
-- 注意与「展出状态」（正在/即将/往期，由日期实时算）语义不同，两者互不影响
CALL sp_add_column_if_absent('exhibition', 'status',
                             'TINYINT NOT NULL DEFAULT 0 COMMENT ''上下架状态：0-上架（C 端可见） 1-下架''');

-- 展览分类扩展为 5 类（与 C 端一致：1-综合 2-当代 3-古典 4-雕塑 5-摄影）
ALTER TABLE `exhibition`
    MODIFY COLUMN `type` INT NOT NULL DEFAULT 1
        COMMENT '展览分类：1-综合展览 2-当代展览 3-古典展览 4-雕塑展览 5-摄影展览';

-- 编号唯一（允许 NULL，历史数据不受影响）
SET @idx_exists = (SELECT COUNT(*)
                   FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'exhibition'
                     AND index_name = 'uk_exhibition_code');
SET @ddl = IF(@idx_exists = 0,
              'ALTER TABLE `exhibition` ADD UNIQUE KEY `uk_exhibition_code` (`exhibition_code`)',
              'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- ---------------------------------------------------------------------------
-- 3.2 gallery：开馆时间 / 闭馆时间
-- ---------------------------------------------------------------------------
-- 新增展馆页是两个独立输入框（「开馆时间」「闭馆日期」），
-- 与 C 端详情页用的 open_time 文本（如「周二到周日 8:30-18:30」）并存：
--   · open_time  给 C 端展示（整串文本）
--   · open_hour / close_hour 给后台表单编辑（结构化）
CALL sp_add_column_if_absent('gallery', 'open_hour',
                             'VARCHAR(20) DEFAULT NULL COMMENT ''每日开馆时间，如 08:30''');

CALL sp_add_column_if_absent('gallery', 'close_hour',
                             'VARCHAR(20) DEFAULT NULL COMMENT ''每日闭馆时间，如 18:30''');

CALL sp_add_column_if_absent('gallery', 'closed_day',
                             'VARCHAR(50) DEFAULT NULL COMMENT ''固定闭馆日，如：周一（节假日除外）''');

CALL sp_add_column_if_absent('gallery', 'gallery_code',
                             'VARCHAR(32) DEFAULT NULL COMMENT ''展馆编号（后台展示用）''');


-- ---------------------------------------------------------------------------
-- 3.3 exhibition_order：核销方式 + 核销人
-- ---------------------------------------------------------------------------
-- 扫码核销页支持两种方式：「扫码」和「输入订单号」，核销记录要区分
CALL sp_add_column_if_absent('exhibition_order', 'verify_type',
                             'TINYINT DEFAULT NULL COMMENT ''核销方式：1-扫码核销 2-输入订单号核销''');

CALL sp_add_column_if_absent('exhibition_order', 'verify_admin_id',
                             'BIGINT UNSIGNED DEFAULT NULL COMMENT ''核销操作的管理员ID，关联 admin.id''');

-- 核销记录按「核销时间」倒序分页，加索引
SET @idx_exists = (SELECT COUNT(*)
                   FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'exhibition_order'
                     AND index_name = 'idx_verify_time');
SET @ddl = IF(@idx_exists = 0,
              'ALTER TABLE `exhibition_order` ADD KEY `idx_verify_time` (`use_time`)',
              'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- ---------------------------------------------------------------------------
-- 3.4 admin：头像 + 所在城市
-- ---------------------------------------------------------------------------
-- 后台个人中心显示「林一 / 南京 / 编辑个人资料」，并有「切换账号」
CALL sp_add_column_if_absent('admin', 'avatar',
                             'VARCHAR(255) DEFAULT NULL COMMENT ''头像URL（可空：新管理员可能还没上传头像）''');

CALL sp_add_column_if_absent('admin', 'city',
                             'VARCHAR(50) DEFAULT NULL COMMENT ''所在城市，如：南京''');

CALL sp_add_column_if_absent('admin', 'admin_code',
                             'VARCHAR(32) DEFAULT NULL COMMENT ''管理员编号（后台展示用）''');


-- ---------------------------------------------------------------------------
-- 3.5 admin.avatar 置为可空
-- ---------------------------------------------------------------------------
-- 若该列之前被建成 NOT NULL DEFAULT ''，只改昵称（不传 avatar）时
-- 动态 SQL 不会更新它，但 INSERT 新管理员时不填头像会报
-- Column 'avatar' cannot be null —— 置为可空更符合「新账号还没上传头像」的实际情况。
ALTER TABLE `admin`
    MODIFY COLUMN `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL（可空：新管理员可能还没上传头像）';


-- =============================================================================
-- 4. 清理辅助存储过程
-- =============================================================================
DROP PROCEDURE IF EXISTS `sp_add_column_if_absent`;


-- =============================================================================
-- 5. 初始化城市字典
-- =============================================================================
INSERT INTO `city` (`name`, `code`, `sort`, `status`)
VALUES ('南京', '320100', 100, 0),
       ('苏州', '320500', 90, 0),
       ('无锡', '320200', 80, 0)
ON DUPLICATE KEY UPDATE `sort` = VALUES(`sort`);


-- =============================================================================
-- 6. 执行后校验
-- =============================================================================
-- ① 新表是否建好
--   SHOW TABLES LIKE 'city';          -- 1 行
--   SHOW TABLES LIKE 'notification';  -- 1 行
--   SELECT * FROM city;               -- 3 行（南京/苏州/无锡）
--
-- ② 补的列是否到位
--   SELECT exhibition_code, daily_stock_limit, type FROM exhibition;   -- 有前两列，type 注释为 5 类
--   SELECT open_hour, close_hour, closed_day, gallery_code FROM gallery;
--   SELECT verify_type, verify_admin_id FROM exhibition_order;
--   SELECT avatar, city, admin_code FROM admin;
--
-- ③ 索引
--   SHOW INDEX FROM exhibition WHERE Key_name = 'uk_exhibition_code';
--   SHOW INDEX FROM exhibition_order WHERE Key_name = 'idx_verify_time';
--
-- -----------------------------------------------------------------------------
-- 7. 不建表的数据（全部实时聚合，避免冗余不一致）
-- -----------------------------------------------------------------------------
-- 数据看板：
--   今日到馆总人数  = COUNT(*) FROM exhibition_order WHERE DATE(use_time)=CURDATE()
--   本周售票总量    = SUM(quantity) WHERE status IN (2,5) AND pay_time >= 本周一
--   本周总营业额    = SUM(total_amount) WHERE status IN (2,5) AND pay_time >= 本周一
--   展览排行        = GROUP BY exhibition_id ORDER BY SUM(quantity) DESC
--   客流占比        = 该展览票数 / 全部票数（Service 层计算）
--
-- 票务数据：
--   当日出售票数    = SUM(quantity) WHERE DATE(pay_time)=统计日期 AND status IN (2,5)
--   当日核销票数    = SUM(quantity) WHERE DATE(use_time)=统计日期
--   当日未核销票数  = 出售票数 - 核销票数
--   单日营收        = SUM(total_amount) WHERE DATE(pay_time)=统计日期 AND status IN (2,5)
--
-- 扫码核销记录：
--   SELECT use_time, order_no, exhibition_id, contact_name, verify_type
--   FROM exhibition_order WHERE use_time IS NOT NULL ORDER BY use_time DESC
--
-- ⚠️ 后续若数据量大导致报表变慢，再补一张按天汇总的
--    `ticket_daily_stats`（统计日期, 展览ID, 售出票数, 核销票数, 营收），
--    由定时任务每晚汇总。当前数据量用实时聚合足够。
