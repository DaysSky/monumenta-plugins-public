package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.BossBarManager;
import com.playmonumenta.plugins.bosses.BossBarManager.BossHealthAction;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellPurgeNegatives;
import com.playmonumenta.plugins.bosses.spells.falsespirit.SpellForceTwo;
import com.playmonumenta.plugins.bosses.spells.snowspirit.DeckTheHalls;
import com.playmonumenta.plugins.bosses.spells.snowspirit.ElfSummon;
import com.playmonumenta.plugins.bosses.spells.snowspirit.JollyBall;
import com.playmonumenta.plugins.bosses.spells.snowspirit.ShiningStar;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SnowSpirit extends SerializedLocationBossAbilityGroup {
	public static final String identityTag = "boss_snowspirit";
	public static final int detectionRange = 75;

	private boolean mFinalPhase;
	private boolean mMinibossesPresent;
	private List<Entity> mActiveMinibosses;

	public SnowSpirit(Plugin plugin, LivingEntity boss, Location spawnLoc, Location endLoc) {
		super(plugin, identityTag, boss, spawnLoc, endLoc);

		mFinalPhase = false;
		mMinibossesPresent = false;
		mActiveMinibosses = new ArrayList<>();

		SpellManager activeSpells1 = new SpellManager(Arrays.asList(
			new SpellForceTwo(plugin, boss, 5, 20 * 3),
			new DeckTheHalls(plugin, boss),
			new ElfSummon(plugin, mBoss, 3, 3, 20, 15)
		));

		SpellManager activeSpells2 = new SpellManager(Arrays.asList(
			new SpellForceTwo(plugin, boss, 5, 20 * 3),
			new DeckTheHalls(plugin, boss),
			new ShiningStar(boss, plugin),
			new ElfSummon(plugin, mBoss, 3, 3, 20, 15)
		));

		List<Spell> passiveSpells = Arrays.asList(
			new SpellPurgeNegatives(boss, 20 * 8),
			new JollyBall(plugin, boss, 12 * 20, 0.25)
		);

		Map<Integer, BossHealthAction> events = new HashMap<>();

		events.put(75, mBoss -> {
			mActiveMinibosses.add(LibraryOfSoulsIntegration.summon(getMinibossSummonLocation(), "GhostOfHolidaysPast"));
			healthScaleMinibosses();

			mMinibossesPresent = true;
			changePhase(SpellManager.EMPTY, passiveSpells, null);
		});

		events.put(50, mBoss -> {
			mActiveMinibosses.add(LibraryOfSoulsIntegration.summon(getMinibossSummonLocation(), "GhostOfHolidaysPast"));
			mActiveMinibosses.add(LibraryOfSoulsIntegration.summon(getMinibossSummonLocation(), "GhostOfHolidaysPresent"));
			healthScaleMinibosses();

			mMinibossesPresent = true;
			changePhase(SpellManager.EMPTY, passiveSpells, null);
		});

		events.put(25, mBoss -> {
			mActiveMinibosses.add(LibraryOfSoulsIntegration.summon(getMinibossSummonLocation(), "GhostOfHolidaysPast"));
			mActiveMinibosses.add(LibraryOfSoulsIntegration.summon(getMinibossSummonLocation(), "GhostOfHolidaysPresent"));
			mActiveMinibosses.add(LibraryOfSoulsIntegration.summon(getMinibossSummonLocation(), "GhostOfHolidaysFuture"));
			healthScaleMinibosses();

			mMinibossesPresent = true;
			changePhase(SpellManager.EMPTY, passiveSpells, null);
			mFinalPhase = true;
		});

		events.put(0, mBoss -> changePhase(SpellManager.EMPTY, Collections.emptyList(), null));

		BossBarManager bossBar = new BossBarManager(boss, detectionRange, BossBar.Color.WHITE, BossBar.Overlay.NOTCHED_10, events);
		constructBoss(activeSpells1, passiveSpells, detectionRange, bossBar, 20 * 10);

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}

				new PartialParticle(Particle.SNOWBALL, mBoss.getLocation(), 10, 1, 1, 1).spawnAsEntityActive(boss);

				if (mSpawnLoc.distance(mBoss.getLocation()) > 6) {
					mTicks += 10;

					if (mTicks >= ShiningStar.DURATION + 20) {
						teleport(mSpawnLoc);
						mTicks = 0;
					}
				} else {
					mTicks = 0;
				}
			}
		}.runTaskTimer(mPlugin, 0, 10);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}

				if (mMinibossesPresent) {
					boolean minibossesAlive = false;
					for (Entity miniboss : mActiveMinibosses) {
						if (!miniboss.isDead() && miniboss.isValid()) {
							minibossesAlive = true;
							break;
						}
					}
					if (!minibossesAlive) {
						mActiveMinibosses = new ArrayList<>();
						mMinibossesPresent = false;
						if (mFinalPhase) {
							changePhase(activeSpells2, passiveSpells, null);
						} else {
							changePhase(activeSpells1, passiveSpells, null);
						}
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 10);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}

				List<Player> players = PlayerUtils.playersInRange(mSpawnLoc, 100, true);
				players.removeIf(player -> player.getLocation().getY() < mSpawnLoc.getY() + 8);
				for (Player player : players) {
					BossUtils.bossDamagePercent(boss, player, 0.1);
				}

			}
		}.runTaskTimer(mPlugin, 0, 20);

		for (Player player : PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true)) {
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 10.0f, 0.75f);
		}
	}

	//Teleport with special effects
	private void teleport(Location loc) {
		World world = loc.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.CLOUD, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(mBoss);
		mBoss.teleport(loc);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		new PartialParticle(Particle.FIREWORKS_SPARK, mBoss.getLocation().add(0, 1, 0), 70, 0.25, 0.45, 0.25, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 35, 0.1, 0.45, 0.1, 0.15).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 25, 0.2, 0, 0.2, 0.1).spawnAsEntityActive(mBoss);
	}


	@Override
	public void death(@Nullable EntityDeathEvent event) {
		changePhase(SpellManager.EMPTY, Collections.emptyList(), null);
		mBoss.setHealth(100);
		mBoss.setInvulnerable(true);
		mBoss.setAI(false);
		mBoss.setGravity(false);
		mBoss.setPersistent(true);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 20 * 1000, 10));
		teleport(mSpawnLoc);
		World world = mBoss.getWorld();

		new ShiningStar(mBoss, mPlugin).run();

		if (event != null) {
			event.setCancelled(true);
			event.setReviveHealth(100);
		}

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks >= ShiningStar.DURATION + 10) {
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_DEATH, SoundCategory.HOSTILE, 10, 0);

					this.cancel();
					mBoss.remove();

					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true)) {
							MessagingUtils.sendTitle(player, Component.text("VICTORY", NamedTextColor.RED, TextDecoration.BOLD), Component.text("Snow Spirit, Remnant of Snow", NamedTextColor.DARK_RED, TextDecoration.BOLD));
							player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.HOSTILE, 100, 0.8f);
						}

						mEndLoc.getBlock().setType(Material.REDSTONE_BLOCK);
					}, 20 * 3);
				}

				if (mTicks % 10 == 0) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1, 0);
				}

				new PartialParticle(Particle.EXPLOSION_LARGE, mBoss.getLocation(), 1, 1, 1, 1).minimumCount(1).spawnAsEntityActive(mBoss);

				mTicks += 2;
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}

	@Override
	public void init() {
		int playerCount = PlayerUtils.playersInRange(mBoss.getLocation(), detectionRange, true).size();
		int hpDel = 2500;
		double bossTargetHp = hpDel * BossUtils.healthScalingCoef(playerCount, 0.5, 0.6);
		EntityUtils.setMaxHealthAndHealth(mBoss, bossTargetHp);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_FOLLOW_RANGE, detectionRange);
		EntityUtils.setAttributeBase(mBoss, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 1);

		mBoss.setPersistent(true);

	}

	@Override
	public void onHurt(DamageEvent event) {
		if (mMinibossesPresent) {
			LivingEntity source = event.getSource();
			event.setCancelled(true);
			if (source instanceof Player player) {
				player.playSound(mBoss.getLocation(), Sound.ITEM_SHIELD_BLOCK, SoundCategory.HOSTILE, 1, 1);
				List<Entity> living = new ArrayList<>(mActiveMinibosses);
				living.removeIf(miniboss -> miniboss.isDead() || !miniboss.isValid());
				String ghosts = living.size() <= 1 ? "is a ghost" : "are " + living.size() + " ghosts";
				player.sendMessage(Component.text("Your weapon glides cleanly through the spirit, seemingly doing nothing. There " + ghosts + " alive.", NamedTextColor.AQUA));
			}
		}
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		if (source instanceof Player player) {
			ItemStack helmet = player.getInventory().getHelmet();
			if (ItemUtils.getPlainName(helmet).contains("The Grinch")) {
				Location loc = mBoss.getLocation();
				event.updateDamageWithMultiplier(1.2);
				player.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 0.75f, 1.65f);
				player.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.HOSTILE, 0.75f, 0.5f);
			}
		}
	}

	private Location getMinibossSummonLocation() {
		double theta = 2 * Math.PI * Math.random();
		double r = 10 * Math.random();
		return mSpawnLoc.clone().add(r * FastUtils.cos(theta), 0, r * FastUtils.sin(theta));
	}

	private void healthScaleMinibosses() {
		for (Entity miniboss : mActiveMinibosses) {
			if (miniboss instanceof LivingEntity mini) {
				int playerCount = PlayerUtils.playersInRange(mSpawnLoc, detectionRange, true).size();
				double hpDel = EntityUtils.getMaxHealth(mini);
				double bossTargetHp = hpDel * BossUtils.healthScalingCoef(playerCount, 0.5, 0.6);
				EntityUtils.setAttributeBase(mini, Attribute.GENERIC_MAX_HEALTH, bossTargetHp);
				mini.setHealth(bossTargetHp);
			}
		}
	}
}
