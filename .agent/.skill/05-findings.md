# 05 · 问答结论库

> **追加规则**：每轮提问结束后在文末追加一条 `F-xxx`，编号连续、不删不改。
> 若新证据推翻了旧结论，在旧条目内追加 `> 修订(日期)：…`，并在 `90-update-log.md` 记为「更正」。
> 每条必须带**可验证证据**（`路径:行号` 或可复制的检索命令），否则不算结论。

## 条目模板

```markdown
### F-0xx · 一句话标题
- **日期**：YYYY-MM-DD
- **问**：用户原问题的概括（保留他的用词，方便以后关键词命中）
- **答**：直接可用的结论（先给做法，再给理由）
- **证据**：`路径:行号`（多条分行）
- **检索**：一条能复现的命令
- **落地**：写在 `src/main` 还是 `src/client`；涉及哪个 mixin json / 注册表
```

---

### F-001 · 两个 jar 是"服务端 + 客户端"吗？该看哪个？
- **日期**：2026-11-22
- **问**：`.gradle\loom-cache\minecraftMaven\net\minecraft` 下的 common 与 clientOnly 分别是什么。
- **答**：`common` = 双端共享逻辑 + 逻辑服务端（`net.minecraft.world/server/network/...`），
  `clientOnly` = 纯客户端（`net.minecraft.client`、`com.mojang.{blaze3d,renderpearl,realmsclient}`）。
  专用服务端只加载 `common`；客户端加载 `common` + `clientOnly`。所以 `common` **不等于**"服务端"。
  规模：common 5037 个 `.java`，clientOnly 2264 个。
- **证据**：
  - `.gradle\loom-cache\launch.cfg`：`fabric.gameJarPath`（common）与 `fabric.gameJarPath.client`（clientOnly）并存
  - 两棵树无同名同名包：`common` 无 `net/minecraft/client`，`client` 无 `net/minecraft/world`
- **检索**：`mc-src.ps1 stats`
- **落地**：`src/main` 只能用 common；`src/client` 两棵都能用。

### F-002 · 当前源码对应哪个版本/映射？
- **日期**：2026-11-22
- **问**：确认 Minecraft 版本、Fabric 版本、Java 版本、命名映射。
- **答**：MC `26.3`，Fabric Loader `0.20.1`，Loom `1.19-SNAPSHOT`，Fabric API `0.166.2+26.3`，Java 25；
  使用 **Mojang 官方命名**（`ServerLevel`、`net.minecraft.world.item.Item`），全树 `@NullMarked`（jspecify）。
- **证据**：`gradle.properties`；`build.gradle`（`options.release = 25`）；`net/minecraft/package-info.java`
- **检索**：`mc-src.ps1 show net/minecraft/sounds/SoundEvent.java`
- **落地**：写代码时直接用官方名；空安全按 `@Nullable` 契约处理。

### F-003 · `ResourceLocation` 找不到了
- **日期**：2026-11-22
- **问**：旧教程里的 `ResourceLocation` 在 26.3 叫什么。
- **答**：改名为 **`net.minecraft.resources.Identifier`**（277 行）。
  构造：`Identifier.fromNamespaceAndPath(ns, path)`、`Identifier.parse("ns:path")`；
  序列化：`Identifier.CODEC` / `Identifier.STREAM_CODEC`；常量 `DEFAULT_NAMESPACE = "minecraft"`。
  相关：`IdentifierException`（`net/minecraft/IdentifierException.java`）、`IdentifierPattern`（`util/`）。
- **证据**：`net/minecraft/resources/Identifier.java:20`、`:22-27`、`:38`
- **检索**：`mc-src.ps1 grep "class Identifier" -Side common`
- **落地**：任何注册/标签/资源路径都用 `Identifier`；Fabric API 侧签名也接受它。

