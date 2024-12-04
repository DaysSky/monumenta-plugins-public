package com.playmonumenta.plugins.depths.bosses.spells.vesperidys;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * SpellBreakPlatform (Abyssal Overmind Spell):
 * Selects all platforms that players are nearest to, and in addition, breaks another extraBroken (int) more.
 * In total, the spell will break N+extraBroken amount of platforms (N is number of players).
 */
public class SpellBreakPlatform extends Spell {
	private final int BROKEN_DURATION = 20 * 20;
	private final int mCastTicks;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Vesperidys mVesperidys;

	private final int mTotalBroken;
	private boolean mOnCooldown = false;

	public SpellBreakPlatform(Plugin plugin, LivingEntity boss, Vesperidys vesperidys, int castTicks, int totalBroken) {
		mPlugin = plugin;
		mBoss = boss;
		mVesperidys = vesperidys;
		mCastTicks = castTicks;
		mTotalBroken = totalBroken;
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mOnCooldown = false;
		}, cooldownTicks() + 20 * 20);

		runSpell(mTotalBroken);

		if (mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 20) {
			BukkitRunnable runSpellLater = new BukkitRunnable() {
				@Override
				public void run() {
					runSpell(mTotalBroken / 2);
				}
			};

			runSpellLater.runTaskLater(mPlugin, 10 * 20);
			mActiveRunnables.add(runSpellLater);
		}
	}

	public void runSpell(int amount) {
		List<Player> hitPlayers = new ArrayList<>();

		// Select all players
		List<Player> players = PlayerUtils.playersInRange(mVesperidys.mSpawnLoc, Vesperidys.detectionRange, true);
		ArrayList<Vesperidys.Platform> platforms = new ArrayList<>();
		for (Player p : players) {
			Vesperidys.Platform platform = mVesperidys.mPlatformList.getPlatformNearestToEntity(p);
			if (!platforms.contains(platform)) {
				platforms.add(platform);
			}

			if (platforms.size() >= amount) {
				break;
			}
		}

		if (platforms.size() < amount) {
			int diff = amount - platforms.size();
			List<Vesperidys.Platform> randomPlatforms = mVesperidys.mPlatformList.getRandomPlatforms(platforms, diff);
			platforms.addAll(randomPlatforms);
		}

		BukkitRunnable runnableA = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT >= mCastTicks) {
					for (Vesperidys.Platform platform : platforms) {
						platform.destroy();
						for (Player player : platform.getPlayersOnPlatform()) {
							if (!hitPlayers.contains(player)) {
								damage(player, platform.getCenter());
								hitPlayers.add(player);
							}
						}

						int currentPhase = mVesperidys.mPhase;

						BukkitRunnable runnableB = new BukkitRunnable() {

							@Override public void run() {
								if (currentPhase < 4 && (currentPhase != mVesperidys.mPhase)) {
									this.cancel();
									return;
								}

								if (mVesperidys.mPhase < 4 || (mVesperidys.mPhase >= 4 && Math.abs(platform.mX) <= 1 && Math.abs(platform.mY) <= 1)) {
									if (mVesperidys.mFullPlatforms) {
										platform.generateFull();
									} else {
										platform.generateInner();
									}
								}
							}
						};

						runnableB.runTaskLater(mPlugin, BROKEN_DURATION);
					}
					this.cancel();
				} else {
					if (mT % (mCastTicks / 5) == 0) {
						for (Vesperidys.Platform platform : platforms) {
							mBoss.getWorld().playSound(platform.getCenter(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1.0f, 0.5f);
							new PartialParticle(Particle.BLOCK_CRACK, platform.getCenter().add(0, 1, 0), 100, 1.6, 0.2, 1.6, Material.DIRT.createBlockData()).spawnAsBoss();
							for (Block block : platform.mBlocks) {
								if (block.getType() != Material.NETHER_WART_BLOCK && block.getType() != Material.AIR) {
									if (FastUtils.RANDOM.nextInt(3) == 0 || mT >= (4 * mCastTicks / 5)) {
										block.setType(Material.NETHER_WART_BLOCK);
									}
								}
							}
						}
					}
				}
				mT += 2;
			}
		};

		runnableA.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnableA);
	}

	public void damage(Player player, Location location) {
		mVesperidys.dealPercentageAndCorruptionDamage(player, 0.5, "Platform Breaker");
		MovementUtils.knockAway(location, player, 0.5f, 0.75f, false);
	}

	@Override public boolean canRun() {
		return !mOnCooldown && !mVesperidys.mTeleportSpell.mTeleporting;
	}

	@Override public int cooldownTicks() {
		return 5 * 20;
	}
}
