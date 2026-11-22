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

### F-011 · 方块类物品怎么注册（可放置 + 可在手中使用）
- **日期**：2026-11-22
- **问**：`models_of_speaker` 里的模型要做一个方块类物品，既能放置又能手持使用；
  怎么"调用 `Items.java` 里的写法"来注册。
- **答**：`Items.java` 里那套是 **private 助手 + 三步**，模组里"复刻"这三步而不是调用它：
  1. `Item.Properties#setId(ResourceKey<Item>)` —— **必填**，否则 `itemIdOrThrow()` 抛 `Item id not set`；
  2. 若是 `BlockItem`，调 `blockItem.registerBlocks(Item.BY_BLOCK, item)` —— 建立 方块→物品 映射
     （`Block#asItem()`、掉落、创造栏都依赖它）；
  3. `Registry.register(BuiltInRegistries.ITEM, ResourceKey<Item>, item)`。
  方块侧可以先 `Blocks.register(...)`（Fabric 已把它拓宽为 `public`），它会自动 `properties.setId(id)`。
  - **放置**：`BlockItem#useOn` → `place(new BlockPlaceContext(ctx))`，默认就能放，不用自己写。
  - **手持使用**：覆写 `Item#use(Level, Player, InteractionHand)`；它只在"右键空气/不指向方块"时被调用。
    指向方块时走的是 `useOn`（先尝试放置），所以"放置优先、手持特效兜底"是默认语义。
  - **资源（26.3 实测，与原版 jar 对照）**：
    | 要写的东西 | 路径 | 说明 |
    |---|---|---|
    | 物品模型定义 | `assets/<ns>/items/<id>.json` | **不是** `models/item/`。内容 `{"model":{"type":"minecraft:model","model":"<ns>:block/<id>"}}`（原版 1659 个） |
    | 方块状态 | `assets/<ns>/blockstates/<id>.json` | `{"variants":{"":{"model":"<ns>:block/<id>"}}}` |
    | 几何模型 | `assets/<ns>/models/block/<id>.json` | Blockbench 的 elements 可直接用；`textures` 建议写全名 `"<ns>:block/mainbody"` |
    | 贴图 | `assets/<ns>/textures/block/*.png` | 模型里的 `"#0"` 等占位最终指向这里 |
    | 语言 | `assets/<ns>/lang/en_us.json` / `zh_cn.json` | 键 `block.<ns>.<id>`（配合 `useBlockDescriptionPrefix()`） |
    | 掉落 | `data/<ns>/loot_table/blocks/<id>.json` | 不写就挖不掉；默认键由方块 id 派生 `blocks/<id>` |
  - Blockbench 元数据（`format_version`、`credit`、`groups`）Java 会忽略；`display` 段是合法的
    （`CuboidModel` 显式解析 `"display"`），可直接用于手持/物品栏姿态。
  - **26.3 战利品表 schema 变了**：池里用单数 `"condition"` / `"modifier"`（不是 1.21 的 `conditions` 数组），
    原版样例见 `data/minecraft/loot_table/blocks/jukebox.json`。
- **证据**：`world/item/Items.java:2937-2944`（registerItem 三步）、`:2871-2911`（registerBlock 变体）、
  `world/item/BlockItem.java:35`（构造）、`:41`（useOn→place）、`:55-98`（place 流程）、`:190`（registerBlocks）、
  `world/item/Item.java:122`（`public static final Map<Block,Item> BY_BLOCK`）、`:665`（setId）、`:688`（itemIdOrThrow）、
  `world/level/block/Blocks.java:5655`（public `register`，内部 `properties.setId(id)`）、
  `core/Registry.java:110/114`（两个 `register` 重载）、
  `client/resources/model/cuboid/CuboidModel.java:51`（`display` 解析）
- **检索**：`mc-src.ps1 show net/minecraft/world/item/Items.java -From 2900 -To 2945`
- **产物**：完整可复制代码见 `10-code-recipes.md` **R-001**（4 个 java 文件 + 全部资源 JSON）。
- **落地**：`src/main`（common 侧）；方块与物品都要注册，客户端无需额外代码（模型自动加载）。
  `Item.Properties#useBlockDescriptionPrefix()` 让语言键变成 `block.<modid>.<id>`，与方块共用同一套名称。

