package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.NavigableSet;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Expedite implements Infusion {

	private static final int DURATION = 5 * 20;
	public static final double PERCENT_SPEED_PER_LEVEL = 0.0075;
	private static final String PERCENT_SPEED_EFFECT_NAME = "ExpeditePercentSpeedEffect";
	private static final String CHECK_ONCE_THIS_TICK_METAKEY = "ExpediteTick";
	public static final int MAX_STACKS = 3;

	@Override
	public String getName() {
		return "Expedite";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.EXPEDITE;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getAbility() == null || event.getAbility().isFake() || event.getType() == DamageEvent.DamageType.AILMENT) {
			return;
		}

		if (MetadataUtils.checkOnceThisTick(plugin, player, CHECK_ONCE_THIS_TICK_METAKEY)) {
			double percentSpeed = PERCENT_SPEED_PER_LEVEL * value;
			NavigableSet<Effect> oldEffects = plugin.mEffectManager.getEffects(player, PERCENT_SPEED_EFFECT_NAME);
			if (oldEffects != null && !oldEffects.isEmpty()) {
				Effect oldEffect = oldEffects.last();
				int oldStacks = (int) Math.round(oldEffect.getMagnitude() / percentSpeed);
				if (oldStacks >= MAX_STACKS) {
					oldEffect.setDuration(DURATION);
				} else {
					oldEffect.clearEffect();
					plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, percentSpeed * (oldStacks + 1), PERCENT_SPEED_EFFECT_NAME));
					AbilityUtils.playPassiveAbilitySound(player, player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.1f, 1.0f);
				}
			} else {
				plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, percentSpeed, PERCENT_SPEED_EFFECT_NAME));
				AbilityUtils.playPassiveAbilitySound(player, player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.1f, 1.0f);
			}
		}
	}

	public static double getMovementSpeedBonus(double level, int stacks) {
		return PERCENT_SPEED_PER_LEVEL * level * stacks;
	}

}
