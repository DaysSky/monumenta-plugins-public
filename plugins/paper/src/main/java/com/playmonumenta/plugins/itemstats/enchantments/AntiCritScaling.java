package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class AntiCritScaling implements Enchantment {

	public static final double CRIT_BONUS = 1.5;

	@Override
	public String getName() {
		return "AntiCritScaling";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ANTI_CRIT_SCALING;
	}

	@Override
	public double getPriorityAmount() {
		return -9999; // second damage modifier just after strength cancel
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && PlayerUtils.isFallingAttack(player)) {
			event.setFlatDamage(event.getFlatDamage() / CRIT_BONUS);
		}
	}
}
