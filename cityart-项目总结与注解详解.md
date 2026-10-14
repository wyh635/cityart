# cityart 项目总结 —— 注解详解 · 必备文件 · 文件关联全解

> 学习项目(城市艺术 C 端平台),Spring Boot 2.7.3 + MyBatis + MySQL + Druid + Knife4j + JWT + Redis(配置)
> 代码骨架与「苍穹外卖」同源(包名 com.sky → com.cityart,类名 sky-xxx → 去掉前缀),目前只实现了 **C 端手机号注册/登录**,其余基础设施已全部搭好。
> 如果你读过桌面上的《sky-take-out-项目结构与文件职责总结》,可以重点看「四、注解」「五、必备文件」「六、坑」三节,其余是 cityart 自己的全景。

---

## 〇、整体认知(先看这个)

### 0.1 三个 Maven 模块各管什么

| 模块 | 定位 | 放什么 | 一句话职责 |
|---|---|---|---|
| **cityart-common** | 地基 | 常量、异常体系、Result、工具、配置绑定类、ThreadLocal、JSON 转换器 | 与业务无关的公共代码 |
| **cityart-pojo** | 数据对象 | dto / entity / vo(纯字段 + Lombok) | 请求参数、数据库表、响应结果的对象定义 |
| **cityart-server** | 应用主体 | 启动类 + Controller/Service/Mapper + 配置/拦截器/切面/异常处理 + yml | 真正干活,能打包运行的模块 |

- 依赖方向:**server → common、pojo**;common 与 pojo 互相独立。
- 三个模块共用一个包根 **`com.cityart`**,这是关键设计:启动类 `@SpringBootApplication` 默认从 `com.cityart` 往下扫描,才能把 common 里带 `@Component` 的类(如 JwtProperties)、pojo 里的实体一网打尽。
- 根 pom.xml 只做「依赖版本管理」(`dependencyManagement`),真正声明依赖在各子模块 pom。

### 0.2 全项目目录树(27 个 Java 文件)

```
cityart/
├─ pom.xml                              # 父工程:仅版本管理
├─ cityart-common/pom.xml
│  └─ src/main/java/com/cityart/
│     ├─ constant/    MessageConstant  JwtClaimsConstant  AutoFillConstant  StatusConstant
│     ├─ context/     BaseContext                        (ThreadLocal 存当前用户id)
│     ├─ enumeration/ OperationType                      (INSERT/UPDATE 枚举)
│     ├─ exception/   BaseException 及其子类 ×3(LoginFailed/PasswordError/AccountLocked)
│     ├─ json/        JacksonObjectMapper                (统一日期序列化)
│     ├─ properties/  JwtProperties                      (绑定 yml 的 cityart.jwt)
│     ├─ result/      Result<T>                          (统一返回体)
│     └─ utils/       JwtUtil                            (JWT 签发/解析)
├─ cityart-pojo/pom.xml
│  └─ src/main/java/com/cityart/
│     ├─ dto/         UserLoginDTO                       (注册/登录入参,带校验)
│     ├─ entity/      User                               (对应 user 表)
│     └─ vo/          UserVO                             (注册/登录出参,含 token)
└─ cityart-server/pom.xml
   └─ src/main/
      ├─ java/com/cityart/
      │  ├─ CityartApplication.java       (启动类)
      │  ├─ annotation/  AutoFill.java     (自定义注解:标记需要自动填充的方法)
      │  ├─ aspect/      AutoFillAspect.java (AOP:拦截 @AutoFill 并反射填充公共字段)
      │  ├─ config/      WebMvcConfiguration.java (注册拦截器/文档/静态资源/JSON转换器)
      │  ├─ controller/user/ UserController.java
      │  ├─ handler/     GlobalExceptionHandler.java (全局异常出口)
      │  ├─ interceptor/ JwtTokenUserInterceptor.java (JWT 校验守卫)
      │  ├─ mapper/      UserMapper.java   (MyBatis DAO,注解 SQL)
      │  ├─ service/     UserService.java  (业务接口)
      │  └─ service/impl/ UserServiceImpl.java (@Service 实现)
      └─ resources/
         ├─ application.yml               (主配置:端口/datasource/mybatis/jwt 占位符)
         └─ application-dev.yml           (dev 环境:真实的库名/账号密码)
```

⚠️ 注意:仓库里**没有建表 SQL 文件、没有 mapper XML 目录、没有测试代码**,这三个属于「跑起来之前要补」的东西,见第五节和第六节。

### 0.3 一次「用户登录」请求的完整旅程(文件 + 方法 + 注解全链路)

