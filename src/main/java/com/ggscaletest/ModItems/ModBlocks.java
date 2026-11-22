/**
 * 模组方块注册入口。
 * 按照 dsh 对源码的解压，方块类物品的注册需要先注册方块，再注册物品。
 *  这和1.21.1的注册顺序不一样，1.21.1是先注册物品再注册方块
 * @file ModBlocks.java
 */
package com.ggscaletest.ModItems;

import com.ggscaletest.AnnoyingSpeaker;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模组方块注册入口。
 * 
 * @see net.minecraft.world.level.block.Blocks
 * @since 26.3
 */
public class ModBlocks {
    /**
     * annoying-speaker 方块类物品的注册 ID，完整形式为
     * 
     * @c annoying-speaker:annoying-speaker。
     *
     *    AnnoyingSpeaker.id("annoying-speaker")这个方法直接调用了
     * @c Identifier.fromNamespaceAndPath，返回一个 @c Identifier 实例。由此避免了重复写
     * @c new Identifier("annoying-speaker",
     *    "annoying-speaker")。
     *    我的天啊dsh大人，这个写法简直惊为天人。在见到这个写法前，我根本没意识到@c
     *    AnnoyingSpeaker.id()这个方法的存在，总是手动写值，导致报错漫天飞。
     *    毫不犹豫就接受了d老师的写法
     * 
     * @since 26.3
     */
    public static final Identifier ANNOYING_SPEAKER_ID = AnnoyingSpeaker.id("annoying-speaker");

    /**
     * 方块的注册表键。
     *
     * @since 26.3
     */
    public static final ResourceKey<Block> ANNOYING_SPEAKER_KEY = ResourceKey.create(Registries.BLOCK,
            ANNOYING_SPEAKER_ID);

    /**
     * block: annoying-speaker
     *
     * 暂无任何特性：直接使用 @c Block 的默认实现（完整立方体碰撞箱、默认方块状态、无额外逻辑）。
     * 唯一必须的例外是 @c noOcclusion()：模型不是满方块，不关遮蔽会让相邻方块的面被错误剔除。
     * 注册表 @c BuiltInRegistries.BLOCK；ID 由 @c Blocks.register 自动写入，这里不要再调 @c setId。
     *
     * @see net.minecraft.world.level.block.Blocks
     * @since 26.3
     */
    public static final Block ANNOYING_SPEAKER = Blocks.register(
            ANNOYING_SPEAKER_KEY,
            Block::new,
            BlockBehaviour.Properties.of()
                    .strength(1.5F)
                    .noOcclusion());

    /**
     * ModBlocks.class 的初始化器，并打印注册结果。
     * 
     * @since 26.3
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ModBlocks.class);

    public static void initializer() {
        LOGGER.info("Class ModBlocks is initialized successfully!");
        LOGGER.info("Registered block {}", ANNOYING_SPEAKER_KEY.identifier());
    }

}
