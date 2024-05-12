package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public final class SpectateBot extends GenericCommand implements Listener {
	public static final int MAX_RADIUS = 17;
	public static final int MIN_RADIUS = 4;
	public static final double DISTANCE_VELOCITY = 0.05;
	public static final int TICK_PERIOD = 1;
	public static final int DISTANCE_VELOCITY_ADJUST_PERIOD = 20;
	public static final double MAX_PITCH = 80;
	public static final double MIN_PITCH = 5;
	public static final int AUTO_PLAYER_SWITCH_TICKS = 3600;

	private static class SpectateContext {
		public double mYawVelocity = 0.1;
		public double mPitchVelocity = 0.07;

		public double mDistanceVelocity = 0;
		public double mYaw = 0;
		public double mPitch = 20;
		public double mDistance = MAX_RADIUS;
		public int mTimeSinceLastSwitch = 0;
		public @Nullable Player mTarget = null;
		public Location mLastTargetLoc;
		public final Player mSpectator;

		public SpectateContext(Player player) {
			mSpectator = player;
			mLastTargetLoc = player.getLocation();
		}
	}

	public final Map<UUID, SpectateContext> mSpectators = new HashMap<>();
	public @Nullable BukkitRunnable mRunnable = null;

	public SpectateBot(Plugin plugin) {
		/* No-argument variant which just is the sender (if they are a player) */
		new CommandAPICommand("spectatebot")
			.withPermission(CommandPermission.fromString("monumenta.command.spectatebot"))
			.executes((sender, args) -> {
				if (sender instanceof Player && ((Player) sender).getGameMode().equals(GameMode.SPECTATOR)) {
					run(plugin, (Player) sender);
				} else {
					throw CommandAPI.failWithString("This command must be run by a player in spectator mode!");
				}
			})
			.register();
	}

	private static @Nullable Player getPlayerToSpectate(Player spectator) {
		List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
		players.removeIf((p) -> p.equals(spectator) || p.getGameMode().equals(GameMode.SPECTATOR));
		if (players.isEmpty()) {
			return null;
		}
		Collections.shuffle(players);
		return players.get(0);
	}

	private static Location computeLoc(double distance, double yaw, double pitch, Location targetLoc) {
		Vector vec = new Vector(FastUtils.cos(Math.toRadians(yaw)),
		                        FastUtils.sin(Math.toRadians(pitch)),
								FastUtils.sin(Math.toRadians(yaw)));

		Location cameraLoc = targetLoc.clone();
		cameraLoc = cameraLoc.add(vec.normalize().multiply(distance));
		return cameraLoc;
	}

	private void run(Plugin plugin, Player player) {
		if (mSpectators.containsKey(player.getUniqueId())) {
			player.sendMessage(Component.text("You are no longer spectate botting", NamedTextColor.RED));
			mSpectators.remove(player.getUniqueId());
		} else {
			mSpectators.put(player.getUniqueId(), new SpectateContext(player));

			if (mRunnable == null || mRunnable.isCancelled()) {
				mRunnable = new BukkitRunnable() {
					int mTicks = 0;

					@Override
					public void run() {
						Iterator<Map.Entry<UUID, SpectateContext>> it = mSpectators.entrySet().iterator();
						while (it.hasNext()) {
							Map.Entry<UUID, SpectateContext> entry = it.next();
							SpectateContext ctx = entry.getValue();

							if (ctx.mTarget == null || !ctx.mTarget.isOnline() || ctx.mTimeSinceLastSwitch > AUTO_PLAYER_SWITCH_TICKS) {
								ctx.mTarget = getPlayerToSpectate(ctx.mSpectator);
								if (ctx.mTarget == null) {
									ctx.mSpectator.sendMessage(Component.text("No player to spectate", NamedTextColor.RED));
									it.remove();
									continue;
								} else {
									ctx.mSpectator.sendMessage(Component.text("Now spectating: " + ctx.mTarget.getName(), NamedTextColor.RED));
									ctx.mTimeSinceLastSwitch = 0;
									ctx.mLastTargetLoc = ctx.mTarget.getLocation();
									ctx.mSpectator.teleport(ctx.mLastTargetLoc);
								}
							}

							ctx.mTimeSinceLastSwitch++;

							/*
							 * Periodically skip a tick to allow the player to download the world
							 * Skip the first many ticks when switching targets
							 */
							if (ctx.mTimeSinceLastSwitch < 100 || FastUtils.RANDOM.nextInt(200) == 0) {
								/* Make sure the player won't get idle kicked */
								NmsUtils.getVersionAdapter().resetPlayerIdleTimer(ctx.mSpectator);
								continue;
							}

							/* Rolling average target location - 49 parts previous location, 1 part new location */
							Location targetRawLoc = ctx.mTarget.getLocation();
							Location targetLoc = ctx.mLastTargetLoc.multiply(49.0d).add(targetRawLoc).multiply(0.02d);

							/* Compute the camera aiming location */
							Location cameraLoc = computeLoc(ctx.mDistance, ctx.mYaw, ctx.mPitch, targetLoc);

							/* Compute where the camera should look */
							Vector lookVect = targetLoc.toVector().subtract(cameraLoc.toVector()).normalize();
							cameraLoc.setDirection(lookVect);

							/* Move the spectator and adjust their facing direction */
							ctx.mSpectator.teleport(cameraLoc);

							/* Compute the next location */
							ctx.mYaw += ctx.mYawVelocity;
							ctx.mPitch += ctx.mPitchVelocity;

							if (ctx.mPitch <= MIN_PITCH) {
								ctx.mPitchVelocity = Math.abs(ctx.mPitchVelocity);
							} else if (ctx.mPitch >= MAX_PITCH) {
								ctx.mPitchVelocity = -Math.abs(ctx.mPitchVelocity);
							}

							/* Adjust forward/outer zoom velocity at a slower rate */
							if (mTicks >= DISTANCE_VELOCITY_ADJUST_PERIOD) {
								mTicks = 0;
								if (LocationUtils.hasLineOfSight(targetRawLoc, cameraLoc)) {
									ctx.mDistanceVelocity = DISTANCE_VELOCITY;
								} else {
									ctx.mDistanceVelocity = -DISTANCE_VELOCITY;
								}
							}
							ctx.mDistance += ctx.mDistanceVelocity;
							if (ctx.mDistance > MAX_RADIUS) {
								ctx.mDistance = MAX_RADIUS;
							} else if (ctx.mDistance < MIN_RADIUS) {
								ctx.mDistance = MIN_RADIUS;
							}

							/* Adjust the player's velocity towards the next location to try to reduce screen jitter */
							Location nextCameraLoc = computeLoc(ctx.mDistance, ctx.mYaw, ctx.mPitch, targetLoc);

							ctx.mSpectator.setVelocity(nextCameraLoc.toVector().subtract(cameraLoc.toVector()).multiply(10.0d * TICK_PERIOD / 20.0d));
						}

						mTicks++;

						/* Cancel this runnable if there is nothing to do */
						if (mSpectators.isEmpty()) {
							this.cancel();
							mRunnable = null;
						}
					}
				};

				mRunnable.runTaskTimer(plugin, 0, TICK_PERIOD);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerGameModeChangeEvent(PlayerGameModeChangeEvent event) {
		Player player = event.getPlayer();

		// If the player switches out of spectator remove them from the list
		if (!event.getNewGameMode().equals(GameMode.SPECTATOR)) {
			mSpectators.remove(player.getUniqueId());
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();

		// If the player leaves the game remove them from the list
		mSpectators.remove(player.getUniqueId());
	}
}
