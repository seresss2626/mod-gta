package dev.gtacraft;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Отправляет клиенту деньги, звёзды розыска и цель задания. */
public final class Hud {
	public static void tick(MinecraftServer server) {
		if (server.getTickCount() % 5 != 0) {
			return;
		}
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			Mission m = MissionManager.get(p);
			String title = m == null ? "" : m.title;
			String obj = m == null ? "" : m.hudObjective();
			int secs = m == null || m.ticksLeft < 0 ? -1 : m.ticksLeft / 20;
			ServerPlayNetworking.send(p, new HudPayload(PlayerStats.money(p), Wanted.stars(p), Wanted.searching(p), title, obj, secs, Radio.stationName(p)));
		}
	}
}
