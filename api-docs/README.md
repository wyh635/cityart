# api-docs —— 接口文档

> 艺览 ciTY 项目接口文档。所有文档遵循同一套规范：`Result<T>` 统一返回体、`/api/app/...` 路径前缀、camelCase 字段命名。

## 文档清单

| 编号 | 文档 | 对应页面 | 接口数 | 认证 |
|---|---|---|---|---|
| 01 | [01登录接口文档.md](01登录接口文档.md) | 登录 / 注册 / 忘记密码 | 10 | 多数 🔓 |
| 02 | [02首页模块接口文档.md](02首页模块接口文档.md) | 首页 | 4 | 全部 🔓 |
| 03 | [03全部展览与美术馆列表接口文档.md](03全部展览与美术馆列表接口文档.md) | 全部展览、美术馆 | 5 | 全部 🔓 |
| 04 | [04详情页接口文档.md](04详情页接口文档.md) | 展览详情、美术馆详情 | 4 | 全部 🔓 |

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

# 详情页（4 个，注意用通配符 * 而不是具体数字）
/api/app/exhibitions/*
/api/app/exhibitions/*/works
/api/app/galleries/*
/api/app/galleries/*/exhibitions
```

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

## 待补充的模块

| 模块 | 状态 |
|---|---|
| 票务 / 下单 / 票夹 | 待写（对应表已 drop，需重新设计；详情页「购票」按钮目前只占位） |
| 个人中心 / 收藏 / 看展记录 | 待写（对应表已 drop，需重新设计；详情页「收藏」按钮目前只占位） |

## 相关目录

| 目录 | 内容 |
|---|---|
| [db/](../db/README.md) | 数据库设计：4 张表的建表 SQL + 每张表的结构说明 |