```
POST /api/app/login  (请求体 JSON: {"phone":"138****8000","password":"123456"})
   │
   ▼ ① UserController.login()            [@RestController]
   │      @Validated 先做参数校验(手机号格式/密码长度,不通过→抛 MethodArgumentNotValidException)
   │      @RequestBody 把 JSON 反序列化成 UserLoginDTO
   ▼ ② UserService.login()                (接口 —— Controller 只依赖接口)
   ▼ ③ UserServiceImpl.login()            [@Service + @Autowired 注入两个依赖]
   │      userMapper.getByPhone() → 查不到抛 LoginFailedException("账号或密码错误")
   │      MD5 比对密码 → 不一致抛 PasswordErrorException
   │      查 status → DISABLE(1) 抛 AccountLockedException
   │      调私有方法 createToken(userId):JwtUtil.createJWT(jwtProperties 的密钥和过期时间)
   │      → 构建 UserVO(含 token) 返回
   ▼ ④ UserMapper.getByPhone()            [@Mapper + @Select]
   │      select * from user where phone = #{phone}  → 返回 User 实体
   ▼ ⑤ Controller 把 UserVO 包进 Result.success(vo)
        → JacksonObjectMapper 序列化 → JSON 回给前端
        { "code":1, "msg":"操作成功", "data":{ id,phone,nickname,token } }
```

注册流程完全相同,只是多走一次 `insert`(`@Insert + @Options(useGeneratedKeys)` 让自增主键回填到 `user.getId()`)。

### 0.4 登录之后的请求(拦截器在做什么)—— 目前是「断链」状态

```
后续请求: 前端在请求头 header 带  authentication: <登录返回的token>
   ▼ JwtTokenUserInterceptor.preHandle()   [implements HandlerInterceptor, 由 WebMvcConfiguration 注册]
   │      1. request.getHeader(userTokenName)  取 token(没有 → 解析异常 → 401)
   │      2. JwtUtil.parseJWT(userSecretKey, token) 验签
   │      3. 从 claims 取 userId → BaseContext.setCurrentId(userId)  (ThreadLocal)
   │      4. 放行 → Controller/Service 里随时能拿当前登录用户
   ▼ 业务方法...
   ▼ 请求结束:建议在 afterCompletion() 里 BaseContext.removeCurrentId() (现在没写,见第六节坑⑦)
```

**⚠️ 断链**:`WebMvcConfiguration` 注册拦截器时写的是 `addPathPatterns("/user/**")`,而 `UserController` 类上的前缀是 `@RequestMapping("/api/app")` —— **两套路径对不上,当前任何接口都不会被拦截,登录保护形同虚设**(详见第六节坑①)。

---

## 一、cityart-common —— 公共基础设施(14 个类)

### constant 包:全局常量,消灭魔法值

| 文件 | 作用 | 与谁关联 |
|---|---|---|
| `MessageConstant` | 约 27 条中文提示文案("操作成功""账号不存在"…) | `Result` 默认 msg、`UserServiceImpl` 抛异常、`GlobalExceptionHandler` 拼"xx已存在"。注意:里面大量菜品/套餐/订单文案是从苍穹外卖原样带过来的,cityart 目前用不到,是**预留** |
| `StatusConstant` | `ENABLE=0 / DISABLE=1` | 只有 `UserServiceImpl.login` 用(判断账号是否被禁用) |
| `JwtClaimsConstant` | JWT 载荷键名 `userId/empId/phone…` | `UserServiceImpl` 创建 claims 时放键,`JwtTokenUserInterceptor` 解析时取键 |
| `AutoFillConstant` | 反射方法名字符串 `setCreateTime…` | 只有 `AutoFillAspect` 用(见三) |

⚠️ `StatusConstant` 的取值和苍穹外卖**正好相反**(sky 是 ENABLE=1/DISABLE=0)。本工程内部逻辑自洽(新建用户默认 status=0 即启用、登录时 status==1 才锁),但建表时默认值必须写 0,别和笔记里的惯例混了。

### context / enumeration / exception

| 文件 | 作用 | 与谁关联 |
|---|---|---|
| `BaseContext` | **ThreadLocal 存"当前登录用户 id"**,静态方法 set/get/remove | 拦截器写入;未来的 AOP 自动填充、业务取 userId。**只有登录后的请求有值,注册/登录请求没有** |
| `OperationType` | `INSERT / UPDATE` 枚举 | `AutoFill` 注解的 `value()` 类型;`AutoFillAspect` 靠它决定填 4 个还是 2 个字段 |
| `BaseException` | 继承 RuntimeException 的业务异常基类 | 所有自定义异常继承它;`GlobalExceptionHandler` 只认它,子类全被兜住 |
| `LoginFailedException` | 手机号已注册 / 账号或密码错误 | `UserServiceImpl` 抛 |
| `PasswordErrorException` | 密码错误 | `UserServiceImpl.login` 抛(比 sky 多了这个,sky 密码错误也抛 LoginFailed) |
| `AccountLockedException` | 账号被禁用 | `UserServiceImpl.login` 抛 |

### json / properties / result / utils

| 文件 | 作用 | 与谁关联 |
|---|---|---|
| `json/JacksonObjectMapper` | 继承 Jackson 的 ObjectMapper;自定义 LocalDateTime → `yyyy-MM-dd HH:mm`(注意**没有秒**)、LocalDate → `yyyy-MM-dd`;未知属性不报错 | `WebMvcConfiguration.extendMessageConverters` 把它装进 `MappingJackson2HttpMessageConverter` 并插到转换器第 0 位 → **全项目 JSON 日期格式统一由它决定** |
| `properties/JwtProperties` | `@Component + @ConfigurationProperties(prefix="cityart.jwt")`,绑定 yml 的 `user-secret-key / user-ttl / user-token-name`(小驼峰自动匹配 kebab-case) | **两处消费**:`UserServiceImpl` 签发 token、`JwtTokenUserInterceptor` 验签和取 header 名 |
| `result/Result<T>` | **全后端统一返回体**:`code`(1 成功 / 0 失败)+ `msg` + `data`;静态方法 `success() / success(T) / error(msg)` | 所有 Controller 返回类型、`GlobalExceptionHandler` 的返回值,无一例外 |
| `utils/JwtUtil` | HS256 算法封装:`createJWT(密钥, 过期毫秒, claims)` 签发、`parseJWT(密钥, token)` 解析 | `UserServiceImpl.createToken` 签发;`JwtTokenUserInterceptor` 验签 |

