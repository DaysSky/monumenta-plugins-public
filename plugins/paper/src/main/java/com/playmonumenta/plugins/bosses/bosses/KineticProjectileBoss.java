package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

public class KineticProjectileBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_kineticprojectile";
	public static final int detectionRange = 16;

	private static final boolean SINGLE_TARGET = true;
	private static final boolean LAUNCH_TRACKING = false;
	private static final int COOLDOWN = 20 * 4;
	private static final int DELAY = 10;
	private static final double SPEED = 0.7;
	private static final double TURN_RADIUS = 0;
	private static final int DISTANCE = 32;
	private static final int LIFETIME_TICKS = (int) (DISTANCE / SPEED);
	private static final double HITBOX_LENGTH = 1.25;
	private static final boolean COLLIDES_WITH_BLOCKS = true;
	private static final boolean LINGERS = true;
	private static final int DAMAGE = 20;
	private static final double RADIUS = 2.5;
	private static final float KNOCKBACK_SPEED = 0.5f;
	private static final NamedTextColor COLOR = NamedTextColor.RED;

	public KineticProjectileBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Spell spell = new SpellBaseSeekingProjectile(plugin, boss, detectionRange, SINGLE_TARGET, LAUNCH_TRACKING, COOLDOWN, DELAY,
				SPEED, TURN_RADIUS, LIFETIME_TICKS, HITBOX_LENGTH, COLLIDES_WITH_BLOCKS, LINGERS,
				// Initiate Aesthetic
				(World world, Location loc, int ticks) -> {
					GlowingManager.startGlowing(boss, COLOR, DELAY, GlowingManager.BOSS_SPELL_PRIORITY);
				},
				// Launch Aesthetic
				(World world, Location loc, int ticks) -> {
					world.playSound(loc, Sound.ENTITY_IRON_GOLEM_STEP, SoundCategory.HOSTILE, 1f, 0.5f);
				},
				// Projectile Aesthetic
				(World world, Location loc, int ticks) -> {
					new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 15, 0.2, 0.2, 0.2, 0.1).spawnAsEntityActive(boss);
				},
				// Hit Action
				(World world, @Nullable LivingEntity target, Location loc, @Nullable Location prevLoc) -> {
					world.playSound(loc, Sound.ENTITY_IRON_GOLEM_HURT, SoundCategory.HOSTILE, 1f, 0.5f);
					new PartialParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0).minimumCount(1).spawnAsEntityActive(boss);

					BoundingBox hitbox = new BoundingBox();
					hitbox.shift(loc);
					hitbox.expand(RADIUS);

					for (Player p : PlayerUtils.playersInRange(loc, RADIUS * 3, true)) {
						if (hitbox.overlaps(p.getBoundingBox())) {
							BossUtils.blockableDamage(boss, p, DamageType.MAGIC, DAMAGE, prevLoc);

							if (!p.equals(target)) {
								MovementUtils.knockAway(loc, p, KNOCKBACK_SPEED, false);
							}
						}
					}
				});

		super.constructBoss(spell, detectionRange);
	}
}
