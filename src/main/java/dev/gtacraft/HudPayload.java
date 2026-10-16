package dev.gtacraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Состояние интерфейса, которое сервер отправляет клиенту. */
public record HudPayload(int money, int stars, boolean searching, String title, String objective, int seconds, String radio, String ammo) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<HudPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GtaCraft.MODID, "hud"));

	public static final StreamCodec<RegistryFriendlyByteBuf, HudPayload> CODEC = CustomPacketPayload.codec(
		(HudPayload p, RegistryFriendlyByteBuf buf) -> {
			buf.writeInt(p.money);
			buf.writeInt(p.stars);
			buf.writeBoolean(p.searching);
			buf.writeUtf(p.title);
			buf.writeUtf(p.objective);
			buf.writeInt(p.seconds);
			buf.writeUtf(p.radio);
			buf.writeUtf(p.ammo);
		},
		(RegistryFriendlyByteBuf buf) -> new HudPayload(buf.readInt(), buf.readInt(), buf.readBoolean(), buf.readUtf(), buf.readUtf(), buf.readInt(), buf.readUtf(), buf.readUtf()));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
