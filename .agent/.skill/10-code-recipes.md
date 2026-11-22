# 10 · 可复制代码配方（Code Recipes）

> 每条配方都是**经源码逐行核对**的 26.3 + Fabric 写法，可直接粘贴。
> 与 `05-findings.md` 的区别：05 记"为什么/查到了什么"，这里记"照抄就能跑"。
> 注释写法以 `11-doxygen-rules.md`（D1–D22）为准；本文件的示例注释仅为演示 `@note`/`@see` 用法。

---

## R-001 · 方块类物品：可放置 + 可手持使用

**对应结论**：`05-findings.md` F-011　**适用**：`src/main`（common 侧，双端）

### 0. 先理解要复刻的三步
原版 `Items.registerItem`（`world/item/Items.java:2937-2944`）是 **private**，模组不能调用，只能复刻：

1. `Item.Properties#setId(ResourceKey<Item>)` —— 必填，否则 `itemIdOrThrow()` 抛 `Item id not set`
2. `BlockItem#registerBlocks(Item.BY_BLOCK, item)` —— 建立 方块 → 物品 映射
3. `Registry.register(BuiltInRegistries.ITEM, key, item)`

方块侧可以直接调 `Blocks.register(...)`：Fabric 的 access widener 已把它拓宽成 `public`
（`Blocks.java:5655`），它内部会做 `properties.setId(id)` 再 `Registry.register`。

### 0.1 最小版（"暂无特性"时先这样）

不需要自定义子类：方块传 `Block::new`，物品传 `BlockItem::new`，行为全部用原版默认。
要加行为时，再按 §1 / §3 换成 `SpeakerBlock` / `SpeakerItem`。

> ID 允许连字符：`Identifier.validPathChar` 含 `-`（`Identifier.java:262`），所以 `annoying-speaker:annoying-speaker` 合法。

### 1. `src/main/java/com/ggscaletest/ModBlocks/SpeakerBlock.java`
```java
package com.ggscaletest.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SpeakerBlock extends Block {
    // 与 Blockbench 模型的包围盒一致：x 1~13, y 0~15, z 4~14
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 4.0, 13.0, 15.0, 14.0);

    public SpeakerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    // 覆写 getShape 即可，碰撞箱默认就是 getShape（BlockBehaviour.java:321）
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
```

### 2. `src/main/java/com/ggscaletest/ModBlocks/ModBlocks.java`

```java
package com.ggscaletest.ModBlocks;

import com.ggscaletest.AnnoyingSpeaker;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
    private ModBlocks() {
    }

    /** 方块与物品共用同一个 Identifier（原版 BlockItemIds 的惯例） */
    public static final Identifier SPEAKER_ID = AnnoyingSpeaker.id("annoying_speaker");
    public static final ResourceKey<Block> SPEAKER_KEY = ResourceKey.create(Registries.BLOCK, SPEAKER_ID);

    public static final Block SPEAKER = Blocks.register(
        SPEAKER_KEY,
        SpeakerBlock::new,
        BlockBehaviour.Properties.of()
            .strength(1.5F)      // 硬度/抗爆，类似木头
            .sound(SoundType.WOOD)
            .noOcclusion()       // 非完整方块必须关遮蔽，否则相邻面被剔除、出现透洞
    );
}
```

> 不用 Fabric 拓宽时的手写等价物：
> `Registry.register(BuiltInRegistries.BLOCK, SPEAKER_KEY, new SpeakerBlock(BlockBehaviour.Properties.of().setId(SPEAKER_KEY).strength(1.5F).noOcclusion()));`

### 3. `src/main/java/com/ggscaletest/ModItems/SpeakerItem.java`

```java
package com.ggscaletest.ModItems;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class SpeakerItem extends BlockItem {
    public SpeakerItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    /**
     * 拿在手上的行为。
     * 注意分工：右键"方块"走 BlockItem#useOn（放置优先）；右键空气才走到这里。
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {                    // 声音只由服务端发，避免两端各响一次
            float pitch = 0.8F + level.getRandom().nextFloat() * 0.6F;
            level.playSound(null, player, SoundEvents.NOTE_BLOCK_PLING, SoundSource.PLAYERS, 1.0F, pitch);
            player.getCooldowns().addCooldown(stack, 10); // 0.5s 冷却，可选
        }
        return InteractionResult.SUCCESS;                // 客户端立即摆臂
    }
}
```

