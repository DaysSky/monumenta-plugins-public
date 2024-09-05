package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.SpawnerUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class Eruption implements Enchantment {

	private static final float RADIUS = 5.0f;
	private static final float SAPPER_RADIUS = 8.0f;
	private static final float DAMAGE_PER_LEVEL = 4.0f;
	private static final Particle.DustOptions YELLOW_1_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f);
	private static final Particle.DustOptions YELLOW_2_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f);
	private static final Particle.DustOptions BLEED_COLOR = new Particle.DustOptions(Color.fromRGB(210, 44, 44), 1.0f);
	private static final Particle.DustOptions RED_COLOR = new Particle.DustOptions(Color.fromRGB(200, 0, 0), 1.0f);

	@Override
	public String getName() {
		return "Eruption";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.ERUPTION;
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, double level, BlockBreakEvent event) {
		ItemStack item = player.getInventory().getItemInMainHand();
		if (ItemUtils.isPickaxe(item) && event.getBlock().getType() == Material.SPAWNER) {
			if (!SpawnerUtils.tryBreakSpawner(event.getBlock(), 1 + Plugin.getInstance().mItemStatManager.getEnchantmentLevel(event.getPlayer(), EnchantmentType.DRILLING), false)) {
				return;
			}
			List<LivingEntity> mobs = EntityUtils.getNearbyMobs(event.getBlock().getLocation(), RADIUS);

			//Get enchant levels on pickaxe
			int fire = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.FIRE_ASPECT);
			int ice = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.ICE_ASPECT);
			int thunder = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.THUNDER_ASPECT);
			int decay = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.DECAY);
			int bleed = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.BLEEDING);
			int sapper = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.SAPPER) > 0 ? plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.SAPPER) : 0;
			int adrenaline = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.ADRENALINE) > 0 ? plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.ADRENALINE) : 0;
			int wind = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.WIND_ASPECT) > 0 ? plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.WIND_ASPECT) : 0;

			double damage = DAMAGE_PER_LEVEL * level;
			//Damage any mobs in the area
			for (LivingEntity mob : mobs) {
				DamageUtils.damage(player, mob, DamageType.OTHER, damage, ClassAbility.ERUPTION, false, true);
			}

			//Sapper Interaction
			if (sapper > 0) {
				player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 2.0f, 1.6f);
				player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.05f, 1.0f);
				new PartialParticle(Particle.HEART, event.getBlock().getLocation().add(0, 1, 0), 25, 1.5, 1.5, 1.5).spawnAsPlayerActive(player);
				for (Player p : PlayerUtils.playersInRange(event.getBlock().getLocation(), SAPPER_RADIUS, true)) {
					new PartialParticle(Particle.HEART, p.getEyeLocation(), 5, 1, 1, 1).spawnAsPlayerActive(player);
					if (p == player) {
						continue;
					}
					PlayerUtils.healPlayer(plugin, p, sapper, player);
				}
			}

			//Adrenaline Interaction
			if (adrenaline > 0) {
				for (Player p : PlayerUtils.playersInRange(event.getBlock().getLocation(), SAPPER_RADIUS, true)) {
					if (p == player) {
						continue;
					}
					new PartialParticle(Particle.REDSTONE, p.getLocation().add(0, 1, 0), 12, 0.4, 0.5, 0.4, RED_COLOR).spawnAsPlayerActive(player);
					// on block break adrenaline is reduced by 50%
					double speed = Adrenaline.PERCENT_SPEED_PER_LEVEL * adrenaline * 0.5;
					int duration = Adrenaline.SPAWNER_DURATION;
					plugin.mEffectManager.addEffect(p, Adrenaline.PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(duration, speed, Adrenaline.PERCENT_SPEED_EFFECT_NAME));
				}
			}

			//Visual feedback
			if (ice > 0) {
				player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.6f, 1.3f);
				new PartialParticle(Particle.SNOW_SHOVEL, event.getBlock().getLocation(), 25, 1.5, 1.5, 1.5).spawnAsPlayerActive(player);
			}
			if (thunder > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.6f, 0.8f);
				new PartialParticle(Particle.REDSTONE, event.getBlock().getLocation(), 12, 1.5, 1.5, 1.5, YELLOW_1_COLOR).spawnAsPlayerActive(player);
				new PartialParticle(Particle.REDSTONE, event.getBlock().getLocation(), 12, 1.5, 1.5, 1.5, YELLOW_2_COLOR).spawnAsPlayerActive(player);
			}
			if (decay > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.4f, 0.7f);
				new PartialParticle(Particle.SQUID_INK, event.getBlock().getLocation(), 25, 1.5, 1.5, 1.5).spawnAsPlayerActive(player);
			}
			if (bleed > 0 || adrenaline > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_SLIME_SQUISH, SoundCategory.PLAYERS, 0.7f, 0.7f);
				new PartialParticle(Particle.REDSTONE, event.getBlock().getLocation(), 25, 1.5, 1.5, 1.5, BLEED_COLOR).spawnAsPlayerActive(player);
			}
			if (wind > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.0f, 0.30f);
				new PartialParticle(Particle.CLOUD, event.getBlock().getLocation(), 25, 1.5, 1.5, 1.5).spawnAsPlayerActive(player);
			}
			if (fire > 0 || fire + ice + thunder + decay + bleed + adrenaline + wind == 0) {
				player.playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, SoundCategory.PLAYERS, 0.6f, 0.9f);
				new PartialParticle(Particle.LAVA, event.getBlock().getLocation(), 25, 1.5, 1.5, 1.5).spawnAsPlayerActive(player);
			}
		}
	}
}
