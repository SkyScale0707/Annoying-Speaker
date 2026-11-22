# 02 · 通用 + 逻辑服务端源码结构（`minecraft-common`）

`5037` 个 `.java`。这一侧是**双端共享逻辑 + 逻辑服务端**：全部游戏规则、世界状态、数据驱动内容都在这里，
客户端只是把它渲染出来。写服务端逻辑、注册内容、数据生成、协议，全部落在这一侧。

---

## 1. 顶层包地图

| 包 | 文件 | 职责 |
|---|---:|---|
| `net.minecraft.world` | 2715 | 世界本体：方块、实体、物品、区块、世界生成、容器、效果、伤害、时钟 |
| `net.minecraft.util` | 735 | 工具；**其中 411 个是 `util/datafix`**（存档版本迁移，几乎不用读） |
| `net.minecraft.server` | 432 | 专用服务端：主循环、玩家管理、命令、资源包、JSON-RPC、对话框、权限 |
| `net.minecraft.network` | 416 | 协议：包定义、聊天、编解码、同步器 |
| `net.minecraft.data` | 174 | **数据生成器**（datagen）：配方、战利品、标签、模型、世界生成导出 |
| `net.minecraft.commands` | 124 | Brigadier 命令树、参数类型、选择器 |
| `net.minecraft.core` | 123 | 注册表、`BlockPos`/`Direction`、`Holder`、数据组件、粒子 |
| `net.minecraft.advancements` | 115 | 进度/准则/触发器 |
| `net.minecraft.gametest` | 48 | 官方 GameTest 框架（`@GameTest` 等注解在 `net.minecraft.gametest.framework`） |
| `net.minecraft.nbt` | 43 | NBT 读写 |
| `net.minecraft.tags` | 29 | 标签键与标签加载 |
| `net.minecraft`（根，19） | 19 | `SharedConstants`、`CrashReport`、`ChatFormatting`、`IdentifierException` 等 |
| `net.minecraft.gizmos` | 15 | 调试几何体（服务端可下发调试线框） |
| `net.minecraft.resources` | 12 | `Identifier`、`ResourceKey`、`RegistryOps`、注册表数据加载 |
| `net.minecraft.stats` | 10 | 统计 |
| `com.mojang.math` | 10 | 数学（`Vec3` 在 `world.phys`，这里只有矩阵/四元数等） |
| `net.minecraft.sounds` | 6 | `SoundEvent`、`SoundEvents`（1842 条注册）、`SoundSource` |
| `net.minecraft.references` | 5 | **ID 常量表**：`ItemIds`(499 行)、`BlockIds`、`BlockItemId`/`BlockItemIds`（`ResourceKey` 常量，26.3 注册标配） |
| `net.minecraft.locale` | 3 | 语言表 |
| `net.minecraft.recipebook` | 3 | 配方书服务端部分 |

---

## 2. 启动链路（专用服务端）

```
net/minecraft/server/Main.java                  ← main()，解析参数、加载 eula/世界
  └─ Bootstrap.java                              ← 注册表自举（vanilla 内容注册）
  └─ WorldLoader.java / WorldStem.java           ← 资源包 + 数据包 → 注册表冻结
  └─ MinecraftServer.java (2277 行)              ← 主编排器：tick 循环、世界列表、玩家列表、
     ├─ DedicatedServer.java (851)               ← 专用服务端实现（控制台、RCON、属性文件）
     ├─ ServerLevel.java (2086) × N              ← 每个维度一个世界
     ├─ PlayerList.java (860)                    ← 玩家连接/断开/广播/OP
     └─ ServerConnectionListener.java (238)      ← Netty 监听，创建各阶段 packet listener
```

服务端单 tick 顺序（`MinecraftServer#tickServer`）：网络 → 玩家列表 → 每个 `ServerLevel`（实体、方块实体、区块、天气）
→ 自动保存 → 广播。读 tick 相关 bug 时从 `MinecraftServer#tickServer` / `ServerLevel#tick` 入手。

## 3. `net.minecraft.server` 明细

