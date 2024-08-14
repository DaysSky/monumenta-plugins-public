package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

/**
 * @deprecated use boss_projectile instead, like this:
 * <blockquote><pre>
 * /bos var Tags add boss_projectile
 * /bos var Tags add boss_projectile[damage=24,speed=0.2,delay=20,cooldown=320,turnRadius=3.141]
 * /bos var Tags add boss_projectile[SoundStart=[(BLOCK_BEACON_POWER_SELECT,1,0.5)],SoundLaunch=[(ENTITY_EVOKER_CAST_SPELL,1,0.5)],SoundProjectile=[(BLOCK_BEACON_POWER_SELECT,0.4,0.5)],SoundHit=[(BLOCK_BEACON_DEACTIVATE,1,0.5)]]
 * /bos var Tags add boss_projectileboss_projectile[ParticleLaunch=[(SPELL_WITCH,40,0,0,0,0.3)],ParticleProjectile=[(SPELL_WITCH,6,0,0,0,0.3),(SMOKE_LARGE,2,0.2,0.2,0.2,0)],ParticleHit=[(SPELL_WITCH,50,0,0,0,0.3)]]
 * </pre></blockquote>
 * G3m1n1Boy
 */
@Deprecated
public class TrackingProjectileBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_trackingprojectile";
	public static final int detectionRange = 24;

	private static final int DAMAGE = 24;
	private static final int DELAY = 20 * 1;
	private static final double SPEED = 0.2;
	private static final int COOLDOWN = 20 * 16;
	private static final boolean LINGERS = true;
	private static final double HITBOX_LENGTH = 0.5;
	private static final int LIFETIME_TICKS = 20 * 16;
	private static final double TURN_RADIUS = Math.PI;
	private static final boolean SINGLE_TARGET = true;
	private static final boolean LAUNCH_TRACKING = true;
	private static final boolean COLLIDES_WITH_BLOCKS = true;
	private static final NamedTextColor COLOR = NamedTextColor.RED;

	public TrackingProjectileBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Spell spell = new SpellBaseSeekingProjectile(plugin, boss, detectionRange, SINGLE_TARGET, LAUNCH_TRACKING, COOLDOWN, DELAY,
				SPEED, TURN_RADIUS, LIFETIME_TICKS, HITBOX_LENGTH, COLLIDES_WITH_BLOCKS, LINGERS,
				// Initiate Aesthetic
				(World world, Location loc, int ticks) -> {
					GlowingManager.startGlowing(boss, COLOR, DELAY, GlowingManager.BOSS_SPELL_PRIORITY);
					world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 1f, 0.5f);
				},
				// Launch Aesthetic
				(World world, Location loc, int ticks) -> {
					new PartialParticle(Particle.SPELL_WITCH, loc, 40, 0, 0, 0, 0.3).spawnAsEntityActive(boss);
					world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 1f, 0.5f);
				},
				// Projectile Aesthetic
				(World world, Location loc, int ticks) -> {
					new PartialParticle(Particle.SPELL_WITCH, loc, 6, 0, 0, 0, 0.3).spawnAsEntityActive(boss);
					new PartialParticle(Particle.SMOKE_LARGE, loc, 2, 0.4, 0.4, 0.4, 0).spawnAsEntityActive(boss);
					if (ticks % 40 == 0) {
						world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 0.4f, 0.5f);
					}
				},
				// Hit Action
				(World world, @Nullable LivingEntity player, Location loc, @Nullable Location prevLoc) -> {
					world.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 2f, 0.5f);
					new PartialParticle(Particle.SPELL_WITCH, loc, 50, 0, 0, 0, 0.25).spawnAsEntityActive(boss);
					if (player != null) {
						BossUtils.blockableDamage(boss, player, DamageType.MAGIC, DAMAGE, prevLoc);
					}
				});

		super.constructBoss(spell, detectionRange);
	}
}
