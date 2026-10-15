package dev.gtacraft;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Основа задания: цель-маркер, таймер, награда, уборка за собой. */
public abstract class Mission {
	public final ServerPlayer p;
	public final ServerLevel level;
	public final String title;
	public String objective = "";
	public BlockPos target;
	public int ticksLeft = -1;
	public boolean finished;
	protected final List<Entity> spawned = new ArrayList<>();

	protected Mission(ServerPlayer p, String title) {
		this.p = p;
		this.level = (ServerLevel) p.level();
		this.title = title;
	}

	/** Вызывается каждый тик, пока задание идёт. */
	public abstract void tick();

	public void onKill(LivingEntity victim) {}

	/** true, если нажатие по блоку обработано заданием. */
	public boolean onUseBlock(BlockPos pos) {
		return false;
	}

	public void cleanup() {
		for (Entity e : spawned) {
			if (e != null && e.isAlive()) {
				e.discard();
			}
		}
		spawned.clear();
	}

	protected void succeed(int reward, String text) {
		if (finished) {
			return;
		}
		finished = true;
		PlayerStats.add(p, reward);
		p.displayClientMessage(Component.literal("Задание выполнено: " + title + ". " + text + " +$" + reward), false);
		p.displayClientMessage(Component.literal("Награда +$" + reward), true);
	}

	protected void fail(String reason) {
		if (finished) {
			return;
		}
		finished = true;
		p.displayClientMessage(Component.literal("Задание провалено: " + title + ". " + reason), false);
	}

	protected double dist2d(BlockPos a) {
		return Math.hypot(p.getX() - (a.getX() + 0.5), p.getZ() - (a.getZ() + 0.5));
	}

	protected boolean inCar() {
		return p.getVehicle() != null && CarManager.isCarEntity(p.getVehicle());
	}

	protected <T extends Mob> T spawnMob(EntityType<T> type, BlockPos pos, String name, boolean glow) {
		T m = type.create(level, EntitySpawnReason.EVENT);
		if (m == null) {
			return null;
		}
		m.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
		if (name != null) {
			m.setCustomName(Component.literal(name));
			m.setCustomNameVisible(true);
		}
		m.setPersistenceRequired();
		if (glow) {
			m.setGlowingTag(true);
		}
		level.addFreshEntity(m);
		spawned.add(m);
		return m;
	}

	/** Строка цели для HUD: текст + расстояние и стрелка направления. */
	public String hudObjective() {
		if (target == null) {
			return objective;
		}
		double dx = target.getX() + 0.5 - p.getX(), dz = target.getZ() + 0.5 - p.getZ();
		double bearing = Math.toDegrees(Math.atan2(-dx, dz)); // 0 = юг, как yaw в Minecraft
		double rel = ((bearing - p.getYRot()) % 360 + 540) % 360 - 180; // -180..180, 0 = прямо
		String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
		int idx = (int) Math.round(rel / 45.0);
		idx = ((idx % 8) + 8) % 8;
		// rel > 0: цель правее (yaw растёт по часовой)
		return objective + "  " + arrows[idx] + " " + Math.round(Math.hypot(dx, dz)) + " м";
	}
}
