package com.playmonumenta.plugins.bosses.bosses.lich;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import com.playmonumenta.plugins.bosses.spells.SpellConditionalTeleport;
import com.playmonumenta.plugins.bosses.spells.headlesshorseman.SpellBurningVengence;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public class LichDemiseBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_demise";
	public static final int detectionRange = Lich.detectionRange;

	Location mCenter;

	public LichDemiseBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mCenter = Lich.getLichSpawn();

		Location loc = mBoss.getLocation().clone().add(-23, 0, 0);
		int playercount = Lich.playersInRange(loc, detectionRange, true).size();
		double hpdel = 325;
		double hp = (int) (hpdel * (1 + (1 - 1 / Math.E) * Math.log(playercount)));
		EntityUtils.setMaxHealthAndHealth(mBoss, hp);

		SpellManager activeSpells = new SpellManager(List.of(
			new SpellBurningVengence(plugin, boss, 20 * 25, mCenter, detectionRange, 0.3)
		));

		List<Spell> passives = Arrays.asList(
			new SpellBlockBreak(mBoss, 1, 3, 1, (int) mCenter.getY(), false, false, true),
			// Teleport the boss to spawnLoc if he gets too far away from where he spawned
			new SpellConditionalTeleport(mBoss.getVehicle(), mCenter, b -> loc.distance(b.getLocation()) > 80),
			// Teleport the boss to spawnLoc if he is stuck in bedrock
			new SpellConditionalTeleport(mBoss.getVehicle(), mCenter, b -> b.getLocation().getBlock().getType() == Material.BEDROCK ||
				                                                               b.getLocation().add(0, 1, 0).getBlock().getType() == Material.BEDROCK ||
				                                                               b.getLocation().getBlock().getType() == Material.LAVA)
		);

		super.constructBoss(activeSpells, passives, detectionRange, null);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		LivingEntity horse = (LivingEntity) mBoss.getVehicle();
		if (horse != null) {
			horse.setHealth(0);
		}
	}
}
