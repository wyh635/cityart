# exhibition_calendar —— 展览日历库存表

> 所属项目：艺览 ciTY
> 数据库：`city_art` ｜ 引擎：InnoDB ｜ 字符集：`utf8mb4 / utf8mb4_unicode_ci`
> 建表脚本：[01_create_tables.sql](01_create_tables.sql) ｜ 增量脚本：[05_order_favorite_tables.sql](05_order_favorite_tables.sql)
> 关联文档：[ticket_type-表结构说明.md](ticket_type-表结构说明.md)、[exhibition_order-表结构说明.md](exhibition_order-表结构说明.md)

---

## 一、表的作用

控制**哪些日期可以购票、每天放多少票**。购票页「选择观展日期」的下拉列表就是本表的数据。

**对应原型区块**：购票页 — 「选择观展日期 DATE」

```
请选择观展日期
2026.07.28（周四）
2026.07.29（周五）
2026.07.30（周六）    已售完      ← 灰色不可选
2026.07.31（周日）
2026.08.02（周二）
```

## ⭐ 核心设计：**只放表里有的日期**

**本表里没有记录的日期 = 不发售。**

这是「只放 5 天的票」的实现方式 —— 不需要额外字段标记"哪几天放票"，**插几行就放几天**。

> 例：展览展期是 `2026.05.01 ~ 2026.08.02`（94 天），但只插 5 行
> （07.28 / 07.29 / 07.30 / 07.31 / 08.02），购票页下拉就**只显示这 5 天**。

---

## 二、字段结构

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键ID |
| `exhibition_id` | BIGINT UNSIGNED | 否 | — | 展览ID，关联 `exhibition.id` |
| `calendar_date` | DATE | 否 | — | 可预约日期 |
| `total_stock` | INT | 否 | 0 | 当日总放票数 |
| `sold_count` | INT | 否 | 0 | 当日已售数量 |
| `status` | TINYINT | 否 | 0 | 状态：0-可预约 1-已约满/闭馆 |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE | 最近更新时间 |

### 字段详解

**`calendar_date` — 可预约日期**

用 `DATE` 不用 `DATETIME`：日期粒度就是"天"，原型上也是 `2026.07.28（周四）`。

- 与 `exhibition_id` 组成**唯一键** → 同一天不会重复放票
- 查询时只看 `>= CURDATE()` 的日期（过去的不显示）

**`total_stock` / `sold_count` — 当日库存**

- 当日剩余 = `total_stock - sold_count`
- **剩余为 0 → 前端显示「已售完」并置灰**
- `total_stock = 0` 可以表示**当天闭馆/不发售**（但更推荐**不插这一行**）

**`status` — 日期状态**

`0` = 可预约，`1` = 已约满或闭馆。

> 两种表达「售完」的方式：① 靠 `total_stock - sold_count = 0` 实时算；② 卖完后把 `status` 改成 1。
> **推荐 ①**，不用额外维护；`status` 只在**人工闭馆**（如临时布展）时用。

---

## 三、索引

| 索引名 | 类型 | 字段 | 用途 |
|---|---|---|---|
| `PRIMARY` | 主键 | `id` | 主键查询 |
| `uk_exhibition_date` | 唯一 | `exhibition_id, calendar_date` | 同一天只有一行（**扣减走这一行的行锁**） |
| `idx_date` | 普通 | `calendar_date` | 按日期统计 / 清理过期数据 |

**为什么唯一键很重要**：扣减库存用的是「条件更新」，靠**行锁**保证原子性。如果同一天有多行，锁就不成立，会超卖。

---

## 四、常用查询

```sql
-- ① 购票页「选择观展日期」（只列今天之后的）
SELECT calendar_date,
       total_stock - sold_count AS remaining,
       CASE WHEN total_stock - sold_count <= 0 OR status = 1
            THEN 1 ELSE 0 END AS soldOut
FROM exhibition_calendar
WHERE exhibition_id = #{exhibitionId}
  AND calendar_date >= CURDATE()
ORDER BY calendar_date ASC;

-- ② 校验所选日期是否可售（下单前）
SELECT COUNT(*) FROM exhibition_calendar
WHERE exhibition_id = #{exhibitionId}
  AND calendar_date = #{visitDate}
  AND status = 0
  AND total_stock - sold_count >= #{quantity};
-- 结果为 0 → 「所选日期已售完或不可预约」

-- ③ 扣减当日库存（防超卖，核心）
UPDATE exhibition_calendar
SET sold_count = sold_count + #{quantity}
WHERE exhibition_id = #{exhibitionId}
  AND calendar_date = #{visitDate}
  AND total_stock - sold_count >= #{quantity};
-- 受影响行数 = 0 → 抛异常，事务回滚

-- ④ 回滚当日库存（超时取消 / 退款）
UPDATE exhibition_calendar
SET sold_count = sold_count - #{quantity}
WHERE exhibition_id = #{exhibitionId}
  AND calendar_date = #{visitDate};

-- ⑤ 放票（运营插入日期）
INSERT INTO exhibition_calendar (exhibition_id, calendar_date, total_stock)
VALUES (1, '2026-07-28', 200), (1, '2026-07-29', 200), (1, '2026-07-30', 200);
```

---

## 五、业务规则

| 规则 | 实现方式 |
|---|---|
| **只放指定的几天票** | 表里没有的日期即不发售，无需额外字段 |
| 一天一行 | `uk_exhibition_date` 唯一索引 |
| 已售完不可选 | `total_stock - sold_count <= 0` 时前端置灰 |
| 闭馆日不发售 | 运营**不插**这一行；或插 `status = 1` |
| 不允许超卖 | `UPDATE ... WHERE total_stock - sold_count >= ?` |
| 过去日期不展示 | 查询带 `calendar_date >= CURDATE()` |
| 超时取消释放库存 | 定时任务回滚 `sold_count` |

---

## 六、设计说明

1. **为什么"只放 5 天"用插行实现，而不是加个标记字段**：插行方式零成本、最直观，运营想加一天就插一行，想停一天就删一行。加标记字段反而要先批量生成整个展期的行，再逐行标记。
2. **为什么不存时间点（如 14:00 / 15:30）**：原型上「选择观展日期」只到天。如果需要「上午场 / 下午场」，得加 `time_slot` 字段并把它纳入唯一键。
3. **库存是两维的**：
   - `ticket_type` 管**票种维度**（成人票总共 500 张）
   - `exhibition_calendar` 管**日期维度**（7月28日总共放 200 张）
   - **下单时两个都要扣**，任一不足就失败
4. **`total_stock` 是「当日总放票数」，不分票种**：如果需要「7月30日的老人票单独限 50 张」，就得升级成 `日期 × 票种` 双维度表（第三张表）。

---

## 七、相关表

```
exhibition (1) ──── (N) exhibition_calendar
```

| 关联表 | 关联字段 | 说明 |
|---|---|---|
| `exhibition` | `exhibition_calendar.exhibition_id → exhibition.id` | 日历所属展览（**父表**） |
| `ticket_type` | 无直接外键 | 票种维度的库存，与本表构成**两维库存** |
| `exhibition_order` | `visit_date` 对应 `calendar_date` | 订单预约的日期 |

**下游影响**：

- **展期变了要同步维护日历**：`exhibition.start_date` / `end_date` 调整后，本表里超出新展期的日期记录要么删掉要么作废
- 已售出的日期记录**不能直接删**（有订单引用），要保证 `sold_count` 可回滚
- 建议每天清理/归档 `calendar_date < CURDATE()` 的历史记录（可选）