### F-012 · 自写代码的 Doxygen 注释按什么规则
- **日期**：2026-11-22
- **问**：确立 Doxygen 文档注释编写规则。
- **答**：规则独立成文 → **`11-doxygen-rules.md`**（`D1`–`D22`，可在 review 里直接引用编号）。要点：
  - 形式：只用 `/** */`，块内 ` * ` 对齐，单行 ≤ 120 列（`D1`–`D5`）；
  - 结构：**首行即摘要**（`JAVADOC_AUTOBRIEF = YES`，因此**禁写 `@brief`**，否则重复渲染）、标签顺序固定、`@param` 名字逐字一致（`D6`–`D11`）；
  - 内容：写**约束**不写复述——侧别、线程、tick 阶段、幂等性；注册表字段必须写表名 + 完整 ID + 原版 `@see`；Mixin 必须写目标签名与冲突风险（`D12`–`D18`）；
  - 边界：不写 getter/一行方法，禁 `@author`/`@date`/`@version`，过期注释视为缺陷（`D19`–`D22`）；
  - 交叉引用用 `@ref` / `@see`，**禁用 `{@link}` / `{@code}`**（Doxygen 不解析 javadoc 内联标签，会原样吐出花括号），代码字体用 `@c`、参数名用 `@p`。
- **证据**：Doxygen 无 javadoc 内联标签支持；`JAVADOC_AUTOBRIEF = YES` 与手写 `@brief` 冲突。
- **检索**：无源码检索；规则作用于 `src/main/java`、`src/client/java` 的 `com.ggscaletest.*`。
- **产物**：`11-doxygen-rules.md`（4 类模板、提交前自检命令、最小 Doxyfile）。

### F-013 · 在 ModItems/ModBlocks 里注册一个"暂无特性"的方块类物品（id = annoying-speaker）
- **日期**：2026-11-22
- **问**：设置一个暂时无任何特性的方块类物品，物品 ID 为 `annoying-speaker`，用既有 `ModItems` 与 `ModBlocks` 注册。
- **答**：按 F-011 的三步落进两个既有类，**不引入自定义子类**（"无特性"= 用原版 `Block` 与 `BlockItem`）：
  - `ModBlocks`：`ANNOYING_SPEAKER_ID = AnnoyingSpeaker.id("annoying-speaker")`（→ 完整 ID `annoying-speaker:annoying-speaker`）、
    `ANNOYING_SPEAKER_KEY = ResourceKey.create(Registries.BLOCK, …)`、`Blocks.register(KEY, Block::new, Properties.of().strength(1.5F))`；
  - `ModItems`：`ANNOYING_SPEAKER_ITEM_KEY` 复用方块 ID，`registerBlockItem(…, BlockItem::new)` 做 `setId → registerBlocks → Registry.register`；
  - `AnnoyingSpeaker#onInitialize()` 先 `ModBlocks.initializer()` 再 `ModItems.initializer()`，把"方块先于物品"变成显式顺序。
  - ID 用连字符是合法的：`Identifier.validPathChar` 明确包含 `-`（`Identifier.java:262`），不会抛 `IdentifierException`。
- **证据**：`resources/Identifier.java:262`（允许 `-`）、`world/level/block/Block.java:232`（`public Block(Properties)`，故 `Block::new` 可用）、
  `resources/ResourceKey.java:28`（`create(ResourceKey<? extends Registry<T>>, Identifier)`）、
  `world/level/block/Blocks.java:5655`（`register` 三重载中唯一的 3 参版本）
- **检索**：`mc-src.ps1 show net/minecraft/world/level/block/Blocks.java -From 5648 -To 5670`
- **落地**：`src/main`（common 侧）。**尚未编译验证**——本机只有 JDK 21，Loom 1.19 要求 JDK 25（见 `01-sources.md` §6）。
- **产物**：`ModBlocks.java`、`ModItems.java`（含 Doxygen 注释）、`AnnoyingSpeaker.java` 的 2 行改动；
  最小版代码同时并入 `10-code-recipes.md` R-001。

---

## 待答 / 候选问题（下次提问时优先验证）

- 自定义 `SoundEvent` 注册在 26.3 的正确写法（`Registries.SOUND_EVENT` + `Holder`）。
- `Item.Properties` 在 26.3 的完整方法集（与旧版差异）。
- 自定义 `BlockEntity` + `BlockEntityRenderer` 的两侧配合。
- Fabric API `0.166.2+26.3` 里对应事件（`UseBlockCallback` 等）是否更名。
- 客户端 `KeyMapping` 注册与 `ClientTickEvents` 的推荐入口。
