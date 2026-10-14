# api-docs —— 接口文档

> 艺览 ciTY 项目接口文档。所有文档遵循同一套规范：`Result<T>` 统一返回体、`/api/app/...` 路径前缀、camelCase 字段命名。

## 文档清单

| 编号 | 文档 | 对应页面 | 接口数 | 认证 |
|---|---|---|---|---|
| 01 | [01登录接口文档.md](01登录接口文档.md) | 登录 / 注册 / 忘记密码 | 10 | 多数 🔓 |
| 02 | [02首页模块接口文档.md](02首页模块接口文档.md) | 首页 | 4 | 全部 🔓 |
| 03 | [03全部展览与美术馆列表接口文档.md](03全部展览与美术馆列表接口文档.md) | 全部展览、美术馆 | 5 | 全部 🔓 |
| 04 | [04详情页接口文档.md](04详情页接口文档.md) | 展览详情、美术馆详情 | 4 | 全部 🔓 |
| 05 | [05票务与个人中心接口文档.md](05票务与个人中心接口文档.md) | 购票、支付、我的订单、票夹、看展记录、收藏、个人中心 | 18 | 多为 🔒 |
| 06 | [06后台管理接口文档.md](06后台管理接口文档.md) | **B 端**：数据看板、展览/展馆管理、票务数据、扫码核销、个人中心 | 28 | 除登录外全 🔒 |

## 统一规范速查

### 返回格式

```json
{
  "code": 1,
  "msg": "操作成功",
  "data": {}
}
```

| code | 含义 |
|------|------|
| `1` | 成功 |
| `0` | 失败 |

### 路径规范

| 前缀 | 说明 |
|------|------|
| `/api/app/...` | C 端（小程序） |
| `/api/admin/...` | B 端（管理后台） |

### 认证标识

| 标识 | 含义 |
|------|------|
| 🔓 | 免登录可访问 |
| 🔒 | 必须带 token |

请求头名由 `application.yml` 的 `cityart.jwt.user-token-name` 决定，当前为 `authentication`。

### 分页规范

**请求**：`page`（默认 1）、`pageSize`（默认 10，最大 50）

**返回**（`PageResult<T>`）：

```json
{
  "total": 23,
  "pages": 3,
  "page": 1,
  "pageSize": 10,
  "list": []
}
```

### 字段命名

- JSON 统一 **camelCase**，数据库为 `snake_case`，由 MyBatis 自动映射
- 日期：`LocalDate` → `yyyy-MM-dd`；`LocalDateTime` → `yyyy-MM-dd HH:mm`
- 金额：`DECIMAL(10,2)` → JSON 数字

## ⚠️ 上线前必查：JWT 白名单

拦截器注册的是 `/api/app/**`，**所有免登录接口都必须加入 `excludePathPatterns`**，否则一律返回 401。

当前累计需要放行的路径：

```
# 登录模块
/api/app/login
/api/app/register

# 首页模块（4 个）
/api/app/banner
/api/app/exhibition
/api/app/recommend
/api/app/gallery

# 全部展览与美术馆列表（5 个）
/api/app/exhibitions/page
/api/app/galleries/page
/api/app/dict/exhibition-types
/api/app/dict/gallery-types
/api/app/dict/cities

# 详情页里「纯查询」的 2 个（作品集、馆内展览列表）
/api/app/exhibitions/*/works
/api/app/galleries/*/exhibitions

# 票务模块中免登录的 3 个（票种、日历、支付回调）
/api/app/exhibitions/*/ticket-types
/api/app/exhibitions/*/calendar
/api/app/orders/pay-notify
```

> ⚠️ **两个详情接口（`/api/app/exhibitions/{id}`、`/api/app/galleries/{id}`）不要写进本清单** ——
> 它们是**可选登录**（方法上标 `@OptionalLogin`）：游客可看，带 token 时额外返回真实 `isFavorited`。
> 一旦加进白名单就会绕过拦截器，`userId` 恒为 `null`，收藏状态永远是 `false`。

> 新增免登录接口时，**必须同步更新这份清单和 `WebMvcConfiguration`**。

## 接口编号总表

### 01 登录模块

| 编号 | Path | Method | 认证 |
|---|---|---|---|
| 1 | `/api/app/register` | POST | 🔓 |
| 2 | `/api/app/login` | POST | 🔓 |
| 3 | `/api/app/profile` | GET | 🔒 |
| 4 | `/api/app/profile` | PUT | 🔒 |
| 5 | `/api/admin/login` | POST | 🔓 |
| 6 | `/api/admin/profile` | GET | 🔒 |
| 7 | `/api/admin/profile` | PUT | 🔒 |
| 8 | `/api/send-code` | POST | 🔓 |
| 9 | `/api/verify-code` | POST | 🔓 |
| 10 | `/api/reset-password` | PUT | 🔓 |

### 02 首页模块

| 编号 | Path | Method | 认证 | 数据来源 |
|---|---|---|---|---|
| 1 | `/api/app/banner` | GET | 🔓 | `banner` |
| 2 | `/api/app/exhibition` | GET | 🔓 | `exhibition` JOIN `gallery` |
| 3 | `/api/app/recommend` | GET | 🔓 | 算法接口（不查库） |
| 4 | `/api/app/gallery` | GET | 🔓 | `gallery` |

### 03 全部展览与美术馆列表

