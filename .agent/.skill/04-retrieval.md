# 04 · 检索手册

目标：任何"XX 在哪儿 / 怎么改"的问题，都能在 **3 次工具调用内**收敛到一个文件。

---

## 1. 五步法

| 步 | 动作 | 工具 |
|---|---|---|
| ① 定侧 | 这段逻辑属于 common 还是 clientOnly？ | `01-sources.md` §1、§8/§10 口诀 |
| ② 查旧账 | 之前答过吗？有改名/陷阱记录吗？ | `05-findings.md` |
| ③ 收包 | 从包地图挑 1–2 个候选目录 | `02`/`03` 包表 + `mc-src.ps1 tree …` |
| ④ 找类 | 类名 → 文件 → 定义行 | `find <正则>` → `grep "class X"` |
| ⑤ 读+反查 | 读方法体，再搜谁调用/被调用 | `show` + `grep "\.name\("` |

## 2. 工具速查

```powershell
$s = ".agent\.skill\scripts\mc-src.ps1"
powershell -NoProfile -ExecutionPolicy Bypass -File $s ensure               # 解包/校验
powershell -NoProfile -ExecutionPolicy Bypass -File $s anchors              # 入口类 + 行数
powershell -NoProfile -ExecutionPolicy Bypass -File $s stats                # 双树规模与顶层包
powershell -NoProfile -ExecutionPolicy Bypass -File $s find PlaySound       # 同名类
powershell -NoProfile -ExecutionPolicy Bypass -File $s grep "extends Goal" -Side common -Max 20
powershell -NoProfile -ExecutionPolicy Bypass -File $s tree net/minecraft/world/entity/ai -Depth 1
powershell -NoProfile -ExecutionPolicy Bypass -File $s show net/minecraft/world/level/Level.java -From 380 -To 480
```

限制与技巧：`grep` 默认 `-Side both`，输出 `相对路径:行号: 内容`；命中过多就加 `-Side`、
把正则写具体（`"void playSound"`）、或直接 `show` 目标文件。

## 3. 命名约定（按后缀查，命中率最高）

| 后缀/前缀 | 含义 | 例 |
|---|---|---|
| `*Packet` | 网络包数据体；`Clientbound`=下行，`Serverbound`=上行 | `ClientboundSoundPacket` |
| `*PacketListenerImpl` | 收包处理者（逻辑在 `handle*` 方法） | `ServerGamePacketListenerImpl` |
| `*Payload` | 自定义负载（Fabric/原版新协议常用） | — |
| `*Renderer` | 渲染器 | `EntityRenderer`、`LevelRenderer` |
| `*Model` | 模型几何与姿态 | `player/PlayerModel` |
| `*FeatureRenderer` | 实体附加层（火焰、披风…） | `FlameFeatureRenderer` |
| `*Screen` / `*Widget` | 界面 / 控件 | `PauseScreen`、`Button` |
| `*Overlay` | 覆盖层（HUD 之上） | `DebugScreenOverlay` |
| `*Goal` | 生物 AI 目标 | `MeleeAttackGoal` |
| `*Sensor` | AI 感知器 | `NearestLivingEntitySensor` |
| `*Block` / `BlockEntity` | 方块 / 方块实体 | `NoteBlock`、`JukeboxBlockEntity` |
| `*Item` | 物品（`Items` 是总表） | `NoteBlockItem` |
| `*Entity` / `*Mob` | 实体基类 / 生物 | `Entity`、`Mob` |
| `*Effect` | 效果（药水/附魔/消耗） | `PlaySoundEffect` |
| `*Recipe` / `*Serializer` | 配方 / 配方编解码 | `ShapedRecipe` |
| `*Codec` 常量、`CODEC` 字段 | 序列化（`Codec`=数据，`StreamCodec`=网络） | `Identifier.CODEC` |
| `*Provider` | datagen 生成器 | `RecipeProvider` |
| `*SavedData` | 世界级持久数据 | `ScoreboardSaveData` |
| `*Placement` / `*Feature` / `*Carver` / `*Structure` | 世界生成四件套 | `OrePlacement`、`TreeFeature` |
| `*DataComponentType` | 物品组件类型（`DataComponents` 是总表） | `DataComponents.FOOD` |
| `*ParticleOptions` | 粒子参数 | `DustParticleOptions` |
| `*SoundEvent` / `SoundEvents` | 声音事件 / 总表 | `SoundEvents.NOTE_BLOCK_HARP` |
| `*Position` / `*Pos` | 坐标 | `BlockPos` |

