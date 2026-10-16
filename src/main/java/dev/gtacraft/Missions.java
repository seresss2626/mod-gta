package dev.gtacraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;

/** Реализации заданий в духе GTA. */
public final class Missions {
	private static final Random R = new Random();

	/** Точка на тротуаре на расстоянии [min, max] от игрока. */
	static BlockPos sidewalkNear(ServerPlayer p, int min, int max) {
		City c = City.current;
		BlockPos best = null;
		for (int i = 0; i < 60; i++) {
			BlockPos b = c.sidewalk(R);
			double d = Math.hypot(b.getX() - p.getX(), b.getZ() - p.getZ());
			if (d >= min && d <= max) {
				return b;
			}
			if (best == null || Math.abs(d - (min + max) / 2.0) < Math.abs(Math.hypot(best.getX() - p.getX(), best.getZ() - p.getZ()) - (min + max) / 2.0)) {
				best = b;
			}
		}
		return best;
	}

	static BlockPos roadNear(ServerPlayer p, int min, int max) {
		City c = City.current;
		BlockPos best = null;
		for (int i = 0; i < 60; i++) {
			BlockPos b = c.road(R);
			double d = Math.hypot(b.getX() - p.getX(), b.getZ() - p.getZ());
			if (d >= min && d <= max) {
				return b;
			}
			best = b;
		}
		return best;
	}

	// ---------------------------------------------------------------- Предотвратить ограбление
	public static class Robbery extends Mission {
		private final int[] shop;
		private int stage;
		private final List<Mob> robbers = new ArrayList<>();

		public Robbery(ServerPlayer p, int[] shop) {
			super(p, "Ограбление магазина");
			this.shop = shop;
			this.target = new BlockPos(shop[0], City.current.gy + 1, shop[1]);
			this.objective = "Поезжайте к магазину: там грабят продавца";
		}

		@Override
		public void tick() {
			if (stage == 0 && dist2d(target) < 28) {
				stage = 1;
				BlockPos in = new BlockPos(shop[2], City.current.gy + 1, shop[3]);
				LivingEntity clerk = spawnNpc(in, "Продавец", false);
				if (clerk != null) {
					clerk.setInvulnerable(true);
				}
				for (int i = 0; i < 2; i++) {
					Mob r = spawnMob(EntityType.VINDICATOR, in.offset(i * 2 - 1, 0, 1), "Грабитель", true);
					if (r != null) {
						robbers.add(r);
						r.addTag("gta_robber");
					}
				}
				ticksLeft = 20 * 120;
				p.displayClientMessage(Component.literal("Грабители в магазине! Остановите их."), true);
			}
			if (stage == 1) {
				long alive = robbers.stream().filter(Mob::isAlive).count();
				objective = "Остановите грабителей (осталось " + alive + ")";
				if (alive == 0) {
					succeed(2500, "Продавец в безопасности.");
				}
			}
		}
	}

	// ---------------------------------------------------------------- Ограбление банка
	public static class BankHeist extends Mission {
		private final int[] bank;
		private int stage;
		private int drill;
		private static final int DRILL_TICKS = 200;

		public BankHeist(ServerPlayer p, int[] bank) {
			super(p, "Ограбление банка");
			this.bank = bank;
			this.target = new BlockPos(bank[0], City.current.gy + 1, bank[1]);
			this.objective = "Доберитесь до банка (золотое здание)";
		}

		private BlockPos vault() {
			return new BlockPos(bank[2], bank[3], bank[4]);
		}

		@Override
		public boolean onUseBlock(BlockPos pos) {
			if (stage == 1 && pos.equals(vault())) {
				stage = 2;
				drill = 0;
				Wanted.add(p, 3);
				level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ANVIL_LAND, net.minecraft.sounds.SoundSource.BLOCKS, 1f, 0.6f);
				p.displayClientMessage(Component.literal("Сработала сигнализация! Не отходите от сейфа."), false);
				return true;
			}
			return false;
		}

