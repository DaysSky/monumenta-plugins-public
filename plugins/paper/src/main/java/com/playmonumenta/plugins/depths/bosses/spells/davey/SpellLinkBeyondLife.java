package com.playmonumenta.plugins.depths.bosses.spells.davey;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.depths.bosses.Davey;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellLinkBeyondLife extends Spell {
	private static final String SUMMON_NAME_1 = "AbyssalTrilobite";
	private static final String SUMMON_NAME_2 = "DistortedScoundrel";
	private static final String SUMMON_NAME_3 = "DistortedCrewman";
	private static final String SUMMON_NAME_4 = "DecayedCorpse";
	private static final int SPAWN_COUNT = 4; // Summon count 4-8 depending on players alive
	private static final int RANGE = 10;
	private static final int MAX_MOBS = 15;
	private static final int ELITE_CHANCE_PER_FLOOR = 15;

	private final LivingEntity mBoss;
	private final int mCooldownTicks;
	private final int mFightNumber;

	public SpellLinkBeyondLife(LivingEntity boss, int cooldown, int fightNumber) {
		mBoss = boss;
		mCooldownTicks = cooldown;
		mFightNumber = fightNumber;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation();
		loc.getWorld().playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 20, 1);
		int summonCount = SPAWN_COUNT + PlayerUtils.playersInRange(mBoss.getLocation(), Davey.detectionRange, true).size();

		PlayerUtils.nearbyPlayersAudience(mBoss.getLocation(), Davey.detectionRange)
			.sendMessage(Component.text("[Davey]", NamedTextColor.GOLD)
				.append(Component.text(" Now ye've done it. She be watchin'. Help me, heathens of ", NamedTextColor.BLUE))
				.append(Component.text("ngbgbggb", NamedTextColor.BLUE, TextDecoration.OBFUSCATED))
				.append(Component.text("!", NamedTextColor.BLUE)));

		BukkitRunnable run = new BukkitRunnable() {
			int mTicks = 0;
			int mSummons = 0;

			@Override
			public void run() {
				mTicks++;

				if (mSummons >= summonCount) {
					this.cancel();
					return;
				}

				if (mTicks % 20 == 0) {
					double x = -1;
					double z = -1;
					int attempts = 0;
					//Summon the mob, every second
					//Try until we have air space to summon the mob
					while (x == -1 || loc.getWorld().getBlockAt(loc.clone().add(x, .25, z)).getType() != Material.AIR) {
						x = FastUtils.randomDoubleInRange(-RANGE, RANGE);
						z = FastUtils.randomDoubleInRange(-RANGE, RANGE);

						attempts++;
						//Prevent infinite loop
						if (attempts > 20) {
							break;
						}
					}
					//Summon the mob using our location
					Location sLoc = loc.clone().add(x, 0.25, z);
					loc.getWorld().playSound(sLoc, Sound.BLOCK_GRAVEL_BREAK, SoundCategory.HOSTILE, 1, 0.75f);
					new PartialParticle(Particle.BLOCK_CRACK, sLoc, 16, 0.25, 0.1, 0.25, 0.25, Material.GRAVEL.createBlockData()).spawnAsEntityActive(mBoss);
					Random r = new Random();
					int roll = r.nextInt(3);
					Entity summonedMob = null;
					if (isEliteSummon()) {
						summonedMob = LibraryOfSoulsIntegration.summon(sLoc, SUMMON_NAME_4);
						if (summonedMob != null) {
							summonedMob.addScoreboardTag(DelvesUtils.DELVE_MOB_TAG);
							for (Entity passenger : summonedMob.getPassengers()) {
								if (passenger != null) {
									passenger.addScoreboardTag(DelvesUtils.DELVE_MOB_TAG);
								}
							}
						}
					} else {
						if (roll == 0) {
							summonedMob = LibraryOfSoulsIntegration.summon(sLoc, SUMMON_NAME_1);
						} else if (roll == 1) {
							summonedMob = LibraryOfSoulsIntegration.summon(sLoc, SUMMON_NAME_2);
						} else if (roll == 2) {
							summonedMob = LibraryOfSoulsIntegration.summon(sLoc, SUMMON_NAME_3);
						}
					}

					if (summonedMob != null) {
						summonedMob.setPersistent(true);
					}
					mSummons++;
				}

			}
		};
		run.runTaskTimer(Plugin.getInstance(), 0, 1);
		mActiveRunnables.add(run);
	}

	public boolean isEliteSummon() {
		Random r = new Random();
		int roll = r.nextInt(100);
		if (roll < (Math.sqrt(mFightNumber) - 1) * ELITE_CHANCE_PER_FLOOR) {
			return true;
		}
		return false;
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}

	@Override
	public boolean canRun() {
		return EntityUtils.getNearbyMobs(mBoss.getLocation(), Davey.detectionRange).size() < MAX_MOBS;
	}
}
