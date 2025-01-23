package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.InfernoDamage;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class Fueled implements Infusion {

	public static final double DR_PER_MOB = 0.003;
	private static final int MOB_CAP = 4;
	private static final double RADIUS = 8;

	@Override
	public String getName() {
		return "Fueled";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.FUELED;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.getType() != DamageEvent.DamageType.TRUE) {
			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(player.getLocation(), RADIUS);

			int count = 0;
			for (LivingEntity mob : mobs) {
				if (EntityUtils.isStunned(mob) || EntityUtils.isSlowed(plugin, mob) || mob.hasPotionEffect(PotionEffectType.SLOW)
					    || mob.getFireTicks() > 0 || plugin.mEffectManager.hasEffect(mob, InfernoDamage.class)) {
					count++;
				}
				if (count >= MOB_CAP) {
					break;
				}
			}

			double multiplier = 1 - (DR_PER_MOB * count * value);
			event.setFlatDamage(event.getDamage() * multiplier);
		}
	}

	public static double getDamageTakenMultiplier(double level) {
		return 1 - DR_PER_MOB * MOB_CAP * level;
	}

}
