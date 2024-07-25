package com.playmonumenta.plugins.inventories;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.itemstats.infusions.StatTrackManager;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles anvils and upgraded lime tesseract
 */
public class AnvilFixInInventory implements Listener {
	private static final String REPAIR_OBJECTIVE = "RepairT";
	private final Plugin mPlugin;

	public AnvilFixInInventory(Plugin plugin) {
		mPlugin = plugin;
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (!event.getClick().equals(ClickType.RIGHT)) {
			return;
		}

		ItemStack anvilOrTess = event.getCursor();
		boolean isUpgradedLimeTesseract = ItemStatUtils.isUpgradedLimeTesseract(anvilOrTess);
		if (anvilOrTess == null || (!anvilOrTess.getType().equals(Material.ANVIL) && !isUpgradedLimeTesseract)) {
			return;
		}

		ItemStack item = event.getCurrentItem();

		if (item == null || item.getType().equals(Material.AIR) || item.getType().equals(Material.ANVIL)) {
			return;
		}

		Player player = (Player) event.getWhoClicked();

		if (item.getAmount() > 1) {
			player.sendMessage(Component.text("Cannot repair stacks of items!", NamedTextColor.RED));
			event.setCancelled(true);
			return;
		}

		// Check lime tess charges
		int limeTesseractCharges = 0;
		if (isUpgradedLimeTesseract) {
			limeTesseractCharges = ItemStatUtils.getCharges(anvilOrTess);
			if (limeTesseractCharges <= 0) {
				player.sendMessage(Component.text("There are no anvils in the tesseract!", NamedTextColor.RED));
				event.setCancelled(true);
				return;
			}
		}

		// before shulker check so you can unshatter them as well
		boolean unshattered = Shattered.unshatterOneLevel(item);

		if (!unshattered && ItemUtils.isShulkerBox(item.getType())) {
			return;
		}

		// Put anvils into lime tess
		if (!unshattered && !isUpgradedLimeTesseract && ItemStatUtils.isUpgradedLimeTesseract(item)) {
			ItemStatUtils.setCharges(item, ItemStatUtils.getCharges(item) + anvilOrTess.getAmount());
			ItemUpdateHelper.generateItemStats(item);
			anvilOrTess.setAmount(0);
			event.setCancelled(true);
			player.updateInventory();
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
			return;
		}

		if (unshattered
			    || (item.getDurability() > 0 && !item.getType().isBlock() && item.hasItemMeta() && ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.CURSE_OF_IRREPARIBILITY) == 0)) {
			if (!unshattered) {
				item.setDurability((short) 0);
			}
			if (isUpgradedLimeTesseract) {
				ItemStatUtils.setCharges(anvilOrTess, limeTesseractCharges - 1);
				ItemUpdateHelper.generateItemStats(anvilOrTess);
			} else {
				anvilOrTess.subtract();
			}

			World world = player.getWorld();
			world.playSound(player.getLocation(), Sound.BLOCK_ANVIL_DESTROY, SoundCategory.PLAYERS, 0.5f, 1.0f);
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				world.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 0.75f, 0.75f);
				world.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 0.75f, 0.75f);
			}, 21);

			int repCount = ScoreboardUtils.getScoreboardValue(player, REPAIR_OBJECTIVE).orElse(0) + 1;
			ScoreboardUtils.setScoreboardValue(player, REPAIR_OBJECTIVE, repCount);
			NmsUtils.getVersionAdapter().runConsoleCommandSilently("execute as " + player.getName() + " run function monumenta:mechanisms/item_repair/grant_repair_advancement");

			StatTrackManager.getInstance().incrementStatImmediately(event.getCurrentItem(), player, InfusionType.STAT_TRACK_REPAIR, 1);

			player.updateInventory();
			event.setCancelled(true);
		} else {
			player.sendMessage(Component.text("This is not a valid item to repair!", NamedTextColor.RED));
			event.setCancelled(true);
		}
	}

}
