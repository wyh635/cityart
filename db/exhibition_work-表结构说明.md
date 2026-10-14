# exhibition_work —— 展览作品集表

> 所属项目：艺览 ciTY
> 数据库：`city_art` ｜ 引擎：InnoDB ｜ 字符集：`utf8mb4 / utf8mb4_unicode_ci`
> 建表脚本：[01_create_tables.sql](01_create_tables.sql) ｜ 增量脚本：[02_alter_existing.sql](02_alter_existing.sql)

---

## 一、表的作用

存放**展览中展出的具体作品**。对应展览详情页的「作品集 / COLLECTION」区块 —— 每件作品一张卡片，左侧是作品图，右侧是作品名称、作者、简介。

**对应原型区块**：展览详情页 — 作品集 / COLLECTION

**为什么单独建表**：一场展览有 N 件作品（原型上就是「上一页 1 2 3 下一页」分页显示），属于**一对多**关系。把作品信息塞进 `exhibition.description` 是不行的 —— 无法排序、无法分页、无法按作品查询。

---

## 二、字段结构

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键ID |
| `exhibition_id` | BIGINT UNSIGNED | 否 | — | 所属展览，关联 `exhibition.id` |
| `name` | VARCHAR(200) | 否 | — | 作品名称 |
| `author` | VARCHAR(100) | 是 | NULL | 作者姓名 |
| `image` | VARCHAR(500) | 是 | NULL | 作品图片地址 |
| `description` | TEXT | 是 | NULL | 作品简介 |
| `sort` | INT | 否 | 0 | 排序，数字越大越靠前 |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE | 最近更新时间 |

### 字段详解

**`exhibition_id` — 所属展览（外键）**

关联 `exhibition.id`。查询时**必须带这个条件**，否则会把所有展览的作品混在一起返回。

> 本表**不建物理外键**，关系由应用层保证 + `idx_exhibition` 索引支撑（与项目其他表一致，避免高并发下的锁竞争）。

**`name` — 作品名称**

原型示例：「策杖行吟图（扇面局部图）」。

> 括号里的「扇面局部图」是作品名的一部分，**不拆字段**。

**`author` — 作者 ⭐**

原型示例：「陆俨少」。**单独成列，不要塞进 `description`**。

- 原型上「作者」是独立一行、加粗显示，和下方简介是两种排版
- 独立列才能支持「按艺术家筛选作品」「统计某艺术家的参展作品数」这类需求

**`image` — 作品图**

详情页左侧那张画作图片。建议宽高比统一，避免前端列表错位。

**`description` — 作品简介**

原型示例：「此幅为扇面山水局部，绘古装隐士持杖漫步于江岸郊野…」

用 `TEXT`，简介有长有短，**别用 VARCHAR(255) 卡死**。

**`sort` — 排序**

同场展览内多件作品的展示顺序，越大越靠前。建议以 10 为间隔（100、90、80…），方便中间插作品。

---

## 三、索引

| 索引名 | 类型 | 字段 | 用途 |
|---|---|---|---|
| `PRIMARY` | 主键 | `id` | 主键查询 |
| `idx_exhibition` | 组合 | `exhibition_id, sort` | 按展览查作品 + 排序 |

**为什么是组合索引**：详情页的查询固定是

```sql
WHERE exhibition_id = ? ORDER BY sort DESC
```

`exhibition_id` 过滤 + `sort` 排序，组合索引一次搞定，避免 `filesort`。

---

## 四、常用查询

```sql
-- ① 展览详情页：取某展览的作品集（按排序倒序）
SELECT id, name, author, image, description
FROM exhibition_work
WHERE exhibition_id = #{exhibitionId}
ORDER BY sort DESC, id ASC;

-- ② 分页版（作品多于一屏时，原型底部有分页控件）
SELECT id, name, author, image, description
FROM exhibition_work
WHERE exhibition_id = #{exhibitionId}
ORDER BY sort DESC, id ASC
LIMIT #{offset}, #{pageSize};

-- ③ 统计某展览的作品数量
SELECT COUNT(*) FROM exhibition_work WHERE exhibition_id = #{exhibitionId};
```

---

## 五、业务规则

| 规则 | 实现方式 |
|---|---|
| 作品必须归属于某个展览 | `exhibition_id NOT NULL` |
| 作品顺序可调 | `sort` 字段，`ORDER BY sort DESC` |
| 分页浏览 | `LIMIT` + `idx_exhibition` 索引 |
| 同一展览作品名可重复 | 未加唯一索引（不同展览可能有同名作品，且同名也属正常） |
| 删除展览时处理作品 | 应用层先删作品，或置为不可见；**不建议建 ON DELETE CASCADE** |

---

## 六、设计说明

1. **不存 `exhibition_title`** —— 标题从 `exhibition` 表 JOIN 取。冗余了以后改展览名就会出现不一致。
2. **不做图片多图** —— 一件作品一张主图。若以后要「多角度细节图」，再加 `images` 字段或拆 `work_image` 表。
3. **`author` 不做艺术家表关联** —— 当前项目没有 `artist` 表，作者直接存姓名文本。若以后要做「艺术家主页」，再拆出 `artist` 表并用 `artist_id` 关联。
4. **`create_time` / `update_time` 都是本表自带** —— 新建表，无历史包袱。

---

## 七、相关表

```
exhibition (1) ──── (N) exhibition_work
```

| 关联表 | 关联字段 | 说明 |
|---|---|---|
| `exhibition` | `exhibition_work.exhibition_id → exhibition.id` | 作品所属展览（**父表**） |

**插入顺序**：先插 `exhibition` 拿到 `id`，再插本表并带上该 `exhibition_id`。

**给测试造数据时的注意点**：

```sql
-- 先确认展览存在并拿到 id
SELECT id, title FROM exhibition WHERE title = '咫尺千里';
-- 再插作品
INSERT INTO exhibition_work (exhibition_id, name, author, image, description, sort)
VALUES (1, '策杖行吟图（扇面局部图）', '陆俨少', '/static/mock/work/cezhang.jpg', '作品简介…', 100);
```

> ⚠️ `exhibition_id` 写错不会报错（没有物理外键），但详情页会查不出作品。造完数据记得用上面的查询 ① 验证一遍。
