package dev.gtacraft;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;

/** Прохожие: жители деревни, которые гуляют по тротуарам. */
public final class Peds {
	public static final String TAG = "gta_ped";

	public static void spawn(ServerLevel level, City city, int count) {
		Random r = new Random();
		for (int i = 0; i < count; i++) {
			BlockPos pos = city.sidewalk(r);
			Villager v = EntityType.VILLAGER.create(level, EntitySpawnReason.COMMAND);
			if (v == null) {
				continue;
			}
			v.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, r.nextFloat() * 360f, 0f);
			v.setCustomName(Component.literal("Прохожий"));
			v.setPersistenceRequired();
			v.addTag(TAG);
			level.addFreshEntity(v);
		}
	}
}
