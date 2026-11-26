# CsuPlusPlus KMP 客户端

CSU++ 的 Kotlin Multiplatform 跨平台客户端，目标平台包括 Android、Desktop（JVM）、iOS 与 Web（Kotlin/Wasm），基于 Compose Multiplatform 与 [Kotlin Toolchain](https://kotlin-toolchain.org/dev/) 构建。

## 目录结构

- [/androidApp](./androidApp)：Android 应用入口。
- [/desktopApp](./desktopApp)：Desktop（JVM）应用入口。
- [/iosApp](./iosApp)：iOS 应用入口（发布说明见 [iosApp/RELEASE.md](./iosApp/RELEASE.md)）。
- [/webApp](./webApp)：Web 应用入口（编译为 Kotlin/Wasm）。
- [/shared](./shared)：跨平台共享模块，包含 Compose UI、业务逻辑与平台适配：
    - `src`：全平台公共代码（`core`、`data`、`di`、`presentation`）
    - `src@nonWeb`：除 Web 外的原生/JVM 平台共享代码
    - `src@android` / `src@jvm` / `src@ios` / `src@wasmJs`：各平台专用实现
    - `composeResources`：共享资源文件

## 构建与运行

项目根目录的 `kotlin`（macOS/Linux）和 `kotlin.bat`（Windows）为自举包装脚本，首次运行会自动下载固定版本的 Kotlin Toolchain，无需手动安装。

### 构建

- 构建整个项目：`./kotlin build`
- 仅构建共享模块：`./kotlin build -m shared`

### 运行

可通过 IDE 工具栏的运行配置启动，或在命令行执行：

- Android：`./kotlin run -m androidApp`
- Desktop：`./kotlin run -m desktopApp`
- iOS：`./kotlin run -m iosApp`
- Web：`./kotlin run -m webApp`

## 测试说明

本项目不编写或维护单元测试/集成测试，改动后通过 `./kotlin build -m shared` 或 `./kotlin build` 验证各目标平台编译通过即可。