### 4. `src/main/java/com/ggscaletest/ModItems/ModItems.java`

```java
package com.ggscaletest.ModItems;

import com.ggscaletest.ModBlocks.ModBlocks;
import java.util.function.BiFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModItems {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModItems.class);

    public static final ResourceKey<Item> SPEAKER_ITEM_KEY =
        ResourceKey.create(Registries.ITEM, ModBlocks.SPEAKER_ID);

    public static final Item SPEAKER =
        registerBlockItem(SPEAKER_ITEM_KEY, ModBlocks.SPEAKER, SpeakerItem::new);

    /** 复刻 Items.registerItem（Items.java:2937）：setId → registerBlocks → Registry.register */
    private static BlockItem registerBlockItem(ResourceKey<Item> id, Block block,
                                               BiFunction<Block, Item.Properties, BlockItem> factory) {
        BlockItem item = factory.apply(block, new Item.Properties()
            .setId(id)                     // 必填
            .useBlockDescriptionPrefix()); // 语言键用 block.<ns>.<id>，与方块共用名字
        item.registerBlocks(Item.BY_BLOCK, item);                  // 方块 ↔ 物品
        return Registry.register(BuiltInRegistries.ITEM, id, item); // 进注册表
    }

    public static void initializer() {
        LOGGER.info("ModItems initializer called");
        LOGGER.info("registered {}", SPEAKER_ITEM_KEY.identifier());
    }
}
```

`AnnoyingSpeaker.onInitialize()` 里已有 `ModItems.initializer();`，调用它即可完成注册
（静态字段在类加载时注册；`initializer()` 只负责日志）。

### 5. 资源文件（26.3 路径，照抄）

```text
src/main/resources/
  assets/annoying-speaker/blockstates/annoying_speaker.json
  assets/annoying-speaker/items/annoying_speaker.json          ← 注意是 items/ 不是 models/item/
  assets/annoying-speaker/models/block/annoying_speaker.json   ← Blockbench 的 elements 放这里
  assets/annoying-speaker/textures/block/{mainbody,handles,decorations}.png
  assets/annoying-speaker/lang/{en_us,zh_cn}.json
  data/annoying-speaker/loot_table/blocks/annoying_speaker.json
```

`blockstates/annoying_speaker.json`
```json
{ "variants": { "": { "model": "annoying-speaker:block/annoying_speaker" } } }
```

`items/annoying_speaker.json`（新式物品模型定义，原版 1659 个这样的文件）
```json
{ "model": { "type": "minecraft:model", "model": "annoying-speaker:block/annoying_speaker" } }
```

`models/block/annoying_speaker.json`：把 `models_of_speaker\annoying speaker.json` 复制过来，只改 `textures`
（`format_version` / `credit` / `groups` 删掉；`elements` / `display` 原样保留）
```json
{
  "textures": {
    "0": "annoying-speaker:block/mainbody",
    "1": "annoying-speaker:block/handles",
    "2": "annoying-speaker:block/decorations",
    "particle": "annoying-speaker:block/mainbody"
  },
  "elements": [ /* 原样 */ ],
  "display":  { /* 原样 */ }
}
```

`data/annoying-speaker/loot_table/blocks/annoying_speaker.json`（不写就挖了不掉）
```json
{
  "type": "minecraft:block",
  "pools": [
    {
      "condition": { "type": "minecraft:survives_explosion" },
      "entries": [ { "type": "minecraft:item", "name": "annoying-speaker:annoying_speaker" } ],
      "rolls": 1
    }
  ],
  "random_sequence": "annoying-speaker:blocks/annoying_speaker"
}
```
> 26.3 的池用**单数** `condition` / `modifier`，不是 1.21 的 `conditions` 数组（`LootPool.java:37-38`）。

`lang/zh_cn.json`
```json
{ "block.annoying-speaker.annoying_speaker": "烦人音箱" }
```

### 6. 验收

1. `.\gradlew runClient` 进游戏。
2. `/give @s annoying-speaker:annoying_speaker` 拿到物品（创造栏里暂时不会出现）。
3. 右键方块 → 放置；右键空气 → 响一声。
4. 挖掉 → 掉落自身（靠 loot_table）。

### 7. 四个必踩的坑

