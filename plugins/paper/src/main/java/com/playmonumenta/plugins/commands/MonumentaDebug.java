package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class MonumentaDebug {
	static final String COMMAND = "monumentadebug";

	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.monumentadebug");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("INFO"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				plugin.getLogger().setLevel(Level.INFO);
			})
			.register();

		arguments.clear();
		arguments.add(new LiteralArgument("FINE"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				plugin.getLogger().setLevel(Level.FINE);
			})
			.register();

		arguments.clear();
		arguments.add(new LiteralArgument("FINER"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				plugin.getLogger().setLevel(Level.FINER);
			})
			.register();

		arguments.clear();
		arguments.add(new LiteralArgument("FINEST"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				plugin.getLogger().setLevel(Level.FINEST);
			})
			.register();
	}
}
