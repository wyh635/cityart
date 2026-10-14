# exhibition —— 展览表

> 所属项目：艺览 ciTY
> 数据库：`city_art` ｜ 引擎：InnoDB ｜ 字符集：`utf8mb4 / utf8mb4_unicode_ci`
> 建表脚本：[01_create_tables.sql](01_create_tables.sql) ｜ 增量脚本：[02_alter_existing.sql](02_alter_existing.sql)

---

## 一、表的作用

存放**展览信息**，是整个项目最核心、被引用最多的表。首页热门展览、展览详情页、美术馆详情页里的展览卡片、票务下单，全都围绕本表。

**对应原型区块**：
- 首页 — 当前热门展览（横排海报）
- 首页 — 资料卡（海报 + 标题 + 英文副标题 + 展期）
- 展览详情页 — 头部（标题、展期、地址、开放时间、票价、购票按钮）
- 展览详情页 — 展览简介
- 展览详情页 — 展览实景
- 美术馆详情页 — 正在展出 / 往期展出 列表
- 购票流程 — 票价与库存

---

## 二、字段结构

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键ID |
| `gallery_id` | BIGINT UNSIGNED | 否 | — | 所属美术馆，关联 `gallery.id` |
| `title` | VARCHAR(200) | 否 | — | 展览标题 |
| `poster_image` | VARCHAR(255) | 否 | — | 封面海报图 |
| `images` | VARCHAR(1000) | 是 | NULL | 展览实景图，多张逗号分隔 |
| `start_date` | DATE | 否 | — | 开展日期 |
| `end_date` | DATE | 否 | — | 闭展日期 |
| `open_time` | VARCHAR(50) | 是 | NULL | 展览开放时间 |
| `description` | TEXT | 是 | NULL | 展览简介长文 |
| `is_hot` | TINYINT | 否 | 0 | 是否热门：0-否 1-是 |
| `sort_order` | INT | 否 | 0 | 排序权重 |
| `type` | INT | 否 | 1 | 展览分类：1-综合展览 2-当代展览 3-古典展览 4-雕塑展览 5-摄影展览 |
| `price` | DECIMAL(10,2) | 是 | NULL | 票价（单一定价） |
| `total_stock` | INT | 否 | 0 | 总库存 |
| `sold_count` | INT | 否 | 0 | 已售数量 |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE | 最近更新时间 |

### 字段详解

**`gallery_id` — 所属美术馆**

关联 `gallery.id`。展览详情页上的**地址**（「南京市玄武区长江路333号」）就是靠它 JOIN `gallery` 表取出来的，本表**不重复存地址**。

**`poster_image` — 封面海报**

首页卡片、美术馆详情页卡片、展览详情页大图**共用这一张**。

**`images` — 展览实景图**

对应详情页「展览实景 / INSTALLATION」区块的 2 张展厅照片。多张用英文逗号分隔：

```
/static/mock/installation/zhichi-1.jpg,/static/mock/installation/zhichi-2.jpg
```

> 💡 这是**简单实现**。如果以后需要给单张图加排序、说明文字，应拆出 `exhibition_image` 表。

**`start_date` / `end_date` — 展期**

两个独立的 `DATE` 字段（不是 DATETIME），分别存开展、闭展日期。

- 用 `DATE` 不用 `DATETIME`：`05.01` 只需要年月日，用 DATETIME 会存成 `2026-05-01 00:00:00`，判断「正在展出」时还要处理时分秒，纯属添麻烦
- 前端展示时自己拼成 `05.01—08.02`

**⭐ 展出状态不落库，由日期实时判断**

本表**没有**表示「正在进行 / 即将开始 / 已结束」的字段，全部实时算：

| 状态 | 判断条件 |
|---|---|
| 正在展出 | `start_date <= CURDATE() <= end_date` |
| 即将展出 | `start_date > CURDATE()` |
| 往期展出 | `end_date < CURDATE()` |

好处：**不需要任何维护**，展览到期自动归入「往期」，不用定时任务改状态。