| 编号 | Path | Method | 认证 | 数据来源 |
|---|---|---|---|---|
| 1 | `/api/app/dict/exhibition-types` | GET | 🔓 | 枚举常量 |
| 2 | `/api/app/dict/gallery-types` | GET | 🔓 | 枚举常量 |
| 3 | `/api/app/dict/cities` | GET | 🔓 | `gallery` |
| 4 | `/api/app/exhibitions/page` | GET | 🔓 | `exhibition` JOIN `gallery` |
| 5 | `/api/app/galleries/page` | GET | 🔓 | `gallery` |

### 04 详情页

| 编号 | Path | Method | 认证 | 数据来源 |
|---|---|---|---|---|
| 1 | `/api/app/exhibitions/{id}` | GET | 🔓 | `exhibition` JOIN `gallery` |
| 2 | `/api/app/exhibitions/{id}/works` | GET | 🔓 | `exhibition_work` |
| 3 | `/api/app/galleries/{id}` | GET | 🔓 | `gallery` + `exhibition` |
| 4 | `/api/app/galleries/{id}/exhibitions` | GET | 🔓 | `exhibition` |

### 05 票务与个人中心

| 编号 | Path | Method | 认证 | 数据来源 |
|---|---|---|---|---|
| 1 | `/api/app/exhibitions/{id}/ticket-types` | GET | 🔓 | `ticket_type` |
| 2 | `/api/app/exhibitions/{id}/calendar` | GET | 🔓 | `exhibition_calendar` |
| 3 | `/api/app/orders` | POST | 🔒 | 两维库存 + `exhibition_order` |
| 4 | `/api/app/orders/{orderNo}` | GET | 🔒 | `exhibition_order` |
| 5 | `/api/app/orders/{orderNo}/pay` | POST | 🔒 | 支付平台 |
| 6 | `/api/app/orders/pay-notify` | POST | 🔓 | 支付回调 |
| 7 | `/api/app/orders/{orderNo}/pay-result` | GET | 🔒 | `exhibition_order` |
| 8 | `/api/app/orders/{orderNo}/cancel` | PUT | 🔒 | 订单 + 库存回滚 |
| 9 | `/api/app/orders/{orderNo}/refund` | POST | 🔒 | 订单 + 库存回滚 |
| 10 | `/api/app/orders/page` | GET | 🔒 | `exhibition_order` |
| 11 | `/api/app/tickets/page` | GET | 🔒 | `exhibition_order` |
| 12 | `/api/app/records/page` | GET | 🔒 | `exhibition_order` |
| 13 | `/api/app/favorites/page` | GET | 🔒 | `favorite` |
| 14 | `/api/app/favorites` | POST | 🔒 | `favorite` |
| 15 | `/api/app/favorites` | DELETE | 🔒 | `favorite` |
| 16 | `/api/app/favorites/check` | GET | 🔒 | `favorite` |
| 17 | `/api/app/personal/center` | GET | 🔒 | `user` + `favorite` + `exhibition_order` |
| 18 | `/api/app/profile` | PUT | 🔒 | `user`（对 01 文档的扩展） |

### 06 后台管理（B 端）

> 全部在 `/api/admin/**` 下（与 C 端 `/api/app/**` 区分），**除登录外都要 token**，且需要**另一个 B 端拦截器**用 `admin-secret-key` 验签。

| 编号 | Path | Method | 认证 | 数据来源 |
|---|---|---|---|---|
| 1 | `/api/admin/dashboard/overview` | GET | 🔒 | `exhibition_order` 聚合 |
| 2 | `/api/admin/dashboard/exhibition-rank` | GET | 🔒 | 订单 + 展览 |
| 3~6 | `/api/admin/dict/**` | GET | 🔒 | `city` / 枚举 / `gallery` |
| 7~13 | `/api/admin/exhibitions/**` | GET/POST/PUT/DELETE | 🔒 | 展览 + 票种 + 日历 |
| 14~18 | `/api/admin/galleries/**` | GET/POST/PUT/DELETE | 🔒 | `gallery` |
| 19~20 | `/api/admin/orders/**` | GET/PUT | 🔒 | 订单 + 库存回滚 |
| 21~23 | `/api/admin/stats/tickets**` | GET | 🔒 | 订单聚合 / POI 导出 |
| 24 | `/api/admin/orders/verify` | POST | 🔒 | 核销 |
| 25 | `/api/admin/verify-records/page` | GET | 🔒 | 订单 JOIN |
| 26~27 | `/api/admin/profile` | GET/PUT | 🔒 | `admin` |
| 28 | `/api/admin/upload` | POST | 🔒 | 文件系统 / OSS |

## 待补充的模块

| 模块 | 状态 |
|---|---|
| B 端登录拦截器 | 代码待写（`JwtTokenAdminInterceptor`，用 `admin-secret-key` 验签） |
| 后台前端路由 | 待定 |

> 详情页的「购票」和「收藏」按钮，此前标记为**占位**；现在对应表和接口文档均已就绪，可按 `05票务与个人中心接口文档.md` 实现。

## 相关目录

| 目录 | 内容 |
|---|---|
| [db/](../db/README.md) | 数据库设计：8 张业务表 + `07_admin_tables.sql`（后台新增 `city` / `notification` 及字段扩展）+ 每张表的结构说明 |