---

## 二、cityart-pojo —— 纯数据对象(3 个类)

口诀不变:**dto 是"前端给我的"、vo 是"我给前端的"、entity 是"和表一一对应的"**。

| 文件 | 作用 | 与谁关联 |
|---|---|---|
| `entity/User` | 对应 user 表:id/phone/password/nickname/createTime/updateTime/status | `UserMapper` 的 SQL 结果与入参;**insert SQL 目前只写 4 列**(见坑⑤) |
| `dto/UserLoginDTO` | 注册 + 登录共用入参(phone、password),带校验注解 | `UserController.register/login` 的 `@RequestBody` 目标;`UserServiceImpl` 读它 |
| `vo/UserVO` | 出参:id + phone + nickname + **token** | `UserServiceImpl` 组装、Controller 放进 Result 返回 |

注意:注册和登录的入参一模一样,所以**共用一个 DTO**;如果以后注册要多收"验证码/昵称",就得拆成 `UserRegisterDTO` 和 `UserLoginDTO` 两个。

---

## 三、cityart-server —— 应用主体(10 个 Java 文件 + 2 个 yml)

### 包结构与职责

```
annotation  自定义注解(只做"标记",不含逻辑)
aspect      切面(把标记翻译成横切逻辑:AOP 自动填充)
config      配置类(注册拦截器、Knife4j 文档、静态资源、JSON 转换器)
handler     全局异常兜底(统一转成 Result)
interceptor JWT 拦截器(请求入口守卫)
controller  收参 → 调 service → 包 Result 返回
service     业务接口(Controller 只认接口)
service/impl @Service 实现(真正的业务逻辑)
mapper       MyBatis DAO 接口(注解 SQL)
resources    application.yml / application-dev.yml
```

### 逐文件职责 + 关联表

| 文件 | 关键内容 | 与谁关联 |
|---|---|---|
| `CityartApplication` | 启动类:`@SpringBootApplication` + main + 三个附加注解 | `SpringApplication.run()` 启动整个应用;注解由它"点亮"全项目(见 4-A) |
| `annotation/AutoFill` | 自定义注解,成员 `OperationType value()` | 标在 Mapper 的 insert/update 方法上;目前 **cityart 还没有任何 Mapper 方法贴它**(UserMapper 手写了 createTime) |
| `aspect/AutoFillAspect` | `@Pointcut(execution(* com.cityart.mapper.*.*(..)) && @annotation(AutoFill))` + `@Before`;反射调用 setCreateTime/setCreateUser/setUpdateTime/setUpdateUser | 消费 `AutoFill` 注解、`OperationType`、`AutoFillConstant`、`BaseContext`(取当前操作人);**同样处于"已架好、没启用"状态** |
| `config/WebMvcConfiguration` | ① 注册 JwtTokenUserInterceptor(拦 `/user/**`,放行 login/register);② `@Bean` 注册 Knife4j Docket(扫 `controller.user` 包);③ `/doc.html` 静态资源映射;④ 注入 JacksonObjectMapper 扩展消息转换器 | 和 interceptor、JacksonObjectMapper、knife4j、UserController **四个方向耦合**,是 server 里牵扯最多的类 |
| `interceptor/JwtTokenUserInterceptor` | `preHandle`:取 header `authentication` → 验签 → 写 BaseContext;失败返回 401 | 被 WebMvcConfiguration 注册;消费 JwtProperties/JwtUtil/JwtClaimsConstant/BaseContext |
| `handler/GlobalExceptionHandler` | `@ExceptionHandler` 三个重载:BaseException / 参数校验异常 / SQL 唯一键冲突 | 兜住 Service 抛的一切业务异常;消费 Result、MessageConstant |
| `controller/user/UserController` | `@RequestMapping("/api/app")`;register + login 两个 `@PostMapping` | → UserService 接口 →(impl)→ UserMapper |
| `service/UserService` | 接口:`register` / `login` | Controller 依赖接口,实现靠 Spring 注入 UserServiceImpl |
| `service/impl/UserServiceImpl` | 见 0.3 流程图 | 消费 UserMapper / JwtProperties / JwtUtil / 三个异常 / DigestUtils |
| `mapper/UserMapper` | `getByPhone(@Select)`、`insert(@Insert + @Options 回填主键)` | 只有 UserServiceImpl 调用;SQL 直接对 user 表 |

### 方法级关联表(文件 ↔ 方法 ↔ URL ↔ SQL)

