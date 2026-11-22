# 90 · 修订日志与维护清单

## 每次问答后的四步（固定流程）

0. **必读**：每轮回答用户之前，先读 `.agent\.skill\agent_read.md`（用户原始指令，永久不变，不得改写）。
   这是用户明确要求的强制动作，不是可选步骤。
   - 该文件里的触发词 **`ask`**：对话出现 `ask` 时，本轮必须归档（问题+解答+产物）。
1. **追加结论** → `05-findings.md` 文末，按模板写 `F-0xx`，编号连续。
2. **沉淀手法** → 若本次有"这样搜很快"的经验，追加一行到 `04-retrieval.md` §9。
3. **修正结构** → 若发现 `01`–`04` 的事实错误（路径、行号、类名），就地改正。
4. **记日志** → 在下表追加一行，写清触发问题、改了哪些文件、一句话摘要。
5. **写代码时** → 注释必须符合 `11-doxygen-rules.md`（D1–D22）；提交前跑该文件 §7 自检。

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
| 2 | 2026-11-22 | 用户要求 `agent_read.md` 成为**每轮必读**（文件内新增触发词 `ask`）；并提问"方块类物品如何注册（可放置+可手持使用）" | 新建 `10-code-recipes.md`（R-001 完整代码+资源 JSON）；`00-index.md` 新增 §0 每轮必读/`ask` 归档规则与流程第 0 步、文件清单加 10；`90` 维护清单加第 0 步；`01-sources.md` 补充"jar 已被 Fabric 打补丁"；`03-client-structure.md` 新增 §6.1 资源文件布局；`05-findings.md` 新增 F-011；`04-retrieval.md` 新增 3 条手法 + 1 行任务入口 + 修正 Fabric API 源码位置 | 明确"`Items.java` 那套是 private 助手，模组复刻 setId→registerBlocks→Registry.register 三步"；记录 `useOn`(放置) 与 `use`(手持) 的分工；实测出 26.3 三处易踩坑：物品模型在 `assets/<ns>/items/`、战利品池用单数 `condition`、`common` jar 是 Fabric 补丁版 |
| 3 | 2026-11-22 | 用户要求"确立 Doxygen 文档注释编写规则" | 新建 `11-doxygen-rules.md`（D1–D22、4 类模板、自检命令、最小 Doxyfile）；`00-index.md` 文件清单加 11、流程第 6 步引用；`90` 清单加第 5 步与待办；`05-findings.md` 新增 F-012 | 定下 5 条关键决策：①首行即 brief（`JAVADOC_AUTOBRIEF=YES`，禁 `@brief`）②交叉引用用 `@ref`/`@see`、禁 `{@link}`/`{@code}` ③注释写约束不写复述（侧别/线程/tick 阶段/幂等）④注册表字段必写表名+完整 ID+`@see` ⑤禁 `@author`/`@date`/`@version`，过期注释即缺陷 |
| 4 | 2026-11-22 | 要求在既有 `ModItems`/`ModBlocks` 注册一个"暂无特性"的方块类物品，ID `annoying-speaker` | 改写 `ModBlocks.java`、`ModItems.java`（注册 + Doxygen 注释）；`AnnoyingSpeaker.java` 加 `ModBlocks.initializer()` 与 import；`01-sources.md` 新增 §6 本地构建环境；`11` 新增 §6.0 文件头格式；`05` 新增 F-013；`10` 新增 §0.1 最小版说明 | 用 F-011/R-001 的结论落地：`Block::new` + `BlockItem::new` 无自定义子类；ID `annoying-speaker:annoying-speaker` 合法（`validPathChar` 含 `-`）。**编译未验证**：本机 JDK 21，Loom 1.19 要求 JDK 25，`gradlew compileJava` 在配置阶段即失败 |
| 5 | 2026-11-22 | 把 `models_of_speaker` 的资源落进 resources，让方块/物品在游戏里显示 | 新建 `blockstates`、`items`、`models/block`、`lang/{en_us,zh_cn}`、`loot_table/blocks` 共 6 个 JSON；复制 3 张贴图到 `textures/block/`；`ModBlocks` 加 `noOcclusion()`；`05` 新增 F-014/F-015；`03` §6.1 补三个易误判点；`90` 待办更新 | 资源文件名必须等于注册表路径 `annoying-speaker`（含连字符）；`mainbody.png` 为 64×64（等比放大，勿加 `texture_width`）；`display.on_shelf` 在 26.3 合法；`blockstates` 空键 `""` 会让 PowerShell 的 `ConvertFrom-Json` 误报，改用 Python 校验 |
| 6 | 2026-11-22 | **ask**：如何加精细碰撞箱与方块朝向（要求同时改到文件） | 新建 `src/main/java/.../SpeakerBlock.java`（`HorizontalDirectionalBlock` + `Shapes.rotateHorizontal` + 5 个 box 合并）；`ModBlocks` 工厂改 `SpeakerBlock::new` 并更新注释/`@see`；`blockstates/annoying-speaker.json` 改为 4 个 `facing` variant（`y` 0/90/180/270）；`10-code-recipes.md` 新增 **R-002**；`05` 新增 **F-016**；`04` 新增 1 条手法 | 朝向靠继承 `HorizontalDirectionalBlock` 白拿 `rotate`/`mirror`；四个覆写缺一不可（漏 `createBlockStateDefinition` 直接崩）；形状只写北向再 `Shapes.rotateHorizontal`，与 blockstate 的 `y` 同源，手算必穿模；只覆写 `getShape` 即同时管碰撞箱与选中框 |

## 已知待办

- [ ] 每轮问答后保持 `05-findings.md` 编号连续（当前最大 `F-016`）。
- [ ] **每轮回答前先读 `agent_read.md`**（用户强制要求；对应 §0 与上面清单第 0 步）。
- [ ] 剩余 4 个 `.java`（`AnnoyingSpeaker`、两个 Mixin、`AnnoyingSpeakerClient`）尚未按 `11` 补注释；待用户确认后统一补齐。
- [ ] 装机 JDK 25 后跑一次 `gradlew compileJava`，补验 F-013/F-014（当前所有 Java 改动都只做了静态核对）。
- [ ] 资源已落位但**未做运行期验证**；进游戏后逐项确认：模型是否现形、UV 是否错位（整体偏暗可加 `"ambientocclusion": false`）、挖掉是否掉落。
- [ ] 朝向与碰撞箱也未做运行期验证：放置后检查四面朝向是否与模型一致、选中框是否贴合、结构旋转（`/place` 或 jigsaw）后朝向是否跟随。
- [ ] 若要让碰撞箱/选中框贴合模型，后续给方块加 `getShape`（`Block.box(1,0,4,13,15,14)`，对应模型包围盒）。
- [ ] 若后续启用 Fabric datagen / 事件 API，补一节 `06-fabric-api.md`（Fabric 源码不在两个 jar 内，需另找 jar）。
- [ ] 若 jar 版本升级（如 `26.4`），整份 `01`–`04` 需按新树复核，不能沿用行号。
