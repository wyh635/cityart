-- =============================================================================
-- 艺览 ciTY · 票务与个人中心 —— 初始化测试数据（可选执行）
-- =============================================================================
-- 前置：必须先执行 db/04_init_data.sql（它会造出展览 1~6）
-- 用途：为购票流程、票夹、看展记录、收藏列表造出可验证的数据
--
-- 关键设计：
--   · 票种按原型配 4 种：成人票 ¥58 / 老人票 ¥38 / 学生票 ¥38 / 儿童票 ¥38
--   · 日历只放 5 天，其中 1 天造出「已售完」状态，验证前端的置灰逻辑
--   · 日期全部用「相对日期」函数，保证数据永远有效、不会过期
--
-- ⚠️ 会先清空 4 张表的现有数据，请勿在有真实数据的环境执行。
-- =============================================================================

USE `city_art`;

-- 清空（注意顺序：先删子表）
DELETE FROM `favorite`;
DELETE FROM `exhibition_order`;
DELETE FROM `exhibition_calendar`;
DELETE FROM `ticket_type`;


-- =============================================================================
-- 1. 票种（展览 1「咫尺千里」配 4 种，价格按原型）
--    其余展览各配 1 个成人票，用于列表页显示票价
-- =============================================================================
INSERT INTO `ticket_type` (`exhibition_id`, `name`, `price`, `total_stock`, `sold_count`, `limit_per_user`, `sort`, `status`)
VALUES
-- 展览 1：完整的 4 种票种（对应购票页原型）
(1, '成人票', 58.00, 500, 0, 5, 100, 0),
(1, '老人票', 38.00, 300, 0, 5, 90, 0),
(1, '学生票', 38.00, 300, 0, 5, 80, 0),
(1, '儿童票', 38.00, 200, 0, 5, 70, 0),
-- 展览 2：单一票种
(2, '成人票', 38.00, 300, 0, 5, 100, 0),
-- 展览 3：单一票种
(3, '成人票', 58.00, 400, 0, 5, 100, 0),
-- 展览 4（即将展出）：单一票种
(4, '成人票', 68.00, 200, 0, 5, 100, 0),
-- 展览 5、6（往期）：保留票种用于历史订单显示
(5, '成人票', 63.00, 300, 0, 5, 100, 0),
(5, '学生票', 38.00, 100, 0, 5, 90, 0),
(6, '成人票',  0.00, 300, 0, 5, 100, 0);


-- =============================================================================
-- 2. 日历库存（展览 1 只放 5 天；其中第 3 天造「已售完」）
-- =============================================================================
-- 展览 1：只放 5 天（对应原型下拉的 5 行），其中第 3 天造「已售完」
INSERT INTO `exhibition_calendar` (`exhibition_id`, `calendar_date`, `total_stock`, `sold_count`, `status`)
VALUES
(1, DATE_ADD(CURDATE(), INTERVAL 2 DAY),  200, 0,   0),  -- 可预约
(1, DATE_ADD(CURDATE(), INTERVAL 3 DAY),  200, 0,   0),  -- 可预约
(1, DATE_ADD(CURDATE(), INTERVAL 4 DAY),  200, 200, 0),  -- 已售完（剩余 0，验证置灰）
(1, DATE_ADD(CURDATE(), INTERVAL 5 DAY),  200, 0,   0),  -- 可预约
(1, DATE_ADD(CURDATE(), INTERVAL 7 DAY),  200, 0,   0);  -- 可预约

-- 展览 2：放 3 天
INSERT INTO `exhibition_calendar` (`exhibition_id`, `calendar_date`, `total_stock`, `sold_count`, `status`)
VALUES
(2, DATE_ADD(CURDATE(), INTERVAL 1 DAY),  100, 0, 0),
(2, DATE_ADD(CURDATE(), INTERVAL 2 DAY),  100, 0, 0),
(2, DATE_ADD(CURDATE(), INTERVAL 3 DAY),  100, 0, 0);

