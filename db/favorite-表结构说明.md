# favorite —— 用户收藏表

> 所属项目：艺览 ciTY
> 数据库：`city_art` ｜ 引擎：InnoDB ｜ 字符集：`utf8mb4 / utf8mb4_unicode_ci`
> 建表脚本：[01_create_tables.sql](01_create_tables.sql) ｜ 增量脚本：[05_order_favorite_tables.sql](05_order_favorite_tables.sql)

---

## 一、表的作用

存放用户的**收藏记录**，用**一张表**同时支持「收藏展览」和「收藏美术馆」两种对象。

**对应原型区块**：
- 收藏列表页 —— 顶部「展览 / 美术馆」双 tab
- 个人中心 —— 数据统计「我的收藏 4」
- 展览详情页 / 美术馆详情页 —— 右上角「收藏 / 已收藏」按钮

## ⭐ 核心设计：**多态存储**

用 `target_type` 区分收藏的是什么：

| `target_type` | 含义 | `target_id` 指向 |
|---|---|---|
| 1 | 收藏展览 | `exhibition.id` |
| 2 | 收藏美术馆 | `gallery.id` |

**为什么不建两张表**（`favorite_exhibition` + `favorite_gallery`）：两张表字段完全一样，查询要 `UNION`，统计要两次 COUNT。多态一张表，一个 `WHERE user_id = ?` 就拿到全部，统计也是**一条 COUNT**。

---

## 二、字段结构

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键ID |
| `user_id` | BIGINT UNSIGNED | 否 | — | 用户ID，关联 `user.id` |
| `target_type` | TINYINT | 否 | — | 收藏类型：1-展览 2-美术馆 |
| `target_id` | BIGINT UNSIGNED | 否 | — | 收藏对象ID |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | 收藏时间 |
| `update_time` | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE | 最近更新时间 |

### 字段详解

**`target_type` / `target_id` — 多态定位**

两个字段合起来才能确定"收藏了什么"。

- `(1, 101)` = 收藏了 ID 为 101 的展览
- `(2, 3)` = 收藏了 ID 为 3 的美术馆

> ⚠️ **本表不建物理外键** —— 多态列无法指向固定的父表。校验由应用层完成：**收藏前先查目标是否存在**。

**`create_time` — 收藏时间**

收藏列表按它倒序（最近收藏的在前）。

---

## 三、索引

| 索引名 | 类型 | 字段 | 用途 |
|---|---|---|---|
| `PRIMARY` | 主键 | `id` | 主键查询 |
| `uk_user_target` | **唯一** | `user_id, target_type, target_id` | **防重复收藏** ⭐ |
| `idx_target` | 组合 | `target_type, target_id` | 统计某展览/美术馆被收藏数 |
| `idx_user_time` | 组合 | `user_id, create_time` | 收藏列表按时间倒序分页 |

**为什么 `uk_user_target` 是核心**：它保证**同一用户对同一对象只能收藏一次**。这是数据库层面的硬约束，比应用层"先查再插"可靠（并发下不会插入两条）。

---

## 四、常用查询

```sql
-- ① 收藏列表：展览 tab
SELECT f.target_id, f.create_time AS favorite_time,
       e.title, e.poster_image, e.start_date, e.end_date, e.price,
       g.name AS gallery_name
FROM favorite f
JOIN exhibition e ON e.id = f.target_id
JOIN gallery    g ON g.id = e.gallery_id
WHERE f.user_id = #{userId} AND f.target_type = 1
ORDER BY f.create_time DESC, f.id DESC
LIMIT #{offset}, #{pageSize};

-- ② 收藏列表：美术馆 tab
SELECT f.target_id, f.create_time AS favorite_time,
       g.name, g.cover_image, g.address, g.exhibition_count
FROM favorite f
JOIN gallery g ON g.id = f.target_id
WHERE f.user_id = #{userId} AND f.target_type = 2
ORDER BY f.create_time DESC, f.id DESC
LIMIT #{offset}, #{pageSize};

-- ③ 个人中心「我的收藏」统计（展览 + 美术馆合计）
SELECT COUNT(*) FROM favorite WHERE user_id = #{userId};

-- ④ 详情页判断收藏状态
SELECT COUNT(*) FROM favorite
WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId};

-- ⑤ 添加收藏（幂等，重复收藏不报错）
INSERT IGNORE INTO favorite (user_id, target_type, target_id)
VALUES (#{userId}, #{targetType}, #{targetId});

-- ⑥ 取消收藏
DELETE FROM favorite
WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId};

-- ⑦ 统计某展览/美术馆被收藏数
SELECT COUNT(*) FROM favorite WHERE target_type = #{targetType} AND target_id = #{targetId};
```

---

## 五、业务规则

| 规则 | 实现方式 |
|---|---|
| **不可重复收藏** | `uk_user_target` 唯一索引 + `INSERT IGNORE` |
| 重复收藏返回成功（幂等） | 用 `INSERT IGNORE`，或捕获唯一键冲突后当作成功 |
| 收藏列表分 tab | `WHERE target_type = ?` |
| 「我的收藏」是合计 | 不带 `target_type` 的 COUNT |
| 收藏对象被删除时 | 查询用 `JOIN` 过滤 —— 目标不存在则天然不展示，**无需级联删除** |
| 收藏时间倒序 | `ORDER BY create_time DESC` |

---

## 六、设计说明

1. **为什么多态而不是两张表**：见上文「核心设计」。代价是无法用物理外键，需应用层保证 `target_id` 有效。
2. **`idx_target` 的作用**：如果详情页要显示「N 人收藏」，这条索引让 `COUNT(*) WHERE target_type AND target_id` 走索引。
3. **没有 `favorite_count` 冗余列**：早期设计曾在 `gallery` / `exhibition` 加 `favorite_count` 冗余字段。**本设计不再需要** —— 收藏数用 `idx_target` 实时 COUNT 即可（收藏是低频操作，数据量小）。
   > 若后续数据量大、详情页 COUNT 成瓶颈，再加冗余列 + 收藏时同步维护。
4. **收藏不需要软删除**：取消收藏直接 `DELETE`，不需要保留历史。

---

## 七、相关表

```
user (1) ──── (N) favorite ────(多态)──┬── exhibition
                                        └── gallery
```

| 关联表 | 关联方式 | 说明 |
|---|---|---|
| `user` | `favorite.user_id → user.id` | 收藏归属（**唯一有物理语义的关联**） |
| `exhibition` | `target_type = 1` 时 `target_id` 指向 | 多态，无外键 |
| `gallery` | `target_type = 2` 时 `target_id` 指向 | 多态，无外键 |

**下游影响**：

- **详情页的「收藏」按钮**依赖本表。两个详情接口（`/api/app/exhibitions/{id}`、`/api/app/galleries/{id}`）都是**可选登录**：带 token 时用 `countByUserAndTarget` 查本表返回真实 `isFavorited`，游客恒为 `false`。
- 切换收藏调 `POST` / `DELETE /api/app/favorites`，**添加用 `INSERT IGNORE`**，靠 `uk_user_target` 唯一索引做幂等（重复收藏影响行数 0，不报错）。
- 展览 / 美术馆被删除后，本表会留下"悬空"收藏记录。**查询用 JOIN 天然过滤掉**，不会报错；如需清理，可定期跑：

```sql
DELETE f FROM favorite f
LEFT JOIN exhibition e ON f.target_type = 1 AND e.id = f.target_id
LEFT JOIN gallery    g ON f.target_type = 2 AND g.id = f.target_id
WHERE (f.target_type = 1 AND e.id IS NULL)
   OR (f.target_type = 2 AND g.id IS NULL);
```
