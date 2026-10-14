# exhibition_order —— 展览购票订单表

> 所属项目：艺览 ciTY
> 数据库：`city_art` ｜ 引擎：InnoDB ｜ 字符集：`utf8mb4 / utf8mb4_unicode_ci`
> 建表脚本：[01_create_tables.sql](01_create_tables.sql) ｜ 增量脚本：[05_order_favorite_tables.sql](05_order_favorite_tables.sql)
> 关联文档：[ticket_type-表结构说明.md](ticket_type-表结构说明.md)、[exhibition_calendar-表结构说明.md](exhibition_calendar-表结构说明.md)

---

## 一、表的作用

**一张表支撑 5 个页面**，是整个票务模块的核心：

| 页面 | 如何用本表 |
|---|---|
| 购票页（确认下单） | INSERT 一条 `status = 1` 的订单 |
| 支付页 | 读 `order_no` / `total_amount` / `pay_expire_time`（30 分钟倒计时） |
| 票夹 | 查 `status = 2` 且未核销的订单，展示 `qr_code` |
| 看展记录 | 查 `use_time IS NOT NULL` 的订单，按月分组 |
| 个人中心统计 | 「有效票夹」= `COUNT(*) WHERE status = 2` |

## ⭐ 核心设计：**一单一码**

**一个订单一个二维码**，不建 `ticket` 表。

- 买 2 张 → 1 条订单、`quantity = 2`、**1 个 `qr_code`**
- 核销时**整单一起核销**（`status → 5`，写 `use_time`）
- 原型票夹卡片显示「共2张」+ 一个大二维码，正是这个模型

> ⚠️ 若以后要「2 张票分别入场、分开核销」，必须拆出 `ticket` 表（一票一码）。

---

## 二、字段结构

### 订单标识

| 字段名 | 类型 | 允许空 | 说明 |
|---|---|---|---|
| `id` | BIGINT UNSIGNED | 否 | 主键ID |
| `order_no` | VARCHAR(32) | 否 | 订单号，**唯一** |
| `user_id` | BIGINT UNSIGNED | 否 | 下单用户，关联 `user.id` |
| `exhibition_id` | BIGINT UNSIGNED | 否 | 展览ID，关联 `exhibition.id` |
| `gallery_id` | BIGINT UNSIGNED | 否 | 美术馆ID（冗余，票夹显示「观展场馆」） |

### 票种与金额（快照）

| 字段名 | 类型 | 允许空 | 说明 |
|---|---|---|---|
| `ticket_type_id` | BIGINT UNSIGNED | 否 | 票种ID，关联 `ticket_type.id` |
| `ticket_name` | VARCHAR(50) | 否 | 票种名称**快照**（如「成人票」） |
| `price` | DECIMAL(10,2) | 否 | 成交单价**快照**（元） |
| `quantity` | INT | 否 | 购票数量 |
| `total_amount` | DECIMAL(10,2) | 否 | 订单总金额 = `price × quantity` |

### 状态与观展

| 字段名 | 类型 | 允许空 | 说明 |
|---|---|---|---|
| `status` | TINYINT | 否 | 订单状态：1-待支付 2-已支付 3-已取消 4-已退款 5-已完成 |
| `visit_date` | DATE | 否 | 预约观展日期 |
| `qr_code` | VARCHAR(500) | 是 | 电子票二维码（一单一码） |
| `use_time` | DATETIME | 是 | 核销入场时间 |
| `expire_time` | DATETIME | 是 | 电子票有效期 |

### 联系人

| 字段名 | 类型 | 允许空 | 说明 |
|---|---|---|---|
| `contact_name` | VARCHAR(50) | 是 | 联系人姓名 |
| `contact_phone` | VARCHAR(11) | 是 | 联系人手机号 |

### 支付

| 字段名 | 类型 | 允许空 | 说明 |
|---|---|---|---|
| `pay_type` | TINYINT | 是 | 支付方式：1-微信 2-支付宝 |
| `pay_time` | DATETIME | 是 | 支付成功时间 |
| `pay_expire_time` | DATETIME | 是 | **支付超时时间 = 下单 + 30 分钟** |

### 取消与退款

| 字段名 | 类型 | 允许空 | 说明 |
|---|---|---|---|
| `cancel_time` | DATETIME | 是 | 取消时间 |
| `cancel_reason` | VARCHAR(255) | 是 | 取消原因 |
| `refund_status` | TINYINT | 否 | 0-无退款 1-退款中 2-已退款 3-退款驳回 |
| `refund_amount` | DECIMAL(10,2) | 是 | 实际退款金额 |

### 时间戳

| 字段名 | 类型 | 说明 |
|---|---|---|
| `create_time` | DATETIME | 下单时间 |
| `update_time` | DATETIME | 最近更新时间 |

