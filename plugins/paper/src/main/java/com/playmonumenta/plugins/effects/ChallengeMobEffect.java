package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.abilities.warrior.guardian.Challenge;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class ChallengeMobEffect extends Effect {
	private static final String effectId = "ChallengeMobEffect";

	private final Challenge mChallenge;

	public ChallengeMobEffect(int duration, Challenge challenge) {
		super(duration, effectId);
		mChallenge = challenge;
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		mChallenge.incrementKills(event.getEntity());
	}

	@Override
	public void onExplode(EntityExplodeEvent event) {
		mChallenge.incrementKills((LivingEntity) event.getEntity());
	}

	@Override
	public String toString() {
		return String.format("ChallengeMobEffect duration:%d", this.getDuration());
	}
}
