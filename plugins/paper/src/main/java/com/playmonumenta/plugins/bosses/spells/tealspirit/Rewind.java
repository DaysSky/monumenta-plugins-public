package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.TealSpirit;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class Rewind extends Spell {
	private static final double RADIUS = 5;
	public static final int CHARGE_TIME = 6 * 20;
	public static final int REWIND_TIME = 6 * 20;
	public static final int COOLDOWN_TIME = 2 * 20;
	private static final int DAMAGE = 100;
	private static int mToggle = 0;

	private final LivingEntity mBoss;
	private final Location mCenter;
	private final TealSpirit mTeal;
	private final SpellManager mActiveSpells;
	private final List<Spell> mPassiveSpells;
	private final ChargeUpManager mWindUp;
	private final ChargeUpManager mWindDown;

	public Rewind(LivingEntity boss, Location center, TealSpirit tealSpirit, SpellManager activeSpells, List<Spell> passiveSpells) {
		mBoss = boss;
		mCenter = center;
		mTeal = tealSpirit;
		mActiveSpells = activeSpells;
		mPassiveSpells = passiveSpells;
		mWindUp = new ChargeUpManager(mBoss, CHARGE_TIME, Component.text("Winding Up...", NamedTextColor.AQUA), BossBar.Color.RED, BossBar.Overlay.PROGRESS, TealSpirit.detectionRange);
		mWindDown = new ChargeUpManager(mBoss, REWIND_TIME, Component.text("Turning Back Time...", NamedTextColor.AQUA), BossBar.Color.RED, BossBar.Overlay.PROGRESS, TealSpirit.detectionRange);
	}

	@Override
	public void run() {
		World world = mCenter.getWorld();
		PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true).forEach(player -> player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1.0f, 0.7f));

		PPCircle back = new PPCircle(Particle.TOTEM, mCenter, 1.5).count(12);
		PPCircle surround = new PPCircle(Particle.REDSTONE, mCenter, RADIUS).data(new Particle.DustOptions(Color.WHITE, 1));
		PPCircle inside = new PPCircle(Particle.SPELL_WITCH, mCenter, RADIUS).ringMode(false).delta(0, 0.2, 0).extra(0.1);
		Plugin plugin = Plugin.getInstance();

		int maxDir = 270 + (45 * mToggle);
		int minDir = 0 + (45 * mToggle);

		if (mToggle == 0) {
			mToggle = 1;
		} else {
			mToggle = 0;
		}

		BukkitRunnable outer = new BukkitRunnable() {
			@Override
			public void run() {
				// CONE HANDLING
				if (mWindUp.getTime() % 10 == 0) {
					for (int dir = minDir; dir <= maxDir; dir += 90) {
						Vector vec;
						for (double degree = 60; degree < 120; degree += 5) {
							for (double r = 0; r < 27; r++) {
								double radian1 = Math.toRadians(degree);
								vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
								vec = VectorUtils.rotateYAxis(vec, mCenter.getYaw() + dir);
								Location l = mCenter.clone().add(vec);
								if (r % 2 == 0) {
									new PartialParticle(Particle.END_ROD, l, 1, 0.05, 1.5, 0.05, 0.01).spawnAsEntityActive(mBoss);
								}
							}
						}
					}
				}


				if (mWindUp.nextTick()) {
					mWindUp.reset();

					List<Player> players = PlayerUtils.playersInRange(mCenter, TealSpirit.detectionRange, true);
					HashMap<Player, Location> origins = new HashMap<>();
					players.forEach(player -> origins.put(player, player.getLocation()));

					players.forEach(player -> player.playSound(player.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.HOSTILE, 1.0f, 1.5f));

					mWindDown.setTime(REWIND_TIME);

					// CONE DAMAGE
					Location loc = mCenter;
					world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 3, 0);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 3, 0.5f);
					world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 3, 0.5f);
					Vector vec;
					List<BoundingBox> boxes = new ArrayList<>();

					for (double r = 0; r < 27; r++) {
						for (int dir = minDir; dir <= maxDir; dir += 90) {
							for (double degree = 70; degree < 110; degree += 5) {
								double radian1 = Math.toRadians(degree);
								vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
								vec = VectorUtils.rotateYAxis(vec, mCenter.getYaw() + dir);
								Location l = loc.clone().add(vec);
								new PartialParticle(Particle.FLAME, l, 1, 0.05, 1.5, 0.05, 0.01).spawnAsEntityActive(mBoss);
								//1.5 -> 15
								BoundingBox box = BoundingBox.of(l, 0.65, 15, 0.65);
								boxes.add(box);
							}
						}
					}

					//Damage player by 35 in cone after warning is over (2 seconds) and knock player away
					for (Player player : PlayerUtils.playersInRange(loc, 40, true)) {

						List<Player> hitPlayers = new ArrayList<>();
						for (BoundingBox box : boxes) {
							if (player.getBoundingBox().overlaps(box) && !hitPlayers.contains(player)) {
								DamageUtils.damage(mBoss, player, DamageEvent.DamageType.BLAST, DAMAGE, null, true, true, "Shatter");
								MovementUtils.knockAway(mCenter, player, 0, .75f, false);
								hitPlayers.add(player);
							}
						}
					}

					BukkitRunnable inner = new BukkitRunnable() {
						@Override
						public void run() {
							if (mWindDown.previousTick()) {
								mWindDown.reset();

								HashMap<Player, HashMap<LivingEntity, Vector>> relatives = new HashMap<>();
								for (LivingEntity mob : EntityUtils.getNearbyMobs(mCenter, TealSpirit.detectionRange, mBoss)) {
									Location mobLoc = mob.getLocation();
									Player player = EntityUtils.getNearestPlayer(mobLoc, RADIUS);
									if (player != null) {
										relatives.computeIfAbsent(player, key -> new HashMap<>())
											.put(mob, mobLoc.toVector().subtract(player.getLocation().toVector()));
									}
								}

								for (Player player : players) {
									if (player.isDead() || player.getLocation().distance(mCenter) > TealSpirit.detectionRange) {
										continue;
									}
									Location origin = Objects.requireNonNull(origins.get(player));
									player.teleport(origin);
									HashMap<LivingEntity, Vector> relative = relatives.get(player);
									if (relative != null) {
										for (Map.Entry<LivingEntity, Vector> entry : relative.entrySet()) {
											LivingEntity mob = entry.getKey();
											if (mob.getType() != EntityType.SHULKER || !mob.getScoreboardTags().contains("NoMove")) {
												Location destination = origin.clone().add(entry.getValue());
												mob.teleport(destination);
												new PartialParticle(Particle.FALLING_OBSIDIAN_TEAR, destination.clone().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1).spawnAsEntityActive(mBoss);
											}
										}
									}

									player.playSound(origin, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 0.8f, 1.5f);
									new PartialParticle(Particle.FALLING_OBSIDIAN_TEAR, origin.clone().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1).spawnAsEntityActive(mBoss);
								}
								mTeal.changePhase(mActiveSpells, mPassiveSpells, null);
								this.cancel();
							}

							int time = mWindDown.getTime();
							if (time % 5 == 0) {
								for (Player player : new ArrayList<>(origins.keySet())) {
									if (player.isDead() || player.getLocation().distance(mCenter) > TealSpirit.detectionRange) {
										players.remove(player);
										origins.remove(player);
										continue;
									}
									back.location(Objects.requireNonNull(origins.get(player))).spawnAsBoss();

									Location loc = player.getLocation();
									double ratio = ((double) REWIND_TIME - time) / REWIND_TIME;
									player.playSound(loc, Sound.BLOCK_CONDUIT_AMBIENT, SoundCategory.HOSTILE, 1.0f + (float) ratio, 0.8f * (time % 10 == 0 ? 1 : 2));
									surround.location(loc.clone().add(0, 0.05, 0)).count(10 + (int) (70 * ratio)).spawnAsBoss();
									inside.location(loc).count(10 + (int) (20 * ratio)).spawnAsBoss();
								}
							}
						}

						@Override
						public synchronized void cancel() {
							super.cancel();
							mWindDown.reset();
						}
					};
					mActiveRunnables.add(inner);
					inner.runTaskTimer(plugin, 0, 1);

					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				mWindUp.reset();
			}
		};
		mActiveRunnables.add(outer);
		outer.runTaskTimer(plugin, 0, 1);
	}

	@Override
	public boolean onlyForceCasted() {
		return true;
	}

	@Override
	public int cooldownTicks() {
		return CHARGE_TIME + REWIND_TIME + COOLDOWN_TIME;
	}
}
