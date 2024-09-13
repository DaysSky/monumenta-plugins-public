package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpellBaseCharge extends Spell {
	@FunctionalInterface
	public interface WarningAction {
		/**
		 * Action to notify player when the boss starts the attack
		 * <p>
		 * Probably slow the boss down, particles, and a sound
		 *
		 * @param target Targeted LivingEntity
		 */
		void run(LivingEntity target);
	}

	@FunctionalInterface
	public interface WarningParticles {
		/**
		 * Particles to indicate the path of the boss's charge
		 *
		 * @param loc Location to spawn a particle
		 */
		void run(Location loc);
	}

	@FunctionalInterface
	public interface StartAction {
		/**
		 * Action run when the boss begins the attack
		 * Boss location will be the origin point
		 * <p>
		 * Probably Particles / sound
		 *
		 * @param target Targeted LivingEntity
		 */
		void run(LivingEntity target);
	}

	@FunctionalInterface
	public interface HitPlayerAction {
		/**
		 * Action to take when a player is hit by the boss charge
		 * <p>
		 * Probably particles, sound, and damage player
		 *
		 * @param target Hit LivingEntity
		 */
		void run(LivingEntity target);
	}

	@FunctionalInterface
	public interface ParticleAction {
		/**
		 * User function called many times per tick with the location where
		 * the boss's charge is drawn like a laser
		 *
		 * @param loc Location to spawn a particle
		 */
		void run(Location loc);
	}

	@FunctionalInterface
	public interface EndAction {
		/**
		 * Action to run on the boss when the attack is completed
		 * Boss location will be the end point
		 * <p>
		 * Probably just particles
		 */
		void run();
	}

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRange;
	private final int mCooldown;
	private final int mChargeTicks;
	private final @Nullable WarningAction mWarningAction;
	private final @Nullable ParticleAction mWarnParticleAction;
	private final @Nullable StartAction mStartAction;
	private final @Nullable HitPlayerAction mHitPlayerAction;
	private final @Nullable ParticleAction mParticleAction;
	private final @Nullable EndAction mEndAction;
	private final boolean mStopOnFirstHit;
	private final int mCharges;
	private final int mRate;
	private final double mYStartAdd;
	private final boolean mTargetFurthest;
	private final @Nullable GetSpellTargets<? extends LivingEntity> mTargets;

	public SpellBaseCharge(Plugin plugin, LivingEntity boss, int range, int chargeTicks,
	                       @Nullable WarningAction warning, @Nullable ParticleAction warnParticles, @Nullable StartAction start,
	                       @Nullable HitPlayerAction hitPlayer, @Nullable ParticleAction particle, EndAction end) {
		this(plugin, boss, range, 160, chargeTicks, false, 0, 0, 0, warning, warnParticles, start, hitPlayer, particle, end);
	}

	public SpellBaseCharge(Plugin plugin, LivingEntity boss, int range, int cooldown, int chargeTicks,
	                       @Nullable WarningAction warning, @Nullable ParticleAction warnParticles, @Nullable StartAction start,
	                       @Nullable HitPlayerAction hitPlayer, @Nullable ParticleAction particle, @Nullable EndAction end) {
		this(plugin, boss, range, cooldown, chargeTicks, false, 0, 0, 0, warning, warnParticles, start, hitPlayer, particle, end);
	}

	public SpellBaseCharge(Plugin plugin, LivingEntity boss, int range, int cooldown, int chargeTicks, boolean stopOnFirstHit,
	                       @Nullable WarningAction warning, @Nullable ParticleAction warnParticles, StartAction start,
	                       @Nullable HitPlayerAction hitPlayer, @Nullable ParticleAction particle, @Nullable EndAction end) {
		this(plugin, boss, range, cooldown, chargeTicks, stopOnFirstHit, 0, 0, 0, warning, warnParticles, start, hitPlayer, particle, end);
	}

	public SpellBaseCharge(Plugin plugin, LivingEntity boss, int range, int cooldown, int chargeTicks, boolean stopOnFirstHit,
	                       int charges, int rate, @Nullable WarningAction warning, @Nullable ParticleAction warnParticles, @Nullable StartAction start,
	                       @Nullable HitPlayerAction hitPlayer, @Nullable ParticleAction particle, EndAction end) {
		this(plugin, boss, range, cooldown, chargeTicks, stopOnFirstHit, charges, rate, 0, warning, warnParticles, start, hitPlayer, particle, end);
	}

	public SpellBaseCharge(Plugin plugin, LivingEntity boss, int range, int cooldown, int chargeTicks, boolean stopOnFirstHit, int charges, int rate, double yStartAdd,
	                       @Nullable WarningAction warning, @Nullable ParticleAction warnParticles, @Nullable StartAction start,
	                       @Nullable HitPlayerAction hitPlayer, @Nullable ParticleAction particle, @Nullable EndAction end) {
		this(plugin, boss, range, cooldown, chargeTicks, stopOnFirstHit, charges, rate, yStartAdd, false, warning, warnParticles, start, hitPlayer, particle, end);
	}

	public SpellBaseCharge(Plugin plugin, LivingEntity boss, int range, int cooldown, int chargeTicks, boolean stopOnFirstHit,
	                       int charges, int rate, double yStartAdd, boolean targetFurthest, @Nullable WarningAction warning,
	                       @Nullable ParticleAction warnParticles, @Nullable StartAction start, @Nullable HitPlayerAction hitPlayer, @Nullable ParticleAction particle,
	                       @Nullable EndAction end) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mChargeTicks = chargeTicks;
		mWarningAction = warning;
		mWarnParticleAction = warnParticles;
		mStartAction = start;
		mHitPlayerAction = hitPlayer;
		mParticleAction = particle;
		mEndAction = end;
		mStopOnFirstHit = stopOnFirstHit;
		mCharges = charges;
		mRate = rate;
		mYStartAdd = yStartAdd;
		mTargetFurthest = targetFurthest;
		mCooldown = cooldown;
		mTargets = null;
	}

	public SpellBaseCharge(Plugin plugin, LivingEntity boss, int cooldown, int chargeTicks, boolean stopOnFirstHit,
	                       int charges, int rate, double yStartAdd, @Nullable GetSpellTargets<LivingEntity> targets, @Nullable WarningAction warning,
	                       @Nullable ParticleAction warnParticles, @Nullable StartAction start, @Nullable HitPlayerAction hitPlayer, @Nullable ParticleAction particle, @Nullable EndAction end) {
		mPlugin = plugin;
		mBoss = boss;
		mChargeTicks = chargeTicks;
		mWarningAction = warning;
		mWarnParticleAction = warnParticles;
		mStartAction = start;
		mHitPlayerAction = hitPlayer;
		mParticleAction = particle;
		mEndAction = end;
		mStopOnFirstHit = stopOnFirstHit;
		mCharges = charges;
		mRate = rate;
		mYStartAdd = yStartAdd;
		mCooldown = cooldown;

		mTargets = targets;

		mTargetFurthest = false;
		mRange = 0;
	}

	@Override
	public void run() {
		LivingEntity target;
		List<? extends LivingEntity> bystanders;
		if (mTargets != null) {
			bystanders = mTargets.getTargets();
			if (bystanders.isEmpty()) {
				return;
			}
			target = bystanders.get(0);
		} else {
			// Get list of all nearby players who could be hit by the attack
			bystanders = PlayerUtils.playersInRange(mBoss.getLocation(), mRange * 2, true);

			// Choose random player within range that has line of sight to boss
			List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mRange, false);
			players.removeIf(p -> !LocationUtils.hasLineOfSight(mBoss, p));
			if (players.isEmpty()) {
				return;
			}
			Collections.shuffle(players);
			target = players.get(0);
			if (mTargetFurthest) {
				double distance = 0;
				for (Player player : players) {
					if (LocationUtils.hasLineOfSight(mBoss, player)) {
						if (mBoss.getLocation().distance(player.getLocation()) > distance) {
							distance = mBoss.getLocation().distance(player.getLocation());
							target = player;
						}
					}
				}
			}
		}

		if (mCharges <= 0 || mRate <= 0) {
			launch(target, bystanders);
		} else {
			launch(target, bystanders, mCharges, mRate);
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldown; // 8 seconds
	}

	/**
	 * Helper function for doCharge which checks if the charge is allowed to
	 * pass through a block. Needed because block.isSolid includes carpets
	 * @param block The block being checked
	 */
	public static boolean passable(Block block) {
		return block.isPassable() || !block.isSolid();
	}

	/**
	 * Does a charge attack - which may not do anything, depending on parameters passed
	 * Returns whether the charge hit a player or not
	 *
	 * @param target         The intended target of the attack
	 * @param charger        The living entity charging the player
	 * @param validTargets   Other targets (including the target!) who might be indicentally hit by the charge
	 * @param start          Action to run on boss at start location (may be null)
	 * @param particle       Action to spawn particle at locations along path (may be null)
	 * @param hitPlayer      Action to run if a player is hit (may be null)
	 * @param end            Action to run on boss at end location (may be null)
	 * @param teleBoss       Boolean indicating whether the boss should actually be teleported to the end
	 * @param stopOnFirstHit Boolean indicating whether the boss should damage only one player at a time
	 */
	public static boolean doCharge(LivingEntity target, Entity charger, Location targetLoc, List<? extends LivingEntity> validTargets, @Nullable StartAction start,
	                               @Nullable ParticleAction particle, @Nullable HitPlayerAction hitPlayer, @Nullable EndAction end, boolean teleBoss, boolean stopOnFirstHit, double yStartAdd) {
		final Location launLoc;
		if (charger instanceof LivingEntity le) {
			launLoc = le.getEyeLocation().add(0, yStartAdd, 0);
		} else {
			launLoc = charger.getLocation().add(0, yStartAdd, 0);
		}

		/* Test locations that are iterated in the loop */
		Location endLoc = launLoc.clone();
		Location endLoc1 = launLoc.clone().add(0, 1, 0); // Same as endLoc but one block higher

		Vector baseVect = new Vector(targetLoc.getX() - launLoc.getX(), targetLoc.getY() - launLoc.getY(), targetLoc.getZ() - launLoc.getZ());
		baseVect = baseVect.normalize().multiply(0.3);

		if (start != null) {
			start.run(target);
		}

		LivingEntity switchAggro = target;
		boolean chargeHitsPlayer = false;
		boolean cancel = false;
		BoundingBox box = charger.getBoundingBox();
		List<LivingEntity> hitEntities = new ArrayList<>();
		for (int i = 0; i < 200; i++) {
			box.shift(baseVect);
			endLoc.add(baseVect);
			endLoc1.add(baseVect);

			if (particle != null) {
				particle.run(endLoc);
			}

			// Check if the bounding box overlaps with any of the surrounding blocks
			for (int x = -1; x <= 1 && !cancel; x++) {
				for (int y = -1; y <= 1 && !cancel; y++) {
					for (int z = -1; z <= 1 && !cancel; z++) {
						Block block = endLoc.clone().add(x, y, z).getBlock();
						// If it overlaps with any, move it back to the last safe location
						// and terminate the charge before the block.
						if (block.getBoundingBox().overlaps(box) && !block.isLiquid()) {
							endLoc.subtract(baseVect);
							cancel = true;
						}
					}
				}
			}

			if (!cancel && (!passable(endLoc.getBlock()) || !passable(endLoc1.getBlock()))) {
				// No longer air - need to go back a bit so we don't tele the boss into a block
				endLoc.subtract(baseVect.multiply(1));
				// Charge terminated at a block
				break;
			} else if (launLoc.distance(endLoc) > (launLoc.distance(targetLoc) + 6.0f)) {
				// Reached end of charge without hitting anything
				break;
			}

			for (LivingEntity player : validTargets) {
				if (hitEntities.contains(player)) {
					continue;
				}
				if (player.getWorld() == charger.getWorld() && player.getLocation().distance(endLoc) < 1.8F) {
					// Hit player - mark this and continue
					chargeHitsPlayer = true;
					switchAggro = player;

					if (hitPlayer != null) {
						hitPlayer.run(player);
						hitEntities.add(player);
					}
					if (stopOnFirstHit) {
						cancel = true;
						break;
					}
				}
			}

			if (cancel) {
				break;
			}
		}

		if (teleBoss) {
			EntityUtils.teleportStack(charger, endLoc);
		}

		if (charger instanceof Mob mob && switchAggro != null) {
			mob.setTarget(switchAggro);
		}

		if (end != null) {
			end.run();
		}

		return chargeHitsPlayer;
	}

	private void launch(LivingEntity target, List<? extends LivingEntity> targets) {
		BukkitRunnable runnable = new BukkitRunnable() {
			private int mTicks = 0;
			final Location mTargetLoc = target.getEyeLocation();

			@Override
			public void run() {
				if (EntityUtils.shouldCancelSpells(mBoss)) {
					this.cancel();
					return;
				}
				if (mTicks == 0) {
					if (mWarningAction != null) {
						mWarningAction.run(target);
					}
				} else if (mTicks > 0 && mTicks < mChargeTicks) {
					// This runs once every other tick while charging
					doCharge(target, mBoss, mTargetLoc, targets, null, mWarnParticleAction, null, null, false, mStopOnFirstHit, mYStartAdd);
				} else if (mTicks >= mChargeTicks) {
					// Do the "real" charge attack
					doCharge(target, mBoss, mTargetLoc, targets, mStartAction, mParticleAction, mHitPlayerAction,
						mEndAction, true, mStopOnFirstHit, mYStartAdd);
					this.cancel();
					mActiveRunnables.remove(this);
				}

				mTicks += 2;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	private void launch(LivingEntity target, List<? extends LivingEntity> targets, int charges, int rate) {
		BukkitRunnable runnable = new BukkitRunnable() {
			private int mTicks = 0;
			private int mChargesDone = 0;
			@Nullable Location mTargetLoc;
			List<? extends LivingEntity> mBystanders = targets;
			LivingEntity mTarget = target;

			@Override
			public void run() {
				if (EntityUtils.shouldCancelSpells(mBoss)) {
					if (mBoss != null) {
						mBoss.setAI(true);
					}
					this.cancel();
					return;
				}
				if (mTicks == 0 || mTargetLoc == null) {
					mTargetLoc = mTarget.getEyeLocation();
					if (mWarningAction != null) {
						mWarningAction.run(target);
					}
				} else if (mTicks > 0 && mTicks < mChargeTicks) {
					// This runs once every other tick while charging
					doCharge(mTarget, mBoss, mTargetLoc, mBystanders, null, mWarnParticleAction, null, null, false, mStopOnFirstHit, mYStartAdd);
				} else if (mTicks >= mChargeTicks) {
					// Do the "real" charge attack
					doCharge(mTarget, mBoss, mTargetLoc, mBystanders, mStartAction, mParticleAction, mHitPlayerAction,
						mEndAction, true, mStopOnFirstHit, mYStartAdd);
					mChargesDone++;
					if (mChargesDone >= charges) {
						this.cancel();
						mActiveRunnables.remove(this);
					} else {
						if (mTargets != null) {
							// Get list of all nearby players who could be hit by the attack
							mBystanders = mTargets.getTargets();

							// Choose random player within range that has line of sight to boss
							for (LivingEntity entity : mBystanders) {
								mTarget = entity;
								mTargetLoc = mTarget.getEyeLocation();
								break;
							}
						}
						mTicks -= rate;
					}
				}

				mTicks += 2;
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}
}
