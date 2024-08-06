package com.playmonumenta.plugins.bosses.bosses;

import com.destroystokyo.paper.entity.Pathfinder;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
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
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.loot.Lootable;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

/**
 * Places and destroys blocks to reach a target player.
 * @author G3m1n1Boy
 */
public class BlockPlacerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_blockplacer";

	public static final String STOP_PLACING_BLOCK = "STOP-boss_blockplacer";

	private static final int IDLE_TIME = 20 * 8;
	private static final int NEW_TARGET_RANGE = 25;

	private static final int BEELINE_DISTANCE = 8;

	public BlockPlacerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		/* TODO: Encoding all of the boss logic into one big spell like this is bad. This should be separated into 2
		    spells, one for block placement and remove current block destruction code in favor of SpellBlockBreak */
		List<Spell> spells = List.of(
			new Spell() {

				private int mNoTargetTicks = 0;
				private int mLowMovementTicks = 0;
				private final Mob mMob = (Mob) mBoss;
				private Location mLastLocation = mMob.getLocation();

				@Override
				public void run() {
					if (mBoss.getScoreboardTags().contains(STOP_PLACING_BLOCK) || ZoneUtils.hasZoneProperty(mBoss, ZoneUtils.ZoneProperty.BLOCKBREAK_DISABLED)) {
						return;
					}

					LivingEntity target = mMob.getTarget();
					List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), NEW_TARGET_RANGE, false);
					if (target instanceof Player player && !players.isEmpty()) {
						if (!player.getWorld().equals(mMob.getWorld())) {
							mMob.setTarget(null);
							return;
						}
						mNoTargetTicks = 0;
						execute(player);
					} else {
						mNoTargetTicks += 5;
						if (mNoTargetTicks > IDLE_TIME) {
							if (!players.isEmpty()) {
								mMob.setTarget(players.get(FastUtils.RANDOM.nextInt(players.size())));
							}
						}
					}
				}

				@Override
				public int cooldownTicks() {
					return 0;
				}


				private void execute(Player target) {
					Location targetLocation = target.getLocation();
					Location bossLocation = mMob.getLocation();
					Pathfinder pathfinder = mMob.getPathfinder();
					Pathfinder.PathResult path = pathfinder.getCurrentPath();

					if (bossLocation.distance(mLastLocation) < 0.25) {
						mLowMovementTicks += 5;
					} else {
						mLowMovementTicks = 0;
					}

					mLastLocation = bossLocation;

					if (mLowMovementTicks >= 20) {
						dig();
					}

					double distanceToTarget = bossLocation.distance(targetLocation);

					if (path == null) {
						if (!dig()) {
							bridge(pathfinder, bossLocation, targetLocation);
						}
					} else {
						double distanceToTargetFlat = bossLocation.clone().subtract(targetLocation).toVector().setY(0).length();
						if (mBoss.hasLineOfSight(target) && distanceToTargetFlat < BEELINE_DISTANCE / 2.0
							    && targetLocation.getY() - bossLocation.getY() < BEELINE_DISTANCE / 2.0) {
							bridge(pathfinder, bossLocation, targetLocation);
						}

						Location nextPoint = path.getNextPoint();
						Location finalPoint = path.getFinalPoint();
						if ((distanceToTarget > 2 && (finalPoint == null || finalPoint.distance(bossLocation) < 1))
							    || nextPoint == null
							    || (distanceToTarget < path.getNextPoint().distance(targetLocation)
								        && (distanceToTarget < BEELINE_DISTANCE || path.getFinalPoint().distance(targetLocation) > BEELINE_DISTANCE))) {
							if (!dig()) {
								bridge(pathfinder, bossLocation, targetLocation);
							}
						}
					}
				}

				// Copypasta-ed from SpellBlockBreak lol, RIP modularity cause I'm lazy
				public boolean dig() {
					boolean destroyedBlock = false;

					Location loc = mBoss.getLocation();
					/* Get a list of all blocks that impede the boss's movement */
					List<Block> badBlockList = new ArrayList<>();
					Location testloc = new Location(loc.getWorld(), 0, 0, 0);
					for (int x = -1; x <= 1; x++) {
						testloc.setX(loc.getX() + x);
						for (int y = 0; y <= 3; y++) {
							testloc.setY(loc.getY() + y);
							for (int z = -1; z <= 1; z++) {
								testloc.setZ(loc.getZ() + z);
								Block block = testloc.getBlock();
								Material material = block.getType();

								if (BlockUtils.isEnvHazardForMobs(material)) {
									/* Break these blocks immediately, don't add them to the bad block list */
									EntityExplodeEvent event = new EntityExplodeEvent(mBoss, mBoss.getLocation(), new ArrayList<>(List.of(block)), 0f);
									Bukkit.getServer().getPluginManager().callEvent(event);
									if (!event.isCancelled() && !event.blockList().isEmpty()) {
										/* Only allow bosses to break blocks in areas where explosions are allowed */
										testloc.getBlock().setType(Material.AIR);
										loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.BLOCKS, 0.2f, 0.6f);
										new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 6, 1, 1, 1, 0.03).spawnAsEntityActive(boss);
										destroyedBlock = true;
									}
								} else if (y > 0 && material.isSolid() && isBreakable(block, material)) {
									badBlockList.add(block);
								}
							}
						}
					}

					/* If more than two blocks, destroy all blocking blocks */
					if (badBlockList.size() > 2) {
						/* Call an event with these exploding blocks to give plugins a chance to modify it */
						EntityExplodeEvent event = new EntityExplodeEvent(mBoss, mBoss.getLocation(), badBlockList, 0f);
						Bukkit.getServer().getPluginManager().callEvent(event);
						if (event.isCancelled()) {
							return destroyedBlock;
						}

						/* Remove any remaining blocks, which might have been modified by the event */
						for (Block block : badBlockList) {
							block.setType(Material.AIR);
						}
						if (!badBlockList.isEmpty()) {
							loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.BLOCKS, 0.3f, 0.9f);
							new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 6, 1, 1, 1, 0.03).spawnAsEntityActive(boss);
							destroyedBlock = true;
						}
					}

					return destroyedBlock;
				}

				private void bridge(Pathfinder pathfinder, Location bossLocation, Location targetLocation) {
					List<Location> bridgeLocations = new ArrayList<>();
					Vector direction = targetLocation.clone().subtract(bossLocation).toVector().normalize();

					if (Math.abs(direction.getX()) > Math.abs(direction.getZ())) {
						if (direction.getX() > 0) {
							bridgeLocations.add(bossLocation.clone().add(1, -1, 0));
							if (bossLocation.getY() < targetLocation.getY()) {
								bridgeLocations.add(bossLocation.clone().add(1, 0, 0));
							}
						} else {
							bridgeLocations.add(bossLocation.clone().add(-1, -1, 0));
							if (bossLocation.getY() < targetLocation.getY()) {
								bridgeLocations.add(bossLocation.clone().add(-1, 0, 0));
							}
						}
					} else {
						if (direction.getZ() > 0) {
							bridgeLocations.add(bossLocation.clone().add(0, -1, 1));
							if (bossLocation.getY() < targetLocation.getY()) {
								bridgeLocations.add(bossLocation.clone().add(0, 0, 1));
							}
						} else {
							bridgeLocations.add(bossLocation.clone().add(0, -1, -1));
							if (bossLocation.getY() < targetLocation.getY()) {
								bridgeLocations.add(bossLocation.clone().add(0, 0, -1));
							}
						}
					}

					for (Location loc : bridgeLocations) {
						Block block = loc.getBlock();
						Material material = block.getType();
						if (BlockUtils.isMechanicalBlock(material) || block.isSolid() || BlockUtils.containsWater(block)) {
							continue;
						}

						if (ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.ADVENTURE_MODE)) {
							continue;
						}

						if (LocationUtils.blocksIntersectPlayer(null, loc.getWorld(), List.of(loc))) {
							continue;
						}

						block.setType(Material.POLISHED_BLACKSTONE_BRICKS);
						loc.getWorld().playSound(loc, Sound.BLOCK_NETHER_BRICKS_PLACE, SoundCategory.BLOCKS, 1f, 0.7f);
						pathfinder.moveTo(loc.clone().add(0, 1, 0));
						Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> pathfinder.moveTo(loc.clone().add(0, 1, 0)), 5);
					}
				}

				private static boolean isBreakable(Block block, Material material) {
					return !BlockUtils.isMechanicalBlock(material) && material.isSolid() &&
						       (!(block.getState() instanceof Lootable) || !((Lootable) block.getState()).hasLootTable());
				}
			}
		);

		super.constructBoss(SpellManager.EMPTY, spells, NEW_TARGET_RANGE * 2, null);
	}
}
