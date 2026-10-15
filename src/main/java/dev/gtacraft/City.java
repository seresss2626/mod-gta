package dev.gtacraft;

import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Данные построенного города: нужны заданиям, чтобы знать, где банк, магазины и перекрёстки. */
public final class City {
	public static final int CELL = 32;

	public int x0, z0, gy, n;
	/** [doorX, doorZ, insideX, insideZ] для каждого магазина */
	public List<int[]> shops = new ArrayList<>();
	/** [doorX, doorZ, vaultX, vaultY, vaultZ], null если банка нет */
	public int[] bank;

	public static City current;
	private static final Gson GSON = new Gson();

	public static void load(MinecraftServer server) {
		current = null;
		try {
			Path f = server.getWorldPath(LevelResource.ROOT).resolve("gtacraft_city.json");
			if (Files.exists(f)) {
				current = GSON.fromJson(Files.readString(f), City.class);
			}
		} catch (Exception e) {
			GtaCraft.LOG.warn("Cannot read city data", e);
		}
	}

	public void save(MinecraftServer server) {
		try {
			Files.writeString(server.getWorldPath(LevelResource.ROOT).resolve("gtacraft_city.json"), GSON.toJson(this));
		} catch (Exception e) {
			GtaCraft.LOG.warn("Cannot save city data", e);
		}
	}

	/** Центр перекрёстка (i, j), на уровне пола (над дорогой). */
	public BlockPos intersection(int i, int j) {
		return new BlockPos(x0 + i * CELL + 5, gy + 1, z0 + j * CELL + 5);
	}

	/** Случайная точка на тротуаре. */
	public BlockPos sidewalk(Random r) {
		int span = n * CELL + 11;
		int band = r.nextInt(n + 1) * CELL;
		int along = r.nextInt(span);
		boolean alongZ = r.nextBoolean();
		int off = r.nextBoolean() ? 1 : 9;
		if (alongZ) {
			return new BlockPos(x0 + band + off, gy + 1, z0 + along);
		}
		return new BlockPos(x0 + along, gy + 1, z0 + band + off);
	}

	/** Случайная точка на проезжей части. */
	public BlockPos road(Random r) {
		int span = n * CELL + 11;
		int band = r.nextInt(n + 1) * CELL;
		int along = r.nextInt(span);
		int off = 3 + r.nextInt(5);
		if (r.nextBoolean()) {
			return new BlockPos(x0 + band + off, gy + 1, z0 + along);
		}
		return new BlockPos(x0 + along, gy + 1, z0 + band + off);
	}
}
