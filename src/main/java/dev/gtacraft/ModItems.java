package dev.gtacraft;

import java.util.function.Function;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModItems {
	public static Item PISTOL, SHOTGUN, SMG, SNIPER, RPG, CAR_KEY, PHONE, BUGGY_BODY;

	private static <T extends Item> T reg(String name, Function<Item.Properties, T> factory, Item.Properties props) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(GtaCraft.MODID, name));
		T item = factory.apply(props.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void init() {
		PISTOL = reg("pistol", p -> new GunItem(p, GunItem.Type.PISTOL), new Item.Properties().stacksTo(1));
		SHOTGUN = reg("shotgun", p -> new GunItem(p, GunItem.Type.SHOTGUN), new Item.Properties().stacksTo(1));
		SMG = reg("smg", p -> new GunItem(p, GunItem.Type.SMG), new Item.Properties().stacksTo(1));
		SNIPER = reg("sniper", p -> new GunItem(p, GunItem.Type.SNIPER), new Item.Properties().stacksTo(1));
		RPG = reg("rpg", p -> new GunItem(p, GunItem.Type.RPG), new Item.Properties().stacksTo(1));
		CAR_KEY = reg("car_key", CarKeyItem::new, new Item.Properties().stacksTo(1));
		PHONE = reg("phone", PhoneItem::new, new Item.Properties().stacksTo(1));
		for (CarType t : CarType.values()) {
			Item body = reg(t.id + "_body", Item::new, new Item.Properties());
			if (t == CarType.BUGGY) {
				BUGGY_BODY = body;
			}
		}

		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(e -> {
			e.accept(PISTOL);
			e.accept(SHOTGUN);
			e.accept(SMG);
			e.accept(SNIPER);
			e.accept(RPG);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e -> { e.accept(CAR_KEY); e.accept(PHONE); });
	}
}
