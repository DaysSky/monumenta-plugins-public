package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsCombosAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DarkCombos extends DepthsCombosAbility {

	public static final String ABILITY_NAME = "Dark Combos";
	public static final double[] VULN_AMPLIFIER = {0.14, 0.20, 0.26, 0.32, 0.38, 0.5};
	public static final int DURATION = 20 * 3;
	public static final int HIT_REQUIREMENT = 3;

	public static final DepthsAbilityInfo<DarkCombos> INFO =
		new DepthsAbilityInfo<>(DarkCombos.class, ABILITY_NAME, DarkCombos::new, DepthsTree.SHADOWDANCER, DepthsTrigger.COMBO)
			.displayItem(Material.FLINT)
			.descriptions(DarkCombos::getDescription)
			.singleCharm(false);

	private final int mDuration;
	private final double mVuln;

	public DarkCombos(Plugin plugin, Player player) {
		super(plugin, player, INFO, HIT_REQUIREMENT, CharmEffects.DARK_COMBOS_HIT_REQUIREMENT.mEffectName);
		mDuration = CharmManager.getDuration(mPlayer, CharmEffects.DARK_COMBOS_DURATION.mEffectName, DURATION);
		mVuln = VULN_AMPLIFIER[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.DARK_COMBOS_VULNERABILITY_AMPLIFIER.mEffectName);
	}




	@Override
	public void activate(DamageEvent event, LivingEntity enemy) {
		activate(enemy, mPlayer, mPlugin, mDuration, mVuln);
	}

	public static void activate(LivingEntity enemy, Player player) {
		activate(enemy, player, Plugin.getInstance(), DURATION, VULN_AMPLIFIER[0]);
	}

	public static void activate(LivingEntity enemy, Player player, Plugin plugin, int duration, double vuln) {
		EntityUtils.applyVulnerability(plugin, duration, vuln, enemy);
		playSounds(player.getWorld(), player.getLocation());
		new PartialParticle(Particle.SPELL_WITCH, enemy.getLocation(), 15, 0.5, 0.2, 0.5, 0.65).spawnAsPlayerActive(player);
		PotionUtils.applyPotion(player, enemy, new PotionEffect(PotionEffectType.GLOWING, duration, 0, true, false));
	}

	public static void playSounds(World world, Location loc) {
		world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 2.0f, 1.2f);
		world.playSound(loc, Sound.ENTITY_PLAYER_BREATH, SoundCategory.PLAYERS, 1.0f, 1.6f);
		world.playSound(loc, Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 1.4f, 0.1f);
		world.playSound(loc, Sound.ENTITY_PLAYER_DEATH, SoundCategory.PLAYERS, 1.0f, 1.0f);
		world.playSound(loc, Sound.ITEM_CROSSBOW_SHOOT, SoundCategory.PLAYERS, 0.5f, 0.1f);
		world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.3f, 0.6f);
	}

	private static Description<DarkCombos> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<DarkCombos>(color)
			.add("Every ")
			.add(a -> a.mHitRequirement, HIT_REQUIREMENT, true)
			.add(" melee attacks, apply ")
			.addPercent(a -> a.mVuln, VULN_AMPLIFIER[rarity - 1], false, true)
			.add(" vulnerability for ")
			.addDuration(a -> a.mDuration, DURATION)
			.add(" seconds.");
	}


}

