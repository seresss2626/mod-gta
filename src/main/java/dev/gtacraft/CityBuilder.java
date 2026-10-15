package dev.gtacraft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Строит город из блоков по очереди (несколько тысяч блоков за тик), чтобы сервер не зависал. */
public final class CityBuilder {
	private static final int CELL = 32;   // шаг сетки кварталов
	private static final int BAND = 11;   // полоса дороги: тротуар 2 + проезжая часть 7 + тротуар 2
	private static final int HMAX = 84;   // высота очистки над землёй
	private static final int BUDGET = 20000;
	private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

	private record Fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {}

	private static final class Task {
		ServerLevel level;
		ServerPlayer owner;
		ArrayDeque<Fill> q = new ArrayDeque<>();
		Fill cur;
		int cx, cy, cz;
		long total, done;
		List<double[]> cars = new ArrayList<>();
		int lastPct = -1;
	}

	private static final List<Task> TASKS = new ArrayList<>();
	private static final BlockPos.MutableBlockPos M = new BlockPos.MutableBlockPos();

	private static BlockState b(Block block) {
		return block.defaultBlockState();
	}

	private static void fill(Task t, int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
		int ax = Math.min(x1, x2), bx = Math.max(x1, x2), ay = Math.min(y1, y2), by = Math.max(y1, y2), az = Math.min(z1, z2), bz = Math.max(z1, z2);
		t.q.add(new Fill(ax, ay, az, bx, by, bz, s));
		t.total += (long) (bx - ax + 1) * (by - ay + 1) * (bz - az + 1);
	}

