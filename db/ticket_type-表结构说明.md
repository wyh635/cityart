# ticket_type —— 展览票种表

> 所属项目：艺览 ciTY
> 数据库：`city_art` ｜ 引擎：InnoDB ｜ 字符集：`utf8mb4 / utf8mb4_unicode_ci`
> 建表脚本：[01_create_tables.sql](01_create_tables.sql) ｜ 增量脚本：[05_order_favorite_tables.sql](05_order_favorite_tables.sql)
> 关联文档：[exhibition-表结构说明.md](exhibition-表结构说明.md)、[exhibition_order-表结构说明.md](exhibition_order-表结构说明.md)

---

## 一、表的作用

存放**一个展览的多个票种**。购票页「选择票种」区域的每一行（成人票 ¥58 / 老人票 ¥38 / 学生票 ¥38 / 儿童票 ¥38）就是本表的一条记录。

**对应原型区块**：购票页 — 「选择票种 TYPE」

```
⊙ 成人票              ¥58
○ 老人票              ¥38
○ 学生票              ¥38
○ 儿童票              ¥38
```

**为什么单独建表**：一个展览有 4 种票、4 种价格、各自独立的库存与限购规则，属于**一对多**关系，无法塞进 `exhibition` 的单列里。

> ⚠️ **这推翻了早期「票价单一定价」的设计**。当时把 `price` / `total_stock` / `sold_count` 放在 `exhibition` 表，现在改为**以本表为准**。
> `exhibition` 表的那三个字段**保留**，仅用于列表页展示票价（取最低价或默认票种价），**下单扣库存一律走本表**。

---

## 二、字段结构

| 字段名 | 类型 | 允许空 | 默认值 | 说明 |
|---|---|---|---|---|
| `id` | BIGINT UNSIGNED | 否 | AUTO_INCREMENT | 主键ID |
| `exhibition_id` | BIGINT UNSIGNED | 否 | — | 所属展览，关联 `exhibition.id` |
| `name` | VARCHAR(50) | 否 | — | 票种名称 |
| `price` | DECIMAL(10,2) | 否 | 0.00 | 该票种单价（元） |
| `total_stock` | INT | 否 | 0 | 总库存 |
| `sold_count` | INT | 否 | 0 | 已售数量 |
| `limit_per_user` | INT | 否 | 5 | 每人限购数量 |
| `sort` | INT | 否 | 0 | 排序，越大越靠前 |
| `status` | TINYINT | 否 | 0 | 状态：0-在售 1-停售 |
| `create_time` | DATETIME | 否 | CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | 否 | CURRENT_TIMESTAMP ON UPDATE | 最近更新时间 |

### 字段详解

**`name` — 票种名称**

原型上的四种：`成人票`、`老人票`、`学生票`、`儿童票`。

- 同一展览下票种名不能重复 → `uk_exhibition_name (exhibition_id, name)`
- **存的是明文名称**，不存数字枚举。理由：票种会变（以后加「团体票」「亲子票」），存数字就要改代码；存明文则加一条记录即可

**`price` — 该票种单价**

⚠️ **这是多票种设计的核心字段** —— 每个票种价格不同。

- 原型：成人票 ¥58，老人票/学生票/儿童票均 ¥38
- `exhibition.price` 只用于列表页展示，**下单时以本表价格为准**
- 下单时会把价格**快照**到 `exhibition_order.price`，防止后台改价影响历史订单

**`total_stock` / `sold_count` — 库存（票种维度）**

- 剩余库存 = `total_stock - sold_count`
- 下单：`sold_count + quantity`；超时取消 / 退款：`sold_count - quantity`
- ⚠️ **库存是两维的**：本表管**票种维度**，`exhibition_calendar` 管**日期维度**，下单时**两者都要扣**

**`limit_per_user` — 限购**

原型「选择张数（每人至多可购五张）」→ 默认 `5`。

下单前需校验：该用户在该展览的累计已购数量 + 本次数量 ≤ `limit_per_user`。

```sql
SELECT COALESCE(SUM(quantity), 0) FROM exhibition_order
WHERE user_id = ? AND exhibition_id = ? AND status IN (1, 2, 5);
```

**`status` — 在售状态**

