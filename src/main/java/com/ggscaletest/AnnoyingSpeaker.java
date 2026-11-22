package com.ggscaletest;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ggscaletest.ModItems.ModBlocks;
import com.ggscaletest.ModItems.ModItems;

public class AnnoyingSpeaker implements ModInitializer {
	public static final String MOD_ID = "annoying-speaker";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");
		// 闲的蛋痛, 在这里插个眼
		LOGGER.info("The Mod [Annoying Speaker] is loaded successfully!");
		ModBlocks.initializer();
		ModItems.initializer();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
