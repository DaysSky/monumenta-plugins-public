package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class UpdateHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("updatehelditem", "monumenta.command.updatehelditem", UpdateHeldItem::run);
	}

	public static void run(CommandSender sender, Player player) throws WrapperCommandSyntaxException {
		ItemStack item = player.getEquipment().getItemInMainHand();
		if (item.getAmount() <= 0) {
			throw CommandAPI.failWithString("Player must have a valid item in their main hand!");
		}
		ItemUpdateHelper.fixLegacies(item);
		String errorFound = ItemUpdateHelper.regenerateStats(item);
		ItemUpdateHelper.generateItemStats(item);
		// String errorFound = ItemUpdateHelper.checkForErrors(item);
		if (errorFound != null) {
			errorFound = "Errors found with held item: " + errorFound;
			throw CommandAPI.failWithString(errorFound);
		}
	}
}
