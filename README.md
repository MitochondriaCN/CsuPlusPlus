# CsuPlusPlus

基于 .NET 10、ASP.NET Core Web API 和 EF Core 的后端项目。使用 Controller → Service → DbContext 分层，通过 DTO 返回接口数据，使用邮箱密码登录和 JWT Bearer 认证。

## 环境要求

- 开发、构建：.NET 10 SDK。
- 生产数据库：PostgreSQL。
- 生成迁移：与项目 EF Core 版本一致的 `dotnet-ef` 工具，目前为 `10.0.13`。

以下开发命令均在仓库根目录执行。

## 本地运行

```bash
dotnet restore
dotnet run --project CsuPlusPlus --launch-profile http
```

默认监听 `http://localhost:5253`。开发环境可访问 `/openapi/v1.json` 获取 OpenAPI 文档。

数据库和 JWT 配置分别从 `appsettings.json`、`appsettings.Development.json` 加载。开发连接字符串为 `Data Source=csuplusplus.db`；SQLite 文件路径相对于进程工作目录，使用上述启动方式时通常位于项目目录内。

### 数据库初始化策略

| 环境                              | 数据库     | 启动行为                                       |
|-----------------------------------|------------|------------------------------------------------|
| Development                       | SQLite     | `EnsureCreatedAsync()`，初始化空库，不应用迁移 |
| 非 Development（包括 Production） | PostgreSQL | `MigrateAsync()`，执行尚未应用的迁移           |

`EnsureCreatedAsync()` 不会更新已有表结构，也不会在数据库已有表时自动补齐缺失表。修改实体或映射后，如果需要更新本地 SQLite 结构，应先停止应用，确认本地数据可以丢弃，再删除对应的 `csuplusplus.db`，下次启动会重新建表。

开发 SQLite 是可重建的开发数据库；当前仓库只维护 PostgreSQL 迁移，不要把这套迁移直接用于 SQLite。也不要对同一个数据库混用 `EnsureCreatedAsync()` 和迁移初始化。

初始化只创建结构，不会自动添加大学、学院等业务数据。注册请求中的 `collegeId` 必须指向已存在的学院。

## 生成 PostgreSQL 迁移

### 1. 安装工具

首次使用时安装：

```bash
dotnet tool install --global dotnet-ef --version 10.0.13
```

如果已经安装其他版本，可以更新到当前项目使用的版本：

```bash
dotnet tool update --global dotnet-ef --version 10.0.13
```

确保 .NET SDK 和全局工具目录在 `PATH` 中。升级项目 EF Core 时，同步调整工具版本。

### 2. 修改模型后生成迁移

仓库已包含初始迁移 `InitialCreate`，无需重复生成。后续例如增加课程描述字段后，可以执行：

```bash
dotnet ef migrations add AddCourseDescription \
  --project CsuPlusPlus \
  --output-dir Infra/Migrations/Postgres \
  -- --environment Production
```

将 `AddCourseDescription` 替换为实际变更名称。必须传入 `-- --environment Production`，让设计时工厂选择 Npgsql，而不是开发环境的 SQLite。

`Infra/AppDbContextFactory.cs` 专门为 EF 工具创建 DbContext，读取数据库配置，但不执行应用启动流程。因此，生成迁移不要求提供 JWT 密钥，也不会触发启动时自动迁移；通常不需要连接 PostgreSQL。

生成后检查并一同提交以下文件：

- 新迁移的 `.cs` 文件：`Up` 执行变更，`Down` 撤销变更。
- 对应的 `.Designer.cs` 文件。
- 更新后的 `AppDbContextModelSnapshot.cs`。

`--output-dir` 只是文件存放位置，不会自动隔离不同数据库 provider 的迁移。当前流程只生成 PostgreSQL 迁移。

### 3. 检查迁移

检查当前模型是否还有未包含在迁移中的变更：

```bash
dotnet ef migrations has-pending-model-changes \
  --project CsuPlusPlus \
  -- --environment Production
```

导出迁移 SQL 供检查；下面会将初始迁移到最新迁移的 SQL 写入仓库根目录的 `migration.sql`：

```bash
dotnet ef migrations script \
  --project CsuPlusPlus \
  --output migration.sql \
  -- --environment Production
```

SQL 导出不等于执行迁移。如果最近一条迁移尚未应用且生成有误，可以先移除再重新生成：

