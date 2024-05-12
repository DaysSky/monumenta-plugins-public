package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.ItemStackArgument;
import dev.jorel.commandapi.arguments.LootTableArgument;
import java.util.Collection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;

/*
 * NOTICE!
 * If this enchantment gets changed, make sure someone updates the Python item replacement code to match!
 * Constants and new enchantments included!
 * This most likely means @NickNackGus or @Combustible
 * If this does not happen, your changes will NOT persist across weekly updates!
 */
public class GiveSoulbound extends GenericCommand {
	@SuppressWarnings("unchecked")
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.givesoulbound");

		new CommandAPICommand("givesoulbound")
			.withPermission(perms)
			.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
			.withArguments(new ItemStackArgument("item"))
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>) args.get("players")) {
					give(player, args.getUnchecked("item"));
				}
			})
			.register();

		new CommandAPICommand("givesoulbound")
			.withPermission(perms)
			.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
			.withArguments(new LootTableArgument("item_loot_table"))
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>) args.get("players")) {
					LootTable table = args.getUnchecked("item_loot_table");
					if (table != null) {
						Collection<ItemStack> loot = table.populateLoot(FastUtils.RANDOM, new LootContext.Builder(player.getLocation()).build());
						if (!loot.isEmpty()) {
							/* Give the player the first item in the collection */
							give(player, loot.iterator().next());
						}
					}
				}
			})
			.register();
	}

	private static void give(Player player, ItemStack stack) {
		ItemStatUtils.addInfusion(stack, InfusionType.SOULBOUND, 1, player.getUniqueId());
		InventoryUtils.giveItem(player, stack);
	}
}