| 现象 | 原因 | 修法 |
|---|---|---|
| 启动崩 `Item id not set` | 忘了 `Properties#setId` | 按上面 `registerBlockItem` 写 |
| 挖掉不掉落 / `Block#asItem` 异常 / 创造栏缺项 | 忘了 `registerBlocks(Item.BY_BLOCK, item)` | 补上 |
| 手持显示紫黑 missing model | 物品模型放进了 `models/item/` | 放 `assets/<ns>/items/<id>.json` |
| 方块面被剔除、能看到内部 | 忘了 `noOcclusion()` | 加上；并检查 `getShape` 包围盒 |

---

## R-002 · 方块朝向 + 贴合模型的碰撞箱

**对应结论**：`05-findings.md` F-016　**适用**：`src/main`（common 侧）

### 0. 选对基类

| 需求 | 基类 | 自带 |
|---|---|---|
| 只要水平四向（北东南西） | `HorizontalDirectionalBlock` | `FACING`、`rotate`、`mirror` |
| 还要上下（活塞、按钮那类） | `DirectionalBlock` | `FACING`（6 向）、`rotate`、`mirror` |

`rotate`/`mirror` 是结构旋转（`/place`、jigsaw）用的，自己写容易漏，直接继承最省事。

### 1. `src/main/java/<pkg>/SpeakerBlock.java`

```java
public class SpeakerBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    // 只写"朝北"一份，坐标单位 1/16 格，与模型 JSON 的 from/to 一一对应
    private static final VoxelShape SHAPE_NORTH = Shapes.or(
        Block.box(2.0, 0.0, 3.0, 13.0, 11.0, 14.0),   // 主体
        Block.box(6.0, 11.0, 3.0, 9.0, 13.0, 5.0),    // 关节
        Block.box(6.0, 11.0, 12.0, 9.0, 13.0, 14.0),  // 关节
        Block.box(7.0, 13.0, 4.0, 8.0, 15.0, 13.0),   // 提手横梁
        Block.box(1.0, 1.0, 4.0, 2.0, 10.0, 13.0)     // 侧装饰板
    );

    // 其余三向交给官方工具，旋转约定与 blockstate 的 y 完全一致
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(SHAPE_NORTH);

    public SpeakerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
```

注册处把工厂换成 `SpeakerBlock::new`：

```java
public static final Block ANNOYING_SPEAKER = Blocks.register(
    ANNOYING_SPEAKER_KEY,
    SpeakerBlock::new,
    BlockBehaviour.Properties.of().strength(1.5F).noOcclusion());
```

### 2. blockstate 必须给全四向

```json
{
  "variants": {
    "facing=north": { "model": "annoying-speaker:block/annoying-speaker" },
    "facing=east":  { "model": "annoying-speaker:block/annoying-speaker", "y": 90 },
    "facing=south": { "model": "annoying-speaker:block/annoying-speaker", "y": 180 },
    "facing=west":  { "model": "annoying-speaker:block/annoying-speaker", "y": 270 }
  }
}
```

`y` 与 `Shapes.rotateHorizontal` 用的是同一套约定（`OctahedralGroup.BLOCK_ROT_Y_90`），所以**不要**自己手算旋转，否则必穿模。

### 3. 逐向包围盒速查（本例）

| 朝向 | 形状包围盒（1/16 格） |
|---|---|
| north | x 1–13, y 0–15, z 3–14 |
| east | x 2–13, y 0–15, z 1–13 |
| south | x 3–15, y 0–15, z 2–13 |
| west | x 3–14, y 0–15, z 3–15 |

模型或包围盒改动后，用这张表反推 `Block.box` 是否还对齐。

### 4. 五个坑

| 现象 | 原因 | 修法 |
|---|---|---|
| 启动崩"属性不属于该方块" | `createBlockStateDefinition` 漏了 `FACING` | `builder.add(FACING)` |
| 选中框还是整格 | 只覆写 `getCollisionShape` | 覆写 `getShape`（碰撞与选中框都走它） |
| 转向后碰撞箱跟模型错位 | 手算旋转 | 用 `Shapes.rotateHorizontal` + blockstate `y` |
| 放下后永远朝北 | 没覆写 `getStateForPlacement` | 见 §1 |
| 结构旋转后朝向乱掉 | 没继承 `HorizontalDirectionalBlock` | 换基类，别自己写 `rotate` |

