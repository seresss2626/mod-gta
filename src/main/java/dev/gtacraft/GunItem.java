package dev.gtacraft;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class GunItem extends Item {
	public enum Type {
		//            cooldown pellets damage spread range mag reload kick
		PISTOL(8, 1, 7.0f, 0.010, 80, 12, 40, 1.8f),
		SHOTGUN(22, 9, 3.0f, 0.075, 28, 6, 60, 5.5f),
		SMG(3, 1, 4.0f, 0.030, 64, 30, 50, 1.0f),
		SNIPER(30, 1, 28.0f, 0.0, 200, 5, 70, 7.0f),
		RPG(50, 0, 0f, 0.0, 120, 1, 80, 4.5f);

		final int cooldown, pellets, mag, reload;
		final float damage, kick;
		final double spread, range;

		Type(int cooldown, int pellets, float damage, double spread, double range, int mag, int reload, float kick) {
			this.cooldown = cooldown;
			this.pellets = pellets;
			this.damage = damage;
			this.spread = spread;
			this.range = range;
			this.mag = mag;
			this.reload = reload;
			this.kick = kick;
		}
	}

	// ---------------------------------------------------------------- магазины и перезарядка
	private static final Map<UUID, int[]> AMMO = new HashMap<>();

	private record Job(UUID id, int slot, Type type, int end, int start) {}

	private static final java.util.List<Job> JOBS = new java.util.ArrayList<>();

	static int ammo(ServerPlayer p, Type t) {
		int[] a = AMMO.computeIfAbsent(p.getUUID(), k -> fullMags());
		return a[t.ordinal()];
	}

	private static int[] fullMags() {
		int[] a = new int[Type.values().length];
		for (Type t : Type.values()) {
			a[t.ordinal()] = t.mag;
		}
		return a;
	}

	private static void setAmmo(ServerPlayer p, Type t, int v) {
		AMMO.computeIfAbsent(p.getUUID(), k -> fullMags())[t.ordinal()] = v;
	}

	public Type gunType() {
		return type;
	}

	public boolean isReloadVariant() {
		return reloading;
	}

	static Item gunItem(Type t) {
		return switch (t) {
			case PISTOL -> ModItems.PISTOL;
			case SHOTGUN -> ModItems.SHOTGUN;
			case SMG -> ModItems.SMG;
			case SNIPER -> ModItems.SNIPER;
			case RPG -> ModItems.RPG;
		};
	}

	/** Начинает перезарядку: оружие в руке заменяется «перезаряжаемой» версией с анимацией. */
	public static void startReload(ServerLevel sl, ServerPlayer p) {
		ItemStack held = p.getMainHandItem();
		if (!(held.getItem() instanceof GunItem g) || g.reloading) {
			return;
		}
		Type t = g.type;
		if (ammo(p, t) >= t.mag) {
			return;
		}
		int slot = p.getInventory().getSelectedSlot();
		ItemStack rs = new ItemStack(ModItems.reloadItem(t));
		p.getInventory().setItem(slot, rs);
		p.getCooldowns().addCooldown(rs, t.reload);
		int now = sl.getServer().getTickCount();
		JOBS.removeIf(j -> j.id.equals(p.getUUID()));
		JOBS.add(new Job(p.getUUID(), slot, t, now + t.reload, now));
		sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CROSSBOW_LOADING_START, SoundSource.PLAYERS, 1.0f, 1.1f);
	}

	public static void tickReloads(MinecraftServer server) {
		if (JOBS.isEmpty()) {
			return;
		}
		int now = server.getTickCount();
		Iterator<Job> it = JOBS.iterator();
		while (it.hasNext()) {
			Job j = it.next();
			ServerPlayer p = server.getPlayerList().getPlayer(j.id);
			if (p == null) {
				it.remove();
				continue;
			}
			if (now == j.start + j.type.reload / 2) {
				p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CROSSBOW_LOADING_MIDDLE, SoundSource.PLAYERS, 1.0f, 1.0f);
			}
			if (now >= j.end) {
				ItemStack cur = p.getInventory().getItem(j.slot);
				if (cur.getItem() == ModItems.reloadItem(j.type)) {
					p.getInventory().setItem(j.slot, new ItemStack(gunItem(j.type)));
				}
				setAmmo(p, j.type, j.type.mag);
				p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS, 1.0f, 1.0f);
				it.remove();
			}
		}
	}

	/** Строка для HUD: название и патроны, либо пусто. */
	public static String hudText(ServerPlayer p) {
		ItemStack st = p.getMainHandItem();
		if (st.getItem() instanceof GunItem g) {
			String name = st.getHoverName().getString();
			if (g.reloading) {
				return name.replace(" (перезарядка)", "").replace(" (reloading)", "") + "  \u21BB";
			}
			return name + "  " + ammo(p, g.type) + "/" + g.type.mag;
		}
		if (st.getItem() instanceof GrenadeItem) {
			return st.getHoverName().getString() + "  x" + st.getCount();
		}
		return "";
	}

	private final Type type;
	private final boolean reloading;

	public GunItem(Properties props, Type type, boolean reloading) {
		super(props);
		this.type = type;
		this.reloading = reloading;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
			return InteractionResult.SUCCESS;
		}
		if (reloading) {
			return InteractionResult.FAIL;
		}
		int mag = ammo(sp, type);
		if (player.isShiftKeyDown() || mag <= 0) {
			startReload(sl, sp);
			return InteractionResult.CONSUME;
		}
		setAmmo(sp, type, mag - 1);
		player.getCooldowns().addCooldown(player.getItemInHand(hand), type.cooldown);
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		playShotSound(sl, player);
		muzzleFlash(sl, player, eye, look);
		Peds.scare(sl, player.position(), 26);
		float yawKick = (sl.random.nextFloat() - 0.5f) * type.kick * 0.5f;
		ServerPlayNetworking.send(sp, new KickPayload(type.kick * (0.85f + sl.random.nextFloat() * 0.3f), yawKick));

		if (type == Type.RPG) {
			fireRocket(sl, player, eye, look);
		} else {
			Phys.ejectCasing(sl, sp);
			for (int i = 0; i < type.pellets; i++) {
				Vec3 d = look.add(
					(sl.random.nextDouble() - 0.5) * 2 * type.spread,
					(sl.random.nextDouble() - 0.5) * 2 * type.spread,
					(sl.random.nextDouble() - 0.5) * 2 * type.spread).normalize();
				shoot(sl, player, eye, d);
			}
		}
		if (mag - 1 <= 0) {
			startReload(sl, sp);
		}
		return InteractionResult.SUCCESS;
	}

	private void muzzleFlash(ServerLevel sl, Player p, Vec3 eye, Vec3 look) {
		double yaw = Math.toRadians(p.getYRot());
		Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
		Vec3 m = eye.add(look.scale(1.1)).add(right.scale(0.22)).add(0, -0.18, 0);
		sl.sendParticles(ParticleTypes.FLAME, m.x, m.y, m.z, type == Type.SHOTGUN ? 6 : 3, 0.03, 0.03, 0.03, 0.03);
		sl.sendParticles(ParticleTypes.SMOKE, m.x, m.y, m.z, 2, 0.04, 0.04, 0.04, 0.01);
		if (type == Type.SNIPER || type == Type.RPG) {
			sl.sendParticles(ParticleTypes.POOF, m.x, m.y, m.z, 4, 0.1, 0.1, 0.1, 0.02);
		}
	}

	private void playShotSound(ServerLevel sl, Player p) {
		switch (type) {
			case PISTOL -> sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.2f, 1.9f);
			case SMG -> sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0f, 1.5f);
			case SHOTGUN -> sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.5f, 1.8f);
			case SNIPER -> sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 2.0f, 0.8f);
			case RPG -> sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 2.0f, 0.6f);
		}
	}

	private record Hit(Vec3 pos, Entity entity) {}

	private Hit trace(ServerLevel sl, Player player, Vec3 eye, Vec3 dir, double range) {
		Vec3 end = eye.add(dir.scale(range));
		BlockHitResult bh = sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		double maxDist = bh.getType() == HitResult.Type.MISS ? range : bh.getLocation().distanceTo(eye);
		Vec3 clipEnd = eye.add(dir.scale(maxDist));
		Entity target = null;
		double best = maxDist;
		AABB area = player.getBoundingBox().expandTowards(dir.scale(maxDist)).inflate(1.0);
		for (Entity e : sl.getEntities(player, area,
			en -> en instanceof LivingEntity && en.isAlive() && !en.isSpectator() && !(en instanceof ArmorStand) && en != player.getVehicle())) {
			Optional<Vec3> hit = e.getBoundingBox().inflate(0.15).clip(eye, clipEnd);
			if (hit.isPresent()) {
				double dist = eye.distanceTo(hit.get());
				if (dist < best) {
					best = dist;
					target = e;
				}
			}
		}
		return new Hit(eye.add(dir.scale(best)), target);
	}

	private void trail(ServerLevel sl, Vec3 from, Vec3 to) {
		Vec3 delta = to.subtract(from);
		double len = delta.length();
		Vec3 step = delta.normalize().scale(1.2);
		Vec3 p = from.add(step.scale(1.5));
		for (double t = 1.8; t < len; t += 1.2) {
			sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0, 0, 0, 0);
			p = p.add(step);
		}
	}

	private void shoot(ServerLevel sl, Player player, Vec3 eye, Vec3 dir) {
		Hit h = trace(sl, player, eye, dir, type.range);
		trail(sl, eye, h.pos());
		if (h.entity() instanceof LivingEntity le) {
			le.hurtServer(sl, sl.damageSources().playerAttack(player), type.damage);
			sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR, h.pos().x, h.pos().y, h.pos().z, 3, 0.1, 0.1, 0.1, 0.1);
		} else {
			sl.sendParticles(ParticleTypes.SMOKE, h.pos().x, h.pos().y, h.pos().z, 4, 0.05, 0.05, 0.05, 0.01);
			BlockState st = sl.getBlockState(BlockPos.containing(h.pos().add(dir.scale(0.05))));
			if (!st.isAir()) {
				sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), h.pos().x, h.pos().y, h.pos().z, 8, 0.08, 0.08, 0.08, 0.05);
			}
		}
	}

	private void fireRocket(ServerLevel sl, Player player, Vec3 eye, Vec3 dir) {
		Hit h = trace(sl, player, eye, dir, type.range);
		Vec3 delta = h.pos().subtract(eye);
		Vec3 step = dir.scale(1.0);
		for (double t = 2; t < delta.length(); t += 1.0) {
			Vec3 q = eye.add(dir.scale(t));
			sl.sendParticles(ParticleTypes.FLAME, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0.0);
			sl.sendParticles(ParticleTypes.SMOKE, q.x, q.y, q.z, 1, 0.05, 0.05, 0.05, 0.0);
		}
		Vec3 at = h.pos().subtract(dir.scale(0.3));
		sl.explode(player, at.x, at.y, at.z, 3.5f, Level.ExplosionInteraction.TNT);
	}
}
