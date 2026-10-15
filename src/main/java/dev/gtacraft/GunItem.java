package dev.gtacraft;

import java.util.Optional;
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
		//            cooldown pellets damage spread range
		PISTOL(8, 1, 7.0f, 0.010, 80),
		SHOTGUN(22, 9, 3.0f, 0.075, 28),
		SMG(3, 1, 4.0f, 0.030, 64),
		SNIPER(30, 1, 28.0f, 0.0, 200),
		RPG(50, 0, 0f, 0.0, 120);

		final int cooldown, pellets;
		final float damage;
		final double spread, range;

		Type(int cooldown, int pellets, float damage, double spread, double range) {
			this.cooldown = cooldown;
			this.pellets = pellets;
			this.damage = damage;
			this.spread = spread;
			this.range = range;
		}
	}

	private final Type type;

	public GunItem(Properties props, Type type) {
		super(props);
		this.type = type;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel sl)) {
			return InteractionResult.SUCCESS;
		}
		player.getCooldowns().addCooldown(player.getItemInHand(hand), type.cooldown);
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		playShotSound(sl, player);

		if (type == Type.RPG) {
			fireRocket(sl, player, eye, look);
			return InteractionResult.SUCCESS;
		}
		for (int i = 0; i < type.pellets; i++) {
			Vec3 d = look.add(
				(sl.random.nextDouble() - 0.5) * 2 * type.spread,
				(sl.random.nextDouble() - 0.5) * 2 * type.spread,
				(sl.random.nextDouble() - 0.5) * 2 * type.spread).normalize();
			shoot(sl, player, eye, d);
		}
		return InteractionResult.SUCCESS;
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
