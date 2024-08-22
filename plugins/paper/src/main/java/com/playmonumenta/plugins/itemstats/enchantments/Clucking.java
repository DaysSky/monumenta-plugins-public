package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Clucking implements Enchantment {

	private static final int DROPPED_TICK_PERIOD = 60;
	private boolean mRun = false;

	@Override
	public String getName() {
		return "Clucking";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CLUCKING;
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		if (oneHz && mRun && FastUtils.RANDOM.nextBoolean()) {
			mRun = false;
			player.playSound(player.getLocation(), Sound.ENTITY_CHICKEN_AMBIENT, SoundCategory.PLAYERS, 1.0f, 0.8f);
		} else if (oneHz) {
			mRun = true;
		}
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, double value) {
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (!item.getLocation().isChunkLoaded() || item.isDead() || !item.isValid()) {
					this.cancel();
					return;
				}

				item.getWorld().playSound(item.getLocation(), Sound.ENTITY_CHICKEN_AMBIENT, SoundCategory.PLAYERS, 1.0f, 0.8f);

				// Very infrequently check if the item is still actually there
				mTicks++;
				if (mTicks > 200) {
					mTicks = 0;
					if (!EntityUtils.isStillLoaded(item)) {
						this.cancel();
					}
				}
			}
		}.runTaskTimer(plugin, 10, DROPPED_TICK_PERIOD);
	}
}
