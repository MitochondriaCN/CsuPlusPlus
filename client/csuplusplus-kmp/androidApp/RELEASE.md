# Android 构建与发布

均在 `client/csuplusplus-kmp` 根目录执行。

## 发版前准备

1. 在仓库根目录 `version.properties` 中更新 `buildCode` 与 `buildName`，并运行 `./scripts/sync-version.sh`（或直接运行 `./scripts/sync-version.sh <buildCode> <buildName>`）同步更新 `androidApp/module.yaml` 与各平台配置。
2. 首次构建需确保已生成签名密钥（`keystore.properties` 与 `*.keystore` 勿提交到 Git）：
   ```bash
   cd androidApp && ../kotlin tool generate-keystore --properties-file keystore.properties
   ```

## 打包

- **AAB（上架 Google Play，自动执行 R8 混淆与签名）**：
  ```bash
  ./kotlin package -m androidApp
  ```
  产物：`build/tasks/_androidApp_bundleAndroid/gradle-project-release.aab`

- **APK（直接安装分发）**：
  ```bash
  ./kotlin build -p android -m androidApp -v release
  ```
  或从 `.aab` 导出已签名的通用 APK：
  ```bash
  bundletool build-apks \
    --bundle=build/tasks/_androidApp_bundleAndroid/gradle-project-release.aab \
    --output=build/dist/android/app.apks \
    --mode=universal \
    --ks=androidApp/release.keystore \
    --ks-key-alias=alias
  ```
  解压 `app.apks` 中的 `universal.apk` 即可分发。
