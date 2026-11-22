/**
 * 音响的方块物品：把右键语义委托给 @ref SpeakerMouseClickUseMethod。
 *
 * @file SpeakerItem.java
 */
package com.ggscaletest.ModItems;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 音响的方块物品。
 *
 * 只做一件事：把 @c useOn 交给 @ref SpeakerMouseClickUseMethod，实现"潜行才放置"。
 * 放置本身的细节（朝向、方块实体、物品消耗）仍然复用 @c BlockItem 的原版实现。
 *
 * @see com.ggscaletest.ModItems.SpeakerMouseClickUseMethod
 * @see net.minecraft.world.item.BlockItem
 * @since 26.3
 */
public class SpeakerItem extends BlockItem {
    /**
     * 构造物品。
     *
     * @param block      对应的方块，必须已注册
     * @param properties 物品属性，注册处会带上 ID 与语言键前缀
     * @since 26.3
     */
    public SpeakerItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    /**
     * 右键空气时的入口：打开与方块侧相同的界面。
     *
     * @param level  所在世界
     * @param player 使用者
     * @param hand   触发的手
     * @return 固定 @c SUCCESS
     * @note 只有"右键没打到方块"才会走到这里；打到方块时走 @ref useOn 与方块侧逻辑。
     * @since 26.3
     */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return SpeakerMouseClickUseMethod.use(level, player, hand);
    }

    /**
     * 右键点击方块时的入口。
     *
     * @param context 使用上下文，含玩家、手持物与命中结果
     * @return 潜行时是放置结果；否则 @c PASS
     * @note 这里不加任何自己的条件判断，规则全部集中在 @ref SpeakerMouseClickUseMethod，
     *       便于以后调整"怎么算放置、怎么算交互"。
     * @since 26.3
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        return SpeakerMouseClickUseMethod.useOn(this, context);
    }
}
