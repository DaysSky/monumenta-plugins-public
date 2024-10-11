package com.playmonumenta.plugins.listeners;

import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.RespawnStasis;
import com.playmonumenta.plugins.effects.Stasis;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.PotionEffectApplyEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.Nullable;

public class StasisListener implements Listener {

	public static boolean isInStasis(@Nullable Entity entity) {
		// Only players can stasis, at least for now
		// No need to do a bunch of iteration to check on other entities
		if (!(entity instanceof Player)) {
			return false;
		}
		// RespawnStasis extends stasis, so this catches both
		return Plugin.getInstance().mEffectManager.hasEffect(entity, Stasis.class);
	}

	public static boolean isInRespawnStasis(@Nullable Entity entity) {
		return entity instanceof Player && Plugin.getInstance().mEffectManager.hasEffect(entity, RespawnStasis.class);
	}

	public static @Nullable RespawnStasis getRespawnStasis(@Nullable Entity entity) {
		if (!(entity instanceof Player)) {
			return null;
		}
		return Plugin.getInstance().mEffectManager.getActiveEffect(entity, RespawnStasis.class);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if (isInStasis(player)) {
			// allow falling in respawn stasis
			if (Math.abs(event.getTo().getX() - event.getFrom().getX()) < 0.00001
				    && Math.abs(event.getTo().getZ() - event.getFrom().getZ()) < 0.00001
				    && event.getTo().getY() < event.getFrom().getY()
				    && isInRespawnStasis(player)
				    && player.getGameMode() != GameMode.SPECTATOR) {
				return;
			}

			RespawnStasis rs = Plugin.getInstance().mEffectManager.getActiveEffect(player, RespawnStasis.class);
			if (rs != null
				    && rs.getDuration() < RespawnStasis.DURATION - RespawnStasis.SPECTATE_DURATION
				    && MetadataUtils.checkOnceInRecentTicks(Plugin.getInstance(), player, "RespawnStasisNearbyMobsCheck", 5)
				    && EntityUtils.getNearbyMobs(player.getLocation(), 8).isEmpty()
			) {
				endRespawnStasis(player);
				return;
			}

			Location to = event.getFrom();
			to.setPitch(event.getTo().getPitch());
			to.setYaw(event.getTo().getYaw());
			event.setTo(to);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void damageEvent(DamageEvent event) {
		if (event.getCause() == EntityDamageEvent.DamageCause.VOID || event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		if (isInStasis(event.getDamager()) || isInStasis(event.getDamagee())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityCombustByEntityEvent(EntityCombustByEntityEvent event) {
		if (isInStasis(event.getEntity()) || isInStasis(event.getCombuster())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void projectileLaunchEvent(ProjectileLaunchEvent event) {
		ProjectileSource source = event.getEntity().getShooter();
		if (source instanceof Player player && isInStasis(player)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void projectileHitEvent(ProjectileHitEvent event) {
		Projectile proj = event.getEntity();
		ProjectileSource source = proj.getShooter();
		if (source instanceof Player player && isInStasis(player)) {
			event.setCancelled(true);
			proj.remove();
		}

		if (isInStasis(event.getHitEntity())) {
			event.setCancelled(true);
			proj.remove();
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerItemConsumeEvent(PlayerItemConsumeEvent event) {
		if (isInStasis(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void potionEffectApplyEvent(PotionEffectApplyEvent event) {
		if (isInStasis(event.getApplied()) || isInStasis(event.getApplier())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		if (isInStasis(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		if (isInStasis(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		if (isInStasis(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerItemDamageEvent(PlayerItemDamageEvent event) {
		if (isInStasis(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	// RespawnStasis-specific event handlers

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (isInRespawnStasis(event.getWhoClicked())) {
			event.setCancelled(true);
			GUIUtils.refreshOffhand(event);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void inventoryDragEvent(InventoryDragEvent event) {
		if (isInRespawnStasis(event.getWhoClicked())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityTargetEvent(EntityTargetEvent event) {
		if (isInRespawnStasis(event.getTarget())) {
			event.setCancelled(true);
		}
	}

	private void endRespawnStasis(Player player) {
		RespawnStasis respawnStasis = Plugin.getInstance().mEffectManager.getActiveEffect(player, RespawnStasis.class);
		if (respawnStasis != null && (respawnStasis.getDuration() < RespawnStasis.DURATION - RespawnStasis.MINIMUM_DURATION)) {
			respawnStasis.setDuration(0);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerInteractEvent(PlayerInteractEvent event) {
		if (event.getAction() == Action.PHYSICAL) {
			return;
		}
		if (isInRespawnStasis(event.getPlayer())) {
			event.setCancelled(true);
			// Allow "respawning" after 1 second (to prevent accidental instant respawns)
			endRespawnStasis(event.getPlayer());
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerAnimationEvent(PlayerAnimationEvent event) {
		if (isInRespawnStasis(event.getPlayer())) {
			event.setCancelled(true);
			endRespawnStasis(event.getPlayer());
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityRegainHealthEvent(EntityRegainHealthEvent event) {
		if (isInStasis(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerToggleSneakEvent(PlayerToggleSneakEvent event) {
		if (isInStasis(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void playerStopSpectatingEntityEvent(PlayerStopSpectatingEntityEvent event) {
		Player player = event.getPlayer();
		RespawnStasis rs = Plugin.getInstance().mEffectManager.getActiveEffect(player, RespawnStasis.class);
		if (rs != null && !rs.mCanStopSpectating) {
			// For some reason cancelling the event softlocks you, but this works
			player.setSpectatorTarget(event.getSpectatorTarget());
		}
	}

}
