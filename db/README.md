# db —— 数据库设计

> 艺览 ciTY 项目数据库脚本。数据库名：`city_art`，引擎 InnoDB，字符集 `utf8mb4 / utf8mb4_unicode_ci`。

## 文件说明

| 文件 | 用途 | 什么时候用 |
|---|---|---|
| `01_create_tables.sql` | 4 张表的完整建表语句（含 `DROP TABLE`） | **全新环境**，从零建库 |
| `02_alter_existing.sql` | 增量变更（`ALTER`，不删表、不丢数据） | **已有旧表**的库，只想改结构 |
| `03_rollback.sql` | 回滚到变更前的结构 | 执行 `02` 之后想反悔 |
| `04_init_data.sql` | 初始化测试数据 | 本地开发造数据 |

> ⚠️ `01` 和 `02` **二选一**，不要都执行。

## 本次设计的 4 张表

| # | 表名 | 用途 | 状态 | 表结构文档 |
|---|---|---|---|---|
| 1 | `banner` | 首页轮播图 | 已有表，新增 `update_time` | [banner-表结构说明.md](banner-表结构说明.md) |
| 2 | `gallery` | 美术馆（首页热门美术馆 + 美术馆详情页 + 美术馆列表页） | 已有表，删 `district`，加 `open_time`/`is_hot`（保留 `type`） | [gallery-表结构说明.md](gallery-表结构说明.md) |
| 3 | `exhibition` | 展览（首页热门展览 + 展览详情页 + 场馆内展览列表） | 已有表，删 `subtitle`，加 `images`/`open_time` | [exhibition-表结构说明.md](exhibition-表结构说明.md) |
| 4 | `exhibition_work` | 作品集（展览详情页「作品集」区块） | **新建** | [exhibition_work-表结构说明.md](exhibition_work-表结构说明.md) |

每份表结构文档都包含：表的作用与对应原型区块、字段结构表、索引设计、常用查询 SQL、业务规则、设计说明、相关表与典型坑。

**表关系**

```
gallery (1) ──< exhibition (N)          exhibition.gallery_id
exhibition (1) ──< exhibition_work (N)  exhibition_work.exhibition_id
```

## 三条设计约定

1. **「艺览智荐」不落库** —— 该区块由算法接口直接返回推荐结果，项目侧不建表、不查库。
2. **票价单一定价，不拆票种表** —— 一场展览一个价格，`price` / `total_stock` / `sold_count` 全在 `exhibition` 表。
   - 剩余库存 = `total_stock - sold_count`
   - 防超卖：`UPDATE exhibition SET sold_count = sold_count + #{n} WHERE id = #{id} AND total_stock - sold_count >= #{n}`
3. **展期状态由日期实时计算，不落库** —— 不设 `status` 字段表示展出状态：
   | 状态 | 判断条件 |
   |---|---|
   | 正在展出 | `start_date <= CURDATE() <= end_date` |
   | 即将展出 | `start_date > CURDATE()` |
   | 往期展出 | `end_date < CURDATE()` |

## 页面区块与表对应

| 页面区块 | 数据来源 |
|---|---|
| 首页 — 轮播图 | `banner ORDER BY sort DESC` |
| 首页 — 当前热门展览 | `exhibition WHERE is_hot = 1` + 展期判断 |
| 首页 — 艺览智荐 | 算法接口（不查库） |
| 首页 — 热门美术馆 | `gallery WHERE is_hot = 1 AND status = 0` |
| 展览详情 — 头部 | `exhibition` + JOIN `gallery`（取 address / open_time） |
| 展览详情 — 展览简介 | `exhibition.description` |
| 展览详情 — 展览实景 | `exhibition.images`（英文逗号分隔多图） |
| 展览详情 — 作品集 | `exhibition_work WHERE exhibition_id = ? ORDER BY sort DESC` |
| 美术馆详情 — 顶部 | `gallery` |
| 美术馆详情 — 展馆简介 | `gallery.intro` |
| 美术馆详情 — 正在/往期展出 | `exhibition WHERE gallery_id = ?` + 展期判断 |

## 执行方式

```bash
# 方式一：命令行
mysql -u root -p < db/01_create_tables.sql
mysql -u root -p city_art < db/04_init_data.sql

# 方式二：IDEA Database 面板里直接打开 .sql 文件，选中语句执行
```

## 已知待办

- `gallery.exhibition_count` 是**冗余字段**，新增/删除展览或展览过期时不会自动更新，需由应用层同步或定时任务重算。
- `exhibition.images` 用逗号分隔存多图，属于简单实现；若以后需要单图排序、单图说明，应拆出 `exhibition_image` 表。