> ⚠️ 注意区分：本表若存在 `status` 字段（部分环境有），那是**后台上下架**状态，**不参与展期判断**，两者语义不同。

**`is_hot` — 是否热门**

首页「当前热门展览」区块按它筛选。完整条件是：

```sql
WHERE is_hot = 1 AND start_date <= CURDATE() AND end_date >= CURDATE()
```

即：**既被运营标记为热门，又确实在展期内**。

**`sort_order` — 排序权重**

同一个区块内多张卡片的顺序，越大越靠前。

**`type` — 展览分类**

用于「全部展览」列表页顶部**「展览类型」下拉**的筛选。

| 值 | 含义 | 前端下拉显示 |
|---|---|---|
| 1 | 综合展览 | 综合展览 |
| 2 | 当代展览 | 当代展览 |
| 3 | 古典展览 | 古典展览 |
| 4 | 雕塑展览 | 雕塑展览 |
| 5 | 摄影展览 | 摄影展览 |

- 「全部类型」**不占值**，由前端不传 `type` 参数实现
- 未指定分类的展览，默认存 `1`（综合展览）
- 分类是**硬编码数字**。中文名由后端字典接口返回（见 [api-docs/03全部展览与美术馆列表接口文档.md](../api-docs/03全部展览与美术馆列表接口文档.md)），前端不硬编码
- 若以后要让运营自助维护分类，需拆出 `exhibition_category` 分类表

---

## 三、票务字段（重点）

本项目**票价单一定价**，一场展览一个价格，所以**不设票种表**，三个字段都在本表：

| 字段 | 说明 |
|---|---|
| `price` | 票价（元），如 `58.00` |
| `total_stock` | 总库存（票数上限） |
| `sold_count` | 已售数量（下单 +quantity，退款/取消 -quantity） |

**剩余库存计算**：

```
剩余库存 = total_stock - sold_count
```

**防超卖下单**（核心 SQL）：

```sql
UPDATE exhibition
SET sold_count = sold_count + #{quantity}
WHERE id = #{exhibitionId}
  AND total_stock - sold_count >= #{quantity};
-- 受影响行数 = 0 → 抛「库存不足」异常，事务回滚
```

> 🔑 **必须用「条件更新 + 判断影响行数」的写法**：靠数据库行锁保证原子性，比「先查库存再更新」可靠。后者在并发下会超卖。

**回滚库存**（取消订单 / 退款）：

```sql
UPDATE exhibition SET sold_count = sold_count - #{quantity} WHERE id = #{exhibitionId};
```

> ⚠️ 若以后要分「成人票 / 学生票」不同价格，必须拆出 `ticket_type` 表，把 `price` / `total_stock` / `sold_count` 都挪过去。**当前不需要。**

---

## 四、索引

| 索引名 | 类型 | 字段 | 用途 |
|---|---|---|---|
| `PRIMARY` | 主键 | `id` | 主键查询 |
| `idx_gallery` | 普通 | `gallery_id` | 查某馆的所有展览 |
| `idx_dates` | 组合 | `start_date, end_date` | 按展期筛选（正在/即将/往期） |
| `idx_hot` | 组合 | `is_hot, sort_order` | 首页热门展览查询与排序 |
| `idx_type` | 普通 | `type` | 全部展览列表页按展览类型筛选 |

---

## 五、常用查询

