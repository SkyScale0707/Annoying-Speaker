# 01 · 源码包坐标与两侧划分

## 1. 两个包各是什么

| 术语 | 目录名 | 来源 jar | 内容 | 谁运行它 |
|---|---|---|---|---|
| **common**（通用） | `minecraft-common-1b0498e74c` | `minecraft-common-...-26.3-sources.jar` | `net.minecraft.*` 全部**非客户端**代码：世界、实体、物品、方块、注册表、协议定义、命令、数据生成、逻辑服务端 | 专用服务端 + 客户端（客户端里跑的"集成服务端"） |
| **clientOnly**（纯客户端） | `minecraft-clientOnly-1b0498e74c` | `minecraft-clientOnly-...-26.3-sources.jar` | `net.minecraft.client.*`、`net.minecraft.realms.*`、`com.mojang.{blaze3d,renderpearl,realmsclient}.*` | **仅**物理客户端 |

> ⚠️ 最常见的误解：`common` **不等于**"服务端"。它是「双端共享 + 逻辑服务端」。
> 专用服务端只加载 `common`；客户端加载 `common` **加上** `clientOnly`。
> 因此 `common` 里能看到 `server/`（专用服务端实现）也能看到 `world/`（两侧共用）。

**官方证据**：`.gradle\loom-cache\launch.cfg` 里客户端启动参数同时给出了两个 game jar：

```text
fabric.gameJarPath       = ...\minecraft-common-...-26.3.jar
fabric.gameJarPath.client= ...\minecraft-clientOnly-...-26.3.jar
```

## 2. 物理坐标与指纹

根目录：`<repo>\.gradle\loom-cache\minecraftMaven\net\minecraft\`

| 文件 | 字节 | 条目 | 说明 |
|---|---:|---:|---|
| `minecraft-common-...-26.3-sources.jar` | 6,202,271 | 5038（5037 `.java`） | 通用源码（本 skill 主对象之一） |
| `minecraft-common-...-26.3.jar` | 25,366,758 | 18554 | remap 后的 class 包 |
| `minecraft-common-...-26.3.jar.backup` | 25,482,872 | — | loom 在 remap 前保留的备份 |
| `minecraft-common-...-26.3.jar.backup.linemap.txt` | 2,100,211 | — | 行号映射（类名 + 制表符分隔的行号对），调试栈对齐用 |
| `minecraft-clientOnly-...-26.3-sources.jar` | 2,649,607 | 2265（2264 `.java`） | 纯客户端源码 |
| `minecraft-clientOnly-...-26.3.jar` | 13,867,905 | 15682 | remap 后的 class 包 |
| `*.pom` | — | — | 纯坐标占位：`net.minecraft:minecraft-common-1b0498e74c:26.3` |

**SHA256 前缀**（用于判断缓存是否对应同一份 jar）：

- `common`   → `876DE95A51BD0C1FF40C4094582E770A`
- `clientOnly` → `9EF3282D5276D73B0D6D456F7C6FB8B2`

jar 更新后 `mc-src.ps1 ensure` 会自动重新解包（缓存内有 `.source-stamp.txt` 记录哈希）。
其它 loom 资产：`source_mappings\*.tiny`（本项目为 `empty.tiny`，即未启用外部映射叠加）、`log4j.xml`、`launch.cfg`。

## 3. 版本坐标（来自 `gradle.properties` / `build.gradle`）

| 项 | 值 |
|---|---|
| Minecraft | `26.3` |
| Fabric Loader | `0.20.1` |
| Loom | `1.19-SNAPSHOT` |
| Fabric API | `0.166.2+26.3` |
| Java | 25（`options.release = 25`） |
| 映射 | Mojang 官方名（mojmap 风格：`ServerLevel`、`net.minecraft.world.item.Item`、`Level` 等） |
| 资源索引 | `26.3-34`（`--assetIndex`） |

源码形态注意点：

- 反编译产物（Vineflower 风格）：保留字段顺序、**几乎没有注释**；形参大量 `final`、字段访问写成 `this.xxx`，这类写法是反编译特征而非代码要求。
- 全树 `@NullMarked`（`org.jspecify.annotations`），局部可空处标 `@Nullable`。**读签名时把 `@Nullable` 当作有效 API 信息**。
- 包级 `package-info.java` 只含 `@NullMarked`，不提供说明文字。

## 4. 解包缓存

| 位置 | 内容 |
|---|---|
| `.agent\.cache\src\common\` | 5037 个 `.java`，路径与 jar 内一致 |
| `.agent\.cache\src\client\` | 2264 个 `.java` |
| `.agent\.cache\.gitignore` | 整个缓存对 git 视而不见（`*` + `!.gitignore`） |

缓存是**可丢弃的派生物**：删掉后 `mc-src.ps1 ensure` 会重建（约 10 秒）。
禁止在缓存里改代码；所有修改都应落在 `src/main` / `src/client`。

## 5. 本模组工程与两棵树的关系

`build.gradle` 启用了 `loom { splitEnvironmentSourceSets() }`，因此：

| 工程源码集 | 编译期可见的源码树 | 运行环境 | 典型内容 |
|---|---|---|---|
| `src/main`（`com.ggscaletest.*`） | `common` | 双端 | 物品、方块、注册、通用事件（`ModItems`、`AnnoyingSpeaker`） |
| `src/client`（`com.ggscaletest.client.*`） | `common` + `client` | 仅客户端 | 渲染、GUI、按键、`ClientModInitializer` |

混入配置也按此切分：`src/main/resources/annoying-speaker.mixins.json`（通用）与
`src/client/resources/annoying-speaker.client.mixins.json`（客户端）。
**在 `src/main` 里引用 `net.minecraft.client.*` 会编译不过**——这是判断"这段代码该放哪一侧"的硬约束。
