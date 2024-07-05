package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellBarrier extends Spell {

	public static final double CARAPACE_DAMAGE_MODIFIER = 1.3;

	@FunctionalInterface
	public interface RefreshBarrierAction {
		/**
		 * The action that runs when the barrier comes up
		 *
		 * @param loc The location to run the effect
		 */
		void run(Location loc);
	}

	@FunctionalInterface
	public interface BarrierRunningAmbientAction {
		/**
		 * The action that runs every other tick to create a circle of particles while the barrier is active
		 *
		 * @param loc The location to run the effect
		 */
		void run(Location loc);
	}

	@FunctionalInterface
	public interface BreakBarrierAction {
		/**
		 * The action to run when the barrier gets broken
		 *
		 * @param loc The location at which to run the effect
		 */
		void run(Location loc);
	}

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mActivationRadius;
	private final int mRechargeTime;
	private final int mHitsToBreak;
	private final boolean mIsCarapace;
	private final RefreshBarrierAction mRefreshAction;
	private final BarrierRunningAmbientAction mRunningAmbientAction;
	private final BreakBarrierAction mBreakAction;

	private int mCurrentHits = 0;
	private boolean mActive = false;
	private int mTimer = 0;

	public SpellBarrier(Plugin plugin, LivingEntity boss, int detectionRadius, int rechargeTime, int hitsToBreak, boolean isCarapace, RefreshBarrierAction refreshAction, BarrierRunningAmbientAction ambientRunningAction,
	                    BreakBarrierAction breakAction) {
		mPlugin = plugin;
		mBoss = boss;
		mActivationRadius = detectionRadius;
		mRechargeTime = rechargeTime;
		mHitsToBreak = hitsToBreak;
		mIsCarapace = isCarapace;
		mRefreshAction = refreshAction;
		mRunningAmbientAction = ambientRunningAction;
		mBreakAction = breakAction;
	}

	@Override
	public void run() {
		// Might as well not activate it outside of line of sight
		boolean hasLineOfSight = false;
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), mActivationRadius * 4, true)) {
			if (LocationUtils.hasLineOfSight(mBoss, player)) {
				hasLineOfSight = true;
				break;
			}
		}
		if (!hasLineOfSight) {
			return;
		}

		//Activates once every 5 ticks
		mTimer -= 5;
		if (!mActive && mTimer <= 0) {
			mTimer = mRechargeTime;
			mActive = true;
			Location loc = mBoss.getLocation();
			mRefreshAction.run(loc);
			BukkitRunnable runnable = new BukkitRunnable() {
				@Override
				public void run() {
					if (mBoss == null || mBoss.isDead() || !mBoss.isValid() || !mActive) {
						this.cancel();
					}
					mBoss.removePotionEffect(PotionEffectType.POISON);
					mBoss.removePotionEffect(PotionEffectType.WITHER);
					Location location = mBoss.getLocation();
					for (double i = 0; i < 360; i += 15) {
						double radian1 = Math.toRadians(i);
						location.add(FastUtils.cos(radian1), 0, FastUtils.sin(radian1));
						mRunningAmbientAction.run(location);
						location.subtract(FastUtils.cos(radian1), 0, FastUtils.sin(radian1));
					}

				}
			};
			runnable.runTaskTimer(mPlugin, 0, 20 * 1);
			mActiveRunnables.add(runnable);
		}
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (mActive) {
			Location loc = mBoss.getLocation();
			mCurrentHits++;
			if (mCurrentHits == mHitsToBreak) {
				mCurrentHits = 0;
				mBreakAction.run(loc);
				event.setCancelled(true);
				mActive = false;
				mTimer = mRechargeTime;
				return;
			}
			event.setCancelled(true);
			World world = mBoss.getWorld();
			world.playSound(loc, Sound.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 1, 1);
		}
	}

	@Override
	public void onDamage(DamageEvent event, LivingEntity damagee) {
		if (mActive && mIsCarapace) {
			event.setDamage(event.getFlatDamage() * CARAPACE_DAMAGE_MODIFIER);
		}
	}

	@Override
	public int castTicks() {
		return mRechargeTime;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
