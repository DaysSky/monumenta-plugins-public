package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class TealAntiCheat extends Spell {
	private static final double RADIUS = 26;
	private static final double HEIGHT_UP = 6;
	private static final double HEIGHT_DOWN = 3;
	private static final int RANGE = 50;

	private LivingEntity mBoss;
	private int mDuration;
	private Location mSpawnLoc;
	private int mTimer = 0;

	public TealAntiCheat(LivingEntity boss, int duration, Location spawnLoc) {
		mBoss = boss;
		mDuration = duration;
		mSpawnLoc = spawnLoc;
	}

	@Override
	public void run() {
		mTimer += 2;
		if (mTimer > mDuration) {
			mTimer = 0;
			for (Player player : PlayerUtils.playersInRange(mSpawnLoc, RANGE, true)) {
				Location loc = player.getLocation();
				double height = loc.getY() - mSpawnLoc.getY();
				if (LocationUtils.xzDistance(mSpawnLoc, loc) > RADIUS || (height > HEIGHT_UP && PlayerUtils.isOnGround(player)) || height < -HEIGHT_DOWN) {
					BossUtils.bossDamagePercent(mBoss, player, 0.85);
					player.sendMessage(Component.text("You are too far from the fight!", NamedTextColor.RED));
				}
			}
			for (Entity e : EntityUtils.getNearbyMobs(mSpawnLoc, 100)) {
				if (e.getScoreboardTags().contains("Boss") && !e.getName().contains("Marching Fate") && LocationUtils.xzDistance(mSpawnLoc, e.getLocation()) > RADIUS) {
					e.teleport(mSpawnLoc);
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
