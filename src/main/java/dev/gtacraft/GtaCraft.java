package dev.gtacraft;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GtaCraft implements ModInitializer {
	public static final String MODID = "gtacraft";
	public static final Logger LOG = LoggerFactory.getLogger(MODID);

	@Override
	public void onInitialize() {
		ModItems.init();
		CommandRegistrationCallback.EVENT.register(GtaCommands::register);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			CarManager.tick(server);
			CityBuilder.tick(server);
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Kit.giveOnce(handler.getPlayer()));
		UseEntityCallback.EVENT.register(CarManager::onUseEntity);
		LOG.info("GTA Craft loaded");
	}
}
