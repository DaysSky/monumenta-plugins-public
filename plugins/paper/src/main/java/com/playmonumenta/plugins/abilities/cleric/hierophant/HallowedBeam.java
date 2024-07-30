package com.playmonumenta.plugins.abilities.cleric.hierophant;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.cleric.hierophant.HallowedBeamCS;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Grappling;
import com.playmonumenta.plugins.itemstats.enchantments.PointBlank;
import com.playmonumenta.plugins.itemstats.enchantments.Recoil;
import com.playmonumenta.plugins.itemstats.enchantments.Sniper;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Predicate;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class HallowedBeam extends MultipleChargeAbility {

	private static final int HALLOWED_1_MAX_CHARGES = 2;
	private static final int HALLOWED_2_MAX_CHARGES = 3;
	private static final int HALLOWED_1_COOLDOWN = 20 * 16;
	private static final int HALLOWED_2_COOLDOWN = 20 * 12;
	private static final double HALLOWED_HEAL_PERCENT = 0.3;
	private static final double HALLOWED_DAMAGE_REDUCTION_PERCENT = -0.1;
	private static final int HALLOWED_DAMAGE_REDUCTION_DURATION = 20 * 5;
	private static final String PERCENT_DAMAGE_RESIST_EFFECT_NAME = "HallowedPercentDamageResistEffect";
	private static final int HALLOWED_RADIUS = 4;
	private static final int HALLOWED_UNDEAD_STUN = 20; // 20 * 1
	private static final int HALLOWED_LIVING_STUN = 20 * 2;
	private static final int CAST_RANGE = 30;
	private static final String MODE_SCOREBOARD = "HallowedBeamMode";

	public static final String CHARM_DAMAGE = "Hallowed Beam Damage";
	public static final String CHARM_COOLDOWN = "Hallowed Beam Cooldown";
	public static final String CHARM_HEAL = "Hallowed Beam Healing";
	public static final String CHARM_DISTANCE = "Hallowed Beam Distance";
	public static final String CHARM_STUN = "Hallowed Beam Stun Duration";
	public static final String CHARM_RESISTANCE = "Hallowed Beam Resistance";
	public static final String CHARM_RESISTANCE_DURATION = "Hallowed Beam Resistance Duration";
	public static final String CHARM_CHARGE = "Hallowed Beam Charge";

	public static final AbilityInfo<HallowedBeam> INFO =
		new AbilityInfo<>(HallowedBeam.class, "Hallowed Beam", HallowedBeam::new)
			.linkedSpell(ClassAbility.HALLOWED_BEAM)
			.scoreboardId("HallowedBeam")
			.shorthandName("HB")
			.descriptions(
				("Left-click with a projectile weapon while looking directly at a player or mob within %s blocks to shoot a beam of light. " +
					"If aimed at a player, the beam instantly heals them for %s%% of their max health, knocking back enemies within %s blocks. " +
					"If aimed at an Undead, it instantly deals magic damage equal to your projectile damage to the target, and stuns them for %ss. " +
					"If aimed at a non-undead mob, it instantly stuns them for %ss. %s charges. " +
					"Swap hands while holding a projectile weapon will change the mode of Hallowed Beam between 'Default' (default), " +
					"'Healing' (only heals players, does not work on mobs), and 'Attack' (only applies mob effects, does not heal). " +
					"This skill can only apply Recoil twice before touching the ground. Cooldown: %ss each charge.")
					.formatted((long) CAST_RANGE,
						StringUtils.multiplierToPercentage(HALLOWED_HEAL_PERCENT),
						(long) HALLOWED_RADIUS,
						StringUtils.ticksToSeconds(HALLOWED_UNDEAD_STUN),
						StringUtils.ticksToSeconds(HALLOWED_LIVING_STUN),
						(long) HALLOWED_1_MAX_CHARGES,
						StringUtils.ticksToSeconds(HALLOWED_1_COOLDOWN)),
				("Hallowed Beam has %s charges (and can apply Recoil three times before touching the ground), " +
					"the cooldown is reduced to %ss, and players healed by it gain +%s damage resistance for %ss.")
					.formatted((long) HALLOWED_2_MAX_CHARGES,
						StringUtils.ticksToSeconds(HALLOWED_2_COOLDOWN),
						StringUtils.multiplierToPercentageWithSign(Math.abs(HALLOWED_DAMAGE_REDUCTION_PERCENT)),
						StringUtils.ticksToSeconds(HALLOWED_DAMAGE_REDUCTION_DURATION)))
			.simpleDescription("Heal a targeted player, damage a targeted Undead, or stun a targeted non-Undead from a distance.")
			.cooldown(HALLOWED_1_COOLDOWN, HALLOWED_2_COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", HallowedBeam::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.addTrigger(new AbilityTriggerInfo<>("swapMode", "swap mode", HallowedBeam::swapMode, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false),
				AbilityTriggerInfo.HOLDING_PROJECTILE_WEAPON_RESTRICTION))
			.displayItem(Material.BOW);

	private @Nullable Crusade mCrusade;

	private enum Mode {
		DEFAULT(0, "Default"),
		HEALING(1, "Healing"),
		ATTACK(2, "Attack");

		public final int mScore;
		private final String mLabel;

		Mode(int score, String label) {
			mScore = score;
			mLabel = label;
		}
	}

	private Mode mMode = Mode.DEFAULT;
	private int mLastCastTicks = 0;
	private final HallowedBeamCS mCosmetic;

	public HallowedBeam(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = (int) CharmManager.getLevel(player, CHARM_CHARGE) + (isLevelOne() ? HALLOWED_1_MAX_CHARGES : HALLOWED_2_MAX_CHARGES);
		mCharges = getTrackedCharges();
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new HallowedBeamCS());
		if (player != null) {
			int modeIndex = ScoreboardUtils.getScoreboardValue(player, MODE_SCOREBOARD).orElse(0);
			mMode = Mode.values()[Math.max(0, Math.min(modeIndex, Mode.values().length - 1))];
		}

		Bukkit.getScheduler().runTask(plugin,
			() -> mCrusade = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, Crusade.class));
	}

	public double getPercentHealth(LivingEntity le) {
		return le.getHealth()/EntityUtils.getMaxHealth(le);
	}

	public boolean cast() {
		World world = mPlayer.getWorld();

		// Targeting
		double range = CharmManager.getRadius(mPlayer, CHARM_DISTANCE, CAST_RANGE);
		Predicate<Entity> playerFilter = e -> e instanceof Player p && p != mPlayer && p.getGameMode() != GameMode.SPECTATOR
			&& getPercentHealth(p) < 0.995 // Do not heal if health is full
			&& Plugin.getInstance().mEffectManager.getEffects(p, PercentHeal.class).stream() // Do not heal if there is a custom effect preventing heal
				.filter(percentHeal -> percentHeal.getValue() < -0.995).findAny().isEmpty();
		Predicate<Entity> hostileFilter = e -> EntityUtils.isHostileMob(e) && !ScoreboardUtils.checkTag(e, AbilityUtils.IGNORE_TAG) && !e.isDead() && e.isValid();

		LivingEntity e = null;
		switch (mMode) {
			case ATTACK -> e = EntityUtils.getEntityAtCursor(mPlayer, range, hostileFilter);
			case HEALING -> {
				ArrayList<LivingEntity> entities = new ArrayList<>(EntityUtils.getEntitiesAtCursor(mPlayer, range, playerFilter));
				if (!entities.isEmpty()) {
					// Sort by lower %hp
					entities.sort((a, b) -> Double.compare(getPercentHealth(a), getPercentHealth(b)));
					e = entities.get(0);
				}
			}
			case DEFAULT -> e = EntityUtils.getEntityAtCursor(mPlayer, range, hostileFilter.or(playerFilter));
			default -> {
			}
		}

		if (e == null) {
			return false;
		}

		int ticks = Bukkit.getServer().getCurrentTick();
		// Prevent double casting on accident
		if (ticks - mLastCastTicks <= 5 || !consumeCharge()) {
			return false;
		}
		mLastCastTicks = ticks;

		PlayerInventory inventory = mPlayer.getInventory();
		ItemStack inMainHand = inventory.getItemInMainHand();

		//Unsure why the runnable needs to exist, but it breaks if I don't have it
		LivingEntity targetedEntity = e;
		Bukkit.getScheduler().runTask(mPlugin, () -> {
			Location loc = mPlayer.getEyeLocation();
			Vector dir = loc.getDirection();

			if (targetedEntity instanceof Player healedPlayer) {
				mCosmetic.beamHealEffect(world, mPlayer, healedPlayer, dir, CAST_RANGE);
				PlayerUtils.healPlayer(mPlugin, healedPlayer, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_HEAL, EntityUtils.getMaxHealth(healedPlayer) * HALLOWED_HEAL_PERCENT), mPlayer);

				Location eLoc = healedPlayer.getLocation().add(0, healedPlayer.getHeight() / 2, 0);
				if (isLevelTwo()) {
					double resistance = HALLOWED_DAMAGE_REDUCTION_PERCENT - CharmManager.getLevelPercentDecimal(mPlayer, CHARM_RESISTANCE);
					int duration = CharmManager.getDuration(mPlayer, CHARM_RESISTANCE_DURATION, HALLOWED_DAMAGE_REDUCTION_DURATION);
					mPlugin.mEffectManager.addEffect(healedPlayer, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(duration, resistance));
				}
				for (LivingEntity le : EntityUtils.getNearbyMobs(eLoc, HALLOWED_RADIUS)) {
					MovementUtils.knockAway(healedPlayer, le, 0.65f, true);
				}
			} else {
				mCosmetic.beamHarm(world, mPlayer, targetedEntity, dir, CAST_RANGE);
				Location eLoc = LocationUtils.getHalfHeightLocation(targetedEntity);
				int stunDuration;
				if (Crusade.enemyTriggersAbilities(targetedEntity, mCrusade)) {
					double damage = ItemStatUtils.getAttributeAmount(inMainHand, AttributeType.PROJECTILE_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND);
					damage += Sniper.apply(mPlayer, targetedEntity, ItemStatUtils.getEnchantmentLevel(inMainHand, EnchantmentType.SNIPER));
					damage += PointBlank.apply(mPlayer, targetedEntity, ItemStatUtils.getEnchantmentLevel(inMainHand, EnchantmentType.POINT_BLANK));
					// Hallowed Beam has special case for proj damage scaling in ProjectileDamageMultiply. See AbilityUtils for more info
					damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, damage);
					DamageUtils.damage(mPlayer, targetedEntity, DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true, true);

					if (ItemStatUtils.getEnchantmentLevel(inMainHand, EnchantmentType.FIRE_ASPECT) > 0) {
						EntityUtils.applyFire(mPlugin, 20 * 15, targetedEntity, mPlayer);
					}

					stunDuration = HALLOWED_UNDEAD_STUN;

					mCosmetic.beamHarmCrusade(mPlayer, eLoc);
				} else {
					stunDuration = HALLOWED_LIVING_STUN;

					mCosmetic.beamHarmOther(mPlayer, eLoc);
				}

				EntityUtils.applyStun(mPlugin, CharmManager.getDuration(mPlayer, CHARM_STUN, stunDuration), targetedEntity);

				Crusade.addCrusadeTag(targetedEntity, mCrusade);
			}
			applyRecoil();
			applyGrappling(targetedEntity);
		});

		return true;
	}

	public void applyRecoil() {
		ItemStack item = mPlayer.getInventory().getItemInMainHand();
		double recoil = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.RECOIL);
		if (recoil > 0
			&& !EntityUtils.isRecoilDisable(mPlugin, mPlayer, mMaxCharges)
			&& !mPlayer.isSneaking()
			&& !ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
			Recoil.applyRecoil(mPlayer, recoil);
			EntityUtils.applyRecoilDisable(mPlugin, 9999, (int) EntityUtils.getRecoilDisableAmount(mPlugin, mPlayer) + 1, mPlayer);
		}
	}

	public void applyGrappling(LivingEntity target) {
		ItemStack item = mPlayer.getInventory().getItemInMainHand();
		double grappling = ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.GRAPPLING);
		if (grappling > 0
			&& !EntityUtils.isRecoilDisable(mPlugin, mPlayer, mMaxCharges)
			&& !ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES)) {
			if (getPlayer().isSneaking()) {
				Grappling.pullMob(mPlayer, target, Grappling.MOB_HORIZONTAL_SPEED, grappling);
			} else {
				Grappling.pullMob(target, mPlayer, Grappling.PLAYER_HORIZONTAL_SPEED, grappling);
			}
			EntityUtils.applyRecoilDisable(mPlugin, 9999, (int) EntityUtils.getRecoilDisableAmount(mPlugin, mPlayer) + 1, mPlayer);
		}
	}

	public boolean swapMode() {
		if (mMode == Mode.DEFAULT) {
			mMode = Mode.HEALING;
		} else if (mMode == Mode.HEALING) {
			mMode = Mode.ATTACK;
		} else {
			mMode = Mode.DEFAULT;
		}
		sendActionBarMessage(ClassAbility.HALLOWED_BEAM.getName() + " Mode: " + mMode.mLabel);
		ScoreboardUtils.setScoreboardValue(mPlayer, MODE_SCOREBOARD, mMode.mScore);
		ClientModHandler.updateAbility(mPlayer, this);
		return true;
	}

	@Override
	public @Nullable String getMode() {
		return mMode.name().toLowerCase(Locale.ROOT);
	}
}
