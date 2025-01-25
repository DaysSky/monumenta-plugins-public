package com.playmonumenta.plugins.abilities.warrior.guardian;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.warrior.CounterStrike;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warrior.guardian.ChallengeCS;
import com.playmonumenta.plugins.effects.ChallengeMobEffect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Challenge extends Ability {

	private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "ChallengePercentDamageDealtEffect";
	private static final String SPEED_EFFECT_NAME = "ChallengePercentSpeedEffect";
	private static final String AFFECTED_MOB_EFFECT_NAME = "ChallengeMobEffect";
	private static final int DURATION = 20 * 10;
	private static final double PERCENT_DAMAGE_DEALT_EFFECT_1 = 0.2;
	private static final double PERCENT_DAMAGE_DEALT_EFFECT_2 = 0.3;
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = DamageType.getAllMeleeTypes();

	private static final int ABSORPTION_PER_MOB_1 = 1;
	private static final int ABSORPTION_PER_MOB_2 = 2;
	private static final int MAX_ABSORPTION_1 = 4;
	private static final int MAX_ABSORPTION_2 = 8;
	private static final int CHALLENGE_RANGE = 14;
	private static final int COOLDOWN = 20 * 20;
	private static final int KILLED_MOBS_CAP = 6;
	private static final double SPEED_PER = 0.04;
	private static final int CDR_PER = 10;

	public static final String CHARM_DURATION = "Challenge Duration";
	public static final String CHARM_DAMAGE = "Challenge Damage";
	public static final String CHARM_ABSORPTION_PER = "Challenge Absorption Health Per Mob";
	public static final String CHARM_ABSORPTION_MAX = "Challenge Max Absorption Health";
	public static final String CHARM_SPEED_PER = "Challenge Speed Per Mob";
	public static final String CHARM_CDR_PER = "Challenge Cooldown Reduction Per Mob";
	public static final String CHARM_RANGE = "Challenge Range";
	public static final String CHARM_COOLDOWN = "Challenge Cooldown";
	public static final String CHARM_MAX_MOBS = "Challenge Max Mobs";

	public static final AbilityInfo<Challenge> INFO =
		new AbilityInfo<>(Challenge.class, "Challenge", Challenge::new)
			.linkedSpell(ClassAbility.CHALLENGE)
			.scoreboardId("Challenge")
			.shorthandName("Ch")
			.descriptions(
				("Left-clicking while sneaking makes all enemies within %s blocks target you. " +
					"You gain %s Absorption per affected mob (up to %s Absorption) for %s seconds and +%s%% melee damage for %s seconds. Cooldown: %ss.")
					.formatted(CHALLENGE_RANGE, ABSORPTION_PER_MOB_1, MAX_ABSORPTION_1, StringUtils.ticksToSeconds(DURATION),
						StringUtils.multiplierToPercentage(PERCENT_DAMAGE_DEALT_EFFECT_1),
						StringUtils.ticksToSeconds(DURATION), StringUtils.ticksToSeconds(COOLDOWN)),
				"You gain %s Absorption per affected mob (up to %s Absorption) and +%s%% melee damage instead. When %s affected mobs (or all if there are fewer) are dead, for each mob, gain +%s%% speed for %s seconds and reduce the cooldown of Guardian skills by %s seconds."
					.formatted(ABSORPTION_PER_MOB_2, MAX_ABSORPTION_2, StringUtils.multiplierToPercentage(PERCENT_DAMAGE_DEALT_EFFECT_2), KILLED_MOBS_CAP, StringUtils.multiplierToPercentage(SPEED_PER), StringUtils.ticksToSeconds(DURATION), StringUtils.ticksToSeconds(CDR_PER)))
			.simpleDescription("Taunt all mobs around you, gaining absorption and damage.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", Challenge::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true)))
			.displayItem(Material.IRON_AXE);

	private final double mPercentDamageDealtEffect;
	private final double mAbsorptionPerMob;
	private final double mMaxAbsorption;
	private final int mDuration;
	private @Nullable CounterStrike mCounterStrike;

	private List<LivingEntity> mAffectedEntities = new ArrayList<>();
	private int mKillCount = 0;

	private final ChallengeCS mCosmetic;

	public Challenge(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mPercentDamageDealtEffect = (isLevelOne() ? PERCENT_DAMAGE_DEALT_EFFECT_1 : PERCENT_DAMAGE_DEALT_EFFECT_2) + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE);
		mAbsorptionPerMob = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION_PER, isLevelOne() ? ABSORPTION_PER_MOB_1 : ABSORPTION_PER_MOB_2);
		mMaxAbsorption = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ABSORPTION_MAX, isLevelOne() ? MAX_ABSORPTION_1 : MAX_ABSORPTION_2);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ChallengeCS());
		Bukkit.getScheduler().runTask(plugin, () -> {
			mCounterStrike = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, CounterStrike.class);
		});
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		Location loc = mPlayer.getLocation();
		List<LivingEntity> mobs = new Hitbox.SphereHitbox(loc, CharmManager.getRadius(mPlayer, CHARM_RANGE, CHALLENGE_RANGE)).getHitMobs();
		mobs.removeIf(mob -> ScoreboardUtils.checkTag(mob, AbilityUtils.IGNORE_TAG));
		if (!mobs.isEmpty()) {
			AbsorptionUtils.addAbsorption(mPlayer, mAbsorptionPerMob * mobs.size(), mMaxAbsorption, mDuration);
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_DEALT_EFFECT_NAME, new PercentDamageDealt(mDuration, mPercentDamageDealtEffect, AFFECTED_DAMAGE_TYPES));

			World world = mPlayer.getWorld();
			mCosmetic.onCast(mPlayer, world, loc);

			mobs.stream().filter(mob -> mob instanceof Mob).forEach(mob -> {
				EntityUtils.applyTaunt(mob, mPlayer);
				if (mCounterStrike != null) {
					mCounterStrike.onTaunt(mob);
				}
			});
			if (isLevelTwo()) {
				clearAffectedEntities();
				mAffectedEntities = mobs;
				mobs.forEach(mob -> {
					mPlugin.mEffectManager.addEffect(mob, AFFECTED_MOB_EFFECT_NAME, new ChallengeMobEffect(60 * 20, this));
					mCosmetic.onCastEffect(mPlayer, world, mob.getLocation());
				});
			}

			putOnCooldown();
			return true;
		}
		return false;
	}

	public void incrementKills(LivingEntity mob) {
		mKillCount++;
		mCosmetic.killMob(mPlayer, mPlayer.getWorld(), mob.getLocation());
		if (mKillCount >= KILLED_MOBS_CAP + (int) CharmManager.getLevel(mPlayer, CHARM_MAX_MOBS) || mKillCount >= mAffectedEntities.size()) {
			double speed = mKillCount * (SPEED_PER + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED_PER));
			mPlugin.mEffectManager.addEffect(mPlayer, SPEED_EFFECT_NAME, new PercentSpeed(mDuration, speed, SPEED_EFFECT_NAME));

			int cdr = mKillCount * CharmManager.getDuration(mPlayer, CHARM_CDR_PER, CDR_PER);
			EnumSet.of(ClassAbility.CHALLENGE, ClassAbility.BODYGUARD, ClassAbility.SHIELD_WALL).forEach(ca -> mPlugin.mTimers.updateCooldown(mPlayer, ca, cdr));

			mCosmetic.maxMobs(mPlayer, mPlayer.getWorld(), mPlayer.getLocation());

			clearAffectedEntities();
		}
	}

	public void clearAffectedEntities() {
		mAffectedEntities.stream().filter(mob -> mob.isValid() && !mob.isDead()).forEach(mob -> mPlugin.mEffectManager.clearEffects(mob, AFFECTED_MOB_EFFECT_NAME));
		mAffectedEntities = new ArrayList<>();
		mKillCount = 0;
	}
}
