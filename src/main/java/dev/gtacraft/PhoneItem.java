package dev.gtacraft;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Телефон: ПКМ берёт новое задание, ПКМ с приседанием отменяет текущее. */
public class PhoneItem extends Item {
	public PhoneItem(Properties props) {
		super(props);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(player instanceof ServerPlayer sp) || level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (sp.isShiftKeyDown()) {
			MissionManager.cancel(sp);
		} else if (MissionManager.get(sp) != null) {
			sp.displayClientMessage(Component.literal("Текущее задание: " + MissionManager.get(sp).title), true);
		} else {
			MissionManager.startRandom(sp);
		}
		return InteractionResult.SUCCESS;
	}
}
