package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.EnumSet;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class ThunderAspect implements Enchantment {
	public static final double CHANCE = 0.1;
	public static final int DURATION_MELEE = 40;
	public static final int DURATION_PROJ = 10;
	public static final int DURATION_MELEE_ELITE = 10;
	public static final float BONUS_DAMAGE = 1;

	public static final String CHARM_STUN_CHANCE = "Thunder Aspect Stun Chance";

	private static final Particle.DustOptions COLOR_YELLOW = new Particle.DustOptions(Color.fromRGB(251, 231, 30), 1f);
	private static final Particle.DustOptions COLOR_FAINT_YELLOW = new Particle.DustOptions(Color.fromRGB(255, 241, 110), 1f);

	@Override
	public String getName() {
		return "Thunder Aspect";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.THUNDER_ASPECT;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	@Override
	public double getPriorityAmount() {
		return 14;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		DamageType type = event.getType();
		if (AbilityUtils.isAspectTriggeringEvent(event, player)) {
			// If used with arrow, must be critical
			if (event.getDamager() instanceof AbstractArrow arrow && !(arrow instanceof Trident) && !arrow.isCritical()) {
				return;
			}

			if (enemy instanceof IronGolem || enemy instanceof Guardian) {
				event.addUnmodifiableDamage(BONUS_DAMAGE);
			}

			if (event.getAbility() == ClassAbility.ERUPTION) {
				//Special case for eruption - always stun
				EntityUtils.applyStun(plugin, (int) (10 * level), enemy);
			} else if (!(type == DamageType.PROJECTILE || event.getAbility() == ClassAbility.EXPLOSIVE || event.getAbility() == ClassAbility.PREDATOR_STRIKE)) {
				apply(plugin, player, level, enemy, type == DamageType.MELEE);
			} else {
				double chance = (CHANCE + CharmManager.getLevelPercentDecimal(player, CHARM_STUN_CHANCE)) * level;
				if (FastUtils.RANDOM.nextDouble() < chance) {
					if (!EntityUtils.isElite(enemy) && !EntityUtils.isCCImmuneMob(enemy)) {
						EntityUtils.applyStun(plugin, DURATION_PROJ, enemy);
						Location halfHeightLocation = LocationUtils.getHalfHeightLocation(enemy);
						double widerWidthDelta = PartialParticle.getWidthDelta(enemy) * 1.5;
						// /particle dust 1 0.945 0.431 1 7053 78.9 7069 0.225 0.45 0.225 0 10
						PartialParticle partialParticle = new PartialParticle(
							Particle.REDSTONE,
							halfHeightLocation,
							10,
							widerWidthDelta,
							PartialParticle.getHeightDelta(enemy),
							widerWidthDelta,
							0,
							COLOR_FAINT_YELLOW
						).spawnAsEnemy();
						// /particle dust 0.984 0.906 0.118 1 7053 78.9 7069 0.225 0.45 0.225 0 10
						partialParticle.mExtra = 1;
						partialParticle.mData = COLOR_YELLOW;
						partialParticle.spawnAsEnemy();
						// /particle firework 7053 78.9 7069 0.225 0.45 0.225 0.5 0
						partialParticle.mParticle = Particle.FIREWORKS_SPARK;
						partialParticle.mCount = 15;
						partialParticle.mExtra = 0.4;
						partialParticle.mData = null;
						partialParticle.mDirectionalMode = true;
						partialParticle.mExtraVariance = 0.1;
						partialParticle.deltaVariance(true, false, true);
						partialParticle.mVaryPositiveY = true;
						partialParticle.spawnAsEnemy();

						World world = enemy.getWorld();
						Location enemyLocation = enemy.getLocation();
						world.playSound(enemyLocation, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 0.5f, 1.5f);
						world.playSound(enemyLocation, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, SoundCategory.PLAYERS, 0.5f, 1.2f);
					}
				}
			}
		}
	}

	public static void apply(Plugin plugin, Player player, double level, LivingEntity enemy, boolean particles) {
		Location loc = enemy.getLocation();
		World world = enemy.getWorld();
		if (particles) {
			world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 0.5f, 2.0f);
			world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.2f, 2.0f);
		}

		double chance = (CHANCE + CharmManager.getLevelPercentDecimal(player, CHARM_STUN_CHANCE)) * level * (particles ? player.getCooledAttackStrength(0) : 1);
		if (FastUtils.RANDOM.nextDouble() < chance) {
			if (EntityUtils.isElite(enemy)) {
				EntityUtils.applyStun(plugin, DURATION_MELEE_ELITE, enemy);
			} else {
				EntityUtils.applyStun(plugin, DURATION_MELEE, enemy);
			}

			if (particles && !EntityUtils.isCCImmuneMob(enemy)) {
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 0.65f, 1.5f);
				loc = loc.add(0, 1, 0);
				new PartialParticle(Particle.REDSTONE, loc, 12, 0.5, 0.5, 0.5, COLOR_YELLOW).spawnAsPlayerActive(player);
				new PartialParticle(Particle.REDSTONE, loc, 12, 0.5, 0.5, 0.5, COLOR_FAINT_YELLOW).spawnAsPlayerActive(player);
				new PartialParticle(Particle.FIREWORKS_SPARK, loc, 15, 0, 0, 0, 0.15).spawnAsPlayerActive(player);
			}
		}
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile projectile) {
		if (EntityUtils.isAbilityTriggeringProjectile(projectile, false)) {
			World world = player.getWorld();
			Location loc = player.getLocation();
			world.playSound(loc, Sound.ENTITY_PLAYER_HURT_ON_FIRE, SoundCategory.PLAYERS, 0.5f, 2.0f);
			world.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 0.9f, 2.0f);
			world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.35f, 2.0f);
		}
	}
}
