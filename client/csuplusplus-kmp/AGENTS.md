# AGENTS.md

## 测试规范

- 本项目（`csuplusplus-kmp`）**不需要编写或维护单元测试/集成测试**。
- 不要创建 `test@*`（如 `shared/test@jvm`）测试目录或新增测试依赖。
- 修改代码后，使用 `./kotlin build -m shared`（或 `./kotlin build`）验证各目标平台编译通过即可。

## UI 与代码规范

- **顶栏与多行吸顶头部**：`TopAppBar` 的 `actions` 仅放图标按钮，带文本的筛选 badge 行放在标题下方；若多行头部需要随列表滚动统一变色，必须用单个 `Surface` 包裹整块头部并将 `TopAppBar` 的 `containerColor` / `scrolledContainerColor` 设为 `Color.Transparent`，由 `listState.canScrollBackward` 驱动背景色，禁止用多个独立的 `animateColorAsState` 或 `overlappedFraction` 拼凑同步。
- **保持简洁**：新增小功能时优先复用已有弹窗、菜单项与统一状态/异步提交逻辑，避免为单个功能引入大量重复样板代码。
