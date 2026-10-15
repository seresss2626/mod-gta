package dev.gtacraft;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
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
		PayloadTypeRegistry.playS2C().register(HudPayload.TYPE, HudPayload.CODEC);
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			PlayerStats.load(server);
			City.load(server);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> PlayerStats.save());
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayer sp) {
				Wanted.clear(sp);
				MissionManager.onPlayerDeath(sp);
				PlayerStats.add(sp, -(PlayerStats.money(sp) / 10));
			} else if (source.getEntity() instanceof ServerPlayer killer) {
				if (entity.getTags().contains(Peds.TAG)) {
					Wanted.add(killer, 1);
				} else if (entity.getTags().contains(Wanted.COP_TAG)) {
					Wanted.add(killer, 2);
				}
				MissionManager.onKill(killer, entity);
			}
		});
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!level.isClientSide() && player instanceof ServerPlayer sp && MissionManager.onUseBlock(sp, hit.getBlockPos())) {
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		});
		CommandRegistrationCallback.EVENT.register(GtaCommands::register);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			CarManager.tick(server);
			CityBuilder.tick(server);
			Wanted.tick(server);
			Radio.tick(server);
			MissionManager.tick(server);
			Hud.tick(server);
			if (server.getTickCount() % 600 == 0) {
				PlayerStats.save();
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Kit.giveOnce(handler.getPlayer()));
		UseEntityCallback.EVENT.register(CarManager::onUseEntity);
		LOG.info("GTA Craft loaded");
	}
}
