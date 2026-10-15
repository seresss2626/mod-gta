package dev.gtacraft;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class Kit {
	private static final String TAG = "gta_kit_given";

	private static final Item[] BLOCKS = {
		Items.GRAY_CONCRETE, Items.LIGHT_GRAY_CONCRETE, Items.WHITE_CONCRETE, Items.BLACK_CONCRETE,
		Items.YELLOW_CONCRETE, Items.RED_CONCRETE, Items.CYAN_CONCRETE, Items.BLUE_CONCRETE, Items.PINK_CONCRETE,
		Items.STONE_BRICKS, Items.BRICKS, Items.SMOOTH_STONE, Items.QUARTZ_BLOCK, Items.GLASS,
		Items.LIGHT_BLUE_STAINED_GLASS, Items.BLACK_STAINED_GLASS, Items.IRON_BLOCK, Items.SEA_LANTERN,
		Items.GLOWSTONE, Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.OAK_LOG, Items.GRASS_BLOCK, Items.DIRT,
		Items.STONE, Items.COBBLESTONE, Items.SANDSTONE, Items.LADDER, Items.IRON_BARS, Items.COBBLESTONE_WALL,
		Items.OAK_DOOR, Items.IRON_DOOR, Items.TORCH, Items.LANTERN, Items.OAK_LEAVES, Items.WATER_BUCKET
	};

	public static void give(ServerPlayer p) {
		p.getInventory().add(new ItemStack(ModItems.PISTOL));
		p.getInventory().add(new ItemStack(ModItems.SMG));
		p.getInventory().add(new ItemStack(ModItems.SHOTGUN));
		p.getInventory().add(new ItemStack(ModItems.SNIPER));
		p.getInventory().add(new ItemStack(ModItems.RPG));
		p.getInventory().add(new ItemStack(ModItems.CAR_KEY));
		p.getInventory().add(new ItemStack(ModItems.PHONE));
		for (Item b : BLOCKS) {
			int n = b == Items.WATER_BUCKET ? 1 : 64;
			p.getInventory().add(new ItemStack(b, n));
		}
		p.displayClientMessage(Component.literal("Выдан набор: всё оружие, ключ от машины, телефон и блоки."), false);
	}

	public static void giveOnce(ServerPlayer p) {
		if (p.getTags().contains(TAG)) {
			return;
		}
		p.addTag(TAG);
		give(p);
		p.displayClientMessage(Component.literal("Команды: /gta city - построить город, /gta car - машина, /gta kit - набор снова."), false);
	}
}
