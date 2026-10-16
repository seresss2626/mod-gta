package dev.gtacraft;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Отдача оружия: на сколько градусов клиент должен подбросить прицел. */
public record KickPayload(float pitch, float yaw) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<KickPayload> TYPE =
		new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GtaCraft.MODID, "kick"));

	public static final StreamCodec<RegistryFriendlyByteBuf, KickPayload> CODEC = CustomPacketPayload.codec(
		(KickPayload p, RegistryFriendlyByteBuf buf) -> {
			buf.writeFloat(p.pitch);
			buf.writeFloat(p.yaw);
		},
		(RegistryFriendlyByteBuf buf) -> new KickPayload(buf.readFloat(), buf.readFloat()));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
