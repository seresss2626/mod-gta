package dev.gtacraft;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;

/** Правый клик по блоку ставит багги, правый клик в воздухе садит в ближайшую машину. */
public class CarKeyItem extends Item {
	public CarKeyItem(Properties props) {
		super(props);
	}

	@Override
	public InteractionResult useOn(UseOnContext ctx) {
		if (ctx.getLevel() instanceof ServerLevel sl && ctx.getPlayer() instanceof ServerPlayer sp) {
			BlockPos p = ctx.getClickedPos().above();
			CarManager.spawn(sl, p.getX() + 0.5, p.getY(), p.getZ() + 0.5, sp.getYRot());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel sl && player instanceof ServerPlayer sp) {
			if (sp.getVehicle() != null && CarManager.isCarEntity(sp.getVehicle())) {
				Radio.next(sp);
			} else {
				CarManager.enterNearest(sl, sp);
			}
		}
		return InteractionResult.SUCCESS;
	}
}
