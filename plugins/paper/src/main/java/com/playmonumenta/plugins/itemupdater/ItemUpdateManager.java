package com.playmonumenta.plugins.itemupdater;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.inventories.CustomContainerItemManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.tracking.PlayerTracking;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBTCompoundList;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.EnderSignal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.AbstractHorseInventory;
import org.bukkit.inventory.ArmoredHorseInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.LlamaInventory;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class ItemUpdateManager implements Listener {
	// Updates items if needed as they load.
	private final Plugin mPlugin;

	public ItemUpdateManager(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		String pathNode = "PlayerJoinEvent " + player.getName();
		mPlugin.getLogger().fine("ItemUpdateManager: " + pathNode);

		List<String> path = new ArrayList<>();
		path.add(pathNode);
		List<String> subPath;

		// This is separate from the player inventory.
		@Nullable ItemStack[] enderItems = player.getEnderChest().getContents();
		for (int i = 0; i < enderItems.length; i++) {
			subPath = new ArrayList<>(path);
			subPath.add("in enderchest slot " + i);
			try {
				updateNested(subPath, enderItems[i]);
			} catch (Exception e) {
				logNestedException(subPath, e);
			}
		}

		// Entities specified are saved to the player file.
		subPath = new ArrayList<>(path);
		subPath.add("the following is the player's vehicle: ");
		try {
			updateNested(subPath, player.getVehicle());
		} catch (Exception e) {
			logNestedException(subPath, e);
		}

		subPath = new ArrayList<>(path);
		subPath.add("the following is on the player's left shoulder: ");
		try {
			updateNested(subPath, PlayerUtils.getPlayerShoulderEntity(player, false));
		} catch (Exception e) {
			logNestedException(subPath, e);
		}

		subPath = new ArrayList<>(path);
		subPath.add("the following is on the player's right shoulder: ");
		try {
			updateNested(subPath, PlayerUtils.getPlayerShoulderEntity(player, true));
		} catch (Exception e) {
			logNestedException(subPath, e);
		}

		// Update the rest of the player's stuff as an entity.
		try {
			updateNested(path, (Entity) player);
		} catch (Exception e) {
			logNestedException(path, e);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerAttemptPickupItem(PlayerAttemptPickupItemEvent event) {
		List<String> path = new ArrayList<>();
		path.add("PlayerAttemptPickupItemEvent");

		try {
			Item entity = event.getItem();
			ItemStack item = entity.getItemStack();
			updateNested(path, item);
		} catch (Exception e) {
			logNestedException(path, e);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerDropItem(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		List<String> path = new ArrayList<>();
		path.add("PlayerDropItemEvent " + player.getName());

		final int droppedSlot = PlayerTracking.getInstance().getDroppedSlotId(event);

		Item entity = event.getItemDrop();
		if (entity.isValid()) {
			try {
				updateNested(path, entity.getItemStack());
			} catch (Exception e) {
				logNestedException(path, e);
			}
		}

		if (droppedSlot >= 0) {
			new BukkitRunnable() {
				@Override
				public void run() {
					@Nullable ItemStack item = player.getInventory().getItem(droppedSlot);
					if (item != null) {
						try {
							updateNested(path, item);
						} catch (Exception e) {
							logNestedException(path, e);
						}
					}
				}
			}.runTask(mPlugin);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityPickupItemEvent(EntityPickupItemEvent event) {
		List<String> path = new ArrayList<>();
		path.add("EntityPickupItemEvent");

		Item entity = event.getItem();
		ItemStack item = entity.getItemStack();
		try {
			updateNested(path, item);
		} catch (Exception e) {
			logNestedException(path, e);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void itemSpawnEvent(ItemSpawnEvent event) {
		List<String> path = new ArrayList<>();
		path.add("ItemSpawnEvent");

		Item itemEntity = event.getEntity();
		ItemStack item = itemEntity.getItemStack();
		try {
			updateNested(path, item);
		} catch (Exception e) {
			logNestedException(path, e);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void inventoryOpenEvent(InventoryOpenEvent event) {
		List<String> path = new ArrayList<>();
		path.add("InventoryOpenEvent");

		try {
			updateNested(path, event.getInventory());
		} catch (Exception e) {
			logNestedException(path, e);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityAddToWorldEvent(EntityAddToWorldEvent event) {
		if (event.getEntity() instanceof Merchant) {
			List<String> path = new ArrayList<>();
			path.add("EntityAddToWorldEvent");
			updateNested(path, event.getEntity());
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent event) {
		List<String> path = new ArrayList<>();
		path.add("PlayerArmorStandManipulateEvent");
		updateNested(path, event.getArmorStandItem());
	}

	public static void updateCharms(Player p, CharmManager.CharmType charmType, List<ItemStack> equippedCharms) {
		List<String> path = new ArrayList<>();
		path.add("Player " + p.getName()
		+ " updating charms (" + charmType + ")");

		List<String> subPath;
		int i = 0;
		for (ItemStack equippedCharm : equippedCharms) {
			subPath = new ArrayList<>(path);
			subPath.add(Integer.toString(i));
			updateNested(subPath, equippedCharm);
			i++;
		}
	}

	public static void updateNested(List<String> path, @Nullable ItemStack item) {
		updateNested(path, item, false);
	}

	public static void updateNested(List<String> path, @Nullable ItemStack item, boolean bypass) {
		if (ItemUtils.isNullOrAir(item)) {
			return;
		}

		try {
			if (!bypass && (!ItemStatUtils.isDirty(item) || GUIUtils.isPlaceholder(item))) {
				return;
			}
			// Update quiver before generating item stats
			if (ItemStatUtils.isQuiver(item)) {
				final List<String> pathClone = path;
				// Update arrows in quivers immediately - otherwise, they would need updating every time an arrow is shot or picked up
				NBT.modify(item, nbt -> {
					ReadWriteNBTCompoundList arrowNBTs = ItemStatUtils.getItemList(nbt);
					List<String> arrowPath = new ArrayList<>(pathClone);
					arrowPath.add("in quiver");
					for (ReadWriteNBT arrowNBT : arrowNBTs) {
						ItemStack arrow = NBT.itemStackFromNBT(arrowNBT);
						if (arrow == null) {
							continue;
						}
						long currentAmount = NBT.modify(arrow, arrowTag -> {
							long a = ItemStatUtils.addPlayerModified(arrowTag).getLong(CustomContainerItemManager.AMOUNT_KEY);
							ItemStatUtils.removePlayerModified(arrowTag);
							return a;
						});
						updateNested(arrowPath, arrow, true);
						NBT.modify(arrow, arrowTag -> {
							ItemStatUtils.addPlayerModified(arrowTag).setLong(CustomContainerItemManager.AMOUNT_KEY, currentAmount);
						});
						arrowNBT.clearNBT();
						arrowNBT.mergeCompound(NBT.itemStackToNBT(arrow));
					}
				});
			}

			ItemUpdateHelper.generateItemStats(item);
		} catch (Exception e) {
			path = new ArrayList<>(path);
			path.add("in ItemStack " + ItemUtils.getGiveCommand(item));
			logNestedException(path, e);
		}

		/* Updating containers nested in items disabled for now to improve performance.
		if (item.hasItemMeta()) {
			ItemMeta itemMeta = item.getItemMeta();
			if (itemMeta instanceof BlockStateMeta) {
				BlockStateMeta blockStateMeta = (BlockStateMeta) itemMeta;
				if (blockStateMeta.hasBlockState()) {
					BlockState blockState = blockStateMeta.getBlockState();
					try {
						updateNested(path, blockState);
					} catch (Exception e) {
						logNestedException(path, e);
					}
				}
			}

			// TODO Spawn eggs don't expose their contained entity NBT - now what?
		}
		*/
	}

	public static void updateNested(List<String> path, BlockState blockState) {
		if (blockState == null) {
			return;
		}

		path = new ArrayList<>(path);
		path.add("in BlockState");

		// TODO CreatureSpawner (Mob Spawner) does not expose its entities. Now what?

		if (blockState instanceof Jukebox) {
			try {
				updateNested(path, ((Jukebox) blockState).getRecord());
			} catch (Exception e) {
				logNestedException(path, e);
			}
		}

		if (blockState instanceof InventoryHolder) {
			try {
				updateNested(path, (InventoryHolder) blockState);
			} catch (Exception e) {
				logNestedException(path, e);
			}
		}
	}

	public static void updateNested(List<String> path, Entity entity) {
		if (entity == null) {
			return;
		}

		path = new ArrayList<>(path);
		Location location = entity.getLocation();
		path.add("in Entity " + entity.getType().getKey().asString()
			+ " named " + entity.getName()
			+ " at " + location.getWorld().getName() + " " + location.toVector());

		if (entity instanceof AbstractArrow) {
			// Includes tridents and all arrow types.
			try {
				updateNested(path, ((AbstractArrow) entity).getItemStack());
			} catch (Exception e) {
				logNestedException(path, e);
			}
		}

		if (entity instanceof AbstractHorse) {
			AbstractHorseInventory absHorseInv = ((AbstractHorse) entity).getInventory();
			try {
				updateNested(path, absHorseInv.getSaddle());
			} catch (Exception e) {
				logNestedException(path, e);
			}

			if (absHorseInv instanceof ArmoredHorseInventory) {
				try {
					updateNested(path, ((ArmoredHorseInventory) absHorseInv).getArmor());
				} catch (Exception e) {
					logNestedException(path, e);
				}
			}

			if (absHorseInv instanceof LlamaInventory) {
				try {
					updateNested(path, ((LlamaInventory) absHorseInv).getDecor());
				} catch (Exception e) {
					logNestedException(path, e);
				}
			}
		}

		if (entity instanceof Merchant merchant) {
			List<MerchantRecipe> offers = merchant.getRecipes();
			for (MerchantRecipe offer : offers) {
				List<ItemStack> items = offer.getIngredients();
				for (ItemStack item : items) {
					updateNested(path, item);
				}
				updateNested(path, offer.getResult());
				offer.setIngredients(items);
			}
			merchant.setRecipes(offers);
		}

		if (entity instanceof EnderSignal) {
			try {
				updateNested(path, ((EnderSignal) entity).getItem());
			} catch (Exception e) {
				logNestedException(path, e);
			}
		}

		// TODO Falling blocks can have a NBT tags, but there's no method for it. Now what?

		if (entity instanceof Item) {
			try {
				updateNested(path, ((Item) entity).getItemStack());
			} catch (Exception e) {
				logNestedException(path, e);
			}
		}

		if (entity instanceof ItemFrame) {
			try {
				updateNested(path, ((ItemFrame) entity).getItem());
			} catch (Exception e) {
				logNestedException(path, e);
			}
		}

		if (entity instanceof Merchant merchant) {
			for (int i = 0; i < merchant.getRecipeCount(); i++) {
				MerchantRecipe trade = merchant.getRecipe(i);
				List<ItemStack> ingredients = trade.getIngredients();
				for (int j = 0; j < ingredients.size(); j++) {
					List<String> subPath = new ArrayList<>(path);
					subPath.add("in trade " + i + " ingredient " + j);

					ItemStack ingredient = ingredients.get(j);
					try {
						updateNested(subPath, ingredient);
					} catch (Exception e) {
						logNestedException(subPath, e);
					}
				}
			}
		}

		// TODO SpawnerMinecart does not give access to its mobs. Now what?

		if (entity instanceof ThrowableProjectile) {
			try {
				updateNested(path, ((ThrowableProjectile) entity).getItem());
			} catch (Exception e) {
				logNestedException(path, e);
			}
		}

		if (entity instanceof InventoryHolder) {
			try {
				updateNested(path, (InventoryHolder) entity);
			} catch (Exception e) {
				logNestedException(path, e);
			}
		}

		for (Entity passenger : entity.getPassengers()) {
			if (passenger instanceof Player) {
				continue;
			}

			try {
				updateNested(path, passenger);
			} catch (Exception e) {
				logNestedException(path, e);
			}
		}
	}

	public static void updateNested(List<String> path, InventoryHolder inventoryHolder) {
		if (inventoryHolder == null) {
			return;
		}

		path = new ArrayList<>(path);
		path.add("in InventoryHolder");

		try {
			updateNested(path, inventoryHolder.getInventory());
		} catch (Exception e) {
			logNestedException(path, e);
		}
	}

	public static void updateNested(List<String> path, Inventory inventory) {
		if (inventory == null) {
			return;
		}

		path = new ArrayList<>(path);
		path.add("in Inventory");

		@Nullable ItemStack[] items = inventory.getContents();
		for (int i = 0; i < items.length; i++) {
			@Nullable ItemStack item = items[i];
			if (item != null) {
				List<String> subPath = new ArrayList<>(path);
				subPath.add("in slot " + i);
				try {
					updateNested(subPath, item);
				} catch (Exception e) {
					logNestedException(subPath, e);
				}
			}
		}
	}

	public static void logNestedException(List<String> path, Exception e) {
		Plugin.getInstance().getLogger().warning("ItemUpdateManager: An exception occurred:");
		for (String node : path) {
			Plugin.getInstance().getLogger().warning("ItemUpdateManager: " + node);
		}
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String sStackTrace = sw.toString();
		Plugin.getInstance().getLogger().warning(e.getLocalizedMessage() + "\n" + sStackTrace);
	}
}
