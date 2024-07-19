package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.FirstStrikeCooldown;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.EnumSet;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class FirstStrike implements Enchantment {

	private static final double DAMAGE_PER_LEVEL = 0.1;
	private static final double PROJ_REDUCTION = 0.75;
	private static final int DURATION = 3 * 20;
	private static final String SOURCE = "FirstStrikeDisable";
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(244, 141, 123), 0.75f);
	public static final String CHARM_DAMAGE = "First Strike Damage";

	@Override
	public String getName() {
		return "First Strike";
	}

	@Override
	public double getPriorityAmount() {
		return 999;
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.FIRST_STRIKE;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET, Slot.PROJECTILE);
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		DamageType type = event.getType();
		if ((type == DamageType.MELEE
			&& ItemStatUtils.isNotExclusivelyRanged(player.getInventory().getItemInMainHand()))
			|| type == DamageType.PROJECTILE) {
			if (plugin.mEffectManager.getEffects(enemy, SOURCE + player.getName()) == null) {
				double bonus = DAMAGE_PER_LEVEL * level;
				if (type == DamageType.PROJECTILE) {
					bonus *= PROJ_REDUCTION;
				}

				event.updateGearDamageWithMultiplier(1 + CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, bonus));

				double widthDelta = PartialParticle.getWidthDelta(enemy);
				double doubleWidthDelta = widthDelta * 2;
				double heightDelta = PartialParticle.getHeightDelta(enemy);

				new PartialParticle(
					Particle.CRIT,
					LocationUtils.getHeightLocation(enemy, 0.8),
					8,
					doubleWidthDelta,
					heightDelta / 2,
					doubleWidthDelta
				).spawnAsEnemy();

				new PartialParticle(
					Particle.REDSTONE,
					LocationUtils.getHeightLocation(enemy, 0.8),
					6,
					doubleWidthDelta,
					heightDelta / 2,
					doubleWidthDelta,
					1,
					COLOR
				).spawnAsEnemy();

				World world = enemy.getWorld();
				Location enemyLocation = enemy.getLocation();

				world.playSound(
					enemyLocation,
					Sound.ENTITY_ARROW_HIT_PLAYER,
					SoundCategory.PLAYERS,
					0.8f,
					0.45f
				);
				plugin.mEffectManager.addEffect(enemy, SOURCE + player.getName(), new FirstStrikeCooldown(DURATION));
			}
		}
	}
}
