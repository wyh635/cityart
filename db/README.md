# db —— 数据库设计

> 艺览 ciTY 项目数据库脚本。数据库名：`city_art`，引擎 InnoDB，字符集 `utf8mb4 / utf8mb4_unicode_ci`。

## 文件说明

| 文件 | 用途 | 什么时候用 |
|---|---|---|
| `01_create_tables.sql` | **8 张业务表**的完整建表语句（含 `DROP TABLE`） | **全新环境**，从零建库 |
| `02_alter_existing.sql` | 首页/详情页模块的增量变更（可重复执行） | **已有库**，只想改结构 |
| `03_rollback.sql` | 回滚到变更前的结构 | ⚠️ 已过时，待删除 |
| `04_init_data.sql` | 首页 / 展览 / 美术馆 的测试数据 | 本地开发造数据 |
| `05_order_favorite_tables.sql` | 票务与个人中心的增量变更（可重复执行） | **已有库**，补 4 张新表 |
| `06_init_order_data.sql` | 票种 / 日历 / 订单 / 收藏 的测试数据（需先跑 `04`） | 验证购票流程与个人中心 |
| `07_admin_tables.sql` | 后台管理模块的增量变更（可重复执行）：新建 `city` / `notification`，`exhibition` / `gallery` / `exhibition_order` / `admin` 补字段 | **已有库**，要跑后台接口前必须执行 |
| `08_repair_denormalized.sql` | **冗余字段校准**：按订单表重算 6 个冗余字段（幂等） | 数据出现漂移时手工修一次（平时由定时任务兜底） |

> ⚠️ `01` 与「`02` + `05` + `07`」**二选一**：
> - 全新环境 → 只跑 `01`（再按需跑 `07` 补后台字段）
> - 已有库 → 依次跑 `02` → `05` → `07`（都已做幂等，重复执行不报错）
>
> 登录模块的 `user` / `admin` 建表语句见 [../01登录模块数据库建表方案.md](../01登录模块数据库建表方案.md)；
> `user` 表的 `city` / `gender` 两个扩展列由 `02` 与 `05` 幂等添加。

## 冗余字段（由代码维护，不要手工改）

以下字段是为**性能**而重复存储的，写入路径必须同步维护。若怀疑数据不对，
跑 `08_repair_denormalized.sql` 或调 `POST /api/admin/data-repair` 校准（幂等）。

| 冗余字段 | 唯一可信来源 | 口径 |
|---|---|---|
| `ticket_type.sold_count` | `exhibition_order` | `SUM(quantity) WHERE status IN (2,5)` |
| `exhibition_calendar.sold_count` | `exhibition_order` | 按「展览 + 观展日期」，`status IN (2,5)` |
| `exhibition.sold_count` | `exhibition_order` | 按展览，`status IN (2,5)` |
| `exhibition.total_stock` | `ticket_type` | `SUM(total_stock)`（在售票种） |
| `exhibition.price` | `ticket_type` | `MIN(price)`（在售票种，展示用） |
| `gallery.exhibition_count` | `exhibition` | 该馆「今天在展期内」的展览数 |
| `user.visit_count` | `exhibition_order` | `use_time IS NOT NULL` 的订单数 |

> 📌 口径为什么是 `status IN (2,5)`：只有**已支付(2)** 与**已完成(5)** 才真正占用库存；
> 待支付(1)、已取消(3)、已退款(4) 都不计入已售。

## 表清单（8 张业务表 + user）

| # | 表名 | 用途 | 表结构文档 |
|---|---|---|---|
| 1 | `banner` | 首页轮播图 | [banner-表结构说明.md](banner-表结构说明.md) |
| 2 | `gallery` | 美术馆（首页热门 + 美术馆列表 / 详情页） | [gallery-表结构说明.md](gallery-表结构说明.md) |
| 3 | `exhibition` | 展览（首页热门 + 全部展览列表 + 详情页） | [exhibition-表结构说明.md](exhibition-表结构说明.md) |
| 4 | `exhibition_work` | 作品集（展览详情页「作品集」区块） | [exhibition_work-表结构说明.md](exhibition_work-表结构说明.md) |
| 5 | `ticket_type` | 票种（成人/老人/学生/儿童票，各自价格与库存） | [ticket_type-表结构说明.md](ticket_type-表结构说明.md) |
| 6 | `exhibition_calendar` | 日历库存（只放售票的若干天） | [exhibition_calendar-表结构说明.md](exhibition_calendar-表结构说明.md) |
| 7 | `exhibition_order` | 订单（一单一码，服务购票/支付/票夹/看展记录） | [exhibition_order-表结构说明.md](exhibition_order-表结构说明.md) |
| 8 | `favorite` | 收藏（展览 / 美术馆 多态） | [favorite-表结构说明.md](favorite-表结构说明.md) |
| — | `user` | 用户（登录模块建；本文档扩展 `city` / `gender`） | [../01登录模块数据库建表方案.md](../01登录模块数据库建表方案.md) |

每份表结构文档都包含：表的作用与对应原型区块、字段结构表、索引设计、常用查询 SQL、业务规则、设计说明、相关表与下游影响。

**表关系**

```
gallery    (1) ──< exhibition (N)           exhibition.gallery_id
exhibition (1) ──< exhibition_work (N)      exhibition_work.exhibition_id
exhibition (1) ──< ticket_type (N)          ticket_type.exhibition_id
exhibition (1) ──< exhibition_calendar (N)  exhibition_calendar.exhibition_id
exhibition (1) ──< exhibition_order (N)     exhibition_order.exhibition_id
ticket_type(1) ──< exhibition_order (N)     exhibition_order.ticket_type_id
user       (1) ──< exhibition_order (N)     exhibition_order.user_id
user       (1) ──< favorite (N)             favorite.user_id
favorite   ──(多态)──┬── exhibition (target_type = 1)
                     └── gallery    (target_type = 2)
```