## 4. 双端对照表（同一个概念，两侧各有一份）

| 概念 | common（逻辑） | clientOnly（表现） |
|---|---|---|
| 世界 | `world/level/Level.java`（抽象） + `server/level/ServerLevel.java` | `client/multiplayer/ClientLevel.java` |
| 玩家 | `world/entity/player/Player` → `server/level/ServerPlayer.java` | `client/player/LocalPlayer.java` |
| 连接 | `server/network/ServerGamePacketListenerImpl.java` | `client/multiplayer/ClientPacketListener.java` |
| 区块 | `server/level/ChunkMap.java`、`world/level/chunk/LevelChunk.java` | `client/renderer/chunk/*`（只负责渲染） |
| 实体表现 | 逻辑在 `world/entity/*` | 渲染器 `client/renderer/entity/*`，模型 `client/model/*` |
| 声音 | `sounds/SoundEvents.java`、`Level#playSound` | `client/sounds/SoundManager`、`SoundEngine` |
| 方块 | `world/level/block/*` | `client/renderer/block/*`（模型/渲染类型） |
| 界面 | `server/dialog/*`（服务端驱动对话框） | `client/gui/screens/*` |
| 数据生成 | `data/*`（含服务端数据） | `client/data/*`（图集等客户端资源） |
| 命令 | `commands/*`、`server/commands/*` | `client/gui/components/CommandSuggestions` |

## 5. 常见任务 → 检索入口

| 任务 | 先看这里 | 一条能跑的命令 |
|---|---|---|
| 新增物品/方块 | `core/registries/BuiltInRegistries`、`references/{ItemIds,BlockIds,BlockItemIds}`、`world/item/Items`、`world/level/block/Blocks` | `grep "= register\(" -Side common -Max 10` |
| 注册可放置+可手持使用的方块物品 | `Items.java:2937`（三步）、`BlockItem#useOn/place`、`Item#use` | `show net/minecraft/world/item/Items.java -From 2900 -To 2945` |
| 让某个东西发声 | `world/level/Level.java:383-476` | `show net/minecraft/world/level/Level.java -From 375 -To 480` |
| 自定义声音事件 | `sounds/SoundEvent.java`、`sounds/SoundEvents.java` | `find SoundEvent` |
| 新增网络包 | `network/protocol/game/*` + 两侧 listener | `find ServerboundChatPacket` |
| 新增界面 | `client/gui/screens/Screen.java:121` | `show net/minecraft/client/gui/screens/Screen.java -From 100 -To 140` |
| 改 HUD | `client/gui/Hud.java`（`extract*` 系列） | `grep "private void extract" -Side client -Max 30` |
| 加按键 | `client/KeyMapping.java`、`client/Options.java` | `find KeyMapping` |
| 实体渲染 | `client/renderer/entity/*`、`client/model/*` | `find "EntityRenderer"` |
| 新 AI 行为 | `world/entity/ai/goal/*` | `grep "extends Goal" -Side common` |
| 新命令 | `commands/Commands.java`、`server/commands/*` | `find VanillaCommand` |
| 数据生成 | `data/*Provider`、`data/recipes/RecipeProvider.java` | `find "Provider" -Side common -Max 30` |
| 世界生成 | `world/level/levelgen/{feature,structure,placement}` | `grep "class .*Feature extends" -Side common` |
| 战利品表 | `world/level/storage/loot/*`（194 个） | `find LootTable` |
| 进度/准则 | `advancements/*` | `find CriterionTrigger` |
| 物品组件 | `core/component/DataComponents.java` | `show net/minecraft/core/component/DataComponents.java -From 1 -To 40` |
| 附魔 | `world/item/enchantment/*` | `find Enchantment` |
| 存档/持久数据 | `world/level/storage/*`、`level/saveddata/*` | `find SavedData` |
| 客户端粒子 | `client/particle/*` | `find ParticleProvider` |
| 混合注入点 | 先找到目标类/方法，再 `grep` 其调用者 | `grep "\.playSound\(" -Side common -Max 20` |

