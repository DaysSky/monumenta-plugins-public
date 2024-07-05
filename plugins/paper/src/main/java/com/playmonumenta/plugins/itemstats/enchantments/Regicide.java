package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.EnumSet;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Regicide implements Enchantment {

	private static final double DAMAGE_BONUS_PER_LEVEL = 0.1;
	private static final double BOSS_BONUS_PER_LEVEL = 0.05;
	public static final String CHARM_DAMAGE = "Regicide Damage";

	@Override
	public String getName() {
		return "Regicide";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.REGICIDE;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET, Slot.PROJECTILE);
	}

	@Override
	public double getPriorityAmount() {
		return 28;
	}

	public static double calculateDamageMultiplier(double level, Player player, LivingEntity target, DamageEvent event) {
		if (EntityUtils.isElite(target)) {
			return (1 + CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, DAMAGE_BONUS_PER_LEVEL * level));
		} else if (EntityUtils.isBoss(target)) {
			return (1 + CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, BOSS_BONUS_PER_LEVEL * level));
		} else {
			return 1;
		}
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity target) {
		if (event.getType() != DamageType.AILMENT
			    && event.getType() != DamageType.POISON
			    && event.getType() != DamageType.FALL
			    && event.getType() != DamageType.OTHER
			    && event.getType() != DamageType.TRUE) {
				event.updateGearDamageWithMultiplier(calculateDamageMultiplier(level, player, target, event));
		}
	}
}
