package dev.gtacraft;

import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Машина = невидимая маленькая стойка для брони (физика и посадка) + item_display с моделью багги.
 * Управление читается из пакета ввода игрока: W/S газ и тормоз, A/D руль, пробел ручник, Shift выход.
 */
public final class CarManager {
	private static final String TAG = "gta_car";
	private static final java.util.Map<java.util.UUID, Double> SPEED = new java.util.HashMap<>();

	/** Сколько блоков занимает одна единица модели * 16 (масштаб отображения модели). */
	private static final double SCALE = 3.0;
	/** Смещение модели вверх, чтобы колёса стояли на земле (подбирается по месту посадки стойки). */
	private static final double LIFT = 0.5125;

	public static void spawn(ServerLevel level, double x, double y, double z, float yaw) {
		MinecraftServer server = level.getServer();
		String cmd = String.format(Locale.ROOT,
			"summon minecraft:armor_stand %.3f %.3f %.3f {Small:1b,Invisible:1b,NoGravity:1b,Invulnerable:1b,Silent:1b,"
				+ "Rotation:[%.1ff,0.0f],Tags:[\"%s\"],Passengers:[{id:\"minecraft:item_display\","
				+ "item:{id:\"gtacraft:buggy_body\",count:1},item_display:\"none\",Rotation:[%.1ff,0.0f],"
				+ "transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
				+ "scale:[%.2ff,%.2ff,%.2ff],translation:[0f,%.4ff,0f]}}]}",
			x, y, z, yaw, TAG, yaw, SCALE, SCALE, SCALE, LIFT);
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(level).withSuppressedOutput(), cmd);
	}

	public static void spawnInFront(ServerLevel level, ServerPlayer p) {
		double ang = Math.toRadians(p.getYRot());
		spawn(level, p.getX() - Math.sin(ang) * 4, p.getY(), p.getZ() + Math.cos(ang) * 4, p.getYRot());
	}

	private static boolean isCar(Entity e) {
		return e instanceof ArmorStand && e.getTags().contains(TAG);
	}

	public static void enterNearest(ServerLevel level, ServerPlayer p) {
		List<ArmorStand> list = level.getEntitiesOfClass(ArmorStand.class, p.getBoundingBox().inflate(6), CarManager::isCar);
		ArmorStand best = null;
		double bd = 1e9;
		for (ArmorStand a : list) {
			double d = a.distanceToSqr(p);
			if (d < bd) {
				bd = d;
				best = a;
			}
		}
		if (best != null) {
			p.startRiding(best, true, true);
			p.displayClientMessage(Component.literal("W/S - газ, A/D - руль, Пробел - тормоз, Shift - выйти"), true);
		} else {
			p.displayClientMessage(Component.literal("Рядом нет машины. Нажмите ключом по земле, чтобы поставить багги."), true);
		}
	}

	public static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hit) {
		if (isCar(entity)) {
			if (!level.isClientSide() && player instanceof ServerPlayer sp) {
				sp.startRiding(entity, true, true);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	public static void tick(MinecraftServer server) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			Entity v = p.getVehicle();
			if (!isCar(v)) {
				continue;
			}
			drive((ServerLevel) p.level(), p, (ArmorStand) v);
		}
	}

	private static void drive(ServerLevel level, ServerPlayer p, ArmorStand car) {
		Input in = p.getLastClientInput();
		double v = SPEED.getOrDefault(car.getUUID(), 0.0);
		if (in.forward()) {
			v += 0.03;
		} else if (in.backward()) {
			v -= v > 0.02 ? 0.05 : 0.018;
		} else {
			v *= 0.97;
		}
		if (in.jump()) {
			v *= 0.86;
		}
		v = Math.max(-0.25, Math.min(0.72, v));
		if (Math.abs(v) < 0.004 && !in.forward() && !in.backward()) {
			v = 0;
		}

		float yaw = car.getYRot();
		float steer = (in.right() ? 1 : 0) - (in.left() ? 1 : 0);
		yaw += steer * 4.2f * (float) Math.signum(v) * (float) Math.min(1.0, Math.abs(v) / 0.2);

		double ang = Math.toRadians(yaw);
		double fx = -Math.sin(ang), fz = Math.cos(ang);
		double x = car.getX(), y = car.getY(), z = car.getZ();
		double nx = x + fx * v, nz = z + fz * v;

		if (v != 0) {
			if (free(level, car, nx, y, nz)) {
				x = nx;
				z = nz;
			} else if (free(level, car, nx, y + 1.0, nz)) {
				x = nx;
				z = nz;
				y += 1.0;
			} else {
				v = -v * 0.25;
			}
		}
		if (free(level, car, x, y - 0.4, z)) {
			y -= 0.4;
		} else if (free(level, car, x, y - 0.1, z)) {
			y -= 0.1;
		}

		car.setPos(x, y, z);
		car.setYRot(yaw);
		for (Entity pass : car.getPassengers()) {
			if (!(pass instanceof Player)) {
				pass.setYRot(yaw);
				pass.setXRot(0);
			}
		}
		SPEED.put(car.getUUID(), v);

		if (Math.abs(v) > 0.22) {
			AABB hit = new AABB(x - 1.0, y, z - 1.0, x + 1.0, y + 1.5, z + 1.0).move(fx * 0.8, 0, fz * 0.8);
			for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, hit,
				e -> e != p && e.isAlive() && !(e instanceof ArmorStand))) {
				e.hurtServer(level, level.damageSources().playerAttack(p), (float) (Math.abs(v) * 28));
				e.push(fx * 0.9, 0.35, fz * 0.9);
			}
		}
		if (level.getGameTime() % 4 == 0) {
			p.displayClientMessage(Component.literal(String.format(Locale.ROOT, "%d км/ч", Math.round(Math.abs(v) * 20 * 3.6))), true);
		}
	}

	private static boolean free(ServerLevel level, Entity self, double x, double y, double z) {
		return level.noCollision(self, new AABB(x - 0.9, y + 0.05, z - 0.9, x + 0.9, y + 1.1, z + 0.9));
	}
}
