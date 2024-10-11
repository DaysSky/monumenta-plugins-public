package com.playmonumenta.plugins.gallery;

import com.playmonumenta.plugins.gallery.interactables.BaseInteractable;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NmsUtils;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GalleryUtils {

	//TODO - for future me - change this jank to a map inside GalleryMap different for each map.
	protected static final int GOLD_ROUND_1 = 0;
	protected static final int GOLD_PER_MOB_AFTER_ROUND_1 = 60;
	protected static final int STARTING_ROUND_FOR_SCALING = 20;
	protected static final int STARTING_ROUND_FOR_SCALING_HARDER = 45;
	private static final double HEALTH_SCALE_PER_ROUND = 0.05;
	private static final double HEALTH_SCALE_PER_ROUND_HARDER = 0.1;
	private static final double SPEED_SCALE_PER_ROUND = 0.01;
	private static final double MAX_SPEED_SCALE = 0.15;
	private static final double[] MOB_COUNT_MULTIPLIER_PER_PLAYER = {1, 1.33, 1.67, 2};
	private static final double[] MOB_HEALTH_MULTIPLIER_PER_PLAYER = {0.25, 0.4, 0.55, 0.75};

	//https://www.desmos.com/calculator/9lvoxpwcnw graph of the mobs scaling
	public static int getMobsCountForRound(int round, int players) {
		int count = 0;

		count += (int) Math.round(Math.pow(5f / 13f, -Math.sqrt(1 / 13f)) * Math.pow(round, 5f / 13f) + 6);
		if (round >= 13) {
			count += (int) Math.round(Math.pow((5f / 13f) * (round - 12), 1 + 5f / 8f));
		}
		if (round >= 20) {
			count += (int) Math.round(Math.sqrt(round - 20));
		}

		count = (int) (count * MOB_COUNT_MULTIPLIER_PER_PLAYER[players - 1]);
		return count;
	}

	public static int getGold(int currentRound, int playerCount) {
		if (currentRound == 1) {
			return GOLD_ROUND_1;
		}
		int oldMobs = getMobsCountForRound(currentRound - 1, playerCount);
		return oldMobs * GOLD_PER_MOB_AFTER_ROUND_1;

	}

	public static void scaleMobPerLevel(LivingEntity mob, int round) {
		if (round <= STARTING_ROUND_FOR_SCALING || mob == null || !mob.isValid()) {
			return;
		}

		double healthScale = getHealthScaleForLevel(round);

		double speedScale = Math.min(SPEED_SCALE_PER_ROUND * (round - STARTING_ROUND_FOR_SCALING), MAX_SPEED_SCALE);

		EntityUtils.scaleMaxHealth(mob, healthScale, "GalleryHealthScaleRound");
		EntityUtils.addAttribute(mob, Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier("GallerySpeedScaleRound", speedScale, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
	}

	public static void scaleMobPerPlayerCount(LivingEntity mob, int count) {
		if (count <= 1) {
			return;
		}
		EntityUtils.scaleMaxHealth(mob, getHealthScaleForPlayerCount(count), "GalleryHealthScalePlayers");
	}

	public static double getHealthScaleForLevel(int round) {
		if (round <= STARTING_ROUND_FOR_SCALING) {
			return 0;
		} else if (round <= STARTING_ROUND_FOR_SCALING_HARDER) {
			return HEALTH_SCALE_PER_ROUND * (round - STARTING_ROUND_FOR_SCALING);
		} else {
			return HEALTH_SCALE_PER_ROUND * (STARTING_ROUND_FOR_SCALING_HARDER - STARTING_ROUND_FOR_SCALING) + (HEALTH_SCALE_PER_ROUND_HARDER * (round - STARTING_ROUND_FOR_SCALING_HARDER));
		}
	}

	public static double getDamageScaleForLevel(int round) {
		if (round <= STARTING_ROUND_FOR_SCALING) {
			return 0;
		} else if (round <= STARTING_ROUND_FOR_SCALING_HARDER) {
			return (round - STARTING_ROUND_FOR_SCALING) * 0.1;
		} else {
			return (STARTING_ROUND_FOR_SCALING_HARDER - STARTING_ROUND_FOR_SCALING) * 0.1 + (round - STARTING_ROUND_FOR_SCALING_HARDER) * 0.2;
		}
	}

	public static double getHealthScaleForPlayerCount(int count) {
		return MOB_HEALTH_MULTIPLIER_PER_PLAYER[count - 1];
	}

	public static boolean isLookingAt(Player player, BaseInteractable interactable) {
		Vector playerDir = player.getEyeLocation().getDirection().normalize();
		Location origin = player.getLocation();
		Vector toInteractableVec = interactable.getLocation().toVector().subtract(origin.toVector()).normalize();
		return playerDir.dot(toInteractableVec) > 0.7 && LocationUtils.hasLineOfSight(player, interactable.getEntity());
	}

	public static void runCommandAsEntity(@NotNull Entity entity, @NotNull String command) {
		NmsUtils.getVersionAdapter().runConsoleCommandSilently("execute at " + entity.getUniqueId() + " as " + entity.getUniqueId() + " run " + command);
	}

	public static boolean canBuy(GalleryGame game, Integer value) {
		return game.getPlayersCoins() >= value;
	}

	public static void pay(GalleryGame game, Integer value) {
		int newValue = game.getPlayersCoins() - value;
		game.setPlayersCoins(newValue);
	}

	public static boolean isHoldingTrinket(Player player) {
		return player != null && player.isOnline() && InventoryUtils.testForItemWithName(player.getInventory().getItemInMainHand(), "Gallery Brochure", true);
	}

	public static void printDebugMessage(String message) {
		MMLog.warning("[Gallery] " + message);
	}

	public static void despawnMob(@NotNull LivingEntity mob) {
		GalleryGame game = GalleryManager.GAMES.get(mob.getWorld().getUID());

		if (game != null) {
			game.despawnMob(mob);
		} else {
			mob.remove();
			printDebugMessage("A mob that is not in a game has been otherwise despawned. This is probably a bug! World: " + mob.getWorld().getName() + ", Mob Type: " + mob.getType() + ", Mob Name:" + mob.getName());
		}
	}

	public static boolean isGalleryElite(@NotNull LivingEntity mob) {
		return mob.getScoreboardTags().contains(GalleryManager.TAG_MOB_ELITE);
	}

	public static boolean ignoreScaling(@NotNull LivingEntity mob) {
		return mob.getScoreboardTags().contains(GalleryManager.TAG_MOB_IGNORE_SCALING);
	}

	public static boolean isPlayerDeath(LivingEntity player) {
		GalleryGame game = GalleryManager.GAMES.get(player.getWorld().getUID());
		if (game != null) {
			GalleryPlayer gPlayer = game.getGalleryPlayer(player.getUniqueId());
			if (gPlayer != null) {
				return gPlayer.isDead();
			}
		}
		return false;

	}

	public static @Nullable GalleryGame getGame(@NotNull Location location) {
		return GalleryManager.GAMES.get(location.getWorld().getUID());
	}
}
