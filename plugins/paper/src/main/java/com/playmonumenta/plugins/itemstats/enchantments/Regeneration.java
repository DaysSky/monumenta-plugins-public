package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import org.bukkit.entity.Player;

public class Regeneration implements Enchantment {
	// Level 1 heals 1 health total every 3s.
	// Divide by 4 because tick() is called 4 times per second
	private static final double TICK_HEALING_1 = 1d / 3 / 4;

	@Override
	public String getName() {
		return "Regeneration";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.OFFHAND, Slot.MAINHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET);
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.REGENERATION;
	}

	@Override
	public void tick(Plugin plugin, Player player, double level, boolean twoHertz, boolean oneHertz) {
		PlayerUtils.healPlayer(plugin, player, healPer5Ticks(level));
	}

	public static double healPer5Ticks(double level) {
		return TICK_HEALING_1 * Math.sqrt(level);
	}

}