---

## 三、字段详解

**`order_no` — 订单号 ⭐**

业务唯一号，原型上形如 `202655915169965`、`2026072501100668`、`2026061001100668`。

- **不要用自增 `id` 当订单号**（会暴露业务量、且容易被遍历）
- 建议格式：`yyyyMMdd` + 序列/雪花 ID，长度 ≤ 32
- **唯一索引** `uk_order_no` 保证不重复，也用于**防重复提交**（幂等）

**`ticket_name` / `price` — 为什么要快照 ⭐**

下单那一刻把票种名和价格**复制**一份存进订单。

- 后台把「成人票」改价 ¥58 → ¥68：**历史订单仍显示 ¥58**（快照）
- 后台删掉某票种：历史订单不受影响（快照里还有名字）
- ⚠️ **这是订单系统的铁律：订单金额一旦生成就不能被动变**

**`status` — 订单状态**

| 值 | 名称 | 对应原型 tab | 说明 |
|---|---|---|---|
| 1 | 待支付 | 待支付 | 下单后默认 |
| 2 | 已支付 | 已支付 | 票夹里可见（有效票） |
| 3 | 已取消 | 已取消 | 用户主动取消 / 超时自动取消，**库存已释放** |
| 4 | 已退款 | 已退款 | 退款成功，**库存已释放** |
| 5 | 已完成 | — | 已核销入场 |

**状态流转**：

```
                    ┌── 用户取消 / 超时30分钟 ──► 3-已取消（释放库存）
1-待支付 ───────────┤
                    └── 支付成功 ──────────────► 2-已支付 ──┬── 核销入场 ──► 5-已完成
                                                            └── 申请退款 ──► 4-已退款（释放库存）
```

**`pay_expire_time` — 30 分钟支付时效 ⭐**

来自原型「订单生成后仅有 30 分钟支付时效，超时自动取消并释放库存」。

- 下单时 `pay_expire_time = NOW() + 30 分钟`
- 支付页倒计时 = `pay_expire_time - NOW()`（原型显示「剩余支付时间：30:00」）
- **定时任务**扫超时订单并释放库存

**`expire_time` — 电子票有效期**

票夹上「有效-45天后过期」的来源。

- 支付成功时生成，例如 `visit_date + 45 天`
- 有效票夹统计可加条件 `AND expire_time > NOW()` 排除过期票

**`use_time` — 核销入场时间**

看展记录页显示「入场时间：2026-06-15 15:30」的来源。**有值 = 已核销**。

---

## 四、索引

| 索引名 | 类型 | 字段 | 用途 |
|---|---|---|---|
| `PRIMARY` | 主键 | `id` | 主键查询 |
| `uk_order_no` | 唯一 | `order_no` | 订单号唯一 + 防重复提交 |
| `idx_user_status_time` | 组合 | `user_id, status, create_time` | **我的订单**列表（按用户 + 状态 + 时间倒序） |
| `idx_user_use` | 组合 | `user_id, use_time` | **看展记录**按月分组 |
| `idx_exhibition_date` | 组合 | `exhibition_id, visit_date` | 按展览 + 日期统计 |
| `idx_pay_expire` | 组合 | `status, pay_expire_time` | **定时任务扫超时订单** |

---

## 五、常用查询