## 6. 判定"这个类属于哪一侧"

```powershell
# 该文件是否客户端专属？看它 import 谁
powershell ... -File $s grep "^import net\.minecraft\.client" -Side common -Max 5   # 应当 0 命中
```

- `common` 里出现 `import net.minecraft.client.*` → 说明找错树了，或该类被 `@Environment(CLIENT)` 保护（极少）。
- `client` 里出现 `import net.minecraft.server.*` 是**正常**的（集成服务端桥接，如 `client/server/*`）。
- 编译约束是最硬的判定：`src/main` 引 `net.minecraft.client` → 直接编译失败。

## 7. 读反编译源码的注意事项

| 现象 | 含义 |
|---|---|
| 形参全 `final`、字段写 `this.x` | 反编译风格，不代表编码要求 |
| 方法名 `lambda$xxx$0`、`access$000` | 合成成员，读逻辑时忽略 |
| 大量 `@Nullable` / `@NullMarked` | jspecify 契约，**重要**：参数可空就是真可空 |
| 无注释、无 javadoc | 只能靠调用点推断意图 |
| 数字常量（`0.5F`、`20`） | 原版魔法值；改行为时优先复用原常量而非重写 |
| 行号与线上崩溃栈不一致 | 用 `.jar.backup.linemap.txt` 或直接按方法名搜 |

## 8. 收敛技巧（命中过多时）

1. 先按**目录**收：`grep` 前在 `tree` 里确认目标子包，或把正则加上包名前缀（`world.level.block`）。
2. 用**方向前缀**收：`Clientbound`/`Serverbound`、`Client`/`Server`。
3. 用**定义式**而不是任意出现：`grep "class Foo\b"`、`grep "record Foo\("`、`grep "interface Foo\b"`。
4. 找"谁调用了它"：`grep "\.methodName\("`，然后排除定义文件本身。
5. 排除噪音区：`util/datafix`（411 个迁移类）、`data/`（生成器）几乎从不参与运行时逻辑。

## 9. 已验证有效的手法（持续追加）

- **索引表反查法**：`Items` / `Blocks` / `SoundEvents` / `EntityType` / `MobEffects` / `DataComponents` 是"内容总目录"，
  找某个 ID 的注册写法，直接在这些文件里搜字符串（如 `"block.note_block.harp"`），比搜类名快。
- **ID 常量表法**：26.3 原版注册用 `ResourceKey` 常量而非字符串，全部集中在 `net.minecraft.references`
  （`ItemIds` 499 行、`BlockIds`、`BlockItemIds`）。要确认某方块/物品的**真实 ID**，先查这里。
- **注册助手法**：`Blocks.register(BlockItemId, Properties)`、`Items.registerItem(...)` / `Items.registerBlock(...)`
  是原版唯一注册入口（`Blocks.java:68`、`Items.java:2867-2907`），照抄它们比照抄旧教程安全。
- **看 private 助手的尾部**：原版注册类（`Items`/`Blocks`/`SoundEvents`/`EntityType`）的**文件末尾**才是真正的注册机制
  （`Items.java:2937` 的 `registerItem` 揭示 `setId` + `registerBlocks` + `Registry.register` 三步）。
  读不到"怎么注册"时，直接 `show <表类>.java -From <总行数-45>`。
