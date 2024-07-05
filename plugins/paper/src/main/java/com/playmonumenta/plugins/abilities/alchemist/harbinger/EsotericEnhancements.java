package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.alchemist.PotionAbility;
import com.playmonumenta.plugins.bosses.bosses.abilities.AlchemicalAberrationBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger.EsotericEnhancementsCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class EsotericEnhancements extends Ability implements PotionAbility {
	private static final double ABERRATION_POTION_DAMAGE_MULTIPLIER_1 = 1;
	private static final double ABERRATION_POTION_DAMAGE_MULTIPLIER_2 = 1.5;
	private static final double ABERRATION_POTION_DAMAGE_RAW_1 = 6;
	private static final double ABERRATION_POTION_DAMAGE_RAW_2 = 7.5;
	private static final double ABERRATION_DAMAGE_RADIUS = 3;
	private static final int ABERRATION_SUMMON_DURATION = 30;
	private static final double ABERRATION_BLEED_AMOUNT = 0.2;
	private static final int ABERRATION_BLEED_DURATION = 4 * 20;
	private static final int ABERRATION_COOLDOWN = 5 * 20;
	private static final double ABERRATION_TARGET_RADIUS = 8;
	private static final int ABERRATION_LIFETIME = 15 * 20;
	private static final int TICK_INTERVAL = 5;
	private static final double MAX_TARGET_Y = 4;

	public static final String CHARM_DAMAGE = "Esoteric Enhancements Damage";
	public static final String CHARM_RADIUS = "Esoteric Enhancements Radius";
	public static final String CHARM_BLEED = "Esoteric Enhancements Bleed Amplifier";
	public static final String CHARM_DURATION = "Esoteric Enhancements Bleed Duration";
	public static final String CHARM_COOLDOWN = "Esoteric Enhancements Cooldown";
	public static final String CHARM_CREEPER = "Esoteric Enhancements Creeper";
	public static final String CHARM_REACTION_TIME = "Esoteric Enhancements Reaction Time";
	public static final String CHARM_FUSE = "Esoteric Enhancements Fuse Time";
	public static final String CHARM_SPEED = "Esoteric Enhancements Speed";

	public static final AbilityInfo<EsotericEnhancements> INFO =
		new AbilityInfo<>(EsotericEnhancements.class, "Esoteric Enhancements", EsotericEnhancements::new)
			.linkedSpell(ClassAbility.ESOTERIC_ENHANCEMENTS)
			.scoreboardId("Esoteric")
			.shorthandName("Es")
			.descriptions(
				("When afflicting a mob with a Brutal potion within %ss of afflicting that mob with a Gruesome potion, " +
				"summon an Alchemical Aberration. The Aberration targets the mob with the highest health within %s blocks " +
				"and explodes on that mob, dealing %s + %s%% of your potion damage and applying %s%% Bleed for %ss to all mobs " +
				"in a %s block radius. Cooldown: %ss.")
					.formatted(
							StringUtils.ticksToSeconds(ABERRATION_SUMMON_DURATION),
							StringUtils.to2DP(ABERRATION_TARGET_RADIUS),
							ABERRATION_POTION_DAMAGE_RAW_1,
							StringUtils.multiplierToPercentage(ABERRATION_POTION_DAMAGE_MULTIPLIER_1),
							StringUtils.multiplierToPercentage(ABERRATION_BLEED_AMOUNT),
							StringUtils.ticksToSeconds(ABERRATION_BLEED_DURATION),
							StringUtils.to2DP(ABERRATION_DAMAGE_RADIUS),
							StringUtils.ticksToSeconds(ABERRATION_COOLDOWN)
					),
				"Damage is increased to %s + %s%% of your potion damage."
					.formatted(
						ABERRATION_POTION_DAMAGE_RAW_2,
						StringUtils.multiplierToPercentage(ABERRATION_POTION_DAMAGE_MULTIPLIER_2)
					)
			)
			.simpleDescription("Stack the effects of gruesome and brutal on an enemy to summon a friendly creeper.")
			.cooldown(ABERRATION_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.CREEPER_HEAD);

	private final double mDamageMultiplier;
	private final double mDamageRaw;
	private final HashMap<LivingEntity, Integer> mAppliedMobs;
	private final EsotericEnhancementsCS mCosmetic;

	public EsotericEnhancements(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mAppliedMobs = new HashMap<>();

		mDamageMultiplier = isLevelOne() ? ABERRATION_POTION_DAMAGE_MULTIPLIER_1 : ABERRATION_POTION_DAMAGE_MULTIPLIER_2;
		mDamageRaw = isLevelOne() ? ABERRATION_POTION_DAMAGE_RAW_1 : ABERRATION_POTION_DAMAGE_RAW_2;

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new EsotericEnhancementsCS());
	}

	@Override
	public void apply(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {
		if (isGruesome) {
			mAppliedMobs.put(mob, mob.getTicksLived());
		} else if (!isOnCooldown()) {
			// Clear out list so it doesn't build up
			int reactionTime = CharmManager.getDuration(mPlayer, CHARM_REACTION_TIME, ABERRATION_SUMMON_DURATION);
			mAppliedMobs.entrySet().removeIf((entry) -> (entry.getKey().getTicksLived() - entry.getValue() > reactionTime));

			// If it's still in the list, it was applied recently enough
			if (mAppliedMobs.remove(mob) != null) {
				int num = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_CREEPER);
				for (int i = 0; i < num; i++) {
					summonAberration(mob.getLocation(), playerItemStats);
					mCosmetic.esotericSummonEffect(mob.getWorld(), mPlayer, mob.getLocation());
				}
				putOnCooldown();
			}
		}
	}

	private void summonAberration(Location loc, ItemStatManager.PlayerItemStats playerItemStats) {
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			Creeper aberration = (Creeper) LibraryOfSoulsIntegration.summon(loc, mCosmetic.getLos());
			if (aberration == null) {
				MMLog.warning("Failed to spawn Alchemical Aberration from Library of Souls");
				return;
			}
			EntityUtils.setRemoveEntityOnUnload(aberration);

			AlchemicalAberrationBoss alchemicalAberrationBoss = BossUtils.getBossOfClass(aberration, AlchemicalAberrationBoss.class);
			if (alchemicalAberrationBoss == null) {
				MMLog.warning("Failed to get AlchemicalAberrationBoss for AlchemicalAberration");
				return;
			}

			double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, ABERRATION_DAMAGE_RADIUS);
			double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, mDamageMultiplier * playerItemStats.getItemStats().get(AttributeType.POTION_DAMAGE.getItemStat()));
			damage += mDamageRaw;
			alchemicalAberrationBoss.spawn(mPlayer, damage, radius, CharmManager.getDuration(mPlayer, CHARM_DURATION, ABERRATION_BLEED_DURATION), ABERRATION_BLEED_AMOUNT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_BLEED), playerItemStats);

			aberration.setMaxFuseTicks(CharmManager.getDuration(mPlayer, CHARM_FUSE, aberration.getMaxFuseTicks()));
			aberration.setExplosionRadius((int) radius);
			EntityUtils.setAttributeBase(aberration, Attribute.GENERIC_MOVEMENT_SPEED, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SPEED, EntityUtils.getAttributeBaseOrDefault(aberration, Attribute.GENERIC_MOVEMENT_SPEED, 0)));
			if (isLevelTwo()) {
				aberration.setPowered(true);
			}

			new BukkitRunnable() {
				int mTicks = 0;
				@Nullable LivingEntity mTarget = null;
				@Override
				public void run() {
					if (mTicks >= ABERRATION_LIFETIME || !mPlayer.isOnline() || mPlayer.isDead() || !aberration.isValid()) {
						aberration.remove();
						this.cancel();
						return;
					}

					List<LivingEntity> inRadiusMobs = EntityUtils.getNearbyMobs(aberration.getLocation(), radius, aberration).stream()
						.filter(mob -> Math.abs(mob.getLocation().getY() - aberration.getLocation().getY()) <= MAX_TARGET_Y)
						.filter(mob -> isValidTarget(aberration, mob)).toList();
					if (!inRadiusMobs.isEmpty()) {
						mTarget = inRadiusMobs.stream().max(Comparator.comparingDouble(Damageable::getHealth)).orElse(null);
					}

					if (!isValidTarget(aberration, mTarget)) {
						LivingEntity newTarget = findTarget(aberration);
						if (newTarget != null) {
							mTarget = newTarget;
						}
					}

					if (mTarget != null && mTarget.isValid()) {
						aberration.setTarget(mTarget);
					}

					mTicks += TICK_INTERVAL;
				}
			}.runTaskTimer(mPlugin, 0, TICK_INTERVAL);

		}, 1);
	}

	private boolean isValidTarget(LivingEntity aberration, @Nullable LivingEntity mob) {
		return mob != null
			       && mob.isValid()
			       && mob.getLocation().distance(aberration.getLocation()) <= 1.5 * ABERRATION_TARGET_RADIUS
			       && Math.abs(mob.getLocation().getY() - aberration.getLocation().getY()) <= 2 * MAX_TARGET_Y
			       && !DamageUtils.isImmuneToDamage(mob, DamageEvent.DamageType.MAGIC)
			       && !mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG);
	}

	private @Nullable LivingEntity findTarget(LivingEntity aberration) {

		List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(aberration.getLocation(), ABERRATION_TARGET_RADIUS, aberration).stream()
			.filter(mob -> Math.abs(mob.getLocation().getY() - aberration.getLocation().getY()) <= MAX_TARGET_Y)
			.filter(mob -> isValidTarget(aberration, mob)).toList();

		List<LivingEntity> lineOfSightNearbyMobs = nearbyMobs.stream().filter(mob -> mob.hasLineOfSight(aberration)).toList();

		if (lineOfSightNearbyMobs.size() > 0) {
			return lineOfSightNearbyMobs.stream().max(Comparator.comparingDouble(Damageable::getHealth)).orElse(null);
		}
		return nearbyMobs.stream().max(Comparator.comparingDouble(Damageable::getHealth)).orElse(null);
	}

}
