package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellFinalHeatMech extends Spell {

	private final Plugin mPlugin;
	private double mT = 20 * 7;
	private final int mSoloCooldown = 20 * 29;
	private double mCooldown;
	private final double mMaxFactor = 1.7;
	private final Location mCenter;
	private final double mRange;
	private final LivingEntity mBoss;
	private boolean mTrigger = false;
	private boolean mDamage;
	private List<Player> mPlayers = new ArrayList<>();
	private final PartialParticle mFlame;
	private final PartialParticle mDmg;
	private final PartialParticle mExpL;

	public SpellFinalHeatMech(Plugin plugin, LivingEntity boss, Location loc, double range) {
		mPlugin = plugin;
		mBoss = boss;
		mCenter = loc;
		mRange = range;
		mFlame = new PartialParticle(Particle.FLAME, mBoss.getLocation(), 2, 0.25, .25, .25, 0.025);
		mDmg = new PartialParticle(Particle.DAMAGE_INDICATOR, mBoss.getLocation(), 4, 10, 1, 10, 0);
		mExpL = new PartialParticle(Particle.EXPLOSION_LARGE, mBoss.getLocation(), 200, 10, 1, 10, 0);
	}

	@Override
	public void run() {
		//update player count every 5 seconds
		if (!mTrigger) {
			mPlayers = Lich.playersInRange(mCenter, mRange, true);
			mTrigger = true;
			new BukkitRunnable() {

				@Override
				public void run() {
					mTrigger = false;
				}

			}.runTaskLater(mPlugin, 20 * 5);
		}
		//cooldown
		double cooldownFactor = Math.min(mMaxFactor, Math.sqrt(mPlayers.size()) / 5 + 0.8);
		mCooldown = mSoloCooldown / cooldownFactor;
		mT -= 5;
		if (mT <= 0) {
			mT += mCooldown;
			mDamage = false;
			World world = mBoss.getWorld();
			BukkitRunnable runA = new BukkitRunnable() {
				int mT = 0;

				@Override
				public void run() {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.HOSTILE, 5.0f, 0.8f);
					if (mT >= 20) {
						this.cancel();
						world.playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_DEATH, SoundCategory.HOSTILE, 5.0f, 1.0f);
						toss();
					}
					if (Lich.bossDead()) {
						this.cancel();
					}
					mT += 10;
				}

			};
			runA.runTaskTimer(mPlugin, 0, 10);
			mActiveRunnables.add(runA);
		}
	}

	private void toss() {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GHAST_SHOOT, SoundCategory.HOSTILE, 4, 1);
		List<Vector> vectors = new ArrayList<>();
		vectors.add(new Vector(0.22, 1.0, 0.22));
		vectors.add(new Vector(0.27, 0.95, 0.55));
		vectors.add(new Vector(0.55, 0.95, 0.27));

		for (int i = 0; i < 4; i++) {
			List<Vector> vec = new ArrayList<>(vectors);

			//rotate vectors
			for (int j = 0; j < vec.size(); j++) {
				Vector v = vec.get(j);
				Vector v2 = VectorUtils.rotateYAxis(v, i * 90);
				vec.set(j, v2);
			}

			for (Vector v : vec) {
				VectorUtils.rotateYAxis(v, 90 * i);
				FallingBlock fallingBlock = world.spawn(mBoss.getLocation().add(0, 5, 0), FallingBlock.class, b -> b.setBlockData(Material.MAGMA_BLOCK.createBlockData()));
				fallingBlock.setDropItem(false);
				EntityUtils.disableBlockPlacement(fallingBlock);
				fallingBlock.setVelocity(v);

				BukkitRunnable runB = new BukkitRunnable() {

					@Override
					public void run() {
						Location l = fallingBlock.getLocation();
						mFlame.location(l).spawnAsBoss();
						mDmg.location(mCenter.clone().add(0, 5.5, 0)).spawnAsBoss();
						if (fallingBlock.isOnGround() || !fallingBlock.isValid()) {
							this.cancel();
							fallingBlock.remove();
							world.playSound(l, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 1);
							damage();
						}
						if (Lich.bossDead()) {
							this.cancel();
							fallingBlock.remove();
						}
					}

				};
				runB.runTaskTimer(mPlugin, 0, 1);
				mActiveRunnables.add(runB);
			}
		}
	}

	private void damage() {
		//only damage once for all 12 falling blocks
		if (!mDamage) {
			mDamage = true;
			Location loc = mCenter.clone().add(0, 5.5, 0);
			BoundingBox box = BoundingBox.of(loc, 22, 2, 22);
			List<Player> players = Lich.playersInRange(mCenter, mRange, true);
			for (Player p : players) {
				if (p.getBoundingBox().overlaps(box)) {
					BossUtils.bossDamagePercent(mBoss, p, 0.5, "Malakut's Dynamo");
					AbilityUtils.increaseHealingPlayer(p, 20 * 30, -0.85, "Lich");
					MovementUtils.knockAway(mBoss, p, 0.5f, false);
					EntityUtils.applyFire(com.playmonumenta.plugins.Plugin.getInstance(), 5 * 20, p, mBoss);
				}
			}
			mExpL.location(loc).spawnAsBoss();
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