-- 展览 3：放 3 天
INSERT INTO `exhibition_calendar` (`exhibition_id`, `calendar_date`, `total_stock`, `sold_count`, `status`)
VALUES
(3, DATE_ADD(CURDATE(), INTERVAL 1 DAY),  150, 0, 0),
(3, DATE_ADD(CURDATE(), INTERVAL 4 DAY),  150, 0, 0),
(3, DATE_ADD(CURDATE(), INTERVAL 6 DAY),  150, 0, 0);

-- 展览 4（即将展出）：还没开始放票，故意留空 → 验证「无票可售」的表现
-- 展览 5、6（往期）：不放票


-- =============================================================================
-- 3. 订单（覆盖 4 种状态，用于验证我的订单页的 4 个 tab）
--    前置：user 表至少有 id=1 的用户
-- =============================================================================
-- 五条订单，分别覆盖 4 个状态 tab：
--   ① 待支付   → 支付页 + 30 分钟倒计时
--   ② 已支付   → 票夹（有 qr_code）+ 有效票夹统计
--   ③ 已核销   → 看展记录（有 use_time）
--   ④ 已退款   → 「已退款」tab
--   ⑤ 已取消   → 「已取消」tab
INSERT INTO `exhibition_order`
(`order_no`, `user_id`, `exhibition_id`, `gallery_id`, `ticket_type_id`, `ticket_name`,
 `price`, `quantity`, `total_amount`, `status`, `visit_date`, `qr_code`, `use_time`, `expire_time`,
 `contact_name`, `contact_phone`, `pay_type`, `pay_time`, `pay_expire_time`,
 `refund_status`, `refund_amount`, `create_time`)
VALUES
-- ① 待支付
('20261001000001', 1, 1, 1, 1, '成人票', 58.00, 1, 58.00, 1,
 DATE_ADD(CURDATE(), INTERVAL 2 DAY), NULL, NULL, NULL,
 '熊籽', '13800138000', NULL, NULL, DATE_ADD(NOW(), INTERVAL 30 MINUTE),
 0, NULL, NOW()),
-- ② 已支付
('20261001000002', 1, 1, 1, 1, '成人票', 58.00, 2, 116.00, 2,
 DATE_ADD(CURDATE(), INTERVAL 3 DAY),
 '/static/mock/qr/20261001000002.png', NULL, DATE_ADD(NOW(), INTERVAL 45 DAY),
 '熊籽', '13800138000', 1, NOW(), DATE_ADD(NOW(), INTERVAL 30 MINUTE),
 0, NULL, NOW()),
-- ③ 已核销（看展记录数据源：use_time 不为空）
('20260901000003', 1, 5, 4, 8, '成人票', 63.00, 1, 63.00, 5,
 DATE_SUB(CURDATE(), INTERVAL 30 DAY),
 '/static/mock/qr/20260901000003.png', DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY),
 '熊籽', '13800138000', 1, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 30 DAY),
 0, NULL, DATE_SUB(NOW(), INTERVAL 31 DAY)),
-- ④ 已退款
('20260901000004', 1, 2, 1, 5, '成人票', 38.00, 1, 38.00, 4,
 DATE_SUB(CURDATE(), INTERVAL 20 DAY),
 '/static/mock/qr/20260901000004.png', NULL, NULL,
 '熊籽', '13800138000', 1, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY),
 2, 38.00, DATE_SUB(NOW(), INTERVAL 21 DAY)),
-- ⑤ 已取消
('20260901000005', 1, 3, 1, 6, '成人票', 58.00, 1, 58.00, 3,
 DATE_SUB(CURDATE(), INTERVAL 10 DAY), NULL, NULL, NULL,
 '熊籽', '13800138000', NULL, NULL, DATE_SUB(NOW(), INTERVAL 10 DAY),
 0, NULL, DATE_SUB(NOW(), INTERVAL 10 DAY));