| 请求 | Controller 方法 | Service 接口方法 | impl 实现 | Mapper 方法 + SQL | 异常出口 |
|---|---|---|---|---|---|
| POST /api/app/register | `register(@Validated @RequestBody UserLoginDTO)` | `register` | `UserServiceImpl.register` | `getByPhone` → `insert`(回填 id) | LoginFailedException("该手机号已注册") |
| POST /api/app/login | `login(...)` | `login` | `UserServiceImpl.login` | `getByPhone` | PasswordError / AccountLocked / LoginFailedException |

| Mapper 方法 | SQL(注解内) | 涉及 user 表列 | 实体回填 |
|---|---|---|---|
| `getByPhone(String phone)` | `select * from user where phone = #{phone}` | 全部列 | → User |
| `insert(User user)` | `insert into user(phone,password,nickname,create_time) values(...)` | 只 4 列 | `@Options` 把自增 id 写回 `user.id` |

| 切面/拦截器/转换器 | 触发条件 | 生效方法 |
|---|---|---|
| `AutoFillAspect.autoFill` `@Before` | 切点 = 任意 mapper 方法且带 `@AutoFill` | **当前 0 个方法触发**(想启用:给 insert/update 方法加注解,并把实体补上 setCreateTime 等) |
| `JwtTokenUserInterceptor.preHandle` | URL 命中 `/user/**` | **当前 0 个 URL 命中**(路径写错,见坑①) |
| JacksonObjectMapper | 所有 Controller 返回体序列化 | 全部接口,日期格式 `yyyy-MM-dd HH:mm` |

---

## 四、注解全手册 —— 每个注解:含义 + 是否必须

> **必须性三档说明**:
> - **[必须]** —— 没有它,工程/该功能根本跑不起来(语法或框架硬性要求);
> - **[视功能]** —— 你要实现某能力(事务、校验、文档…)就必须要,不做就不需要;
> - **[建议/可选]** —— 不加也能跑,但规范或省事程度决定最好加。
>
> 一个注解本质上是一个"接口",`@interface` 只是声明它的语法;`value()` 是它的成员(属性)。

### A. 启动类 / 全局开关(2 个文件里的 6 个注解)

| 注解 | 出现位置 | 含义 | 必须? |
|---|---|---|---|
| `@SpringBootApplication` | CityartApplication | 组合注解 = `@SpringBootConfiguration` + `@EnableAutoConfiguration` + `@ComponentScan`。声明"这是 Spring Boot 应用",并**自动扫描当前包 `com.cityart` 及其子包**找 Bean | **[必须]** 启动类必须有,全工程有且仅有一个 |
| `@EnableTransactionManagement` | 同上 | 开启注解事务 —— 让 `@Transactional` 生效 | **[视功能]** 用到 `@Transactional`(多表增改)才必须;目前 cityart 还没有任何事务方法,可以删 |
| `@EnableCaching` | 同上 | 开启 Spring Cache 抽象 —— 让 `@Cacheable/@CacheEvict` 生效 | **[视功能]** 用到缓存注解才必须;目前无用,可删 |
| `@Slf4j` | 6 个类 | Lombok 编译期生成 `log` 字段(`log.info(...)`) | **[建议]** 想打日志就必须有;嫌它可写 `LoggerFactory.getLogger()` 手动造,但要写更多样板 |
| `@Override` | UserServiceImpl | 标记重写接口方法;编译器帮你检查方法签名有没有写错 | **[建议]** 不是必须,但建议写 |
| `implements Serializable` | Result / DTO / VO | 不是注解,是标记接口:对象可被序列化(网络传输/缓存/磁盘) | **[建议]** 规范要求;不写当前也能跑 |

### B. Controller 层(8 个)

| 注解 | 含义 | 必须? |
|---|---|---|
| `@RestController` | = `@Controller` + `@ResponseBody`:声明这是 Controller,且**返回值直接序列化成 JSON 写进响应体** | **[必须]** 写接口层类必加;想返回页面/视图的才用 @Controller |
| `@RequestMapping` | 声明请求路径映射;可加在类上(公共前缀)或方法上 | **[必须]** 类上加它定前缀(`/api/app`)+ 方法上用 `@PostMapping` 缩写;也可类上不写、方法上写全路径 |
| `@PostMapping` | `@RequestMapping(method=POST)` 的缩写 | **[必须之一]** 一个对外方法必须有请求映射注解(`@GetMapping/@PostMapping/@PutMapping/@DeleteMapping` 至少一个),否则没人能访问 |
| `@RequestBody` | 把请求体 JSON 反序列化为后面的对象(UserLoginDTO) | **[视功能]** 前端以 JSON 提交时必须有;如果是 URL 参数/表单就不用,而用 `@RequestParam` 或直接接对象 |
| `@Validated` | 开启方法参数校验:检查 DTO 字段上的 `@NotBlank/@Pattern/@Size`,不通过抛 `MethodArgumentNotValidException` | **[视功能]** 要校验入参就加(同时 DTO 里要有校验注解 + 引入 starter-validation);目前两个接口都加了,很有必要 |
| `@Autowired` | 依赖注入:让 Spring 把 UserService 的实现类注入进来 | **[视功能]** 要注入就加;也可用构造器注入或 `@Resource` 替代,但**字段注入 + @Autowired 是本工程(也是 sky 模板)的惯例** |
| `@Api` / `@ApiOperation` | Knife4j/Swagger 接口文档:分组 tags、接口说明 | **[可选]** 纯文档增强;删掉接口照样工作(对应 yml 里的 knife4j 依赖也可删) |
| `@Slf4j` | 同 A 组 | [建议] |