```sql
-- ① 支付页 / 订单详情
SELECT o.order_no, o.status, o.total_amount, o.pay_expire_time, o.create_time,
       e.title, e.poster_image,
       g.name AS gallery_name, g.address AS gallery_address
FROM exhibition_order o
JOIN exhibition e ON e.id = o.exhibition_id
JOIN gallery    g ON g.id = o.gallery_id
WHERE o.order_no = #{orderNo} AND o.user_id = #{userId};

-- ② 我的订单列表（status 不传 = 全部）
SELECT o.id, o.order_no, o.status, o.ticket_name, o.quantity, o.total_amount,
       o.visit_date, o.create_time, e.title, e.poster_image, g.name AS gallery_name
FROM exhibition_order o
JOIN exhibition e ON e.id = o.exhibition_id
JOIN gallery    g ON g.id = o.gallery_id
WHERE o.user_id = #{userId}
  <if test='status != null'> AND o.status = #{status} </if>
ORDER BY o.create_time DESC, o.id DESC
LIMIT #{offset}, #{pageSize};

-- ③ 票夹（已支付且未核销）
SELECT o.order_no, o.ticket_name, o.quantity, o.visit_date, o.qr_code, o.expire_time,
       e.title, e.poster_image, g.name AS gallery_name
FROM exhibition_order o
JOIN exhibition e ON e.id = o.exhibition_id
JOIN gallery    g ON g.id = o.gallery_id
WHERE o.user_id = #{userId} AND o.status = 2
ORDER BY o.visit_date DESC, o.id DESC;

-- ④ 看展记录（已核销，按月分组）
SELECT DATE_FORMAT(o.use_time, '%Y-%m') AS month_group,
       o.order_no, o.use_time, e.title, e.poster_image, g.name AS gallery_name
FROM exhibition_order o
JOIN exhibition e ON e.id = o.exhibition_id
JOIN gallery    g ON g.id = o.gallery_id
WHERE o.user_id = #{userId} AND o.use_time IS NOT NULL
ORDER BY o.use_time DESC, o.id DESC
LIMIT #{offset}, #{pageSize};

-- ⑤ 个人中心「有效票夹」统计
SELECT COUNT(*) FROM exhibition_order
WHERE user_id = #{userId} AND status = 2;

-- ⑥ 核销（扫码入场）
UPDATE exhibition_order
SET status = 5, use_time = NOW()
WHERE order_no = #{orderNo} AND status = 2;
-- 受影响行数 = 0 → 「该票已核销或状态不对」

-- ⑦ 超时自动取消（定时任务，每 1 分钟）
SELECT id, order_no, ticket_type_id, quantity, visit_date
FROM exhibition_order
WHERE status = 1 AND pay_expire_time < NOW()
LIMIT 100;

UPDATE exhibition_order
SET status = 3, cancel_time = NOW(), cancel_reason = '支付超时自动取消'
WHERE id = #{id} AND status = 1;   -- 条件更新防并发
-- 随后回滚 ticket_type.sold_count 与 exhibition_calendar.sold_count
```

---

## 六、业务规则

| 规则 | 来源 | 实现方式 |
|---|---|---|
| 30 分钟支付时效 | 观展须知 | `pay_expire_time = 下单 + 30分钟`，定时任务取消 |
| 超时自动取消并**释放库存** | 观展须知 | `status → 3` + 回滚两张库存表 |
| 仅已支付订单可申请退款 | 观展须知 | 应用层校验 `status = 2` |
| 闭馆日无法核销入场 | 观展须知 | 核销前校验 `visit_date` 在日历可售范围内 |
| 每人限购 5 张 | 购票页文案 | 下单前查 `SUM(quantity)` + `ticket_type.limit_per_user` |
| 不允许超卖 | — | 条件更新 + 判断影响行数（两张库存表） |
| 一单一码 | — | `qr_code` 存订单表，核销整单 |
| 订单金额不可篡改 | — | `price` / `ticket_name` 快照 |
| 支付回调幂等 | — | `UPDATE ... WHERE order_no = ? AND status = 1` |

---

## 七、设计说明

1. **为什么冗余 `gallery_id`**：票夹和看展记录都要显示「观展场馆」，从 `exhibition` 再 JOIN 一次能拿到，但冗余一列可以让票夹查询少一层 JOIN。
2. **为什么不用自增 id 当订单号**：暴露业务量、可被遍历猜测。用独立的 `order_no`。
3. **为什么 `status` 用数字而不是字符串**：索引效率高，且能和原型 tab 一一对应。
4. **`refund_status` 与 `status` 是两个维度**：`status` 是整个订单的生命周期，`refund_status` 只管退款子流程。一笔订单可能 `status = 4`（已退款）且 `refund_status = 2`（已退款成功）。
5. **一单一码的取舍**：简单、够用（原型就这样），但**无法部分核销**。若业务要求"2 张票分两次入场"，必须拆 `ticket` 表。

---

## 八、相关表

```
user (1)         ──── (N) exhibition_order
exhibition (1)   ──── (N) exhibition_order
gallery (1)      ──── (N) exhibition_order     (冗余关联)
ticket_type (1)  ──── (N) exhibition_order
exhibition_calendar ──  exhibition_order.visit_date 对应 calendar_date
```

| 关联表 | 关联字段 | 说明 |
|---|---|---|
| `user` | `user_id` | 订单归属 |
| `exhibition` | `exhibition_id` | 购票的展览 |
| `gallery` | `gallery_id` | 观展场馆（冗余） |
| `ticket_type` | `ticket_type_id` | 所选票种 |
| `exhibition_calendar` | `visit_date` ↔ `calendar_date` | 预约的日期 |

**下游影响**：

- 订单是**不可删除**的业务数据，取消/退款只改状态，**绝不物理删除**
- 订单牵连**两张库存表**的 `sold_count`：下单时都 +，取消/退款时都 -
- 核销成功后应同步 `user.visit_count + 1`（个人中心「观展次数」数据源）
- **删除展览或票种前，必须确认没有未完结订单引用**
