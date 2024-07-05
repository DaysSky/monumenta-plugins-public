package com.playmonumenta.plugins.abilities.scout;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.scout.hunter.PredatorStrike;
import com.playmonumenta.plugins.abilities.scout.ranger.Quickdraw;
import com.playmonumenta.plugins.abilities.scout.ranger.TacticalManeuver;
import com.playmonumenta.plugins.abilities.scout.ranger.WhirlingBlade;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.AgilityCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Agility extends Ability {

	private static final int AGILITY_1_EFFECT_LVL = 0;
	private static final int AGILITY_2_EFFECT_LVL = 1;
	private static final int AGILITY_BONUS_DAMAGE = 1;
	private static final double SCALING_DAMAGE = 0.1;
	private static final double ENHANCEMENT_COOLDOWN_REFRESH = 0.05;

	public static final String CHARM_HASTE = "Agility Haste Amplifier";

	private final int mHasteAmplifier;

	public static final AbilityInfo<Agility> INFO =
		new AbilityInfo<>(Agility.class, "Agility", Agility::new)
			.scoreboardId("Agility")
			.shorthandName("Agl")
			.descriptions(
				String.format("You gain permanent Haste %s. Your melee damage is increased by +%d.", StringUtils.toRoman(AGILITY_1_EFFECT_LVL + 1), AGILITY_BONUS_DAMAGE),
				String.format("You gain permanent Haste %s. Increase melee damage by +%d plus %d%% of final damage done.", StringUtils.toRoman(AGILITY_2_EFFECT_LVL + 1), AGILITY_BONUS_DAMAGE, (int) (SCALING_DAMAGE * 100)),
				String.format("Breaking a spawner refreshes the cooldown of all your skills by %s%%.",
					StringUtils.multiplierToPercentage(ENHANCEMENT_COOLDOWN_REFRESH)))
			.simpleDescription("Gain haste and increased melee damage.")
			.displayItem(Material.GOLDEN_PICKAXE);

	private Ability[] mScoutAbilities = {};

	private final AgilityCS mCosmetic;

	public Agility(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mHasteAmplifier = (isLevelOne() ? AGILITY_1_EFFECT_LVL : AGILITY_2_EFFECT_LVL) + (int) CharmManager.getLevel(mPlayer, CHARM_HASTE);
		Bukkit.getScheduler().runTask(plugin, () -> {
			AbilityManager abilityManager = mPlugin.mAbilityManager;
			mScoutAbilities = Stream.of(WindBomb.class, Volley.class, HuntingCompanion.class, EagleEye.class,
					WhirlingBlade.class, TacticalManeuver.class, Quickdraw.class, PredatorStrike.class)
				                  .map(c -> abilityManager.getPlayerAbilityIgnoringSilence(player, c))
				                  .filter(Objects::nonNull)
				                  .toArray(Ability[]::new);
		});
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new AgilityCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_ENCH || event.getType() == DamageType.MELEE_SKILL) {
			if (isLevelTwo()) {
				event.setDamage(event.getFlatDamage() + AGILITY_BONUS_DAMAGE);
				event.updateDamageWithMultiplier(1 + SCALING_DAMAGE);
				mCosmetic.agilityEffectLevelTwo(mPlayer, enemy);
			} else {
				event.setDamage(event.getFlatDamage() + AGILITY_BONUS_DAMAGE);
				mCosmetic.agilityEffectLevelOne(mPlayer, enemy);
			}
		}
		return false; // only changes event damage
	}

	@Override
	public boolean blockBreakEvent(BlockBreakEvent event) {
		if (isEnhanced() && event.getBlock().getType() == Material.SPAWNER) {
			UUID uuid = mPlayer.getUniqueId();
			for (Ability ability : mScoutAbilities) {
				ClassAbility linkedSpell = ability.getInfo().getLinkedSpell();
				if (linkedSpell != null && mPlugin.mTimers.isAbilityOnCooldown(uuid, linkedSpell)) {
					int cooldownRefresh = (int) (ability.getModifiedCooldown() * ENHANCEMENT_COOLDOWN_REFRESH);
					mPlugin.mTimers.updateCooldown(mPlayer, linkedSpell, cooldownRefresh);
					mCosmetic.agilityEnhancementEffect(mPlayer);
				}
			}
		}
		return true;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (oneSecond) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
				new PotionEffect(PotionEffectType.FAST_DIGGING, 21, mHasteAmplifier, true, false));
		}
	}
}
