# 03 · 纯客户端源码结构（`minecraft-clientOnly`）

`2264` 个 `.java`。只有物理客户端会加载这一侧；它**不包含**世界/实体/物品等逻辑本体，
而是"如何看、如何听、如何操作"：渲染、GUI、HUD、输入、声音、客户端侧世界镜像、资源加载。

---

## 1. 顶层包地图

| 包 | 文件 | 职责 |
|---|---:|---|
| `net.minecraft.client` | 1891 | 客户端全部逻辑 |
| `com.mojang.renderpearl` | 146 | **新增** GPU 抽象层（设备/渲染通道/管线/SPIR-V） |
| `com.mojang.realmsclient` | 137 | Realms 联机界面与请求 |
| `com.mojang.blaze3d` | 86 | 窗口、输入、缓冲、顶点、着色器装配等底层 |
| `net.minecraft.realms` | 4 | Realms 客户端的薄桥接 |

## 2. `net.minecraft.client` 内部

| 子包 | 文件 | 职责 |
|---|---:|---|
| （根，41） | 41 | `Minecraft`(3104)、`Options`(2037)、`KeyMapping`、`MouseHandler`、`Camera`、`SoundManager` 外的输入/计时器 |
| `renderer` | 715 | 渲染总成：实体/BE/方块/物品渲染器、状态提取、管线、后期 |
| `gui` | 445 | 界面：`Screen`、`Hud`、`GuiGraphicsExtractor`、控件、字体、布局、导航 |
| `model` | 268 | 实体/玩家/方块实体模型几何：`animal`115、`monster`72、`object`39、`geom`18 |
| `resources` | 110 | 客户端资源：`model`39（模型加载）、`sounds`26、`metadata`14、`language`7 |
| `particle` | 88 | 粒子引擎与各类粒子 |
| `multiplayer` | 63 | 客户端世界与连接：`ClientPacketListener`、`ClientLevel`、`chat`23、`resolver`7 |
| `data` | 28 | **客户端 datagen**（`AtlasProvider`、图集） |
| `animation` | 23 | 动画系统 |
| `telemetry` | 18 | 遥测 |
| `color` | 17 | 方块/生物群系颜色计算 |
| `sounds` | 17 | 声音引擎：`SoundEngine`556、`SoundManager`346、`SoundBufferLibrary`91 |
| `player` | 11 | `LocalPlayer`(1282)、`AbstractClientPlayer` |
| `tutorial` | 10 | 新手引导 |
| `searchtree` | 8 | 创造模式搜索树 |
| `input` | 8 | 输入抽象 |
| `server` | 6 | 客户端内嵌服务端的少量桥接（集成服） |
| `entity` | 4 | `ClientAvatarEntity`、`ClientMannequin` |
| `main` | 4 | `Main`(369) 启动、`GameConfig` |
| `quickplay` | 3 | 快速进入世界/服务器 |
| `waypoints` | 2 | 航点的客户端表现 |
| `profiling` | 2 | 客户端性能采样 |

---

## 3. 启动链路与主循环

```
net/minecraft/client/main/Main.java (369)     ← main()，解析参数、建 GameConfig
  └─ Minecraft.java (3104)                    ← 客户端单例：窗口、渲染器、资源、界面栈
     ├─ Minecraft#run()  → 主循环
     ├─ Minecraft#runTick() (1167)            ← 一帧：tick 逻辑 → 输入 → 渲染
     └─ Minecraft#tick()  (1874)              ← 20Hz 客户端 tick（GUI、世界、声音、网络）
```

关键点：
- `Minecraft` 是**单例**（`Minecraft.getInstance()`），几乎所有客户端功能都挂在它上面（`getTextureManager()`、`getSoundManager()`、`setScreen()`…）。
- 帧时间由 `DeltaTracker` 表达（`getGameTimeDeltaPartialTick(...)`）；渲染插值都要用它。
- 改"HUD/界面/渲染"之前，先确认入口是 **tick 阶段**（逻辑）还是 **render/extract 阶段**（画面），26.3 二者分离得很干净。

## 4. 渲染架构（26.3 重点）

```
Minecraft#runTick
  └─ GameRenderer.java (856)        ← 世界渲染总入口：相机、投影、雾、后期、HUD 调用
     ├─ LevelRenderer.java (1336)   ← 区块地形、实体、天气、天空
     │   ├─ SubmitNodeCollector / OrderedSubmitNodeCollector   ← "收集渲染任务"
     │   ├─ SubmitNodeStorage / SubmitNodeCollection           ← 任务存储与排序
     │   └─ renderer/entity 292 · blockentity 60 · block 48 · item 63 · feature 27
     ├─ renderer/state/{level,gui}  ← 每帧的渲染状态快照（提取→提交两段式）
     ├─ PostChain / PostEffect      ← 后期处理链
     └─ Gui 的 extract 流程（见 §5）
```

