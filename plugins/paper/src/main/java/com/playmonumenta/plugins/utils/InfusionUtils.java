package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.listeners.AuditListener;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class InfusionUtils {

	/**
	 * When set to true the refund function will return all the XP used for the infusion, when false only the 75%
	 */
	public static final boolean FULL_REFUND = true;
	public static final double REFUND_PERCENT = 0.75;
	public static final String PULSATING_GOLD = "epic:r1/items/currency/pulsating_gold";
	public static final String PULSATING_EMERALD = "epic:r2/items/currency/pulsating_emerald";
	public static final String PULSATING_DIAMOND = "epic:r3/items/currency/pulsating_diamond";

	public enum InfusionSelection {
		VITALITY("vitality", InfusionType.VITALITY, TextColor.fromHexString("#FF8C00"), Material.ORANGE_STAINED_GLASS_PANE),
		TENACITY("tenacity", InfusionType.TENACITY, TextColor.fromCSSHexString("#A9A9A9"), Material.BLACK_STAINED_GLASS_PANE),
		VIGOR("vigor", InfusionType.VIGOR, TextColor.fromHexString("#FF0000"), Material.RED_STAINED_GLASS_PANE),
		FOCUS("focus", InfusionType.FOCUS, TextColor.fromHexString("#FFFF00"), Material.YELLOW_STAINED_GLASS_PANE),
		PERSPICACITY("perspicacity", InfusionType.PERSPICACITY, TextColor.fromHexString("#6666FF"), Material.LIGHT_BLUE_STAINED_GLASS_PANE),
		ACUMEN("acumen", InfusionType.ACUMEN, TextColor.fromHexString("#32CD32"), Material.LIME_STAINED_GLASS_PANE),
		REFUND("refund", null, NamedTextColor.WHITE, Material.GRINDSTONE),
		SPEC_REFUND("special", null, NamedTextColor.WHITE, Material.GRINDSTONE);

		private final String mLabel;
		private final @Nullable InfusionType mInfusionType;
		private final TextColor mColor;
		private final Material mMaterial;

		InfusionSelection(String label, @Nullable InfusionType infusionType, TextColor color, Material material) {
			mLabel = label;
			mInfusionType = infusionType;
			mColor = color;
			mMaterial = material;
		}

		public static @Nullable InfusionSelection getInfusionSelection(@Nullable String label) {
			if (label == null) {
				return null;
			}
			for (InfusionSelection selection : InfusionSelection.values()) {
				if (selection.getLabel().equals(label)) {
					return selection;
				}
			}
			return null;
		}

		public static InfusionSelection getByType(InfusionType infusionType) {
			for (InfusionSelection infusionSelection : values()) {
				if (infusionSelection.mInfusionType == infusionType) {
					return infusionSelection;
				}
			}
			return REFUND;
		}

		public String getLabel() {
			return mLabel;
		}

		public String getCapitalizedLabel() {
			return StringUtils.capitalizeWords(mLabel);
		}

		public @Nullable InfusionType getInfusionType() {
			return mInfusionType;
		}

		public TextColor getColor() {
			return mColor;
		}

		public Material getMaterial() {
			return mMaterial;
		}
	}

	public static void refundInfusion(ItemStack item, Player player) throws WrapperCommandSyntaxException {
		Region region = ItemStatUtils.getRegion(item);
		int refundMaterials = 0;

		//Calculate refund amount
		// First level is free and we calculate based on the level below current.
		int infuseLevel = getInfuseLevel(item) - 1;
		int costMult = getCostMultiplierWithCheck(item);
		while (infuseLevel > 0) {
			refundMaterials += (costMult * (int) Math.pow(2, infuseLevel - 1)) * item.getAmount();
			infuseLevel--;
		}

		int level = getInfuseLevel(item);

		int xp = ExperienceUtils.getTotalExperience(player);
		int refundXP = 0;

		if (region == Region.VALLEY || region == Region.ISLES) {
			switch (ItemStatUtils.getTier(item)) {
				case COMMON:
				case UNCOMMON:
				case UNIQUE:
				case EVENT:
				case RARE:
				case PATRON:
					switch (level) {
						case 1:
							refundXP = ExperienceUtils.LEVEL_30;
							break;
						case 2:
							refundXP = ExperienceUtils.LEVEL_30 + ExperienceUtils.LEVEL_40;
							break;
						case 3:
							refundXP = ExperienceUtils.LEVEL_30 + ExperienceUtils.LEVEL_40 + ExperienceUtils.LEVEL_50;
							break;
						case 4:
							refundXP = ExperienceUtils.LEVEL_30 + ExperienceUtils.LEVEL_40 + ExperienceUtils.LEVEL_50 + ExperienceUtils.LEVEL_60;
							break;
						default:
						case 0:
							break;
					}
					break;
				case ARTIFACT:
					switch (level) {
						case 1:
							refundXP = ExperienceUtils.LEVEL_40;
							break;
						case 2:
							refundXP = ExperienceUtils.LEVEL_40 + ExperienceUtils.LEVEL_50;
							break;
						case 3:
							refundXP = ExperienceUtils.LEVEL_40 + ExperienceUtils.LEVEL_50 + ExperienceUtils.LEVEL_60;
							break;
						case 4:
							refundXP = ExperienceUtils.LEVEL_40 + ExperienceUtils.LEVEL_50 + ExperienceUtils.LEVEL_60 + ExperienceUtils.LEVEL_70;
							break;
						default:
						case 0:
							break;
					}
					break;
				case EPIC:
					switch (level) {
						case 1:
							refundXP = ExperienceUtils.LEVEL_50;
							break;
						case 2:
							refundXP = ExperienceUtils.LEVEL_50 + ExperienceUtils.LEVEL_60;
							break;
						case 3:
							refundXP = ExperienceUtils.LEVEL_50 + ExperienceUtils.LEVEL_60 + ExperienceUtils.LEVEL_70;
							break;
						case 4:
							refundXP = ExperienceUtils.LEVEL_50 + ExperienceUtils.LEVEL_60 + ExperienceUtils.LEVEL_70 + ExperienceUtils.LEVEL_80;
							break;
						default:
						case 0:
							break;
					}
					break;
				default:
					throw CommandAPI.failWithString("Invalid item. Item must be infused!");
			}
		} else if (region == Region.RING) {
			// All Ring items has same infusion price, Artifact level.
			switch (level) {
				case 1:
					refundXP = ExperienceUtils.LEVEL_40;
					break;
				case 2:
					refundXP = ExperienceUtils.LEVEL_40 + ExperienceUtils.LEVEL_50;
					break;
				case 3:
					refundXP = ExperienceUtils.LEVEL_40 + ExperienceUtils.LEVEL_50 + ExperienceUtils.LEVEL_60;
					break;
				case 4:
					refundXP = ExperienceUtils.LEVEL_40 + ExperienceUtils.LEVEL_50 + ExperienceUtils.LEVEL_60 + ExperienceUtils.LEVEL_70;
					break;
				default:
				case 0:
					break;
			}
		}

		//Remove the infusion enchants from the item
		for (InfusionSelection sel : InfusionSelection.values()) {
			InfusionType infusionType = sel.getInfusionType();
			if (infusionType != null) {
				ItemStatUtils.removeInfusion(item, infusionType, false);
			}
		}
		ItemUpdateHelper.generateItemStats(item);
		if (refundMaterials > 0 && region != null) {
			giveMaterials(player, region, refundMaterials);
		}

		refundXP = (int) (refundXP * (FULL_REFUND ? 1 : REFUND_PERCENT) * item.getAmount());
		ExperienceUtils.setTotalExperience(player, xp + refundXP);

		AuditListener.logPlayer("[Infusion] Refund - player=" + player.getName() + ", item='" + ItemUtils.getPlainName(item) + "', from level=" + level + ", stack size=" + item.getAmount()
			                        + ", refunded material count=" + refundMaterials + ", refunded XP=" + refundXP);

	}

	private static void giveMaterials(Player player, Region region, int refundMaterials) throws WrapperCommandSyntaxException {
		ItemStack stack;
		if (region.equals(Region.VALLEY)) {
			stack = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(PULSATING_GOLD));
		} else if (region.equals(Region.ISLES)) {
			stack = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(PULSATING_EMERALD));
		} else if (region.equals(Region.RING)) {
			stack = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(PULSATING_DIAMOND));
		} else {
			throw CommandAPI.failWithString("Item must have a Region tag!");
		}
		if (stack != null) {
			stack.setAmount(refundMaterials);
			InventoryUtils.giveItem(player, stack);
			return;
		}
		throw CommandAPI.failWithString("ERROR while refunding infusion (failed to get loot table). Please contact a moderator if you see this message!");
	}

	public static int calcInfuseCost(ItemStack item) throws WrapperCommandSyntaxException {
		// First level is free
		int infuseLvl = getInfuseLevel(item);
		int cost = getCostMultiplierWithCheck(item);
		// Special case for first level
		if (infuseLvl == 0) {
			cost = 0;
		} else if (infuseLvl <= 3) {
			cost *= (int) Math.pow(2, infuseLvl - 1);
		} else {
			throw CommandAPI.failWithString("Items may only be infused 4 times!");
		}
		return cost;
	}

	public static int getInfuseLevel(ItemStack item) {
		return ItemStatUtils.getInfusionLevel(item, InfusionType.ACUMEN) + ItemStatUtils.getInfusionLevel(item, InfusionType.FOCUS)
			+ ItemStatUtils.getInfusionLevel(item, InfusionType.PERSPICACITY) + ItemStatUtils.getInfusionLevel(item, InfusionType.TENACITY)
			+ ItemStatUtils.getInfusionLevel(item, InfusionType.VIGOR) + ItemStatUtils.getInfusionLevel(item, InfusionType.VITALITY);
	}

	private static int getCostMultiplierWithCheck(ItemStack item) throws WrapperCommandSyntaxException {
		int mult = getCostMultiplier(item);
		if (mult < 0) {
			throw CommandAPI.failWithString("Invalid item tier. Only Common and higher tiered items are able to be infused!");
		}
		return mult;
	}

	/**
	 * Gets the infusion cost multiplier for the given item, or -1 if the item is not of a tier that can be infused.
	 */
	public static int getCostMultiplier(ItemStack item) {
		if (ItemStatUtils.getRegion(item) == Region.RING) {
			return 3;
		}

		switch (ItemStatUtils.getTier(item)) {
			case COMMON:
			case UNCOMMON:
			case UNIQUE:
			case EVENT:
			case RARE:
			case PATRON:
				return 2;
			case ARTIFACT:
				return 3;
			case EPIC:
				return 6;
			default:
				return -1;
		}
	}

	private static int getExpInfuseCost(ItemStack item) throws WrapperCommandSyntaxException {
		int costMult = getCostMultiplierWithCheck(item);
		int level = getInfuseLevel(item);
		switch (costMult) {
			case 2:
				switch (level) {
					case 0:
					// Infuse Level 0 Rare
						return ExperienceUtils.LEVEL_30;
					case 1:
					// Infuse Level 1 Rare
						return ExperienceUtils.LEVEL_40;
					case 2:
					// Infuse Level 2 Rare
						return ExperienceUtils.LEVEL_50;
					case 3:
					// Infuse Level 3 Rare
						return ExperienceUtils.LEVEL_60;
					default:
					// Infuse Level 4 Rare
						throw CommandAPI.failWithString("ERROR while calculating experience cost (invalid score multiplier). Please contact a moderator if you see this message!");
				}
			case 3:
				switch (level) {
					case 0:
					// Infuse Level 0 Artifact
						return ExperienceUtils.LEVEL_40;
					case 1:
					// Infuse Level 1 Artifact
						return ExperienceUtils.LEVEL_50;
					case 2:
					// Infuse Level 2 Artifact
						return ExperienceUtils.LEVEL_60;
					case 3:
					// Infuse Level 3 Artifact
						return ExperienceUtils.LEVEL_70;
					default:
					// Infuse Level 4 Artifact
						throw CommandAPI.failWithString("ERROR while calculating experience cost (invalid score multiplier). Please contact a moderator if you see this message!");
				}
			case 6:
				switch (level) {
					case 0:
					// Infuse Level 0 Epic
						return ExperienceUtils.LEVEL_50;
					case 1:
					// Infuse Level 1 Epic
						return ExperienceUtils.LEVEL_60;
					case 2:
					// Infuse Level 2 Epic
						return ExperienceUtils.LEVEL_70;
					case 3:
					// Infuse Level 3 Epic
						return ExperienceUtils.LEVEL_80;
					default:
					// Infuse Level 4 Epic
						throw CommandAPI.failWithString("ERROR while calculating experience cost (invalid score multiplier). Please contact a moderator if you see this message!");
				}
			default:
				// Infuse level What even happened?
				throw CommandAPI.failWithString("ERROR while calculating experience cost (invalid score multiplier). Please contact a moderator if you see this message!");
		}
	}

	public static InfusionSelection getCurrentInfusion(ItemStack item) {

		if (ItemStatUtils.getInfusionLevel(item, InfusionType.ACUMEN) > 0) {
			return InfusionSelection.ACUMEN;
		}

		if (ItemStatUtils.getInfusionLevel(item, InfusionType.FOCUS) > 0) {
			return InfusionSelection.FOCUS;
		}

		if (ItemStatUtils.getInfusionLevel(item, InfusionType.PERSPICACITY) > 0) {
			return InfusionSelection.PERSPICACITY;
		}

		if (ItemStatUtils.getInfusionLevel(item, InfusionType.TENACITY) > 0) {
			return InfusionSelection.TENACITY;
		}

		if (ItemStatUtils.getInfusionLevel(item, InfusionType.VIGOR) > 0) {
			return InfusionSelection.VIGOR;
		}

		if (ItemStatUtils.getInfusionLevel(item, InfusionType.VITALITY) > 0) {
			return InfusionSelection.VITALITY;
		}

		return InfusionSelection.REFUND;
	}

	public static boolean infuseItem(Player player, ItemStack item, InfusionSelection selection) {
		if (!getCurrentInfusion(item).equals(selection) && getInfuseLevel(item) > 0) {
			return false;
		}

		InfusionType infusionType = selection.getInfusionType();
		if (infusionType == null) {
			return false;
		}

		int prevLvl = ItemStatUtils.getInfusionLevel(item, infusionType);
		ItemStatUtils.addInfusion(item, infusionType, prevLvl + 1, player.getUniqueId());

		return true;
	}


	public static boolean isInfusionable(ItemStack item) {
		if (item == null) {
			return false;
		}

		if (item.getAmount() != 1) {
			return false;
		}

		Region region = ItemStatUtils.getRegion(item);
		if (region != Region.VALLEY && region != Region.ISLES && region != Region.RING) {
			return false;
		}

		switch (ItemStatUtils.getTier(item)) {
			case COMMON:
			case UNCOMMON:
			case UNIQUE:
			case EVENT:
			case RARE:
			case PATRON:
			case ARTIFACT:
			case EPIC:
			case LEGENDARY:
				break;
			default:
				return false;
		}

		return true;
	}

	public static int getExpLvlInfuseCost(Plugin plugin, Player player, ItemStack item) {
		int exp;
		try {
			exp = getExpInfuseCost(item);
		} catch (WrapperCommandSyntaxException e) {
			return -1;
		}

		switch (exp) {
			case ExperienceUtils.LEVEL_30:
				return 30;
			case ExperienceUtils.LEVEL_40:
				return 40;
			case ExperienceUtils.LEVEL_50:
				return 50;
			case ExperienceUtils.LEVEL_60:
				return 60;
			case ExperienceUtils.LEVEL_70:
				return 70;
			case ExperienceUtils.LEVEL_80:
				return 80;
			default:
				return 0;
		}
	}

	public static boolean tryToPayInfusion(Player player, ItemStack item) {
		//currency
		ItemStack currency = null;
		if (ItemStatUtils.getRegion(item) == Region.RING) {
			currency = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(PULSATING_DIAMOND));
		} else if (ItemStatUtils.getRegion(item) == Region.ISLES) {
			currency = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(PULSATING_EMERALD));
		} else if (ItemStatUtils.getRegion(item) == Region.VALLEY) {
			currency = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(PULSATING_GOLD));
		}

		if (currency == null) {
			//something went wrong
			return false;
		}

		int amount;
		try {
			amount = calcInfuseCost(item);
		} catch (WrapperCommandSyntaxException e) {
			return false;
		}

		int expCost;
		try {
			expCost = getExpInfuseCost(item);
		} catch (WrapperCommandSyntaxException e) {
			return false;
		}

		int newLevel = getInfuseLevel(item) + 1;

		if (player.getGameMode() == GameMode.CREATIVE) {
			AuditListener.log("[Infusion] Player " + player.getName() + " infused an item while in creative mode! item='" + ItemUtils.getPlainName(item) + "', to level=" + newLevel
				                  + ", stack size=" + item.getAmount() + ", normal material cost count=" + amount + ", normal XP cost=" + expCost);
			return true;
		}

		int currentExp = ExperienceUtils.getTotalExperience(player);
		if (currentExp < expCost) {
			return false;
		}

		currency.setAmount(amount);
		if (!WalletUtils.tryToPayFromInventoryAndWallet(player, currency)) {
			return false;
		}

		ExperienceUtils.setTotalExperience(player, currentExp - expCost);

		AuditListener.logPlayer("[Infusion] Item infused - player=" + player.getName() + " item='" + ItemUtils.getPlainName(item) + "', to level=" + newLevel + ", stack size=" + item.getAmount()
			                        + ", material cost count=" + amount + ", XP cost=" + expCost);


		return true;
	}

}
