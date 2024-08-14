package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.TealSpirit;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SandsOfTime extends Spell {
	private static final double RADIUS = 21;
	private static final double HEIGHT = 4;
	private static final int BLUE_ROOT = 1 * 20;
	private static final double DIST = 25;
	private static final int SPREAD = 4;
	private static final int BLUE_DELAY = 4 * 20;
	private static final String ROOT_EFFECT = "SandsOfTimePercentSpeedEffect";
	private static final String SPELL_NAME = "Sands of Time";

	private final LivingEntity mBoss;
	private final Location mCenter;
	private final int mCooldownTicks;
	private final double mDamage;
	private final int mBellTime;
	private final ChargeUpManager mChargeUp;

	public SandsOfTime(LivingEntity boss, Location center, int cooldownTicks, int damage, int bellTime) {
		mBoss = boss;
		mCenter = center;
		mCooldownTicks = cooldownTicks;
		mDamage = damage;
		mBellTime = bellTime;
		mChargeUp = new ChargeUpManager(mCenter, mBoss, 4 * mBellTime, Component.text("Channeling " + SPELL_NAME + "...", NamedTextColor.BLUE), BossBar.Color.BLUE, BossBar.Overlay.PROGRESS, TealSpirit.detectionRange);
	}

	@Override
	public void run() {
		Plugin plugin = Plugin.getInstance();
		mBoss.setAI(false);
		mBoss.setGravity(false);

		PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true).forEach(player -> player.sendMessage(Component.text("now the very sands of time will be unleashed!", NamedTextColor.DARK_AQUA)));

		List<Location> locs = new ArrayList<>();
		locs.add(mCenter.clone().add(RADIUS, HEIGHT, 0));
		locs.add(mCenter.clone().add(-RADIUS, HEIGHT, 0));
		locs.add(mCenter.clone().add(0, HEIGHT, RADIUS));
		locs.add(mCenter.clone().add(0, HEIGHT, -RADIUS));
		Collections.shuffle(locs);

		List<SandsColor> sandsColors = new ArrayList<>();
		sandsColors.add(SandsColor.RED);
		sandsColors.add(SandsColor.RED);
		sandsColors.add(SandsColor.BLUE);
		sandsColors.add(SandsColor.BLUE);
		Collections.shuffle(sandsColors);

		BukkitRunnable runnable = new BukkitRunnable() {
			boolean mComplete = false;

			@Override
			public void run() {
				if (mComplete) {
					return;
				}

				int time = mChargeUp.getTime();
				if (time % mBellTime == 0 && time < 4 * mBellTime) {
					int i = time / mBellTime;
					Location loc = locs.get(i);
					mBoss.teleport(loc);
					SandsColor sandsColor = sandsColors.get(i);
					mChargeUp.setColor(sandsColor.mBarColor);
					mChargeUp.setTitle(Component.text("Channeling " + SPELL_NAME + "...", sandsColor.mTextColor));
					GlowingManager.startGlowing(mBoss, sandsColor.mGlowColor, -1, GlowingManager.BOSS_SPELL_PRIORITY, null, SPELL_NAME);

					for (Player player : PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true)) {
						Location playerLoc = player.getLocation();
						Location soundLoc = playerLoc.clone().add(LocationUtils.getDirectionTo(loc, playerLoc).normalize().multiply(3));
						player.playSound(soundLoc, Sound.BLOCK_BELL_USE, SoundCategory.HOSTILE, 1, sandsColor.mPitch);
						player.playSound(soundLoc, Sound.BLOCK_BELL_USE, SoundCategory.HOSTILE, 0.75f, sandsColor.mPitch * 0.334f);
					}
				}

				if (mChargeUp.nextTick()) {
					Location tallCenter = mCenter.clone().add(0, HEIGHT, 0);
					mBoss.teleport(tallCenter);
					GlowingManager.clear(mBoss, SPELL_NAME);

					for (int i = 0; i < locs.size(); i++) {
						Location loc = locs.get(i);
						SandsColor sandsColor = sandsColors.get(i);
						BukkitRunnable activateRunnable = new BukkitRunnable() {
							@Override
							public void run() {
								activate(plugin, loc, sandsColor, tallCenter);
							}
						};
						activateRunnable.runTaskLater(plugin, sandsColor.mDelay);
						mActiveRunnables.add(activateRunnable);
					}

					Bukkit.getScheduler().runTaskLater(plugin, this::cancel, BLUE_DELAY);

					mChargeUp.reset();
					mComplete = true;
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				mChargeUp.reset();
				mBoss.setAI(true);
				mBoss.setGravity(true);
			}
		};
		runnable.runTaskTimer(plugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private void activate(Plugin plugin, Location loc, SandsColor sandsColor, Location tallCenter) {
		World world = loc.getWorld();
		Vector dir = LocationUtils.getDirectionTo(loc, mCenter);
		Vector horizontal = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
		for (int i = 0; i < DIST; i += SPREAD) {
			for (int j = -i; j <= i; j += SPREAD) {
				Vector offset = dir.clone().multiply(i).add(horizontal.clone().multiply(j));
				Location target = mCenter.clone().add(offset);
				if (target.distance(mCenter) > DIST) {
					continue;
				}
				target.add(FastUtils.randomDoubleInRange(-0.75, 0.75), 0, FastUtils.randomDoubleInRange(-0.75, 0.75));
				target = LocationUtils.fallToGround(target, mCenter.getY());
				Location current = mCenter.clone().add(0, 6, 0).add(offset.clone().multiply(1.0 / 3));
				Vector path = LocationUtils.getDirectionTo(target, current);
				double pullback = -0.5;
				double shoot = (current.distance(target) - pullback * 3) / 5;

				BukkitRunnable runnable = new BukkitRunnable() {
					int mT = 1;

					@Override
					public void run() {
						Vector move;
						if (mT <= 2) {
							move = new Vector();
						} else if (mT <= 5) {
							move = path.clone().multiply(pullback);
						} else {
							move = path.clone().multiply(shoot);
						}

						double length = move.length();
						for (double r = 0; r <= length; r += 0.3 + 0.2 * length) {
							new PartialParticle(Particle.REDSTONE, current.clone().add(move.clone().normalize().multiply(r)), 1, new Particle.DustOptions(sandsColor.mColor, 2.5f))
								.spawnAsEntityActive(mBoss);
						}
						current.add(move);

						if (mT >= 10) {
							this.cancel();
							return;
						}

						mT++;
					}
				};
				mActiveRunnables.add(runnable);
				runnable.runTaskTimer(plugin, 0, 1);
			}
		}

		BukkitRunnable sound1Runnable = new BukkitRunnable() {
			@Override
			public void run() {
				List<Location> soundLocs = getRandomLocationsNear(tallCenter, 2);
				for (Location soundLoc : soundLocs) {
					world.playSound(soundLoc, Sound.ITEM_CROSSBOW_LOADING_START, SoundCategory.HOSTILE, 2.0f, 0.75f);
				}
			}
		};
		sound1Runnable.runTaskLater(plugin, 2);
		mActiveRunnables.add(sound1Runnable);

		BukkitRunnable sound2Runnable = new BukkitRunnable() {
			@Override
			public void run() {
				List<Location> soundLocs = getRandomLocationsNear(tallCenter, 3);
				for (Location soundLoc : soundLocs) {
					world.playSound(soundLoc, Sound.ITEM_TRIDENT_THROW, SoundCategory.HOSTILE, 1.5f, 1.25f);
				}
			}
		};
		sound2Runnable.runTaskLater(plugin, 5);
		mActiveRunnables.add(sound2Runnable);

		BukkitRunnable damageRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true)) {
					Location playerLoc = player.getLocation();
					// Within 2 blocks or 45 degrees in either direction
					// 0.7071 = sqrt(2) / 2
					if (playerLoc.distanceSquared(mCenter) < 2 * 2 || dir.clone().setY(0).normalize().dot(LocationUtils.getDirectionTo(playerLoc, mCenter).setY(0).normalize()) >= 0.7071) {
						DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, mDamage, null, false, false, SPELL_NAME);
						sandsColor.applyEffects(player);
					}
				}

				List<Location> soundLocs = getRandomLocationsNear(loc, 3);
				for (Location soundLoc : soundLocs) {
					world.playSound(soundLoc, Sound.ITEM_TRIDENT_HIT_GROUND, SoundCategory.HOSTILE, 1.2f, 1.25f);
				}
				soundLocs = getRandomLocationsNear(loc, 3);
				for (Location soundLoc : soundLocs) {
					world.playSound(soundLoc, Sound.ENTITY_ARROW_HIT, SoundCategory.HOSTILE, 1.2f, 0.8f);
				}
			}
		};
		damageRunnable.runTaskLater(plugin, 10);
		mActiveRunnables.add(damageRunnable);
	}

	private List<Location> getRandomLocationsNear(Location center, int num) {
		List<Location> locs = new ArrayList<>();
		for (int i = 0; i < num; i++) {
			locs.add(center.clone().add(FastUtils.randomDoubleInRange(-3, 3), FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(-3, 3)));
		}
		return locs;
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}

	@Override
	public void cancel() {
		super.cancel();
		GlowingManager.clear(mBoss, SPELL_NAME);
	}

	private enum SandsColor {
		RED(Color.RED, NamedTextColor.RED, BossBar.Color.RED, NamedTextColor.DARK_RED, 0.5f, 0, null),
		BLUE(Color.BLUE, NamedTextColor.BLUE, BossBar.Color.BLUE, NamedTextColor.BLUE, 0.354f, BLUE_DELAY, p -> Plugin.getInstance().mEffectManager.addEffect(p, ROOT_EFFECT, new PercentSpeed(BLUE_ROOT, -1, ROOT_EFFECT)));

		private final Color mColor;
		private final TextColor mTextColor;
		private final BossBar.Color mBarColor;
		private final NamedTextColor mGlowColor;
		private final float mPitch;
		private final int mDelay;
		private final @Nullable Consumer<Player> mOnHitEffect;

		SandsColor(Color color, TextColor chatColor, BossBar.Color barColor, NamedTextColor glowColor, float pitch, int delay, @Nullable Consumer<Player> onHitEffect) {
			mColor = color;
			mTextColor = chatColor;
			mBarColor = barColor;
			mGlowColor = glowColor;
			mPitch = pitch;
			mDelay = delay;
			mOnHitEffect = onHitEffect;
		}

		private void applyEffects(Player player) {
			if (mOnHitEffect != null) {
				mOnHitEffect.accept(player);
			}
		}
	}
}
