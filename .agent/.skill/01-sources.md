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
- **这两个 jar 是 Fabric 打过补丁的 Minecraft，不是纯原版**：`net.minecraft` 下约 42 个文件 `import net.fabricmc.*`
  （如 `Item.Properties implements net.fabricmc.fabric.api.item.v1.FabricItem.Properties`、
  `Registry` 引用 `FabricRegistry`）。被拓宽可见性的成员带注释
  `Access widened by fabric-transitive-access-wideners-v1 to accessible`（例：`Blocks.register` 是 `public`）。
  → 实践含义：这些成员在 Fabric 环境里**可以直接调用**，但在纯原版里可能是 private；
  写通用教程时不要把它们当原版 API 宣传。

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


## 6. 本地构建环境（2026-11-22 实测，已修正）

> ⚠️ **以下命令只允许用户执行。agent 不得运行 `gradlew` / `runClient`**（`agent_read.md` 的硬性要求）。
> 本节保留这些信息，是为了在用户贴出报错时能对上环境，而不是给 agent 的执行清单。
> agent 的验证手段：源码比读、JSON 解析（python）、字节码 `findstr`、行号核对。

| 项 | 实测值 |
|---|---|
| `JAVA_HOME`（系统默认） | `D:\java_develop_kit\jdk-21-windows-x64`（21.0.8）—— **跑不了 Gradle** |
| 可用的 JDK 25 | `C:\Users\逸\.vscode\extensions\redhat.java-1.58.0-win32-x64\jre\25.0.4.1-win32-x86_64`（VSCode Java 扩展内置的**完整 JDK**，含 `javac`，Temurin 25.0.4.1） |
| `GRADLE_USER_HOME` | `E:\MCProject\gradle_cache` |
| Gradle | 9.8.2 |
| Loom | `1.19.4`；插件 classpath 要求 JVM 25 |
| 用 JDK 21 跑 gradlew | **配置阶段**失败：`Dependency requires at least JVM runtime version 25. This build uses a Java 21 JVM` |

正确跑法：

```powershell
$env:JAVA_HOME='C:\Users\逸\.vscode\extensions\redhat.java-1.58.0-win32-x64\jre\25.0.4.1-win32-x86_64'
cmd /c "gradlew.bat compileJava --console=plain"   # 实测 BUILD SUCCESSFUL（约 1s 增量）
cmd /c "gradlew.bat runClient   --console=plain"   # 实测可启动，日志 run/logs/latest.log
```

要点：

- **"本机没有 JDK 25"曾经是错的**：它藏在 VSCode 扩展目录里，`where java` 与常见安装目录都找不到。
  找法：`Get-Process java | Select Path`，或直接看 `~/.vscode/extensions/redhat.java-*/jre/`。
- 用错 JVM 时失败在**配置阶段**（不是编译阶段），错误里写得很直白："This build uses a Java 21 JVM"。
- `runClient` 会弹出游戏窗口，验证完记得关；`gradlew` 后台跑时不要重复启动第二个实例。
- `org.gradle.configuration-cache=true` 会把失败的配置结果也缓存；换 JVM 后若行为异常，加 `--no-configuration-cache`。
