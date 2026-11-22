# 11 · Doxygen 注释编写规则

适用对象：本项目 `src/main/java`、`src/client/java` 下的**自写代码**（`com.ggscaletest.*`）。
**不适用**于 `.gradle\loom-cache` 里的 Minecraft 源码（反编译产物，无注释，也不允许改）。

规则编号 `D1`–`D22` 可在 review 里直接引用，例如"D7 违反"。

---

## 1. 形式（形式不对，Doxygen 就不认）

| 编号 | 规则 |
|---|---|
| **D1** | 只有一种注释块：`/**` 开头、`*/` 结尾。禁用 `///`、`/*!`、`<!-- -->`。 |
| **D2** | 块内每行以 ` * ` 对齐；`/**` 与 ` */` 单独成行。 |
| **D3** | 缩进跟随所在文件既有风格（`AnnoyingSpeaker.java` 用 tab，`ModItems/*` 用 4 空格），不跨文件统一。 |
| **D4** | 单行 ≤ 120 列；超长按语义换行，不把标识符拆开。 |
| **D5** | 注释块与被注释成员之间**不留空行**，注释块上方与上一个成员之间**留一空行**。 |

```java
/**
 * 正确形式。
 *
 * 细节段落与摘要之间空一行。
 *
 * @see net.minecraft.world.item.Item
 */
public static final Item SPEAKER = ...;
```

## 2. 结构

| 编号 | 规则 |
|---|---|
| **D6** | 首行是**一句话摘要**：≤ 50 字、完整句子、以 `。` 结尾，后接空行。Doxyfile 开 `JAVADOC_AUTOBRIEF = YES`，首行即 brief，**不要再写 `@brief`**（会重复渲染）。 |
| **D7** | 标签顺序固定：`@param` → `@return` → `@throws` → `@note` → `@warning` → `@see` → `@since` → `@deprecated`。 |
| **D8** | `@param` 按签名顺序排列，名字与形参**逐字一致**（改形参名必须同改注释）。 |
| **D9** | `@return` 仅在非 `void` 且返回值语义不显然时写；禁止复述方法名（"@return 返回物品"是废话）。 |
| **D10** | `@since` 写 **Minecraft 版本号**（本版本 `26.3`），不写日期。 |
| **D11** | 交叉引用用 `@ref` 与 `@see`，禁用 `{@link}` / `{@code}`（Doxygen 不解析 javadoc 内联标签，会原样吐出花括号）。代码字体用 `@c`，参数名用 `@p`。 |

## 3. 内容（这才是注释的价值所在）

| 编号 | 规则 |
|---|---|
| **D12** | 注释写**约束**，不写**复述**。必须回答其中至少一条：跑在哪一侧（server / client / both）、线程、tick 阶段、调用时机、是否幂等、失败会怎样。 |
| **D13** | 注册表字段必须写：注册到哪张表 + 完整 ID + 原版同类 `@see`。 |
| **D14** | 可空性用文字写明（"可为 null：表示…"），且必须与 `@Nullable` 标注一致，不得矛盾。 |
| **D15** | Mixin 类注释必须写：目标类、注入目的、目标方法名与签名、冲突风险。注入方法注释写：注入点、时机、是否 `cancellable`。 |
| **D16** | 用 26.3 的**现行类名**，禁止旧名（`ResourceLocation`、`GuiGraphics`、`ServerWorld`）。旧名只在"迁移说明"里出现，且需括注"旧名"。 |
| **D17** | 语言：中文为主；类型名、方法名、ID 保持英文不翻译；中文句子用中文标点，代码片段用 `@c` 包裹。 |
| **D18** | `@note` 写"使用者必须知道的坑"，`@warning` 写"会崩/会丢数据"。二者不互换。 |

## 4. 边界（不写什么）

| 编号 | 规则 |
|---|---|
| **D19** | 不为 getter/setter、一行 `return`、显而易见的小私有方法写文档注释；这些用普通 `//` 都不写。 |
| **D20** | 禁止 `@author` / `@date` / `@version`（git 已记录）；文件头如需署名，只允许 `@author` 一项。 |
| **D21** | 不写变更日志式注释（"2026-11-22 改成…"）；历史交给 git。 |
| **D22** | 过期注释即缺陷：改代码必须同一次提交改注释；被注释掉的旧代码直接删，不留 `//` 坟场。 |

## 5. 标签白名单

| 标签 | 用途 | 必填 |
|---|---|---|
| `@file` | 文件级说明（放在 `package` 之前） | 仅注册入口/Mixin/工具类文件 |
| `@param` | 参数，按签名顺序 | 每个非显然参数 |
| `@return` | 返回值语义 | 非 void 且不显然时 |
| `@throws` | 受检异常或明确的运行时异常 | 有则必填 |
| `@note` | 使用注意 | 有坑时 |
| `@warning` | 危险/崩溃风险 | 有则必填 |
| `@see` | 关联类/方法 | 与原版对应的类必填 |
| `@since` | MC 版本 | 新公共 API 必填 |
| `@deprecated` | 废弃说明 + 替代方案 | 废弃即必填 |
| `@todo` | 待办，须带条件（"等 XX 可用后"） | 有则必填 |
| `@c` / `@p` / `@ref` | 代码字体 / 参数名 / 引用 | 内联引用统一用这三个 |
| `@code` … `@endcode` | 代码块，≤ 10 行 | 示例时 |
| `@li` | 列表项 | 列表时 |

`@brief` 不写（D6）；`@author`/`@date`/`@version` 不用（D20）；`{@link}`/`{@code}` 禁用（D11）。

## 6. 四类模板（照抄）

### 6.0 文件头格式