	public static void start(ServerLevel level, ServerPlayer p, int n) {
		Task t = new Task();
		t.level = level;
		t.owner = p;
		int m = n / 2;
		int gy = p.blockPosition().getY() - 1;
		int x0 = p.blockPosition().getX() - m * CELL - 5;
		int z0 = p.blockPosition().getZ() - m * CELL - 5;
		int span = n * CELL + BAND;
		int x1 = x0 + span - 1, z1 = z0 + span - 1;
		Random r = new Random(x0 * 341873128712L + z0 * 132897987541L);

		BlockState road = b(Blocks.BLACK_CONCRETE), walk = b(Blocks.LIGHT_GRAY_CONCRETE), yellow = b(Blocks.YELLOW_CONCRETE), white = b(Blocks.WHITE_CONCRETE);

		fill(t, x0, gy - 3, z0, x1, gy - 1, z1, b(Blocks.STONE));
		fill(t, x0, gy + 1, z0, x1, gy + HMAX, z1, b(Blocks.AIR));
		fill(t, x0, gy, z0, x1, gy, z1, walk);

		// дороги
		for (int i = 0; i <= n; i++) {
			int bs = i * CELL;
			fill(t, x0 + bs + 2, gy, z0, x0 + bs + 8, gy, z1, road);
			fill(t, x0, gy, z0 + bs + 2, x1, gy, z0 + bs + 8, road);
		}
		// разметка
		for (int i = 0; i <= n; i++) {
			int bs = i * CELL;
			for (int k = 0; k < span; k += 4) {
				if (!inBand(k, n)) {
					fill(t, x0 + bs + 5, gy, z0 + k, x0 + bs + 5, gy, z0 + k + 1, yellow);
					fill(t, x0 + k, gy, z0 + bs + 5, x0 + k + 1, gy, z0 + bs + 5, yellow);
				}
			}
			// пешеходные переходы перед перекрёстками
			for (int j = 0; j <= n; j++) {
				int cxx = x0 + i * CELL, czz = z0 + j * CELL;
				for (int k = 2; k <= 8; k += 2) {
					fill(t, cxx + k, gy, czz - 3, cxx + k, gy, czz - 1, white);
					fill(t, cxx + k, gy, czz + BAND, cxx + k, gy, czz + BAND + 2, white);
					fill(t, cxx - 3, gy, czz + k, cxx - 1, gy, czz + k, white);
					fill(t, cxx + BAND, gy, czz + k, cxx + BAND + 2, gy, czz + k, white);
				}
			}
		}
		// фонари вдоль дорог
		for (int i = 0; i <= n; i++) {
			int bs = i * CELL;
			for (int k = 14; k < span; k += 16) {
				if (!inBand(k, n) && !inBand(k + 1, n)) {
					lamp(t, gy, x0 + bs + 2, z0 + k);
					lamp(t, gy, x0 + bs + 8, z0 + k + 8);
					lamp(t, gy, x0 + k, z0 + bs + 2);
					lamp(t, gy, x0 + k + 8, z0 + bs + 8);
				}
			}
		}
		// кварталы
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				int lx = x0 + i * CELL + BAND, lz = z0 + j * CELL + BAND;
				double roll = r.nextDouble();
				if (roll < 0.70) {
					building(t, r, gy, lx, lz);
				} else if (roll < 0.90) {
					park(t, r, gy, lx, lz);
				} else {
					plaza(t, gy, lx, lz);
				}
			}
		}
		// машины на дорогах возле центра
		int cx = p.blockPosition().getX(), cz = p.blockPosition().getZ();
		t.cars.add(new double[]{cx + 0.5, gy + 1, cz + 7.5, 0});
		t.cars.add(new double[]{cx + 0.5 - 2, gy + 1, cz - 9.5, 180});
		t.cars.add(new double[]{cx + 9.5, gy + 1, cz + 0.5, 90});
		TASKS.add(t);
	}

	/** true, если координата k (от начала города) попадает в полосу дороги. */
	private static boolean inBand(int k, int n) {
		int mod = k % CELL;
		return k / CELL <= n && mod < BAND;
	}

	private static void lamp(Task t, int gy, int x, int z) {
		fill(t, x, gy + 1, z, x, gy + 5, z, b(Blocks.COBBLESTONE_WALL));
		fill(t, x, gy + 6, z, x, gy + 6, z, b(Blocks.SEA_LANTERN));
	}

	private static void plaza(Task t, int gy, int lx, int lz) {
		fill(t, lx + 1, gy, lz + 1, lx + 19, gy, lz + 19, b(Blocks.SMOOTH_STONE));
		fill(t, lx + 8, gy + 1, lz + 8, lx + 12, gy + 1, lz + 12, b(Blocks.STONE_BRICKS));
		fill(t, lx + 9, gy + 1, lz + 9, lx + 11, gy + 1, lz + 11, b(Blocks.WATER));
		fill(t, lx + 10, gy + 1, lz + 10, lx + 10, gy + 4, lz + 10, b(Blocks.QUARTZ_BLOCK));
		fill(t, lx + 10, gy + 5, lz + 10, lx + 10, gy + 5, lz + 10, b(Blocks.SEA_LANTERN));
	}

	private static void park(Task t, Random r, int gy, int lx, int lz) {
		fill(t, lx + 1, gy, lz + 1, lx + 19, gy, lz + 19, b(Blocks.GRASS_BLOCK));
		fill(t, lx + 10, gy, lz + 1, lx + 10, gy, lz + 19, b(Blocks.COBBLESTONE));
		fill(t, lx + 1, gy, lz + 10, lx + 19, gy, lz + 10, b(Blocks.COBBLESTONE));
		for (int i = 0; i < 7; i++) {
			int tx = lx + 3 + r.nextInt(15), tz = lz + 3 + r.nextInt(15);
			if (Math.abs(tx - (lx + 10)) < 2 || Math.abs(tz - (lz + 10)) < 2) {
				continue;
			}
			int h = 4 + r.nextInt(3);
			fill(t, tx, gy + 1, tz, tx, gy + h, tz, b(Blocks.OAK_LOG));
			fill(t, tx - 2, gy + h - 1, tz - 2, tx + 2, gy + h, tz + 2, b(Blocks.OAK_LEAVES));
			fill(t, tx - 1, gy + h + 1, tz - 1, tx + 1, gy + h + 2, tz + 1, b(Blocks.OAK_LEAVES));
		}
	}

	private static void building(Task t, Random r, int gy, int lx, int lz) {
		int bw = 13 + r.nextInt(5), bd = 13 + r.nextInt(5);
		int bx = lx + 1 + r.nextInt(21 - bw - 1), bz = lz + 1 + r.nextInt(21 - bd - 1);
		int floors = 4 + r.nextInt(15);
		int h = floors * 4;
		Block[] walls = {Blocks.GRAY_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.WHITE_CONCRETE, Blocks.STONE_BRICKS,
			Blocks.BRICKS, Blocks.CYAN_CONCRETE, Blocks.BROWN_CONCRETE, Blocks.BLUE_CONCRETE, Blocks.QUARTZ_BLOCK};
		BlockState wall = b(walls[r.nextInt(walls.length)]);
		BlockState glass = b(r.nextBoolean() ? Blocks.LIGHT_BLUE_STAINED_GLASS : Blocks.BLACK_STAINED_GLASS);
		BlockState air = b(Blocks.AIR);
		int ex = bx + bw - 1, ez = bz + bd - 1;

		fill(t, bx, gy + 1, bz, ex, gy + h, ez, wall);
		fill(t, bx + 1, gy + 1, bz + 1, ex - 1, gy + h - 1, ez - 1, air);
		for (int k = 1; k < floors; k++) {
			int y = gy + 4 * k;
			fill(t, bx + 1, y, bz + 1, ex - 1, y, ez - 1, b(Blocks.SMOOTH_STONE));
		}
		// шахта лестницы
		for (int k = 1; k < floors; k++) {
			int y = gy + 4 * k;
			fill(t, bx + 1, y, bz + 1, bx + 2, y, bz + 2, air);
		}
		BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH);
		fill(t, bx + 1, gy + 1, bz + 1, bx + 1, gy + h - 1, bz + 1, ladder);
		// окна
		for (int k = 0; k < floors; k++) {
			int y = gy + 4 * k + 2;
			for (int x = bx + 3; x + 1 <= ex - 2; x += 3) {
				fill(t, x, y, bz, x + 1, y + 1, bz, glass);
				fill(t, x, y, ez, x + 1, y + 1, ez, glass);
			}
			for (int z = bz + 3; z + 1 <= ez - 2; z += 3) {
				fill(t, bx, y, z, bx, y + 1, z + 1, glass);
				fill(t, ex, y, z, ex, y + 1, z + 1, glass);
			}
			// освещение
			fill(t, bx + bw / 2, y + 1, bz + bd / 2, bx + bw / 2, y + 1, bz + bd / 2, b(Blocks.SEA_LANTERN));
		}
		// дверь
		int dx = bx + bw / 2 - 1;
		fill(t, dx, gy + 1, bz, dx + 2, gy + 3, bz, air);
		// крыша и неоновая окантовка
		fill(t, bx, gy + h, bz, ex, gy + h, ez, b(Blocks.GRAY_CONCRETE));
		BlockState neon = b(r.nextBoolean() ? Blocks.SEA_LANTERN : Blocks.PINK_CONCRETE);
		fill(t, bx, gy + h + 1, bz, ex, gy + h + 1, bz, neon);
		fill(t, bx, gy + h + 1, ez, ex, gy + h + 1, ez, neon);
		fill(t, bx, gy + h + 1, bz, bx, gy + h + 1, ez, neon);
		fill(t, ex, gy + h + 1, bz, ex, gy + h + 1, ez, neon);
		if (floors > 9) {
			int ax = bx + bw / 2, az = bz + bd / 2 + 2;
			fill(t, ax, gy + h + 1, az, ax, gy + h + 9, az, b(Blocks.IRON_BARS));
			fill(t, ax, gy + h + 10, az, ax, gy + h + 10, az, b(Blocks.REDSTONE_LAMP));
		}
	}

	public static void tick(MinecraftServer server) {
		if (TASKS.isEmpty()) {
			return;
		}
		Task t = TASKS.get(0);
		int budget = BUDGET;
		while (budget > 0) {
			if (t.cur == null) {
				t.cur = t.q.poll();
				if (t.cur == null) {
					break;
				}
				t.cx = t.cur.x1;
				t.cy = t.cur.y1;
				t.cz = t.cur.z1;
			}
			Fill f = t.cur;
			M.set(t.cx, t.cy, t.cz);
			if (t.level.getBlockState(M) != f.s) {
				t.level.setBlock(M, f.s, FLAGS);
				budget -= 1;
			} else {
				budget -= 0;
			}
			t.done++;
			if (++t.cx > f.x2) {
				t.cx = f.x1;
				if (++t.cz > f.z2) {
					t.cz = f.z1;
					if (++t.cy > f.y2) {
						t.cur = null;
					}
				}
			}
			if ((t.done & 0x3FFFF) == 0) {
				break; // не застревать на огромных заливках из уже готовых блоков
			}
		}
		int pct = (int) (100 * t.done / Math.max(1, t.total));
		if (pct != t.lastPct && pct % 10 == 0) {
			t.lastPct = pct;
			t.owner.displayClientMessage(Component.literal("Город: " + pct + "%"), true);
		}
		if (t.cur == null && t.q.isEmpty()) {
			for (double[] c : t.cars) {
				CarManager.spawn(t.level, c[0], c[1], c[2], (float) c[3]);
			}
			t.owner.displayClientMessage(Component.literal("Город готов. Садитесь в машину: ключ в руки и правый клик рядом с ней."), false);
			TASKS.remove(0);
		}
	}
}
