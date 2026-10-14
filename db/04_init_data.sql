-- =============================================================================
-- 艺览 ciTY · 初始化测试数据（可选执行）
-- =============================================================================
-- 用途：本地开发时快速造出首页 / 详情页能显示的数据。
-- ⚠️ 会先清空 4 张表的现有数据，请勿在有真实数据的环境执行。
--
-- 关键设计：展览日期全部使用「相对日期」函数（DATE_SUB / DATE_ADD）
--          以保证数据永远有效，不会因为时间推移而全部过期。
-- =============================================================================

USE `city_art`;

-- 清空（注意顺序：先删子表，再删父表）
-- 用 TRUNCATE 而不是 DELETE：顺手把自增 ID 重置回 1，
-- 这样下面的 exhibition_work 才能稳定地挂在 exhibition_id = 1 上（可重复执行）
TRUNCATE TABLE `exhibition_work`;
TRUNCATE TABLE `exhibition`;
TRUNCATE TABLE `gallery`;
TRUNCATE TABLE `banner`;

-- =============================================================================
-- 1. 轮播图
-- =============================================================================
INSERT INTO `banner` (`image_url`, `title`, `sort`)
VALUES ('/static/mock/banner/water-print.jpg', '江苏水印木刻实践与研究特展', 100),
       ('/static/mock/banner/jiangsu-art.jpg', '江苏省美术馆90周年系列展', 90);

-- =============================================================================
-- 2. 美术馆（首页热门 4 个 + 1 个非热门用于对照）
-- =============================================================================
INSERT INTO `gallery` (`name`, `cover_image`, `address`, `city`, `intro`, `open_time`,
                       `exhibition_count`, `is_hot`, `status`, `type`)
VALUES ('江苏省美术馆', '/static/mock/gallery/jiangsu.jpg',
        '南京市玄武区长江路333号（总统府对面）', '南京',
        '江苏省美术馆坐落于南京长江路文化建筑群，其前身是 1936 年建成的国立美术陈列馆，为中国近现代首座国家级美术馆，是我国美术馆事业的起点。馆内典藏近 14000 件美术作品，涵盖古今中外门类，中国书画藏品最为丰富。',
        '周二到周日 8:30-18:30', 2, 1, 0, 1),

       ('国立美术陈列馆', '/static/mock/gallery/guoli.jpg',
        '南京市玄武区长江路266号', '南京',
        '国立美术陈列馆旧址为中国近现代第一座国家级美术馆建筑，是民国新民族形式建筑的代表作。',
        '周二到周日 8:30-18:30', 1, 1, 0, 3),

       ('四方美术馆', '/static/mock/gallery/sifang.jpg',
        '南京市浦口区珍七路9号（佛手湖）', '南京',
        '四方当代美术馆由美国建筑师 Steven Holl 设计，坐落于老山森林公园，以当代艺术展览与建筑实验著称。',
        '周二到周日 9:00-17:00', 1, 1, 0, 2),

       ('金陵美术馆', '/static/mock/gallery/jinling.jpg',
        '南京市秦淮区剪子巷50号（老门东）', '南京',
        '金陵美术馆位于老门东历史文化街区，聚焦金陵画派与当代艺术的城市对话。',
        '周二到周日 9:00-17:00', 1, 1, 0, 1),

       ('南京艺术学院美术馆', '/static/mock/gallery/nua.jpg',
        '南京市鼓楼区虎踞北路15号（南京艺术学院内）', '南京',
        '南京艺术学院美术馆以学院派艺术研究与青年艺术家扶持为特色。',
        '周二到周日 9:00-17:00', 0, 0, 0, 2),

       -- 另一个城市，用于验证城市字典 / 城市筛选
       ('苏州美术馆', '/static/mock/gallery/suzhou.jpg',
        '苏州市姑苏区人民路2075号', '苏州',
        '苏州美术馆坐落于苏州古城，以近现代书画与江南地域艺术的收藏研究为特色。',
        '周二到周日 9:00-17:00', 0, 0, 0, 1);

-- =============================================================================
-- 3. 展览
--    正在展出：start_date <= CURDATE() <= end_date
--    即将展出：start_date > CURDATE()
--    往期展出：end_date < CURDATE()
-- =============================================================================
INSERT INTO `exhibition` (`gallery_id`, `title`, `poster_image`, `images`,
                          `start_date`, `end_date`, `open_time`, `description`,
                          `is_hot`, `sort_order`, `type`, `price`, `total_stock`, `sold_count`)
VALUES
-- ① 热门 + 正在展出（首页「当前热门展览」会显示）
(1, '咫尺千里', '/static/mock/exhibition/zhichi.jpg',
 '/static/mock/installation/zhichi-1.jpg,/static/mock/installation/zhichi-2.jpg',
 DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 60 DAY), '周二到周日 8:30-18:30',
 '陆俨少是近现代山水画大家，在山水笔墨上，构筑起陆俨少一帜。本次「咫尺千里」展，汇集陆俨少手卷、册页、扇面三类小品佳作，作品画幅虽小，却可容纳千山万壑，于方寸之间铺展千里山河。',
 1, 100, 2, 58.00, 500, 0),

