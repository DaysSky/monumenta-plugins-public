package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Scout;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Versatile extends Ability {

	public static final float DAMAGE_MULTIPLY_MELEE = 0.50f;
	public static final float DAMAGE_MULTIPLY_PROJ = 0.40f;

	public static final AbilityInfo<Versatile> INFO =
		new AbilityInfo<>(Versatile.class, null, Versatile::new)
			.canUse(player -> AbilityUtils.getClassNum(player) == Scout.CLASS_ID);

	public Versatile(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		// Hunting Companion uses both melee and projectile scaling already
		if (event.getAbility() == ClassAbility.HUNTING_COMPANION) {
			return false;
		}

		if (event.getType() == DamageEvent.DamageType.MELEE || event.getType() == DamageEvent.DamageType.MELEE_SKILL || event.getType() == DamageEvent.DamageType.MELEE_ENCH) {
			double percentproj = mPlugin.mItemStatManager.getAttributeAmount(mPlayer, AttributeType.PROJECTILE_DAMAGE_MULTIPLY);
			if (percentproj > 1) {
				event.setDamage(event.getDamage() * (1 + (percentproj - 1) * DAMAGE_MULTIPLY_MELEE));
			}
		} else if (event.getType() == DamageEvent.DamageType.PROJECTILE || event.getType() == DamageEvent.DamageType.PROJECTILE_SKILL) {
			double percentatk = mPlugin.mItemStatManager.getAttributeAmount(mPlayer, AttributeType.ATTACK_DAMAGE_MULTIPLY);
			if (percentatk > 1) {
				event.setDamage(event.getDamage() * (1 + (percentatk - 1) * DAMAGE_MULTIPLY_PROJ));
			}
		}
		return false; // no recursion possible as we only change the damage amount
	}

}
