package dev.gtacraft.client;

import dev.gtacraft.GtaCraft;
import dev.gtacraft.HudPayload;
import dev.gtacraft.KickPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

public class GtaCraftClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(HudPayload.TYPE, (payload, context) -> GtaHud.state = payload);
		ClientPlayNetworking.registerGlobalReceiver(KickPayload.TYPE, (payload, context) -> Recoil.kick(payload.pitch(), payload.yaw()));
		ClientTickEvents.END_CLIENT_TICK.register(Recoil::tick);
		HudElementRegistry.attachElementAfter(VanillaHudElements.SUBTITLES, Identifier.fromNamespaceAndPath(GtaCraft.MODID, "gta_hud"), GtaHud::render);
	}
}
