package com.playmonumenta.plugins.fishing;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.infusions.StatTrackFishCaught;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.protocollib.FishingParticleListener;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class FishingManager implements Listener {
	private final ImmutableList<Supplier<? extends FishingMinigame>> FISHING_MINIGAMES = ImmutableList.of(
		() -> new RingsFM(4, false, 0),
		() -> new RingsFM(3, true, 0),
		() -> new RingsFM(3, false, 2),
		() -> new DirectionalFM(200, 0.3, Color.fromRGB(200, 0, 20), 0.016, false),
		() -> new DirectionalFM(140, 0.45, Color.fromRGB(180, 20, 120), 0.026, false),
		() -> new DirectionalFM(140, 0.75, Color.fromRGB(255, 200, 50), 0.021, true),
		() -> new RhythmFM(false, false),
		() -> new RhythmFM(true, false),
		() -> new RhythmFM(false, true),
		() -> new PointAndClickFM(120, 7, 0.4, false, false),
		() -> new PointAndClickFM(130, 6, 0.5, true, false),
		() -> new PointAndClickFM(150, 8, 0.4, false, true),
		() -> new MinesweeperFM(40 * 20, 4, 5),
		() -> new MinesweeperFM(30 * 20, 3, 4),
		() -> new MinesweeperFM(55 * 20, 6, 6)
	);
	private final HashMap<Player, FishingMinigame> mPlayerMinigameMap = new HashMap<>();
	private final HashMap<Player, FishHook> mPlayerFishHookMap = new HashMap<>();
	private final HashMap<Player, BaitInfo> mPlayerBaitInfoMap = new HashMap<>();
	private final ArrayList<Player> mLeftClickSuppression = new ArrayList<>();
	private final ArrayList<Player> mPlayerPrepCombatList = new ArrayList<>();
	private final FishingCombatManager mCombatManager;
	private static final String LESSER_LOOT_TABLE = "epic:r3/world/fishing/custom_fishing/cache_lesser";
	private static final String GREATER_LOOT_TABLE = "epic:r3/world/fishing/custom_fishing/cache_greater";
	private static final String ABYSSAL_LOOT_TABLE = "epic:r3/world/fishing/custom_fishing/cache_abyssal";
	private static final String WEIGHTED_FISH_TABLE = "epic:r3/items/fishing/fish/ring_fish_greater_weighted";
	private static final String FISH_COMBAT_PERMISSION = "monumenta.fishingcombat";

	public FishingManager(FishingCombatManager combatManager) {
		mCombatManager = combatManager;
	}

	@EventHandler(ignoreCancelled = false)
	public void onFish(PlayerFishEvent event) {
		Player player = event.getPlayer();
		if (event.getState() == PlayerFishEvent.State.FISHING && mPlayerMinigameMap.containsKey(player)) {
			// Safety precaution, don't let the player start fishing while they're on the minigame map.
			event.setCancelled(true);
		} else if (event.getState() == PlayerFishEvent.State.FISHING && !mPlayerFishHookMap.containsKey(player)) {
			FishHook fishHook = event.getHook();
			mPlayerFishHookMap.put(player, fishHook);
			// Keep track of the fishhook in case it meets an unexpected end.
			trackFishHook(player, fishHook);
		} else if (event.getState() == PlayerFishEvent.State.BITE && !mPlayerMinigameMap.containsKey(player)) {
			// Player gets bite while not in minigame.
			prepareSpecialFishingEventStart(player);
		} else if (event.getState() == PlayerFishEvent.State.BITE) {
			// Player gets bite while in minigame.
			event.setCancelled(true);
		} else if (event.getState() == PlayerFishEvent.State.FAILED_ATTEMPT && (mPlayerMinigameMap.containsKey(player) || mPlayerPrepCombatList.contains(player))) {
			// Player fails a bite with a special fishing event prepared for them.
			unprepareSpecialFishingEvent(player);
		} else if (event.getState() == PlayerFishEvent.State.REEL_IN) {
			mPlayerFishHookMap.remove(player);
		} else if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
			mPlayerFishHookMap.remove(player);
			Item caughtItem = (Item) event.getCaught();
			if (caughtItem == null) {
				return;
			}
			ItemStack caughtItemStack = caughtItem.getItemStack();
			Material caughtItemType = caughtItemStack.getType();
			if (caughtItemType == Material.CHEST) {
				caughtItemStack.setItemMeta(getLesserChest().getItemMeta());
			} else if (Constants.Materials.FISH.contains(caughtItemType)) {
				// If the player has a bait to have a higher chance at a specific fish type,
				// roll the chance, and get a fish from that loot table if successful.
				BaitInfo baitInfo = mPlayerBaitInfoMap.get(player);
				if (baitInfo != null && shouldReplaceCaughtFish(baitInfo) && baitInfo.mBait.mReplacementLootTable != null) {
					ItemStack overrideItem = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(baitInfo.mBait.mReplacementLootTable.mPath));
					if (overrideItem != null) {
						caughtItemStack.setItemMeta(overrideItem.getItemMeta());
						caughtItemStack.setType(overrideItem.getType());
					}
				}

				modifyAndAssessFishQuality(player, caughtItemStack, baitInfo);
			}
		}
	}

	@EventHandler(ignoreCancelled = false)
	public void onInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (!mPlayerMinigameMap.containsKey(player) && !mPlayerPrepCombatList.contains(player)) {
			return;
		}

		FishHook fishHook = mPlayerFishHookMap.get(player);
		if (fishHook == null) {
			return;
		}

		// Start combat before event cancel to trigger an onFish REEL_IN event correctly if combat does not begin.
		if (mPlayerPrepCombatList.contains(player) && event.getAction().isRightClick()) {
			// If the arena fails to initiate properly, just let the player fish instead.
			boolean initiated = initiateFishingCombat(player);
			if (initiated) {
				event.setCancelled(true);
			} else {
				unprepareSpecialFishingEvent(player);
			}
			return;
		}

		// We can cancel all clicks, as the player is preparing or in a mini-game, ensuring they have a fishhook.
		event.setCancelled(true);

		// Get minigame info only after we know this isn't a combat initiation.
		if (!mPlayerMinigameMap.containsKey(player)) {
			return;
		}
		FishingMinigame fishingMinigame = mPlayerMinigameMap.get(player);

		// Minigame initiation.
		if (fishingMinigame.minigameUnstarted() && event.getAction().isRightClick()) {
			initiateFishingMinigame(player, fishingMinigame, fishHook);
			return;
		}

		// Minigame input detection.
		if (!fishingMinigame.twentyTicksPassed() || mLeftClickSuppression.contains(player)) {
			return;
		}
		if (event.getAction().isLeftClick()) {
			fishingMinigame.onLeftClickInternal();
		} else if (event.getAction().isRightClick()) {
			fishingMinigame.onRightClickInternal();
			suppressLeftClick(player); // Right clicks while fishing cause left click events immediately after, so suppress it.
		}
	}

	private void trackFishHook(Player player, FishHook fishHook) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (fishHook.isValid()) {
					return;
				}
				if (!mPlayerFishHookMap.containsKey(player)) {
					this.cancel();
					return;
				}
				mPlayerPrepCombatList.remove(player);
				if (mPlayerMinigameMap.containsKey(player)) {
					FishingMinigame fishingMinigame = mPlayerMinigameMap.get(player);
					fishingMinigame.cancelMinigame();
					FishingParticleListener.allowFishingParticles(player);
					mPlayerMinigameMap.remove(player);
				}
				mPlayerFishHookMap.remove(player);
				this.cancel();
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private void prepareSpecialFishingEventStart(Player player) {
		FishHook fishHook = mPlayerFishHookMap.get(player);
		if (fishHook == null) {
			return;
		}

		double eventProbability = FastUtils.randomDoubleInRange(0, 1);
		double minigameOdds = 0.1;
		double combatOdds = 0.05;

		// Bait Management
		// Remove any previous bait effects being applied to the player, get the next bait, then put them on the bait map.
		mPlayerBaitInfoMap.remove(player);
		BaitInfo baitInfo = getFirstBait(player);
		mPlayerBaitInfoMap.put(player, baitInfo);

		// If they have bait, the quantity may be decreased, and we can do *stuff* with it.
		if (baitInfo != null) {
			baitInfo.mItemStack.setAmount(baitInfo.mItemStack.getAmount() - 1);
			minigameOdds += baitInfo.mBait.mMinigameOdds;
			combatOdds += baitInfo.mBait.mCombatOdds;
		}

		if (eventProbability < minigameOdds) {
			FishingParticleListener.suppressFishingParticles(player, fishHook.getLocation());
			mPlayerMinigameMap.put(player, rollMinigame());
		} else if (eventProbability < minigameOdds + combatOdds &&
			           !ZoneUtils.hasZoneProperty(fishHook.getLocation(), ZoneUtils.ZoneProperty.ADVENTURE_MODE) &&
			           !ZoneUtils.hasZoneProperty(player.getLocation(), ZoneUtils.ZoneProperty.ADVENTURE_MODE) &&
			           player.hasPermission(FISH_COMBAT_PERMISSION)) {
			mPlayerPrepCombatList.add(player);
		}
	}

	private void unprepareSpecialFishingEvent(Player player) {
		if (mPlayerPrepCombatList.contains(player)) {
			mPlayerPrepCombatList.remove(player);
		} else if (mPlayerMinigameMap.containsKey(player) && mPlayerMinigameMap.get(player).minigameUnstarted()) {
			mPlayerMinigameMap.remove(player);
			FishingParticleListener.allowFishingParticles(player);
		}
	}

	private boolean initiateFishingCombat(Player player) {
		mPlayerPrepCombatList.remove(player);
		FishHook hook = mPlayerFishHookMap.get(player);
		if (hook == null) {
			return false;
		}
		Location cleanedFishHookLoc = findWaterSurface(hook.getLocation()).add(0, 0.01, 0);
		boolean initiated = mCombatManager.initiate(player, cleanedFishHookLoc, ScoreboardUtils.getScoreboardValue(player, "FishCombatDifficulty").orElse(0));
		if (initiated) {
			removeHook(player, false);
		}
		return initiated;
	}

	private void initiateFishingMinigame(Player player, FishingMinigame fishingMinigame, FishHook fishHook) {
		Location fishHookLoc = fishHook.getLocation().clone();
		Location centre = findWaterSurface(fishHookLoc).add(0, 0.01, 0);

		fishingMinigame.beginTracking(); // Track the mini-game progress separately.

		player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1f, 1.5f);
		fishingMinigame.previewMinigame(player, centre.clone());
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> fishingMinigame.previewMinigame(player, centre), 10);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> fishingMinigame.beginMinigame(this, player, centre), 20);
	}

	public void minigameSuccess(Player player) {
		player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 1f, 1.5f);
		new PartialParticle(Particle.VILLAGER_HAPPY, player.getEyeLocation(), 20).delta(3, 0.5, 3).spawnAsPlayerActive(player);
		double chooseReward = FastUtils.randomDoubleInRange(0, 1);
		if (chooseReward < 0.8) {
			BaitInfo baitInfo = mPlayerBaitInfoMap.get(player);
			@Nullable FishLootTable forcedLootTable = baitInfo == null ? null : baitInfo.mBait.mReplacementLootTable;
			@Nullable String forcedLootTableString = forcedLootTable == null ? null : forcedLootTable.mGreaterPath;
			if (baitInfo == null || !shouldReplaceCaughtFish(baitInfo)) {
				forcedLootTableString = null;
			}

			ItemStack reward = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(Objects.requireNonNullElse(forcedLootTableString, WEIGHTED_FISH_TABLE)));
			if (reward == null) {
				return;
			}

			modifyAndAssessFishQuality(player, reward, baitInfo);

			InventoryUtils.giveItem(player, reward);
		} else {
			ItemStack reward = new ItemStack(Material.CHEST);
			reward.setItemMeta(getLesserChest().getItemMeta());
			InventoryUtils.giveItem(player, reward);
		}
		removeHook(player, true);
	}

	public void minigameFailure(Player player) {
		player.playSound(player, Sound.ENTITY_PAINTING_BREAK, SoundCategory.PLAYERS, 1f, 0.5f);
		removeHook(player, true);
	}

	private FishingMinigame rollMinigame() {
		return FISHING_MINIGAMES.get(FastUtils.randomIntInRange(0, FISHING_MINIGAMES.size() - 1)).get();
	}

	private void removeHook(Player player, boolean inMinigame) {
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, SoundCategory.PLAYERS, 1, 1);
		FishHook fishHook = mPlayerFishHookMap.get(player);
		if (!(fishHook == null)) {
			fishHook.remove();
		}
		mPlayerFishHookMap.remove(player);
		if (inMinigame) {
			FishingParticleListener.allowFishingParticles(player);

			// Remove the player from the mini-game map 2 ticks later as a safety precaution.
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mPlayerMinigameMap.remove(player), 2);
		}
	}

	private Location findWaterSurface(Location location) {
		location.setY(Math.floor(location.getY()));
		while (!(location.getBlock().getType() == Material.AIR)) {
			location.setY(location.getY() + 1);
		}
		return location;
	}

	private void suppressLeftClick(Player player) {
		mLeftClickSuppression.add(player);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mLeftClickSuppression.remove(player), 1);
	}

	private @Nullable Bait getBaitIfItemMatches(@Nullable String itemName) {
		for (Bait bait : Bait.values()) {
			if (Objects.equals(itemName, bait.mItemName)) {
				return bait;
			}
		}
		return null;
	}

	public static ItemStack getLesserChest() {
		return ChestUtils.giveChestWithLootTable(
			LESSER_LOOT_TABLE,
			"Architect's Lesser Fishing Cache",
			"#26ABBD",
			List.of(
				Component.text(
					"Moderate loot from the Architect's Ocean.",
					NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
			)
		);
	}

	public static ItemStack getGreaterChest() {
		return ChestUtils.giveChestWithLootTable(
			GREATER_LOOT_TABLE,
			"Architect's Greater Fishing Cache",
			"#26ABBD",
			List.of(
				Component.text(
					"Plentiful loot from the Architect's Ocean.",
					NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
			)
		);
	}

	public static ItemStack getAbyssalChest(int tier) {
		return ChestUtils.giveChestWithLootTable(
			ABYSSAL_LOOT_TABLE + tier,
			"Architect's Abyssal Fishing Cache " + StringUtils.toRoman(tier),
			"#26ABBD",
			List.of(
				Component.text(
					"Untold loot from the Architect's Ocean.",
					NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
			)
		);
	}

	private @Nullable ItemStack getNextFish(Player player, String previousFishName, int newQuality) {
		StringBuilder lootTablePath = new StringBuilder("epic:r3/items/fishing/fish/t" + newQuality + "/");
		String[] previousFishNameSplit = previousFishName.split(" ");
		for (int i = 1; i < previousFishNameSplit.length - 1; i++) {
			lootTablePath.append(previousFishNameSplit[i].toLowerCase(Locale.getDefault())).append("_");
		}
		lootTablePath.append(previousFishNameSplit[previousFishNameSplit.length - 1].toLowerCase(Locale.getDefault()));
		return InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(lootTablePath.toString()));
	}

	private void modifyAndAssessFishQuality(Player player, ItemStack fishItem, @Nullable BaitInfo baitInfo) {
		StatTrackFishCaught.fishCaught(player);
		int quality = ItemStatUtils.getFishQuality(fishItem);
		if (quality < 5) {
			double increaseChance = Plugin.getInstance().mEffectManager.getFishQualityIncrease(player);
			if (baitInfo != null) {
				increaseChance *= 1 + baitInfo.mBait.mQualityIncreaseOdds;
			}
			if (increaseChance > 1 && FastUtils.randomDoubleInRange(0, 1) <= increaseChance - 1) {
				quality++;
				new PartialParticle(Particle.ELECTRIC_SPARK, player.getEyeLocation(), 20).delta(1, 0.5, 1).spawnForPlayer(ParticleCategory.OWN_ACTIVE, player);
				ItemStack nextFish = getNextFish(player, ItemUtils.getPlainName(fishItem), quality);
				if (nextFish == null) {
					return;
				}
				fishItem.setItemMeta(nextFish.getItemMeta());
			}
		}

		if (quality == 5) {
			fiveStarAesthetics(player);
		}
	}

	public static void fiveStarAesthetics(Player player) {
		player.playSound(player, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1f, 1.5f);
		new PartialParticle(Particle.REDSTONE, player.getEyeLocation(), 30).delta(1, 0.5, 1).data(new Particle.DustOptions(Color.fromRGB(255, 200, 70), 0.9f)).spawnForPlayer(ParticleCategory.OWN_ACTIVE, player);
	}

	public @Nullable BaitInfo getFirstBait(Player player) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getSize(); i++) {
			ItemStack item = inventory.getItem(i);
			@Nullable String itemName = ItemUtils.getPlainNameIfExists(item);
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			Bait bait = getBaitIfItemMatches(itemName);
			if (bait != null) {
				return new BaitInfo(bait, item);
			}
		}
		return null;
	}

	private boolean shouldReplaceCaughtFish(BaitInfo baitInfo) {
		return Math.random() <= baitInfo.mBait.mReplacementChance;
	}

	private static class BaitInfo {
		final Bait mBait;
		final ItemStack mItemStack;

		public BaitInfo(Bait bait, ItemStack itemStack) {
			mBait = bait;
			mItemStack = itemStack;
		}
	}
}
