package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.ArrowConsumeEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;


public class Retrieval implements Enchantment {
	private static final float RETRIEVAL_CHANCE = 0.1f;

	@Override
	public String getName() {
		return "Retrieval";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.RETRIEVAL;
	}

	@Override
	public void onConsumeArrow(Plugin plugin, Player player, double level, ArrowConsumeEvent event) {
		double chance = RETRIEVAL_CHANCE * level;
		if (FastUtils.RANDOM.nextDouble() < chance) {
			player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 0.3f, 1.0f);
			event.setCancelled(true);
		}
	}

}
