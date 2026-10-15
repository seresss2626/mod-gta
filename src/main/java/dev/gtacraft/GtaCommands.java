package dev.gtacraft;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
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
			.then(carCommand())
			.then(missionCommand())
			.then(Commands.literal("radio").executes(c -> {
				Radio.next(c.getSource().getPlayerOrException());
				return 1;
			}))
			.then(Commands.literal("spawncar")
				.then(Commands.argument("x", DoubleArgumentType.doubleArg())
					.then(Commands.argument("y", DoubleArgumentType.doubleArg())
						.then(Commands.argument("z", DoubleArgumentType.doubleArg()).executes(c -> {
							CarManager.spawn(c.getSource().getLevel(), DoubleArgumentType.getDouble(c, "x"),
								DoubleArgumentType.getDouble(c, "y"), DoubleArgumentType.getDouble(c, "z"), 0f);
							return 1;
						})))))
			.then(Commands.literal("citypos")
				.then(Commands.argument("x", IntegerArgumentType.integer())
					.then(Commands.argument("y", IntegerArgumentType.integer())
						.then(Commands.argument("z", IntegerArgumentType.integer())
							.then(Commands.argument("size", IntegerArgumentType.integer(1, 6)).executes(c -> {
								CityBuilder.start(c.getSource().getLevel(), null, IntegerArgumentType.getInteger(c, "size"),
									IntegerArgumentType.getInteger(c, "x"), IntegerArgumentType.getInteger(c, "y"),
									IntegerArgumentType.getInteger(c, "z"));
								return 1;
							}))))))
			.then(Commands.literal("city")
				.executes(c -> city(c.getSource(), 3))
				.then(Commands.argument("size", IntegerArgumentType.integer(1, 6))
					.executes(c -> city(c.getSource(), IntegerArgumentType.getInteger(c, "size"))))));
	}

	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> carCommand() {
		com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> car = Commands.literal("car").executes(c -> {
			ServerPlayer p = c.getSource().getPlayerOrException();
			CarManager.spawnInFront((net.minecraft.server.level.ServerLevel) p.level(), p);
			return 1;
		});
		for (CarType t : CarType.values()) {
			car.then(Commands.literal(t.id).executes(c -> {
				ServerPlayer p = c.getSource().getPlayerOrException();
				CarManager.spawnInFront((net.minecraft.server.level.ServerLevel) p.level(), p, t);
				return 1;
			}));
		}
		return car;
	}

	private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> missionCommand() {
		com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> m = Commands.literal("mission").executes(c -> {
			MissionManager.startRandom(c.getSource().getPlayerOrException());
			return 1;
		});
		for (MissionManager.Type t : MissionManager.Type.values()) {
			m.then(Commands.literal(t.id).executes(c -> {
				MissionManager.start(c.getSource().getPlayerOrException(), t);
				return 1;
			}));
		}
		m.then(Commands.literal("cancel").executes(c -> {
			MissionManager.cancel(c.getSource().getPlayerOrException());
			return 1;
		}));
		return m;
	}

	private static int city(CommandSourceStack src, int n) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer p = src.getPlayerOrException();
		CityBuilder.start((net.minecraft.server.level.ServerLevel) p.level(), p, n);
		src.sendSuccess(() -> Component.literal("Строю город " + (n + 1) + "x" + (n + 1) + " кварталов вокруг вас. Это займёт несколько секунд."), false);
		return 1;
	}
}
