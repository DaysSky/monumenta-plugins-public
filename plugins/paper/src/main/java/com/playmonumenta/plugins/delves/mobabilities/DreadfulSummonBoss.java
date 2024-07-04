package com.playmonumenta.plugins.delves.mobabilities;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.BossParameters;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

public class DreadfulSummonBoss extends BossAbilityGroup {
	public static final String identityTag = "Dreadful";
	private static final LoSPool DREADNAUGHTS_LAND_POOL = new LoSPool.LibraryPool("~DelveDreadnaughtLand");
	private static final LoSPool DREADNAUGHT_WATER_POOL = new LoSPool.LibraryPool("~DelveDreadnaughtWater");

	public static class Parameters extends BossParameters {
		public double SPAWN_CHANCE = 0;
	}

	final Parameters mParam;


	public DreadfulSummonBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = Parameters.getParameters(boss, identityTag, new Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), -1, null);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		if (event != null && EntityUtils.isElite(mBoss) && !DelvesUtils.isDelveMob(mBoss)) {
			int numSpawns = FastUtils.roundRandomly(mParam.SPAWN_CHANCE);
			for (int i = 0; i < numSpawns; i++) {
				Location loc = mBoss.getLocation();
				boolean isWaterLoc = BlockUtils.containsWater(loc.getBlock());
				Entity entity;
				loc.getWorld().playSound(loc, Sound.ENTITY_POLAR_BEAR_WARNING, SoundCategory.HOSTILE, 0.6f, 0.8f);
				loc.getWorld().playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.HOSTILE, 1.3f, 0.5f);
				loc.getWorld().playSound(loc, Sound.ENTITY_SKELETON_CONVERTED_TO_STRAY, SoundCategory.HOSTILE, 0.6f, 0.7f);
				if (isWaterLoc) {
					entity = DREADNAUGHT_WATER_POOL.spawn(loc);
				} else {
					entity = DREADNAUGHTS_LAND_POOL.spawn(loc);
				}

				// Safety net in case the Dreadnaught doesn't get summoned for some reason
				if (entity != null && mBoss.hasMetadata(Constants.SPAWNER_COUNT_METAKEY)) {
					/* Include the original mob's metadata for spawner counting on the Dreadnaught. This should prevent
					   drop farming from Dreadnaughts when a death event is detected in the mob listener. */
					entity.setMetadata(Constants.SPAWNER_COUNT_METAKEY, mBoss.getMetadata(Constants.SPAWNER_COUNT_METAKEY).get(0));
				}

				loc.add(0, 1, 0);
				new PartialParticle(Particle.FLAME, loc, 50, 0, 0, 0, 0.1).spawnAsEnemy();
				new PartialParticle(Particle.SMOKE_LARGE, loc, 50, 0.5, 1, 0.5, 0).spawnAsEnemy();
			}
		}
	}
}
