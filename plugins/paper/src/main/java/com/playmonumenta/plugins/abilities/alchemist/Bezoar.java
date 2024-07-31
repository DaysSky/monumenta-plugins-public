package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.BezoarCS;
import com.playmonumenta.plugins.effects.CustomRegeneration;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class Bezoar extends Ability {
	private static final int FREQUENCY = 5;
	private static final double RADIUS = 16;
	private static final int LINGER_TIME = 10 * 20;
	private static final int DEBUFF_REDUCTION = 10 * 20;
	private static final int HEAL_DURATION = 2 * 20;
	private static final double HEAL_PERCENT = 0.05;
	private static final int DAMAGE_DURATION = 8 * 20;
	private static final double DAMAGE_PERCENT = 0.15;
	private static final int POTIONS = 1;

	private static final double PHILOSOPHER_STONE_BEZOAR_COUNT = 10;
	private static final int PHILOSOPHER_STONE_POTIONS = 3;
	public static final int PHILOSOPHER_STONE_ABSORPTION_AMOUNT = 4;
	public static final int PHILOSOPHER_STONE_ABSORPTION_DURATION = 16 * 20;

	public static final String CHARM_REQUIREMENT = "Bezoar Generation Requirement";
	public static final String CHARM_LINGER_TIME = "Bezoar Linger Duration";
	public static final String CHARM_DEBUFF_REDUCTION = "Bezoar Debuff Reduction";
	public static final String CHARM_HEAL_DURATION = "Bezoar Healing Duration";
	public static final String CHARM_HEALING = "Bezoar Healing";
	public static final String CHARM_DAMAGE_DURATION = "Bezoar Damage Duration";
	public static final String CHARM_DAMAGE = "Bezoar Damage Modifier";
	public static final String CHARM_PHILOSOPHER_STONE_RATE = "Bezoar Philosopher Stone Spawn Rate";
	public static final String CHARM_ABSORPTION = "Bezoar Absorption Health";
	public static final String CHARM_ABSORPTION_DURATION = "Bezoar Absorption Duration";
	public static final String CHARM_POTIONS = "Bezoar Potions";
	public static final String CHARM_PHILOSOPHER_STONE_POTIONS = "Bezoar Philosopher Stone Potions";
	public static final String CHARM_RADIUS = "Bezoar Radius";

	public static final AbilityInfo<Bezoar> INFO =
		new AbilityInfo<>(Bezoar.class, "Bezoar", Bezoar::new)
			.linkedSpell(ClassAbility.BEZOAR)
			.scoreboardId("Bezoar")
			.shorthandName("BZ")
			.descriptions(
				("Every %sth mob killed within %s blocks of the Alchemist spawns a Bezoar that lingers for %ss. " +
				"Picking up a Bezoar will grant the Alchemist an additional Alchemist Potion, " +
				"and will grant both the player who picks it up and the Alchemist a custom healing effect that " +
				"regenerates %s%% of max health every second for %ss and reduces the duration of all current " +
				"potion debuffs by %ss.")
					.formatted(
						FREQUENCY,
						StringUtils.to2DP(RADIUS),
						StringUtils.ticksToSeconds(LINGER_TIME),
						StringUtils.multiplierToPercentage(HEAL_PERCENT),
						StringUtils.ticksToSeconds(HEAL_DURATION),
						StringUtils.ticksToSeconds(DEBUFF_REDUCTION)
					),
				"The Bezoar now additionally grants +%s%% damage from all sources for %ss."
					.formatted(
						StringUtils.multiplierToPercentage(DAMAGE_PERCENT),
						StringUtils.ticksToSeconds(DAMAGE_DURATION)
					),
				("Every %s bezoars spawned, summon a Philosopher's Stone instead. " +
				"A Philosopher's Stone grants the Alchemist %s potions, and gives the same effects as bezoars, while also " +
				"granting %s absorption for %ss.")
					.formatted(
						StringUtils.to2DP(PHILOSOPHER_STONE_BEZOAR_COUNT),
						PHILOSOPHER_STONE_POTIONS,
						PHILOSOPHER_STONE_ABSORPTION_AMOUNT,
						StringUtils.ticksToSeconds(PHILOSOPHER_STONE_ABSORPTION_DURATION)
					)
			)
			.simpleDescription("Every few mobs that are killed nearby, spawn an item that can be picked up for damage and healing buffs.")
			.displayItem(Material.LIME_CONCRETE);

	private int mKills = 0;
	private int mBezoarsSpawned = 0;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private final int mLingerTime;

	private final BezoarCS mCosmetic;

	public Bezoar(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mLingerTime = CharmManager.getDuration(mPlayer, CHARM_LINGER_TIME, LINGER_TIME);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new BezoarCS());

		Bukkit.getScheduler().runTask(plugin, () -> {
			mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
		});
	}

	public void dropBezoar(EntityDeathEvent event) {
		mKills = 0;
		Location loc = event.getEntity().getLocation().add(0, 0.25, 0);
		// Every n bezoars spawned, it should spawn a philosopher stone instead.
		if (isEnhanced()) {
			mBezoarsSpawned++;
			if (mBezoarsSpawned >= PHILOSOPHER_STONE_BEZOAR_COUNT) {
				mBezoarsSpawned = 0;
				spawnPhilosopherStone(loc);
			} else {
				spawnBezoar(loc);
			}
		} else {
			spawnBezoar(loc);
		}
	}

	private void spawnBezoar(Location loc) {
		World world = loc.getWorld();
		Item item = spawnItem(world, loc, false);

		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				mT++;
				Location itemLoc = item.getLocation();
				mCosmetic.periodicBezoarEffects(mPlayer, itemLoc, mT, false);
				for (Player p : new Hitbox.UprightCylinderHitbox(itemLoc, 0.7, 0.7).getHitPlayers(true)) {
					if (p != mPlayer) {
						applyEffects(p, false);
						mCosmetic.targetEffects(p, itemLoc, false);
					}
					applyEffects(mPlayer, false);
					mCosmetic.targetEffects(mPlayer, itemLoc, false);

					if (mAlchemistPotions != null) {
						mAlchemistPotions.incrementCharges(POTIONS + (int) CharmManager.getLevel(mPlayer, CHARM_POTIONS));
					}

					item.remove();
					mCosmetic.pickupEffects(mPlayer, itemLoc, false);

					this.cancel();
					return;
				}

				if (mT >= mLingerTime || item.isDead()) {
					this.cancel();
					item.remove();
					mCosmetic.expireEffects(mPlayer, itemLoc, false);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}


	private void applyEffects(Player player, boolean isPhilosopherStone) {
		int debuffReduction = CharmManager.getDuration(mPlayer, CHARM_DEBUFF_REDUCTION, DEBUFF_REDUCTION);
		for (PotionEffectType effectType : PotionUtils.getNegativeEffects(mPlugin, player)) {
			PotionEffect effect = player.getPotionEffect(effectType);
			if (effect != null) {
				player.removePotionEffect(effectType);
				// No chance of overwriting and we don't want to trigger PotionApplyEvent for "upgrading" effects, so don't use PotionUtils here
				player.addPotionEffect(new PotionEffect(effectType, Math.max(effect.getDuration() - debuffReduction, 0), effect.getAmplifier()));
			}
		}

		// If the effects are from a philosopher stone, it should give them with triple the duration.
		double maxHealth = EntityUtils.getMaxHealth(player);
		int healDuration = CharmManager.getDuration(mPlayer, CHARM_HEAL_DURATION, HEAL_DURATION);
		mPlugin.mEffectManager.addEffect(player, "BezoarHealing", new CustomRegeneration(healDuration, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, maxHealth * HEAL_PERCENT), mPlayer, mPlugin));

		if (isLevelTwo()) {
			int damageDuration = CharmManager.getDuration(mPlayer, CHARM_DAMAGE_DURATION, DAMAGE_DURATION);
			mPlugin.mEffectManager.addEffect(player, "BezoarPercentDamageDealtEffect", new PercentDamageDealt(damageDuration, DAMAGE_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE)));
		}

		if (isPhilosopherStone) {
			AbsorptionUtils.addAbsorption(player, PHILOSOPHER_STONE_ABSORPTION_AMOUNT, PHILOSOPHER_STONE_ABSORPTION_AMOUNT, PHILOSOPHER_STONE_ABSORPTION_DURATION);
		}
	}

	private void spawnPhilosopherStone(Location loc) {
		World world = loc.getWorld();
		Item item = spawnItem(world, loc, true);

		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				mT++;
				Location itemLoc = item.getLocation();
				mCosmetic.periodicBezoarEffects(mPlayer, itemLoc, mT, true);
				for (Player p : PlayerUtils.playersInRange(itemLoc, 1, true)) {
					if (p != mPlayer) {
						applyPhilosopherEffects(p);
						mCosmetic.targetEffects(p, itemLoc, true);
					}
					applyPhilosopherEffects(mPlayer);
					mCosmetic.targetEffects(mPlayer, itemLoc, true);

					if (mAlchemistPotions != null) {
						mAlchemistPotions.incrementCharges(PHILOSOPHER_STONE_POTIONS + (int) CharmManager.getLevel(mPlayer, CHARM_PHILOSOPHER_STONE_POTIONS));
					}

					item.remove();
					mCosmetic.pickupEffects(mPlayer, itemLoc, true);

					this.cancel();
					return;
				}

				if (mT >= mLingerTime || item.isDead()) {
					this.cancel();
					item.remove();
					mCosmetic.expireEffects(mPlayer, itemLoc, true);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void applyPhilosopherEffects(Player player) {
		applyEffects(player, true);
	}

	private Item spawnItem(World world, Location loc, boolean philosophersStone) {
		return AbilityUtils.spawnAbilityItem(world, loc, mCosmetic.bezoarMat(philosophersStone), mCosmetic.bezoarName(philosophersStone), true, 0, true, true);
	}

	public boolean shouldDrop() {
		return mKills >= FREQUENCY + (int) CharmManager.getLevel(mPlayer, CHARM_REQUIREMENT);
	}

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (event.getEntity().getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
			return;
		}

		mKills++;
		if (shouldDrop()) {
			dropBezoar(event);
		}
	}

	@Override
	public double entityDeathRadius() {
		return CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
	}

}