| 子包 | 文件 | 职责 |
|---|---:|---|
| （根，27） | 27 | `MinecraftServer`、`WorldLoader`、`ReloadableServerResources`、`ServerFunctionManager`、`ServerScoreboard`、`Bootstrap` |
| `commands` | 111 | `VanillaCommand` 之外的全部原版命令实现（`/sound`、`/summon`、`/data`…） |
| `jsonrpc` | 65 | **新增**：服务端管理 JSON-RPC 接口（`Connection`、`JsonRpc`、`IncomingRpcMethods`） |
| `packs` | 57 | 资源包/数据包：`repository`(11)、`resources`(18)、`metadata`(5)、`linkfs`(6) |
| `level` | 42 | 服务端世界：`ServerLevel`、`ServerPlayer`、`ServerChunkCache`、`ChunkMap`、`ChunkHolder`、`DistanceManager`、票证系统 |
| `dialog` | 35 | **新增**：服务端驱动对话框（`Dialog`、`ActionButton`、`DialogAction`） |
| `network` | 29 | 各阶段 `Server*PacketListenerImpl`、`PlayerChunkSender`、`TextFilter` |
| `players` | 19 | `PlayerList`、`PlayerAdvancements`、`ServerStatsCounter`、白名单/OP 存档 |
| `permissions` | 12 | **新增**：权限系统（`Permission`、`PermissionLevel`、`PermissionSet`、`Permissions`） |
| `rcon` | 9 | RCON 远程控制 |
| `dedicated` | 7 | 专用服务端启动与设置 |
| `notifications` | 5 | **新增**：服务端通知 |
| `gui` | 4 | 专用服务端的 Swing 控制台窗口（`MinecraftServerGui`、`PlayerListComponent`、`StatsComponent`） |
| `bossevents` | 3 | `ServerBossEvent` |
| `chase` | 3 | **新增**：`ChaseServer`（追猎/观战相机） |
| `waypoints` | 2 | 服务端航点同步 |
| `advancements` | 2 | 进度管理器 |

**难点提示**：区块加载/卸载的因果链是 `ChunkMap → DistanceManager(票证) → ChunkHolder → ChunkLevel`，
排查"区块不加载/不卸载/实体被挤掉"一律从 `server/level/ChunkMap.java` 与 `TicketType.java` 开始。

## 4. `net.minecraft.world` 明细

| 子包 | 文件 | 职责 |
|---|---:|---|
| `level/block` | 474 | 方块基类 + 全部方块实现 + `state/`（`BlockBehaviour`、`BlockState`） |
| `level/levelgen` | 458 | 世界生成：`structure`132、`feature`135、`densityfunction`41、`placement`26、`material`26、`blockpredicates`21、`carver`5 |
| `entity/ai` | 277 | 生物 AI：`behavior`119、`goal`73、`sensing`27、`memory`8、`navigation`7、`control`9、`attributes`9 |
| `level/storage` | 217 | 存档：**`loot`194**（战利品表）+ 区域文件/`LevelStorageAccess`/DataFixer 接线 |
| `entity/animal` | 130 | 动物实现 |
| `entity/monster` | 84 | 怪物实现 |
| `item/crafting` | 70 | 配方：`Recipe`、`RecipeSerializer`、各配方类型 |
| `level/chunk` | 55 | `LevelChunk`、`ChunkAccess`、`ProtoChunk`、`ChunkStatus` 链 |
| `item/component` | 53 | 物品组件**负载类型**（`Consumable`、`BundleContents`、`BlocksAttacks`、`Bees`…）；组件类型与总表在 `core/component`（`DataComponentType`、`DataComponents`） |
| `item/enchantment` | 47 | 附魔 + 附魔效果（`effects/` 里是 `PlaySoundEffect` 这类） |
| `entity/projectile` | 37 | 弹射物 |
| `entity/{vehicle,boss}` | 24 / 24 | 载具 / BOSS |
| `phys/shapes` | 21 | `VoxelShape`、碰撞箱 |
| `level/{entity,biome}` | 19 / 19 | 实体查找/追踪 / 生物群系 |
| `level/{lighting,pathfinder,gameevent}` | 15 / 15 / 15 | 光照 / 寻路 / 游戏事件（监守者振动） |
| `level/saveddata` | 14 | `SavedData`（世界级持久数据） |
| `item/{slot,equipment}` | 14 / 14 | 槽位 / 装备 |
| `entity/player` | 14 | `Player`、`Inventory`、`PlayerEnderChestContainer` |
| `level/{material,redstone,dimension}` | 12 / 10 / 8 | 材质 / 红石 / 维度 |
| `level/gamerules` | 7 | 游戏规则（`GameRules` 在 `world/level/gamerules/`） |
| `damagesource` | — | `DamageSource`、`CombatTracker`（在 `world/damagesource`） |
| `effect` | — | `MobEffect`、`MobEffects` |
| `food` | — | `FoodProperties`、`FoodConstants` |
| `attribute` | — | **环境属性系统**（`EnvironmentAttributes`、`EnvironmentAttributeSystem`，新增） |
| `clock` | — | **世界时钟**（`WorldClocks`、`ServerClockManager`，新增） |
| `timeline` | — | **时间线/关键帧**（驱动环境属性，新增） |
| `waypoints` | — | **航点**（定位条，新增） |
| `ticking`/`ticks` | — | 计划刻调度（`LevelTicks`、`ScheduledTick`） |

常用索引表（内容注册的"总目录"，找 ID 先来这里）：

