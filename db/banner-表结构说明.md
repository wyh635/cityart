# banner —— 首页轮播图表

> 所属项目：艺览 ciTY
> 数据库：`city_art` ｜ 引擎：InnoDB ｜ 字符集：`utf8mb4 / utf8mb4_unicode_ci`
> 建表脚本：[01_create_tables.sql](01_create_tables.sql) ｜ 增量脚本：[02_alter_existing.sql](02_alter_existing.sql)

---

## 一、表的作用

存放**首页顶部轮播图**。首页第一屏的整张大图（如「江苏水印木刻实践与研究特展」）就是本表的一条记录，由管理员在后台维护。

**对应原型区块**：首页 — 轮播图（最顶部大图）

---

## 二、字段结构

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键ID |
| `image_url` | VARCHAR(500) | 否 | — | 轮播大图地址，**文案直接烧入图中** |
| `title` | VARCHAR(100) | 是 | `''` | 标题，**仅供后台识别，页面不展示** |
| `sort` | INT | 否 | 0 | 排序，数字越大越靠前 |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE | 最近更新时间 |

### 字段详解

**`image_url` — 轮播大图**

原型图上的「展览地点：江苏省美术馆新馆负一层」「主办：江苏省美术馆」「承办：…」这些文字，**全部烧进图片里**，不单独建字段存。

- 理由：运营用设计软件做一张图，比在后台填 3 个输入框更快、排版更可控
- 图片建议尺寸：宽 750px 以上（小程序全屏横幅比例约 375×200 ~ 375×240）

**`title` — 标题**

页面上**不显示**。作用是后台列表里让运营一眼看清「这张图是哪个展览的」，避免传错图。

**`sort` — 排序**

首页轮播按 `sort DESC` 取，数字越大越靠前。建议以 10 为间隔设置（100、90、80…），中间好插新图。

---

## 三、索引

| 索引名 | 类型 | 字段 | 用途 |
|---|---|---|---|
| `PRIMARY` | 主键 | `id` | 主键查询 |
| `idx_sort` | 普通 | `sort` | 首页按排序取轮播图 |

---

## 四、常用查询

```sql
-- 首页轮播图列表（按排序倒序）
SELECT id, image_url, title
FROM banner
ORDER BY sort DESC;
```

> 本表数据量极小（通常不超过 10 条），可以不缓存、不加分页。

---

## 五、业务规则

| 规则 | 实现方式 |
|---|---|
| 轮播顺序可控 | `ORDER BY sort DESC` |
| 图片必填 | `image_url NOT NULL` |
| 后台可增删改 | 管理端接口操作，无 C 端写入 |

---

## 六、设计说明

1. **没有 `status` 字段**：轮播图不需要上下架状态，不需要就直接删记录。如果以后要「定时上下线」，再加 `status` + `start_time` / `end_time`。
2. **没有点击跳转字段**：原型的轮播图不可点击。如果以后要跳转到展览详情页，需加 `link_type`（1-展览 2-美术馆 3-外链）+ `link_id` / `link_url`。
3. **`update_time` 是本项目新增列**：旧表只有 `create_time`，见 [02_alter_existing.sql](02_alter_existing.sql) 第 1 节。

---

## 七、相关表

本表**无外键关联**，独立存在。

- 轮播图上展示的展览内容，来自手动上传的图片，不与 `exhibition` 表关联
- 若以后要「点轮播图跳展览详情」，则需要根据 `link_id` 关联 `exhibition.id`