		@Override
		public void tick() {
			if (stage == 0 && dist2d(target) < 14) {
				stage = 1;
				target = vault();
				objective = "Вскройте сейф: ПКМ по золотому блоку";
			} else if (stage == 2) {
				if (p.distanceToSqr(vault().getX() + 0.5, vault().getY() + 0.5, vault().getZ() + 0.5) > 64) {
					stage = 1;
					p.displayClientMessage(Component.literal("Вы отошли от сейфа. Взлом сорван."), true);
					return;
				}
				drill++;
				objective = "Вскрываем сейф: " + (drill * 100 / DRILL_TICKS) + "%";
				if (drill % 20 == 0) {
					level.playSound(null, vault(), net.minecraft.sounds.SoundEvents.IRON_GOLEM_HURT, net.minecraft.sounds.SoundSource.BLOCKS, 1f, 0.5f);
				}
				if (drill >= DRILL_TICKS) {
					level.setBlock(vault(), Blocks.AIR.defaultBlockState(), 3);
					stage = 3;
					target = null;
					objective = "Скройтесь от полиции: оторвитесь на 60 м и снимите розыск";
				}
			} else if (stage == 3) {
				BlockPos door = new BlockPos(bank[0], 0, bank[1]);
				if (Wanted.stars(p) == 0 && dist2d(door) > 60) {
					succeed(15000, "Вы ушли с добычей.");
				}
			}
		}

