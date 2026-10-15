package dev.gtacraft;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class GtaCommands {
	public static void register(CommandDispatcher<CommandSourceStack> d, CommandBuildContext ctx, Commands.CommandSelection sel) {
		d.register(Commands.literal("gta")
			.then(Commands.literal("kit").executes(c -> {
				Kit.give(c.getSource().getPlayerOrException());
				return 1;
			}))
			.then(Commands.literal("car").executes(c -> {
				ServerPlayer p = c.getSource().getPlayerOrException();
				CarManager.spawnInFront((net.minecraft.server.level.ServerLevel) p.level(), p);
				return 1;
			}))
			.then(Commands.literal("city")
				.executes(c -> city(c.getSource(), 3))
				.then(Commands.argument("size", IntegerArgumentType.integer(1, 6))
					.executes(c -> city(c.getSource(), IntegerArgumentType.getInteger(c, "size"))))));
	}

	private static int city(CommandSourceStack src, int n) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer p = src.getPlayerOrException();
		CityBuilder.start((net.minecraft.server.level.ServerLevel) p.level(), p, n);
		src.sendSuccess(() -> Component.literal("Строю город " + (n + 1) + "x" + (n + 1) + " кварталов вокруг вас. Это займёт несколько секунд."), false);
		return 1;
	}
}
