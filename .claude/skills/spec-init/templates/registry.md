# NNN 取号登记表

> 多人协作下保证特性序号 `NNN` 唯一的**单一事实源**。规矩：**先登记后开工**——取号发生在共享的 main 上、且在开特性分支之前；否则各自分支里 `max+1` 互相看不见，合并才撞号。
> 完整流程见 [`workflow.md`](workflow.md)「取号约定」节。

## 怎么取号

1. 切到最新 main：`git checkout main && git pull`（先看到别人已登记的号）。
2. 在下表**追加一行**，`NNN = 当前最大号 + 1`，status 填 `reserved`：
   ```
   | 017 | inventory-alert | 甲 | 2026-06-10 | reserved | 库存预警 |
   ```
3. 只提交这一行并 push 进 main（仅这一行，可免 PR）：
   ```
   git add docs/engineering/registry.md
   git commit -m "chore(registry): 预留 NNN=017 inventory-alert"
   git push
   ```
   - 并发抢同号时，后 push 者会 non-fast-forward 被拒、或 PR 合并时本表冲突 → **当场暴露** → 改取下一个号重来。
4. 再开 `feat/017-inventory-alert` 分支，跑 `/spec-prd`，直接用 017。

## 状态约定

- `reserved` 已预留、开发中。
- `done` 特性已合并交付。
- `abandoned` 放弃；**号作废不回收**（回收会打断 design/plan/测试/历史的引用）。

## 登记表

| NNN | slug | owner | date | status | 说明 |
| --- | --- | --- | --- | --- | --- |
| 001 | example-slug | 示例 | 2026-01-01 | done | 示例行，新建项目时删除 |