		@Override
		public void cleanup() {
			level.setBlock(vault(), Blocks.GOLD_BLOCK.defaultBlockState(), 3);
			super.cleanup();
		}
	}

	// ---------------------------------------------------------------- Такси
	public static class Taxi extends Mission {
		private LivingEntity client;
		private int stage;
		private int outTicks;
		private BlockPos dest;
		private double fareDist;

		public Taxi(ServerPlayer p) {
			super(p, "Такси");
			this.target = sidewalkNear(p, 40, 90);
			this.objective = "Подъедьте к клиенту на машине";
			this.client = spawnNpc(target, "Клиент", true);
		}

		@Override
		public void tick() {
			if (stage == 0) {
				if (client != null && client.isAlive()) {
					target = client.blockPosition();
				}
				if (inCar() && dist2d(target) < 6) {
					stage = 1;
					if (client != null) {
						client.discard();
					}
					dest = sidewalkNear(p, 90, 170);
					target = dest;
					fareDist = dist2d(dest);
					ticksLeft = (int) (20 * (fareDist / 6 + 30));
					objective = "Отвезите клиента к месту назначения";
					p.displayClientMessage(Component.literal("Клиент сел в машину."), true);
				}
			} else {
				if (!inCar()) {
					outTicks++;
					objective = "Вернитесь в машину! (" + (30 - outTicks / 20) + " с)";
					if (outTicks > 600) {
						fail("Клиент ушёл.");
					}
				} else {
					outTicks = 0;
					objective = "Отвезите клиента к месту назначения";
					if (dist2d(dest) < 8) {
						succeed((int) (300 + fareDist * 6 + ticksLeft / 20 * 4), "Чаевые за скорость включены.");
					}
				}
			}
		}
	}

	// ---------------------------------------------------------------- Доставка груза
	public static class Delivery extends Mission {
		private int stage;
		private BlockPos dest;
		private double dist;

		public Delivery(ServerPlayer p) {
			super(p, "Доставка груза");
			this.target = sidewalkNear(p, 30, 80);
			this.objective = "Заберите посылку";
		}

		@Override
		public void tick() {
			if (stage == 0 && dist2d(target) < 3) {
				stage = 1;
				dest = sidewalkNear(p, 90, 170);
				target = dest;
				dist = dist2d(dest);
				ticksLeft = (int) (20 * (dist / 3 + 40));
				objective = "Доставьте посылку";
				p.displayClientMessage(Component.literal("Посылка у вас."), true);
			} else if (stage == 1 && dist2d(dest) < 4) {
				succeed((int) (600 + dist * 4), "Клиент доволен.");
			}
		}
	}

	// ---------------------------------------------------------------- Заказ
	public static class Assassination extends Mission {
		private LivingEntity victim;
		private int guards;

		public Assassination(ServerPlayer p) {
			super(p, "Заказ");
			BlockPos at = sidewalkNear(p, 60, 110);
			this.target = at;
			this.objective = "Найдите и устраните цель";
			this.victim = spawnNpc(at, "Цель", true);
			for (int i = 0; i < 2; i++) {
				Mob g = spawnMob(EntityType.SKELETON, at.offset(i * 2 - 1, 0, 2), "Охранник", false);
				if (g != null) {
					g.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW));
					g.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
				}
			}
			this.ticksLeft = 20 * 300;
		}

		@Override
		public void tick() {
			if (victim != null && victim.isAlive()) {
				target = victim.blockPosition();
			}
		}

		@Override
		public void onKill(LivingEntity v) {
			if (v == victim) {
				Wanted.add(p, 2);
				succeed(4500, "Цель устранена.");
			}
		}
	}

	// ---------------------------------------------------------------- Угон
	public static class CarTheft extends Mission {
		private ArmorStand car;
		private int stage;
		private BlockPos dest;

		public CarTheft(ServerPlayer p) {
			super(p, "Угон машины");
			BlockPos at = roadNear(p, 50, 110);
			this.car = CarManager.spawnAndGet(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, CarType.SPORT);
			if (car != null) {
				spawned.add(car);
				for (net.minecraft.world.entity.Entity e : car.getPassengers()) {
					e.setGlowingTag(true);
				}
			}
			this.target = at;
			this.objective = "Найдите и угоните спорткар";
			this.ticksLeft = 20 * 480;
		}

		@Override
		public void tick() {
			if (car == null || !car.isAlive()) {
				fail("Машина уничтожена.");
				return;
			}
			if (stage == 0) {
				target = car.blockPosition();
				if (p.getVehicle() == car) {
					stage = 1;
					dest = roadNear(p, 100, 190);
					target = dest;
					objective = "Отвезите машину в гараж";
					Wanted.add(p, 1);
				}
			} else if (dist2d(dest) < 7 && p.getVehicle() == car) {
				p.stopRiding();
				for (net.minecraft.world.entity.Entity e : new ArrayList<>(car.getPassengers())) {
					e.discard();
				}
				car.discard();
				succeed(3500, "Машина сдана в гараж.");
			}
		}
	}

	// ---------------------------------------------------------------- Уличная гонка
	public static class Race extends Mission {
		private final List<BlockPos> pts = new ArrayList<>();
		private int idx;

		public Race(ServerPlayer p) {
			super(p, "Уличная гонка");
			City c = City.current;
			List<BlockPos> all = new ArrayList<>();
			for (int i = 0; i <= c.n; i++) {
				for (int j = 0; j <= c.n; j++) {
					all.add(c.intersection(i, j));
				}
			}
			java.util.Collections.shuffle(all, R);
			double total = 0;
			BlockPos prev = p.blockPosition();
			for (int i = 0; i < Math.min(6, all.size()); i++) {
				pts.add(all.get(i));
				total += Math.hypot(all.get(i).getX() - prev.getX(), all.get(i).getZ() - prev.getZ());
				prev = all.get(i);
			}
			this.ticksLeft = (int) (20 * (total / 9 + 25));
			this.target = pts.get(0);
			this.objective = "Гонка: контрольная точка 1/" + pts.size();
		}

		@Override
		public void tick() {
			if (!inCar()) {
				objective = "Сядьте в машину, чтобы участвовать в гонке";
				return;
			}
			objective = "Гонка: контрольная точка " + (idx + 1) + "/" + pts.size();
			if (dist2d(target) < 8) {
				idx++;
				level.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1.2f);
				if (idx >= pts.size()) {
					succeed(3000 + ticksLeft / 20 * 10, "Вы пришли первым.");
				} else {
					target = pts.get(idx);
				}
			}
		}
	}
}
