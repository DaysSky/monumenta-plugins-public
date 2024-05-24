package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.CelestialBlessingCS;
import com.playmonumenta.plugins.effects.Aesthetics;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;


public class CelestialBlessing extends Ability {

	private static final int CELESTIAL_COOLDOWN = 35 * 20;
	private static final int CELESTIAL_COOLDOWN_ENHANCED = 30 * 20;
	private static final int CELESTIAL_DURATION = 15 * 20;
	private static final double CELESTIAL_RADIUS = 12;
	private static final double CELESTIAL_1_EXTRA_DAMAGE = 0.20;
	private static final double CELESTIAL_2_EXTRA_DAMAGE = 0.35;
	private static final double CELESTIAL_EXTRA_SPEED = 0.20;
	private static final String ATTR_NAME = "CelestialBlessingExtraSpeedAttr";
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(
		DamageType.MELEE,
		DamageType.MELEE_SKILL,
		DamageType.MELEE_ENCH,
		DamageType.PROJECTILE,
		DamageType.PROJECTILE_SKILL
	);
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES_ENHANCE = EnumSet.of(
		DamageType.MELEE,
		DamageType.MELEE_SKILL,
		DamageType.MELEE_ENCH,
		DamageType.PROJECTILE,
		DamageType.PROJECTILE_SKILL,
		DamageType.MAGIC
	);

	public static final String DAMAGE_EFFECT_NAME = "CelestialBlessingExtraDamage";
	public static final String CHARM_DAMAGE = "Celestial Blessing Damage Modifier";
	public static final String CHARM_COOLDOWN = "Celestial Blessing Cooldown";
	public static final String CHARM_RADIUS = "Celestial Blessing Radius";
	public static final String CHARM_SPEED = "Celestial Blessing Speed Amplifier";
	public static final String CHARM_DURATION = "Celestial Blessing Duration";

	public static final AbilityInfo<CelestialBlessing> INFO =
		new AbilityInfo<>(CelestialBlessing.class, "Celestial Blessing", CelestialBlessing::new)
			.linkedSpell(ClassAbility.CELESTIAL_BLESSING)
			.scoreboardId("Celestial")
			.shorthandName("CB")
			.descriptions(
				("When you left-click while sneaking, you and all other players in a %s block radius gain +%s%% melee and " +
					"projectile damage and +%s%% speed for %ss. Dealing damage affected by this effect triggers Crusade to mark the enemy as undead, if applicable. Cooldown: %ss.")
					.formatted((long) CELESTIAL_RADIUS,
						StringUtils.multiplierToPercentage(CELESTIAL_1_EXTRA_DAMAGE),
						StringUtils.multiplierToPercentage(CELESTIAL_EXTRA_SPEED),
						StringUtils.ticksToSeconds(CELESTIAL_DURATION),
						StringUtils.ticksToSeconds(CELESTIAL_COOLDOWN)),
				"Increases the buff to +%s%% damage."
					.formatted(StringUtils.multiplierToPercentage(CELESTIAL_2_EXTRA_DAMAGE)),
				"Magic damage is now increased as well. Cooldown: %ss."
					.formatted(StringUtils.ticksToSeconds(CELESTIAL_COOLDOWN_ENHANCED)))
			.simpleDescription("Grant yourself and nearby players speed and increased damage.")
			.cooldown(CELESTIAL_COOLDOWN, CELESTIAL_COOLDOWN, CELESTIAL_COOLDOWN_ENHANCED, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", CelestialBlessing::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true)
				                                                                              .keyOptions(AbilityTrigger.KeyOptions.NO_PICKAXE)))
			.displayItem(Material.SUGAR);

	private final int mDuration;
	private final double mExtraDamage;
	private final EnumSet<DamageType> mAffectedDamageTypes;
	private final CelestialBlessingCS mCosmetic;

	private @Nullable Crusade mCrusade;

	public CelestialBlessing(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDuration = CharmManager.getDuration(player, CHARM_DURATION, CELESTIAL_DURATION);
		mExtraDamage = CharmManager.getLevelPercentDecimal(player, CHARM_DAMAGE) + (isLevelOne() ? CELESTIAL_1_EXTRA_DAMAGE : CELESTIAL_2_EXTRA_DAMAGE);
		mAffectedDamageTypes = isEnhanced() ? AFFECTED_DAMAGE_TYPES_ENHANCE : AFFECTED_DAMAGE_TYPES;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new CelestialBlessingCS());

		Bukkit.getScheduler().runTask(plugin, () -> mCrusade = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Crusade.class));
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		List<Player> affectedPlayers = PlayerUtils.playersInRange(mPlayer.getLocation(), CharmManager.getRadius(mPlayer, CHARM_RADIUS, CELESTIAL_RADIUS), true);

		// Don't buff players that have their class disabled
		affectedPlayers.removeIf(p -> p.getScoreboardTags().contains("disable_class"));

		// Give these players the metadata tag that boosts their damage
		for (Player p : affectedPlayers) {
			mPlugin.mEffectManager.addEffect(p, DAMAGE_EFFECT_NAME, new PercentDamageDealt(mDuration, mExtraDamage, mAffectedDamageTypes));
			mPlugin.mEffectManager.addEffect(p, "CelestialBlessingExtraSpeed", new PercentSpeed(mDuration, CELESTIAL_EXTRA_SPEED + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED), ATTR_NAME));
			mPlugin.mEffectManager.addEffect(p, "CelestialBlessingParticles", new Aesthetics(mDuration,
				(entity, fourHertz, twoHertz, oneHertz) -> {
					mCosmetic.tickEffect(mPlayer, p, fourHertz, twoHertz, oneHertz);
				},
				(entity) -> {
					mCosmetic.loseEffect(mPlayer, p);
				})
			);
			mCosmetic.startEffect(mPlayer, p);
		}

		putOnCooldown();
		return true;
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (mAffectedDamageTypes.contains(event.getType()) && mPlugin.mEffectManager.hasEffect(mPlayer, DAMAGE_EFFECT_NAME)) {
			Crusade.addCrusadeTag(enemy, mCrusade);
		}
		return false;
	}

}