> 冷知识:`@RestController` 里没有标"必须返回 Result"——那是**工程约定**,靠 GlobalExceptionHandler + Result 类保证,框架不强制。

### C. Service 层(2 个)

| 注解 | 含义 | 必须? |
|---|---|---|
| `@Service` | 标记业务层实现类,交给 Spring 管理成 Bean(便于 @Autowired 注入) | **[必须]** 写分层项目的 Service 实现类就要加(换成 @Component 也能注入,但语义不专业) |
| `@Override` | 见 A | [建议] |
| (接口 UserService) | 接口本身**不需要任何注解** | — |

> 为什么 sky/cityart 要"接口 + 实现"两层?Controller 只依赖接口,以后实现可以换、可以 AOP 代理,是分层规范。**小项目也可合并成一个类,不是硬性要求。**

### D. Mapper 层(4 个)

| 注解 | 含义 | 必须? |
|---|---|---|
| `@Mapper` | 告诉 MyBatis 生成该接口的代理实现并注册成 Bean | **[必须之一]** Mapper 接口必须被识别;要么每个接口加 `@Mapper`,要么启动类上加 `@MapperScan("com.cityart.mapper")` 一次扫全部(**二选一,本工程用的是前者**) |
| `@Select` | 注解 SQL:方法上写 `select * from user where phone=#{phone}`,`#{phone}` 绑定参数 | **[二选一]** SQL 要么写在注解里(简单 SQL 用这个),要么写 XML(`resources/mapper/*.xml` 配合 `mybatis.mapper-locations`)再配 `<select id="方法名">`,两种方式取其一。本工程 user 表简单,注解够用 |
| `@Insert` | 注解插入 SQL | 同 @Select |
| `@Options(useGeneratedKeys=true, keyProperty="id")` | 让数据库自增主键**回填**到实体 User 的 id 字段 | **[视功能]** 表是自增主键且你后面要用这个 id(签发 token 要用!)就**必须**加;不用自增(如雪花 id 手动 set)则不需要 |

> `#{phone}` 与字符串拼 SQL 的区别:预编译占位符,防 SQL 注入 —— 永远别用 `$` 拼参。
> 多参数方法(比如按手机号+状态查)需要给每个参数加 `@Param("xxx")`,或用对象整体传参,本项目暂未涉及。

### E. 数据载体层 = Lombok + 校验 + Swagger(pojo 里 10 个)

| 注解 | 含义 | 必须? |
|---|---|---|
| `@Data` | Lombok:自动生成所有字段的 getter/setter(外加 toString/equals/hashCode) | **[视功能]** 想少写样板代码就加;手写 getter/setter 就不需要。**想用就要 IDEA 装 Lombok 插件 + pom 依赖** |
| `@Builder` | 生成链式构造器:`User.builder().phone(..).password(..).build()` | **[视功能]** 代码里用了链式构造(ServiceImpl 里两处 User.builder() 和 UserVO.builder())就**必须**有 |
| `@NoArgsConstructor` | 生成无参构造 | **[重要配套]** `@Builder` 会把默认无参构造"挤掉",而 MyBatis 反射建对象、JSON 反序列化都要无参构造 → **只要用了 @Builder,基本必须配上 @NoArgsConstructor**(必要时再配 @AllArgsConstructor 给 builder 用)。这也是 User/UserVO 三者一起写的原因 |
| `@AllArgsConstructor` | 生成全参构造 | [视功能,配 @Builder 常用] |
| `@NotBlank(message="…")` | 参数校验:字符串不能为 null 且不能是空串/纯空格 | **[视功能]** 想拦非法入参就加;必须与 `@Validated`/`@Valid` 配合才生效,且依赖 `spring-boot-starter-validation` |
| `@Pattern(regexp="^1[3-9]\\d{9}$")` | 正则校验:手机号格式 | 同上 |
| `@Size(min=6,max=16)` | 长度校验:密码 6~16 位 | 同上 |
| `@ApiModel` / `@ApiModelProperty` | Knife4j 文档:描述"这是个啥对象/字段" | [可选] |
| `implements Serializable` | 见 A | [建议] |

> 校验注解(message="中文提示")的提示语 → 校验失败抛 `MethodArgumentNotValidException` → `GlobalExceptionHandler` 取出第一条 message 返回前端。**这条"注解 → 异常 → 全局兜底 → 前端提示"链路是整体设计的精髓。**

### F. 配置与属性绑定类(4 个)

