package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/*
 * Volcanic Demise:
 * Death.
=======
 * Volcanic Demise (CD: 20): Kaul starts summoning meteors that fall from the sky in random areas.
 * Each Meteor deals 42 damage in a 4 block radius on collision with the ground.
 * This ability lasts X seconds and continues spawning meteors until the ability duration runs out.
 * Kaul is immune to damage during the channel of this ability.
 */
public class SpellVolcanicDemise extends Spell {
	private static final String SPELL_NAME = "Volcanic Demise";
	private static final int DAMAGE = 42;
	private static final int METEOR_COUNT = 25;
	private static final int METEOR_RATE = 10;
	private static final double DEATH_RADIUS = 2;
	private static final double HIT_RADIUS = 5;

	private final LivingEntity mBoss;
	private final Plugin mPlugin;
	private final double mRange;
	private final Location mCenter;
	private final ChargeUpManager mChargeUp;

	public SpellVolcanicDemise(Plugin plugin, LivingEntity boss, double range, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mCenter = center;

		mChargeUp = new ChargeUpManager(mBoss, 20 * 2, Component.text("Charging ", NamedTextColor.GREEN).append(Component.text(SPELL_NAME + "...", NamedTextColor.DARK_RED, TextDecoration.BOLD)),
			BossBar.Color.RED, BossBar.Overlay.NOTCHED_10, 60);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		List<Player> players = PlayerUtils.playersInRange(mCenter, 50, true);
		players.removeIf(p -> p.getLocation().getY() >= 61);
		for (Player player : players) {
			player.sendMessage(Component.text("SCATTER, INSECTS.", NamedTextColor.GREEN));
		}
		// For the advancement "Such Devastation"
		NmsUtils.getVersionAdapter().runConsoleCommandSilently(
			"function monumenta:kaul/volcanic_demise_count");

		BukkitRunnable runnable = new BukkitRunnable() {

			@Override
			public void run() {
				float fTick = mChargeUp.getTime();
				float ft = fTick / 25;
				new PartialParticle(Particle.LAVA, mBoss.getLocation(), 4, 0.35, 0, 0.35, 0.005).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.FLAME, mBoss.getLocation().add(0, 1, 0), 3, 0.3, 0, 0.3, 0.125).spawnAsEntityActive(mBoss);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10, 0.5f + ft);
				if (mChargeUp.nextTick(2)) {
					this.cancel();
					mActiveRunnables.remove(this);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1, 0.5f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.HOSTILE, 1, 0.7f);

					mChargeUp.setTitle(Component.text("Unleashing ", NamedTextColor.GREEN)
						.append(Component.text(SPELL_NAME + "...", NamedTextColor.DARK_RED, TextDecoration.BOLD)));
					BukkitRunnable runnable = new BukkitRunnable() {

						@Override
						public synchronized void cancel() {
							super.cancel();
							mChargeUp.reset();
							mChargeUp.setTitle(Component.text("Charging ", NamedTextColor.GREEN)
								.append(Component.text(SPELL_NAME + "...", NamedTextColor.DARK_RED, TextDecoration.BOLD)));
						}

						int mI = 0;

						int mMeteors = 0;

						@Override
						public void run() {
							mI++;

							mChargeUp.setProgress(1 - ((double) mI / (double) (METEOR_COUNT * METEOR_RATE)));

							if (mI % METEOR_RATE == 0) {
								mMeteors++;
								List<Player> players = PlayerUtils.playersInRange(mCenter, 50, true);
								players.removeIf(p -> p.getLocation().getY() >= 61);
								Collections.shuffle(players);
								for (Player player : players) {
									Location loc = player.getLocation();
									if (loc.getBlock().isLiquid() || !loc.toVector().isInSphere(mCenter.toVector(), 42)) {
										loc.setY(mCenter.getY());
										rainMeteor(loc, players, 10);
									}
								}
								for (int j = 0; j < 4; j++) {
									rainMeteor(mCenter.clone().add(FastUtils.randomDoubleInRange(-mRange, mRange), 0, FastUtils.randomDoubleInRange(-mRange, mRange)), players, 40);
								}

								// Target one random player. Have a meteor rain nearby them.
								if (players.size() >= 1) {
									Player rPlayer = players.get(FastUtils.RANDOM.nextInt(players.size()));
									Location loc = rPlayer.getLocation();
									loc.setY(mCenter.getY());
									rainMeteor(loc.add(FastUtils.randomDoubleInRange(-8, 8), 0, FastUtils.randomDoubleInRange(-8, 8)), players, 40);
								}

								if (mMeteors >= METEOR_COUNT) {
									this.cancel();
									mActiveRunnables.remove(this);
								}
							}

						}

					};
					runnable.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(runnable);
				}
			}

		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	private void rainMeteor(Location locInput, List<Player> players, double spawnY) {
		if (locInput.distance(mCenter) > 50 || locInput.getY() >= 55) {
			// Somehow tried to spawn a meteor too far away from the center point
			return;
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			double mY = spawnY;
			final Location mLoc = locInput.clone();
			final World mWorld = locInput.getWorld();

			@Override
			public void run() {
				players.removeIf(p -> p.getLocation().distance(mCenter) > 50 || p.getLocation().getY() >= 61);

				mY -= 1;
				// Impact Zone
				if (mY % 8 == 0) {
					ParticleUtils.drawRing(mLoc.clone().add(0, 0.2, 0), 180, new Vector(0, 1, 0), DEATH_RADIUS,
							(l, t) -> {
								Vector toCenter = l.clone().subtract(mLoc).toVector().normalize();
								new PartialParticle(Particle.FLAME, l).delta(toCenter.getX(), toCenter.getY(), toCenter.getZ())
										.count(1).extra(0.15).directionalMode(true).distanceFalloff(15).spawnAsBoss();
							}
					);
					ParticleUtils.drawRing(mLoc.clone().add(0, 0.2, 0),
						60,
						new Vector(0, 1, 0),
						2,
						(l, t) ->
						new PartialParticle(Particle.REDSTONE, l)
							.count(1)
							.extra(0)
							.data(new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1))
							.distanceFalloff(15)
							.spawnAsBoss());
				}
				new PartialParticle(Particle.LAVA, mLoc, 3, 2.5, 0, 2.5, 0.05)
						.distanceFalloff(20).spawnAsBoss();
				// Meteor Trail
				Location particle = mLoc.clone().add(0, mY, 0);
				new PartialParticle(Particle.FLAME, particle, 10, 0.2f, 0.2f, 0.2f, 0.1)
						.distanceFalloff(20).spawnAsBoss();
				new PartialParticle(Particle.SMOKE_LARGE, particle, 5, 0, 0, 0, 0.05)
						.distanceFalloff(20).spawnAsBoss();
				mWorld.playSound(particle, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1, 1);
				// Impact
				if (mY <= 0) {
					this.cancel();
					mActiveRunnables.remove(this);
					new PartialParticle(Particle.FLAME, mLoc, 50, 0, 0, 0, 0.175)
						.distanceFalloff(20).spawnAsBoss();
					new PartialParticle(Particle.SMOKE_LARGE, mLoc, 10, 0, 0, 0, 0.25)
						.distanceFalloff(20).spawnAsBoss();
					mWorld.playSound(mLoc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 0.9f);
					Hitbox deathBox = new Hitbox.UprightCylinderHitbox(mLoc, 7, DEATH_RADIUS);
					Hitbox hitBox = new Hitbox.UprightCylinderHitbox(mLoc, 15, HIT_RADIUS);
					List<Player> hitPlayers = new ArrayList<>(hitBox.getHitPlayers(true));
					for (Player player : deathBox.getHitPlayers(true)) {
						DamageUtils.damage(mBoss, player, DamageType.BLAST, 1000, null, false, true, SPELL_NAME);
						MovementUtils.knockAway(mLoc, player, 0.5f, 0.65f);
						hitPlayers.remove(player);
					}
					for (Player player : hitPlayers) {
						boolean didDamage = BossUtils.blockableDamage(mBoss, player, DamageType.BLAST, DAMAGE, SPELL_NAME, mLoc);
						if (didDamage) {
							MovementUtils.knockAway(mLoc, player, 0.5f, 0.65f);
						}
					}
					for (Block block : LocationUtils.getNearbyBlocks(mLoc.getBlock(), 4)) {
						if (FastUtils.RANDOM.nextDouble() < 0.125) {
							if (block.getType() == Material.SMOOTH_RED_SANDSTONE) {
								block.setType(Material.NETHERRACK);
							} else if (block.getType() == Material.NETHERRACK) {
								block.setType(Material.MAGMA_BLOCK);
							} else if (block.getType() == Material.SMOOTH_SANDSTONE) {
								block.setType(Material.SMOOTH_RED_SANDSTONE);
							}
						}
					}
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				if (mY > 0) {
					MMLog.warning("Volcanic Demise cancelled early!");
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int castTicks() {
		return 20 * 17;
	}

	@Override
	public int cooldownTicks() {
		return 20 * 35;
	}

}
