package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.managers.GlowingManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scoreboard.Team;

public class TemporalInstability extends Spell {
	private static final double RADIUS = 11;

	private final LivingEntity mBoss;
	private final Location mCenter;

	private final PPCircle mCircle;

	public TemporalInstability(LivingEntity boss, Location center) {
		mBoss = boss;
		mCenter = center;

		mCircle = new PPCircle(Particle.REDSTONE, mCenter, RADIUS).data(new Particle.DustOptions(Color.AQUA, 2)).count(50);
	}

	@Override
	public void run() {
		Team team = ScoreboardUtils.getEntityTeam(mBoss);
		if (isVulnerable()) {
			GlowingManager.clear(mBoss, "TemporalInstability");
		} else if (team != null && team.color().equals(NamedTextColor.AQUA)) {
			GlowingManager.startGlowing(mBoss, NamedTextColor.WHITE, -1, GlowingManager.BOSS_SPELL_PRIORITY, null, "TemporalInstability");
		}
		mCircle.spawnAsBoss();
	}

	@Override
	public void onHurt(DamageEvent event) {
		if (!isVulnerable()) {
			// Let /kill commands work
			if (event.getType() == DamageEvent.DamageType.TRUE) {
				return;
			}

			event.setCancelled(true);

			if (event.getSource() instanceof Player player && event.getType() != DamageEvent.DamageType.AILMENT) {
				Location loc = mBoss.getLocation();
				player.playSound(loc, Sound.PARTICLE_SOUL_ESCAPE, SoundCategory.HOSTILE, 1.0f, 0.5f);
				player.playSound(loc, Sound.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 1.0f, 0.5f);
				new PartialParticle(Particle.SOUL, loc.clone().add(0, 1.5, 0), 5, 0.5, 2, 0.5, 0).spawnAsEntityActive(mBoss);
				new PartialParticle(Particle.END_ROD, loc.clone().add(0, 1.5, 0), 5, 0.5, 2, 0.5, 0).spawnAsEntityActive(mBoss);
			}
		}
	}

	private boolean isVulnerable() {
		return LocationUtils.xzDistance(mBoss.getLocation(), mCenter) <= RADIUS;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
