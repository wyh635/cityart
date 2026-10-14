-- =============================================================================
-- 艺览 ciTY · 冗余字段校准脚本
-- =============================================================================
-- 背景：以下字段是**冗余字段**（为性能而重复存储），需要由代码同步维护。
--       代码里某些路径早期没有同步，导致历史数据出现漂移。
--       本脚本按「唯一可信来源」（订单表）重算这些字段，一次性校准。
--
-- 七个冗余字段与它们的口径：
--   1. ticket_type.sold_count         ← Σ exhibition_order.quantity（status IN 2,5）
--   2. exhibition_calendar.sold_count ← Σ exhibition_order.quantity（按展览+日期，status IN 2,5）
--   3. exhibition.sold_count          ← Σ exhibition_order.quantity（按展览，status IN 2,5）
--   4. exhibition.total_stock         ← Σ ticket_type.total_stock（在售票种）
--   5. exhibition.price               ← MIN(ticket_type.price)（在售票种，展示用）
--   6. gallery.exhibition_count       ← COUNT(exhibition)（在展期内）
--   7. user.visit_count               ← COUNT(exhibition_order)（use_time 不为空 = 已核销）
--
-- ⚠️ 为什么口径是 status IN (2,5)：
--    2-已支付、5-已完成 才是**真正占用库存**的订单；
--    1-待支付（还没付）、3-已取消、4-已退款都不应计入已售。
--
-- ⚠️ 本脚本可重复执行（幂等），因为它只做「按来源重算覆盖」，不做增量加减。
-- =============================================================================

USE `city_art`;


-- =============================================================================
-- 1. 票种已售数量
-- =============================================================================
UPDATE `ticket_type` t
SET t.`sold_count` = (
    SELECT IFNULL(SUM(o.`quantity`), 0)
    FROM `exhibition_order` o
    WHERE o.`ticket_type_id` = t.`id`
      AND o.`status` IN (2, 5)
);

-- =============================================================================
-- 2. 日历已售数量
-- =============================================================================
UPDATE `exhibition_calendar` c
SET c.`sold_count` = (
    SELECT IFNULL(SUM(o.`quantity`), 0)
    FROM `exhibition_order` o
    WHERE o.`exhibition_id` = c.`exhibition_id`
      AND o.`visit_date` = c.`calendar_date`
      AND o.`status` IN (2, 5)
);

-- =============================================================================
-- 3. 展览已售数量
-- =============================================================================
UPDATE `exhibition` e
SET e.`sold_count` = (
    SELECT IFNULL(SUM(o.`quantity`), 0)
    FROM `exhibition_order` o
    WHERE o.`exhibition_id` = e.`id`
      AND o.`status` IN (2, 5)
);

-- =============================================================================
-- 4. 展览总库存（= 在售票种库存之和）
-- =============================================================================
-- 注意：这修的是 exhibition 表里那个「展示用」的冗余值，
--       真正下单扣的是 ticket_type 表，两者口径不同但都要一致
UPDATE `exhibition` e
SET e.`total_stock` = (
    SELECT IFNULL(SUM(t.`total_stock`), 0)
    FROM `ticket_type` t
    WHERE t.`exhibition_id` = e.`id`
      AND t.`status` = 0
)
WHERE EXISTS (SELECT 1 FROM `ticket_type` t WHERE t.`exhibition_id` = e.`id`);

-- =============================================================================
-- 5. 展览展示价（= 在售票种最低价）
-- =============================================================================
UPDATE `exhibition` e
SET e.`price` = (
    SELECT MIN(t.`price`)
    FROM `ticket_type` t
    WHERE t.`exhibition_id` = e.`id`
      AND t.`status` = 0
)
WHERE EXISTS (SELECT 1 FROM `ticket_type` t WHERE t.`exhibition_id` = e.`id` AND t.`status` = 0);

-- =============================================================================
-- 6. 美术馆当前在展数量
-- =============================================================================
-- 口径：该馆名下「今天在展期内」的展览数（与 GalleryMapper.syncExhibitionCount 一致）
UPDATE `gallery` g
SET g.`exhibition_count` = (
    SELECT COUNT(*)
    FROM `exhibition` e
    WHERE e.`gallery_id` = g.`id`
      AND e.`start_date` <= CURDATE()
      AND e.`end_date` >= CURDATE()
);

-- =============================================================================
-- 7. 用户观展次数
-- =============================================================================
UPDATE `user` u
SET u.`visit_count` = (
    SELECT COUNT(*)
    FROM `exhibition_order` o
    WHERE o.`user_id` = u.`id`
      AND o.`use_time` IS NOT NULL
);


-- =============================================================================
-- 8. 校准后校验（应全部返回 0 行）
-- =============================================================================
-- 把下面每段的 where 条件跑一遍，都应为空：

-- 票种漂移
--   SELECT t.id, t.sold_count FROM ticket_type t
--   WHERE t.sold_count <> (SELECT IFNULL(SUM(o.quantity),0) FROM exhibition_order o
--                          WHERE o.ticket_type_id=t.id AND o.status IN (2,5));

-- 日历漂移
--   SELECT c.id, c.sold_count FROM exhibition_calendar c
--   WHERE c.sold_count <> (SELECT IFNULL(SUM(o.quantity),0) FROM exhibition_order o
--                          WHERE o.exhibition_id=c.exhibition_id AND o.visit_date=c.calendar_date
--                            AND o.status IN (2,5));

-- 展览已售漂移
--   SELECT e.id, e.sold_count FROM exhibition e
--   WHERE e.sold_count <> (SELECT IFNULL(SUM(o.quantity),0) FROM exhibition_order o
--                          WHERE o.exhibition_id=e.id AND o.status IN (2,5));

-- 美术馆在展数漂移
--   SELECT g.id, g.exhibition_count FROM gallery g
--   WHERE g.exhibition_count <> (SELECT COUNT(*) FROM exhibition e
--                                WHERE e.gallery_id=g.id
--                                  AND e.start_date<=CURDATE() AND e.end_date>=CURDATE());

-- 用户观展次数漂移
--   SELECT u.id, u.visit_count FROM user u
--   WHERE u.visit_count <> (SELECT COUNT(*) FROM exhibition_order o
--                           WHERE o.user_id=u.id AND o.use_time IS NOT NULL);

-- 是否出现负数（都不应为负）
--   SELECT COUNT(*) FROM ticket_type WHERE sold_count < 0;
--   SELECT COUNT(*) FROM exhibition_calendar WHERE sold_count < 0;
--   SELECT COUNT(*) FROM exhibition WHERE sold_count < 0;
--   SELECT COUNT(*) FROM user WHERE visit_count < 0;
