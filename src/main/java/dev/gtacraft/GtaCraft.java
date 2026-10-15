package dev.gtacraft;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GtaCraft implements ModInitializer {
	public static final String MODID = "gtacraft";
	public static final Logger LOG = LoggerFactory.getLogger(MODID);

	@Override
	public void onInitialize() {
		LOG.info("GTA Craft loaded");
	}
}