`0` = 在售（购票页可选），`1` = 停售（不展示）。比如某票种卖完了，运营可以停售。

---

## 三、索引

| 索引名 | 类型 | 字段 | 用途 |
|---|---|---|---|
| `PRIMARY` | 主键 | `id` | 主键查询 |
| `uk_exhibition_name` | 唯一 | `exhibition_id, name` | 同一展览票种名不重复 |
| `idx_exhibition_status` | 组合 | `exhibition_id, status, sort` | 购票页取在售票种并排序 |

**为什么 `idx_exhibition_status` 是组合索引**：购票页的查询固定是

```sql
WHERE exhibition_id = ? AND status = 0 ORDER BY sort DESC
```

过滤 + 排序一次覆盖，避免 `filesort`。

---

## 四、常用查询

```sql
-- ① 购票页「选择票种」列表
SELECT id, name, price, total_stock - sold_count AS remaining, limit_per_user
FROM ticket_type
WHERE exhibition_id = #{exhibitionId} AND status = 0
ORDER BY sort DESC, id ASC;

-- ② 列表页展示「票价：¥58/人」（取最低价）
SELECT MIN(price) AS min_price
FROM ticket_type
WHERE exhibition_id = #{exhibitionId} AND status = 0;

-- ③ 扣减库存（防超卖，核心）
UPDATE ticket_type
SET sold_count = sold_count + #{quantity}
WHERE id = #{ticketTypeId}
  AND status = 0
  AND total_stock - sold_count >= #{quantity};
-- 受影响行数 = 0 → 抛「库存不足」，事务回滚

-- ④ 回滚库存（超时取消 / 退款）
UPDATE ticket_type
SET sold_count = sold_count - #{quantity}
WHERE id = #{ticketTypeId};

-- ⑤ 校验限购
SELECT COALESCE(SUM(quantity), 0) AS bought
FROM exhibition_order
WHERE user_id = #{userId} AND exhibition_id = #{exhibitionId}
  AND status IN (1, 2, 5);
```

---

## 五、业务规则

| 规则 | 实现方式 |
|---|---|
| 一场展览多个票种 | 一对多，`exhibition_id` 关联 |
| 一票种一个价格 | `price` 字段 |
| 票种名不重复（同一展览内） | `uk_exhibition_name` 唯一索引 |
| 不允许超卖 | `UPDATE ... WHERE total_stock - sold_count >= ?`，判断影响行数 |
| 每人限购 5 张 | `limit_per_user` + 下单前查历史订单 |
| 停售票种不展示 | `WHERE status = 0` |
| 后台改价不影响历史订单 | 下单时把 `price` / `name` **快照**进 `exhibition_order` |

---

## 六、设计说明

1. **为什么不用数字枚举存票种**：票种会随业务变化（团体票、亲子票、夜场票…），存数字每次都要改代码。存明文名称，运营加票种只需插一条记录。
2. **为什么价格放这里而不是 `exhibition`**：一个展览有多个价格，`exhibition` 的单列装不下。
3. **`exhibition` 表的 `price` 字段保留**：列表页/卡片要显示「¥58」，用最低价或默认票种价即可，避免列表查询多一次 JOIN。
4. **库存为什么是两维**：原型上「已售完」标记在**日期行**上（`2026.07.30（周六）已售完`），说明日期维度有限流；同时票种维度也要控量。所以两张表各自扣减。

---

## 七、相关表

```
exhibition (1) ──── (N) ticket_type
ticket_type (1) ──── (N) exhibition_order
```

| 关联表 | 关联字段 | 说明 |
|---|---|---|
| `exhibition` | `ticket_type.exhibition_id → exhibition.id` | 票种所属展览（**父表**） |
| `exhibition_order` | `exhibition_order.ticket_type_id → ticket_type.id` | 该票种的成交订单（**子表**） |
| `exhibition_calendar` | 无直接外键 | 日期维度的库存，与本表构成**两维库存** |

**下游影响**：

- 删除票种前，必须确认没有未完结的订单引用它
- 票种库存是**下单校验的第一道关**，日期库存是第二道关
- 修改 `price` **不会**影响历史订单（已有快照），但会影响新订单
