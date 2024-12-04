package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.ImperialConstruct;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellStonemason extends Spell {

	private static final String ABILITY_NAME = "Stonemason";
	private static final int CAST_TIME = 20 * 3;

	private final LivingEntity mBoss;
	private final Plugin mPlugin;
	private final int mRadius = 6;
	private final int mDamage;
	private final ChargeUpManager mChargeUp;
	private final Location mStartLoc;
	private final int mRange;

	public SpellStonemason(LivingEntity boss, Plugin plugin, Location startLoc, int range, int damage) {
		mBoss = boss;
		mPlugin = plugin;
		mStartLoc = startLoc;
		mRange = range;
		mDamage = damage;
		mChargeUp = ImperialConstruct.defaultChargeUp(mBoss, CAST_TIME, ABILITY_NAME, mRange);
	}

	@Override
	public void run() {

		List<Player> players = PlayerUtils.playersInRange(mStartLoc, mRange, true);
		World world = mBoss.getWorld();
		mChargeUp.setTime(0);
		List<Location> locs = new ArrayList<>();
		for (Player p : players) {
			locs.add(p.getLocation());
		}

		BukkitRunnable testRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (mChargeUp.nextTick(2)) {
					for (double degree = 0; degree < 360; degree += 15) {
						if (FastUtils.RANDOM.nextDouble() < 0.8) {
							double radian = Math.toRadians(degree);
							double cos = FastUtils.cos(radian);
							double sin = FastUtils.sin(radian);

							for (Location loc : locs) {
								loc.add(cos * mRadius, 0.5, sin * mRadius);
								new PartialParticle(Particle.SQUID_INK, loc, 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);
								loc.subtract(cos * mRadius, 0.5, sin * mRadius);
							}
						}
					}

					for (Location loc : locs) {
						Location tempLoc = loc.clone();
						for (int y = 0; y >= -15; y--) {
							tempLoc.set(loc.getX(), loc.getY() + y, loc.getZ());

							if (!tempLoc.getBlock().getType().isAir()) {
								loc.set(tempLoc.getX(), tempLoc.getY(), tempLoc.getZ());
								break;
							}
						}

						new PartialParticle(Particle.LAVA, loc, 20, 2, 0.1, 2, 0.25).spawnAsEntityActive(mBoss);
						world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1.5f, 2);
						world.playSound(loc, Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.HOSTILE, 1.5f, 0);

						for (Player p : PlayerUtils.playersInRange(loc, mRadius, true)) {
							DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MELEE, mDamage, null, true, true, "Stonemason");
							MovementUtils.knockAway(loc, p, 0f, 1f, false);
							world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 1.5f, 1);
						}
					}

					this.cancel();
				} else {
					if (mChargeUp.getTime() % 6 == 0) {
						for (double degree = 0; degree < 360; degree += 15) {
							double radian = Math.toRadians(degree);
							double cos = FastUtils.cos(radian);
							double sin = FastUtils.sin(radian);

							for (Location loc : locs) {
								loc.add(cos * mRadius, 0.5, sin * mRadius);
								if (FastUtils.RANDOM.nextDouble() < 0.5) {
									new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);
								} else {
									new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);
								}
								loc.subtract(cos * mRadius, 0.5, sin * mRadius);
							}
						}
					}

					for (Location loc : locs) {
						new PartialParticle(Particle.BLOCK_CRACK, loc, 10, 2, 0.1, 2, 0.25, Material.BONE_BLOCK.createBlockData()).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.LAVA, loc, 5, 2, 0.1, 2, 0.25).spawnAsEntityActive(mBoss);

						if (mChargeUp.getTime() % 6 == 0) {
							world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, SoundCategory.HOSTILE, 1, 0f);
						}
					}
				}
			}
		};
		testRunnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(testRunnable);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 5;
	}
}
