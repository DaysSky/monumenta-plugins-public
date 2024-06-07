package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class StarfallBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_starfall";

	public static class Parameters extends BossParameters {
		public int COOLDOWN = 20 * 8;
		public int DELAY = 20 * 2;

		public int DAMAGE = 0;
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.BLAST;
		public double DAMAGE_PERCENTAGE = 0.0;
		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";
		public EffectsList EFFECTS = EffectsList.EMPTY;

		public int TRACKING = 20;

		public int LOCKING_DURATION = 20 * 2;
		public int METEOR_SPEED = 2;
		public double HEIGHT = 16;

		@BossParam(help = "The radius of the explosion")
		public double SPHERE_RADIUS = 4;

		public EntityTargets TARGETS = EntityTargets.GENERIC_ONE_PLAYER_TARGET;
		public EntityTargets TARGETS_EXPLOSION = EntityTargets.GENERIC_PLAYER_TARGET.clone().setRange(5);

		@BossParam(help = "Set to true to target entities; set to false to spawn randomly in the range")
		public boolean DOES_TARGETING = true;

		@BossParam(help = "The range when set to spawn randomly (i.e. DOES_TARGETING = FALSE). Does nothing otherwise")
		public double RANGE = 12;

		@BossParam(help = "The number of starfalls to spawn when set to spawn randomly (i.e. DOES_TARGETING = FALSE). Does nothing otherwise")
		public int COUNT = 3;

		@BossParam(help = "the height above the target location to put particlecircle tracking particles")
		public double LOCKING_CIRCLE_HEIGHT = 0.3;

		public ParticlesList PARTICLE_CIRCLE = ParticlesList.fromString("[(FLAME,1,0,0,0,0.1)]");

		public ParticlesList PARTICLE_METEOR = ParticlesList.fromString("[(FLAME,30,0.1,0.1,0.1,0.1),(SMOKE_LARGE,3)]");
		public ParticlesList PARTICLE_EXPLOSION = ParticlesList.fromString("[(FLAME,175,0.1,0.1,0.1,0.25),(SMOKE_LARGE,50,0,0,0,0.25),(EXPLOSION_NORMAL,50,0,0,0,0.25)]");

		public SoundsList SOUND_LOCKING = SoundsList.fromString("[(ITEM_FIRECHARGE_USE,1,0)]");
		public SoundsList SOUND_METEOR = SoundsList.fromString("[(ENTITY_BLAZE_SHOOT,3,1)]");
		public SoundsList SOUND_EXPLOSION = SoundsList.fromString("[(ENTITY_DRAGON_FIREBALL_EXPLODE,3,1)]");
		@BossParam(help = "LibraryOfSouls name of the mob spawned when the grenade explodes")
		public LoSPool SPAWNED_MOB_POOL = LoSPool.EMPTY;
		@BossParam(help = "how often particles/sounds spawn during the starfall's falling sequence (BASED ON HEIGHT)")
		public int FALL_AESTHETIC_INTERVAL = 1;
		@BossParam(help = "knockback dealt by the meteor")
		public float KNOCKBACK = 0.6f;

	}

	public StarfallBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = Parameters.getParameters(boss, identityTag, new Parameters());
		Spell spell = new Spell() {
			@Override
			public void run() {
				if (p.DOES_TARGETING) {
					List<? extends LivingEntity> targets = p.TARGETS.getTargetsList(mBoss);
					targets.forEach(e -> spawnStarfall(p, mBoss, e.getLocation(), (ticks, oldLoc) -> ticks <= p.TRACKING ? e.getLocation() : oldLoc));
				} else {
					List<Location> locs = new ArrayList<>();
					for (int i = 1; i <= p.COUNT; i++) {
						locs.add(LocationUtils.randomLocationInCircle(mBoss.getLocation(), p.RANGE));
					}
					locs.forEach(l -> spawnStarfall(p, mBoss, l, (ticks, oldLoc) -> l));
				}
			}

			@Override
			public int cooldownTicks() {
				return p.COOLDOWN;
			}

			@Override
			public boolean canRun() {
				return !p.TARGETS.getTargetsList(mBoss).isEmpty() || !p.DOES_TARGETING;
			}
		};

		super.constructBoss(spell, (int) (p.TARGETS.getRange() * 2), null, p.DELAY);
	}

	private void spawnStarfall(Parameters p, LivingEntity boss, Location loc, BiFunction<Integer, Location, Location> locationGetter) {
		new BukkitRunnable() {
			int mTicks = 0;
			Location mLocation = loc;

			@Override
			public void run() {
				if (EntityUtils.shouldCancelSpells(mBoss)) {
					cancel();
					return;
				}
				if (mTicks >= p.LOCKING_DURATION) {
					new BukkitRunnable() {
						int mTicks = 0;
						final double mStep = p.METEOR_SPEED;
						double mCurrentHeight = p.HEIGHT;

						@Override
						public void run() {
							if (EntityUtils.shouldCancelSpells(mBoss)) {
								cancel();
								return;
							}

							mCurrentHeight -= mStep;
							mTicks++;
							Location meteorCenter = mLocation.clone().add(0, mCurrentHeight, 0);

							if (mCurrentHeight <= 0) {
								p.PARTICLE_EXPLOSION.spawn(boss, meteorCenter, p.TARGETS_EXPLOSION.getRange() / 2, p.TARGETS_EXPLOSION.getRange() / 2, p.TARGETS_EXPLOSION.getRange() / 2, 0.1);
								p.SOUND_EXPLOSION.play(meteorCenter);

								// don't spawn in safe zones!
								if (!ZoneUtils.hasZoneProperty(meteorCenter, ZoneUtils.ZoneProperty.RESIST_5) || ZoneUtils.hasZoneProperty(mBoss.getLocation(), ZoneUtils.ZoneProperty.BLITZ)) {
									Entity spawn = p.SPAWNED_MOB_POOL.spawn(meteorCenter);
									if (spawn != null) {
										summonPlugins(spawn);
									}
								}


								for (LivingEntity target : p.TARGETS_EXPLOSION.getTargetsListByLocation(mBoss, meteorCenter)) {

									if (p.DAMAGE > 0) {
										// Must be looking up to block
										BossUtils.blockableDamage(mBoss, target, p.DAMAGE_TYPE, p.DAMAGE, p.SPELL_NAME, meteorCenter);
									}

									if (p.DAMAGE_PERCENTAGE > 0.0) {
										BossUtils.bossDamagePercent(mBoss, target, p.DAMAGE_PERCENTAGE, meteorCenter, p.SPELL_NAME);
									}
									p.EFFECTS.apply(target, mBoss);
									MovementUtils.knockAway(meteorCenter, target, p.KNOCKBACK, true);
								}

								cancel();
							}
							if (mTicks % p.FALL_AESTHETIC_INTERVAL == 0) {
								p.SOUND_METEOR.play(meteorCenter, 3.0f, (float) (mCurrentHeight / p.HEIGHT) * 1.5f);
								p.PARTICLE_METEOR.spawn(boss, meteorCenter);
							}
						}
					}.runTaskTimer(mPlugin, 0, 1);

					cancel();
					return;
				}

				mLocation = locationGetter.apply(mTicks, mLocation);

				if (mTicks % 4 == 0) {
					double size = (p.LOCKING_DURATION - mTicks) / 20.0 * p.SPHERE_RADIUS;
					for (int degree = 0; degree <= 360; degree += 5) {
						double radiant = Math.toRadians(degree);
						Location l = mLocation.clone().add(FastUtils.cos(radiant) * size, p.LOCKING_CIRCLE_HEIGHT, FastUtils.sin(radiant) * size);
						p.PARTICLE_CIRCLE.spawn(boss, l);
					}
				}

				p.SOUND_LOCKING.play(mLocation, 1, 1.5f * (((float) mTicks + 1) / (float) p.LOCKING_DURATION));

				mTicks += 2;
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}

	public void summonPlugins(Entity summon) {

	}
}
