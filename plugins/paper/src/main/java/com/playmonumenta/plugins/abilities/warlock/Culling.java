package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.Warlock;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;


public class Culling extends Ability {

	private static final int PASSIVE_DURATION = 6 * 20;
	private static final String WARLOCK_PASSIVE_EFFECT_NAME = "CullingPercentDamageResistEffect";
	private static final double WARLOCK_PASSIVE_DAMAGE_REDUCTION_PERCENT = -0.1;
	public static final String CHARM_RESISTANCE = "Culling Resistance Amplifier";
	public static final String CHARM_DURATION = "Culling Duration";

	public static final AbilityInfo<Culling> INFO =
		new AbilityInfo<>(Culling.class, null, Culling::new)
			.canUse(player -> AbilityUtils.getClassNum(player) == Warlock.CLASS_ID);

	public Culling(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (EntityUtils.isHostileMob(event.getEntity())
			    && ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())) {
			double resistance = WARLOCK_PASSIVE_DAMAGE_REDUCTION_PERCENT - CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RESISTANCE);
			int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, PASSIVE_DURATION);
			mPlugin.mEffectManager.addEffect(mPlayer, WARLOCK_PASSIVE_EFFECT_NAME, new PercentDamageReceived(duration, resistance));
		}
	}
}
