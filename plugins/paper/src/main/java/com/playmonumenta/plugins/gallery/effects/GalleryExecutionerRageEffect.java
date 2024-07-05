package com.playmonumenta.plugins.gallery.effects;

import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.gallery.GalleryPlayer;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class GalleryExecutionerRageEffect extends GalleryConsumableEffect {
	/**
	 * - Your melee attacks deal 30% more damage to targets not at full health,
	 *  killing an enemy with a melee attack grants you 20% Resistance for 3s  lasting 3 waves
	 */

	private static final double MELEE_DAMAGE_INCREASE = 1.3;

	public GalleryExecutionerRageEffect() {
		super(GalleryEffectType.EXECUTIONER_RAGE);
	}

	@Override
	public void onPlayerDamage(GalleryPlayer galleryPlayer, DamageEvent event, LivingEntity entity) {
		if (event.getType() == DamageEvent.DamageType.MELEE || event.getType() == DamageEvent.DamageType.MELEE_ENCH || event.getType() == DamageEvent.DamageType.MELEE_SKILL) {
			Player player = galleryPlayer.getPlayer();
			if (player == null) {
				return;
			}
			double maxHealth = EntityUtils.getMaxHealth(entity);
			if (entity.getHealth() < maxHealth) {
				event.updateDamageWithMultiplier(MELEE_DAMAGE_INCREASE);
			}

			if (entity.getHealth() + entity.getAbsorptionAmount() <= event.getFinalDamage(true)) {
				EffectManager.getInstance().addEffect(player, "EXECUTIONER_RAGE", new PercentDamageReceived(20 * 3, -0.2));
			}

		}
	}
}
