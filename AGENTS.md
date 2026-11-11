# AGENTS.md

## 项目概览

CsuPlusPlus 是高校信息平台的后端 API：ASP.NET Core Web API + EF Core，使用 Controller → Service → DbContext 分层，DTO 出参，邮箱密码登录与 JWT Bearer 认证。

## 技术栈

- SDK：.NET 11（`global.json` 固定 `11.0.100-preview.7.*`，允许预览版），目标框架 `net11.0`。
- Web：ASP.NET Core Controller、OpenAPI（`Microsoft.AspNetCore.OpenApi`），开发环境使用 Scalar UI。
- 数据：EF Core 11 RC。Development 用 SQLite，其他环境（含 Production）用 Npgsql/PostgreSQL。
- 认证：JWT Bearer（`Microsoft.AspNetCore.Authentication.JwtBearer`）+ ASP.NET Core PasswordHasher。
- 版本以 `CsuPlusPlus/CsuPlusPlus.csproj` 为准，升级时保持所有 EF Core 包版本一致。

## 目录结构

```text
CsuPlusPlus/
├── Controllers/            # <Area>Controller.cs，仅做参数接收、结果映射、HTTP 状态码
├── Services/<Area>/        # I<Area>Service.cs + <Area>Service.cs + 结果 record/enum
├── Dtos/<Area>/            # 请求/响应 record，DataAnnotations 校验
├── Models/                 # EF 实体（[Table]、[Index]）与 ValueObjects/ 值对象
├── Authorization/          # Policies 常量与授权策略
├── Options/                # 配置绑定类
├── Infra/                  # AppDbContext、AppDbContextFactory、Migrations/Postgres
└── Program.cs              # DI 注册、JWT、数据库初始化、中间件
```

## 架构与代码规范

### 分层

- Controller 不直接访问 `AppDbContext`，只依赖 `I<Area>Service`。
- Service 通过主构造函数注入 `AppDbContext`；跨实体查询用 LINQ + `AsNoTracking()`。
- 接口与实现位于同一 `Services/<Area>` 目录；在 `Program.cs` 以 `AddScoped<I<Area>Service, <Area>Service>()` 注册。
- 写入操作必须调用 `SaveChangesAsync(cancellationToken)`，异步方法以 `Async` 结尾并透传 `CancellationToken`。

### 结果模式

- Service 返回 `record XxxResult(XxxStatus Status, XxxResponse? Response = null)` 与对应 `enum`，不使用异常表达业务失败。
- Controller 用 `switch` 映射结果：成功 `Created(uri: null as Uri, value: ...)` 或 `Ok(...)`，业务失败用 `Problem(statusCode:, title: "中文提示。")`，未知状态 `throw new InvalidOperationException("未知结果。")`。
- 典型状态码：重名 409、资源不存在 404、参数/关联无效 400、未认证 401。

### 模型与校验

- 实体使用 `required` 属性、`[Table("snake_case_plural")]`，唯一约束写在 `[Index(..., IsUnique = true)]`。
- 值对象（`NonEmptyName`、`MailAddress`、`Nickname`）在构造函数中校验，作为 EF `[ComplexType]` 使用。
- 请求 DTO 用 `record` + `required`/`init`，通过 `[Required]`、`[Range]`、`[StringLength]` 等 DataAnnotations 校验，错误消息用中文。
- 响应 DTO 放在 `Dtos/<Area>/`，不直接返回实体。

### 认证与授权

- 策略常量集中在 `Authorization/Policies.cs`；管理类接口加 `[Authorize(Policies.ManageUniversity)]`（要求 `UserRole.Admin`）。
- 需要登录的接口用 `[Authorize]`，公开接口用 `[AllowAnonymous]` 或保持无特性，与现有 Controller 保持一致。
- JWT 配置来自 `Jwt` 配置节；`Secret` 至少 32 字节，生产通过环境变量 `Jwt__Secret` 提供。

### 代码风格

- 文件作用域命名空间、一个公共类型一个文件、4 空格缩进、nullable 与 implicit usings 已启用。
- 新代码不加解释性注释；仅在必要处保留结构性注释（如 `Program.cs` 的分段）。
- 命名与中文提示语沿用现有代码，不引入新的响应包装结构。

## 常用命令

均在仓库根目录执行：

```bash
dotnet restore
dotnet build --nologo
dotnet run --project CsuPlusPlus --launch-profile http   # http://localhost:5253
```

- OpenAPI：`/openapi/v1.json`、`/openapi/v1-3.1.json`；Development 另可访问 Scalar UI。
- 提交前至少执行 `dotnet build`，本项目当前没有测试工程。

## 数据库

| 环境           | 数据库                                 | 启动行为                           |
|----------------|----------------------------------------|------------------------------------|
| Development    | SQLite（`Data Source=csuplusplus.db`） | `EnsureCreatedAsync()`，不应用迁移 |
| 非 Development | PostgreSQL                             | `MigrateAsync()`                   |

- `EnsureCreatedAsync()` 不会更新已有表结构；修改实体后需停止应用、删除本地 `csuplusplus.db` 并重新启动。
- 不要混用 `EnsureCreatedAsync()` 与迁移，也不要把 PostgreSQL 迁移用于本地 SQLite。
- 初始化不写入种子数据，注册时的 `CollegeId` 必须指向已存在的学院。

### 生成迁移

工具版本必须与 `CsuPlusPlus.csproj` 的 EF Core 版本一致（当前为 `11.0.0-rc.2.*`）：

```bash
dotnet tool install --global dotnet-ef --version <与 EF Core 一致的版本>
dotnet ef migrations add <ChangeName> --project CsuPlusPlus \
  --output-dir Infra/Migrations/Postgres -- --environment Production
dotnet ef migrations has-pending-model-changes --project CsuPlusPlus -- --environment Production
```

- 必须带 `-- --environment Production`，让 `Infra/AppDbContextFactory` 选择 Npgsql。
- 一起提交迁移 `.cs`、`.Designer.cs` 与更新后的 `AppDbContextModelSnapshot.cs`。
- 已应用到生产的历史迁移不要删除或改写，用新迁移表达变更。

## 部署

- `.github/workflows/deploy.yml` 仅在推送 `main` 时触发：构建镜像 → 推送 GHCR → SSH 执行 `scripts/deploy.sh` 更新 `csuplusplus` 容器。
- 服务器配置放在 `/opt/csuplusplus/app.env`，至少包含 `ConnectionStrings__DefaultConnection` 与 `Jwt__Secret`，不要提交到 Git。
- 生产容器映射 `8080`，部署以 `/health/check` 返回 `200` 作为成功判据；数据库迁移在应用启动时自动执行且不会回滚。
- 详细步骤见 `README.md`（注意其中 .NET 10 / dotnet-ef 10.0.13 的描述已过时，以本文件和 csproj 为准）。

## 当前接口

| 方法 | 路径                 | 鉴权  | 说明                                                |
|------|----------------------|-------|-----------------------------------------------------|
| POST | `/auth/register`     | 匿名  | 邮箱、密码、昵称、学院 ID 注册，返回 JWT 与用户 DTO |
| POST | `/auth/login`        | 匿名  | 邮箱密码登录                                        |
| GET  | `/profile/me`        | JWT   | 当前用户资料                                        |
| GET  | `/health/check`      | 匿名  | 健康检查（部署探测用）                              |
| POST | `/university/create` | Admin | 创建大学                                            |
| POST | `/college/create`    | Admin | 在指定大学下创建学院（请求体含 `universityId`）     |

受保护接口使用请求头：

```http
Authorization: Bearer <token>
```
