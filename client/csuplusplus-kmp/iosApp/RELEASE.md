# iOS 构建与发布

需在 macOS + Xcode 环境下于 `client/csuplusplus-kmp` 根目录执行。

## 发版前准备

1. 运行 `./scripts/sync-version.sh <buildCode> <buildName>`（或修改 `version.properties` 后运行 `./scripts/sync-version.sh`）统一同步 `MARKETING_VERSION` 与 `CURRENT_PROJECT_VERSION`。
2. 在 Xcode 中打开 `iosApp/module.xcodeproj` 确认 **Signing & Capabilities** 已选定 `Team` 与正式唯一的 `Bundle Identifier`。

## 打包与发布

- **导出 `.ipa`**：
  ```bash
  ./kotlin package -m iosApp -f ipa
  # 产物：build/packages/ipa/iosApp-release.ipa
  ```
  如需指定 App Store / Ad Hoc 导出配置，追加 `--ipa-export-options-plist-path=iosApp/ExportOptions.plist`。

- **上传 TestFlight**：
  ```bash
  ./kotlin publish testflight -m iosApp
  ```
  （CI 环境可通过 `KOTLIN_TOOLCHAIN_ASC_KEY_PATH`、`KOTLIN_TOOLCHAIN_ASC_KEY_ID`、`KOTLIN_TOOLCHAIN_ASC_KEY_ISSUER_ID` 传入 App Store Connect API Key。）

  也可直接在 Xcode 中使用 `Product | Archive` → `Distribute App` 上传。
