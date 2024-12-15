package com.playmonumenta.plugins.bosses.spells.falsespirit;

import com.playmonumenta.plugins.bosses.bosses.FalseSpirit;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class DamageBlocker extends Spell {

	private static final double MAX_DEFLECT_VELOCITY = 3.0;

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private List<Player> mWarned = new ArrayList<Player>();
	private GatesOfHell mHell;
	private GatesOfHell mCeilingHell;

	public DamageBlocker(Plugin plugin, LivingEntity boss, GatesOfHell hell, GatesOfHell ceilingHell) {
		mPlugin = plugin;
		mBoss = boss;
		mHell = hell;
		mCeilingHell = ceilingHell;

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
				mWarned.clear();
			}
		}.runTaskTimer(mPlugin, 0, 20 * 5);
	}

	@Override
	public void bossHitByProjectile(ProjectileHitEvent event) {
		Projectile proj = event.getEntity();
		if (proj.getShooter() instanceof Player player) {
			if (player.getLocation().distance(mBoss.getLocation()) > 7) {
				//Do not do damage if farther than 7 blocks away
				if (!(proj instanceof Trident)) {
					Projectile deflected = (Projectile) mBoss.getWorld().spawnEntity(proj.getLocation().subtract(proj.getVelocity().normalize()), proj.getType());
					deflected.setShooter(mBoss);
					if (deflected instanceof Arrow arrow && proj instanceof Arrow projec) {
						arrow.setCritical(projec.isCritical());
						arrow.setBasePotionType(projec.getBasePotionType());
						for (PotionEffect effect : projec.getCustomEffects()) {
							arrow.addCustomEffect(effect, true);
						}
					}
					deflected.setVelocity(LocationUtils.getDirectionTo(player.getLocation().add(0, 1.25, 0), deflected.getLocation()).multiply(Math.min(MAX_DEFLECT_VELOCITY, proj.getVelocity().length())));
					proj.remove();
				}
			}
		}
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		if (source instanceof Player player) {
			if (player.getLocation().distance(mBoss.getLocation()) > 7 || mHell.checkPortals() || mCeilingHell.checkPortals()) {
				event.setCancelled(true);

				DamageEvent.DamageType type = event.getType();
				if (type != DamageEvent.DamageType.FIRE && type != DamageEvent.DamageType.AILMENT) {
					if (!mWarned.contains(player) && (mHell.checkPortals() || mCeilingHell.checkPortals())) {
						player.sendMessage(Component.text("Foolish. I am made of nothing. Your attacks shall do nothing to me while my gates are powered.", NamedTextColor.DARK_RED));
						mWarned.add(player);
					} else if (!mWarned.contains(player)) {
						player.sendMessage(Component.text("[Bhairavi]", NamedTextColor.GOLD).append(Component.text(" You must get closer! It's turning your attacks to nothing!", NamedTextColor.WHITE)));
						mWarned.add(player);
					}
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_ANVIL_PLACE, SoundCategory.HOSTILE, 1, 2);
					new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation(), 10, 0, 0, 0, 0.1).spawnAsEntityActive(mBoss);
				}
			} else {
				player.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 10, 1.5f);
			}
		}
	}

	@Override
	public void run() {
		Location bossLoc = mBoss.getLocation();
		if (mHell.checkPortals() || mCeilingHell.checkPortals()) {
			//Always block with portals up
			Vector vec;
			for (int y = 0; y < 2; y++) {
				for (double degree = 0; degree < 360; degree += 40) {
					double radian1 = Math.toRadians(degree);
					vec = new Vector(FastUtils.cos(radian1) * 1, y, FastUtils.sin(radian1) * 1);
					vec = VectorUtils.rotateYAxis(vec, bossLoc.getYaw());

					Location l = bossLoc.clone().add(vec);
					new PartialParticle(Particle.SPELL_INSTANT, l, 1, 0.1, 0.2, 0.1, 0).spawnAsEntityActive(mBoss);
				}
			}
		} else {
			//Show particles when player is further than 7 blocks to indicate no damage will be done
			for (Player player : PlayerUtils.playersInRange(bossLoc, FalseSpirit.detectionRange, true)) {
				if (bossLoc.distance(player.getLocation()) > 7) {
					Vector vec;
					for (int y = 0; y < 2; y++) {
						for (double degree = 0; degree < 360; degree += 40) {
							double radian1 = Math.toRadians(degree);
							vec = new Vector(FastUtils.cos(radian1) * 1, y, FastUtils.sin(radian1) * 1);
							vec = VectorUtils.rotateYAxis(vec, bossLoc.getYaw());

							Location l = bossLoc.clone().add(vec);
							new PartialParticle(Particle.SPELL_INSTANT, l, 1, 0.1, 0.2, 0.1, 0).spawnAsEntityActive(mBoss);
						}
					}
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 4;
	}


}
