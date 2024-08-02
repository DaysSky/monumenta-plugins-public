package com.playmonumenta.plugins.abilities.mage;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.mage.FrostNovaCS;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class FrostNova extends Ability {

	public static final String NAME = "Frost Nova";
	public static final ClassAbility ABILITY = ClassAbility.FROST_NOVA;

	public static final int DAMAGE_1 = 5;
	public static final int DAMAGE_2 = 10;
	public static final int SIZE = 7;
	public static final double SLOW_MULTIPLIER_1 = 0.2;
	public static final double SLOW_MULTIPLIER_2 = 0.4;
	public static final double ELITE_SLOW_MULTIPLIER_REDUCTION = 0.1;
	public static final double ENHANCED_DAMAGE_MODIFIER = 1.15;
	public static final int DURATION_TICKS = 4 * Constants.TICKS_PER_SECOND;
	public static final int ENHANCED_FROZEN_DURATION = 1 * Constants.TICKS_PER_SECOND;
	public static final int COOLDOWN_TICKS = 18 * Constants.TICKS_PER_SECOND;
	public static final int ENHANCED_COOLDOWN_TICKS = 16 * Constants.TICKS_PER_SECOND;

	public static final String CHARM_DAMAGE = "Frost Nova Damage";
	public static final String CHARM_COOLDOWN = "Frost Nova Cooldown";
	public static final String CHARM_RANGE = "Frost Nova Range";
	public static final String CHARM_SLOW = "Frost Nova Slowness Amplifier";
	public static final String CHARM_DURATION = "Frost Nova Slowness Duration";
	public static final String CHARM_FROZEN = "Frost Nova Frozen Duration";

	public static final AbilityInfo<FrostNova> INFO =
		new AbilityInfo<>(FrostNova.class, NAME, FrostNova::new)
			.linkedSpell(ABILITY)
			.scoreboardId("FrostNova")
			.shorthandName("FN")
			.descriptions(
				String.format(
					"While sneaking, left-clicking with a wand unleashes a frost nova," +
						" dealing %s ice magic damage to all enemies in a %s-block sphere around you," +
						" afflicting them with %s%% slowness for %ss, and extinguishing them if they're on fire." +
						" Slowness is reduced by %s%% on elites and bosses, and all players in the nova are also extinguished." +
						" The damage ignores iframes. Cooldown: %ss.",
					DAMAGE_1,
					SIZE,
					StringUtils.multiplierToPercentage(SLOW_MULTIPLIER_1),
					StringUtils.ticksToSeconds(DURATION_TICKS),
					StringUtils.multiplierToPercentage(ELITE_SLOW_MULTIPLIER_REDUCTION),
					StringUtils.ticksToSeconds(COOLDOWN_TICKS)
				),
				String.format(
					"Damage is increased from %s to %s. Base slowness is increased from %s%% to %s%%.",
					DAMAGE_1,
					DAMAGE_2,
					StringUtils.multiplierToPercentage(SLOW_MULTIPLIER_1),
					StringUtils.multiplierToPercentage(SLOW_MULTIPLIER_2)
				),
				String.format(
					"Damage is increased by %s%% and cooldown is reduced to %ss. Non elites and bosses are frozen for %ss, having their AI and gravity removed.",
					StringUtils.multiplierToPercentage(ENHANCED_DAMAGE_MODIFIER - 1),
					ENHANCED_COOLDOWN_TICKS / 20,
					StringUtils.ticksToSeconds(ENHANCED_FROZEN_DURATION)
				)
			)
			.simpleDescription("Damage and slow nearby mobs.")
			.cooldown(COOLDOWN_TICKS, COOLDOWN_TICKS, ENHANCED_COOLDOWN_TICKS, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", FrostNova::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true),
				AbilityTriggerInfo.HOLDING_MAGIC_WAND_RESTRICTION))
			.displayItem(Material.ICE);

	private final float mLevelDamage;
	private final double mLevelSlowMultiplier;

	private final FrostNovaCS mCosmetic;

	public FrostNova(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		double damage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
		mLevelDamage = (float) CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isEnhanced() ? damage * ENHANCED_DAMAGE_MODIFIER : damage);
		mLevelSlowMultiplier = (isLevelOne() ? SLOW_MULTIPLIER_1 : SLOW_MULTIPLIER_2) + CharmManager.getLevelPercentDecimal(player, CHARM_SLOW);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new FrostNovaCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();
		float damage = SpellPower.getSpellDamage(mPlugin, mPlayer, mLevelDamage);
		int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION_TICKS);
		double size = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, SIZE);
		Hitbox hitbox = new Hitbox.SphereHitbox(LocationUtils.getHalfHeightLocation(mPlayer), size);
		for (LivingEntity mob : hitbox.getHitMobs()) {
			if (EntityUtils.isElite(mob) || EntityUtils.isBoss(mob)) {
				EntityUtils.applySlow(mPlugin, duration, mLevelSlowMultiplier - ELITE_SLOW_MULTIPLIER_REDUCTION, mob);
			} else {
				EntityUtils.applySlow(mPlugin, duration, mLevelSlowMultiplier, mob);
				if (isEnhanced()) {
					int frozenDuration = CharmManager.getDuration(mPlayer, CHARM_FROZEN, ENHANCED_FROZEN_DURATION);
					EntityUtils.applyFreeze(mPlugin, frozenDuration, mob);
				}
			}
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true, false);

			if (mob.getFireTicks() > 1) {
				mob.setFireTicks(1);
			}
		}

		// Extinguish fire on all nearby players
		for (Player player : PlayerUtils.playersInRange(mPlayer.getLocation(), SIZE, true)) {
			if (player.getFireTicks() > 1) {
				player.setFireTicks(1);
			}
		}

		World world = mPlayer.getWorld();
		mCosmetic.onCast(mPlugin, mPlayer, world, size);

		return true;
	}

}
