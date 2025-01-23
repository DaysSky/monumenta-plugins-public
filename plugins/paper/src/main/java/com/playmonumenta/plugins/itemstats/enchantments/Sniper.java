package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.EnumSet;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;

public class Sniper implements Enchantment {
	public static final int DISTANCE = 12;
	public static final int DAMAGE_PER_LEVEL = 2;

	@Override
	public String getName() {
		return "Sniper";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.SNIPER;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	@Override
	public double getPriorityAmount() {
		return 7;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE && !event.isCancelled()) {
			if (event.getDamager() instanceof AbstractArrow arrow && !(arrow instanceof Trident) && !arrow.isCritical()) {
				return;
			}

			event.setFlatDamage(event.getFlatDamage() + apply(player, enemy, level));
		}
	}

	public static double apply(Player player, LivingEntity target, double level) {
		return apply(player, target.getEyeLocation(), level);
	}

	public static double apply(Player player, Location target, double level) {
		if (level > 0 && player.getEyeLocation().distance(target) > DISTANCE) {
			particles(target, player);
			return (level * DAMAGE_PER_LEVEL);
		}
		return 0;
	}

	public static void particles(Location loc, Player player) {
		new PartialParticle(Particle.CRIT, loc, 30, 0, 0, 0, 0.65).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, loc, 30, 0, 0, 0, 0.65).spawnAsPlayerActive(player);
		player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.6f, 0.5f);
	}

}
