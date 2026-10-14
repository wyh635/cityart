# gallery —— 美术馆表

> 所属项目：艺览 ciTY
> 数据库：`city_art` ｜ 引擎：InnoDB ｜ 字符集：`utf8mb4 / utf8mb4_unicode_ci`
> 建表脚本：[01_create_tables.sql](01_create_tables.sql) ｜ 增量脚本：[02_alter_existing.sql](02_alter_existing.sql)

---

## 一、表的作用

存放**美术馆基本信息**。首页底部的「热门美术馆」宫格、以及「美术馆详情页」，数据都来自本表。

**对应原型区块**：
- 首页 — 热门美术馆（2×2 宫格卡片）
- 美术馆详情页 — 顶部信息区（馆名、地址、开放时间、封面图）
- 美术馆详情页 — 展馆简介
- 热门美术馆排行页（排行顺序按本表字段算）

---

## 二、字段结构

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键ID |
| `name` | VARCHAR(100) | 否 | — | 美术馆名称 |
| `cover_image` | VARCHAR(255) | 是 | NULL | 封面图（首页卡片用） |
| `address` | VARCHAR(255) | 否 | — | 详细地址，**已含城市与区县** |
| `city` | VARCHAR(50) | 否 | — | 所在城市，用于筛选 |
| `intro` | TEXT | 是 | NULL | 展馆简介长文 |
| `open_time` | VARCHAR(50) | 是 | NULL | 开放时间 |
| `exhibition_count` | INT | 否 | 0 | 当前在展数量（**冗余字段**） |
| `is_hot` | TINYINT | 否 | 0 | 是否热门：0-否 1-是 |
| `status` | TINYINT | 否 | 0 | 状态：0-正常营业 1-闭馆维护 |
| `type` | INT | 否 | 1 | 美术馆类型：1-综合类 2-当代类 3-古典类 4-雕塑类 5-摄影类 |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE | 最近更新时间 |

### 字段详解

**`cover_image` — 唯一的图片**

本表**只存这一张图**，首页卡片和详情页共用。美术馆详情页里出现的展览海报，是从 `exhibition` 表查的，不存本表。

**`address` — 完整地址**

存「南京市玄武区长江路333号（总统府对面）」这种**完整串**，前端直接显示即可。

> ⚠️ 注意：`city` 字段里的城市名**已经在 `address` 里出现过了**，属于冗余。但这是有意保留的 —— `city` 用于筛选（`WHERE city = ?`），`address` 用于展示，职责不同，所以两个都留。

**`exhibition_count` — 当前在展数量（冗余）**

首页卡片上的「当前展览数 1」直接读这个字段，**不是实时 COUNT**。

- 好处：首页一屏 4 个馆，避免 4 次子查询
- 代价：**新增/删除展览、或展览过期时，这个数字不会自动变**
- 维护方式：二选一
  - 应用层同步：建/删展览时 `UPDATE gallery SET exhibition_count = ...`
  - 定时任务：每天凌晨批量重算（**推荐**，简单可靠）

**`is_hot` — 是否热门**

首页「热门美术馆」宫格按它筛选：`WHERE is_hot = 1 AND status = 0`。

- `is_hot = 1`：运营手动挑选，**上首页的 4 个馆**
- `is_hot = 0`：有详情页、能搜到，但不上首页

排序在 `is_hot = 1` 内部按 `exhibition_count DESC`。

**`status` — 营业状态**

`0` = 正常营业（展示），`1` = 闭馆维护（不展示）。**首页和列表页都要带 `status = 0` 条件。**

**`intro` — 展馆简介**

美术馆详情页「展馆简介 / INTRODUCTION」那一大段文字。用 `TEXT` 类型，不限长度。

**`type` — 美术馆类型**

对应「美术馆列表页」顶部筛选栏的**「美术馆类型」下拉**：

| 值 | 含义 | 前端下拉显示 |
|---|---|---|
| 1 | 综合类 | 综合类美术馆 |
| 2 | 当代类 | 当代类美术馆 |
| 3 | 古典类 | 古典类美术馆 |
| 4 | 雕塑类 | 雕塑类美术馆 |
| 5 | 摄影类 | 摄影类美术馆 |

- 「全部类型」不占值，由前端不传 `type` 参数实现
- 未指定类型的馆，默认存 `1`（综合类）
- 类型是**硬编码数字**，新增类型需要改代码；若以后要让运营自助维护，需拆出 `gallery_category` 分类表

