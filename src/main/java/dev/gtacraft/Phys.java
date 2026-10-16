package dev.gtacraft;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Физика гранат и гильз. Каждое тело — это item_display с 3D-моделью; позицию, отскоки, трение
 * и вращение считает сервер каждый тик (без отдельного типа сущности и рендерера).
 */
public final class Phys {
	public enum Kind {
		FRAG("grenade_world", 1.0, 60, 0.25),
		SMOKE("smoke_world", 1.0, 40, 0.25),
		MOLOTOV("molotov_world", 1.0, 120, 0.22),
		CASING("casing_world", 1.2, 36, 0.05);

		final String model;
		final double scale;
		final int life;
		final double half;

		Kind(String model, double scale, int life, double half) {
			this.model = model;
			this.scale = scale;
			this.life = life;
			this.half = half;
		}
	}

	private static final String TAG = "gta_phys";
	private static int counter;

	private static final class Body {
		Kind kind;
		ServerLevel level;
		Entity disp;
		ServerPlayer owner;
		double x, y, z, vx, vy, vz;
		float rx, ry, sx, sy;
		int age;
		boolean ground;
		boolean hitSound;
		double sx0, sy0, sz0;
		int bounces;
		String pendTag;
		int waited;
	}

	private static final class Zone {
		Kind kind;
		ServerLevel level;
		ServerPlayer owner;
		double x, y, z;
		int left;
	}

	private static final List<Body> BODIES = new ArrayList<>();
	private static final List<Zone> ZONES = new ArrayList<>();

