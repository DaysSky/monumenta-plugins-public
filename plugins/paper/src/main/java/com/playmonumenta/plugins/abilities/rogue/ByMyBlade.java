package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.rogue.ByMyBladeCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;


public class ByMyBlade extends Ability {

	private static final int BY_MY_BLADE_1_HASTE_AMPLIFIER = 1;
	private static final int BY_MY_BLADE_2_HASTE_AMPLIFIER = 3;
	private static final int BY_MY_BLADE_HASTE_DURATION = 4 * 20;
	private static final int BY_MY_BLADE_1_DAMAGE = 12;
	private static final int BY_MY_BLADE_2_DAMAGE = 24;
	private static final int BY_MY_BLADE_COOLDOWN = 10 * 20;
	private static final double ENHANCEMENT_HEAL_PERCENT = 0.05;
	private static final double ENHANCEMENT_HEAL_PERCENT_ELITE = 0.15;
	private static final double ENHANCEMENT_DAMAGE_MULT = 0.2;

	public static final String CHARM_DAMAGE = "By My Blade Damage";
	public static final String CHARM_COOLDOWN = "By My Blade Cooldown";
	public static final String CHARM_HASTE_AMPLIFIER = "By My Blade Haste Amplifier";
	public static final String CHARM_HASTE_DURATION = "By My Blade Haste Duration";
	public static final String CHARM_HEALTH = "By My Blade Enhancement Health";
	public static final String CHARM_ELITE_HEALTH = "By My Blade Enhancement Elite Health";

	public static final AbilityInfo<ByMyBlade> INFO =
		new AbilityInfo<>(ByMyBlade.class, "By My Blade", ByMyBlade::new)
			.linkedSpell(ClassAbility.BY_MY_BLADE)
			.scoreboardId("ByMyBlade")
			.shorthandName("BmB")
			.descriptions(
				String.format("While holding two swords, attacking an enemy with a critical attack deals an extra %s melee damage to that enemy, and grants you Haste %s for %ss. Cooldown: %ss.",
					BY_MY_BLADE_1_DAMAGE,
					StringUtils.toRoman(BY_MY_BLADE_1_HASTE_AMPLIFIER + 1),
					BY_MY_BLADE_HASTE_DURATION / 20,
					BY_MY_BLADE_COOLDOWN / 20),
				String.format("Damage is increased from %s to %s. Haste level is increased from %s to %s.",
					BY_MY_BLADE_1_DAMAGE,
					BY_MY_BLADE_2_DAMAGE,
					StringUtils.toRoman(BY_MY_BLADE_1_HASTE_AMPLIFIER + 1),
					StringUtils.toRoman(BY_MY_BLADE_2_HASTE_AMPLIFIER + 1)),
				String.format("By My Blade does %s%% extra damage. Killing an enemy with this ability heals you for %s%% of your max health, increased to %s%% if the target was an elite or boss.",
					(int) (ENHANCEMENT_DAMAGE_MULT * 100),
					(int) (ENHANCEMENT_HEAL_PERCENT * 100),
					(int) (ENHANCEMENT_HEAL_PERCENT_ELITE * 100)))
			.simpleDescription("Critical hits periodically do more damage and give haste.")
			.cooldown(BY_MY_BLADE_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.SKELETON_SKULL);

	private final double mDamageBonus;
	private final int mHasteAmplifier;
	private final ByMyBladeCS mCosmetic;

	public ByMyBlade(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageBonus = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, (isLevelOne() ? BY_MY_BLADE_1_DAMAGE : BY_MY_BLADE_2_DAMAGE) * (isEnhanced() ? 1 + ENHANCEMENT_DAMAGE_MULT : 1));
		mHasteAmplifier = (isLevelOne() ? BY_MY_BLADE_1_HASTE_AMPLIFIER : BY_MY_BLADE_2_HASTE_AMPLIFIER) + (int) CharmManager.getLevel(mPlayer, CHARM_HASTE_AMPLIFIER);

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ByMyBladeCS());
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE
			    && !isOnCooldown()
			    && PlayerUtils.isFallingAttack(mPlayer)
			    && InventoryUtils.rogueTriggerCheck(mPlugin, mPlayer)) {

			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
				new PotionEffect(PotionEffectType.FAST_DIGGING, CharmManager.getDuration(mPlayer, CHARM_HASTE_DURATION, BY_MY_BLADE_HASTE_DURATION), mHasteAmplifier, false, true));

			DamageUtils.damage(mPlayer, enemy, DamageType.MELEE_SKILL, mDamageBonus, mInfo.getLinkedSpell(), true);

			Location loc = enemy.getLocation();
			World world = mPlayer.getWorld();
			loc.add(0, 1, 0);
			int level = 1;
			if (isLevelTwo()) {
				mCosmetic.bmbDamageLv2(mPlayer, enemy);
				level = 2;
			}
			if (isEnhanced()) {
				// This might be a bit scuffed... but hopefully it feels better this way.
				// As BMB applies first before melee hit, if the enemy survives BMB but dies to melee
				// It doesn't heal the player. So we delay this check by 1 tick.
				cancelOnDeath(new BukkitRunnable() {
					@Override
					public void run() {
						if (enemy.isDead() || !enemy.isValid()) {
							// Heal Player - 5% normal, 15% elite or boss
							if (EntityUtils.isElite(enemy) || EntityUtils.isBoss(enemy)) {
								PlayerUtils.healPlayer(mPlugin, mPlayer, EntityUtils.getMaxHealth(mPlayer) * (CharmManager.getLevelPercentDecimal(mPlayer, CHARM_ELITE_HEALTH) + ENHANCEMENT_HEAL_PERCENT_ELITE));
							} else {
								PlayerUtils.healPlayer(mPlugin, mPlayer, EntityUtils.getMaxHealth(mPlayer) * (CharmManager.getLevelPercentDecimal(mPlayer, CHARM_HEALTH) + ENHANCEMENT_HEAL_PERCENT));
							}
							mCosmetic.bmbHeal(mPlayer, loc);
						}
					}
				}.runTaskLater(mPlugin, 1));
			}
			mCosmetic.bmbDamage(world, mPlayer, enemy, level);
			putOnCooldown();
		}
		return false;
	}

}
