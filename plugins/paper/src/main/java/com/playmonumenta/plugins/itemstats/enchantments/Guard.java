package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.OnHitTimerEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import java.util.NavigableSet;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Guard implements Enchantment {
	private static final double ARMOR_BONUS_PER_LEVEL = 0.2;
	public static final int PAST_HIT_DURATION_TIME_MAINHAND = 6 * 20;
	public static final int PAST_HIT_DURATION_TIME_OFFHAND = 4 * 20;
	private static final String GUARD_EFFECT_NAME = "GuardEffect";

	@Override
	public String getName() {
		return "Guard";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.GUARD;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (!event.isBlockedByShield()) {
			return;
		}

		// dummy amount (only used for inure)
		plugin.mEffectManager.addEffect(player, GUARD_EFFECT_NAME, new OnHitTimerEffect(player.getInventory().getItemInMainHand().getType() == Material.SHIELD ? PAST_HIT_DURATION_TIME_MAINHAND : PAST_HIT_DURATION_TIME_OFFHAND, 1));
	}

	public static double applyGuard(DamageEvent event, Plugin plugin, Player player) {
		NavigableSet<Effect> guard = plugin.mEffectManager.getEffects(player, GUARD_EFFECT_NAME);
		if (guard != null) {
			return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.GUARD) * ARMOR_BONUS_PER_LEVEL;
		} else {
			return 0;
		}
	}

}
