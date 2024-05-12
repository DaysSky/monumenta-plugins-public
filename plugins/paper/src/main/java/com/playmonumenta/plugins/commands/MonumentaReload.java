package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.mmquest.commands.MMQuest;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import org.bukkit.command.CommandSender;

public class MonumentaReload {
	public static void register(Plugin plugin) {
		new CommandAPICommand("monumentareload")
			.withPermission(CommandPermission.fromString("monumenta.command.monumentareload"))
			.executes((sender, args) -> {
				run(plugin, sender);
			})
			.register();
	}

	private static void run(Plugin plugin, CommandSender sender) {
		plugin.reloadMonumentaConfig(sender);
		MMQuest.setupQuests(Plugin.getInstance());
	}
}

