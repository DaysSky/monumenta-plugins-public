package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.HandOfLightCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.Nullable;

public class HandOfLight extends Ability {
	public static final int RANGE = 12;
	public static final int NEARBY_SPHERE_RANGE = 2;
	private static final double HEALING_ANGLE = 70; // (half) angle of the healing cone
	private static final double HEALING_DOT_ANGLE = Math.cos(Math.toRadians(HEALING_ANGLE));
	private static final int HEALING_1_COOLDOWN = 14 * 20;
	private static final int HEALING_2_COOLDOWN = 10 * 20;
	private static final int FLAT_1 = 4;
	private static final int FLAT_2 = 8;
	private static final double PERCENT_1 = 0.1;
	private static final double PERCENT_2 = 0.2;
	private static final int DAMAGE_PER_1 = 2;
	private static final int DAMAGE_PER_2 = 3;
	private static final int DAMAGE_MAX_1 = 10;
	private static final int DAMAGE_MAX_2 = 12;
	private static final int REGEN_LEVEL = 1;
	private static final int REGEN_DURATION = 4 * 20;
	private static final double ENHANCEMENT_COOLDOWN_REDUCTION_PER_4_HP_HEALED = 0.025;
	private static final double ENHANCEMENT_COOLDOWN_REDUCTION_MAX = 0.5;
	private static final int ENHANCEMENT_UNDEAD_STUN_DURATION = 10;

	public static final String CHARM_DAMAGE = "Hand of Light Damage";
	public static final String CHARM_COOLDOWN = "Hand of Light Cooldown";
	public static final String CHARM_RANGE = "Hand of Light Range";
	public static final String CHARM_HEALING = "Hand of Light Healing";

