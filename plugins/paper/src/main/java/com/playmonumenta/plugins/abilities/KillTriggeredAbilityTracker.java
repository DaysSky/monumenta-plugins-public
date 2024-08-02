package com.playmonumenta.plugins.abilities;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class KillTriggeredAbilityTracker {

	public interface KillTriggeredAbility {
		void triggerOnKill(LivingEntity mob);
	}

	private final KillTriggeredAbility mLinkedAbility;
	private final int mDamageDealtToBossesPerKill;

	private double mDamageDealtToBosses = 0;

	public KillTriggeredAbilityTracker(KillTriggeredAbility linkedAbility, int damage) {
		mLinkedAbility = linkedAbility;
		mDamageDealtToBossesPerKill = damage;
	}

	public KillTriggeredAbilityTracker(Player player, KillTriggeredAbility linkedAbility, int r1damage, int r2damage, int r3damage) {
		this(linkedAbility, ServerProperties.getAbilityEnhancementsEnabled(player) ? r3damage : (ServerProperties.getClassSpecializationsEnabled(player) ? r2damage : r1damage));
	}

	public void updateDamageDealtToBosses(DamageEvent event) {
		// Prevent Cloak and Dagger's own damage from counting towards its boss damage thresholds
		if (event.getAbility() == ClassAbility.CLOAK_AND_DAGGER) {
			return;
		}

		updateDamageDealtToBosses(event.getFinalDamage(false), event.getDamagee());
	}

	public void updateDamageDealtToBosses(double damage, LivingEntity mob) {
		if (EntityUtils.isBoss(mob)) {
			mDamageDealtToBosses += damage;
			while (mDamageDealtToBosses >= mDamageDealtToBossesPerKill) {
				mDamageDealtToBosses -= mDamageDealtToBossesPerKill;
				mLinkedAbility.triggerOnKill(mob);
			}
		}
	}

}
