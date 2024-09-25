package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.ViciousCombosCS;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class ViciousCombos extends Ability {

	private static final int VICIOUS_COMBOS_RANGE = 5;
	private static final int VICIOUS_COMBOS_COOL_1 = 20;
	private static final int VICIOUS_COMBOS_COOL_2 = 2 * 20;
	private static final int VICIOUS_COMBOS_CRIPPLE_DURATION = 5 * 20;
	private static final double VICIOUS_COMBOS_CRIPPLE_VULN_LEVEL = 0.15;
	private static final double VICIOUS_COMBOS_CRIPPLE_WEAKNESS_LEVEL = 0.15;
	private static final int ENHANCEMENT_COOLDOWN_REDUCTION = 1 * 20;
	private static final int ENHANCEMENT_CHARGE_LIFETIME = 3 * 20;
	private static final double ENHANCEMENT_DAMAGE_INCREASE = 0.2;

	public static final String CHARM_CDR = "Vicious Combos Cooldown Reduction";
	public static final String CHARM_RADIUS = "Vicious Combos Radius";
	public static final String CHARM_VULN = "Vicious Combos Vulnerability Amplifier";
	public static final String CHARM_WEAKEN = "Vicious Combos Weakness Amplifier";
	public static final String CHARM_DURATION = "Vicious Combos Duration";
	public static final String CHARM_DAMAGE_AMPLIFIER = "Vicious Combos Enhancement Damage Amplifier";

	public static final AbilityInfo<ViciousCombos> INFO =
		new AbilityInfo<>(ViciousCombos.class, "Vicious Combos", ViciousCombos::new)
			.linkedSpell(ClassAbility.VICIOUS_COMBOS)
			.scoreboardId("ViciousCombos")
			.shorthandName("VC")
			.descriptions(
				String.format("Passively, killing an enemy refreshes the cooldown of your abilities by %s second. Killing an Elite or Boss enemy instead resets the cooldown of your abilities.",
					VICIOUS_COMBOS_COOL_1 / 20),
				String.format("Killing an enemy now refreshes your ability cooldowns by %s seconds. Killing an Elite or Boss enemy inflicts nearby enemies within %s blocks with %s%% weaken and %s%% Vulnerability for %s seconds.",
					VICIOUS_COMBOS_COOL_2 / 20,
					VICIOUS_COMBOS_RANGE,
					(int) (VICIOUS_COMBOS_CRIPPLE_WEAKNESS_LEVEL * 100),
					(int) (VICIOUS_COMBOS_CRIPPLE_VULN_LEVEL * 100),
					VICIOUS_COMBOS_CRIPPLE_DURATION / 20),
				String.format("When an ability goes on cooldown, your next melee attack in %ss deals %s%% more melee damage and that ability's cooldown is refreshed by %ss, prioritizing the last ability.",
					ENHANCEMENT_CHARGE_LIFETIME / 20,
					(int) (ENHANCEMENT_DAMAGE_INCREASE * 100),
					ENHANCEMENT_COOLDOWN_REDUCTION / 20))
			.simpleDescription("Killing mobs reduces cooldowns, and killing elite mobs completely refresh them.")
			.quest216Message("-------n-------u-------")
			.displayItem(Material.ZOMBIE_HEAD);

	private @Nullable ClassAbility mLastAbility = null;
	private int mAbilityCastTime = 0;

	private final ViciousCombosCS mCosmetic;

	public ViciousCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ViciousCombosCS());
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		LivingEntity killedEntity = event.getEntity();

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			Location loc = killedEntity.getLocation();
			loc = loc.add(0, 0.5, 0);
			World world = mPlayer.getWorld();

			double radius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, VICIOUS_COMBOS_RANGE);
			if (EntityUtils.isElite(killedEntity) || EntityUtils.isBoss(killedEntity)) {
				mPlugin.mTimers.removeAllCooldowns(mPlayer);
				MessagingUtils.sendActionBarMessage(mPlayer, "All your cooldowns have been reset");

				if (isLevelTwo()) {
					int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, VICIOUS_COMBOS_CRIPPLE_DURATION);
					double vuln = VICIOUS_COMBOS_CRIPPLE_VULN_LEVEL + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_VULN);
					double weaken = VICIOUS_COMBOS_CRIPPLE_WEAKNESS_LEVEL + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKEN);
					for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, radius, mPlayer)) {
						new PartialParticle(Particle.SPELL_MOB, mob.getLocation().clone().add(0, 1, 0), 10, 0.35, 0.5, 0.35, 0).spawnAsPlayerActive(mPlayer);
						EntityUtils.applyVulnerability(mPlugin, duration, vuln, mob);
						EntityUtils.applyWeaken(mPlugin, duration, weaken, mob);
					}
				}
				mCosmetic.comboOnElite(world, loc, mPlayer, radius, killedEntity);

			} else if (EntityUtils.isHostileMob(killedEntity)) {
				int timeReduction = (isLevelOne() ? VICIOUS_COMBOS_COOL_1 : VICIOUS_COMBOS_COOL_2) + (int) (CharmManager.getLevel(mPlayer, CHARM_CDR) * 20);

				mPlugin.mTimers.updateCooldowns(mPlayer, timeReduction);
				mCosmetic.comboOnKill(world, loc, mPlayer, radius, killedEntity);
			}
		}, 1);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		// If:
		// Is Enhanced,
		// Is a Melee Attack,
		// LastAbility does exist
		// The LastAbility cast time is within charge's lifetime.
		if (isEnhanced()
			&& event.getType() == DamageEvent.DamageType.MELEE
			&& mLastAbility != null
			&& Bukkit.getServer().getCurrentTick() < mAbilityCastTime + ENHANCEMENT_CHARGE_LIFETIME) {
			double enhancementDamageMulti = ENHANCEMENT_DAMAGE_INCREASE + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_DAMAGE_AMPLIFIER);
			event.updateDamageWithMultiplier(1 + enhancementDamageMulti);
			mPlugin.mTimers.updateCooldown(mPlayer, mLastAbility, ENHANCEMENT_COOLDOWN_REDUCTION);

			// mPlayer.sendMessage(mLastAbility.getName() + " has been reduced!");

			clearState();
		}
		return false;
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		if (isEnhanced()) {
			// Run this 1 tick late to prevent ByMyBlade triggering it immediately.
			new BukkitRunnable() {
				@Override
				public void run() {
					// Get the index of the ability in mAbilities, add to the order.
					mLastAbility = event.getSpell();
					mAbilityCastTime = Bukkit.getServer().getCurrentTick();
					// mPlayer.sendMessage(mLastAbility.getName() + " is Selected");
				}
			}.runTaskLater(mPlugin, 1);
		}

		return true;
	}

	public void clearState() {
		mLastAbility = null;
		mAbilityCastTime = 0;
		// mPlayer.sendMessage("Cleared");
	}
}
