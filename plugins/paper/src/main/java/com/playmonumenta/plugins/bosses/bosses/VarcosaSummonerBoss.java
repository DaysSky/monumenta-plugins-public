package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellPlayerAction;
import com.playmonumenta.plugins.bosses.spells.SpellPurgeNegatives;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellActions;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellJibberJabber;
import com.playmonumenta.plugins.bosses.spells.varcosamist.SpellSummonConstantly;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;


public final class VarcosaSummonerBoss extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_varcosa_summoner";
	public static final int detectionRange = 50;
	public static int mSummonPeriod = 20 * 5;
	private Location mCenter;
	private boolean mActive;

	public VarcosaSummonerBoss(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);

		List<String> summonableMobs = List.of("SeaWolf", "PirateGunner", "DrownedCrewman");

		String[] speak = new String[] {
			"Yarr! It be you again? This shan't end the way it did before, matey!",
			"Ye took me treasure, ye stole me fleece. Now I be takin' ye to the beyond!",
			"Yarr! Me ghostly crew rides forth!"};

		int radius = 50;
		for (LivingEntity e : mBoss.getLocation().getNearbyEntitiesByType(ArmorStand.class, radius, radius, radius)) {
			if (e.getScoreboardTags().contains("varcosa_center")) {
				mCenter = e.getLocation();
				break;
			}
		}

		if (mCenter == null) {
			mCenter = mSpawnLoc.clone().subtract(0, 0.5, 0);
		}

		SpellPlayerAction action = SpellActions.getTooLowAction(mBoss, mCenter);

		SpellPlayerAction tooHighAction = SpellActions.getTooHighAction(mBoss, mCenter);

		BukkitRunnable runnable = SpellActions.getTeleportEntityRunnable(mBoss, mCenter);
		runnable.runTaskTimer(plugin, 20, 20 * 2);

		summonArmorStandIfNoneAreThere(mCenter.clone().add(0, 0, 11.5));
		summonArmorStandIfNoneAreThere(mCenter.clone().add(0, 0, -11.5));
		summonArmorStandIfNoneAreThere(mCenter.clone().add(11.5, 0, 0));
		summonArmorStandIfNoneAreThere(mCenter.clone().add(-11.5, 0, 0));

		List<Spell> passiveSpells = Arrays.asList(new SpellSummonConstantly(summonableMobs, mSummonPeriod, 50, 6, 2, mCenter, this),
			new SpellJibberJabber(mBoss, speak, radius),
			action, tooHighAction);
		SpellManager activeSpells = new SpellManager(List.of(new SpellPurgeNegatives(mBoss, 100)));

		BossBarManager bossBar = new BossBarManager(mBoss, detectionRange + 20, BarColor.RED, BarStyle.SEGMENTED_10, null);

		super.constructBoss(activeSpells, passiveSpells, detectionRange, bossBar);
	}

	@Override
	public void death(@Nullable EntityDeathEvent event) {
		mActive = false;
		changePhase(SpellManager.EMPTY, Collections.emptyList(), null);

		String dio = "Fine! Ye make me do this meself!";
		PlayerUtils.executeCommandOnNearbyPlayers(mSpawnLoc, detectionRange, "tellraw @s [\"\",{\"text\":\"" + dio + "\",\"color\":\"red\"}]");

		new BukkitRunnable() {
			@Override
			public void run() {
				if (!PlayerUtils.playersInRange(mCenter, detectionRange, true).isEmpty()) {
					mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
				}
			}
		}.runTaskLater(mPlugin, 20 * 1);

	}

	@Override
	public void init() {
		int bossTargetHp = 0;
		int hpDelta = 650;
		int playerCount = BossUtils.getPlayersInRangeForHealthScaling(mBoss, detectionRange);
		while (playerCount > 0) {
			bossTargetHp = bossTargetHp + hpDelta;
			hpDelta = hpDelta / 2 + 75;
			playerCount--;
		}

		mBoss.setInvulnerable(true);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 99, 9999));
		mBoss.setAI(false);

		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_MAX_HEALTH, bossTargetHp);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);

		mBoss.setHealth(bossTargetHp);

		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
			MessagingUtils.sendBoldTitle(player, Component.text("Varcosa", NamedTextColor.RED), Component.text("Mighty Pirate Captain", NamedTextColor.DARK_RED));
		}

		mActive = true;
	}

	public void onSummonKilled() {
		if (mActive) {
			mBoss.setHealth(mBoss.getHealth() - 20 < 0 ? 0 : mBoss.getHealth() - 20);
		}
	}

	private void summonArmorStandIfNoneAreThere(Location loc) {
		if (loc.getNearbyEntitiesByType(ArmorStand.class, 2, 2, 2).isEmpty()) {
			ArmorStand as = (ArmorStand) mBoss.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
			as.setInvisible(true);
			as.setInvulnerable(true);
			as.setMarker(true);
			as.addScoreboardTag("summon_constantly_stand");
		}
	}
}