- **`Access widened` 注释＝Fabric 拓宽**：`grep "Access widened by fabric-transitive-access-wideners"` 可列出
  被 Fabric 变成 public 的原版成员（如 `Blocks.register`、`Item.Properties`），这些就是模组可复用的官方入口。
- **朝向模板法**：任何"方块要转向"的问题，先看 `HorizontalDirectionalBlock` 的子类——`AbstractFurnaceBlock`
  最干净（`getStateForPlacement`/`rotate`/`mirror`/`createBlockStateDefinition` 四件套），照抄即可。
  形状四向一律用 `Shapes.rotateHorizontal(north)`，别手算旋转矩阵。
- **形状找现成写法**：`grep "Shapes.rotateHorizontal(" -Side common` 能一次列出几十个带朝向的方块形状写法。
- **抽象基类三分法**：`Level`（抽象）/ `ServerLevel` / `ClientLevel` 三件套是理解双端差异的最短路径；同类还有
  `Player` 三分（`Player`/`ServerPlayer`/`LocalPlayer`）、`PacketListener` 三分。
- **`handle*` 定位法**：任何下行包的行为，都能在 `ClientPacketListener` 里找到同名 `handleXxx`。
- **两侧同名搜索**：`find X` 会同时给出两侧命中，先看 `[common]` 还是 `[client]` 再决定读哪个。

## 10. 已知陷阱与改名（读旧教程必看）

| 旧名/旧写法 | 26.3 实际 | 备注 |
|---|---|---|
| `ResourceLocation` | **`net.minecraft.resources.Identifier`** | 方法 `Identifier.fromNamespaceAndPath(...)`；字段 `namespace`/`path` |
| `GuiGraphics` | **`GuiGraphicsExtractor`** | `gui/GuiGraphicsExtractor.java`，1488 行 |
| `Screen#render(GuiGraphics, …)` | **`extractRenderState(GuiGraphicsExtractor, …)`** | `Screen.java:121` |
| `Screen#renderBackground` | `extractBackground` / `extractMenuBackground` | `Screen.java:425` 起 |
| `Widget#renderWidget` | `extractWidgetRenderState` → `extractContents` | `AbstractWidget.java:56`(外层) / `:90`(抽象) |
| `ServerWorld` / `ClientWorld`（Yarn 命名） | `ServerLevel` / `ClientLevel` | 本项目用 mojmap |
| `client/resources/sound/SoundManager` | `client/sounds/SoundManager` | 目录名是 `sounds` |
| `RenderSystem` 一把梭 | 两段式 extract + `GuiRenderer`/`LevelRenderer` 提交 | 见 `03-client-structure.md` §4 |
| 物品 NBT 标签 | 数据组件 `DataComponents` / `ItemStack` 组件 API | 见 `world/item/ItemStack.java` |

## 11. 盲区（这两棵树里**没有**的东西）

| 缺失内容 | 实际位置 | 说明 |
|---|---|---|
| Fabric API / Fabric Loader 源码 | `E:\MCProject\gradle_cache\caches\modules-2\files-2.1\net.fabricmc.fabric-api\<模块>\…\<模块>-sources.jar`（本地 48 个模块，含 item / sound / registry-sync 等；**item-group 模块本地缺失**，要用时先确认是否存在）+ `fabric-loader-0.20.1-sources.jar` | 回答"用 Fabric API 怎么写"时需说明来源，别在 Minecraft 源码里找 |
| `assets/`（贴图、模型 JSON、shader、语言） | `.gradle\loom-cache` 的 asset 目录 / 客户端 jar | 本 skill 只覆盖 `.java` |
| 数据包/资源包 JSON 内容 | 同上的 client jar / `run/` | 数据驱动内容的**定义**在 jar 资源里 |
| 原生库（LWJGL、OpenAL） | Gradle 依赖 | — |
| 混淆名（obf） | 本项目已 remap，不需要 | 查 obf 需 `minecraft-*.jar.backup` + 映射表 |
