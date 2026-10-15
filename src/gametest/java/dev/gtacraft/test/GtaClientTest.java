package dev.gtacraft.test;

import dev.gtacraft.City;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Запускает настоящий клиент Minecraft и делает снимки экрана ключевых сцен. */
public class GtaClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		context.runOnClient(client -> {
			client.options.renderDistance().set(7);
			client.options.simulationDistance().set(8);
			client.options.framerateLimit().set(30);
		});
		String seed = System.getenv().getOrDefault("GTA_TEST_SEED", "7919");
		try (TestSingleplayerContext sp = context.worldBuilder()
			.adjustSettings(creator -> creator.setSeed(seed))
			.create()) {
			var server = sp.getServer();
			context.waitTicks(100);

			server.runCommand("gamemode spectator @p");
			server.runCommand("tp @p 0 140 0");
			server.runCommand("gta citypos 0 100 0 3");
			context.waitTicks(400);

			server.runCommand("tp @p -50 140 -50 -45 30");
			context.waitTicks(80);
			context.waitTicks(100);
			context.takeScreenshot("01_city_aerial");

			City c = City.current;
			if (c != null && c.bank != null) {
				server.runCommand("tp @p " + c.bank[0] + " 104 " + (c.bank[1] - 16) + " 0 8");
				context.waitTicks(60);
				context.waitTicks(100);
				context.takeScreenshot("02_bank_facade");
				server.runCommand("tp @p " + c.bank[2] + " 102 " + (c.bank[4] - 5) + " 0 20");
				context.waitTicks(40);
				context.takeScreenshot("03_bank_vault_room");
			}
			if (c != null && !c.shops.isEmpty()) {
				int[] s = c.shops.get(0);
				server.runCommand("tp @p " + s[0] + " 103 " + (s[1] - 12) + " 0 10");
				context.waitTicks(60);
				context.waitTicks(100);
				context.takeScreenshot("04_shop_facade");
			}

			server.runCommand("gamemode survival @p");
			server.runCommand("tp @p 0 101 0 0 6");
			server.runCommand("execute as @p run gta kit");
			context.waitTicks(40);

			String[] types = {"buggy", "sport", "pickup", "taxi", "police"};
			for (int i = 0; i < types.length; i++) {
				server.runCommand("gta spawncar " + (-6 + i * 3) + " 101 14 " + types[i]);
			}
			server.runCommand("tp @p 0 104 -2 0 20");
			context.waitTicks(40);
			context.takeScreenshot("05_cars_row");

			server.runCommand("execute as @p run gta wanted 3");
			server.runCommand("execute as @p run gta money 12345");
			server.runCommand("execute as @p run gta mission bank");
			server.runCommand("tp @p 0 101 -30 0 0");
			context.waitTicks(260);
			context.takeScreenshot("06_wanted_mission_cops");
			server.runCommand("execute as @p run gta wanted 0");
			server.runCommand("execute as @p run gta mission cancel");

			server.runCommand("tp @p 30 101 5 -90 0");
			server.runCommand("gta spawncar 26 101 5 sport");
			context.waitTicks(20);
			server.runCommand("execute as @p run gta enter");
			context.waitTicks(10);
			context.getInput().pressKey(options -> options.keyTogglePerspective);
			context.getInput().holdKeyFor(options -> options.keyUp, 50);
			context.takeScreenshot("07_driving_third_person");
		}
	}
}
