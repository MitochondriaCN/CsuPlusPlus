# Desktop 构建与发布

均在 `client/csuplusplus-kmp` 根目录执行。原生安装包需在对应目标操作系统上构建。发版前请先通过 `./scripts/sync-version.sh` 统一同步版本号。

## 1. 打包可执行 JAR

```bash
./kotlin package -m desktopApp -f executable-jar
```

产物位于 `build/tasks/_desktopApp_executableJarJvm/`。

## 2. 生成原生安装包（`jpackage`）

```bash
rm -rf build/jpackage-input && mkdir -p build/jpackage-input
cp build/tasks/_desktopApp_executableJarJvm/*.jar build/jpackage-input/
```

- **macOS（`.dmg`）**：
  ```bash
  jpackage \
    --type dmg \
    --name "CSU++" \
    --app-version "0.1.1" \
    --mac-package-identifier "ink.qingshuige.csuplusplus" \
    --mac-package-name "CSU++" \
    --icon desktopApp/resources/AppIcon.icns \
    --input build/jpackage-input \
    --main-jar desktopApp-jvm-executable.jar \
    --main-class org.springframework.boot.loader.launch.JarLauncher \
    --dest build/dist/macos
  ```
  > 公开分发需在 `jpackage` 追加 `--mac-sign --mac-signing-key-user-name "Developer ID Application: ..."`，并使用 `xcrun notarytool submit` 公证与 `xcrun stapler staple` 装订。

- **Windows / Linux**：将 `--type dmg` 替换为 `msi`（或 `exe`）/ `deb`，并改用对应平台图标（`.ico` / `.png`），移除 `--mac-*` 参数即可。
