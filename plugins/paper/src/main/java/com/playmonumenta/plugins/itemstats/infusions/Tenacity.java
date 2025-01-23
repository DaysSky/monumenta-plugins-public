package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Tenacity implements Infusion {

	public static final double DAMAGE_REDUCTION_PER_LEVEL = 0.005;

	@Override
	public String getName() {
		return "Tenacity";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.TENACITY;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		event.setFlatDamage(event.getDamage() * getDamageTakenMultiplier(value));
	}

	public static double getDamageTakenMultiplier(double level) {
		return 1 - level * DAMAGE_REDUCTION_PER_LEVEL;
	}

}
