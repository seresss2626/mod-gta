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
	public static Item GRENADE, SMOKE_GRENADE, MOLOTOV;
	private static final Item[] RELOAD = new Item[GunItem.Type.values().length];

	public static Item reloadItem(GunItem.Type t) {
		return RELOAD[t.ordinal()];
	}

	private static <T extends Item> T reg(String name, Function<Item.Properties, T> factory, Item.Properties props) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(GtaCraft.MODID, name));
		T item = factory.apply(props.setId(key));
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static void init() {
		PISTOL = reg("pistol", p -> new GunItem(p, GunItem.Type.PISTOL, false), new Item.Properties().stacksTo(1));
		SHOTGUN = reg("shotgun", p -> new GunItem(p, GunItem.Type.SHOTGUN, false), new Item.Properties().stacksTo(1));
		SMG = reg("smg", p -> new GunItem(p, GunItem.Type.SMG, false), new Item.Properties().stacksTo(1));
		SNIPER = reg("sniper", p -> new GunItem(p, GunItem.Type.SNIPER, false), new Item.Properties().stacksTo(1));
		RPG = reg("rpg", p -> new GunItem(p, GunItem.Type.RPG, false), new Item.Properties().stacksTo(1));
		for (GunItem.Type t : GunItem.Type.values()) {
			RELOAD[t.ordinal()] = reg(t.name().toLowerCase(java.util.Locale.ROOT) + "_reload", p -> new GunItem(p, t, true), new Item.Properties().stacksTo(1));
		}
		GRENADE = reg("grenade", p -> new GrenadeItem(p, Phys.Kind.FRAG), new Item.Properties().stacksTo(16));
		SMOKE_GRENADE = reg("smoke_grenade", p -> new GrenadeItem(p, Phys.Kind.SMOKE), new Item.Properties().stacksTo(16));
		MOLOTOV = reg("molotov", p -> new GrenadeItem(p, Phys.Kind.MOLOTOV), new Item.Properties().stacksTo(16));
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
			e.accept(GRENADE);
			e.accept(SMOKE_GRENADE);
			e.accept(MOLOTOV);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(e -> { e.accept(CAR_KEY); e.accept(PHONE); });
	}
}
