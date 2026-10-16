package dev.gtacraft;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Бросаемые предметы: осколочная, дымовая, коктейль Молотова. С Shift — бросок поближе и помягче. */
public class GrenadeItem extends Item {
	private final Phys.Kind kind;

	public GrenadeItem(Properties props, Phys.Kind kind) {
		super(props);
		this.kind = kind;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
			return InteractionResult.SUCCESS;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (Phys.throwGrenade(sl, sp, kind, player.isShiftKeyDown())) {
			player.getCooldowns().addCooldown(stack, 18);
			if (!player.hasInfiniteMaterials()) {
				stack.shrink(1);
			}
			Peds.scare(sl, player.position(), 12);
		}
		return InteractionResult.SUCCESS;
	}
}
