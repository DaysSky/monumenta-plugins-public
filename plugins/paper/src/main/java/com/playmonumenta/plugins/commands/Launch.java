package com.playmonumenta.plugins.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Launch extends GenericCommand {
	public static void register() {

		CommandPermission perms = CommandPermission.fromString("monumenta.command.launch");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new DoubleArgument("horizontal"));
		arguments.add(new DoubleArgument("vertical"));

		new CommandAPICommand("launch")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				Vector v = player.getEyeLocation().getDirection();
				v.multiply(args.getUnchecked("horizontal"));
				v.setY(args.getUnchecked("vertical"));
				player.setVelocity(v);
			})
			.register();

		arguments.clear();
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new DoubleArgument("x"));
		arguments.add(new DoubleArgument("y"));
		arguments.add(new DoubleArgument("z"));

		new CommandAPICommand("launch")
		.withPermission(perms)
		.withArguments(arguments)
		.executes((sender, args) -> {
			Player player = args.getUnchecked("player");
			Vector v = player.getEyeLocation().getDirection();
			v.setX(args.getUnchecked("x"));
			v.setY(args.getUnchecked("y"));
			v.setZ(args.getUnchecked("z"));
			player.setVelocity(v);
		})
		.register();
	}
}
