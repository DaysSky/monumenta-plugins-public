package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.InfernoDamage;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.ItemStatManager.PlayerItemStats;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import java.util.EnumSet;
import java.util.NavigableSet;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Inferno implements Enchantment {

	public static final String INFERNO_EFFECT_NAME = "Inferno";

	@Override
	public String getName() {
		return "Inferno";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.INFERNO;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET, Slot.PROJECTILE);
	}

	//Called in EntityUtils.applyFire()
	public static void apply(Plugin plugin, Player player, @Nullable PlayerItemStats playerItemStats, int level, LivingEntity enemy, int duration) {
		plugin.mEffectManager.addEffect(enemy, INFERNO_EFFECT_NAME, new InfernoDamage(duration, level, player, playerItemStats));
	}

	public static boolean hasInferno(Plugin plugin, LivingEntity mob) {
		return plugin.mEffectManager.hasEffect(mob, INFERNO_EFFECT_NAME);
	}

	public static int getInfernoLevel(Plugin plugin, LivingEntity mob) {
		NavigableSet<Effect> effects = plugin.mEffectManager.getEffects(mob, INFERNO_EFFECT_NAME);
		if (effects != null) {
			Effect effect = effects.last();
			return (int) effect.getMagnitude();
		} else {
			return 0;
		}
	}
}