```bash
dotnet ef migrations remove \
  --project CsuPlusPlus \
  -- --environment Production
```

不要删除或随意修改已经应用到生产数据库的历史迁移，应新增迁移表达后续变更。

## 发布和服务器运行

例如发布给 Linux x64 服务器，生成自包含程序：

```bash
dotnet publish CsuPlusPlus/CsuPlusPlus.csproj \
  -c Release \
  -r linux-x64 \
  --self-contained true \
  -o publish
```

服务器为 ARM64 时，将运行时标识改为 `linux-arm64`。

上传整个 `publish` 目录，在服务器上进入该目录并设置运行配置：

```bash
export ASPNETCORE_ENVIRONMENT=Production
export ConnectionStrings__DefaultConnection='Host=localhost;Port=5432;Database=csuplusplus;Username=appuser;Password=替换为实际密码'
export Jwt__Secret='替换为至少32字节的随机密钥'
export ASPNETCORE_URLS='http://127.0.0.1:5253'

./CsuPlusPlus
```

示例 HTTP 地址用于同机反向代理转发，HTTPS 在代理侧配置。生产部署时应正确配置转发头和 HTTPS 重定向行为。

JWT 的 Issuer、Audience 和有效期已有公共配置；可以通过 `Jwt__Issuer`、`Jwt__Audience`、`Jwt__AccessTokenMinutes` 覆盖。开发配置中的密钥仅用于本地开发，生产需提供实际随机密钥。

服务器只需要发布产物和数据库连接，不需要源码、.NET SDK 或 `dotnet-ef`。自包含发布也不需要额外安装 .NET 运行时。

迁移类会编译进发布程序集。程序在接受请求前调用 `MigrateAsync()`，依据 PostgreSQL 中的 `__EFMigrationsHistory` 只执行尚未应用的迁移；失败时启动终止。数据库连接账号需要有相应的建表、修改结构权限；若还需要由应用创建数据库，则需要额外的创建数据库权限。

后续数据库结构发布流程：

```text
修改实体/映射 → 生成并检查迁移 → 重新发布 → 上传发布目录 → 重启应用自动迁移
```

仅修改实体、重新发布而不生成迁移，不会让生产数据库结构自动变化。

## GitHub Actions 自动 Docker 部署

`.github/workflows/deploy.yml` 仅在推送到 `main` 时触发：构建镜像 → 推送 GitHub Container Registry（GHCR）→ SSH 通知服务器拉取镜像 → 更新 `csuplusplus` 容器。SSH 只上传部署脚本，Docker 会复用服务器已有的镜像层。部署串行执行，不中断正在进行的部署；GitHub 同一并发组只保留一个等待任务，密集推送时可能跳过中间版本。

### 1. 初始化服务器

目标为 Linux 服务器，已安装并启动 Docker，同时需要 Bash 和 `timeout`（常见 Linux 发行版自带）。SSH 部署用户需要能直接执行 `docker`，并有 `/opt/csuplusplus` 目录的写权限。

在服务器上执行（将 `deployuser` 换成实际 SSH 用户）：

```bash
sudo mkdir -p /opt/csuplusplus
sudo chown deployuser:deployuser /opt/csuplusplus
```

创建 `/opt/csuplusplus/app.env`，填写真实配置，不要提交到 Git：

```dotenv
ConnectionStrings__DefaultConnection=Host=host.docker.internal;Port=5432;Database=csuplusplus;Username=appuser;Password=替换为实际密码
Jwt__Secret=替换为至少32字节的随机密钥
```

这是 Docker 的 env 文件，值不加 shell 引号。创建后执行 `chmod 600 /opt/csuplusplus/app.env`。

示例数据库位于 Docker 宿主机；数据库在其他服务器时替换 `Host`。容器中的 `localhost` 指向容器自身。宿主机 PostgreSQL 需要监听容器可访问的地址，并通过 `pg_hba.conf` 允许该容器网络的连接。此流程不创建 PostgreSQL，应用启动时会自动执行迁移。

### 2. 填写 GitHub Secrets

在仓库 **Settings → Secrets and variables → Actions → Secrets** 中添加：