- **两段式**：先 `extract*`（把要画的东西写入 state/提交节点），再由 `GuiRenderer` / `LevelRenderer` 统一提交到 GPU。
  读渲染代码时，先找 `extract*` 方法找"画什么"，再找提交器找"怎么画"。
- `renderer/RenderPipelines.java`：所有内置渲染管线的常量入口。
- `renderer/texture`(37)：贴图与图集；`renderer/special`(17)：特殊绘制（地图、钟、附魔光效等）。
- 底层：`com.mojang.blaze3d`（窗口/输入/顶点/缓冲/`systems`）与 **`com.mojang.renderpearl`**（新的设备与管线抽象：
  `api/device`14、`api/pipeline`17、`api/commands`7、`api/buffers`4、`api/textures`6、`api/vertex`3；`backend` 含 SPIR-V/GLSL 编译）。
  26.3 的新渲染后端把 GPU 资源显式化，旧的 `RenderSystem` 仍存在于 `blaze3d/systems`。

## 5. GUI / HUD 架构（与 1.21 之前差别很大）

| 类 | 行数 | 作用 |
|---|---:|---|
| `gui/GuiGraphicsExtractor.java` | 1488 | **绘制 API 本体**（旧 `GuiGraphics` 的继任者）：`fill`、`text`、`blit`、`nextStratum`、`enableScissor`、`textRenderer(...)` |
| `gui/Gui.java` | 491 | 界面总调度（持有 `Hud`，决定各覆盖层顺序） |
| `gui/Hud.java` | 1462 | HUD 全部元素：血条、快捷栏、聊天、计分板、Boss 条、标题 |
| `gui/render/GuiRenderer.java` | 429 | GUI 提交器（把 extract 出来的状态真正画到屏幕） |
| `gui/render/GuiItemAtlas.java` / `DynamicAtlasAllocator` | 145 / 139 | GUI 物品图集与动态分配 |
| `gui/screens/Screen.java` | 675 | 界面基类：`extractRenderState(...)`、`renderBackground`→`extractBackground`、`init()` |

调用约定（写界面必看）：

```java
// 旧写法（1.20 及以前）               // 26.3 写法
render(GuiGraphics g, int mx, int my, float pt)   →   extractRenderState(GuiGraphicsExtractor g, int mx, int my, float pt)
renderBackground(...)                             →   extractBackground(...) / extractMenuBackground(...)
AbstractWidget#renderWidget(...)                   →   extractWidgetRenderState(...)  →  extractContents(...)
```

- 控件：`gui/components`126（`AbstractWidget`、`Button`、`EditBox`、`AbstractSelectionList`、`toasts/`）。
- **屏幕栈在 `Gui` 上**：26.3 的 `Minecraft` 已没有 `setScreen`，改用 `Minecraft.getInstance().gui.setScreen(screen)`
  （`Minecraft.java:308` 的 `public final Gui gui` + `Gui.java:234` 的 `setScreen`）；`Gui` 同时管 HUD 与界面栈，
  是"界面 + 覆盖层"的总调度。- 界面：`gui/screens`222（称号、暂停、选项、创造背包、社交…）。
- 辅助：`gui/layouts`12（`GridLayout` 等）、`gui/navigation`6（手柄/焦点导航，新增）、`gui/contextualbar`5（新增）、`gui/font`31、`gui/narration`8、`gui/spectator`11。
- `gui/render/pip`(8)：GUI 专用渲染管线。

## 6. 资源、模型、声音

| 位置 | 文件 | 说明 |
|---|---:|---|
| `resources/` | 110 | `ReloadableResourceManager`、`ResourceManager` 接线；`model`39 模型加载（`BlockStateModelLoader`、`ModelBaker`）、`sounds`26 音效实例（`AbstractSoundInstance` 等）、`metadata`14、`language`7 |
| `model/` | 268 | 模型几何与姿态：`animal`、`monster`、`object`、`player`、`geom`（`ModelPart` 等） |
| `particle/` | 88 | 粒子引擎、各类粒子渲染 |
| `sounds/` | 17 | `SoundEngine`（混音/流式播放）、`SoundManager`（ID→`SoundEvent`、分类音量）、`MusicManager` |
| `animation/` | 23 | 关键帧动画 |

### 6.1 资源文件布局（26.3，对照原版 jar 实测）