	// ------------------------------------------------------------------ запуск
	public static boolean throwGrenade(ServerLevel level, ServerPlayer p, Kind kind, boolean soft) {
		Vec3 eye = p.getEyePosition();
		Vec3 look = p.getLookAngle();
		Vec3 pos = eye.add(look.scale(0.7)).add(0, -0.25, 0);
		double speed = soft ? 0.42 : 1.05;
		Body b = create(level, kind, pos);
		if (b == null) {
			GtaCraft.LOG.warn("Phys throw: create failed for {} at {}", kind, pos);
			return false;
		}
		GtaCraft.LOG.info("Phys throw {} from player ({}, {}, {}) spawn=({}, {}, {})", kind, String.format(Locale.ROOT, "%.1f", p.getX()),
			String.format(Locale.ROOT, "%.1f", p.getY()), String.format(Locale.ROOT, "%.1f", p.getZ()), String.format(Locale.ROOT, "%.1f", pos.x),
			String.format(Locale.ROOT, "%.1f", pos.y), String.format(Locale.ROOT, "%.1f", pos.z));
		b.owner = p;
		b.vx = look.x * speed;
		b.vy = look.y * speed + (soft ? 0.05 : 0.14);
		b.vz = look.z * speed;
		b.sx = 25f + level.random.nextFloat() * 20f;
		b.sy = (level.random.nextFloat() - 0.5f) * 30f;
		level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.8f, 0.7f);
		return true;
	}

	/** Для проверки физики без игрока: тело стартует с заданной скоростью. */
	public static boolean spawnTest(ServerLevel level, Kind kind, Vec3 pos, Vec3 vel) {
		Body b = create(level, kind, pos);
		if (b == null) {
			GtaCraft.LOG.warn("Phys test: create failed for {} at {}", kind, pos);
			return false;
		}
		b.vx = vel.x;
		b.vy = vel.y;
		b.vz = vel.z;
		b.sx = 30f;
		GtaCraft.LOG.info("Phys test {} at ({}, {}, {}) v=({}, {}, {})", kind, pos.x, pos.y, pos.z, vel.x, vel.y, vel.z);
		return true;
	}

	public static void ejectCasing(ServerLevel level, ServerPlayer p) {
		if (BODIES.size() > 70) {
			return;
		}
		Vec3 look = p.getLookAngle();
		double yaw = Math.toRadians(p.getYRot());
		Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw)); // справа от взгляда
		Vec3 pos = p.getEyePosition().add(look.scale(0.55)).add(right.scale(0.28)).add(0, -0.22, 0);
		Body b = create(level, Kind.CASING, pos);
		if (b == null) {
			return;
		}
		b.owner = p;
		double j = 0.05;
		b.vx = right.x * (0.14 + level.random.nextDouble() * 0.08) + (level.random.nextDouble() - 0.5) * j;
		b.vy = 0.13 + level.random.nextDouble() * 0.06;
		b.vz = right.z * (0.14 + level.random.nextDouble() * 0.08) + (level.random.nextDouble() - 0.5) * j;
		b.sx = 40f + level.random.nextFloat() * 60f;
		b.sy = (level.random.nextFloat() - 0.5f) * 80f;
	}

	/**
	 * Команда summon может выполниться чуть позже (если мы внутри другой команды), поэтому сущность
	 * ищется по уникальному тегу уже в тике: тело создаётся сразу, а модель привязывается позже.
	 */
	private static Body create(ServerLevel level, Kind kind, Vec3 pos) {
		MinecraftServer server = level.getServer();
		String pend = "gta_pn_" + (counter++);
		String cmd = String.format(Locale.ROOT,
			"summon minecraft:item_display %.3f %.3f %.3f {Tags:[\"%s\",\"%s\"],item:{id:\"minecraft:stick\",count:1,"
				+ "components:{\"minecraft:item_model\":\"gtacraft:%s\"}},item_display:\"none\",teleport_duration:1,"
				+ "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],scale:[%.2ff,%.2ff,%.2ff],translation:[0f,0f,0f]}}",
			pos.x, pos.y, pos.z, TAG, pend, kind.model, kind.scale, kind.scale, kind.scale);
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(level), cmd);
		Body b = new Body();
		b.kind = kind;
		b.level = level;
		b.pendTag = pend;
		b.x = pos.x;
		b.y = pos.y;
		b.z = pos.z;
		b.sx0 = pos.x;
		b.sy0 = pos.y;
		b.sz0 = pos.z;
		BODIES.add(b);
		return b;
	}

	private static boolean resolve(Body b) {
		List<Entity> found = b.level.getEntitiesOfClass(Entity.class, new AABB(b.x, b.y, b.z, b.x, b.y, b.z).inflate(3.0),
			e -> e.getTags().contains(b.pendTag));
		if (found.isEmpty()) {
			return false;
		}
		b.disp = found.get(0);
		b.disp.removeTag(b.pendTag);
		return true;
	}

	// ------------------------------------------------------------------ тик
	public static void tick(MinecraftServer server) {
		Iterator<Body> it = BODIES.iterator();
		while (it.hasNext()) {
			Body b = it.next();
			if (b.disp == null) {
				if (!resolve(b) && ++b.waited > 10) {
					GtaCraft.LOG.warn("Phys: model entity for {} never appeared", b.kind);
					it.remove();
				}
				continue;
			}
			if (b.disp.isRemoved()) {
				it.remove();
				continue;
			}
			boolean done;
			try {
				done = step(b);
			} catch (RuntimeException ex) {
				GtaCraft.LOG.error("Phys step failed for " + b.kind, ex);
				done = true;
			}
			if (b.kind != Kind.CASING && b.age % 20 == 0) {
				GtaCraft.LOG.info("Phys {} age={} pos=({}, {}, {}) v=({}, {}, {})", b.kind, b.age, String.format(Locale.ROOT, "%.1f", b.x),
					String.format(Locale.ROOT, "%.1f", b.y), String.format(Locale.ROOT, "%.1f", b.z), String.format(Locale.ROOT, "%.2f", b.vx),
					String.format(Locale.ROOT, "%.2f", b.vy), String.format(Locale.ROOT, "%.2f", b.vz));
			}
			if (done) {
				b.disp.discard();
				it.remove();
			}
		}
		Iterator<Zone> zi = ZONES.iterator();
		while (zi.hasNext()) {
			Zone z = zi.next();
			if (--z.left <= 0 || tickZone(z)) {
				zi.remove();
			}
		}
		if (server.getTickCount() % 200 == 0) {
			sweepOrphans(server);
		}
	}

	/** Убирает «осиротевшие» тела, оставшиеся после перезапуска сервера. */
	private static void sweepOrphans(MinecraftServer server) {
		Set<UUID> live = new HashSet<>();
		for (Body b : BODIES) {
			if (b.disp != null) {
				live.add(b.disp.getUUID());
			}
		}
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (p.level() instanceof ServerLevel level) {
				for (Entity e : level.getEntitiesOfClass(Entity.class, p.getBoundingBox().inflate(80), x -> x.getTags().contains(TAG))) {
					if (!live.contains(e.getUUID())) {
						e.discard();
					}
				}
			}
		}
	}

	/** Возвращает true, если тело нужно удалить. */
	private static boolean step(Body b) {
		b.age++;
		boolean casing = b.kind == Kind.CASING;
		b.vy -= casing ? 0.065 : 0.05;
		b.vx *= 0.995;
		b.vz *= 0.995;
		double h = b.kind.half;
		double impact = 0;
		b.ground = false;

		// ось Y
		double ny = b.y + b.vy;
		if (free(b, b.x, ny, b.z, h)) {
			b.y = ny;
		} else {
			if (b.vy < 0) {
				b.ground = true;
			}
			impact = Math.max(impact, Math.abs(b.vy));
			b.vy = Math.abs(b.vy) < 0.07 ? 0 : -b.vy * (casing ? 0.4 : 0.48);
			b.vx *= 0.74;
			b.vz *= 0.74;
			b.sx *= 0.6;
			b.sy *= 0.6;
		}
		// ось X
		double nx = b.x + b.vx;
		if (free(b, nx, b.y, b.z, h)) {
			b.x = nx;
		} else {
			impact = Math.max(impact, Math.abs(b.vx));
			b.vx = -b.vx * 0.45;
			b.sy = -b.sy;
		}
		// ось Z
		double nz = b.z + b.vz;
		if (free(b, b.x, b.y, nz, h)) {
			b.z = nz;
		} else {
			impact = Math.max(impact, Math.abs(b.vz));
			b.vz = -b.vz * 0.45;
			b.sy = -b.sy;
		}

		double hs = Math.hypot(b.vx, b.vz);
		if (b.ground) {
			// качение: вращение пропорционально скорости
			b.sx = (float) (hs * 120);
			if (hs < 0.012) {
				b.vx = 0;
				b.vz = 0;
			}
		}
		b.rx += b.sx;
		b.ry += b.sy;
		b.sy *= 0.97;
		b.disp.snapTo(b.x, b.y, b.z, b.ry, b.rx);

		if (impact > 0.1) {
			b.bounces++;
		}
		if (impact > 0.1 && !b.hitSound) {
			b.hitSound = casing;
			b.level.playSound(null, b.x, b.y, b.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, casing ? 0.05f : 0.2f, casing ? 2.4f : 1.8f);
		}

		switch (b.kind) {
			case CASING:
				return b.age >= b.kind.life;
			case FRAG:
				if (b.age >= b.kind.life - 20 && b.age % 5 == 0) {
					b.level.sendParticles(ParticleTypes.SMOKE, b.x, b.y + 0.15, b.z, 1, 0.02, 0.02, 0.02, 0.01);
				}
				if (b.age >= b.kind.life) {
					explode(b);
					return true;
				}
				return false;
			case SMOKE:
				b.level.sendParticles(ParticleTypes.CLOUD, b.x, b.y + 0.15, b.z, 1, 0.02, 0.02, 0.02, 0.01);
				if (b.age >= b.kind.life) {
					logEnd(b, "smoke");
					Zone z = new Zone();
					z.kind = Kind.SMOKE;
					z.level = b.level;
					z.owner = b.owner;
					z.x = b.x;
					z.y = b.y;
					z.z = b.z;
					z.left = 240;
					ZONES.add(z);
					b.level.playSound(null, b.x, b.y, b.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.2f, 0.6f);
					Peds.scare(b.level, new Vec3(b.x, b.y, b.z), 10);
					return true;
				}
				return false;
			case MOLOTOV:
				if (impact > 0.18 || b.age >= b.kind.life) {
					Zone z = new Zone();
					z.kind = Kind.MOLOTOV;
					z.level = b.level;
					z.owner = b.owner;
					z.x = b.x;
					z.y = b.y;
					z.z = b.z;
					z.left = 150;
					ZONES.add(z);
					b.level.playSound(null, b.x, b.y, b.z, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
					b.level.playSound(null, b.x, b.y, b.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 0.8f);
					Peds.scare(b.level, new Vec3(b.x, b.y, b.z), 16);
					return true;
				}
				return false;
		}
		return false;
	}

	private static boolean free(Body b, double x, double y, double z, double h) {
		return b.level.noCollision(new AABB(x - h, y - h, z - h, x + h, y + h, z + h));
	}

	private static void logEnd(Body b, String what) {
		GtaCraft.LOG.info("Phys {} {} age={} at ({}, {}, {}) from ({}, {}, {}) bounces={}", b.kind, what, b.age,
			String.format(Locale.ROOT, "%.1f", b.x), String.format(Locale.ROOT, "%.1f", b.y), String.format(Locale.ROOT, "%.1f", b.z),
			String.format(Locale.ROOT, "%.1f", b.sx0), String.format(Locale.ROOT, "%.1f", b.sy0), String.format(Locale.ROOT, "%.1f", b.sz0), b.bounces);
	}

	private static void explode(Body b) {
		logEnd(b, "detonated");
		ServerLevel l = b.level;
		l.explode(b.owner, b.x, b.y + 0.2, b.z, 2.8f, Level.ExplosionInteraction.NONE);
		l.sendParticles(ParticleTypes.EXPLOSION_EMITTER, b.x, b.y + 0.3, b.z, 1, 0, 0, 0, 0);
		l.sendParticles(ParticleTypes.LARGE_SMOKE, b.x, b.y + 0.5, b.z, 18, 0.5, 0.5, 0.5, 0.05);
		Peds.scare(l, new Vec3(b.x, b.y, b.z), 30);
		// осколки: урон по живым в радиусе, ослабевает с расстоянием
		for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(b.x, b.y, b.z, b.x, b.y, b.z).inflate(6),
			x -> x.isAlive() && x != b.owner && !(x instanceof net.minecraft.world.entity.decoration.ArmorStand))) {
			double d = e.position().add(0, e.getBbHeight() / 2, 0).distanceTo(new Vec3(b.x, b.y, b.z));
			if (d < 6) {
				float dmg = (float) (14.0 * (1.0 - d / 6.0));
				if (dmg > 0.5f && b.owner != null) {
					e.hurtServer(l, l.damageSources().playerAttack(b.owner), dmg);
				}
			}
		}
	}

	/** true, если зона закончилась. */
	private static boolean tickZone(Zone z) {
		ServerLevel l = z.level;
		if (z.kind == Kind.SMOKE) {
			if (z.left % 2 == 0) {
				l.sendParticles(ParticleTypes.LARGE_SMOKE, z.x, z.y + 1.4, z.z, 6, 1.7, 1.1, 1.7, 0.005);
				l.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, z.x, z.y + 0.6, z.z, 1, 1.2, 0.2, 1.2, 0.01);
			}
		} else if (z.kind == Kind.MOLOTOV) {
			if (z.left % 2 == 0) {
				l.sendParticles(ParticleTypes.FLAME, z.x, z.y + 0.2, z.z, 7, 1.5, 0.15, 1.5, 0.02);
				l.sendParticles(ParticleTypes.SMOKE, z.x, z.y + 0.9, z.z, 2, 1.2, 0.3, 1.2, 0.01);
			}
			if (z.left % 10 == 0) {
				for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, new AABB(z.x, z.y, z.z, z.x, z.y, z.z).inflate(3.2, 1.5, 3.2),
					x -> x.isAlive() && x != z.owner && !(x instanceof net.minecraft.world.entity.decoration.ArmorStand))) {
					e.setRemainingFireTicks(100);
					if (z.owner != null) {
						e.hurtServer(l, l.damageSources().playerAttack(z.owner), 2.0f);
					}
				}
			}
		}
		return false;
	}
}
