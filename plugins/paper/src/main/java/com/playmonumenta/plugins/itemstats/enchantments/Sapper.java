package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SpawnerUtils;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;



public class Sapper implements Enchantment {

	@Override
	public String getName() {
		return "Sapper";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.SAPPER;
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, double value, BlockBreakEvent event) {
		//Needed for check below. Probably safe?
		ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
		if (ItemUtils.isPickaxe(item) && event.getBlock().getType() == Material.SPAWNER && !ItemUtils.isPickaxe(player.getInventory().getItemInOffHand())) {
			if (!SpawnerUtils.tryBreakSpawner(event.getBlock(), 1 + Plugin.getInstance().mItemStatManager.getEnchantmentLevel(event.getPlayer(), EnchantmentType.DRILLING), false)) {
				return;
			}
			PlayerUtils.healPlayer(plugin, player, value);
			new PartialParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 8, 0.4, 0.4, 0.4, 0.1).spawnAsPlayerActive(player);
		}
	}
}
