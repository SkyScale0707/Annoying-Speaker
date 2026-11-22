/**
 * 模组物品注册入口。
 * 按照 dsh 对源码的解压，方块类物品的注册需要先注册方块，再注册物品。
 *  这和1.21.1的注册顺序不一样，1.21.1是先注册物品再注册方块
 * @file ModItems.java
 */
package com.ggscaletest.ModItems;

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

/**
 * 模组物品注册入口。
 * 
 * @see net.minecraft.world.item.Items
 * @see com.ggscaletest.ModItems.ModBlocks
 * @since 26.3
 */
public class ModItems {
    /**
     * 本模组物品的注册表键，ID 与方块相同：@c annoying-speaker:annoying-speaker。
     * 
     * @c ModBlocks.ANNOYING_SPEAKER_ID
     *    该注册见
     * @c ModBlocks#ANNOYING_SPEAKER_ITEM_KEY
     * @since 26.3
     */
    public static final ResourceKey<Item> ANNOYING_SPEAKER_ITEM_KEY = ResourceKey.create(Registries.ITEM,
            ModBlocks.ANNOYING_SPEAKER_ID);

    /**
     * item: annoying-speaker
     *
     * 暂无任何特性：普通 @c BlockItem，右键方块即放置，右键空气无行为。
     * 注册表 @c BuiltInRegistries.ITEM。
     *
     * @see net.minecraft.world.item.BlockItem
     * @since 26.3
     */
    public static final Item ANNOYING_SPEAKER = registerBlockItem(
            ANNOYING_SPEAKER_ITEM_KEY,
            ModBlocks.ANNOYING_SPEAKER,
            BlockItem::new);

    /**
     * @override @c Items.registerItem 的三步注册，返回已写入注册表的方块物品。
     * 
     * @param id      物品注册表键，同时决定 @c Item.Properties 的 ID 与语言键前缀
     * @param block   对应的方块，必须已注册
     * @param factory 物品工厂，通常传 @c BlockItem::new
     * @return 已注册的物品实例
     * @note 必须调用 @c registerBlocks，否则 @c Block#asItem()、破坏掉落与创造栏条目都会异常。
     * @see net.minecraft.world.item.Items
     * @since 26.3
     */
    private static BlockItem registerBlockItem(ResourceKey<Item> id, Block block,
            BiFunction<Block, Item.Properties, BlockItem> factory) {
        BlockItem item = factory.apply(block, new Item.Properties()
                .setId(id)
                .useBlockDescriptionPrefix());
        item.registerBlocks(Item.BY_BLOCK, item);
        return Registry.register(BuiltInRegistries.ITEM, id, item);
    }

    /**
     * ModItems.class 的初始化器，并打印注册结果。
     *
     * @since 26.3
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ModItems.class);

    public static void initializer() {
        LOGGER.info("Class ModItems is initialized successfully!");
        LOGGER.info("Registered item {}", ANNOYING_SPEAKER_ITEM_KEY.identifier());
    }

}
