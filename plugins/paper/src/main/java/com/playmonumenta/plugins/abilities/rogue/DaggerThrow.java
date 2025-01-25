package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.DaggerThrowCS;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class DaggerThrow extends Ability {

	private static final String DAGGER_THROW_MOB_HIT_TICK = "HitByDaggerThrowTick";
	private static final int DAGGER_THROW_COOLDOWN = 12 * 20;
	private static final int DAGGER_THROW_RANGE = 8;
	private static final int DAGGER_THROW_1_DAMAGE = 4;
	private static final int DAGGER_THROW_2_DAMAGE = 8;
	private static final int DAGGER_THROW_DURATION = 10 * 20;
	private static final int DAGGER_THROW_SILENCE_DURATION = 2 * 20;
	private static final int DAGGER_THROW_DAGGERS = 3;
	private static final double DAGGER_THROW_1_VULN = 0.2;
	private static final double DAGGER_THROW_2_VULN = 0.4;
	private static final double DAGGER_THROW_VULN_ENHANCEMENT = 0.2;
	private static final double DAGGER_THROW_SPREAD = Math.toRadians(25);

	public static final String CHARM_DAMAGE = "Dagger Throw Damage";
	public static final String CHARM_COOLDOWN = "Dagger Throw Cooldown";
	public static final String CHARM_RANGE = "Dagger Throw Range";
	public static final String CHARM_VULN = "Dagger Throw Vulnerability Amplifier";
	public static final String CHARM_DAGGERS = "Dagger Throw Daggers";

	public static final AbilityInfo<DaggerThrow> INFO =
		new AbilityInfo<>(DaggerThrow.class, "Dagger Throw", DaggerThrow::new)
			.linkedSpell(ClassAbility.DAGGER_THROW)
			.scoreboardId("DaggerThrow")
			.shorthandName("DT")
			.descriptions(
				String.format("Sneak left click while holding two swords to throw three daggers which deal %s melee damage and gives each target %s%% Vulnerability for %s seconds. The daggers travel up to %s blocks. Cooldown: %ss.",
					DAGGER_THROW_1_DAMAGE,
					(int) (DAGGER_THROW_1_VULN * 100),
					DAGGER_THROW_DURATION / 20,
					DAGGER_THROW_RANGE,
					DAGGER_THROW_COOLDOWN / 20),
				String.format("The damage is increased to %s and the Vulnerability increased to %s%%.",
					DAGGER_THROW_2_DAMAGE,
					(int) (DAGGER_THROW_2_VULN * 100)),
				String.format("Targets are additionally silenced for %ss. Vulnerability is increased by %s%%.",
					DAGGER_THROW_SILENCE_DURATION / 20,
					(int) (DAGGER_THROW_VULN_ENHANCEMENT * 100)))
			.simpleDescription("Throw daggers that deal damage and apply vulnerability.")
			.cooldown(DAGGER_THROW_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", DaggerThrow::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true),
				AbilityTriggerInfo.HOLDING_TWO_SWORDS_RESTRICTION))
			.displayItem(Material.WOODEN_SWORD);

	private final double mDamage;
	private final double mVulnAmplifier;
	private final DaggerThrowCS mCosmetic;

	public DaggerThrow(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAGGER_THROW_1_DAMAGE : DAGGER_THROW_2_DAMAGE);
		mVulnAmplifier = (isLevelOne() ? DAGGER_THROW_1_VULN : DAGGER_THROW_2_VULN) + (isEnhanced() ? DAGGER_THROW_VULN_ENHANCEMENT : 0) + CharmManager.getLevelPercentDecimal(player, CHARM_VULN);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new DaggerThrowCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		Location startLoc = mPlayer.getEyeLocation();
		Vector dir = startLoc.getDirection();
		double range = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RANGE, DAGGER_THROW_RANGE);
		World world = mPlayer.getWorld();
		mCosmetic.daggerThrowEffect(world, startLoc, mPlayer);

		int daggers = DAGGER_THROW_DAGGERS + (int) CharmManager.getLevel(mPlayer, CHARM_DAGGERS);

		for (int a = (daggers / 2) * -1; a <= (daggers / 2); a++) {
			double totalSpread = (DAGGER_THROW_SPREAD * DAGGER_THROW_DAGGERS);
			double individualSpread = totalSpread / daggers;
			double angle = a * individualSpread;
			Vector newDir = new Vector(FastUtils.cos(angle) * dir.getX() + FastUtils.sin(angle) * dir.getZ(), dir.getY(), FastUtils.cos(angle) * dir.getZ() - FastUtils.sin(angle) * dir.getX());
			newDir.normalize();

			Location endLoc = LocationUtils.rayTraceToBlock(startLoc, newDir, range, loc -> mCosmetic.daggerHitBlockEffect(loc, mPlayer));
			for (LivingEntity mob : Hitbox.approximateCylinder(startLoc, endLoc, 0.7, true).accuracy(0.5).getHitMobs()) {
				if (!MetadataUtils.checkOnceThisTick(mPlugin, mob, DAGGER_THROW_MOB_HIT_TICK)) {
					continue;
				}
				mCosmetic.daggerHitEffect(world, startLoc, mob, mPlayer);
				DamageUtils.damage(mPlayer, mob, DamageType.MELEE_SKILL, mDamage, mInfo.getLinkedSpell(), true);
				EntityUtils.applyVulnerability(mPlugin, DAGGER_THROW_DURATION, mVulnAmplifier, mob);
				if (isEnhanced()) {
					EntityUtils.applySilence(mPlugin, DAGGER_THROW_SILENCE_DURATION, mob);
				}
			}
			mCosmetic.daggerParticle(startLoc, endLoc, mPlayer);
		}

		return true;
	}

}
