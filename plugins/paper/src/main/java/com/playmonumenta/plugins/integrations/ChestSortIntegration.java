package com.playmonumenta.plugins.integrations;

import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.depths.charmfactory.CharmFactory;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Location;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import de.jeff_media.chestsort.api.ChestSortAPI;
import de.jeff_media.chestsort.api.ChestSortEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.loot.LootTable;

public class ChestSortIntegration implements Listener {
	private static boolean checkedForPlugin = false;
	private static boolean mIsEnabled = false;
	private final com.playmonumenta.plugins.Plugin mPlugin;
	private final Set<UUID> mClicked = new HashSet<>();

	public ChestSortIntegration(com.playmonumenta.plugins.Plugin plugin) {
		mPlugin = plugin;
		plugin.getLogger().info("Enabling ChestSort integration");
	}

	private static void checkForPlugin() {
		mIsEnabled = Bukkit.getServer().getPluginManager().isPluginEnabled("ChestSort");
		checkedForPlugin = true;
	}

	public static boolean isPresent() {
		if (!checkedForPlugin) {
			checkForPlugin();
		}

		return mIsEnabled;
	}

	public static void sortInventory(Inventory inventory) {
		if (!isPresent()) {
			return;
		}

		if (inventory instanceof PlayerInventory) {
			ChestSortAPI.sortInventory(inventory, 9, 35);
		} else {
			ChestSortAPI.sortInventory(inventory);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (!isPresent()) {
			return;
		}
		if (event.getWhoClicked() instanceof Player player) {
			InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, player, event);
			Inventory inventory = event.getClickedInventory();
			if (inventory == null) {
				return;
			}

			if (!(inventory instanceof PlayerInventory) && ZoneUtils.hasZoneProperty(player, ZoneProperty.SHOPS_POSSIBLE)) {
				/* Don't sort market chests */
				return;
			}

			if (event.getClick().equals(ClickType.RIGHT)
				&& inventory.getItem(event.getSlot()) == null
				&& event.getAction().equals(InventoryAction.NOTHING)
				&& event.getSlotType() != InventoryType.SlotType.CRAFTING) {

				// Player right-clicked a non-crafting empty space and nothing happened
				// Check if the last thing the player did was also the same thing.
				// If so, sort the chest
				if (mClicked.contains(player.getUniqueId())) {
					ChestSortIntegration.sortInventory(inventory);
					player.updateInventory();
					mClicked.remove(player.getUniqueId());

					// Just in case we sorted an item on top of where the player was clicking
					event.setCancelled(true);
				} else {
					// Mark the player as having right-clicked an empty slot
					mClicked.add(player.getUniqueId());
				}
			} else {
				// Player did something else with this inventory - clear the marker
				mClicked.remove(player.getUniqueId());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void inventoryOpenEvent(InventoryOpenEvent event) {
		if (!isPresent()) {
			return;
		}

		if (event.getPlayer() instanceof Player) {
			mClicked.remove(event.getPlayer().getUniqueId());
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		if (!isPresent()) {
			return;
		}

		InventoryHolder holder = event.getInventory().getHolder();
		if (holder instanceof Player) {
			mClicked.remove(((Player) holder).getUniqueId());
		}
	}

	@SuppressWarnings("EnumOrdinal")
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void chestSortEvent(ChestSortEvent event) {
		for (Map.Entry<ItemStack, Map<String, String>> itemSortMapPair : event.getSortableMaps().entrySet()) {
			ItemStack item = itemSortMapPair.getKey();
			Map<String, String> sortMap = itemSortMapPair.getValue();

			// Fix name sorting to ignore formatting
			String customName = sortMap.get("{customName}");
			if (customName != null) {
				customName = ItemUtils.toPlainTagText(customName);
				sortMap.put("{customName}", customName);
			}

			String strBookAuthor = ItemUtils.getBookAuthor(item);
			if (strBookAuthor == null) {
				strBookAuthor = "~bookAuthor~";
			} else {
				strBookAuthor = ItemUtils.toPlainTagText(strBookAuthor) + " ";
			}

			String strBookTitle = ItemUtils.getBookTitle(item);
			if (strBookTitle == null) {
				strBookTitle = "~bookTitle~";
			} else {
				strBookTitle = ItemUtils.toPlainTagText(strBookTitle) + " ";
			}

			boolean isZenithCharm = ItemStatUtils.isZenithCharm(item);

			String strCharmClass;
			PlayerClass playerClass = ItemStatUtils.getCharmClass(item);
			if (isZenithCharm) {
				strCharmClass = "Zenith";
			} else if (playerClass == null) {
				strCharmClass = "~Generalist~";
			} else {
				strCharmClass = playerClass.mClassName;
			}

			String strZenithCharmRarity;
			if (!isZenithCharm) {
				strZenithCharmRarity = "~zenithCharmRarity~";
			} else {
				strZenithCharmRarity = String.format("%02d", CharmFactory.getZenithCharmRarity(item));
			}

			String strCharmPower = String.valueOf(ItemStatUtils.getCharmPower(item));

			int itemCount = 0;
			if (item != null) {
				itemCount = item.getAmount();
			}
			// Rather than sorting by the count from lowest to highest, sort from highest to lowest.
			// A max custom stack has 127 items, a min custom stack has -128. Scale from max -> 0 to min -> 255.
			String strCount = String.format("%03d", 127 - itemCount);

			String strDamage = String.format("%5.3f", ItemUtils.getDamagePercent(item));

			String strFishQuality = String.valueOf(ItemStatUtils.getFishQuality(item));

			String strIsMaterial = String.valueOf(
				ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.MATERIAL));

			String strLocation = "~location~"; // Missing values start with ~ and wind up at the end
			Location location = ItemStatUtils.getLocation(item);
			if (location != null) {
				int ordinal = location.ordinal();
				String name = location.toString();
				strLocation = String.format("%03d_%s", ordinal, name);
			}

			String strLootTable = "~lootTable~";
			LootTable lootTable = ItemUtils.getLootTable(item);
			if (lootTable != null) {
				// Trailing space to make shorter names appear first
				strLootTable = lootTable.getKey() + " ";
			}

			String strQuest = ItemUtils.getItemQuestId(item);
			if (strQuest == null) {
				strQuest = "~quest~";
			} else {
				strQuest = strQuest + " ";
			}

			String strRegion = "~region~"; // Missing values start with ~ and wind up at the end
			Region region = ItemStatUtils.getRegion(item);
			if (region != null) {
				int ordinal = region.ordinal();
				String name = region.toString();
				strRegion = ordinal + "_" + name;
			}

			String strTier = "~tier~";
			Tier tier = ItemStatUtils.getTier(item);
			if (tier != null) {
				int ordinal = tier.ordinal();
				String name = tier.toString();
				strTier = String.format("%02d_%s", ordinal, name);
			}

			sortMap.put("{bookAuthor}", strBookAuthor);
			sortMap.put("{bookTitle}", strBookTitle);
			sortMap.put("{charmClass}", strCharmClass);
			sortMap.put("{charmPower}", strCharmPower);
			sortMap.put("{zenithCharmRarity}", strZenithCharmRarity);
			sortMap.put("{count}", strCount);
			sortMap.put("{damage}", strDamage);
			sortMap.put("{fishQuality}", strFishQuality);
			sortMap.put("{isMaterial}", strIsMaterial);
			sortMap.put("{location}", strLocation);
			sortMap.put("{lootTable}", strLootTable);
			sortMap.put("{quest}", strQuest);
			sortMap.put("{region}", strRegion);
			sortMap.put("{tier}", strTier);
		}
	}
}
