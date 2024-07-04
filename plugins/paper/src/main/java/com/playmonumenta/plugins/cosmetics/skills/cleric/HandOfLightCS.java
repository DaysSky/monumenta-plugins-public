package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkill;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class HandOfLightCS implements CosmeticSkill {

	@Override
	public ClassAbility getAbility() {
		return ClassAbility.HAND_OF_LIGHT;
	}

	@Override
	public Material getDisplayItem() {
		return Material.PINK_DYE;
	}

	public void lightHealEffect(Player mPlayer, Location loc, Player mTarget) {
		mPlayer.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 2.0f, 1.6f);
		mPlayer.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.05f, 1.0f);
		new PartialParticle(Particle.HEART, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.END_ROD, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
	}

	public void lightHealCastEffect(World world, Location userLoc, Plugin mPlugin, Player mPlayer, float radius, double angle, List<Player> nearbyPlayers) {
		world.playSound(userLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 2.0f, 1.6f);
		world.playSound(userLoc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.05f, 1.0f);
		ParticleUtils.explodingConeEffectSkill(mPlugin, mPlayer, radius, Particle.SPIT, 0.35f, Particle.PORTAL, 3.0f, angle, mPlayer);
	}

	public void lightDamageEffect(Player mPlayer, Location loc, LivingEntity target, List<LivingEntity> undeadMobs) {
		mPlayer.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.5f, 0.8f);
		new PartialParticle(Particle.DAMAGE_INDICATOR, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.END_ROD, loc.add(0, 1, 0), 10, 0.7, 0.7, 0.7, 0.001).spawnAsPlayerActive(mPlayer);
	}

	public void lightDamageCastEffect(World world, Location userLoc, Plugin mPlugin, Player mPlayer, float radius, double angle, List<LivingEntity> nearbyMobs) {
		world.playSound(userLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.5f, 0.8f);
		ParticleUtils.explodingConeEffectSkill(mPlugin, mPlayer, radius, Particle.SPIT, 0.35f, Particle.PORTAL, 3.0f, angle, mPlayer);
	}
}
