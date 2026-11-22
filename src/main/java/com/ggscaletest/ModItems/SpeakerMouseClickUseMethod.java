/**
 * 音响的键鼠操作规则：潜行右键放置、普通右键打开界面。
 *
 * @file SpeakerMouseClickUseMethod.java
 */
package com.ggscaletest.ModItems;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * 音响的键鼠操作规则：潜行右键放置、普通右键打开界面、ESC 退出。
 *
 * 两端（客户端预测与服务端）的调用顺序相同：先方块交互，再物品放置。
 * 因此这里用两条互相让路的规则实现分工：
 *
 * - 按住 Shift（潜行）+ 右键：两端都会跳过方块交互（服务端 @c suppressUsingBlock），
 *   于是只剩物品放置，由 @ref useOn 走到 @c BlockItem#place；
 * - 不按 Shift + 右键已放置的音响：方块交互先执行，@ref openSpeakerUi 返回 @c SUCCESS
 *   消耗这次交互，物品不再放置，界面在客户端打开；
 * - 不按 Shift + 右键其它方块：方块交互无结果，物品侧返回 @c PASS，什么都不放。
 *
 * @see net.minecraft.world.item.BlockItem#useOn
 * @see net.minecraft.world.level.block.state.BlockBehaviour#useWithoutItem
 * @since 26.3
 */
public final class SpeakerMouseClickUseMethod {
    /**
     * 客户端"打开界面"的回调，默认空实现。
     *
     * 专用服务端永远不会触发客户端分支，因此空实现是安全的；客户端在
     * @c ClientModInitializer 里通过 @ref setClientUiOpener 注入真正的实现。
     *
     * @since 26.3
     */
    private static volatile Consumer<BlockPos> clientUiOpener = pos -> {
    };

    private SpeakerMouseClickUseMethod() {
    }

    /**
     * 注入打开界面的客户端实现。
     *
     * @param opener 接收音响坐标的回调，通常实现为 @c Minecraft.getInstance().gui.setScreen(...)
     *               （26.3 的屏幕栈挂在 @c Minecraft#gui 上，@c Minecraft 本身已没有 @c setScreen）
     * @note 只在客户端初始化时调用一次；不要传入会捕获生命周期对象的重量级 lambda。
     * @since 26.3
     */
    public static void setClientUiOpener(Consumer<BlockPos> opener) {
        clientUiOpener = opener;
    }

    /**
     * 物品侧：决定右键点到方块时是否放置。
     *
     * @param item    音响的方块物品
     * @param context 使用上下文
     * @return 潜行时返回放置结果；否则返回 @c PASS，把机会让给方块交互
     * @note 不潜行必须返回 @c PASS。若在这里返回 @c FAIL，原版会把这次右键当成失败，
     *       方块交互（打开界面）就再也不会执行。
     * @see net.minecraft.world.item.context.UseOnContext#isSecondaryUseActive
     * @since 26.3
     */
    public static InteractionResult useOn(BlockItem item, UseOnContext context) {
        if (!context.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        return item.place(new BlockPlaceContext(context));
    }

    /**
     * 物品侧：右键空气（没有方块目标）时打开同一个界面。
     *
     * 这条路径只会在"右键没打到方块"时触发，此时不存在放置的可能，所以不看潜行状态。
     * 界面按"手持"模式打开（没有坐标）。
     *
     * @param level  所在世界
     * @param player 使用者
     * @param hand   触发的手，仅用于保持与 @c Item#use 的签名一致
     * @return 固定 @c SUCCESS，让客户端立即摆臂
     * @see #openSpeakerUi(Level, BlockPos, Player)
     * @since 26.3
     */
    public static InteractionResult use(Level level, Player player, InteractionHand hand) {
        return openSpeakerUi(level, null, player);
    }

    /**
     * 打开音响界面，方块侧与手持侧共用这一个入口。
     *
     * @param level  所在世界
     * @param pos    音响坐标；@c null 表示"手持音响"，界面按无坐标模式显示
     * @param player 交互玩家
     * @return 固定 @c SUCCESS，消耗本次交互，避免继续走到物品放置
     * @note 目前只做客户端 @c setScreen，服务端不落任何状态；接入真实容器时应改为
     *       @c ServerPlayer#openMenu 并注册 @c MenuType，再由 @c MenuScreens 绑定界面。
     * @since 26.3
     */
    public static InteractionResult openSpeakerUi(Level level, @Nullable BlockPos pos, Player player) {
        if (level.isClientSide()) {
            clientUiOpener.accept(pos == null ? null : pos.immutable());
        }
        return InteractionResult.SUCCESS;
    }
}
