package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

@Deprecated
public class SeekingProjectileBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_seekingprojectile";

	public static class Parameters extends BossParameters {
		public int DAMAGE = 20;
		public int DISTANCE = 64;
		public double SPEED = 0.4;
		public int DETECTION = 24;
		public int DELAY = 20 * 1;
		public int COOLDOWN = 20 * 12;
		public boolean LINGERS = true;
		public double HITBOX_LENGTH = 0.5;
		public int FIRE_DURATION = 20 * 5;
		public boolean SINGLE_TARGET = true;
		public boolean LAUNCH_TRACKING = false;
		public double TURN_RADIUS = Math.PI / 30;
		public boolean COLLIDES_WITH_BLOCKS = true;
		public PotionEffectType EFFECT = PotionEffectType.UNLUCK;
		public int EFFECT_AMPLIFIER = 1;
		public int EFFECT_DURATION = 0;
		public PotionEffectType EFFECT_TWO = PotionEffectType.UNLUCK;
		public int EFFECT_AMPLIFIER_TWO = 1;
		public int EFFECT_DURATION_TWO = 0;
		public int ANTIHEAL_DURATION = 0;
		public String COLOR = "red";
	}

	public SeekingProjectileBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());
		Plugin customEffectInstance = Plugin.getInstance();
		int lifetimeTicks = (int) (p.DISTANCE / p.SPEED);

		Spell spell = new SpellBaseSeekingProjectile(plugin, boss, p.DETECTION, p.SINGLE_TARGET, p.LAUNCH_TRACKING, p.COOLDOWN, p.DELAY,
				p.SPEED, p.TURN_RADIUS, lifetimeTicks, p.HITBOX_LENGTH, p.COLLIDES_WITH_BLOCKS, p.LINGERS,
				// Initiate Aesthetic
				(World world, Location loc, int ticks) -> {
					GlowingManager.startGlowing(boss, NamedTextColor.NAMES.valueOr(p.COLOR, NamedTextColor.RED), p.DELAY, GlowingManager.BOSS_SPELL_PRIORITY);
					world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.HOSTILE, 1f, 0.5f);
				},
				// Launch Aesthetic
				(World world, Location loc, int ticks) -> {
					new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0).minimumCount(1).spawnAsEntityActive(boss);
					world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 0.5f, 0.5f);
				},
				// Projectile Aesthetic
				(World world, Location loc, int ticks) -> {
					new PartialParticle(Particle.FLAME, loc, 3, 0, 0, 0, 0.1).spawnAsEntityActive(boss);
					new PartialParticle(Particle.SMOKE_LARGE, loc, 2, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(boss);
					if (ticks % 40 == 0) {
						world.playSound(loc, Sound.ENTITY_BLAZE_BURN, SoundCategory.HOSTILE, 0.5f, 0.2f);
					}
				},
				// Hit Action
				(World world, @Nullable LivingEntity target, Location loc, @Nullable Location prevLoc) -> {
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.5f, 0.5f);
					new PartialParticle(Particle.FLAME, loc, 50, 0, 0, 0, 0.25).spawnAsEntityActive(boss);
					if (target != null) {
						BossUtils.blockableDamage(boss, target, DamageType.MAGIC, p.DAMAGE, prevLoc);
						if (p.FIRE_DURATION != 0) {
							EntityUtils.applyFire(plugin, p.FIRE_DURATION, target, boss);
						}
						if (p.EFFECT_DURATION != 0) {
							target.addPotionEffect(new PotionEffect(p.EFFECT, p.EFFECT_DURATION, p.EFFECT_AMPLIFIER, true, true));
						}
						if (p.EFFECT_DURATION_TWO != 0) {
							target.addPotionEffect(new PotionEffect(p.EFFECT_TWO, p.EFFECT_DURATION_TWO, p.EFFECT_AMPLIFIER_TWO, true, true));
						}
						if (p.ANTIHEAL_DURATION != 0) {
							customEffectInstance.mEffectManager.addEffect(target, "BossPercentHealEffect", new PercentHeal(p.ANTIHEAL_DURATION, 0.5));
							target.addPotionEffect(new PotionEffect(PotionEffectType.BAD_OMEN, p.ANTIHEAL_DURATION, 1, true, true));
						}
					}
				});

		super.constructBoss(spell, p.DETECTION);
	}
}
