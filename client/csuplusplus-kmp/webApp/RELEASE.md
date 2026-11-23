# Web（Kotlin/Wasm）构建与发布

在 `client/csuplusplus-kmp` 根目录执行。

## 构建 Release 产物

```bash
./kotlin build -p wasmJs -m webApp -v release
```

产物位于 `build/tasks/_webApp_buildWasmJsAppWasmJsRelease/`（包含 `.wasm`、`.mjs`、Skiko 运行时及 `index.html`、`styles.css`）。

## 部署

将该目录整体部署到任意静态文件托管服务（Nginx、Caddy、Cloudflare Pages、GitHub Pages 等）：

- 确保服务器正确返回 `.wasm` 的 `application/wasm` MIME 类型。
- 确保后端已配置好对应前端域名的 CORS 允许策略。
