package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.GearDamageIncrease;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.EnumSet;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

public class Execution implements Infusion {

	private static final int DURATION = 4 * 20;
	public static final double PERCENT_DAMAGE_PER_LEVEL = 0.015;
	private static final String PERCENT_DAMAGE_EFFECT_NAME = "ExecutionPercentDamageEffect";
	private static final EnumSet<DamageType> AFFECTED_DAMAGE_TYPES = EnumSet.of(
			DamageType.MELEE,
			DamageType.MELEE_ENCH,
			DamageType.MELEE_SKILL,
			DamageType.PROJECTILE,
			DamageType.PROJECTILE_SKILL,
			DamageType.MAGIC
	);

	@Override
	public String getName() {
		return "Execution";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.EXECUTION;
	}

	@Override
	public void onKill(Plugin plugin, Player player, double value, EntityDeathEvent event, LivingEntity enemy) {
		double percentDamage = getDamageDealtMultiplier(value) - 1;

		BlockData fallingDustData = Material.ANVIL.createBlockData();
		new PartialParticle(Particle.FALLING_DUST, enemy.getLocation().add(0, enemy.getHeight() / 2, 0), 3,
			(enemy.getWidth() / 2) + 0.1, enemy.getHeight() / 3, (enemy.getWidth() / 2) + 0.1, fallingDustData).spawnAsPlayerActive(player);
		plugin.mEffectManager.addEffect(player, PERCENT_DAMAGE_EFFECT_NAME, new GearDamageIncrease(DURATION, percentDamage, AFFECTED_DAMAGE_TYPES));
	}

	public static double getDamageDealtMultiplier(double level) {
		return 1 + PERCENT_DAMAGE_PER_LEVEL * level;
	}

}
