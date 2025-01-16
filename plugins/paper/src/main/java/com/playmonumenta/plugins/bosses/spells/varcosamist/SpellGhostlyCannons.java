package com.playmonumenta.plugins.bosses.spells.varcosamist;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellGhostlyCannons extends Spell {
	private final LivingEntity mBoss;
	private final Plugin mPlugin;
	private final double mRange;
	private final Location mCenter;
	private final boolean mPhaseThree;
	private final String mDio;
	private static final Particle.DustOptions CANNONS_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f);

	public SpellGhostlyCannons(Plugin plugin, LivingEntity boss, double range, Location center, boolean phaseThree, String dio) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mCenter = center;
		mPhaseThree = phaseThree;
		mDio = dio;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		PlayerUtils.nearbyPlayersAudience(mCenter, 50).sendMessage(Component.text(mDio, NamedTextColor.RED));

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks += 2;
				float fTick = mTicks;
				float ft = fTick / 25;
				new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 4, 0.35, 0, 0.35, 0.005).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.CRIT, mBoss.getLocation().add(0, 1, 0), 3, 0.3, 0, 0.3, 0.125).spawnAsEntityActive(mBoss);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.HOSTILE, 10, 0.5f + ft);
				if (mTicks >= 20 * 2) {
					this.cancel();
					mActiveRunnables.remove(this);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_SHOOT, SoundCategory.HOSTILE, 3, 0.5f);
					BukkitRunnable runnable = new BukkitRunnable() {

						int mI = 0;

						@Override
						public void run() {
							mI++;
							List<Player> players = PlayerUtils.playersInRange(mCenter, 24, true);
							Collections.shuffle(players);
							for (Player player : players) {
								Vector loc = player.getLocation().toVector();
								if (player.getLocation().getBlock().isLiquid() || !loc.isInSphere(mCenter.toVector(), 50)) {
									rainCannons(player.getLocation(), players);
								}
							}
							for (int j = 0; j < 4; j++) {
								rainCannons(mCenter.clone().add(FastUtils.randomDoubleInRange(-mRange, mRange), 0, FastUtils.randomDoubleInRange(-mRange, mRange)), players);
							}

							// Target one random player. Have a meteor rain nearby them.
							if (players.size() >= 1) {
								Player rPlayer = players.get(FastUtils.RANDOM.nextInt(players.size()));
								Location loc = rPlayer.getLocation();
								rainCannons(loc.add(FastUtils.randomDoubleInRange(-2, 2), 0, FastUtils.randomDoubleInRange(-2, 2)), players);
							}

							if (mI >= (mPhaseThree ? 30 : 25)) {
								this.cancel();
								mActiveRunnables.remove(this);
							}
						}

					};
					runnable.runTaskTimer(mPlugin, 0, 10);
					mActiveRunnables.add(runnable);
				}
			}

		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	private void rainCannons(Location locInput, List<Player> players) {
		if (locInput.distance(mCenter) > 24) {
			// Somehow tried to spawn a meteor too far away from the center point
			return;
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			Location mLoc = locInput.clone();
			World mWorld = locInput.getWorld();
			int mTicks = 60;

			@Override
			public void run() {
				mTicks--;
				players.removeIf(p -> p.getLocation().distance(mCenter) > 30);
				if (mTicks % 2 == 0) {
					double size = (60 - mTicks) / 20.0;
					for (Player player : players) {
						// Player gets more particles the closer they are to the landing area
						double dist = player.getLocation().distance(mLoc);
						double step = dist < 10 ? 0.5 : (dist < 15 ? 1 : 3);
						for (double deg = 0; deg < 360; deg += (step * 45)) {
							new PartialParticle(Particle.REDSTONE, mLoc.clone().add(FastUtils.cos(deg) * size, 0, FastUtils.sin(deg) * size), 1, 0.15, 0.15, 0.15, 0, CANNONS_COLOR).spawnAsEntityActive(mBoss);
						}
					}
				}
				Location particle = mLoc.clone().add(0, mTicks / 3.0, 0);
				new PartialParticle(Particle.SMOKE_NORMAL, particle, 3, 0.2f, 0.2f, 0.2f, 0.05, null, true).spawnAsEntityActive(mBoss);
				if (FastUtils.RANDOM.nextBoolean()) {
					new PartialParticle(Particle.CRIT, particle, 1, 0, 0, 0, 0, null, true).spawnAsEntityActive(mBoss);
				}
				mWorld.playSound(particle, Sound.ENTITY_ARROW_SHOOT, SoundCategory.HOSTILE, 1, 1);
				if (mTicks <= 0) {
					this.cancel();
					mActiveRunnables.remove(this);
					new PartialParticle(Particle.EXPLOSION_NORMAL, mLoc, 15, 0, 0, 0, 0.175, null, false).spawnAsEntityActive(mBoss);
					new PartialParticle(Particle.CRIT, mLoc, 10, 0, 0, 0, 0.25, null, false).spawnAsEntityActive(mBoss);
					mWorld.playSound(mLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 0.9f);
					BoundingBox box = BoundingBox.of(mLoc, 3, 3, 3);
					for (Player player : PlayerUtils.playersInRange(mLoc, 3, true)) {
						BoundingBox pBox = player.getBoundingBox();
						if (pBox.overlaps(box)) {
							BossUtils.blockableDamage(mBoss, player, DamageType.BLAST, 35, "Ghostly Cannons", mLoc);
							MovementUtils.knockAway(mLoc, player, 0.5f, 0.65f, false);
							AbilityUtils.silencePlayer(player, 15 * 20);
						}
					}
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int castTicks() {
		return 20 * 18;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 40;
	}
}
