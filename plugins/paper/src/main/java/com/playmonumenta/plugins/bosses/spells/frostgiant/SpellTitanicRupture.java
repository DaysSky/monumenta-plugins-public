package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.listeners.StasisListener;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.scriptedquests.growables.GrowableAPI;
import com.playmonumenta.scriptedquests.growables.GrowableProgress;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellTitanicRupture extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mStartLoc;
	private boolean mCooldown = false;
	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.0f);

	//Used to store locations of icicle blocks to delete them
	private List<Location> mLocs = new ArrayList<>();

	//Stores references to growables that are in progress growing
	private List<GrowableProgress> mGrowables = new ArrayList<>();


	public SpellTitanicRupture(Plugin plugin, LivingEntity boss, Location loc) {
		mPlugin = plugin;
		mBoss = boss;
		mStartLoc = loc;
	}

	@Override
	public void run() {
		FrostGiant.freezeGolems(mBoss);
		mCooldown = true;
		new BukkitRunnable() {

			@Override
			public void run() {
				mCooldown = false;
			}

		}.runTaskLater(mPlugin, 20 * 25);
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.HOSTILE, 5, 0.5f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.HOSTILE, 5, 1);
		PlayerUtils.nearbyPlayersAudience(mStartLoc, FrostGiant.detectionRange).sendMessage(Component.text("CRUMBLE UNDER THE WEIGHT OF THE MOUNTAIN.", NamedTextColor.DARK_RED));

		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				mT++;

				if (mT >= 20 * 2.5) {
					this.cancel();
					List<Player> players = PlayerUtils.playersInRange(mStartLoc, FrostGiant.fighterRange, true);
					List<Player> targets = new ArrayList<>();
					if (players.size() >= 3) {
						while (targets.size() < 3) {
							Player player = players.get(FastUtils.RANDOM.nextInt(players.size()));
							if (!targets.contains(player)) {
								targets.add(player);
							}
						}
					} else {
						targets = players;
					}
					for (Player player : targets) {
						createTitanicIcicles(player, players);
					}
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private void createTitanicIcicles(Player target, List<Player> players) {
		Location loc = target.getLocation();
		World world = mBoss.getWorld();
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 5, 0);
		//If in air, subtract 1 y value until the block is not air
		if (target.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
			while (loc.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
				loc.add(0, -1, 0);
			}
		}

		//Call growable to create the Titanic Rupture Icicle 20 blocks above the player
		try {
			GrowableProgress newGrowable = GrowableAPI.grow("titanicruptureicicle", loc.clone().add(0, 20, 0), 1, 10, true);

			// Add it to the list for now, and when it's done, have it remove itself from the list
			mGrowables.add(newGrowable);
			newGrowable.whenComplete((growable) -> mGrowables.remove(growable));
		} catch (Exception e) {
			mPlugin.getLogger().warning("Failed to grow scripted quests structure 'titanicruptureicicle': " + e.getMessage());
			e.printStackTrace();
		}
		List<FallingBlock> ices = new ArrayList<>(1500);

		mLocs.add(loc);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mT = 0;
			float mPitch = 1;

			@Override
			public void run() {
				if (mT >= 40) {

					Location l = loc.clone();
					//Turns all the icicles in the growable into falling blocks
					for (int y = -15; y <= 0; y++) {
						for (int x = -6; x < 6; x++) {
							for (int z = -6; z < 6; z++) {
								l.set(loc.getX() + x, loc.getY() + y + 20, loc.getZ() + z);
								Block b = l.getBlock();
								if (b.getType() == Material.BLUE_ICE || b.getType() == Material.SNOW_BLOCK) {
									ices.add(mBoss.getWorld().spawn(l, FallingBlock.class, fb -> fb.setBlockData(Bukkit.createBlockData(b.getType()))));
									b.setType(Material.AIR);
								}
							}
						}
					}
					//Forces velocity to -1 for y, so it drops quickly
					for (FallingBlock ice : ices) {
						ice.setVelocity(new Vector(0, -1, 0));
						ice.setDropItem(false);
					}

					world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.HOSTILE, 5, 0);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 5, 0);
					//Smaller hitbox for the instant kill
					BoundingBox smallBox = BoundingBox.of(loc, 1, 20, 1);
					for (Player player : players) {
						if (smallBox.overlaps(player.getBoundingBox())) {
							BossUtils.bossDamagePercent(mBoss, player, 10, "Titanic Rupture");
							player.damage(1);
							// If player is in Stasis, grant advancement of Lonely Soloist
							if (StasisListener.isInStasis(player)) {
								AdvancementUtils.grantAdvancement(player, "monumenta:challenges/r2/fg/lonely_soloist");
							}
						}
					}
					//The particles that damage after 2 seconds, in the larger hitbox
					BoundingBox box = BoundingBox.of(loc, 4, 20, 4);
					for (Player player : players) {
						if (box.overlaps(player.getBoundingBox())) {
							DamageUtils.damage(mBoss, player, DamageType.BLAST, 60, null, false, true, "Titanic Rupture");
						}
					}
					Location particleLoc = loc.clone();
					//Creates line of particles
					for (int y = loc.getBlockY(); y < loc.getBlockY() + 10; y += 1) {
						particleLoc.setY(y);
						for (double deg = 0; deg < 360; deg += 10) {
							double cos = FastUtils.cos(deg);
							double sin = FastUtils.sin(deg);
							new PartialParticle(Particle.REDSTONE, loc.clone().add(4 * cos, 0, 4 * sin), 1, 0.15, 0.15, 0.15, RED_COLOR).spawnAsEntityActive(mBoss);
							new PartialParticle(Particle.DRAGON_BREATH, loc.clone().add(5 * cos, 0, 5 * sin), 1, 0.15, 0.15, 0.15, 0.1).spawnAsEntityActive(mBoss);
						}
					}
					FrostGiant.unfreezeGolems(mBoss);
					this.cancel();

					mLocs.remove(loc);
				}

				if (mT % 10 == 0) {
					world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 2, mPitch);
				}
				mPitch += 0.05f;

				for (double deg = 0; deg < 360; deg += 10) {
					if (FastUtils.RANDOM.nextDouble() > 0.4) {
						double cos = FastUtils.cos(deg);
						double sin = FastUtils.sin(deg);
						new PartialParticle(Particle.REDSTONE, loc.clone().add(8 * cos, 0, 8 * sin), 1, 0.15, 0.15, 0.15, RED_COLOR).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.REDSTONE, loc.clone().add(4 * cos, 0, 4 * sin), 1, 0.15, 0.15, 0.15, RED_COLOR).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.REDSTONE, loc.clone().add(2 * cos, 0, 2 * sin), 2, 0.15, 0.15, 0.15, RED_COLOR).spawnAsEntityActive(mBoss);

						new PartialParticle(Particle.DRAGON_BREATH, loc.clone().add(8 * cos, 1, 8 * sin), 1, 0.15, 0.15, 0.15, 0.05).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.DRAGON_BREATH, loc.clone().add(4 * cos, 1, 4 * sin), 1, 0.15, 0.15, 0.15, 0.05).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.DRAGON_BREATH, loc.clone().add(2 * cos, 1, 2 * sin), 2, 0.15, 0.15, 0.15, 0.05).spawnAsEntityActive(mBoss);

						new PartialParticle(Particle.REDSTONE, loc.clone().add(8 * cos, 2, 8 * sin), 1, 0.15, 0.15, 0.15, RED_COLOR).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.REDSTONE, loc.clone().add(4 * cos, 2, 4 * sin), 1, 0.15, 0.15, 0.15, RED_COLOR).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.REDSTONE, loc.clone().add(2 * cos, 2, 2 * sin), 2, 0.15, 0.15, 0.15, RED_COLOR).spawnAsEntityActive(mBoss);
					}
				}
				mT += 5;
			}
		};

		runnable.runTaskTimer(mPlugin, 0, 5);
		mActiveRunnables.add(runnable);
	}

	@Override
	public void cancel() {
		super.cancel();

		/* Need to make a copy of the growables list because it also removes entries from the list when they are cancelled */
		for (GrowableProgress growable : new ArrayList<>(mGrowables)) {
			growable.cancel();
		}
		mGrowables.clear();

		for (Location loc : mLocs) {
			Location l = loc.clone();
			//Deletes icicles in air once cancelled
			for (int y = -15; y <= 0; y++) {
				for (int x = -6; x < 6; x++) {
					for (int z = -6; z < 6; z++) {
						l.set(loc.getX() + x, loc.getY() + y + 20, loc.getZ() + z);
						Block b = l.getBlock();
						if (b.getType() == Material.BLUE_ICE || b.getType() == Material.SNOW_BLOCK) {
							b.setType(Material.AIR);
						}
					}
				}
			}
		}
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}

	@Override
	public int cooldownTicks() {
		return 7 * 20;
	}

}
