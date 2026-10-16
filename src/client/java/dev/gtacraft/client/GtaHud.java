package dev.gtacraft.client;

import dev.gtacraft.GtaCraft;
import dev.gtacraft.HudPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Интерфейс в стиле GTA: деньги и звёзды розыска справа сверху, цель задания слева снизу. */
public final class GtaHud {
	public static volatile HudPayload state;

	private static final Identifier MONEY = Identifier.fromNamespaceAndPath(GtaCraft.MODID, "textures/gui/money.png");
	private static final Identifier STAR_ON = Identifier.fromNamespaceAndPath(GtaCraft.MODID, "textures/gui/star_on.png");
	private static final Identifier STAR_OFF = Identifier.fromNamespaceAndPath(GtaCraft.MODID, "textures/gui/star_off.png");

	// Атлас: 11 ячеек 32x48: цифры 0-9 и знак доллара (индекс 10)
	private static final int CELL_W = 32, CELL_H = 48, ATLAS_W = 352;
	private static final int GLYPH_W = 20, GLYPH_H = 30;

	public static void render(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		HudPayload s = state;
		if (s == null || mc.player == null || mc.options.hideGui) {
			return;
		}
		int right = g.guiWidth() - 18;

		// деньги
		String digits = Integer.toString(s.money());
		int x = right - GLYPH_W;
		for (int i = digits.length() - 1; i >= 0; i--) {
			glyph(g, digits.charAt(i) - '0', x, 14);
			x -= GLYPH_W - 2;
		}
		glyph(g, 10, x, 14);

		// звёзды розыска
		boolean flash = s.searching() && (System.currentTimeMillis() / 400) % 2 == 0;
		int sx = right - 24;
		for (int i = 4; i >= 0; i--) {
			boolean on = i < s.stars();
			Identifier tex = on && !flash ? STAR_ON : STAR_OFF;
			g.blit(RenderPipelines.GUI_TEXTURED, tex, sx, 50, 0f, 0f, 24, 24, 32, 32, 32, 32);
			sx -= 26;
		}

		// оружие и патроны
		if (!s.ammo().isEmpty()) {
			g.drawString(mc.font, s.ammo(), right - mc.font.width(s.ammo()), 94, 0xFFF2C94C, true);
		}

		// радио
		if (!s.radio().isEmpty()) {
			String r = "♪ " + s.radio();
			g.drawString(mc.font, r, right - mc.font.width(r), 80, 0xFFE8E8E8, true);
		}

		// задание
		if (!s.title().isEmpty()) {
			int y = g.guiHeight() - 92;
			g.drawString(mc.font, s.title().toUpperCase(), 18, y, 0xFFF2C94C, true);
			g.drawString(mc.font, s.objective(), 18, y + 12, 0xFFFFFFFF, true);
			if (s.seconds() >= 0) {
				String t = String.format("%d:%02d", s.seconds() / 60, s.seconds() % 60);
				g.drawString(mc.font, t, 18, y + 24, s.seconds() < 20 ? 0xFFFF5555 : 0xFFFFFFFF, true);
			}
		}
	}

	private static void glyph(GuiGraphics g, int index, int x, int y) {
		g.blit(RenderPipelines.GUI_TEXTURED, MONEY, x, y, (float) (index * CELL_W), 0f, GLYPH_W, GLYPH_H, CELL_W, CELL_H, ATLAS_W, CELL_H);
	}
}