| 注解 | 含义 | 必须? |
|---|---|---|
| `@Configuration` | 标记配置类,里面的 `@Bean` 才会被处理、方法才会被调用注册 | **[必须]** 写"手动装配的配置类"就加 |
| `@Bean` | 把方法返回值(如 Docket)注册成 Spring Bean,方法名即 Bean 名 | **[视功能]** 需要手动创建非 `@Component` 的组件(第三方类、带参数构建的类)时用;若类自己能加 @Component 则不需要 @Bean |
| `@Component` | 通用组件注册。JwtProperties、JwtTokenUserInterceptor、AutoFillAspect 三个类都靠它进容器 | **[必须之一]** 非 Controller/Service/Mapper 的类要能被注入,就得有 `@Component`/`@Configuration`/`@Bean` 之一 |
| `@ConfigurationProperties(prefix="cityart.jwt")` | 把 yml 里 `cityart.jwt.user-secret-key` 等 kebab-case 键,自动映射成类的 userSecretKey 小驼峰字段(类型安全配置) | **[视功能]** 想优雅读配置就加;**必须同时满足**:类上有 `@Component`(本工程)或启动类 `@EnableConfigurationProperties(JwtProperties.class)`。配了它,yml 里漏键会启动失败/注入 null,字段名必须与键对应 |

### G. 自定义注解与 AOP(6 个)