**`open_time` — 开放时间**

存字符串「周二到周日 8:30-18:30」而不是拆成开始/结束时间，因为：
- 展示时就是一整串文本
- 真实的开放时间往往不规则（周一闭馆、节假日调整），结构化存储反而麻烦

---

## 三、索引

| 索引名 | 类型 | 字段 | 用途 |
|---|---|---|---|
| `PRIMARY` | 主键 | `id` | 主键查询 |
| `idx_city` | 普通 | `city` | 按城市筛选 |
| `idx_hot` | 组合 | `is_hot, exhibition_count` | 首页热门美术馆查询与排序 |
| `idx_type` | 普通 | `type` | 美术馆列表页按类型筛选 |

**为什么 `idx_hot` 是组合索引**：首页的查询是

```sql
WHERE is_hot = 1 AND status = 0 ORDER BY exhibition_count DESC
```

`is_hot` 过滤 + `exhibition_count` 排序，组合索引能同时覆盖「过滤」和「排序」，避免 `filesort`。

---

## 四、常用查询

```sql
-- ① 首页「热门美术馆」（2×2 宫格，取 4 条）
SELECT id, name, cover_image, address, open_time, exhibition_count
FROM gallery
WHERE is_hot = 1 AND status = 0
ORDER BY exhibition_count DESC, id ASC
LIMIT 4;

-- ② 美术馆列表分页（按城市 + 类型筛选，搜索名称）
SELECT id, name, cover_image, address, exhibition_count
FROM gallery
WHERE status = 0
  AND (#{city} IS NULL OR city = #{city})
  AND (#{type} IS NULL OR type = #{type})
  AND (#{keyword} IS NULL OR name LIKE CONCAT('%', #{keyword}, '%'))
ORDER BY exhibition_count DESC, id ASC
LIMIT #{offset}, #{pageSize};

-- ③ 热门美术馆排行（NO.1 ~ NO.N）
SELECT id, name, cover_image, address, exhibition_count
FROM gallery
WHERE status = 0
ORDER BY exhibition_count DESC, id ASC;

-- ④ 重算在展数量（定时任务用）
UPDATE gallery g
SET g.exhibition_count = (
    SELECT COUNT(*) FROM exhibition e
    WHERE e.gallery_id = g.id
      AND e.start_date <= CURDATE() AND e.end_date >= CURDATE()
);
```

---

## 五、业务规则

| 规则 | 实现方式 |
|---|---|
| 闭馆的馆不展示 | 所有查询带 `WHERE status = 0` |
| 首页只显示运营挑选的馆 | `WHERE is_hot = 1` |
| 按城市筛选 | `idx_city` 索引 |
| 按馆类型筛选 | `idx_type` 索引，`type` 传空表示「全部类型」 |
| 在展数量快捷展示 | `exhibition_count` 冗余字段 + 定时任务重算 |
| 名称不重复 | 应用层校验（本表未加唯一索引，同名馆在不同城市可能合法） |

---

## 六、设计说明

1. **本表只有一张图片**（`cover_image`）。美术馆详情页的展览海报全部来自 `exhibition` 表，不冗余存到本表。
2. **没有 `district`（区县）字段**：区县信息已包含在 `address` 中，单独存一份属于重复。如果以后需要「按区县筛选」，再加回来。
3. **`type`（馆类型）保留**：美术馆列表页顶部有「美术馆类型」筛选栏（综合类 / 当代类 / 古典类 / 雕塑类 / 摄影类），因此保留该字段并加 `idx_type` 索引。
4. **没有 `phone`（咨询电话）字段**：当前设计不需要。
5. **没有 `images`（相册）字段**：本表只存 `cover_image` 一张图，展览海报从 `exhibition` 表查。

---

## 七、相关表

```
gallery (1) ──── (N) exhibition
```

| 关联表 | 关联字段 | 说明 |
|---|---|---|
| `exhibition` | `exhibition.gallery_id → gallery.id` | 一个美术馆有多场展览 |

**下游影响**：
- 删除美术馆前，必须处理其名下的展览（`exhibition.gallery_id`），否则会出现「孤儿展览」（查不到馆名）
- `exhibition_count` 依赖 `exhibition` 表的数据准确性
