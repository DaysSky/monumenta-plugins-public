package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SoulStrainCS extends DivineJusticeCS {

	public static final String NAME = "Soul Strain";
	private static final Color TWIST_COLOR_BASE = Color.fromRGB(0, 180, 180);
	private static final Color TWIST_COLOR_TIP = Color.fromRGB(0, 120, 120);

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The deadliest strike is the one that",
			"pierces the soul itself. For when the mind",
			"is crippled, the flesh falls with it."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.ENDER_PEARL;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private static final Particle.DustOptions CYAN = new Particle.DustOptions(Color.fromRGB(0, 235, 235), 0.75f);
	private static final double[] ANGLE = {200, -22.5, -95};

	@Override
	public Material justiceAsh() {
		return Material.PRISMARINE_CRYSTALS;
	}

	@Override
	public void justiceAshColor(Item item) {
		ScoreboardUtils.addEntityToTeam(item, "GlowingAqua", NamedTextColor.AQUA);
	}

	@Override
	public String justiceAshName() {
		return "Psionic Trace";
	}

	@Override
	public void justiceAshPickUp(Player player, Location loc) {
		player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1f, 1.3f);
		player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.8f, 2f);

		Location particleLocation = loc.add(0, 0.2, 0);
		spawnTendril(particleLocation, player);
	}

	@Override
	public void justiceOnDamage(Player player, LivingEntity enemy, World world, Location enemyLoc, double widerWidthDelta, int combo) {
		Vector dir = player.getEyeLocation().getDirection();
		world.playSound(enemyLoc, Sound.ITEM_AXE_WAX_OFF, SoundCategory.PLAYERS, 1.7f, 1.1f);
		world.playSound(enemyLoc, Sound.ENTITY_ALLAY_HURT, SoundCategory.PLAYERS, 0.6f, 0.6f);
		world.playSound(enemyLoc, Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 1.6f, 0.8f);
		world.playSound(enemyLoc, Sound.BLOCK_SHROOMLIGHT_FALL, SoundCategory.PLAYERS, 1.7f, 0.7f);
		world.playSound(enemyLoc, Sound.BLOCK_SCULK_BREAK, SoundCategory.PLAYERS, 2.0f, 1.0f);
		world.playSound(enemyLoc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.1f, 0.8f);
		new PartialParticle(Particle.SCULK_CHARGE_POP, LocationUtils.getHalfHeightLocation(enemy), 20, 0.1, 0.2 * enemy.getHeight(), 0.1, 0.05).spawnAsPlayerActive(player);
		new PartialParticle(Particle.GLOW, LocationUtils.getHalfHeightLocation(enemy), 12, 0.1, 0.2 * enemy.getHeight(), 0.1, 0.05).spawnAsPlayerActive(player);
		enemyLoc.setDirection(dir);
		ParticleUtils.drawHalfArc(enemyLoc.clone().add(0, 1, 0).subtract(dir.clone().multiply(2.25)), 2, ANGLE[combo], 0, 160, 5, 0.15,
			(Location l, int ring) ->
				new PartialParticle(Particle.REDSTONE, l, 2, 0.05, 0.05, 0.05, 0).data(CYAN).spawnAsPlayerActive(player));
		if (combo == 2) {
			world.playSound(enemyLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.7f, 1.2f);
			world.playSound(enemyLoc, Sound.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 0.75f, 1.1f);
			world.playSound(enemyLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 0.8f, 1.3f);
			// Hieroglyph for "Die"
			double width = Math.min(enemy.getWidth() * 1.15, 1.25);
			Vector front = dir.clone().setY(0).normalize().multiply(-width);
			Vector left = VectorUtils.rotateTargetDirection(front, 90, 0);
			Vector right = VectorUtils.rotateTargetDirection(front, -90, 0);
			Location loc = enemyLoc.clone().add(front);
			Location loc1 = loc.clone().add(left);
			Location loc2 = loc.clone().add(right);
			double hieroglyphRadius = enemyLoc.distance(loc.clone().add(left));
			for (int i = 0; i < 2; i++) {
				double delta = 0.1 * i;
				final Particle.DustOptions RED = new Particle.DustOptions(Color.fromRGB(180 - 60 * i, 0, 60 - 20 * i), 1.0f - i * 0.2f);
				// Axe
				new PPLine(Particle.REDSTONE, loc1, loc1.clone().subtract(front.clone().multiply(2))).data(RED).countPerMeter(12).delta(delta, 0, delta).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc1.clone().subtract(front), loc.clone().subtract(front.clone().multiply(1.5))).data(RED).countPerMeter(12).delta(delta, 0, delta).spawnAsPlayerActive(player);
				new PPLine(Particle.REDSTONE, loc1.clone().subtract(front.clone().multiply(2)), loc.clone().subtract(front.clone().multiply(1.5))).data(RED).countPerMeter(12).delta(delta, 0, delta).spawnAsPlayerActive(player);
				// Head
				new PPLine(Particle.REDSTONE, loc2, loc2.clone().subtract(front.clone().multiply(2))).data(RED).countPerMeter(12).delta(delta, 0, delta).spawnAsPlayerActive(player);
				new PPCircle(Particle.REDSTONE, loc.clone().add(right.clone().multiply(0.5)).subtract(front.clone().multiply(1.5)), width / 2).data(RED).countPerMeter(12).delta(delta, 0, delta).spawnAsPlayerActive(player);
			}
			new PPCircle(Particle.ENCHANTMENT_TABLE, enemyLoc, hieroglyphRadius).countPerMeter(12).extraRange(0.1, 0.15).innerRadiusFactor(1)
				.directionalMode(true).delta(-2, 0.2, 8).rotateDelta(true).spawnAsPlayerActive(player);
		}
	}

	@Override
	public void justiceKill(Player player, Location loc) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_WARDEN_LISTENING_ANGRY, SoundCategory.PLAYERS, 1.2f, 1.2f);
		new PartialParticle(Particle.SOUL, loc, 20, 0.5, 0.8, 0.5, 0.1).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SHRIEK, loc.clone().add(0, 1.5, 0), 3, 0.0, 0.0, 0.0, 0.0).data(0).spawnAsPlayerActive(player);
	}

	@Override
	public void justiceHealSound(List<Player> players, float pitch) {
		for (Player healedPlayer : players) {
			healedPlayer.playSound(healedPlayer.getLocation(), Sound.ENTITY_SKELETON_HORSE_AMBIENT, SoundCategory.PLAYERS, 0.85f, 1.75f);
		}
	}

	private void spawnTendril(Location loc, Player mPlayer) {
		Location to = loc.clone().add(0, 8, 0);

		new BukkitRunnable() {
			final Location mL = loc.clone();
			int mT = 0;

			final int DURATION = FastUtils.RANDOM.nextInt(7, 11);
			final int ITERATIONS = 3;

			final double mXMult = FastUtils.randomDoubleInRange(-1, 1);
			final double mZMult = FastUtils.randomDoubleInRange(-1, 1);
			double mJ = 0;

			@Override
			public void run() {
				mT++;

				for (int i = 0; i < ITERATIONS; i++) {
					mJ++;
					float size = 0.1f + (1.7f * (1f - (float) (mJ / (ITERATIONS * DURATION))));
					double offset = 0.1 * (1f - (mJ / (ITERATIONS * DURATION)));
					double transition = mJ / (ITERATIONS * DURATION);
					double pi = (Math.PI * 2) * (1f - (mJ / (ITERATIONS * DURATION)));


					Vector vec = new Vector(mXMult * FastUtils.cos(pi), 0,
						mZMult * FastUtils.sin(pi));
					Location tendrilLoc = mL.clone().add(vec);
					new PartialParticle(Particle.CRIT_MAGIC, tendrilLoc, 3, 0, 0, 0, 0.2F)
						.spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, tendrilLoc, 3, offset, offset, offset, 0, new Particle.DustOptions(
						ParticleUtils.getTransition(TWIST_COLOR_TIP, TWIST_COLOR_BASE, transition), size))

						.spawnAsPlayerActive(mPlayer);

					mL.add(0, 0.25, 0);
					if (mL.distance(to) < 0.4) {
						this.cancel();
						return;
					}
				}

				if (mT >= DURATION) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