	public static final AbilityInfo<HandOfLight> INFO =
		new AbilityInfo<>(HandOfLight.class, "Hand of Light", HandOfLight::new)
			.linkedSpell(ClassAbility.HAND_OF_LIGHT)
			.scoreboardId("Healing")
			.shorthandName("HoL")
			.descriptions(
				String.format("Right click while sneaking and holding a weapon or tool to heal all other players in a %s block cone in front of you or within %s blocks of you " +
					"for %s hearts + %s%% of their max health and gives them regen %s for %s seconds. " +
					"Additionally, damage all mobs in the area with magic damage equal to %s times the number of undead mobs in the range, up to %s damage. " +
					"If holding a shield, the trigger is changed to sneak + right click. Cooldown: %ss.",
					RANGE,
					NEARBY_SPHERE_RANGE,
					FLAT_1 / 2,
					StringUtils.multiplierToPercentage(PERCENT_1),
					REGEN_LEVEL + 1,
					StringUtils.ticksToSeconds(REGEN_DURATION),
					DAMAGE_PER_1,
					DAMAGE_MAX_1,
					StringUtils.ticksToSeconds(HEALING_1_COOLDOWN)),
				String.format("The healing is improved to %s hearts + %s%% of their max health. Damage is increased to %s damage per undead mob, up to %s. Cooldown: %ss.",
					FLAT_2 / 2,
					StringUtils.multiplierToPercentage(PERCENT_2),
					DAMAGE_PER_2,
					DAMAGE_MAX_2,
					StringUtils.ticksToSeconds(HEALING_2_COOLDOWN)),
				String.format("The cone is changed to a sphere of equal range, centered on the Cleric." +
					              " The cooldown is reduced by %s%% for each 4 health healed, capped at %s%% cooldown." +
					              " All Undead caught in the radius are stunned for %ss.",
					(int) (ENHANCEMENT_COOLDOWN_REDUCTION_PER_4_HP_HEALED * 100),
					(int) (ENHANCEMENT_COOLDOWN_REDUCTION_MAX * 100),
					ENHANCEMENT_UNDEAD_STUN_DURATION / 20.0
				))
			.simpleDescription("Heal all players in front of the Cleric, and damage all mobs based on the number of Undead in the area.")
			.cooldown(HEALING_1_COOLDOWN, HEALING_2_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", HandOfLight::cast, new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK).sneaking(true)
				                                                                        .keyOptions(AbilityTrigger.KeyOptions.SNEAK_WITH_SHIELD).keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS_EXCEPT_SHIELD)))
			.displayItem(Material.PINK_DYE);

	private final double mRange;
	private final int mFlat;
	private final double mPercent;
	private final double mDamagePer;
	private final double mDamageMax;

	private final HandOfLightCS mCosmetic;

	private @Nullable Crusade mCrusade;

	public HandOfLight(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRange = CharmManager.getRadius(mPlayer, CHARM_RANGE, RANGE);
		mFlat = isLevelOne() ? FLAT_1 : FLAT_2;
		mPercent = isLevelOne() ? PERCENT_1 : PERCENT_2;
		mDamagePer = isLevelOne() ? DAMAGE_PER_1 : DAMAGE_PER_2;
		mDamageMax = isLevelOne() ? DAMAGE_MAX_1 : DAMAGE_MAX_2;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new HandOfLightCS());

		Bukkit.getScheduler().runTask(plugin, () -> mCrusade = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Crusade.class));
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		World world = mPlayer.getWorld();
		Location userLoc = mPlayer.getLocation();

		Hitbox hitbox;
		if (!isEnhanced()) {
			hitbox = Hitbox.approximateCone(mPlayer.getEyeLocation(), mRange, Math.toRadians(HEALING_ANGLE))
				         .union(new Hitbox.SphereHitbox(mPlayer.getLocation(), NEARBY_SPHERE_RANGE));
		} else {
			hitbox = new Hitbox.SphereHitbox(mPlayer.getEyeLocation(), mRange);
		}
		List<LivingEntity> nearbyMobs = hitbox.getHitMobs();
		nearbyMobs.removeIf(mob -> mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));

		boolean doCooldown = false;
		List<LivingEntity> undeadMobs = new ArrayList<>(nearbyMobs);
		undeadMobs.removeIf(mob -> !Crusade.enemyTriggersAbilities(mob, mCrusade));
		if (isEnhanced()) {
			undeadMobs.forEach(mob -> EntityUtils.applyStun(mPlugin, ENHANCEMENT_UNDEAD_STUN_DURATION, mob));
			if (!undeadMobs.isEmpty()) {
				doCooldown = true;
			}
		}
		double damage = Math.min(undeadMobs.size() * mDamagePer, mDamageMax);
		damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage);
		double cooldown = getModifiedCooldown();

		if (damage > 0) {
			doCooldown = true;
			for (LivingEntity mob : nearbyMobs) {
				Location loc = mob.getLocation();
				DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true, true);
				mCosmetic.lightDamageEffect(mPlayer, loc, mob, undeadMobs);
			}
			mCosmetic.lightDamageCastEffect(world, userLoc, mPlugin, mPlayer, (float) mRange, !isEnhanced() ? HEALING_DOT_ANGLE : -1, nearbyMobs);
		}

		List<Player> nearbyPlayers = hitbox.getHitPlayers(mPlayer, true);
		nearbyPlayers.removeIf(p -> p.getScoreboardTags().contains("disable_class"));

		if (!nearbyPlayers.isEmpty()) {
			doCooldown = true;
			double healthHealed = 0;
			for (Player p : nearbyPlayers) {
				double maxHealth = EntityUtils.getMaxHealth(p);
				double healthBeforeHeal = p.getHealth();
				PlayerUtils.healPlayer(mPlugin, p, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEALING, mFlat + mPercent * maxHealth), mPlayer);
				healthHealed += p.getHealth() - healthBeforeHeal;

				Location loc = p.getLocation();
				mPlugin.mPotionManager.addPotion(p, PotionManager.PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.REGENERATION, REGEN_DURATION, REGEN_LEVEL, true, true));
				mCosmetic.lightHealEffect(mPlayer, loc, p);
			}

			mCosmetic.lightHealCastEffect(world, userLoc, mPlugin, mPlayer, (float) mRange, !isEnhanced() ? HEALING_DOT_ANGLE : -1, nearbyPlayers);

			if (isEnhanced()) {
				cooldown *= 1 - Math.min((healthHealed / 4) * ENHANCEMENT_COOLDOWN_REDUCTION_PER_4_HP_HEALED, ENHANCEMENT_COOLDOWN_REDUCTION_MAX);
			}
		}

		if (!doCooldown) {
			return false;
		}

		putOnCooldown((int) cooldown);
		return true;
	}
}
