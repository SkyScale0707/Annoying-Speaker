package com.ggscaletest.client;

import com.ggscaletest.ModItems.SpeakerMouseClickUseMethod;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

/**
 * 客户端初始化入口。
 *
 * 这里只做一件事：把"打开音响界面"的实现注入到通用侧的操作规则里。
 * 通用侧（@c src/main）拿不到 @c net.minecraft.client 的类型，所以用回调隔开。
 *
 * @see com.ggscaletest.ModItems.SpeakerMouseClickUseMethod
 * @see com.ggscaletest.client.SpeakerScreen
 * @since 26.3
 */
public class AnnoyingSpeakerClient implements ClientModInitializer {
	/**
	 * 注入界面打开方式：命中方块坐标后直接切屏。
	 *
	 * @note lambda 里才取 @c Minecraft.getInstance()，避免初始化早期实例尚未建立时取值。
	 * @since 26.3
	 */
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as
		// rendering.
		SpeakerMouseClickUseMethod.setClientUiOpener(
				pos -> Minecraft.getInstance().gui.setScreen(new SpeakerScreen(pos)));
	}
}
