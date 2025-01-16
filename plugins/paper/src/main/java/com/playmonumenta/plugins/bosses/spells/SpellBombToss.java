package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpellBombToss extends Spell {

	@FunctionalInterface
	public interface ExplodeAction {
		/**
		 * Optional custom explosion code to replace the TNT explosion
		 *
		 * @param tnt TNT entity at the explosion (useful for line of sight calculations)
		 * @param loc Location of the explosion
		 */
		void run(World world, TNTPrimed tnt, Location loc);
	}

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRange;
	private final int mYield;
	private final int mLobs;
	private final int mFuse;
	private final boolean mSetFire;
	private final boolean mBreakBlocks;
	private final @Nullable ExplodeAction mExplodeAction;
	private final int mCooldown;

	private final List<TNTPrimed> mTNTList = new ArrayList<TNTPrimed>();

	public SpellBombToss(Plugin plugin, LivingEntity boss, int range) {
		this(plugin, boss, range, 4, 1, 50, false, true);
	}

	public SpellBombToss(Plugin plugin, LivingEntity boss, int range, int yield, int lobs, int fuse, boolean setFire, boolean breakBlocks) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mYield = yield;
		mLobs = lobs;
		mFuse = fuse;
		mSetFire = setFire;
		mBreakBlocks = breakBlocks;
		mExplodeAction = null;
		mCooldown = 160;
	}

	public SpellBombToss(Plugin plugin, LivingEntity boss, int range, int lobs, int fuse, ExplodeAction explodeAction) {
		this(plugin, boss, range, lobs, fuse, 160, explodeAction);
	}

	public SpellBombToss(Plugin plugin, LivingEntity boss, int range, int lobs, int fuse, int cooldown, ExplodeAction explodeAction) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mYield = 0;
		mLobs = lobs;
		mFuse = fuse;
		mSetFire = false;
		mBreakBlocks = false;
		mExplodeAction = explodeAction;
		mCooldown = cooldown;
	}

	@Override
	public void run() {
		// Choose random player within range that has line of sight to boss
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mRange, false);

		BukkitRunnable task = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				mTNTList.clear();

				// TODO: Add particles
				Collections.shuffle(players);
				for (Player player : players) {
					if (!player.getGameMode().equals(GameMode.CREATIVE) && LocationUtils.hasLineOfSight(mBoss, player)) {
						launch(player);
						break;
					}
				}
				if (mTicks >= mLobs) {
					this.cancel();
				}
			}

		};

		task.runTaskTimer(mPlugin, 0, 15);
		mActiveRunnables.add(task);
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

	@Override
	public void cancel() {
		super.cancel();

		for (Entity e : mTNTList) {
			if (e.isValid()) {
				e.remove();
			}
		}
		mTNTList.clear();
	}

	public void launch(Player target) {
		Location sLoc = mBoss.getEyeLocation();
		sLoc.getWorld().playSound(sLoc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1, 1);
		try {
			final var tnt = sLoc.getWorld().spawn(sLoc, TNTPrimed.class, e -> {
				e.setFuseTicks(mFuse);
			});

			mTNTList.add(tnt);
			// Dummy TNT
			tnt.setYield(0);
			Location pLoc = target.getLocation();
			Location tLoc = tnt.getLocation();
			Vector vect = new Vector(pLoc.getX() - tLoc.getX(), 0, pLoc.getZ() - tLoc.getZ());
			vect.normalize();
			if (!Double.isFinite(vect.getX())) {
				vect = new Vector(0, 1, 0);
			}
			vect.multiply(pLoc.distance(tLoc) / 20).setY(0.7f);
			tnt.setVelocity(vect);

			int delay = mFuse % 4;
			BukkitRunnable fuseSound = new BukkitRunnable() {
				TNTPrimed mTnt = tnt;
				int mCount = 0;

				@Override
				public void run() {
					if (mCount >= 3) {
						this.cancel();
						return;
					}

					mTnt.getWorld().playSound(mTnt.getLocation(), Sound.ENTITY_TNT_PRIMED, SoundCategory.HOSTILE, 1.5f, 1);

					mCount++;
				}
			};
			fuseSound.runTaskTimer(mPlugin, delay + (mFuse - delay) / 4, (mFuse - delay) / 4);
			mActiveRunnables.add(fuseSound);

			// Create explosion manually for proper damage calculations; source it at a mob entity and use the TNT location
			BukkitRunnable explosion = new BukkitRunnable() {
				TNTPrimed mTnt = tnt;

				@Override
				public void run() {
					if (mExplodeAction == null) {
						mBoss.getLocation().getWorld().createExplosion(mTnt.getLocation(), mYield, mSetFire, mBreakBlocks, mBoss);
					} else {
						mExplodeAction.run(mTnt.getWorld(), mTnt, mTnt.getLocation());
					}
				}
			};
			explosion.runTaskLater(mPlugin, mFuse);
			mActiveRunnables.add(explosion);
		} catch (Exception e) {
			mPlugin.getLogger().warning("Failed to summon TNT for bomb toss: " + e.getMessage());
			e.printStackTrace();
		}
	}


}