| 注解 | 含义 | 必须? |
|---|---|---|
| `@interface` | 声明"这是一个注解类型",不是 Java 关键字而是语法(AutoFill 注解本体) | **[必须]** 自定义注解的声明方式 |
| `@Target(ElementType.METHOD)` | 元注解:该注解**允许标在哪**。可选值:TYPE(类)/METHOD(方法)/FIELD(字段)/PARAMETER(参数)… | **[必须]** 每个自定义注解都要标注它声明作用范围(想标类就 TYPE,想标方法就 METHOD,可逗号并列) |
| `@Retention(RetentionPolicy.RUNTIME)` | 元注解:注解**活到什么时候**。SOURCE(仅源码)/CLASS(默认,编译进字节码)/**RUNTIME(运行时可通过反射读)** | **[必须]** **AOP/反射要用就必须 RUNTIME**,否则切面 `getAnnotation()` 拿到 null |
| `@Aspect` | 标记切面类 | **[必须]** 写 AOP 切面类就加 |
| `@Pointcut("execution(* com.cityart.mapper.*.*(..)) && @annotation(...)")` | 定义切入点表达式:哪些方法被拦 | **[视功能]** 可用可不用(可直接把表达式写进通知注解);但表达式是 AOP 的核心,建议单独抽取 |
| `@Before("autoFillPointCut()")` | 前置通知:目标方法执行**前**先跑这个方法。还有 @After/@AfterReturning/@Around | **[必须之一]** 一个切面至少有一个通知注解(想织入哪个时机用哪个),否则 @Aspect 没意义 |

> `AutoFill` 的整体设计链:**自定义注解(带 OperationType 成员)→ 贴到 Mapper 方法 → @Aspect+@Pointcut 拦截 → @Before 里反射 setXxx → 值来自 LocalDateTime.now() 和 BaseContext**。这套是 sky 的经典考题(AOP 公共字段填充),cityart 原样保留但还没接上(见坑④)。

### H. 异常处理(2 个)

| 注解 | 含义 | 必须? |
|---|---|---|
| `@RestControllerAdvice` | 全局 Controller 增强 = `@ControllerAdvice` + `@ResponseBody`:拦截所有 Controller 抛出的异常,把返回值当 JSON 写回 | **[必须]** 做"统一异常出口"就必须用(或用 @ControllerAdvice+方法 @ResponseBody) |
| `@ExceptionHandler` | 标注"这个方法专门处理 XX 异常";不写参数 = 处理方法参数类型;Spring 按**最精确匹配**,所以三个重载分别接住业务异常/校验异常/SQL 异常 | **[必须]** 与 @RestControllerAdvice 配套;方法多了可拆多个类 |

### I. 拦截器 —— 没有注解,靠"接口 + 注册"

`JwtTokenUserInterceptor` 上只有 `@Component` 和 `@Slf4j`。拦截逻辑靠 **implements HandlerInterceptor**(接口)提供 preHandle/postHandle/afterCompletion 三个钩子;**真正让它生效的是 WebMvcConfiguration 里的注册代码**(addInterceptor + 路径),所以"写了拦截器类但没注册 = 不生效" —— 这正是坑①的根源。

### J. 小结:一个功能从"能跑"到"规范"最少要哪些注解

| 层次 | 语法必须(缺一不可) | 规范/建议(不强制的工程习惯) |
|---|---|---|
| 入口 | `@SpringBootApplication` | `@Slf4j` |
| Controller | `@RestController` + 一个映射注解(@PostMapping…) + 类级 @RequestMapping | `@Api/@ApiOperation`、`@Validated`、`@RequestBody` |
| Service 实现 | `@Service` | 接口化、`@Override` |
| Mapper | `@Mapper` + SQL 注解(@Select 等) | `@Options` 回填主键 |
| 对象 | —(纯 POJO 无注解也能用) | `@Data/@Builder/@NoArgsConstructor`、校验注解、`Serializable` |
| 全局兜底 | —(不做就不做) | `@RestControllerAdvice + @ExceptionHandler`(强烈建议) |
| 装配 | — | `@Configuration + @Bean`、`@Component`、`@ConfigurationProperties` |
| 进阶 | — | AOP 三件套(@Aspect/@Pointcut/@Before)、自定义注解(必须 @Target+@Retention RUNTIME)、`@MapperScan` |

---

## 五、必备文件清单 —— "只要写项目就必须有"的文件

> 下面的答案按「**Spring Boot + Maven 后端项目**」理解。分五级:**缺了 P0 跑都跑不起来 → 缺了 P4 也能上线,只是少了便利**。

### P0 —— 任何 Spring Boot 工程都缺不得(共 3 类)

| 文件 | 为什么必须有 | 缺了会怎样 |
|---|---|---|
| **pom.xml**(父 + 子模块) | Maven 的身份证与依赖清单:父 pom 管版本、子 pom 管各自依赖 | 无法构建/无法导包,IDE 不认识工程 |
| **启动类 XxxApplication.java**(带 `@SpringBootApplication` + main) | Spring Boot 的入口:main 里 `SpringApplication.run()`;注解负责自动配置 + 扫包 | `java -jar` 起不来,没有"服务器" |
| **resources/application.yml**(或 .properties) | 一切运行参数:端口、数据库、日志、mybatis、jwt 密钥… | 严格说 Spring Boot 有默认值能裸启动(端口 8080),但**连不上库、没密钥、没环境区分**,真实项目必写。**它就是"项目配置清单",写项目必须有** |

> 工程细节上还要有:`src/main/java`、`src/main/resources` 目录,JDK + Maven 环境,IDE 配 Lombok 插件(用 @Data/@Slf4j 的话)。

### P1 —— 只要做"连数据库的 Web 接口"就必须有

| 文件/依赖 | 说明 |
|---|---|
| `spring-boot-starter-web` | 无它就不是 web 项目(@RestController 全失效) |
| 数据源依赖 + 配置 | 本项目:`mysql-connector-java`(runtime)+ `druid-spring-boot-starter`;yml 里 datasource 段。⚠️ 其实 **Druid 不是必须的** —— Boot 默认自带 HikariCP 连接池,sky/cityart 只是显式换成 Druid |
| `mybatis-spring-boot-starter` | 用 MyBatis 就必须;用 JPA 就不需要。yml 里 `mybatis` 段(驼峰映射等) |
| **Mapper 接口**(@Mapper) | 没有 DAO 层,Service 无法碰数据库 |
| **Entity 实体类** | MyBatis 结果映射的载体 |
| **建表 SQL(.sql 文件)** | ⚠️ **cityart 目前缺失!**代码假定 user 表已存在。任何人拿到项目要跑,得先建 `city_art` 库 + user 表。补一个 `db/user.sql` 是"写带库项目"该有的文件 |

### P2 —— 只要做"带登录鉴权的对外接口"的正式项目,几乎必须有(不写会很难看)

| 文件 | 作用 | 不写的后果 |
|---|---|---|
| `Result<T>`(统一返回体) | 前后端约定 code/msg/data 结构 | 每个接口返回格式不一,前端没法统一处理 |
| `GlobalExceptionHandler`(@RestControllerAdvice) | 异常统一转 Result | 未捕获异常返回 Spring 默认 500 错误页/堆栈,不规范也暴露内部 |
| 自定义业务异常基类 + 子类 | 用"抛异常"表达业务失败 | 靠返回值判断,代码全是 if 分支 |
| **JWT 四件套**:JwtUtil + JwtProperties + 拦截器类 + 在 WebMvcConfiguration 注册 | 签发 token、验签放行、把用户 id 写进 BaseContext | 接口裸奔,人人可冒充登录用户(目前 cityart 因路径写错正处于此状态,见坑①) |
| BaseContext(ThreadLocal) | 一次请求内随处可取"当前用户" | 每个方法手动把 userId 传来传去 |
| 常量类(MessageConstant/StatusConstant/JwtClaimsConstant) | 消灭魔法值 | 字符串散落,改一处全崩 |

### P3 —— 按功能取舍的增强件(不是"写项目"必须有,是"你这个项目"可选)

| 东西 | 说明 |
|---|---|
| Knife4j 文档(`/doc.html`) + @Api 系列注解 | 调试利器,纯增强。sky 模板标配,删依赖连注解一起删 |
| `spring-boot-starter-validation` + @NotBlank/@Pattern/@Size | 参数校验,强烈建议做,但不是框架强制 |
| AOP 自动填充(AutoFill + AutoFillAspect) | sky 考题级设计,当前未接线,可先不做 |
| Redis / Cache / WebSocket / POI / OSS / 微信支付依赖 | **cityart 的 pom 全部引入了但零使用**(从 sky 抄来的);做订单/报表/缓存时才用得上。**不留着也行,不影响任何现有功能** |
| PageHelper | 分页 starter,同样引入未用,做列表页时启用 |
| spring-boot-configuration-processor | 仅给 IDE 配置提示,optional,留着无妨 |

### P4 —— 文件与方法关联的"最小闭环"复习图

写**任何一个新功能**,你都必须产出的关联文件链(照抄即对):

```
① 建表 SQL(.sql)              user 表
② entity 类                   列 ↔ 字段   (要 @Builder 就配 @NoArgsConstructor)
③ Mapper 接口 + 注解 SQL(@Mapper/@Select/@Insert…)    方法名 ↔ SQL id
④ Service 接口               方法签名 ↔ Controller 调用点
⑤ ServiceImpl(@Service)       @Override 实现;业务抛 BaseException 子类
⑥ Controller(@RestController) 路径映射 ↔ URL;返回 Result<T>
⑦ DTO(入参,加校验) ↔ VO(出参)  与前端字段对齐
⑧ 跑起来:启动类扫包 ✓ → knife4j 或 Postman 调 /doc.html 验证
```

---

## 六、当前代码的问题与坑(从苍穹外卖改造时留下的"对不上")

> 建议对照代码逐个修,修完你对这套骨架的理解会上一个台阶。

**①(最重要)拦截器路径 vs Controller 路径不一致 —— JWT 校验实际没生效**
- `WebMvcConfiguration`:`addPathPatterns("/user/**")`、放行 `"/user/login"`、`"/user/register"`;
- `UserController` 前缀却是 `@RequestMapping("/api/app")`,真实 URL 是 `/api/app/login`、`/api/app/register`;
- 结果:`/api/app/**` 一个都不在 `/user/**` 下 → **拦截器永远不触发,任何请求都不验 token**;
- 修法:要么 Controller 改成 `/user` 前缀,要么(更符合"api/app"语义)把注册改成:
  ```java
  registry.addInterceptor(jwtTokenUserInterceptor)
          .addPathPatterns("/api/app/**")
          .excludePathPatterns("/api/app/login")
          .excludePathPatterns("/api/app/register");
  ```
  放行注册/登录是对的(登录前哪来的 token),但**其他 user 端接口必须被拦**。

**② yml 里 `cityart.jwt.admin-*` 是残留** —— JwtProperties 只声明了 user 三件套,admin 配置无类绑定也无代码消费,可删可留。

**③ 没有建表 SQL** —— 库 `city_art`(application-dev.yml)、表 `user` 全靠手建。补一份:
```sql
CREATE DATABASE IF NOT EXISTS city_art DEFAULT CHARACTER SET utf8mb4;
USE city_art;
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    phone VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(64) NOT NULL,
    nickname VARCHAR(32),
    create_time DATETIME,
    update_time DATETIME,
    status INT DEFAULT 0          -- 注意:本项目 0=启用(与 sky 惯例相反)
);
```

**④ `@AutoFill`/切面"架好了但没接上"** —— UserMapper.insert 手动 set 了 createTime,没贴 `@AutoFill`;如果未来想在 insert/update 里统一自动填公共字段,记得:贴注解 + 实体里要有 setCreateUser(Long)/setUpdateUser(Long)(User 实体目前**没有**这两个字段,User 表也没有 create_user/update_user 列,别照 sky 硬贴)。

**⑤ insert 只写 4 列** —— `update_time`、`status` 依赖数据库默认值(建表 SQL 必须给默认,否则插不进去);也可在 SQL/代码里显式补。

**⑥ Jackson 时间格式没秒** —— `JacksonObjectMapper.DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm"`,所有 LocalDateTime 序列化后会丢秒;要精确到秒改成 `yyyy-MM-dd HH:mm:ss`(注释里已留了备选)。

**⑦ ThreadLocal 没清理(内存泄漏隐患)** —— 拦截器只 set 不 remove;Tomcat 线程复用下,建议补:
```java
@Override
public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    BaseContext.removeCurrentId();
}
```

**⑧ 调试残留** —— `JwtTokenUserInterceptor` 里 `System.out.println("当前线程id…")` 应删;同文件 `log.info("当前用户id：", userId)` 少了 `{}` 占位符,userId 打不出来,改成 `log.info("当前用户id：{}", userId)`。

**⑨ MD5 无盐加密** —— `DigestUtils.md5DigestAsHex` 学习够用,生产建议 BCrypt(Spring Security 自带)等带盐算法。

**⑩ pojo/pom.xml 锁了 jackson-databind 2.9.2** —— Spring Boot 2.7.3 自带 2.13.x,显式降级有版本冲突隐患,建议删掉 `<version>`。

**⑪ `WebMvcConfiguration extends WebMvcConfigurationSupport`** —— sky 模板写法,能跑(它自己补配了静态资源与转换器),但会关闭 Boot 大量 Web MVC 自动配置;**更推荐的现代写法是 `implements WebMvcConfigurer`**,两者风格二选一,别混。

**⑫ 密码入库/出参安全** —— UserVO 不带 password ✓(只回 phone/nickname/token),这个习惯要一直保持:entity 可以带 password,VO/DTO 不该暴露密码字段。

**⑬ 大量 sky 预留** —— MessageConstant 里的菜品/套餐/订单文案、common/server pom 里的 OSS/微信支付/POI/WebSocket、yml 的 admin-jwt,都是原模板的"预告",等 cityart 业务长大再逐个启用或清理,不影响现在跑通。

---

## 七、一句话总结

> **cityart = 苍穹外卖的三层多模块骨架 + 只实现了 C 端注册登录。**
> 骨架期最有价值的是把「约定」立住了:包根统一 → 扫包即全量装配;`Result` 统一返回 + `@RestControllerAdvice` 统一异常 + `MessageConstant` 统一文案;JWT 签发/验签/ThreadLocal 一条线;JacksonObjectMapper 统一日期。**你要练的,就是照 0.3 的调用链和第五节 P4 的闭环,一个功能一个功能地把 UserController 旁边的"兄弟们"补出来**,顺带把第六节 ①③④ 三处断链接上。
