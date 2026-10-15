package dev.gtacraft;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/** Уровень розыска 0-5. Полиция: скелеты в броне, со 3 звёзд добавляется спецназ. */
public final class Wanted {
	public static final String COP_TAG = "gta_cop";

	private static final class State {
		int stars;
		int calm;
		int spawnCd;
		boolean seen;
	}

	private static final Map<UUID, State> STATES = new HashMap<>();

	private static State st(ServerPlayer p) {
		return STATES.computeIfAbsent(p.getUUID(), k -> new State());
	}

	public static int stars(ServerPlayer p) {
		return st(p).stars;
	}

	public static boolean searching(ServerPlayer p) {
		State s = st(p);
		return s.stars > 0 && !s.seen;
	}

	public static void add(ServerPlayer p, int n) {
		State s = st(p);
		int old = s.stars;
		s.stars = Math.min(5, s.stars + n);
		s.calm = 0;
		if (s.stars > old) {
			p.displayClientMessage(Component.literal("Уровень розыска: " + "★".repeat(s.stars)), true);
		}
	}

	public static void clear(ServerPlayer p) {
		State s = st(p);
		boolean had = s.stars > 0;
		s.stars = 0;
		s.calm = 0;
		despawnAll((ServerLevel) p.level(), p, 0);
		if (had) {
			p.displayClientMessage(Component.literal("Розыск снят"), true);
		}
	}

	private static List<Mob> cops(ServerLevel level, ServerPlayer p, double r) {
		return level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(r), e -> e.getTags().contains(COP_TAG));
	}

	private static void despawnAll(ServerLevel level, ServerPlayer p, double minDist) {
		for (Mob m : cops(level, p, 160)) {
			if (m.distanceTo(p) >= minDist) {
				m.discard();
			}
		}
	}

	public static void tick(MinecraftServer server) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			State s = st(p);
			if (s.stars <= 0) {
				continue;
			}
			ServerLevel level = (ServerLevel) p.level();
			List<Mob> list = cops(level, p, 90);
			boolean seen = false;
			for (Mob m : list) {
				if (m.distanceTo(p) < 28 && m.hasLineOfSight(p)) {
					seen = true;
				}
				if (m.distanceTo(p) > 70) {
					m.discard();
				}
				if (m.getTarget() == null || !m.getTarget().isAlive()) {
					m.setTarget(p);
				}
			}
			s.seen = seen;
			if (seen) {
				s.calm = 0;
			} else {
				s.calm++;
				if (s.calm >= 20 * 25) {
					s.calm = 0;
					s.stars--;
					if (s.stars <= 0) {
						clear(p);
						continue;
					}
					p.displayClientMessage(Component.literal("Розыск снижен: " + "★".repeat(s.stars)), true);
				}
			}
			if (s.spawnCd > 0) {
				s.spawnCd--;
			}
			int want = s.stars * 2;
			if (list.size() < want && s.spawnCd <= 0) {
				spawnCop(level, p, s.stars);
				s.spawnCd = Math.max(20, 90 - s.stars * 12);
			}
		}
	}

	private static void spawnCop(ServerLevel level, ServerPlayer p, int stars) {
		for (int tries = 0; tries < 12; tries++) {
			double ang = level.random.nextDouble() * Math.PI * 2;
			double dist = 16 + level.random.nextDouble() * 12;
			int x = (int) Math.floor(p.getX() + Math.cos(ang) * dist);
			int z = (int) Math.floor(p.getZ() + Math.sin(ang) * dist);
			int y0 = (int) p.getY() + 3;
			for (int y = y0; y > p.getY() - 8; y--) {
				if (!level.getBlockState(new net.minecraft.core.BlockPos(x, y - 1, z)).isAir()
					&& level.getBlockState(new net.minecraft.core.BlockPos(x, y, z)).isAir()
					&& level.getBlockState(new net.minecraft.core.BlockPos(x, y + 1, z)).isAir()
					&& level.getBlockState(new net.minecraft.core.BlockPos(x, y - 1, z)).getFluidState().isEmpty()) {
					boolean swat = stars >= 3 && level.random.nextInt(3) == 0;
					EntityType<? extends Mob> type = swat ? EntityType.WITHER_SKELETON : EntityType.SKELETON;
					Mob cop = type.create(level, EntitySpawnReason.EVENT);
					if (cop == null) {
						return;
					}
					cop.snapTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360f, 0f);
					cop.setItemSlot(EquipmentSlot.HEAD, new ItemStack(swat ? Items.NETHERITE_HELMET : Items.IRON_HELMET));
					cop.setItemSlot(EquipmentSlot.CHEST, new ItemStack(swat ? Items.IRON_CHESTPLATE : Items.CHAINMAIL_CHESTPLATE));
					cop.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(swat ? Items.IRON_SWORD : Items.BOW));
					cop.setCustomName(Component.literal(swat ? "Спецназ" : "Полицейский"));
					cop.setPersistenceRequired();
					cop.addTag(COP_TAG);
					cop.setTarget(p);
					level.addFreshEntity(cop);
					return;
				}
			}
		}
	}
}