| 资源 | 路径 | 形状 |
|---|---|---|
| **物品模型定义** | `assets/<ns>/items/<id>.json` | `{"model":{"type":"minecraft:model","model":"<ns>:block/<id>"}}`；原版 1659 个。**注意不是 `models/item/`**（那 1310 个只是被引用的旧几何） |
| 方块状态 | `assets/<ns>/blockstates/<id>.json` | `{"variants":{"":{"model":"<ns>:block/<id>"}}}` |
| 几何模型 | `assets/<ns>/models/block/<id>.json` | `elements` + `textures` + 可选 `display`/`ambientocclusion`；Blockbench 导出可直接用 |
| 贴图 | `assets/<ns>/textures/block/*.png` | 模型里的 `"#0"` 占位最终解析到这里 |
| 语言 | `assets/<ns>/lang/<locale>.json` | 键 `block.<ns>.<id>` / `item.<ns>.<id>` |
| 掉落（数据侧，属 common） | `data/<ns>/loot_table/blocks/<id>.json` | 默认键由方块 id 派生为 `blocks/<id>`（`BlockBehaviour.java:1006`）；不写则挖掉不掉落 |

解析入口：`client/resources/model/cuboid/CuboidModel.java`（`"display"` 在 `:51`）、
加载器 `client/resources/model/{BlockStateModelLoader,ModelBaker}`。
战利品表在 26.3 用单数 `"condition"`/`"modifier"`（`LootPool.java:37-38`），不是旧版的 `conditions` 数组。

两个易误判点：

- 单变体方块的 blockstate 键是空字符串 `""`（原版 `jukebox.json` 即如此）。PowerShell 5.1 的
  `ConvertFrom-Json` 处理不了空键，会报"参数 name 无效"——是解析器的问题，用 `python -c "json.load(...)"` 复核。
- `display` 的槽位在 26.3 包含 `on_shelf`（`ItemTransforms.java:42` → `fixedFromBottom`），Blockbench 导出带它可直接用。
- 贴图分辨率与 UV 空间无关：UV 若按 16 空间导出，贴图放大到 64×64 仍按比例采样，**不要**补 `texture_width/height`。

## 7. 网络与客户端世界

| 类 | 行数 | 作用 |
|---|---:|---|
| `multiplayer/ClientPacketListener.java` | 2773 | **客户端收包总入口**：每个 `Clientbound*Packet` 的一个 `handle*` 方法 |
| `multiplayer/ClientLevel.java` | 1311 | 客户端世界镜像（实体表、粒子、声音、本地方块更新） |
| `player/LocalPlayer.java` | 1282 | 本地玩家输入/预测/手臂动画 |
| `multiplayer/prediction/` | 3 | 客户端预测 |
| `multiplayer/resolver/` | 7 | 实体解析（在客户端把 ID 还原成实体） |
| `multiplayer/chat/` | 23 | 聊天接收、`ChatListener`、消息延迟 |

## 8. 客户端专属杂项

`telemetry`(遥测)、`tutorial`(引导)、`quickplay`(快速进服)、`searchtree`(创造搜索)、`waypoints`(航点表现)、
`profiling`(采样)、`Options`(全部设置项，2037 行)、`KeyMapping`(按键绑定)、`MouseHandler`(鼠标)、`Camera`(相机)。

## 9. `com.mojang.*`

| 包 | 文件 | 说明 |
|---|---:|---|
| `blaze3d/platform` | 26 | 窗口、显示器、输入常量、剪贴板、图标 |
| `blaze3d/{vertex,buffers,pipeline,systems,framegraph}` | 14/3/5/5/3 | 顶点格式、缓冲、管线、图执行、帧图 |
| `blaze3d/audio` | 12 | OpenAL 封装 |
| `renderpearl` | 146 | 新 GPU 抽象（见 §4） |
| `realmsclient` | 137 | Realms 界面 + HTTP 客户端 |

## 10. 客户端侧判定口诀

1. 路径含 `net.minecraft.client` 或 `com.mojang.{blaze3d,renderpearl,realmsclient}` → 仅客户端。
2. 类名带 `Client` 前缀（`ClientLevel`、`ClientPacketListener`、`ClientBootstrap`）→ 客户端侧角色。
3. 类名带 `*Renderer` / `*Screen` / `*Widget` / `*Model` / `*Particle` / `*Overlay` → 基本都在本侧。
4. 想在 `src/main`（common）里用它们 → 编译失败。客户端专属代码必须落在 `src/client`，
   且混入配置写进 `annoying-speaker.client.mixins.json`。
5. 反过来：`common` 里的类在客户端**可用**（客户端同时加载两棵树），所以在 `src/client` 里 import `common` 是合法的。