`@file` 必须放在**摘要之后**：Doxygen 的 autobrief 取注释块首句，若首句是 `@file` 标签，文件摘要就空了。
只给注册入口、Mixin、工具类文件写文件头，普通单类文件可省。

```java
/**
 * 模组方块注册入口。
 *
 * @file ModBlocks.java
 */
package com.ggscaletest.ModItems;
```

### 6.1 注册入口类

```java
/**
 * 模组方块注册入口。
 *
 * 所有方块在本类静态字段中注册，类加载即完成注册；因此 @c onInitialize() 里
 * 只需引用一次 @ref SPEAKER，就能保证"方块先于物品"的注册顺序。
 *
 * @see net.minecraft.world.level.block.Blocks
 * @since 26.3
 */
public final class ModBlocks {
```

### 6.2 注册表字段

```java
    /**
     * 烦人音箱方块。
     *
     * 注册表 @c BuiltInRegistries.BLOCK，ID @c annoying-speaker:annoying_speaker；
     * 与物品共用同一 ID（原版 @c BlockItemIds 的惯例）。
     *
     * @see net.minecraft.world.level.block.Blocks
     * @since 26.3
     */
    public static final Block SPEAKER = ...;
```

### 6.3 方法（必须写侧别）

```java
    /**
     * 让玩家手持使用一次，播放随机音高的音符声。
     *
     * 仅在服务端执行实际播放；客户端返回 @c SUCCESS 只为立即摆臂。
     * 右键"方块"不会走到这里（放置优先，见 @c BlockItem#useOn），只有右键空气才触发。
     *
     * @param level  目标世界，不能为 null
     * @param player 使用者；声音以该玩家为声源
     * @param hand   触发的手，用于取冷却组
     * @return 固定 @c SUCCESS
     * @note 附带 10 tick 冷却；同一 tick 内重复调用无效果。
     * @see net.minecraft.world.item.BlockItem#useOn
     * @since 26.3
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
```

### 6.4 Mixin

```java
/**
 * 在 @c MinecraftServer#loadLevel 头部注入，用于在首个世界加载前初始化本模组服务端状态。
 *
 * @note 目标方法签名为 @c loadLevel()V；26.3 中它是 private，若上游改名，本类必须同步，
 *       否则注入会静默失效（游戏不崩但逻辑不跑）。
 * @see net.minecraft.server.MinecraftServer
 * @since 26.3
 */
@Mixin(MinecraftServer.class)
public class ExampleMixin {

    /**
     * 注入点 @c HEAD：在任何世界加载前执行，不取消原方法。
     *
     * @param info 回调信息，本注入不使用
     */
    @Inject(at = @At("HEAD"), method = "loadLevel")
    private void init(CallbackInfo info) {
```

## 7. 自检（提交前跑一遍）

```powershell
$src = 'src'

# 1) 没有类级文档注释的 .java（应只剩 package-info / 空文件）
Get-ChildItem -Recurse $src -Filter *.java | Where-Object {
  -not (Select-String -Path $_.FullName -Pattern '^\s*/\*\*' -Quiet)
} | ForEach-Object { $_.FullName }

# 2) 注释行超过 120 列
Select-String -Path (Get-ChildItem -Recurse $src -Filter *.java).FullName -Pattern '^\s*(\*|/\*\*)' |
  Where-Object { $_.Line.Length -gt 120 } | ForEach-Object { "$($_.Path):$($_.LineNumber)" }

# 3) 违禁写法
Select-String -Path (Get-ChildItem -Recurse $src -Filter *.java).FullName `
  -Pattern '\{@link|\{@code|@brief|@author|@date|@version|^\s*///' |
  ForEach-Object { "$($_.Path):$($_.LineNumber): $($_.Line.Trim())" }

# 4) 旧类名残留（D16）
Select-String -Path (Get-ChildItem -Recurse $src -Filter *.java).FullName `
  -Pattern 'ResourceLocation|\bGuiGraphics\b|\bServerWorld\b|\bClientWorld\b' |
  ForEach-Object { "$($_.Path):$($_.LineNumber): $($_.Line.Trim())" }
```

人工复核两项：
- 每个 `@param` 名字都在签名里（D8）；
- 每个 public 方法的注释都写了侧别/时机（D12）。

## 8. 附录：最小 Doxyfile

放到仓库根，`doxygen Doxyfile` 生成到 `docs/doxygen/`（不要提交产物）。

```ini
OUTPUT_DIRECTORY     = docs/doxygen
INPUT                = src/main/java src/client/java
FILE_PATTERNS        = *.java
RECURSIVE            = YES
EXCLUDE              = build .gradle run docs
JAVADOC_AUTOBRIEF    = YES
EXTRACT_ALL          = YES
EXTRACT_PRIVATE      = NO
EXTRACT_STATIC       = YES
SOURCE_BROWSER       = YES
GENERATE_LATEX       = NO
QUIET                = YES
```

要点：
- `JAVADOC_AUTOBRIEF = YES` 让首行自动成为摘要（对应 D6），所以不要手写 `@brief`。
- 想让 Mixin 注入方法出现在文档里，`EXTRACT_PRIVATE` 需为 `YES`；默认关，因为私有方法噪音大。
- 若 Doxygen 版本报未知选项 `EXTRACT_STATIC`，删掉该行即可（不影响其他规则）。

## 9. 规则本身的维护

- 本文件是**规则**，不进 `05-findings.md`；新增/修改规则时在 `90-update-log.md` 记一行，并给出编号。
- 规则编号不复用、不重排；废弃规则保留编号并标 `（已废弃，被 Dxx 取代）`。
- 规则冲突时：`11`（本文件）优先于 `10-code-recipes.md` 里的注释示例；代码行为以 `R-xxx` 配方为准。
