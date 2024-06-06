package com.playmonumenta.plugins.delves;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.custominventories.BountyGui;
import com.playmonumenta.plugins.delves.abilities.Entropy;
import com.playmonumenta.plugins.delves.abilities.StatMultiplier;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DungeonUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class DelveCustomInventory extends CustomInventory {

	private final ItemStack SELECT_ALL_MOD_ITEM = getSelectAllModifiers();
	private final ItemStack REMOVE_ALL_MOD_ITEM = getResetModifiers();
	private final ItemStack BOUNTY_SELECTION_ITEM = getBountySelection();
	private final ItemStack STARTING_ITEM = new ItemStack(Material.OBSERVER);
	private final ItemStack STARTING_ITEM_NOT_ENOUGH_POINTS = new ItemStack(Material.OBSERVER);
	private final ItemStack ROTATING_DELVE_MODIFIER_INFO = DelvesModifier.createIcon(Material.MAGENTA_GLAZED_TERRACOTTA, Component.text("Rotating Modifier", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false), new String[] {"Some of these modifiers are randomly available each week.", "Selecting at least one will result in 25% increased XP."});
	private static final Map<String, String> DUNGEON_FUNCTION_MAPPINGS = new HashMap<>();

	{
		ItemMeta meta = STARTING_ITEM.getItemMeta();
		meta.displayName(Component.text("Start delve!", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		STARTING_ITEM.setItemMeta(meta);

		meta = STARTING_ITEM_NOT_ENOUGH_POINTS.getItemMeta();
		meta.displayName(Component.text("Start delve!", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		List<Component> lore = new ArrayList<>();
		lore.add(Component.text("- Requires " + DelvesUtils.MINIMUM_DEPTH_POINTS + " Delve Points to begin", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		meta.lore(lore);
		STARTING_ITEM_NOT_ENOUGH_POINTS.setItemMeta(meta);

		meta = SELECT_ALL_MOD_ITEM.getItemMeta();
		meta.displayName(Component.text("Select all modifiers!", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		SELECT_ALL_MOD_ITEM.setItemMeta(meta);

		meta = REMOVE_ALL_MOD_ITEM.getItemMeta();
		meta.displayName(Component.text("Remove all modifiers!", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		REMOVE_ALL_MOD_ITEM.setItemMeta(meta);

		meta = BOUNTY_SELECTION_ITEM.getItemMeta();
		meta.displayName(Component.text("Back to bounty selection!", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		BOUNTY_SELECTION_ITEM.setItemMeta(meta);


		DUNGEON_FUNCTION_MAPPINGS.put("white", "function monumenta:lobbies/d1/new");
		DUNGEON_FUNCTION_MAPPINGS.put("orange", "function monumenta:lobbies/d2/new");
		DUNGEON_FUNCTION_MAPPINGS.put("magenta", "function monumenta:lobbies/d3/new");
		DUNGEON_FUNCTION_MAPPINGS.put("lightblue", "function monumenta:lobbies/d4/new");
		DUNGEON_FUNCTION_MAPPINGS.put("yellow", "function monumenta:lobbies/d5/new");
		DUNGEON_FUNCTION_MAPPINGS.put("willows", "function monumenta:lobbies/db1/new");
		DUNGEON_FUNCTION_MAPPINGS.put("reverie", "function monumenta:lobbies/dc/new");
		DUNGEON_FUNCTION_MAPPINGS.put("lime", "function monumenta:lobbies/d6/new");
		DUNGEON_FUNCTION_MAPPINGS.put("pink", "function monumenta:lobbies/d7/new");
		DUNGEON_FUNCTION_MAPPINGS.put("gray", "function monumenta:lobbies/d8/new");
		DUNGEON_FUNCTION_MAPPINGS.put("lightgray", "function monumenta:lobbies/d9/new");
		DUNGEON_FUNCTION_MAPPINGS.put("cyan", "function monumenta:lobbies/d10/new");
		DUNGEON_FUNCTION_MAPPINGS.put("purple", "function monumenta:lobbies/d11/new");
		DUNGEON_FUNCTION_MAPPINGS.put("teal", "function monumenta:lobbies/dtl/new");
		DUNGEON_FUNCTION_MAPPINGS.put("forum", "function monumenta:lobbies/dff/new");
		DUNGEON_FUNCTION_MAPPINGS.put("shiftingcity", "function monumenta:lobbies/drl2/new");
		DUNGEON_FUNCTION_MAPPINGS.put("ruin", "function monumenta:lobbies/dmas/new");
		DUNGEON_FUNCTION_MAPPINGS.put("portal", "function monumenta:lobbies/dps/new");
		DUNGEON_FUNCTION_MAPPINGS.put("blue", "function monumenta:lobbies/d12/new");
		DUNGEON_FUNCTION_MAPPINGS.put("brown", "function monumenta:lobbies/d13/new");
	}

	private final int[] DELVE_MOD_ITEM_SLOTS = {
		///* 0,*/   1,  2,  3,  4,  5,  6,  7, // 8,
		///* 9,*/  10, 11, 12, 13, 14, 15, 16, //17,
		///*18,*/  19, 20, 21, 22, 23, 24, 25, //26,
		///*27,*/  28, 29, 30, 31, 32, 33, 34, //35,
		///*36,*/  37, 38, 39, 40, 41, 42, 43, //44,
		/*45,*/    46, 47, 48, 49, 50, 51, 52, //53
	};

	private static final int[] COLUMN_INDEX_SLOT = {
		/*0,*/  1, 2, 3, 4, 5, 6, 7, //53
	};

	private static final int TOTAL_POINT_SLOT = 0;
	private static final int START_SLOT = 8;
	private static final int PRESET_SLOT = 52;
	private static final int PAGE_LEFT_SLOT = 45;
	private static final int PAGE_RIGHT_SLOT = 53;
	private static final int GUI_IDENTIFIER_LOC_L = 36;
	private static final int GUI_IDENTIFIER_LOC_R = 44;

	private final Player mOwner;
	private final boolean mEditableDelvePoint;
	private final String mDungeonName;
	private int mPage = 0;
	private int mTotalPoint = 0;
	private int mIgnoreOldEntropyPoint;
	@Nullable private final DelvePreset mPreset;
	private final boolean mGuiTextures;

	private final Map<DelvesModifier, Integer> mPointSelected = new HashMap<>();

	public DelveCustomInventory(Player owner, String dungeon, boolean editable, @Nullable DelvePreset preset) {
		super(owner, 54, "Delve Modifiers " + (editable ? "Selection" : "Selected"));
		mEditableDelvePoint = editable;
		mDungeonName = dungeon;
		mOwner = owner;
		if (preset == null) {
			for (DelvesModifier mod : DelvesModifier.values()) {
				mPointSelected.put(mod, DelvesUtils.getDelveModLevel(owner, dungeon, mod));
			}
		} else {
			mPointSelected.putAll(preset.mModifiers);
		}
		mIgnoreOldEntropyPoint = mPointSelected.getOrDefault(DelvesModifier.ENTROPY, 0);
		mPreset = preset;
		mGuiTextures = GUIUtils.getGuiTextureObjective(owner);
		loadInv();
	}

	public DelveCustomInventory(Player owner, String dungeon, boolean editable) {
		this(owner, dungeon, editable, null);
	}

	private void loadInv() {
		mTotalPoint = 0;
		mInventory.clear();
		List<DelvesModifier> mods = DelvesModifier.valuesList();

		mInventory.setItem(GUI_IDENTIFIER_LOC_L, GUIUtils.createGuiIdentifierItem("gui_delve_1_l", mGuiTextures));
		mInventory.setItem(GUI_IDENTIFIER_LOC_R, GUIUtils.createGuiIdentifierItem("gui_delve_1_r", mGuiTextures));
		GUIUtils.setGuiNbtTag(SELECT_ALL_MOD_ITEM, "texture", "all_delve_1", mGuiTextures);
		GUIUtils.setGuiNbtTag(REMOVE_ALL_MOD_ITEM, "texture", "clear_delve_1", mGuiTextures);
		GUIUtils.setGuiNbtTag(STARTING_ITEM, "texture", "start_delve_1", mGuiTextures);
		GUIUtils.setGuiNbtTag(STARTING_ITEM_NOT_ENOUGH_POINTS, "texture", "start_delve_2", mGuiTextures);
		GUIUtils.setGuiNbtTag(ROTATING_DELVE_MODIFIER_INFO, "texture", "rotateinfo_delve_1", mGuiTextures);
		GUIUtils.setGuiNbtTag(BOUNTY_SELECTION_ITEM, "texture", "backtobounty_delve_1", mGuiTextures);

		for (DelvesModifier mod : mods) {
			mTotalPoint += mPointSelected.getOrDefault(mod, 0) * mod.getPointsPerLevel();
		}
		// Ignore old entropy point so the count doesn't stack. Also, only count up to how many delve points can still be randomly assigned.
		int entropy = Entropy.getDepthPointsAssigned(mPointSelected.getOrDefault(DelvesModifier.ENTROPY, 0)) - Entropy.getDepthPointsAssigned(mIgnoreOldEntropyPoint);
		int entropyAssignablePoints = DelvesModifier.entropyAssignable().stream()
			.mapToInt(mod -> DelvesUtils.getMaxPointAssignable(mod, 1000) - mPointSelected.getOrDefault(mod, 0))
			.sum();
		mTotalPoint += Math.min(entropy, entropyAssignablePoints);

		mTotalPoint = Math.max(Math.min(DelvesUtils.MAX_DEPTH_POINTS, mTotalPoint), 0);


		mInventory.setItem(TOTAL_POINT_SLOT, getSummary());
		if (mEditableDelvePoint) {
			if (mTotalPoint < 5) {
				mInventory.setItem(START_SLOT, STARTING_ITEM_NOT_ENOUGH_POINTS);
			} else {
				mInventory.setItem(START_SLOT, STARTING_ITEM);
			}
		}

		mods = getAvailableModifiers();

		for (int i = 0; i < 7; i++) {
			if (mPage * 7 + i >= mods.size()) {
				break;
			}
			DelvesModifier mod = mods.get((mPage * 7) + i);
			if (mod != null) {
				mInventory.setItem(DELVE_MOD_ITEM_SLOTS[i], mod.getIcon());
				int level = mPointSelected.getOrDefault(mod, 0);
				for (int j = 0; j < 5; j++) {
					ItemStack stack = DelvesUtils.getRankItem(mod, j + 1, level);
					if (stack != null) {
						int slot = (DELVE_MOD_ITEM_SLOTS[i] - (9 * (j + 1)));
						if (j >= level) {
							GUIUtils.setGuiNbtTag(stack, "texture", "rank_delve_2", mGuiTextures);
						} else {
							GUIUtils.setGuiNbtTag(stack, "texture", "rank_delve_1", mGuiTextures);
						}
						mInventory.setItem(slot, stack);
						if (level > DelvesUtils.MODIFIER_RANK_CAPS.getOrDefault(mod, 0)) {
							break;
						}
					}
				}
				if (DelvesModifier.rotatingDelveModifiers().contains(mod)) {
					mInventory.setItem(DELVE_MOD_ITEM_SLOTS[i] - (9 * 5), ROTATING_DELVE_MODIFIER_INFO);
				}
			}
		}

		if (mPage > 0) {
			mInventory.setItem(PAGE_LEFT_SLOT, getPreviousPage());
			if (mDungeonName.equals("ring") && mEditableDelvePoint) {
				DelvePreset delvePreset = DelvePreset.getDelvePreset(ScoreboardUtils.getScoreboardValue(mOwner, DelvePreset.PRESET_SCOREBOARD).orElse(0));
				if (delvePreset != null) {
					ItemStack presetItem = GUIUtils.createBasicItem(delvePreset.mDisplayItem, "Use Bounty Preset", NamedTextColor.WHITE, true, delvePreset.mName, NamedTextColor.AQUA);
					mInventory.setItem(PRESET_SLOT, presetItem);
				}
			}
		} else if (mEditableDelvePoint) {
			mInventory.setItem(PAGE_LEFT_SLOT, REMOVE_ALL_MOD_ITEM);
		} else if (mPreset != null && !mPreset.isDungeonChallengePreset()) {
			mInventory.setItem(PAGE_LEFT_SLOT, BOUNTY_SELECTION_ITEM);
		}

		if (mods.size() > ((mPage + 1) * 7)) {
			mInventory.setItem(PAGE_RIGHT_SLOT, getNextPage());
		} else if (mEditableDelvePoint) {
			mInventory.setItem(PAGE_RIGHT_SLOT, SELECT_ALL_MOD_ITEM);
		}

		GUIUtils.fillWithFiller(mInventory);
	}

	private List<DelvesModifier> getAvailableModifiers() {
		List<DelvesModifier> mods = DelvesModifier.valuesList();
		if (mDungeonName.startsWith("ring")) {
			mods.removeAll(DelvesModifier.rotatingDelveModifiers());
			mods.remove(DelvesModifier.ENTROPY);
		} else if (mOwner.getGameMode() != GameMode.CREATIVE) {
			List<DelvesModifier> weeklyMods = DelvesUtils.getWeeklyRotatingModifier();
			for (DelvesModifier rotating : DelvesModifier.rotatingDelveModifiers()) {
				if (mPointSelected.getOrDefault(rotating, 0) == 0 && !weeklyMods.contains(rotating)) {
					mods.remove(rotating);
				}
			}
			if (mDungeonName.startsWith("portal") || mDungeonName.startsWith("ruin")) {
				mods.remove(DelvesModifier.FRAGILE);
			}

			boolean depths = mDungeonName.startsWith("depths");
			boolean zenith = mDungeonName.startsWith("zenith");
			if (depths || zenith) {
				mods.removeAll(DelvesModifier.rotatingDelveModifiers());
				mods.remove(DelvesModifier.ENTROPY);
				if (depths) {
					mods.remove(DelvesModifier.TWISTED);
				}
			}
		}
		return mods;
	}

	public ItemStack getSummary() {
		int depthPoints = mTotalPoint;

		ItemStack item = new ItemStack(Material.SOUL_LANTERN, Math.max(1, depthPoints));

		if (mTotalPoint > 0) {
			GUIUtils.setGuiNbtTag(item, "texture", "summary_delve_1", mGuiTextures);
		} else {
			GUIUtils.setGuiNbtTag(item, "texture", "summary_delve_2", mGuiTextures);
		}

		ItemMeta meta = item.getItemMeta();
		meta.displayName(Component.text("Delve Summary", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

		List<Component> lore = new ArrayList<>();

		lore.add(Component.text(String.format("%d Delve Points Assigned", depthPoints), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));

		lore.add(Component.text(""));

		lore.add(Component.text("Stat Multipliers from Delve Points:", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
		double damageMultiplier = StatMultiplier.getDamageMultiplier(depthPoints);
		double healthMultiplier = StatMultiplier.getHealthMultiplier(depthPoints);
		double speedMultiplier = StatMultiplier.getSpeedMultiplier(depthPoints);
		NamedTextColor damageMultiplierColor;
		if (damageMultiplier >= 1.75) {
			damageMultiplierColor = NamedTextColor.DARK_RED;
		} else if (damageMultiplier >= 1.45) {
			damageMultiplierColor = NamedTextColor.RED;
		} else {
			damageMultiplierColor = NamedTextColor.GRAY;
		}
		lore.add(Component.text(String.format("- Damage Multiplier: x%.3f", damageMultiplier), damageMultiplierColor).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(String.format("- Health Multiplier: x%.3f", healthMultiplier), damageMultiplierColor).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(String.format("- Speed Multiplier: x%.3f", speedMultiplier), damageMultiplierColor).decoration(TextDecoration.ITALIC, false));

		lore.add(Component.text(""));

		DungeonUtils.DungeonCommandMapping mapping = DungeonUtils.DungeonCommandMapping.getByShard(mDungeonName);
		boolean exalted = mapping != null && mapping.getTypeName() != null && ScoreboardUtils.getScoreboardValue(mOwner, mapping.getTypeName()).orElse(0) == 1;
		double dungeonMultiplier = StatMultiplier.getStatCompensation(mDungeonName, exalted);
		lore.add(Component.text("Stat Multipliers from Base Dungeon:", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(String.format("- Damage Multiplier: x%.3f", dungeonMultiplier), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text(String.format("- Health Multiplier: x%.3f", dungeonMultiplier), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

		lore.add(Component.text(""));

		if (!(mDungeonName.equals("depths") || mDungeonName.equals("corridors"))) {
			double baseAmount = DelveLootTableGroup.getDelveMaterialTableChance(DelvesUtils.MINIMUM_DEPTH_POINTS, 9001);
			if (!(mDungeonName.equals("portal") || mDungeonName.equals("ruin"))) {
				List<Double> multipliers = new ArrayList<>();
				for (int i = 1; i <= 4; i++) {
					multipliers.add(DelveLootTableGroup.getDelveMaterialTableChance(depthPoints, i) / baseAmount);
				}

				lore.add(Component.text("Delve Material Multipliers (Not Counting Loot Scaling):", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));

				for (int i = 1; i <= 4; i++) {
					double multiplier = multipliers.get(i - 1);
					String s = i == 1 ? "" : "s";
					String plus = i == 4 ? "+" : "";
					// i Player(s): xX.XX
					String text = String.format("- %d%s Player%s: x%.2f", i, plus, s, multiplier);
					NamedTextColor color = NamedTextColor.GRAY;
					if (multiplier <= 0) {
						text = "  " + text;
						color = NamedTextColor.DARK_GRAY;
					} else if (multiplier == DelveLootTableGroup.getDelveMaterialTableChance(9001, i) / baseAmount) {
						text += " (Capped)";
						color = NamedTextColor.YELLOW;
					}
					lore.add(Component.text(text, color).decoration(TextDecoration.ITALIC, false));
				}
			} else {
				double multiplier = DelveLootTableGroup.getDelveMaterialTableChance(depthPoints, 9001) / baseAmount;
				String text = String.format("x%.2f", multiplier);
				NamedTextColor color = NamedTextColor.GRAY;
				if (multiplier <= 0) {
					color = NamedTextColor.DARK_GRAY;
				} else if (multiplier == DelveLootTableGroup.getDelveMaterialTableChance(9001, 9001) / baseAmount) {
					text += " (Capped)";
					color = NamedTextColor.YELLOW;
				}
				lore.add(Component.text("Delve Material Multiplier: ", NamedTextColor.WHITE).append(Component.text(text, color)).decoration(TextDecoration.ITALIC, false));
			}
		}

		lore.add(Component.text(""));

		if (mapping != null && mapping.getDelvePreset() != null && mapping.getDelvePreset().isDungeonChallengePreset() && DelvePreset.validatePresetModifiers(mPointSelected, mapping.getDelvePreset(), false)) {
			lore.add(Component.text("- Challenge Mode Active", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
		} else {
			lore.add(Component.text("- Challenge Mode not Active", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		}
		if (depthPoints >= DelvesUtils.MAX_DEPTH_POINTS) {
			lore.add(Component.text("- All Delves Modifiers Advancement Granted upon Completion", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
		} else {
			lore.add(Component.text("- All Delves Modifiers Advancement not Granted upon Completion", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
		}

		meta.lore(lore);

		item.setItemMeta(meta);
		ItemUtils.setPlainTag(item);

		return item;
	}

	private static ItemStack getResetModifiers() {
		return GUIUtils.createBasicItem(Material.BARRIER, "Reset Modifiers", NamedTextColor.WHITE, true, "");
	}

	private static ItemStack getBountySelection() {
		return GUIUtils.createBasicItem(Material.BARRIER, "Bounty Selection", NamedTextColor.WHITE, true, "");
	}

	private static ItemStack getSelectAllModifiers() {
		return GUIUtils.createBasicItem(Material.ENCHANTED_GOLDEN_APPLE, "Select All Modifiers", NamedTextColor.WHITE, true, "");
	}

	private ItemStack getNextPage() {
		ItemStack item = GUIUtils.createBasicItem(Material.ARROW, "Next Page", NamedTextColor.WHITE, true, "");
		GUIUtils.setGuiNbtTag(item, "texture", "right_delve_1", mGuiTextures);
		return item;
	}

	private ItemStack getPreviousPage() {
		ItemStack item = GUIUtils.createBasicItem(Material.ARROW, "Previous Page", NamedTextColor.WHITE, true, "");
		GUIUtils.setGuiNbtTag(item, "texture", "left_delve_1", mGuiTextures);
		return item;
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);
		if (!mInventory.equals(event.getClickedInventory())) {
			return;
		}

		Player playerWhoClicked = (Player) event.getWhoClicked();
		int slot = event.getSlot();

		List<DelvesModifier> mods = getAvailableModifiers();

		int column = slot % 9;
		int row = slot / 9;
		if (mEditableDelvePoint) {
			for (int i : COLUMN_INDEX_SLOT) {
				if (i == column) {

					int index = column - 1 + (mPage * 7);
					if (index >= mods.size()) {
						//Clicked past all the modifiers
						break;
					}
					DelvesModifier mod = mods.get(index);

					if (row == 5) {
						//last row -> clearing this delve mod point
						mPointSelected.put(mod, 0);
					} else {
						int finaPoint = DelvesUtils.getMaxPointAssignable(mod, 5 - row);
						mPointSelected.put(mod, finaPoint);
						playerWhoClicked.playSound(playerWhoClicked.getLocation(), Sound.BLOCK_STONE_PLACE, SoundCategory.PLAYERS, 1f, 1.5f);
					}
					if (mod == DelvesModifier.ENTROPY) {
						int newPoint = mPointSelected.getOrDefault(mod, 0);
						if (newPoint < mIgnoreOldEntropyPoint) {
							mIgnoreOldEntropyPoint = newPoint;
						}
					}

					break;
				}
			}

		}

		if (slot == PAGE_RIGHT_SLOT) {
			if (mods.size() > ((mPage + 1) * 7)) {
				mPage++;
				playerWhoClicked.playSound(playerWhoClicked.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1f, 1.5f);
			} else if (mEditableDelvePoint) {
				playerWhoClicked.playSound(playerWhoClicked.getLocation(), Sound.ENTITY_WITHER_DEATH, SoundCategory.PLAYERS, 1f, 0.5f);
				for (DelvesModifier mod : getAvailableModifiers()) {
					mPointSelected.put(mod, DelvesUtils.getMaxPointAssignable(mod, 1000));
				}
			}
		}

		if (slot == PAGE_LEFT_SLOT) {
			if (mPage > 0) {
				mPage--;
				playerWhoClicked.playSound(playerWhoClicked.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1f, 1.5f);
			} else if (mEditableDelvePoint) {
				playerWhoClicked.playSound(playerWhoClicked.getLocation(), Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1f, 0.5f);
				mPointSelected.forEach((mod, value) -> mPointSelected.put(mod, 0));
				mIgnoreOldEntropyPoint = 0;
			} else if (mPreset != null && !mPreset.isDungeonChallengePreset()) {
				this.close();
				try {
					new BountyGui(playerWhoClicked, 3, 0).open();
				} catch (Exception e) {
					MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), e);
				}
			}
		}

		if (slot == PRESET_SLOT && mPage > 0 && mDungeonName.equals("ring") && mEditableDelvePoint) {
			DelvePreset delvePreset = DelvePreset.getDelvePreset(ScoreboardUtils.getScoreboardValue(mOwner, DelvePreset.PRESET_SCOREBOARD).orElse(0));
			if (delvePreset != null) {
				mPointSelected.putAll(delvePreset.mModifiers);
			}
		}

		if (slot == START_SLOT && mEditableDelvePoint) {
			if (mTotalPoint >= 5) {
				if (mPointSelected.containsKey(DelvesModifier.ENTROPY)) {
					//give random point for each point of entropy

					int entropyPoint = Entropy.getDepthPointsAssigned(mPointSelected.get(DelvesModifier.ENTROPY)) - Entropy.getDepthPointsAssigned(mIgnoreOldEntropyPoint);

					List<DelvesModifier> entropyableMods = DelvesModifier.valuesList();
					entropyableMods.remove(DelvesModifier.ENTROPY);
					entropyableMods.removeAll(DelvesModifier.rotatingDelveModifiers());
					entropyableMods.remove(DelvesModifier.TWISTED);

					while (entropyPoint > 0) {
						if (entropyableMods.isEmpty()) {
							break;
						}
						Collections.shuffle(entropyableMods);
						DelvesModifier mod = entropyableMods.get(0);
						int oldValue = mPointSelected.getOrDefault(mod, 0);
						if (oldValue == DelvesUtils.getMaxPointAssignable(mod, oldValue + 1)) {
							entropyableMods.remove(mod);
							continue;
						}
						mPointSelected.put(mod, oldValue + 1);
						entropyPoint--;

					}
				}

				int presetId = 0;
				if (mPreset != null && DelvePreset.validatePresetModifiers(mPointSelected, mPreset, true)) {
					presetId = mPreset.mId;
				}
				DelvesManager.savePlayerData(mOwner, mDungeonName, mPointSelected, presetId);
				mInventory.clear();
				playerWhoClicked.closeInventory();
				if (!ServerProperties.getShardName().equals(mDungeonName)) {
					//if we are not in the same shard (devX-depths-R3) means that the owner need to teleport to the selected shard
					String dungeonFunc = DUNGEON_FUNCTION_MAPPINGS.get(mDungeonName);
					if (dungeonFunc == null) {
						playerWhoClicked.sendMessage(Component.text("There have been some problem, please contact the dev team, reason: DUNGEON_FUNCTION_MAPPINGS.get() is null", NamedTextColor.RED));
					} else {
						Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> NmsUtils.getVersionAdapter().runConsoleCommandSilently(
							"execute as " + mOwner.getName() + " at @s run " + dungeonFunc), 0);
					}
				}
			} else {
				playerWhoClicked.sendMessage(Component.text("You need at least 5 delve point to start a delve", NamedTextColor.RED));
			}
		}
		loadInv();

	}

	@Override
	public void inventoryClose(InventoryCloseEvent event) {
		if (mEditableDelvePoint && mPreset != null) {
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> new DelvePresetSelectionGui(mOwner, mDungeonName).open());
		}
	}
}