-- ② 热门 + 正在展出
(1, '江苏水印木刻实践与研究特展', '/static/mock/exhibition/water-print.jpg',
 NULL,
 DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 50 DAY), '周二到周日 8:30-18:30',
 '本次特展系统梳理江苏水印木刻的实践脉络与研究成果，展出各时期代表性版画作品与文献资料。',
 1, 95, 3, 38.00, 300, 0),

-- ③ 非热门 + 正在展出（美术馆详情页「正在展出」会显示，首页热门不显示）
(1, '典藏精品陈列', '/static/mock/exhibition/collection.jpg',
 NULL,
 DATE_SUB(CURDATE(), INTERVAL 25 DAY), DATE_ADD(CURDATE(), INTERVAL 55 DAY), '周二到周日 8:30-18:30',
 '从馆藏近 14000 件作品中精选各门类代表作，呈现中国近现代美术的发展脉络。',
 0, 50, 2, 58.00, 400, 0),

-- ④ 即将展出
(3, '内太空飞行', '/static/mock/exhibition/inner-space.jpg',
 NULL,
 DATE_ADD(CURDATE(), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 75 DAY), '周二到周日 9:00-17:00',
 '以黑白摄影为媒介，探讨身体、空间与感知边界之间的张力。',
 0, 40, 4, 68.00, 200, 0),

-- ⑤ 往期展出（美术馆详情页「往期展出」会显示）
(4, '江山艺术展', '/static/mock/exhibition/jiangshan.jpg',
 NULL,
 DATE_SUB(CURDATE(), INTERVAL 120 DAY), DATE_SUB(CURDATE(), INTERVAL 30 DAY), '周二到周日 9:00-17:00',
 '以山水为主题，呈现当代艺术家对传统山水图式的再诠释。',
 0, 30, 2, 63.00, 300, 0),

-- ⑥ 往期展出
(4, '神华丹青', '/static/mock/exhibition/shenhua.jpg',
 NULL,
 DATE_SUB(CURDATE(), INTERVAL 100 DAY), DATE_SUB(CURDATE(), INTERVAL 20 DAY), '周二到周日 9:00-17:00',
 '汇集当代工笔重彩佳作，展现丹青传统的当代表达。',
 0, 20, 2, 0.00, 300, 0);

-- =============================================================================
-- 4. 作品集（挂在展览 1「咫尺千里」下）
-- =============================================================================
INSERT INTO `exhibition_work` (`exhibition_id`, `name`, `author`, `image`, `description`, `sort`)
VALUES (1, '策杖行吟图（扇面局部图）', '陆俨少', '/static/mock/work/cezhang.jpg',
        '此幅为扇面山水局部，绘古装隐士持杖漫步于江岸郊野，人物神态怡然，衣纹线条简练洒脱；远山以简笔勾写山石草木，意境清旷淡远。画家将文人寄情山水、漫步寻幽的心境凝于方寸扇面，于简约笔墨间，传递传统山水可游可居的诗意。',
        100),
       (1, '云涧水涌图（扇面局部）', '陆俨少', '/static/mock/work/yunjian.jpg',
        '此作尽显陆俨少标志性的勾水技法，层层回旋的水涡灵动生动，婉转线条描摹山间流动云气，山石浓彩晕染，与云水相互交融。于扇面方寸之间捕捉湍水涌动、云气缭绕的瞬间，将山水流动不息的气韵留存纸上。',
        90),
       (1, '江上渔舟图（扇面局部）', '陆俨少', '/static/mock/work/yuzhou.jpg',
        '画面以细密灵动的线条勾写潺潺水波，水纹层层叠卷，满纸皆是江水流动之势，一叶渔舟轻浮于浩渺江面，造型简约寥寥数笔即成，以极简意象烘托江阔天遥，营造出烟波江上清旷辽远的诗歌意境。',
        80);

-- =============================================================================
-- 5. 执行后校验
-- =============================================================================
-- ① 各表行数
--   SELECT 'banner' t, COUNT(*) c FROM banner
--   UNION ALL SELECT 'gallery', COUNT(*) FROM gallery
--   UNION ALL SELECT 'exhibition', COUNT(*) FROM exhibition
--   UNION ALL SELECT 'exhibition_work', COUNT(*) FROM exhibition_work;
--
-- ② 展期状态分布（首页/详情页 tab 的依据）
--   SELECT
--     SUM(start_date <= CURDATE() AND end_date >= CURDATE()) AS 正在展出,
--     SUM(start_date > CURDATE())                            AS 即将展出,
--     SUM(end_date < CURDATE())                              AS 往期展出
--   FROM exhibition;
--
-- ③ 首页「当前热门展览」应有的结果（应返回 2 条：咫尺千里、江苏水印木刻）
--   SELECT e.id, e.title, e.price, g.name AS gallery_name
--   FROM exhibition e JOIN gallery g ON g.id = e.gallery_id
--   WHERE e.is_hot = 1 AND e.start_date <= CURDATE() AND e.end_date >= CURDATE()
--   ORDER BY e.sort_order DESC;
--
-- ④ 首页「热门美术馆」应有的结果（应返回 4 条）
--   SELECT id, name, address, exhibition_count FROM gallery
--   WHERE is_hot = 1 AND status = 0 ORDER BY exhibition_count DESC;
