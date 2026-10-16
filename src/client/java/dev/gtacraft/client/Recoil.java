package dev.gtacraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/** Отдача: прицел резко подбрасывает вверх, затем он частично возвращается назад. */
public final class Recoil {
	private static float pendPitch, pendYaw, appliedPitch;

	public static void kick(float pitch, float yaw) {
		pendPitch += pitch;
		pendYaw += yaw;
	}

	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) {
			pendPitch = pendYaw = appliedPitch = 0;
			return;
		}
		if (Math.abs(pendPitch) > 0.01f || Math.abs(pendYaw) > 0.01f) {
			float sp = pendPitch * 0.6f, sy = pendYaw * 0.6f;
			p.setXRot(Mth.clamp(p.getXRot() - sp, -90f, 90f));
			p.setYRot(p.getYRot() + sy);
			appliedPitch += sp;
			pendPitch -= sp;
			pendYaw -= sy;
		} else if (appliedPitch > 0.02f) {
			float back = appliedPitch * 0.1f;
			p.setXRot(Mth.clamp(p.getXRot() + back * 0.5f, -90f, 90f));
			appliedPitch -= back;
		} else {
			appliedPitch = 0;
		}
	}
}
