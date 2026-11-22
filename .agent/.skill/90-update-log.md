# 90 · 修订日志与维护清单

## 每次问答后的四步（固定流程）

1. **追加结论** → `05-findings.md` 文末，按模板写 `F-0xx`，编号连续。
2. **沉淀手法** → 若本次有"这样搜很快"的经验，追加一行到 `04-retrieval.md` §9。
3. **修正结构** → 若发现 `01`–`04` 的事实错误（路径、行号、类名），就地改正。
4. **记日志** → 在下表追加一行，写清触发问题、改了哪些文件、一句话摘要。

约束：`agent_read.md` 永不修改；`.agent\.cache\**` 永不手改；源码 jar 永不修改（只读）。

## 行号会漂移

`02`–`05` 里引用的行号对应本次解包的这版源码。只要 `.gradle\loom-cache` 里的 jar 没变，
行号就有效（缓存用 SHA256 校验）。若 jar 被 loom 重新生成导致内容变化：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .agent\.skill\scripts\mc-src.ps1 ensure   # 会显示 extracted/cached
powershell -NoProfile -ExecutionPolicy Bypass -File .agent\.skill\scripts\mc-src.ps1 anchors   # 重新核对锚点行数
```

`anchors` 输出里出现 `<-- 已移动/改名` 的条目，说明该类在本版被移动/改名，需回到 `04`/`05` 修正。

## 日志

| 轮 | 日期 | 触发 | 改动 | 摘要 |
|---:|---|---|---|---|
| 1 | 2026-11-22 | 初版：阅读两个源码包的基本结构，编写 skill | 新建 `00-index.md`、`01-sources.md`、`02-server-structure.md`、`03-client-structure.md`、`04-retrieval.md`、`05-findings.md`、`90-update-log.md`、`scripts/mc-src.ps1`；生成 `.agent\.cache\src\{common,client}` 缓存 | 确认 common=双端/逻辑服务端（5037 `.java`）、clientOnly=纯客户端（2264 `.java`）；建立包地图、启动链路、双端对照、任务→路径表；登记 10 条初始结论（含 `ResourceLocation→Identifier`、`GuiGraphics→GuiGraphicsExtractor` 两处重大改名，及 `references` ID 常量表） |

## 已知待办

- [ ] 每轮问答后保持 `05-findings.md` 编号连续（当前最大 `F-010`）。
- [ ] 若后续启用 Fabric datagen / 事件 API，补一节 `06-fabric-api.md`（Fabric 源码不在两个 jar 内，需另找 jar）。
- [ ] 若 jar 版本升级（如 `26.4`），整份 `01`–`04` 需按新树复核，不能沿用行号。