### F-004 · 画 GUI 的 API 变了（GuiGraphics 不见 / render 不生效）
- **日期**：2026-11-22
- **问**：为什么找不到 `GuiGraphics`，`Screen#render` 覆写没反应。
- **答**：26.3 改成**两段式**：先 `extract*` 收集绘制状态，再由 `GuiRenderer` 提交。
  - 绘制 API：`client/gui/GuiGraphicsExtractor.java`（1488 行）
  - 界面入口：`Screen#extractRenderState(GuiGraphicsExtractor, int, int, float)`（`:121`）
  - 背景：`extractBackground`（`:425`）/ `extractMenuBackground`
  - 控件：`AbstractWidget#extractRenderState`（`:56`）→ 抽象 `extractWidgetRenderState`（`:90`）→ `extractContents`
  - 提交器：`client/gui/render/GuiRenderer.java`（429 行）；GUI 物品图集 `GuiItemAtlas`（145）
- **证据**：上述行号；`Hud.java` 全篇 `extract*` 命名
- **检索**：`mc-src.ps1 grep "extractRenderState" -Side client -Max 10`
- **落地**：`src/client`；自定义 Screen 覆写 `extractRenderState`，不要再覆写 `render`。

### F-005 · 怎么让某个东西发出声音（服务端 / 客户端分别）
- **日期**：2026-11-22
- **问**：物品/方块触发声音的正确写法；`playSound` 有哪些重载。
- **答**：逻辑侧统一走 `Level` 的重载族，客户端另有"仅本地播放"：
  - `Level#playSound(...)`：`world/level/Level.java:383 / 425 / 429 / 442 / 455 / 461`
  - `Level#playLocalSound(...)`：`:467 / 473`
  - 服务端实体位置播放：`ServerLevel#playSeededSound`（`:1040 / 1065`）
  - 客户端本地：`ClientLevel#playLocalSound`（`:715 / 725`）、`playSeededSound`（`:685 / 701`）
  - 声音事件总表：`sounds/SoundEvents.java`（1842 条注册；`NOTE_BLOCK_HARP` 在 `:1160`）
  - 常量可用 `Holder.Reference<SoundEvent>`（`AMBIENT_CAVE` `:31`）或 `SoundEvent`（`ARROW_HIT` `:109`）
- **证据**：上列行号
- **检索**：`mc-src.ps1 show net/minecraft/world/level/Level.java -From 375 -To 480`
- **落地**：`src/main`（逻辑侧）用 `level.playSound(null, pos, SoundEvents.X, SoundSource.PLAYERS, vol, pitch)`；
  只有"只给某个玩家听"或"客户端预表现"才需要 `playLocalSound` 与 `src/client`。

### F-006 · 26.3 有哪些新子系统值得知道
- **日期**：2026-11-22
- **问**：这个版本相对旧版多了什么。
- **答**：环境属性 `world/attribute/`、世界时钟 `world/clock/`、时间线 `world/timeline/`、
  航点 `world/waypoints/`+`server/waypoints/`、服务端对话框 `server/dialog/`（35 个类）、
  权限系统 `server/permissions/`、服务端 JSON-RPC `server/jsonrpc/`（65 个类）、
  追猎相机 `server/chase/`、调试几何 `gizmos/`、新 GPU 抽象 `com/mojang/renderpearl`（146 个类）。
- **证据**：各目录文件数（`mc-src.ps1 tree`）；`server/jsonrpc/JsonRpc.java`、`server/permissions/Permission.java`
- **检索**：`mc-src.ps1 tree net/minecraft/server`
- **落地**：写"环境/时间/权限/对话框"类功能时优先用这些新 API，别照抄旧教程。

### F-007 · 数据生成（datagen）从哪进
- **日期**：2026-11-22
- **问**：改配方/战利品/标签/模型，是改 Java 还是 JSON。
- **答**：原版用 `net.minecraft.data`（174 个类）：入口 `data/Main.java` + `DataGenerator`，
  各类 `*Provider`（`RecipeProvider` 1232 行、`LootTableProvider`、`*TagsProvider`、`BlockModelProvider`…）。
  客户端资源另有 `client/data/`（`AtlasProvider`）。
  对模组：数据驱动内容放 `src/main/resources/data/<modid>/...`，或用 Fabric 的 datagen（API 在 Fabric 侧）。