| Secret               | 内容                                                                                            |
|----------------------|-------------------------------------------------------------------------------------------------|
| `DEPLOY_HOST`        | 服务器 IP 或域名                                                                                |
| `DEPLOY_USER`        | SSH 用户名                                                                                      |
| `DEPLOY_PORT`        | SSH 端口，未设置时为 `22`                                                                       |
| `DEPLOY_SSH_KEY`     | 完整 SSH 私钥，配套公钥添加到服务器用户的 `~/.ssh/authorized_keys`；使用无口令的部署专用密钥    |
| `DEPLOY_KNOWN_HOSTS` | 已核实指纹的服务器 SSH 公钥记录，格式与 `~/.ssh/known_hosts` 相同；非标准端口对应 `[主机]:端口` |

仓库不包含 SSH 信息或私钥，填写 Secrets 后生效。

默认镜像架构为 `linux/amd64`；服务器为 ARM64 时，在同一页面的
**Variables** 中添加 `DOCKER_PLATFORM=linux/arm64`。

### 3. 配置 GHCR 拉取权限

镜像地址自动使用小写仓库名，本仓库对应：

```text
ghcr.io/mitochondriacn/csuplusplus:<commit-sha>
```

Actions 使用自动提供的 `GITHUB_TOKEN` 和工作流声明的 `packages: write` 权限推送，无需额外填写推送凭证。若组织策略限制发布包，需允许该仓库工作流写入 Packages；如果同名包已存在，则在包的
**Manage Actions access** 中给本仓库写权限。

GHCR 新包默认是私有的。在首次推送前，使用有该包访问权限的 GitHub 账号创建带 `read:packages` 权限的 Personal access token（classic），并在服务器上以
**`DEPLOY_USER` 对应用户**执行一次登录：

```bash
read -rsp 'GHCR token: ' GHCR_TOKEN; printf '\n'
printf '%s' "$GHCR_TOKEN" | docker login ghcr.io --username 你的GitHub用户名 --password-stdin
unset GHCR_TOKEN
```

凭证保存在该用户的 Docker 配置中，后续部署会复用。不要用 `sudo docker login`，否则凭证会保存给 root。令牌到期后需重新登录；如果组织要求 SSO，还需授权该令牌。服务器必须能访问 `ghcr.io` 及其镜像下载服务。

也可以在首次镜像推送后，将 GitHub Packages 中该包的可见性设为 Public，公开镜像允许匿名拉取。若首次部署因未登录而失败，配置权限后，在 Actions 中重新运行失败任务即可。

### 4. 推送 main

提交配置并推送到 `main` 后，在 GitHub **Actions → Deploy
main** 查看进度。现有 Dockerfile 的 Release 构建在镜像构建过程中执行。

应用以 Production 环境运行，映射服务器 `8080` 端口到容器 `8080` 端口。可通过 `http://服务器地址:8080` 访问（需允许该端口），生产 HTTPS 可接现有反向代理，并按上文配置转发头。若希望只允许同机代理访问，将 `scripts/deploy.sh` 的 `--publish 8080:8080` 改为 `--publish 127.0.0.1:8080:8080`。

部署会短暂停止旧容器，等待新容器 `/health/check` 返回预期的 `200` 后才判定成功。若超时或启动失败，输出容器日志并尝试恢复旧容器。若调整该接口路由或 HTTPS 重定向配置，需要同步调整探测逻辑。数据库迁移不会自动回滚，迁移需保持兼容并做好备份。

排查服务器运行情况：

```bash
docker ps --filter name=csuplusplus
docker logs --tail 100 csuplusplus
```

每次镜像以 commit SHA 标记并推送到 GHCR；部署按构建输出的 digest（`sha256:…`）拉取，确保运行的就是本次构建。拉取失败时旧容器继续运行。首次拉取需要下载全部镜像层，后续仅下载缺少的层；实际速度取决于服务器到 GHCR 的网络。上传的部署脚本执行后自动清理，旧镜像可按磁盘使用情况手动清理。

## 当前接口

| 方法 | 路径             | 说明                                                    |
|------|------------------|---------------------------------------------------------|
| POST | `/auth/register` | 邮箱、密码、昵称、学院 ID 注册，成功返回 JWT 和用户 DTO |
| POST | `/auth/login`    | 邮箱密码登录                                            |
| GET  | `/profile/me`    | 查询当前用户资料，需要 JWT                              |

访问受保护接口时添加请求头：

```http
Authorization: Bearer <token>
```