| 表 | 路径 | 条数（约） |
|---|---|---:|
| 全部物品 | `world/item/Items.java` | 2900+ 行 |
| 全部方块 | `world/level/block/Blocks.java` | 5681 行 |
| 全部声音 | `sounds/SoundEvents.java` | 1842 条注册 |
| 全部实体类型 | `world/entity/EntityType.java` | — |
| 全部效果 | `world/effect/MobEffects.java` | — |

## 5. 网络层

```
net/minecraft/network/protocol/
  handshake(6) → status(8) → login(19) → configuration(14) → game(198)   ← 5 个连接阶段
  common(28)  cookie(6)  ping(6)
```

- 每个包一个类，命名固定：`Clientbound*Packet`（服务端→客户端）/ `Serverbound*Packet`（客户端→服务端）。
- 包体本身只是数据 + `StreamCodec`；**处理逻辑在 `*PacketListenerImpl`**：
  - 服务端：`server/network/ServerGamePacketListenerImpl.java`（2283 行，收包总入口）
  - 客户端：`client/multiplayer/ClientPacketListener.java`（2773 行）
- `network/chat`(63)：`Component`、`Style`、`ClickEvent`、`ChatType` 等。
- `network/syncher`(6)：`SynchedEntityData`（实体数据同步）。
- 26.3 的包定义已全面 `StreamCodec` 化（`Codec` 用于 NBT/JSON，`StreamCodec` 用于网络），新写包照抄邻近包即可。

## 6. 注册表与数据驱动

| 位置 | 作用 |
|---|---|
| `core/registries/BuiltInRegistries.java` (443) | 所有内置注册表实例（`ITEM`、`BLOCK`、`SOUND_EVENT`…） |
| `core/registries/Registries.java` (355) | 注册表 **`ResourceKey` 常量**集合（`Registries.ITEM` 指向哪个表） |
| `core/MappedRegistry.java` / `DefaultedRegistry.java` | 注册表实现；`DefaultedRegistry` 用于 `air`/`empty` 这类默认值表 |
| `core/Holder.java` / `HolderSet.java` | 注册表项的引用与集合（`Holder.Reference` 即"已绑定的注册项"） |
| `resources/Identifier.java` | **资源标识符**（`namespace:path`，本版本替代旧 `ResourceLocation`） |
| `resources/ResourceKey.java` | 注册表键 |
| `resources/RegistryOps.java` / `RegistryDataLoader.java` | 带注册表上下文的编解码 / 从数据包加载注册数据 |
| `tags/*` + `data/tags` | 标签（`TagKey` + 数据包 JSON + datagen 生成） |
| `core/component/*` | 数据组件（`DataComponents` 索引表） |

数据驱动内容（JSON 定义、Java 只留 codec）包括：配方、战利品表、进度、标签、世界生成、附魔、伤害类型、
`dialog`、`trim` 等。**改数值优先找 `src/main/resources/data/...` 或 datagen，而不是 Java。**

## 7. 26.3 里值得先知道的新子系统

| 子系统 | 位置 | 说明 |
|---|---|---|
| 环境属性 | `world/attribute/` | 由数据包/时间线驱动的"世界环境"参数（光照、音效、天空色等），`EnvironmentAttribute` 是 key |
| 世界时钟 | `world/clock/` | `WorldClocks` / `ServerClockManager`，可被命令与数据包读写 |
| 时间线 | `world/timeline/` | 关键帧轨道，驱动环境属性插值 |
| 航点 | `world/waypoints/` + `server/waypoints` | 定位条目标（`TrackedWaypoint`） |
| 服务端对话框 | `server/dialog/` | 服务端直接下发带按钮的 UI 描述 |
| 权限系统 | `server/permissions/` | 细粒度权限集合（不只有 op level） |
| JSON-RPC | `server/jsonrpc/` | 外部程序管理服务端的接口 |
| 追猎相机 | `server/chase/` + `client` 侧 | `ChaseServer`/`ChaseClient` |
| 调试几何体 | `gizmos/` | 服务端下发线框/文字调试图形 |

## 8. 服务端侧判定口诀

1. 路径不含 `net.minecraft.client`、`com.mojang.{blaze3d,renderpearl,realmsclient}` → 本侧。
2. 类名带 `Server` 前缀（`ServerLevel`、`ServerPlayer`）或位于 `server/` 下 → 服务端专属；
   但注意 `ServerLevel` 在客户端集成服务端里也存在。
3. 参考坐标：`Level` 是抽象基类，`ServerLevel`（本侧）/`ClientLevel`（客户端侧）各自实现。
4. 需要 `@Environment(EnvType.SERVER)` 守卫的代码一定在本侧；需要 `CLIENT` 的在另一侧。
