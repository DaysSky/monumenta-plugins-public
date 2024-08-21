package com.playmonumenta.plugins.listeners;

import com.destroystokyo.paper.event.entity.EndermanEscapeEvent;
import com.destroystokyo.paper.event.entity.EntityZapEvent;
import com.destroystokyo.paper.event.entity.PreSpawnerSpawnEvent;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Vex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

public class MobListener implements Listener {

	public static final int SPAWNER_DROP_THRESHOLD = 20;
	private static final NamespacedKey ARMED_ARMOR_STAND_LOOT_TABLE = NamespacedKeyUtils.fromString("epic:items/armed_armor_stand");
	private static final String SPAWNER_FIRST_SPAWN_ATTEMPT_METADATA_KEY = "MonumentaFirstSpawnAttempt";

	/**
	 * Set of entity types that may spawn both on land and floating in water.
	 */
	private static final EnumSet<EntityType> AMPHIBIOUS_MOBS = EnumSet.of(
		EntityType.DROWNED,
		EntityType.GUARDIAN
	);

	/**
	 * Set of entity types that may spawn in the air despite not being {@link EntityUtils#isFlyingMob(EntityType) flying mobs}.
	 */
	private static final EnumSet<EntityType> FALLING_MOBS = EnumSet.of(
		EntityType.CREEPER,
		EntityType.SPLASH_POTION,
		EntityType.PRIMED_TNT
	);

	private final Plugin mPlugin;

	public MobListener(Plugin plugin) {
		mPlugin = plugin;
	}

