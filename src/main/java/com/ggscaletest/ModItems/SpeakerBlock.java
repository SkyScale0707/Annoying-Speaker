/**
 * Annoying-Speaker方块类：水平朝向 + 贴合模型的碰撞箱。
 *
 * @file SpeakerBlock.java
 */
package com.ggscaletest.ModItems;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * annoying-speaker: annoying-speaker, 一个带水平朝向、碰撞箱贴合模型的方块。
 *
 * 继承 @c HorizontalDirectionalBlock 即可白拿 @c rotate 与 @c mirror 的实现，
 * 结构（jigsaw / 命令旋转）旋转时朝向会跟着走。
 * 形状只在"朝北"时手写一次，其余三向交给 @c Shapes.rotateHorizontal，保证与
 * blockstate 的 @c y 旋转使用同一套旋转约定（两者不同步是最常见的穿模原因）。
 *
 * @see net.minecraft.world.level.block.HorizontalDirectionalBlock
 * @see net.minecraft.world.phys.shapes.Shapes#rotateHorizontal
 * @since 26.3
 */
public class SpeakerBlock extends HorizontalDirectionalBlock {
    /**
     * 水平朝向属性，只允许 北/东/南/西。
     *
     * @see net.minecraft.world.level.block.state.properties.BlockStateProperties#HORIZONTAL_FACING
     * @since 26.3
     */
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    /**
     * 朝北（默认朝向）时的形状。
     *
     * 按模型里能挡住玩家的部件合并：主体 + 两个关节 + 提手横梁 + 侧面装饰板。
     * 坐标单位是 1/16 方块，与模型 JSON 的 @c from / @c to 一一对应；改模型必须同改这里。
     *
     * @note 装饰板有 4 块、厚度都是 1，这里合并成一块 x 1~2 的板；若要逐块精确，
     *       继续 @c Shapes.or 追加即可，代价是合并后的形状更碎、碰撞查询更慢。
     * @note 相关模型文件具体位置 @c
     *       resources/assets/annoying-speaker/models/block/annoying_speaker.json。
     * @since 26.3
     */
    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(2.0, 0.0, 3.0, 13.0, 11.0, 14.0),
            Block.box(6.0, 11.0, 3.0, 9.0, 13.0, 5.0),
            Block.box(6.0, 11.0, 12.0, 9.0, 13.0, 14.0),
            Block.box(7.0, 13.0, 4.0, 8.0, 15.0, 13.0),
            Block.box(1.0, 1.0, 4.0, 2.0, 10.0, 13.0));

    /**
     * 四个水平朝向各自的形状，按 @c FACING 直接取值。
     *
     * @since 26.3
     */
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(SHAPE_NORTH);

    /**
     * 构造方块，并把默认状态设为"朝北"。
     *
     * @param properties 方块属性，由注册处传入
     * @note 必须调用父类构造后再 @c registerDefaultState，且 @c createBlockStateDefinition
     *       里要加 @c FACING，否则默认状态会因属性不在状态定义中而抛异常。
     * @since 26.3
     */
    public SpeakerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /**
     * 放置时决定朝向：正面朝向玩家。
     *
     * @param context 放置上下文，含玩家视线方向
     * @return 带朝向的方块状态
     * @note 只在服务端与客户端预测时分别调用一次，纯函数、无副作用。
     * @see net.minecraft.world.level.block.AbstractFurnaceBlock#getStateForPlacement
     * @since 26.3
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /**
     * 返回当前朝向对应的形状。
     *
     * 碰撞箱与选中框都走这里（@c BlockBehaviour#getCollisionShape 默认委托给 @c getShape），
     * 因此只覆写这一个方法即可同时生效。
     *
     * @param state   当前方块状态，其中 @c FACING 一定是四个水平朝向之一
     * @param level   方块所在世界的只读视图
     * @param pos     方块坐标
     * @param context 碰撞上下文（本方块不区分实体）
     * @return 该朝向的形状
     * @since 26.3
     */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    /**
     * 把 @c FACING 登记进方块状态定义。
     *
     * @param builder 状态定义构建器
     * @note 漏掉这一步会在 @c registerDefaultState 时抛"属性不属于该方块"的异常。
     * @since 26.3
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
