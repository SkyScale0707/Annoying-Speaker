/**
 * 音响界面（占位版）：打开即可，ESC 或"完成"按钮退出。
 *
 * @file SpeakerScreen.java
 */
package com.ggscaletest.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * 音响的界面占位实现。
 *
 * 目前只显示标题与副标题，内容留待后续设计。两个入口共用本类：
 * 右键已放置的音响（有坐标）与手持音响右键空气（无坐标）。退出方式有两条，都是原版自带：
 * @c ESC（@c Screen#shouldCloseOnEsc 默认 true）与"完成"按钮（调用 @c onClose）。
 *
 * @note 现在由客户端直接 @c setScreen，服务端不参与；接入真实容器时改成
 *       @c ServerPlayer#openMenu + 注册 @c MenuType，再用 @c MenuScreens.register 绑定本类，
 *       这样多人游戏才能同步界面里的数据。
 * @see net.minecraft.client.gui.screens.Screen
 * @since 26.3
 */
public class SpeakerScreen extends Screen {
    /**
     * 界面标题，语言键由 lang 文件提供。
     *
     * @since 26.3
     */
    private static final Component TITLE = Component.translatable("screen.annoying-speaker.speaker");

    /**
     * 被打开的音响坐标；@c null 表示手持音响打开，界面无坐标可显示。
     *
     * @since 26.3
     */
    private final @Nullable BlockPos pos;

    /**
     * 构造界面。
     *
     * @param pos 音响坐标；@c null 表示手持音响打开，界面按无坐标模式显示
     * @since 26.3
     */
    public SpeakerScreen(@Nullable BlockPos pos) {
        super(TITLE);
        this.pos = pos;
    }

    /**
     * 初始化控件：一个居中的"完成"按钮。
     *
     * @note 每次窗口尺寸变化都会重新调用，控件不要存成需要手动释放的对象。
     * @since 26.3
     */
    @Override
    protected void init() {
        this.addRenderableWidget(
            Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(this.width / 2 - 50, this.height / 2 + 24, 100, 20)
                .build());
    }

    /**
     * 绘制界面内容（26.3 的 extract 式绘制）。
     *
     * @param graphics    绘制接口
     * @param mouseX      鼠标 x
     * @param mouseY      鼠标 y
     * @param partialTick 帧插值
     * @note 背景变暗与模糊由框架调用 @c extractBackground 完成，这里只画文字与控件。
     * @since 26.3
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, TITLE, this.width / 2, this.height / 2 - 44, 0xFFFFFFFF);
        graphics.centeredText(
            this.font,
            this.pos == null
                ? Component.translatable("screen.annoying-speaker.speaker.held")
                : Component.literal("x=" + this.pos.getX() + ", y=" + this.pos.getY() + ", z=" + this.pos.getZ()),
            this.width / 2,
            this.height / 2 - 24,
            0xFFA0A0A0);
    }
}
