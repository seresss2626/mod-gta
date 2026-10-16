package dev.gtacraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Прохожие и NPC заданий. Это «манекены» (Mannequin) со случайными скинами игроков:
 * 24 сгенерированных скина в ресурсах мода и, если в мире есть файл gtacraft_skins.txt
 * (по нику в строке), настоящие скины этих игроков. Ходьбу и испуг считает этот класс.
 */
public final class Peds {
	public static final String TAG = "gta_ped";
	public static final String NPC_TAG = "gta_npc";
	private static final String NEW_TAG = "gta_new";
	public static final int SKINS = 24;

	private static final Random R = new Random();
	private static final List<String> NICKS = new ArrayList<>();
	private static final Map<UUID, State> STATE = new HashMap<>();
	private static boolean loggedSpawn;

	private static final class State {
		float yaw;
		int turnIn;
		int pause;
		int panic;
		Vec3 from = Vec3.ZERO;
		float lastHp = -1f;
	}

	public static void load(MinecraftServer server) {
		NICKS.clear();
		try {
			Path f = server.getWorldPath(LevelResource.ROOT).resolve("gtacraft_skins.txt");
			if (Files.exists(f)) {
				for (String line : Files.readAllLines(f)) {
					String s = line.trim();
					if (s.matches("[A-Za-z0-9_]{3,16}")) {
						NICKS.add(s);
					}
				}
			}
		} catch (Exception e) {
			GtaCraft.LOG.warn("Cannot read gtacraft_skins.txt", e);
		}
		GtaCraft.LOG.info("Ped skins: {} generated + {} nicknames", SKINS, NICKS.size());
	}

	public static void spawn(ServerLevel level, City city, int count) {
		for (int i = 0; i < count; i++) {
			BlockPos pos = city.sidewalk(R);
			spawnNpc(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, R.nextFloat() * 360f, TAG, null, false);
		}
	}

