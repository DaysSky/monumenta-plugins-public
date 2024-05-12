package com.playmonumenta.plugins.commands.experiencinator;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.custominventories.ExperiencinatorMainGui;
import com.playmonumenta.plugins.custominventories.ExperiencinatorSettingsGui;
import com.playmonumenta.plugins.inventories.ClickLimiter;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.scriptedquests.quests.QuestContext;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ExperiencinatorCommand {

	private static final String COMMAND = "experiencinator";
	private static final String PERMISSION_SELF = "monumenta.command.experiencinator.self";
	private static final String PERMISSION_OTHERS = "monumenta.command.experiencinator.others";
	private static final String PERMISSION_ITEMS = "monumenta.command.experiencinator.items";

	@SuppressWarnings("unchecked")
	public static void register() {

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION_SELF)
			.withArguments(new LiteralArgument("menu"))
			.executes((sender, args) -> {
				Player player = CommandUtils.getPlayerFromSender(sender);
				useExperiencinator(player, (experiencinator, item) -> ExperiencinatorMainGui.show(player, Plugin.getInstance(), experiencinator, item));
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION_OTHERS)
			.withArguments(new LiteralArgument("menu"), new EntitySelectorArgument.ManyPlayers("players"))
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>) args.get("players")) {
					useExperiencinator(player, (experiencinator, item) -> ExperiencinatorMainGui.show(player, Plugin.getInstance(), experiencinator, item));
				}
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION_SELF)
			.withArguments(new LiteralArgument("convert"))
			.executes((sender, args) -> {
				Player player = CommandUtils.getPlayerFromSender(sender);
				useExperiencinator(player, (experiencinator, item) -> ExperiencinatorUtils.useExperiencinator(experiencinator, item, player));
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION_OTHERS)
			.withArguments(new LiteralArgument("convert"), new EntitySelectorArgument.ManyPlayers("players"))
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>) args.get("players")) {
					useExperiencinator(player, (experiencinator, item) -> ExperiencinatorUtils.useExperiencinator(experiencinator, item, player));
				}
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION_SELF)
			.withArguments(new LiteralArgument("configure"))
			.executes((sender, args) -> {
				Player player = CommandUtils.getPlayerFromSender(sender);
				useExperiencinator(player, (experiencinator, item) -> ExperiencinatorSettingsGui.showConfig(player, Plugin.getInstance(), experiencinator, item));
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION_OTHERS)
			.withArguments(new LiteralArgument("configure"), new EntitySelectorArgument.ManyPlayers("players"))
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>) args.get("players")) {
					useExperiencinator(player, (experiencinator, item) -> ExperiencinatorSettingsGui.showConfig(player, Plugin.getInstance(), experiencinator, item));
				}
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION_ITEMS)
			.withArguments(new LiteralArgument("convert_items"),
			               new EntitySelectorArgument.OnePlayer("player"),
			               new EntitySelectorArgument.ManyEntities("items"),
			               new StringArgument("conversionName")
				               .replaceSuggestions(ArgumentSuggestions.strings(info -> {
					               Location lootTableLocation = info.sender() instanceof Player ? ((Player) info.sender()).getLocation() : new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
					               ExperiencinatorConfig config = ExperiencinatorUtils.getConfig(lootTableLocation);
					               return config != null ? config.getConversionNames().toArray(new String[0]) : new String[0];
				               })),
			               new StringArgument("conversionResultName")
				               .replaceSuggestions(ArgumentSuggestions.strings(info -> {
					               String conversionName = info.previousArgs() != null ? (String) info.previousArgs().get("conversionName") : null;
					               if (conversionName == null) {
						               return new String[0];
					               }
					               Location lootTableLocation = info.sender() instanceof Player ? ((Player) info.sender()).getLocation() : new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
					               ExperiencinatorConfig config = ExperiencinatorUtils.getConfig(lootTableLocation);
					               if (config == null) {
						               return new String[0];
					               }
					               ExperiencinatorConfig.Conversion conversion = config.getConversion(conversionName);
					               return conversion != null ? conversion.getConversionRateNames().toArray(new String[0]) : new String[0];
				               })),
			               new BooleanArgument("giveToPlayerOnFail"))
			.executes((sender, args) -> {
				for (Entity entity : (Collection<Entity>) args.get("items")) {
					ExperiencinatorUtils.convertItemEntity(args.getUnchecked("player"), entity, args.getUnchecked("conversionName"), args.getUnchecked("conversionResultName"), args.getUnchecked("giveToPlayerOnFail"));
				}
			})
			.register();

	}

	private static void useExperiencinator(Player player, BiConsumer<ExperiencinatorConfig.Experiencinator, ItemStack> func) throws WrapperCommandSyntaxException {
		if (ClickLimiter.isLocked(player)) {
			throw CommandAPI.failWithString("Player is being interaction limited. Bug a dev about this.");
		}

		ExperiencinatorConfig experiencinatorConfig = ExperiencinatorUtils.getConfig(player.getLocation());
		if (experiencinatorConfig == null) {
			player.sendMessage(Component.text("There's a problem with the server's Experiencinator configuration. Please contact a moderator.", NamedTextColor.RED));
			return;
		}

		// Try SQ's "used item" first
		QuestContext questContext = QuestContext.getCurrentContext();
		ItemStack item = questContext != null ? questContext.getUsedItem() : null;
		ExperiencinatorConfig.Experiencinator experiencinator = item != null ? experiencinatorConfig.getExperiencinator(item) : null;

		// Try mainhand second
		if (experiencinator == null) {
			item = player.getInventory().getItemInMainHand();
			experiencinator = experiencinatorConfig.getExperiencinator(item);
		}

		// If still no Experiencinator was found, use the best one from the inventory
		if (experiencinator == null) {
			int bestExperiencinatorIndex = -1;
			List<ExperiencinatorConfig.Experiencinator> experiencinators = experiencinatorConfig.getExperiencinators();
			for (ItemStack currentItem : player.getInventory().getContents()) {
				if (currentItem == null) {
					continue;
				}
				ExperiencinatorConfig.Experiencinator currentExperiencinator = experiencinatorConfig.getExperiencinator(currentItem);
				if (currentExperiencinator == null) {
					continue;
				}
				int currentIndex = experiencinators.indexOf(currentExperiencinator);
				if (currentIndex > bestExperiencinatorIndex) {
					experiencinator = currentExperiencinator;
					bestExperiencinatorIndex = currentIndex;
					item = currentItem;
				}
			}
		}

		if (experiencinator != null) {
			func.accept(experiencinator, item);
		} else {
			throw CommandAPI.failWithString("You don't have an Experiencinator or one of its upgrades in your inventory!");
		}
	}

}
