package dev.gtacraft.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

/** Запускает настоящий клиент Minecraft и делает снимки экрана ключевых сцен. */
public class GtaClientTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			var server = sp.getServer();
			sp.getClientWorld().waitForChunksRender();

			server.runCommand("gamemode spectator @p");
			server.runCommand("tp @p 0 140 0");
			server.runCommand("gta citypos 0 100 0 2");
			context.waitTicks(220);
			server.runCommand("tp @p -75 150 -75 -45 35");
			context.waitTicks(60);
			sp.getClientWorld().waitForChunksRender();
			context.takeScreenshot("01_city_aerial");

			server.runCommand("gamemode survival @p");
			server.runCommand("tp @p 0 101 0 0 6");
			server.runCommand("execute as @p run gta kit");
			context.waitTicks(60);
			context.takeScreenshot("02_street_hud");

			String[] types = {"buggy", "sport", "pickup", "taxi", "police"};
			for (int i = 0; i < types.length; i++) {
				server.runCommand("gta spawncar " + (-6 + i * 3) + " 101 12 " + types[i]);
			}
			server.runCommand("tp @p 0 102 3 0 15");
			context.waitTicks(40);
			context.takeScreenshot("03_cars_row");

			server.runCommand("execute as @p run gta wanted 3");
			server.runCommand("execute as @p run gta money 12345");
			server.runCommand("execute as @p run gta mission bank");
			context.waitTicks(200);
			context.takeScreenshot("04_wanted_mission");

			server.runCommand("execute as @p run gta mission cancel");
			server.runCommand("execute as @p run gta wanted 0");
			server.runCommand("execute as @p run gta mission race");
			server.runCommand("execute as @p run gta enter");
			context.waitTicks(40);
			context.takeScreenshot("05_in_car_race");
		}
	}
}
