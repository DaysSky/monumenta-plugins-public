package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.commands.experiencinator.ExperiencinatorConfig;
import com.playmonumenta.plugins.commands.experiencinator.ExperiencinatorConfig.Conversion;
import com.playmonumenta.plugins.commands.experiencinator.ExperiencinatorSettings;
import com.playmonumenta.plugins.commands.experiencinator.ExperiencinatorUtils;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

/*
 * Selective conversion menu for Experiencinators.
 * Layout:
 *
 * ================================================================
 * | back |   -  |   -  |   -  | inst |   -  |   -  |   -  |   -  |
 * |   -  |   d  |   o  |   o  |   o  |   o  |   o  |   -  |   -  |
 * |   -  |   -  |   y  |   n  |   n  |   n  |   n  |   -  |   -  |
 * ================================================================
 *
 * -: filler
 * back: back to main menu
 * inst: instructions
 * d: default conversion
 * o: conversion options. There will be as many of these as options are available to the player (at most 8 though).
 * y: selected option
 * n: unselected option (currently just filler)
 *
 */
public final class ExperiencinatorSelectiveConvertGui extends CustomInventory {

	private final Player mPlayer;
	private final ExperiencinatorConfig.Experiencinator mExperiencinator;
	private final ItemStack mExperiencinatorItem;
	private final ExperiencinatorConfig mConfig;
	private final ExperiencinatorSettings mSettings;
	private final List<List<Conversion>> mConversions;
	private int mSelectedConversion = -1;

	private ExperiencinatorSelectiveConvertGui(Player owner, ExperiencinatorConfig.Experiencinator experiencinator, ItemStack experiencinatorItem, ExperiencinatorConfig config) {
		super(owner, 3 * 9, "Selective Conversion");

		mPlayer = owner;
		mExperiencinator = experiencinator;
		mExperiencinatorItem = experiencinatorItem;
		mConfig = config;
		mSettings = new ExperiencinatorSettings(mConfig.getScoreboardConfig(), owner);

		mConversions = mConfig.getConversions().stream()
			               .filter(c -> c.getSettingsId() > 0 && c.conversionAllowedInGeneral(mPlayer, experiencinatorItem))
			.collect(Collectors.groupingBy(Conversion::getSettingsId))
			.entrySet().stream()
			.sorted(Entry.comparingByKey())
			.map(Entry::getValue)
			.limit(8) // can't fit more than 8 settings in the current layout (as one is used for the default conversion)
			.toList();

		setupInventory();

	}

