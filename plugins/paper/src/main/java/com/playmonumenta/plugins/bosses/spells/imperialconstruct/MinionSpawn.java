package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.bosses.bosses.ImperialConstruct;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class MinionSpawn extends Spell {

	private int mTimer = 0;
	private final int mDuration;
	private final int mMultiplier;

	private final LivingEntity mBoss;
	private final ImperialConstruct mConstruct;
	private Location mCurrentLoc;

	private final List<String> mMobs = Arrays.asList("AutomatedLyrata", "AutomatedVanguard");
	private final List<Entity> mSpawnedMobs = new ArrayList<>();

	public MinionSpawn(LivingEntity boss, ImperialConstruct construct, Location currentLoc, int duration, int multiplier) {
		mBoss = boss;
		mConstruct = construct;
		mCurrentLoc = currentLoc.clone();
		mDuration = duration;
		mMultiplier = multiplier;
	}

	@Override
	public void run() {
		mTimer += 2;
		if (mTimer >= mDuration) {
			mTimer = 0;

			Location loc = new Location(mBoss.getWorld(), mCurrentLoc.getX() + FastUtils.randomDoubleInRange(-18, 18), mCurrentLoc.getY() + 5, mCurrentLoc.getZ() + FastUtils.randomDoubleInRange(-18, 18));

			for (int i = 0; i < mConstruct.getArenaPlayers().size() * mMultiplier; i++) {
				mSpawnedMobs.add(LibraryOfSoulsIntegration.summon(loc.clone().add(FastUtils.randomDoubleInRange(-2, 2), 0, FastUtils.randomDoubleInRange(-2, 2)), mMobs.get(FastUtils.RANDOM.nextInt(mMobs.size()))));
			}

		}
	}

	public void setLocation(Location loc) {
		mCurrentLoc = loc.clone();
	}

	public void removeMobs() {
		for (Entity e : mSpawnedMobs) {
			e.remove();
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