	/** Создаёт манекен со случайным скином. Возвращает null, если команда не сработала. */
	public static LivingEntity spawnNpc(ServerLevel level, double x, double y, double z, float yaw, String tag, String name, boolean glow) {
		String profile;
		if (!NICKS.isEmpty() && R.nextBoolean()) {
			profile = "\"" + NICKS.get(R.nextInt(NICKS.size())) + "\"";
		} else {
			profile = "{texture:\"gtacraft:entity/ped/p" + R.nextInt(SKINS) + "\"}";
		}
		StringBuilder nbt = new StringBuilder();
		nbt.append(String.format(Locale.ROOT, "{Rotation:[%.1ff,0.0f],Tags:[\"%s\",\"%s\"],profile:%s,hide_description:1b,Invulnerable:0b",
			yaw, tag, NEW_TAG, profile));
		if (name != null) {
			nbt.append(",CustomName:\"").append(name.replace("\"", "")).append("\",CustomNameVisible:1b");
		}
		if (glow) {
			nbt.append(",Glowing:1b");
		}
		nbt.append("}");
		MinecraftServer server = level.getServer();
		String cmd = String.format(Locale.ROOT, "summon minecraft:mannequin %.3f %.3f %.3f %s", x, y, z, nbt);
		final boolean log = !loggedSpawn;
		loggedSpawn = true;
		if (log) {
			GtaCraft.LOG.info("Ped summon: {}", cmd);
		}
		server.getCommands().performPrefixedCommand(
			server.createCommandSourceStack().withLevel(level).withCallback((ok, result) -> {
				if (log || !ok) {
					GtaCraft.LOG.info("Ped summon ok={} result={}", ok, result);
				}
			}), cmd);
		List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, new AABB(x - 1, y - 1, z - 1, x + 1, y + 2.5, z + 1),
			e -> e.getTags().contains(NEW_TAG));
		if (found.isEmpty()) {
			return null;
		}
		LivingEntity e = found.get(0);
		e.removeTag(NEW_TAG);
		return e;
	}

	/** Все вокруг в радиусе разбегаются (выстрел, взрыв). */
	public static void scare(ServerLevel level, Vec3 from, double radius) {
		AABB box = new AABB(from, from).inflate(radius);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> x.getTags().contains(TAG))) {
			State s = STATE.computeIfAbsent(e.getUUID(), k -> fresh());
			s.panic = 90;
			s.from = from;
		}
	}

	private static State fresh() {
		State s = new State();
		s.yaw = R.nextInt(4) * 90f;
		s.turnIn = 30 + R.nextInt(100);
		return s;
	}

	public static void tick(MinecraftServer server) {
		Set<UUID> seen = new HashSet<>();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (!(p.level() instanceof ServerLevel level)) {
				continue;
			}
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(40), x -> x.getTags().contains(TAG))) {
				if (e.isAlive() && seen.add(e.getUUID())) {
					step(level, e, p);
					if (seen.size() == 1 && server.getTickCount() % 100 == 0) {
						GtaCraft.LOG.info("Ped sample pos=({}, {}, {}) yaw={} peds_near={}", String.format(Locale.ROOT, "%.2f", e.getX()),
							String.format(Locale.ROOT, "%.2f", e.getY()), String.format(Locale.ROOT, "%.2f", e.getZ()), STATE.get(e.getUUID()).yaw, 0);
					}
				}
			}
		}
		if (server.getTickCount() % 1200 == 0) {
			STATE.keySet().retainAll(seen);
		}
	}

	private static void step(ServerLevel level, LivingEntity e, ServerPlayer near) {
		State s = STATE.computeIfAbsent(e.getUUID(), k -> fresh());
		float hp = e.getHealth();
		if (s.lastHp >= 0 && hp < s.lastHp - 0.01f) {
			s.panic = 120;
			s.from = near.position();
		}
		s.lastHp = hp;

		double speed;
		if (s.panic > 0) {
			s.panic--;
			Vec3 away = e.position().subtract(s.from);
			if (away.horizontalDistanceSqr() > 0.01) {
				s.yaw = (float) Math.toDegrees(Math.atan2(-away.x, away.z));
			}
			speed = 0.27;
		} else if (s.pause > 0) {
			s.pause--;
			speed = 0;
		} else {
			speed = 0.1;
			if (--s.turnIn <= 0) {
				s.turnIn = 40 + R.nextInt(140);
				int roll = R.nextInt(10);
				if (roll == 0) {
					s.pause = 30 + R.nextInt(80);
				} else if (roll < 4) {
					s.yaw += roll < 3 ? 90 : -90;
				}
			}
			City c = City.current;
			if (c != null) {
				double lo = -8, hi = c.n * City.CELL + 19;
				double rx = e.getX() - c.x0, rz = e.getZ() - c.z0;
				if (rx < lo || rx > hi || rz < lo || rz > hi) {
					Vec3 toC = new Vec3(c.x0 + c.n * City.CELL / 2.0 - e.getX(), 0, c.z0 + c.n * City.CELL / 2.0 - e.getZ());
					s.yaw = (float) Math.toDegrees(Math.atan2(-toC.x, toC.z));
				}
			}
		}

		double rad = Math.toRadians(s.yaw);
		double dx = -Math.sin(rad), dz = Math.cos(rad);
		Vec3 cur = e.getDeltaMovement();
		double vx = dx * speed, vz = dz * speed, vy = cur.y;
		if (speed > 0) {
			AABB ahead = e.getBoundingBox().move(dx * 0.5, 0, dz * 0.5);
			if (!level.noCollision(e, ahead)) {
				if (e.onGround() && level.noCollision(e, ahead.move(0, 1.05, 0))) {
					vy = 0.42; // запрыгнуть на бордюр
				} else {
					s.yaw += R.nextBoolean() ? 90 : -90;
					s.turnIn = 40 + R.nextInt(60);
					vx = vz = 0;
				}
			} else if (s.panic == 0 && e.onGround()) {
				BlockPos below = BlockPos.containing(e.getX() + dx * 1.2, e.getY() - 0.5, e.getZ() + dz * 1.2);
				if (level.getBlockState(below).isAir()) {
					s.yaw += 180;
					s.turnIn = 40;
					vx = vz = 0;
				}
			}
		}
		e.setDeltaMovement(vx, vy, vz);
		e.setYRot(s.yaw);
		e.setYHeadRot(s.yaw);
		e.setYBodyRot(s.yaw);
	}
}