	private void setupInventory() {
		mInventory.clear();
		{
			// back button
			ItemStack backButton = new ItemStack(Material.OBSERVER);
			ItemMeta meta = backButton.getItemMeta();
			meta.displayName(Component.text("Back", NamedTextColor.GRAY, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
			meta.lore(List.of(Component.text("Return to the main menu", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			backButton.setItemMeta(meta);
			GUIUtils.setPlaceholder(backButton);
			mInventory.setItem(0, backButton);
		}

		{
			// instructions
			ItemStack instructions = new ItemStack(Material.DARK_OAK_SIGN);
			ItemMeta meta = instructions.getItemMeta();
			meta.displayName(Component.text("Instructions", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
			meta.lore(List.of(Component.text("Click on items in your inventory", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
				Component.text("to convert them using the selection below.", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
				Component.text("Left click to sell all items of a stack,", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false),
				Component.text("Right click to sell a single item only.", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
			instructions.setItemMeta(meta);
			GUIUtils.setPlaceholder(instructions);
			mInventory.setItem(4, instructions);
		}

		// conversion options
		int conversionStartIndex = getConversionsStartIndex();
		{
			// default conversion
			ItemStack instructions = new ItemStack(Material.CRAFTING_TABLE);
			ItemMeta meta = instructions.getItemMeta();
			meta.displayName(Component.text("Configured Conversion", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
			meta.lore(List.of(Component.text("Uses the conversion configured in the settings", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			instructions.setItemMeta(meta);
			GUIUtils.setPlaceholder(instructions);
			mInventory.setItem(conversionStartIndex, instructions);
		}
		// other conversions
		for (int i = 0; i < mConversions.size(); i++) {
			List<Conversion> conversions = mConversions.get(i);
			List<ExperiencinatorConfig.ConversionResult> conversionResults = conversions.get(0).getAnyConversionResult();
			if (conversionResults == null) { // shouldn't happen
				continue;
			}
			ItemStack item = conversionResults.get(0).getItem();
			ItemMeta meta = item.getItemMeta();
			String conversionNames = conversions.get(0).getCombinedName() != null ? conversions.get(0).getCombinedName() : conversions.stream().map(Conversion::getName).collect(Collectors.joining("/"));
			meta.displayName(Component.text("Convert to ", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).append(Component.text(conversionNames, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)));
			meta.lore(List.of(Component.text("Will convert applicable items to", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text(conversionNames, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			meta.addItemFlags(ItemFlag.values());
			item.setItemMeta(meta);
			GUIUtils.setPlaceholder(item);
			mInventory.setItem(conversionStartIndex + i + 1, item);
		}
		{
			// selected conversion marker
			List<Conversion> conversions = mSelectedConversion >= 0 ? mConversions.get(mSelectedConversion) : null;
			String conversionNames = conversions != null
				                         ? (conversions.get(0).getCombinedName() != null ? conversions.get(0).getCombinedName() : conversions.stream().map(Conversion::getName).collect(Collectors.joining("/")))
				                         : "the configured conversion";
			ItemStack marker = new ItemStack(Material.GOLD_NUGGET);
			ItemMeta meta = marker.getItemMeta();
			meta.displayName(Component.text("Selected Conversion", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
			meta.lore(List.of(Component.text("Will convert applicable items to", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
				Component.text(conversionNames, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
			marker.setItemMeta(meta);
			GUIUtils.setPlaceholder(marker);
			mInventory.setItem(conversionStartIndex + 9 + mSelectedConversion + 1, marker);
		}


		// fill empty slots with filler
		GUIUtils.fillWithFiller(mInventory);

	}

	public static void show(Player player, Plugin plugin, ExperiencinatorConfig.Experiencinator experiencinator, ItemStack experiencinatorItem) {
		if (!ExperiencinatorUtils.checkExperiencinator(experiencinator, experiencinatorItem, player)) {
			return;
		}
		ExperiencinatorConfig config = ExperiencinatorUtils.getConfig(player.getLocation());
		new ExperiencinatorSelectiveConvertGui(player, experiencinator, experiencinatorItem, config).openInventory(player, plugin);
	}

	private int getConversionsStartIndex() {
		// centered as far as possible
		return 9 + 4 - (mConversions.size() + 1) / 2;
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);
		if (event.getClick() != ClickType.RIGHT
			    && event.getClick() != ClickType.LEFT) {
			return;
		}
		Inventory clickedInventory = event.getClickedInventory();
		int slot = event.getSlot();
		if (clickedInventory == mInventory) {
			if (slot == 0) {
				close();
				ExperiencinatorMainGui.show(mPlayer, com.playmonumenta.plugins.Plugin.getInstance(), mExperiencinator, mExperiencinatorItem);
				return;
			}
			int conversionsStartIndex = getConversionsStartIndex();
			if (slot >= conversionsStartIndex && slot - conversionsStartIndex <= mConversions.size()) {
				mSelectedConversion = slot - conversionsStartIndex - 1;
				setupInventory();
			} else if (slot - 9 >= conversionsStartIndex && slot - 9 - conversionsStartIndex <= mConversions.size()) {
				mSelectedConversion = slot - 9 - conversionsStartIndex - 1;
				setupInventory();
			}
		} else if (clickedInventory != null) {
			ItemStack item = clickedInventory.getItem(slot);
			if (item == null) {
				return;
			}
			ItemStack sellItem = item.clone();
			boolean sellAll = event.getClick() == ClickType.LEFT;
			if (!sellAll) {
				sellItem.setAmount(1);
			}
			if (sellItem(sellItem)) {
				if (sellAll) {
					clickedInventory.setItem(slot, null);
				} else {
					item.subtract();
					clickedInventory.setItem(slot, item);
				}
			}
		}
	}

	private boolean sellItem(ItemStack item) {
		Region region = ItemStatUtils.getRegion(item);
		Tier tier = ItemStatUtils.getTier(item);
		String conversionRateName = mExperiencinator.getConversionRates().get(region);
		if (conversionRateName == null) {
			return false;
		}
		int conversionSettingsId = mSelectedConversion < 0 ? mSettings.getConversion(region, tier) : mConversions.get(mSelectedConversion).get(0).getSettingsId();
		Conversion conversion = mConfig.findConversion(conversionSettingsId, region);
		if (conversion == null) {
			return false;
		}
		if (!conversion.conversionAllowed(mPlayer, region, tier, mExperiencinatorItem)) {
			return false;
		}
		return ExperiencinatorUtils.convertSingleItem(item, mPlayer, conversion, conversionRateName);
	}

}
