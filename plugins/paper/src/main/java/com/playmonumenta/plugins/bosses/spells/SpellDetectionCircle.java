package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellDetectionCircle extends Spell {
	private final Plugin mPlugin;
	private final double mRadius;
	private final int mDuration;
	private final Location mCenter;
	private final Location mTarget;

	public static void registerCommand(Plugin plugin) {
		List<Argument<?>> arguments = new ArrayList<>();

		arguments.add(new LiteralArgument("detection_circle"));
		arguments.add(new LocationArgument("center_pos"));
		arguments.add(new IntegerArgument("radius", 1, 2000));
		arguments.add(new IntegerArgument("duration", 1, Integer.MAX_VALUE));
		arguments.add(new LocationArgument("redstone_pos"));

		new CommandAPICommand("mobspell")
			.withPermission(CommandPermission.fromString("mobspell.detectioncircle"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				new SpellDetectionCircle(plugin, args.getUnchecked("center_pos"), args.getUnchecked("radius"),
					args.getUnchecked("duration"), args.getUnchecked("redstone_pos")).run();
			})
			.register();
	}

	public SpellDetectionCircle(Plugin plugin, Location center, int radius, int duration, Location target) {
		mPlugin = plugin;
		mRadius = radius;
		mDuration = duration;
		mCenter = center;
		mTarget = target;
	}

	@Override
	public void run() {
		new BukkitRunnable() {
			private int mRunsLeft = mDuration;

			@Override
			public void run() {
				int precision = FastUtils.RANDOM.nextInt(40) + 50 + (int) (mRadius * 4);
				double increment = (2 * Math.PI) / precision;
				Location particleLoc = new Location(mCenter.getWorld(), 0, mCenter.getY() + 5, 0);
				double angle = 0;
				for (int j = 0; j < precision; j++) {
					angle = (double) j * increment;
					particleLoc.setX(mCenter.getX() + (mRadius * FastUtils.cos(angle)));
					particleLoc.setZ(mCenter.getZ() + (mRadius * FastUtils.sin(angle)));
					particleLoc.setY(mCenter.getY() + 5 * (double) (FastUtils.RANDOM.nextInt(120) - 60) / 60);
					new PartialParticle(Particle.SMOKE_LARGE, particleLoc, 1, 0.02, 0.02, 0.02, 0).spawnAsBoss();
				}

				for (Player player : mCenter.getWorld().getPlayers()) {
					if (player.getLocation().distance(mCenter) < mRadius &&
						    (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)) {
						mTarget.getBlock().setType(Material.REDSTONE_BLOCK);
						this.cancel();
						break;
					}
				}
				if (mRunsLeft <= 0) {
					this.cancel();
				}
				mRunsLeft -= 5;
			}
		}.runTaskTimer(mPlugin, 1, 5);
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}
}
