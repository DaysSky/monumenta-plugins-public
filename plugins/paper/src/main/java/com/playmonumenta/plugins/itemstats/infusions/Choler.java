package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.InfernoDamage;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class Choler implements Infusion {

	public static final double DAMAGE_MLT_PER_LVL = 0.01;

	@Override
	public String getName() {
		return "Choler";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.CHOLER;
	}

	@Override
	public double getPriorityAmount() {
		return 23;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		if (EntityUtils.isStunned(enemy) || EntityUtils.isSlowed(plugin, enemy) || enemy.hasPotionEffect(PotionEffectType.SLOW)
			    || enemy.getFireTicks() > 0 || plugin.mEffectManager.hasEffect(enemy, InfernoDamage.class)) {
			event.updateGearDamageWithMultiplier(getDamageDealtMultiplier(value));
		}
	}

	public static double getDamageDealtMultiplier(double level) {
		return 1 + DAMAGE_MLT_PER_LVL * level;
	}

}