-- 同步库存（让票种/日历的已售数量与订单一致）
UPDATE `ticket_type` SET `sold_count` = 2 WHERE `id` = 1;   -- 展览1 成人票卖了 2 张
UPDATE `ticket_type` SET `sold_count` = 1 WHERE `id` = 5;   -- 展览2 成人票卖了 1 张
UPDATE `ticket_type` SET `sold_count` = 1 WHERE `id` = 8;   -- 展览5 成人票卖了 1 张
UPDATE `exhibition_calendar` SET `sold_count` = sold_count + 0
WHERE `exhibition_id` = 1;


-- =============================================================================
-- 4. 收藏（验证收藏列表的「展览 4 / 美术馆 ?」tab 计数）
-- =============================================================================
INSERT INTO `favorite` (`user_id`, `target_type`, `target_id`, `create_time`)
VALUES
(1, 1, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),   -- 收藏展览「咫尺千里」
(1, 1, 2, DATE_SUB(NOW(), INTERVAL 4 DAY)),   -- 收藏展览「江苏水印木刻」
(1, 1, 3, DATE_SUB(NOW(), INTERVAL 3 DAY)),   -- 收藏展览「典藏精品陈列」
(1, 1, 5, DATE_SUB(NOW(), INTERVAL 2 DAY)),   -- 收藏展览「江山艺术展」
(1, 2, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),   -- 收藏美术馆「江苏省美术馆」
(1, 2, 4, NOW());                             -- 收藏美术馆「金陵美术馆」


-- =============================================================================
-- 5. 同步个人中心统计所需的冗余字段
-- =============================================================================
-- 观展次数：已核销订单数
UPDATE `user` u
SET u.`visit_count` = (
    SELECT COUNT(*) FROM `exhibition_order` o
    WHERE o.`user_id` = u.`id` AND o.`use_time` IS NOT NULL
);

-- 补齐用户的 city / gender（编辑个人资料页展示用）
UPDATE `user` SET `city` = '南京', `gender` = 1 WHERE `id` = 1;


-- =============================================================================
-- 6. 执行后校验
-- =============================================================================
-- ① 各表行数
--   SELECT 'ticket_type' t, COUNT(*) c FROM ticket_type
--   UNION ALL SELECT 'exhibition_calendar', COUNT(*) FROM exhibition_calendar
--   UNION ALL SELECT 'exhibition_order', COUNT(*) FROM exhibition_order
--   UNION ALL SELECT 'favorite', COUNT(*) FROM favorite;
--   预期：10 / 11 / 5 / 6
--
-- ② 购票页「选择票种」（应 4 条）
--   SELECT name, price, total_stock - sold_count AS remaining
--   FROM ticket_type WHERE exhibition_id = 1 AND status = 0 ORDER BY sort DESC;
--
-- ③ 购票页「选择观展日期」（应 5 条，第 3 条 soldOut=1）
--   SELECT calendar_date, total_stock - sold_count AS remaining,
--          CASE WHEN total_stock - sold_count <= 0 THEN 1 ELSE 0 END AS soldOut
--   FROM exhibition_calendar WHERE exhibition_id = 1 AND calendar_date >= CURDATE()
--   ORDER BY calendar_date;
--
-- ④ 个人中心三个统计（预期 收藏=6、有效票夹=1、观展次数=1）
--   SELECT
--     (SELECT COUNT(*) FROM favorite WHERE user_id = 1)                         AS 我的收藏,
--     (SELECT COUNT(*) FROM exhibition_order WHERE user_id = 1 AND status = 2)  AS 有效票夹,
--     (SELECT visit_count FROM user WHERE id = 1)                               AS 观展次数;
--
-- ⑤ 看展记录（应 1 条，有 use_time）
--   SELECT order_no, use_time, ticket_name FROM exhibition_order
--   WHERE user_id = 1 AND use_time IS NOT NULL;