```sql
-- ① 首页「当前热门展览」
SELECT e.id, e.title, e.poster_image, e.start_date, e.end_date, e.price,
       g.name AS gallery_name
FROM exhibition e
JOIN gallery g ON g.id = e.gallery_id
WHERE e.is_hot = 1
  AND e.start_date <= CURDATE() AND e.end_date >= CURDATE()
ORDER BY e.sort_order DESC;

-- ② 全部展览分页（正在展出）
SELECT e.id, e.title, e.poster_image, e.start_date, e.end_date, e.price,
       g.name AS gallery_name
FROM exhibition e
JOIN gallery g ON g.id = e.gallery_id
WHERE e.start_date <= CURDATE() AND e.end_date >= CURDATE()
  AND (#{city} IS NULL OR g.city = #{city})
  AND (#{type} IS NULL OR e.type = #{type})
ORDER BY e.is_hot DESC, e.sort_order DESC, e.start_date ASC, e.id ASC
LIMIT #{offset}, #{pageSize};
-- 即将展出：e.start_date > CURDATE()
-- 往期展出：e.end_date < CURDATE()

-- ③ 美术馆详情页：该馆的展览（分「正在展出」「往期展出」两组）
SELECT id, title, poster_image, start_date, end_date, price,
       CASE WHEN start_date > CURDATE() THEN 'future'
            WHEN end_date   < CURDATE() THEN 'past'
            ELSE 'current' END AS status
FROM exhibition
WHERE gallery_id = #{galleryId}
ORDER BY start_date DESC;

-- ④ 展览详情（含美术馆信息）
SELECT e.*, g.name AS gallery_name, g.address AS gallery_address,
       g.open_time AS gallery_open_time
FROM exhibition e
JOIN gallery g ON g.id = e.gallery_id
WHERE e.id = #{id};

-- ⑤ 重算各馆在展数量（配合 gallery.exhibition_count）
UPDATE gallery g
SET g.exhibition_count = (
    SELECT COUNT(*) FROM exhibition e
    WHERE e.gallery_id = g.id
      AND e.start_date <= CURDATE() AND e.end_date >= CURDATE()
);
```

---

## 六、业务规则

| 规则 | 实现方式 |
|---|---|
| 展出状态 | **不落库**，由 `start_date` / `end_date` 与 `CURDATE()` 实时比较 |
| 首页热门展览 | `is_hot = 1` **且** 在展期内 |
| 不允许超卖 | `UPDATE ... WHERE total_stock - sold_count >= ?`，判断影响行数 |
| 订单价格不被改价影响 | 下单时对 `price` 做**快照**存进订单明细表 |
| 按展期筛选 | `idx_dates` 索引 |
| 展览过期自动归类 | 日期实时算，无需定时任务 |

---

## 七、设计说明

1. **删除了 `subtitle`（英文副标题）** —— 项目不需要英文标题。前端如需展示副标题，用 `title` 即可。
2. **没有 `venue`（展览场地）字段** —— 详情页的地址统一从 `gallery.address` JOIN 取得，不重复存。
3. **`images` 用逗号分隔存多图** —— 简单实现，够用；需要单图元信息时再拆表。
4. **`price` 允许为 NULL** —— 免费展览存 `0.00` 或 NULL 都行，前端遇到 `0` 显示「免费」。

---

## 八、相关表

```
gallery (1) ──── (N) exhibition
exhibition (1) ──── (N) exhibition_work
```

| 关联表 | 关联字段 | 说明 |
|---|---|---|
| `gallery` | `exhibition.gallery_id → gallery.id` | 展览所属美术馆（**父表**） |
| `exhibition_work` | `exhibition_work.exhibition_id → exhibition.id` | 展览的作品集（**子表**） |

**下游影响**：
- 删除展览前，必须先删除其作品集（`exhibition_work`），否则会出现孤儿作品
- 若展览已被订单引用，**不应物理删除**，改为下架或标记
- 展览增删会影响 `gallery.exhibition_count` 冗余字段的准确性

---

## 九、本表的典型坑

| 坑 | 说明 |
|---|---|
| 用 `DATETIME` 存展期 | 判断「正在展出」时要处理时分秒，容易漏掉当天 |
| 用三个独立 `<if>` 拼日期条件 | 应改 `<choose>` 保证三选一互斥，否则会查出往期数据 |
| `ORDER BY` 没有唯一列兜底 | 翻页时会出现重复记录，末尾必须加 `, id ASC` |
| 循环里查馆名 | N+1 问题，应 JOIN 或批量查 |
| 「先查库存再更新」 | 并发下超卖，必须用条件更新 |