- **证据**：`net/minecraft/data/DataGenerator.java:1-121`、`data/recipes/RecipeProvider.java`
- **检索**：`mc-src.ps1 find "Provider" -Side common -Max 30`
- **落地**：`src/main`；资源路径 `assets/<modid>` / `data/<modid>`。

### F-008 · 源码为什么没有注释 / 参数全是 final
- **日期**：2026-11-22
- **问**：读源码时的反常写法是不是有特殊含义。
- **答**：这是反编译产物（Vineflower 风格）的特征：`final` 形参、`this.` 显式限定、`lambda$…` 合成名、
  无 javadoc。**不是** API 约定。唯一有语义的是 `@NullMarked` / `@Nullable`。
- **证据**：`net/minecraft/resources/Identifier.java` 头部；全树 `package-info.java` 只有 `@NullMarked`
- **检索**：`mc-src.ps1 show net/minecraft/world/item/Item.java -From 1 -To 30`
- **落地**：无。

### F-009 · 本模组代码该放 `src/main` 还是 `src/client`
- **日期**：2026-11-22
- **问**：`splitEnvironmentSourceSets()` 下新增文件放哪。
- **答**：`src/main` ↔ common（双端/逻辑）：注册、物品方块、世界逻辑、通用事件、数据生成；
  `src/client` ↔ clientOnly：渲染、GUI/HUD、按键、客户端事件、客户端混入。
  混入分别登记在 `annoying-speaker.mixins.json` / `annoying-speaker.client.mixins.json`。
- **证据**：`build.gradle`（`loom { splitEnvironmentSourceSets() }`）、`src/main/resources/fabric.mod.json`
- **检索**：`mc-src.ps1 grep "import net\.minecraft\.client" -Side common -Max 3`（应为 0）
- **落地**：硬约束——`src/main` 里 import `net.minecraft.client.*` 会编译失败。

### F-010 · 26.3 的注册写法：ResourceKey + ID 常量表
- **日期**：2026-11-22
- **问**：`Blocks`/`Items` 里的注册代码看不懂，ID 为什么不是字符串。
- **答**：分两层：
  1. **ID 层**：`net.minecraft.references` 里是 `ResourceKey` 常量表。`BlockItemId` 是
     `record BlockItemId(ResourceKey<Block> block, ResourceKey<Item> item)`，带
     `create(String)` / `create(Identifier, Identifier)` 工厂；常量形如 `BlockItemIds.STONE`。
  2. **注册层**：私有助手 `Blocks.register(BlockItemId, BlockBehaviour.Properties)`、
     `Items.registerItem(BlockItemId, factory, Item.Properties)`、`Items.registerBlock(BlockItemId, Block)`。
  模组侧（Fabric）等价写法是 `Registry.register(BuiltInRegistries.ITEM, Identifier, item)`，
  不需要照搬 `ResourceKey` 常量表。
- **证据**：`net/minecraft/references/BlockItemId.java:9-21`、`references/ItemIds.java`（499 行）、
  `world/level/block/Blocks.java:68-76`、`world/item/Items.java:2867-2907`
- **检索**：`mc-src.ps1 show net/minecraft/references/BlockItemId.java`
- **落地**：`src/main`。

---

## 待答 / 候选问题（下次提问时优先验证）

- 自定义 `SoundEvent` 注册在 26.3 的正确写法（`Registries.SOUND_EVENT` + `Holder`）。
- `Item.Properties` 在 26.3 的完整方法集（与旧版差异）。
- 自定义 `BlockEntity` + `BlockEntityRenderer` 的两侧配合。
- Fabric API `0.166.2+26.3` 里对应事件（`UseBlockCallback` 等）是否更名。
- 客户端 `KeyMapping` 注册与 `ClientTickEvents` 的推荐入口。
