package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class HalloweenCreeperBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_halloween_creeper";

	private static final NamespacedKey LOOT_TABLE = NamespacedKeyUtils.fromString("epic:event/halloween2019/tricked_creeper");

	public HalloweenCreeperBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		if (!(boss instanceof Creeper creeper)) {
			throw new Exception(identityTag + " only works on mobs!");
		}
		mBoss.addScoreboardTag("NoTrickyTransformation");

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), 100, null);

		if (!creeper.isIgnited()) {
			creeper.customName(Component.text("Tricky Creeper", NamedTextColor.GOLD));
			creeper.setGlowing(true);
			creeper.setExplosionRadius(20);
			creeper.setMaxFuseTicks(100);
			creeper.setIgnited(true);
			creeper.addScoreboardTag(CrowdControlImmunityBoss.identityTag);
			creeper.addScoreboardTag(AbilityUtils.IGNORE_TAG);

			new BukkitRunnable() {
				private int mTicks = 0;

				@Override
				public void run() {
					Location loc = boss.getLocation().add(0, 1, 0);
					World world = loc.getWorld();
					world.playSound(loc, Sound.ENTITY_CREEPER_HURT, SoundCategory.HOSTILE, 1.0f, 0.9f);
					switch (mTicks) {
						case 12 -> {
							Block block = world.getBlockAt(loc);
							if ((block.getType() == Material.AIR || block.isLiquid())
								    && !ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.ADVENTURE_MODE)
								    && !ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.RESTRICTED)
								    && !ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.BLOCKBREAK_DISABLED)) {
								block.setType(Material.CHEST);
								ChestUtils.setNonLootLimitedChest(block, true);
								if (block.getState() instanceof Chest chest) {
									chest.customName(Component.text("Creeperween Chest", NamedTextColor.GOLD, TextDecoration.BOLD));
									chest.setLootTable(Bukkit.getLootTable(LOOT_TABLE));
									chest.update();
								}
							} else {
								InventoryUtils.getItemsFromLootTable(loc, LOOT_TABLE).forEach(item -> loc.getWorld().dropItemNaturally(loc, item));
							}
						}
						case 2, 6 -> summonFirework(loc, true);
						case 9 -> summonFirework(loc, false);
						case 13 -> {
							summonFirework(loc, false);
							this.cancel();
						}
						default -> {
						}
					}
					mTicks++;
				}
			}.runTaskTimer(plugin, 0, 10);
		}
	}

	@Override
	public void onHurt(DamageEvent event) {
		// Reduce the damage but don't cancel the hit
		event.setFlatDamage(0);
	}

	private void summonFirework(Location loc, boolean first) {
		EntityUtils.fireworkAnimation(loc, List.of(Color.fromRGB(255, 106, 31)), first ? FireworkEffect.Type.BALL_LARGE : FireworkEffect.Type.CREEPER, 0, !first);
	}
}
