package dev.gtacraft;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/** Деньги игроков. Хранятся в файле gtacraft_players.json рядом с миром. */
public final class PlayerStats {
	private static final Map<UUID, Integer> MONEY = new HashMap<>();
	private static final Gson GSON = new Gson();
	private static Path file;
	private static boolean dirty;

	public static void load(MinecraftServer server) {
		file = server.getWorldPath(LevelResource.ROOT).resolve("gtacraft_players.json");
		MONEY.clear();
		try {
			if (Files.exists(file)) {
				Map<String, Integer> raw = GSON.fromJson(Files.readString(file), new TypeToken<Map<String, Integer>>() {}.getType());
				if (raw != null) {
					raw.forEach((k, v) -> MONEY.put(UUID.fromString(k), v));
				}
			}
		} catch (Exception e) {
			GtaCraft.LOG.warn("Cannot read player stats", e);
		}
	}

	public static void save() {
		if (file == null || !dirty) {
			return;
		}
		try {
			Map<String, Integer> raw = new HashMap<>();
			MONEY.forEach((k, v) -> raw.put(k.toString(), v));
			Files.writeString(file, GSON.toJson(raw));
			dirty = false;
		} catch (Exception e) {
			GtaCraft.LOG.warn("Cannot save player stats", e);
		}
	}

	public static int money(ServerPlayer p) {
		return MONEY.getOrDefault(p.getUUID(), 500);
	}

	public static void add(ServerPlayer p, int amount) {
		MONEY.put(p.getUUID(), Math.max(0, money(p) + amount));
		dirty = true;
	}
}
