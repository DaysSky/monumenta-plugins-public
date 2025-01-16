package com.playmonumenta.plugins.depths.bosses.spells.nucleus;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Nucleus;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
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

public class SpellRisingTides extends Spell {

	public static final double DAMAGE = .5;
	private static final Particle.DustOptions UP_COLOR = new Particle.DustOptions(Color.fromRGB(66, 140, 237), 1.0f);
	private static final Particle.DustOptions DOWN_COLOR = new Particle.DustOptions(Color.fromRGB(226, 88, 34), 1.0f);

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	public int mCooldownTicks;
	private final Location mStartLoc;
	public Nucleus mBossInstance;

	public SpellRisingTides(Plugin plugin, LivingEntity boss, Location startLoc, int cooldownTicks, Nucleus bossInstance) {
		mPlugin = plugin;
		mBoss = boss;
		mStartLoc = startLoc;
		mCooldownTicks = cooldownTicks;
		mBossInstance = bossInstance;
	}

	@Override
	public boolean canRun() {
		return mBossInstance.mIsHidden;
	}

	@Override
	public void run() {
		int lowCount = 0;
		int highCount = 0;
		List<Player> players = PlayerUtils.playersInRange(mStartLoc, 50, true);
		for (Player p : players) {
			if (p.getLocation().getY() < mStartLoc.getY() + .5) {
				lowCount++;
			} else if (p.getLocation().getY() > mStartLoc.getY() + .5) {
				highCount++;
			}
		}

		cast(lowCount > highCount, players);
	}

	public void cast(boolean tide, List<Player> players) {
		for (Player player : players) {
			if (tide) {
				player.sendMessage(Component.text("The frigid water rises below...", NamedTextColor.BLUE));
			} else {
				player.sendMessage(Component.text("The water above boils with heat..", NamedTextColor.RED));
			}
		}

		World world = mStartLoc.getWorld();

		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				mT += 5;

				//Play knockup sound & particles
				if (mT % 5 == 0 && mT < 55) {
					int t = mT / 5;
					float dPitch = t * 0.2f;
					if (tide) {
						world.playSound(mStartLoc, Sound.ENTITY_DOLPHIN_SPLASH, SoundCategory.HOSTILE, 10, 0f + dPitch);
					} else {
						world.playSound(mStartLoc, Sound.ITEM_FIRECHARGE_USE, SoundCategory.HOSTILE, 10, 2.0f - dPitch);
					}

					Location loc = mStartLoc.clone().add(0, tide ? -.5 + (dPitch / 2) : 2.5 - (dPitch / 2), 0);
					PPCircle particles = new PPCircle(Particle.REDSTONE, loc, 30)
						                     .ringMode(false)
						                     .count(1300)
						                     .delta(0.1, 0.05, 0.1)
						                     .data(tide ? UP_COLOR : DOWN_COLOR)
						                     .distanceFalloff(20);
					particles.spawnAsEntityActive(mBoss);

					// spawn a few particles also far away
					particles
						.count(100)
						.distanceFalloff(0)
						.spawnAsEntityActive(mBoss);

				}

				if (mT == 55) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 20.0f, 1);

					for (double deg = 0; deg < 360; deg += 4) {
						double cos = FastUtils.cosDeg(deg);
						double sin = FastUtils.sinDeg(deg);

						for (int x = 4; x <= 30; x += 8) {
							Location loc = mStartLoc.clone().add(cos * x, 0, sin * x);
							for (Player player : players) {
								double dist = player.getLocation().distance(loc);

								if (dist < 10) {
									new PartialParticle(Particle.SMOKE_NORMAL, loc.add(0, tide ? -0.5 : 2.5, 0), 1, 0.15, 0.15, 0.15, 0).spawnAsEntityActive(mBoss);
									if (deg % 16 == 0) {
										new PartialParticle(Particle.DAMAGE_INDICATOR, loc, 1, 0.15, 0.15, 0.15).spawnAsEntityActive(mBoss);
										new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0.15, 0.15, 0.15, 0).spawnAsEntityActive(mBoss);
									}
								}
							}
						}
					}

					for (Player p : PlayerUtils.playersInRange(mStartLoc, 50, true)) {
						if (tide && p.getLocation().getY() < mStartLoc.getY() + .5) {
							BossUtils.bossDamagePercent(mBoss, p, DAMAGE, "Rising Tides");
							com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, "Tectonic", new PercentSpeed(2 * 20, -.99, "Tectonic"));

						} else if (!tide && p.getLocation().getY() > mStartLoc.getY() + .5) {
							BossUtils.bossDamagePercent(mBoss, p, DAMAGE, "Falling Tides");
							com.playmonumenta.plugins.Plugin.getInstance().mEffectManager.addEffect(p, "Tectonic", new PercentSpeed(2 * 20, -.99, "Tectonic"));
						}
					}
					this.cancel();
				}

			}
		};
		runnable.runTaskTimer(mPlugin, 0, 5);
		mActiveRunnables.add(runnable);
	}


	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}
}
