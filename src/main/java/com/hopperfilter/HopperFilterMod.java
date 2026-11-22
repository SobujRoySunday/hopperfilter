package com.hopperfilter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HopperFilterMod implements ModInitializer {
	public static final String MOD_ID = "hopperfilter";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		FilterConfig.get();
		UseBlockCallback.EVENT.register(HopperInteraction::onUseBlock);
		LOGGER.info("Hopper Filter Setup loaded");
	}
}
