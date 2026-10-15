package dev.gtacraft;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class MissionManager {
	public enum Type {
		ROBBERY("robbery", "Предотвратить ограбление"),
		BANK("bank", "Ограбление банка"),
		TAXI("taxi", "Такси"),
		DELIVERY("delivery", "Доставка груза"),
		HIT("hit", "Заказ"),
		THEFT("theft", "Угон машины"),
		RACE("race", "Уличная гонка");

		public final String id, title;

		Type(String id, String title) {
			this.id = id;
			this.title = title;
		}
	}

	private static final Map<UUID, Mission> ACTIVE = new HashMap<>();

	public static Mission get(ServerPlayer p) {
		return ACTIVE.get(p.getUUID());
	}

	public static void startRandom(ServerPlayer p) {
		Type[] t = Type.values();
		start(p, t[p.level().getRandom().nextInt(t.length)]);
	}

	public static void start(ServerPlayer p, Type type) {
		if (get(p) != null) {
			p.displayClientMessage(Component.literal("Сначала завершите текущее задание: " + get(p).title + ". Присядьте с телефоном, чтобы отменить."), false);
			return;
		}
		City c = City.current;
		if (c == null) {
			p.displayClientMessage(Component.literal("Сначала постройте город: /gta city"), false);
			return;
		}
		Mission m;
		switch (type) {
			case ROBBERY -> {
				if (c.shops.isEmpty()) {
					p.displayClientMessage(Component.literal("В городе нет магазинов. Постройте город побольше: /gta city 3"), false);
					return;
				}
				m = new Missions.Robbery(p, c.shops.get(p.level().getRandom().nextInt(c.shops.size())));
			}
			case BANK -> {
				if (c.bank == null) {
					p.displayClientMessage(Component.literal("В городе нет банка."), false);
					return;
				}
				m = new Missions.BankHeist(p, c.bank);
			}
			case TAXI -> m = new Missions.Taxi(p);
			case DELIVERY -> m = new Missions.Delivery(p);
			case HIT -> m = new Missions.Assassination(p);
			case THEFT -> m = new Missions.CarTheft(p);
			default -> m = new Missions.Race(p);
		}
		ACTIVE.put(p.getUUID(), m);
		p.displayClientMessage(Component.literal("Новое задание: " + m.title), false);
	}

	public static void cancel(ServerPlayer p) {
		Mission m = ACTIVE.remove(p.getUUID());
		if (m != null) {
			m.cleanup();
			p.displayClientMessage(Component.literal("Задание отменено."), true);
		}
	}

	public static void onKill(ServerPlayer killer, LivingEntity victim) {
		Mission m = get(killer);
		if (m != null) {
			m.onKill(victim);
		}
	}

	public static void onPlayerDeath(ServerPlayer p) {
		Mission m = ACTIVE.remove(p.getUUID());
		if (m != null) {
			m.fail("Вы погибли.");
			m.cleanup();
		}
	}

	public static boolean onUseBlock(ServerPlayer p, BlockPos pos) {
		Mission m = get(p);
		return m != null && m.onUseBlock(pos);
	}

	public static void tick(MinecraftServer server) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			Mission m = get(p);
			if (m == null) {
				continue;
			}
			m.tick();
			if (!m.finished && m.ticksLeft >= 0) {
				m.ticksLeft--;
				if (m.ticksLeft == 0) {
					m.fail("Время вышло.");
				}
			}
			if (!m.finished && m.target != null && p.level().getGameTime() % 10 == 0) {
				ServerLevel lv = (ServerLevel) p.level();
				for (int i = 0; i < 24; i++) {
					lv.sendParticles(p, ParticleTypes.END_ROD, true, true, m.target.getX() + 0.5, m.target.getY() + i * 2.0, m.target.getZ() + 0.5, 1, 0, 0, 0, 0);
				}
			}
			if (m.finished) {
				ACTIVE.remove(p.getUUID());
				m.cleanup();
			}
		}
	}
}