	/**
	 * This method handles spawner spawn rules. We use a Paper patch that disables all vanilla spawn rules for spawners,
	 * so all types of mobs can spawn anywhere if there's enough space for the mob.
	 */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	void preSpawnerSpawnEvent(PreSpawnerSpawnEvent event) {

		int currentTick = Bukkit.getServer().getCurrentTick();

		Block spawnerBlock = event.getSpawnerLocation().getBlock();

		int firstSpawnAttempt = MetadataUtils.getOrSetMetadata(spawnerBlock, SPAWNER_FIRST_SPAWN_ATTEMPT_METADATA_KEY, currentTick);

		EntityType type = event.getType();
		boolean inWater = LocationUtils.isLocationInWater(event.getSpawnLocation());

		// water entities: must spawn in water
		if (EntityUtils.isWaterMob(type) && !AMPHIBIOUS_MOBS.contains(type)) {
			if (!inWater) {
				event.setCancelled(true);
			}
			return;
		}

		// Amphibious entities can spawn in water even without a solid block below
		if (inWater && AMPHIBIOUS_MOBS.contains(type)) {
			return;
		}

		// Land entities: must not spawn in the air (i.e. must have a block with collision below)
		// Some entities like creepers don't follow this rule so that they can be used in drop creeper traps
		// after some ticks of failed spawns, air becomes a valid spawn point for all land mobs.
		if (!EntityUtils.isFlyingMob(type)
			    && !FALLING_MOBS.contains(type)
			    && currentTick - firstSpawnAttempt <= 5) {
			if (!event.getSpawnLocation().getBlock().getRelative(BlockFace.DOWN).getType().isSolid()) {
				event.setCancelled(true);
			}
			return;
		}

		// flying/dropping entities: can spawn anywhere, so no more checks

	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	void creatureSpawnEvent(CreatureSpawnEvent event) {
		LivingEntity entity = event.getEntity();
		CreatureSpawnEvent.SpawnReason spawnReason = event.getSpawnReason();

		if (spawnReason == CreatureSpawnEvent.SpawnReason.BUILD_WITHER ||
			    spawnReason == CreatureSpawnEvent.SpawnReason.CURED ||
			    spawnReason == CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE) {
			event.setCancelled(true);
			return;
		}

		// No natural bat or slime spawning
		if (
			(entity instanceof Bat || entity instanceof Slime)
				&& spawnReason.equals(CreatureSpawnEvent.SpawnReason.NATURAL)
		) {
			event.setCancelled(true);
			return;
		}

		// We need to allow spawning hostile mobs intentionally, but disable natural spawns.
		// It's easier to check the intentional ways than the natural ones.
		if (spawnReason != CreatureSpawnEvent.SpawnReason.CUSTOM &&
			    spawnReason != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG &&
			    spawnReason != CreatureSpawnEvent.SpawnReason.DISPENSE_EGG &&
			    spawnReason != CreatureSpawnEvent.SpawnReason.SPAWNER &&
			    spawnReason != CreatureSpawnEvent.SpawnReason.DEFAULT &&
			    spawnReason != CreatureSpawnEvent.SpawnReason.COMMAND &&
			    spawnReason != CreatureSpawnEvent.SpawnReason.BEEHIVE &&
			    EntityUtils.isHostileMob(entity, true) &&
			    ZoneUtils.hasZoneProperty(entity, ZoneProperty.NO_NATURAL_SPAWNS)) {
			event.setCancelled(true);
			return;
		}

		if (!(entity instanceof Player) && !(entity instanceof ArmorStand)) {

			// Mark mobs not able to pick-up items.
			entity.setCanPickupItems(false);

			// Overwrite drop chances for mob armor and held items
			EntityEquipment equipment = entity.getEquipment();
			if (equipment != null) {
				equipment.setHelmetDropChance(ItemUtils.getItemDropChance(equipment.getHelmet()));
				equipment.setChestplateDropChance(ItemUtils.getItemDropChance(equipment.getChestplate()));
				equipment.setLeggingsDropChance(ItemUtils.getItemDropChance(equipment.getLeggings()));
				equipment.setBootsDropChance(ItemUtils.getItemDropChance(equipment.getBoots()));
				equipment.setItemInMainHandDropChance(ItemUtils.getItemDropChance(equipment.getItemInMainHand()));
				equipment.setItemInOffHandDropChance(ItemUtils.getItemDropChance(equipment.getItemInOffHand()));
			}

			mPlugin.mZoneManager.applySpawnEffect(mPlugin, entity);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	void spawnerSpawnEvent(SpawnerSpawnEvent event) {
		CreatureSpawner spawner = event.getSpawner();

		/* This can apparently happen sometimes...? */
		if (spawner == null) {
			return;
		}

		Entity mob = event.getEntity();
		int spawnCount = 1;

		if (spawner.hasMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
			// There should only be one value - just use the latest one
			for (MetadataValue value : spawner.getMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
				// Previous value found - add one to it for the currently-spawning mob
				spawnCount = value.asInt() + 1;
			}
		}

		// Create new metadata entries
		spawner.setMetadata(Constants.SPAWNER_COUNT_METAKEY, new FixedMetadataValue(mPlugin, spawnCount));
		tagSpawnCountRecursively(mob, spawnCount);

		// Successful spawn: reset spawn attempt metadata
		Bukkit.getScheduler().runTask(mPlugin, () -> spawner.getBlock().removeMetadata(SPAWNER_FIRST_SPAWN_ATTEMPT_METADATA_KEY, mPlugin));

		// Delete the spawner if the spawned mob has the boss_spawner_delete tag
		if (mob.getScoreboardTags().contains("boss_spawner_delete")) {
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				if (spawner.getBlock().getType() == Material.SPAWNER) {
					spawner.getBlock().setType(Material.AIR);
				}
			});
		}
	}

	private void tagSpawnCountRecursively(Entity mob, int spawnCount) {
		mob.setMetadata(Constants.SPAWNER_COUNT_METAKEY, new FixedMetadataValue(mPlugin, spawnCount));
		for (Entity passenger : mob.getPassengers()) {
			tagSpawnCountRecursively(passenger, spawnCount);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		// Set base custom damage of crossbows and tridents before other modifications
		// No firework damage!
		if (event.getDamager() instanceof Firework) {
			event.setCancelled(true);
			return;
		}

		if (event.getEntity() instanceof Player) {
			Entity damager = event.getDamager();
			if (damager instanceof AbstractArrow arrow) {
				ProjectileSource source = arrow.getShooter();
				if (source instanceof LivingEntity le) {
					EntityEquipment equipment = le.getEquipment();
					if (equipment != null) {
						ItemStack mainhand = equipment.getItemInMainHand();
						Material material = mainhand.getType();
						if (material == Material.TRIDENT || material == Material.CROSSBOW) {
							ItemMeta meta = mainhand.getItemMeta();
							if (meta != null && meta.hasAttributeModifiers()) {
								Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(Attribute.GENERIC_ATTACK_DAMAGE);
								if (modifiers != null) {
									for (AttributeModifier modifier : modifiers) {
										if (modifier.getOperation() == Operation.ADD_NUMBER) {
											event.setDamage(modifier.getAmount() + 1);
										}
									}
								}
							}
						}
					}
				}
			} else if (damager instanceof Fireball fireball) {
				//Custom damage of fireball set by the horse jump strength attribute in the mainhand of the mob
				ProjectileSource source = fireball.getShooter();
				if (source instanceof LivingEntity le) {
					EntityEquipment equipment = le.getEquipment();
					if (equipment != null) {
						ItemStack mainhand = equipment.getItemInMainHand();

						if (mainhand != null) {
							ItemMeta meta = mainhand.getItemMeta();
							if (meta != null && meta.hasAttributeModifiers()) {
								Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(Attribute.HORSE_JUMP_STRENGTH);
								if (modifiers != null) {
									for (AttributeModifier modifier : modifiers) {
										if (modifier.getOperation() == Operation.ADD_NUMBER) {
											// if Ghast, then scale with distance as well
											if (source instanceof Ghast) {
												double maxOriginalDamage = 14 * fireball.getYield() + 1;
												double ratio = Math.max(0, (event.getDamage() - 1) / (maxOriginalDamage - 1));
												event.setDamage(ratio * (modifier.getAmount() + 1));
											} else {
												event.setDamage(modifier.getAmount() + 1);
											}
										}
									}
								}
							}
						}
					}
				}
			} else if (damager instanceof EvokerFangs fangs) {
				//Custom damage for evoker fangs, tied to main hand damage of evoker.
				LivingEntity source = fangs.getOwner();
				if (source != null) {
					EntityEquipment equipment = source.getEquipment();
					if (equipment != null) {
						ItemStack mainhand = equipment.getItemInMainHand();

						if (mainhand != null) {
							ItemMeta meta = mainhand.getItemMeta();
							if (meta != null && meta.hasAttributeModifiers()) {
								Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(Attribute.GENERIC_ATTACK_DAMAGE);
								if (modifiers != null) {
									for (AttributeModifier modifier : modifiers) {
										if (modifier.getOperation() == Operation.ADD_NUMBER) {
											event.setDamage(modifier.getAmount() + 1);
										}
									}
								}
							}
						}
					}
				} else {
					// Attempt to update fangs damage using EvokerFangDamage nbt tag if it exists.
					try {
						if (fangs.getPersistentDataContainer().has(new NamespacedKey(mPlugin, "evoker-fang-damage"))) {
							double damage = Objects.requireNonNull(fangs.getPersistentDataContainer().get(new NamespacedKey(mPlugin, "evoker-fang-damage"), PersistentDataType.DOUBLE));
							event.setDamage(damage);
						}
					} catch (Exception e) {
							MMLog.warning("[MobListener] Error while replacing EvokerFangs damage with custom EvokerFangDamage. Reason: " + e.getMessage());
							e.printStackTrace();
							return;
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityTargetEvent(EntityTargetEvent event) {
		// Fix a bug where zombies that damaged other zombies sometimes target themselves
		// Obviously there is no reasonable case where a mob should target itself so disable any time that happens
		if (event.getTarget() == event.getEntity()) {
			event.setCancelled(true);
		}
	}

	/* Prevent fire from catching in towns */
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	void blockIgniteEvent(BlockIgniteEvent event) {
		Block block = event.getBlock();

		// If the block is within a safezone, cancel the ignition unless it was from a player in creative mode
		if (ZoneUtils.hasZoneProperty(block.getLocation(), ZoneProperty.ADVENTURE_MODE)) {
			if (event.getCause().equals(BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL)) {
				Player player = event.getPlayer();
				if (player != null && player.getGameMode() != GameMode.ADVENTURE) {
					// Don't cancel the event for non-adventure players
					return;
				}
			}

			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void entityDeathEvent(EntityDeathEvent event) {
		LivingEntity livingEntity = event.getEntity();
		boolean shouldGenDrops = true;

		// Check if this mob was likely spawned by a grinder spawner
		if (livingEntity.hasMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
			int spawnCount = 0;

			// There should only be one value - just use the latest one
			for (MetadataValue value : livingEntity.getMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
				spawnCount = value.asInt();
			}

			if (spawnCount > SPAWNER_DROP_THRESHOLD) {
				shouldGenDrops = false;

				// Don't drop any exp
				event.setDroppedExp(0);

				// Remove all drops except special lore text items
				event.getDrops().removeIf(itemStack -> !ItemUtils.doDropItemAfterSpawnerLimit(itemStack));
			}
		}

		Player player = livingEntity.getKiller();
		if (player != null
			    && EntityUtils.isHostileMob(livingEntity)
			    && !livingEntity.getScoreboardTags().contains(EntityUtils.IGNORE_DEATH_TRIGGERS_TAG)) {
			//  Player kills a mob
			mPlugin.mItemStatManager.onKill(mPlugin, player, event, livingEntity);
			if (!livingEntity.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
				AbilityManager.getManager().entityDeathEvent(player, event, shouldGenDrops);
				for (Player p : PlayerUtils.playersInRange(livingEntity.getLocation(), 20, true)) {
					AbilityManager.getManager().entityDeathRadiusEvent(p, event, shouldGenDrops);
				}
			}
		}

		//Do not run below if it is the death of a player
		if (livingEntity instanceof Player) {
			return;
		}

		//Give wither to vexes spawned from the evoker that died so they die over time
		if (livingEntity instanceof Evoker) {
			List<LivingEntity> vexes = EntityUtils.getNearbyMobs(livingEntity.getLocation(), 30, EnumSet.of(EntityType.VEX));
			for (LivingEntity vex : vexes) {
				if (vex instanceof Vex && livingEntity.equals(((Vex) vex).getSummoner())) {
					vex.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 9999, 3));
				}
			}
		}

		// If the item has meta, run through the lore to check if it has quest item in the lore list
		// Don't do this for armor stands to prevent item duplication
		if (!(livingEntity instanceof ArmorStand)) {
			ListIterator<ItemStack> iter = event.getDrops().listIterator();
			while (iter.hasNext()) {
				ItemStack item = iter.next();
				if (item == null) {
					continue;
				}
				if (ItemUtils.isQuestItem(item)) {
					//Scales based off player count in a 20 meter radius, drops at least one quest item
					int count = PlayerUtils.playersInRange(livingEntity.getLocation(), 20, true).size();
					if (count < 1) {
						count = 1;
					}
					if (count > item.getAmount()) {
						item.setAmount(count);
					}
					return;
				}
			}
		}

		// Drop armed armor stands from armed variants
		if (livingEntity instanceof ArmorStand armorStand && armorStand.hasArms()) {
			List<ItemStack> drops = event.getDrops();
			if (drops.size() > 0 && drops.get(0).equals(new ItemStack(Material.ARMOR_STAND, 1))) {
				ItemStack armedArmorStand = InventoryUtils.getItemFromLootTable(event.getEntity(), ARMED_ARMOR_STAND_LOOT_TABLE);
				if (armedArmorStand != null) {
					drops.set(0, armedArmorStand);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void entityZapEvent(EntityZapEvent event) {
		if (event.getEntityType().equals(EntityType.VILLAGER)) {
			event.setCancelled(true);
		}
	}

	// disable Enderman teleportation
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void endermanEscapeEvent(EndermanEscapeEvent event) {
		event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityDamageEvent(EntityDamageEvent event) {
		// Make Endermen take no damage from water (unless drowning)
		if (event.getCause() == EntityDamageEvent.DamageCause.DROWNING
			    && event.getEntity() instanceof Enderman enderman
			    && enderman.getRemainingAir() > 0) {
			event.setCancelled(true);
		}
	}

}