## 六条设计约定

1. **「艺览智荐」不落库** —— 该区块由算法接口直接返回推荐结果，项目侧不建表、不查库。
2. **展期状态由日期实时计算，不落库** —— 不设表示展出状态的字段：
   | 状态 | 判断条件 |
   |---|---|
   | 正在展出 | `start_date <= CURDATE() <= end_date` |
   | 即将展出 | `start_date > CURDATE()` |
   | 往期展出 | `end_date < CURDATE()` |
3. **多票种** —— 一个展览多个票种、多个价格，价格与库存**以 `ticket_type` 表为准**。
   `exhibition.price` / `total_stock` / `sold_count` 保留，**仅用于列表页展示票价**。
4. **日历只放指定日期** —— `exhibition_calendar` 里**没有记录的日期 = 不发售**，插几行就放几天票。
5. **一单一码** —— 一个订单一个二维码，`qr_code` 存 `exhibition_order` 表，**不建 `ticket` 表**；核销按整单。
6. **库存两维** —— `ticket_type` 管票种维度，`exhibition_calendar` 管日期维度，**下单时两者都要扣**。

**统一防超卖写法**（两张库存表都一样）：

```sql
UPDATE ticket_type
SET sold_count = sold_count + #{quantity}
WHERE id = #{ticketTypeId} AND total_stock - sold_count >= #{quantity};
-- 受影响行数 = 0 → 抛「库存不足」，事务回滚
```

## 页面区块与表对应

| 页面区块 | 数据来源 |
|---|---|
| 首页 — 轮播图 | `banner ORDER BY sort DESC` |
| 首页 — 当前热门展览 | `exhibition WHERE is_hot = 1` + 展期判断 |
| 首页 — 艺览智荐 | 算法接口（不查库） |
| 首页 — 热门美术馆 | `gallery WHERE is_hot = 1 AND status = 0` |
| 全部展览 / 美术馆列表 | `exhibition` JOIN `gallery`、`gallery` |
| 展览详情 — 头部 | `exhibition` + JOIN `gallery` |
| 展览详情 — 展览实景 | `exhibition.images`（英文逗号分隔多图） |
| 展览详情 — 作品集 | `exhibition_work WHERE exhibition_id = ?` |
| 美术馆详情 — 正在/往期展出 | `exhibition WHERE gallery_id = ?` + 展期判断 |
| 购票页 — 选择票种 | `ticket_type WHERE exhibition_id = ? AND status = 0` |
| 购票页 — 选择观展日期 | `exhibition_calendar WHERE exhibition_id = ? AND calendar_date >= CURDATE()` |
| 支付页 | `exhibition_order`（订单号 / 金额 / `pay_expire_time` 倒计时） |
| 票夹 | `exhibition_order WHERE user_id = ? AND status = 2`（展示 `qr_code`） |
| 看展记录 | `exhibition_order WHERE use_time IS NOT NULL`，按月分组 |
| 收藏列表 | `favorite` JOIN `exhibition` / `gallery`（按 `target_type` 分支） |
| 个人中心 — 我的收藏 | `COUNT(*) FROM favorite WHERE user_id = ?` |
| 个人中心 — 有效票夹 | `COUNT(*) FROM exhibition_order WHERE user_id = ? AND status = 2` |
| 个人中心 — 观展次数 | `user.visit_count`（核销时 +1） |
| 编辑个人资料 | `user` 的 `nickname` / `gender` / `city` / `avatar` |

## 订单状态值

| 值 | 名称 | 对应原型 tab | 库存 |
|---|---|---|---|
| 1 | 待支付 | 待支付 | 已锁定，30 分钟超时释放 |
| 2 | 已支付 | 已支付 | 已扣减 |
| 3 | 已取消 | 已取消 | 已释放 |
| 4 | 已退款 | 已退款 | 已释放 |
| 5 | 已完成 | — | 已核销入场 |

## 执行方式

```bash
# 全新环境
mysql -u root -p < db/01_create_tables.sql
mysql -u root -p city_art < db/04_init_data.sql
mysql -u root -p city_art < db/06_init_order_data.sql   # 票务与个人中心的测试数据

# 已有库（幂等，可重复执行）
mysql -u root -p city_art < db/02_alter_existing.sql
mysql -u root -p city_art < db/05_order_favorite_tables.sql

# 方式二：IDEA Database 面板里直接打开 .sql 文件，选中语句执行
```

> 执行后核对：`SHOW TABLES;` 应有 8 张业务表 + `user` + `admin`；`DESC user;` 应有 `city`、`gender`。

## 已知待办

- `gallery.exhibition_count` 是**冗余字段**，新增/删除展览或展览过期时不会自动更新，需由应用层同步或定时任务重算。
- `exhibition.images` 用逗号分隔存多图，属于简单实现；若以后需要单图排序、单图说明，应拆出 `exhibition_image` 表。
- `03_rollback.sql` 与当前设计不一致（按旧版结构编写），**建议删除**；`02` / `05` 均已幂等，回滚脚本意义不大。
- `01_create_tables.sql` 未包含你本地库里那些历史遗留列（`exhibition.category_id` / `view_count` 等），如需对齐请单独处理。
- **支付对接未做**：`pay_type` / `pay_time` / `pay_expire_time` 字段已就绪，但微信/支付宝的支付参数与回调接口尚未设计。
