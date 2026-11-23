# 多平台应用图标导出说明

本文档记录基于 [logo.svg](./logo.svg) 为 `client/csuplusplus-kmp` 各平台生成并替换应用图标的规范、产物路径与复现方法。

---

## 1. 源文件与色板规范

- **矢量源文件**：`design/logo.svg`（`viewBox="0 0 1024 1024"`），包含两个多边形：
  - `upper-mass`：`160,160 864,160 864,228 160,716`
  - `lower-mass`：`864,272 864,864 456,864`
  - 图形原始包围盒为 `[160, 864] × [160, 864]`（`704 × 704`）。
- **色板取色原则**（从客户端当前主题色板动态读取，不写死具体色值）：
  - **浅色前景色**：当前色板 `primary`（本次导出时客户端使用默认 Material 3 亮色方案）
  - **浅色背景色**：当前色板 `background` / `surface`
  - **深色前景色 / 背景色**（用于 iOS Dark 变体）：对应当前主题深色色板的 `primary` 与 `background`
  - **着色变体**（用于 iOS Tinted 变体）：纯白前景 + 纯黑背景（供系统统一着色）

> 后续修改主题色板时，只需按当前色板的 `primary` 与 `background` 重新导出对应资源即可。

---

## 2. 各平台产物与配置

### 2.1 Android（`client/csuplusplus-kmp/androidApp/res/`）

- **自适应图标（API 26+）**
  - `drawable-v24/ic_launcher_foreground.xml`：`108dp × 108dp` 矢量图，将 `704 × 704` 图形等比缩放到安全区 `[30, 78] × [30, 78]`（`48dp × 48dp`，对角线半径 `33.94dp < 36dp`），填充色取 `primary`。
  - `drawable/ic_launcher_background.xml`：`108dp × 108dp` 纯色 `background`。
  - `mipmap-anydpi-v26/ic_launcher.xml`、`mipmap-anydpi-v26/ic_launcher_round.xml`：引用上述前/背景，并补充 `<monochrome android:drawable="@drawable/ic_launcher_foreground" />` 以支持 Android 13+ 主题图标。
- **传统位图（API < 26 兜底）**
  - 在 `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}` 生成 `ic_launcher.png`（圆角矩形底）与 `ic_launcher_round.png`（圆形底），尺寸分别为 `48`、`72`、`96`、`144`、`192` px（RGBA）。

### 2.2 Desktop（`client/csuplusplus-kmp/desktopApp/` & `shared/`）

- **macOS `.app` / `.dmg` / Dock 图标**：
  - `desktopApp/resources/AppIcon.icns`：基于 `1024×1024` 主图（`~9.77%` 外边距，圆角 `22.5%`，浅色 `background` 底 + `primary` 前景）生成 10 个 `.iconset` 尺寸，再通过 `iconutil` 打包，打包时传给 `jpackage --icon`。
- **Compose 窗口图标（Windows / Linux 任务栏与标题栏）**：
  - `shared/composeResources/drawable/app_icon.png`：`512×512` RGBA PNG。
  - `desktopApp/module.yaml`：在 `dependencies` 中添加 `$compose.components.resources`。
  - `desktopApp/src/main.kt`：在 `Window(...)` 中传入 `icon = painterResource(Res.drawable.app_icon)`。

### 2.3 iOS（`client/csuplusplus-kmp/iosApp/src/Assets.xcassets/AppIcon.appiconset/`）

采用 Xcode 单图模式，所有变体均为 `1024×1024`、`8-bit/color RGB` **无透明通道** PNG（图形居中占 `680×680`）：

- `app-icon-1024.png`：默认浅色（浅色 `background` 底 + `primary` 前景）
- `app-icon-1024-dark.png`：深色外观（深色 `background` 底 + `primary` 前景）
- `app-icon-1024-tinted.png`：着色外观（`#000000` 底 + `#FFFFFF` 前景）
- `Contents.json`：为 `universal`（默认）、`dark`、`tinted` 三个条目分别绑定上述文件名。

### 2.4 Web（`client/csuplusplus-kmp/webApp/resources/`）

- `favicon.ico`：多尺寸 ICO（内含 `16×16`、`32×32`、`48×48`）。
- `favicon.png`：`192×192` RGBA PNG。
- `index.html`：在 `<head>` 中引用：
  ```html
  <link rel="icon" href="favicon.ico" sizes="any">
  <link rel="icon" type="image/png" href="favicon.png">
  ```

---

## 3. 复现步骤

环境依赖：`python3`（含 `Pillow` 库）与 macOS 系统自带的 `iconutil`。

1. **解析 SVG 多边形并超采样渲染**
   - 用 `xml.etree.ElementTree` 读取 `design/logo.svg` 中所有 `<polygon>` 的 `points`。
   - 按目标平台尺寸以 2×～4× 分辨率绘制圆角矩形/圆形/纯色背景与缩放后的前景多边形，再使用 `Image.Resampling.LANCZOS` 降采样至目标像素尺寸。
   - iOS 图标需使用 `RGB` 模式（不含 Alpha 通道）。
2. **生成 macOS `.icns`**
   - 将以下 10 个 PNG 放入 `AppIcon.iconset/` 目录：
     - `icon_16x16.png`（16）、`icon_16x16@2x.png`（32）
     - `icon_32x32.png`（32）、`icon_32x32@2x.png`（64）
     - `icon_128x128.png`（128）、`icon_128x128@2x.png`（256）
     - `icon_256x256.png`（256）、`icon_256x256@2x.png`（512）
     - `icon_512x512.png`（512）、`icon_512x512@2x.png`（1024）
   - 执行转换命令：
     ```bash
     iconutil -c icns AppIcon.iconset -o client/csuplusplus-kmp/desktopApp/resources/AppIcon.icns
     ```
3. **生成 Web `favicon.ico`**
   - 使用 Pillow 导出多尺寸 ICO：
     ```python
     img.save("client/csuplusplus-kmp/webApp/resources/favicon.ico", format="ICO", sizes=[(16, 16), (32, 32), (48, 48)])
     ```
