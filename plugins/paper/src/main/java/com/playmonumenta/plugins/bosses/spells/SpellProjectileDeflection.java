package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;

public class SpellProjectileDeflection extends Spell {
	private static final double MAX_DEFLECT_VELOCITY = 3.0;

	private final LivingEntity mBoss;

	public SpellProjectileDeflection(LivingEntity boss) {
		mBoss = boss;
	}

	@Override
	public void bossHitByProjectile(ProjectileHitEvent event) {
		Projectile proj = event.getEntity();
		if (proj.getShooter() instanceof Player player) {
			if (!(proj instanceof Trident)) {
				Projectile deflected = (Projectile) mBoss.getWorld().spawnEntity(proj.getLocation().subtract(proj.getVelocity().normalize()), proj.getType());
				deflected.setShooter(mBoss);
				if (deflected instanceof Arrow deflectedArrow && proj instanceof Arrow originalArrow) {
					deflectedArrow.setCritical(originalArrow.isCritical());

					deflectedArrow.setBasePotionType(originalArrow.getBasePotionType());
					for (PotionEffect effect : originalArrow.getCustomEffects()) {
						deflectedArrow.addCustomEffect(effect, true);
					}

				}
				deflected.setVelocity(LocationUtils.getDirectionTo(player.getLocation().add(0, 1.25, 0), deflected.getLocation()).multiply(Math.max(MAX_DEFLECT_VELOCITY, proj.getVelocity().length())));
				proj.remove();
			}
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 1, 2);
			new PartialParticle(Particle.FIREWORKS_SPARK, proj.getLocation(), 10, 0, 0, 0, 0.1).spawnAsEntityActive(mBoss);
		}
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damger, LivingEntity source) {
		if (event.getDamager() instanceof Projectile && source instanceof Player) {
			event.setCancelled(true);
		}
	}

	@Override
	public void run() {
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
