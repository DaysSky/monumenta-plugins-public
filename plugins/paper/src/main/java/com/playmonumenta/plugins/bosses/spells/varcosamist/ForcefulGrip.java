package com.playmonumenta.plugins.bosses.spells.varcosamist;

import com.playmonumenta.plugins.bosses.spells.SpellBaseSeekingProjectile;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class ForcefulGrip extends SpellBaseSeekingProjectile {
	public static final int detectionRange = 24;

	private static final boolean SINGLE_TARGET = false;
	private static final boolean LAUNCH_TRACKING = true;
	private static final int DELAY = 20 * 1;
	private static final double SPEED = 0.8;
	private static final double TURN_RADIUS = Math.PI / 90;
	private static final int LIFETIME_TICKS = 20 * 8;
	private static final double HITBOX_LENGTH = 0.5;
	private static final boolean COLLIDES_WITH_BLOCKS = true;
	private static final boolean LINGERS = true;
	private static final int DAMAGE = 20;


	public ForcefulGrip(Plugin plugin, LivingEntity boss, int cooldown, String dio) {
		super(plugin, boss, detectionRange, SINGLE_TARGET, LAUNCH_TRACKING, cooldown, DELAY,
			SPEED, TURN_RADIUS, LIFETIME_TICKS, HITBOX_LENGTH, COLLIDES_WITH_BLOCKS, LINGERS,
			// Initiate Aesthetic
			(World world, Location loc, int ticks) -> {
				PotionUtils.applyPotion(null, boss, new PotionEffect(PotionEffectType.GLOWING, DELAY, 0));
				world.playSound(loc, Sound.ITEM_CROSSBOW_LOADING_MIDDLE, SoundCategory.HOSTILE, 2f, 0.5f);
			},
			// Launch Aesthetic
			(World world, Location loc, int ticks) -> {
				new PartialParticle(Particle.SMOKE_NORMAL, loc, 1, 0, 0, 0, 0).spawnAsEntityActive(boss);
				world.playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, SoundCategory.HOSTILE, 2f, 0.5f);
			},
			// Projectile Aesthetic
			(World world, Location loc, int ticks) -> {
				if (ticks == 0) {
					PlayerUtils.nearbyPlayersAudience(boss.getLocation(), 50).sendMessage(Component.text(dio, NamedTextColor.RED));
				}
				new PartialParticle(Particle.CRIT, loc, 3, 0, 0, 0, 0.1).spawnAsEntityActive(boss);
				new PartialParticle(Particle.SMOKE_LARGE, loc, 4, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(boss);
				if (ticks % 40 == 0) {
					world.playSound(loc, Sound.ENTITY_ARROW_SHOOT, SoundCategory.HOSTILE, 2f, 0.2f);
				}
			},
			// Hit Action
			(World world, @Nullable LivingEntity player, Location loc, @Nullable Location prevLoc) -> {
				world.playSound(loc, Sound.ENTITY_ARMOR_STAND_BREAK, SoundCategory.HOSTILE, 1f, 0.5f);
				new PartialParticle(Particle.CRIT, loc, 50, 0, 0, 0, 0.25).spawnAsEntityActive(boss);
				if (player != null) {
					BossUtils.blockableDamage(boss, player, DamageType.PROJECTILE, DAMAGE, "Forceful Grip", prevLoc);
					MovementUtils.pullTowards(boss, player, 1);
				}
			});
	}
}
