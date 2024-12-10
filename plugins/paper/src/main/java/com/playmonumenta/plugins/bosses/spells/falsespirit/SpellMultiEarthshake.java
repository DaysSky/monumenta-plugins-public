package com.playmonumenta.plugins.bosses.spells.falsespirit;

import com.playmonumenta.plugins.bosses.bosses.FalseSpirit;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpellMultiEarthshake extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRadius;
	private final int mDuration;

	private final Location mSpawnLoc;

	private static @Nullable Location targetLocation;

	private static int particleCounter1 = 0;
	private static int particleCounter2 = 0;

	private final List<Player> mNoTarget = new ArrayList<>();

	private final boolean mDelve;

	private static final EnumSet<Material> mIgnoredMats = EnumSet.of(
		Material.AIR,
		Material.CAVE_AIR,
		Material.VOID_AIR,
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.BEDROCK,
		Material.BARRIER,
		Material.SPAWNER,
		Material.CHEST,
		Material.END_PORTAL,
		Material.END_PORTAL_FRAME
	);

	public SpellMultiEarthshake(Plugin plugin, LivingEntity launcher, int radius, int duration, boolean delve, Location spawnLoc) {
		mSpawnLoc = spawnLoc;

		mBoss = launcher;
		mPlugin = plugin;

		mRadius = radius;
		mDuration = duration;

		mDelve = delve;
	}

	@Override
	public void run() {
		List<Player> targets = PlayerUtils.playersInRange(mSpawnLoc, 40, false);
		List<Location> locs = new ArrayList<>(targets.size());

		for (Player target : targets) {
			if (mNoTarget.contains(target)) {
				continue;
			}
			locs.add(target.getLocation());
			target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 1f, 0.75f);
		}

		new BukkitRunnable() {
			float mTicks = 0;
			double mCurrentRadius = mRadius;
			final World mWorld = mBoss.getWorld();

			@Override
			public void run() {

				if (mBoss.isDead() || !mBoss.isValid() || EntityUtils.isStunned(mBoss)) {
					mBoss.setAI(true);
					this.cancel();
					return;
				}


				for (Location playerLoc : locs) {
					targetLocation = playerLoc;
					if (particleCounter1 % 2 == 0) {
						new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, targetLocation, 1, ((double) mRadius * 2) / 2, ((double) mRadius * 2) / 2, ((double) mRadius * 2) / 2, 0.05).spawnAsEntityActive(mBoss);
					}

					new PartialParticle(Particle.BLOCK_CRACK, targetLocation, 2, mRadius / 2.0, 0.1, mRadius / 2.0, Bukkit.createBlockData(Material.STONE)).spawnAsEntityActive(mBoss);

					if (particleCounter1 % 20 == 0 && particleCounter1 > 0) {
						mWorld.playSound(targetLocation, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.HOSTILE, 1f, 0.5f);
						mWorld.playSound(targetLocation, Sound.BLOCK_GRAVEL_BREAK, SoundCategory.HOSTILE, 1f, 0.5f);
						for (int i = 0; i < 360; i += 18) {
							new PartialParticle(Particle.SMOKE_NORMAL, targetLocation.clone().add(FastUtils.cos(Math.toRadians(i)) * mRadius, 0.2, FastUtils.sin(Math.toRadians(i)) * mRadius), 1, 0.1, 0.1, 0.1, 0).spawnAsEntityActive(mBoss);
						}
						new PartialParticle(Particle.BLOCK_CRACK, targetLocation, 80, mRadius / 2.0, 0.1, mRadius / 2.0, Bukkit.createBlockData(Material.DIRT)).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, targetLocation, 8, mRadius / 2.0, 0.1, mRadius / 2.0, 0).spawnAsEntityActive(mBoss);
					}
					particleCounter1++;

					if (mTicks <= (mDuration - 5)) {
						mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.HOSTILE, 1f, 0.25f + (mTicks / 100));
					}

					Location loc = targetLocation.clone();
					for (double i = 0; i < 360; i += 30) {
						double radian1 = Math.toRadians(i);
						loc.add(FastUtils.cos(radian1) * mCurrentRadius, 0, FastUtils.sin(radian1) * mCurrentRadius);

						if (particleCounter2 % 10 == 0) {
							new PartialParticle(Particle.LAVA, loc, 1, 0.25, 0.25, 0.25, 0.1).spawnAsEntityActive(mBoss);
							new PartialParticle(Particle.DRIP_LAVA, mBoss.getLocation().clone().add(0, mBoss.getHeight() / 2, 0), 1, 0.25, 0.45, 0.25, 1).spawnAsEntityActive(mBoss);
						}
						particleCounter2++;
						loc.subtract(FastUtils.cos(radian1) * mCurrentRadius, 0, FastUtils.sin(radian1) * mCurrentRadius);
					}
				}

				mTicks++;
				mCurrentRadius -= (mRadius / ((double) mDuration));

				if (mCurrentRadius <= 0) {
					for (Location loc : locs) {
						this.cancel();

						if (mDelve) {
							ArrayList<Block> blocks = new ArrayList<Block>();

							//Populate the blocks array with nearby blocks- logic here to get the topmost block with air above it
							for (int x = mRadius * -1; x <= mRadius; x++) {
								for (int y = mRadius * -1; y <= mRadius; y++) {
									Block selected = null;
									Block test = mWorld.getBlockAt(loc.clone().add(x, -2, y));
									if (test.getType() != Material.AIR) {
										selected = mWorld.getBlockAt(loc.clone().add(x, -2, y));
									}
									test = mWorld.getBlockAt(loc.clone().add(x, -1, y));
									if (test.getType() != Material.AIR) {
										selected = mWorld.getBlockAt(loc.clone().add(x, -1, y));
									} else if (selected != null) {
										blocks.add(selected);
										continue;
									}
									test = mWorld.getBlockAt(loc.clone().add(x, 0, y));
									if (test.getType() != Material.AIR) {
										selected = mWorld.getBlockAt(loc.clone().add(x, 0, y));
									} else {
										blocks.add(selected);
										continue;
									}
									test = mWorld.getBlockAt(loc.clone().add(x, 1, y));
									if (test.getType() != Material.AIR) {
										selected = mWorld.getBlockAt(loc.clone().add(x, 1, y));
									} else {
										blocks.add(selected);
										continue;
									}
									test = mWorld.getBlockAt(loc.clone().add(x, 2, y));
									if (test.getBlockData().getMaterial() != Material.AIR) {
										selected = mWorld.getBlockAt(loc.clone().add(x, 2, y));
									} else {
										blocks.add(selected);
										continue;
									}
								}
							}

							//Make the blocks go flying
							for (Block b : blocks) {
								if (b == null) {
									continue;
								}

								Material material = b.getType();
								if (!mIgnoredMats.contains(material) && !BlockUtils.containsWater(b) && !(b.getBlockData() instanceof Bed) && FastUtils.RANDOM.nextInt(4) > 1) {
									double x = (FastUtils.RANDOM.nextInt(5) - 2) / 10.0;
									double z = (FastUtils.RANDOM.nextInt(5) - 2) / 10.0;

									FallingBlock block = mWorld.spawn(b.getLocation(), FallingBlock.class, fb -> fb.setBlockData(b.getBlockData()));
									block.setDropItem(false);
									block.setVelocity(new Vector(x, 1.0, z));
									mWorld.getBlockAt(b.getLocation()).setType(Material.AIR);
								}
							}
						}

						//Knock up player
						for (Player p : PlayerUtils.playersInRange(loc, mRadius * 2, true)) {
							mWorld.playSound(p.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 1.0f, 1.0f);

							double yVelocity = p.getLocation().distance(loc) <= mRadius ? 1.5 : 1;
							p.setVelocity(p.getVelocity().add(new Vector(0.0, yVelocity, 0.0)));

							double damage = mDelve ? 45 : 40;
							DamageUtils.damage(mBoss, p, DamageType.BLAST, damage, null, false, true, "Earthshake");
						}
						//Knock up other mobs because it's fun
						List<LivingEntity> mobs = EntityUtils.getNearbyMobs(loc, mRadius * 2);
						for (LivingEntity mob : mobs) {
							mob.setVelocity(mob.getVelocity().add(new Vector(0.0, 2.0, 0.0)));
						}


						mWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 1.35f);
						mWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.5f, 0.5f);
						mWorld.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.HOSTILE, 1.5f, 0.5f);

						new PartialParticle(Particle.CLOUD, loc, 150, 0, 0, 0, 0.5).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.LAVA, loc, 35, mRadius / 2.0, 0.1, mRadius / 2.0, 0).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.BLOCK_CRACK, loc, 200, mRadius / 2.0, 0.1, mRadius / 2.0, Bukkit.createBlockData(Material.DIRT)).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 35, mRadius / 2.0, 0.1, mRadius / 2.0, 0.1).spawnAsEntityActive(mBoss);

						for (int i = 0; i < 100; i++) {
							new PartialParticle(Particle.SMOKE_LARGE, loc.clone().add(-3 + FastUtils.RANDOM.nextDouble() * 6, 0.1, -3 + FastUtils.RANDOM.nextDouble() * 6), 0, 0, 1, 0, 0.2 + FastUtils.RANDOM.nextDouble() * 0.4).spawnAsEntityActive(mBoss);
						}

					}
					particleCounter1 = 0;
					particleCounter2 = 0;
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, FalseSpirit.detectionRange, "tellraw @s [\"\",{\"text\":\"The Congress shall tremble!\",\"color\":\"dark_red\"}]");
	}

	@Override
	public int cooldownTicks() {
		return 10 * 20;
	}

	@Override
	public void nearbyPlayerDeath(PlayerDeathEvent event) {
		mNoTarget.add(event.getEntity());
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mNoTarget.remove(event.getEntity()), 20 * 60);
	}

	@Override
	public boolean hasNearbyPlayerDeathTrigger() {
		return true;
	}
}
