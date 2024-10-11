package com.playmonumenta.plugins.bosses.spells.rkitxet;

import com.playmonumenta.plugins.bosses.ChargeUpManager;
import com.playmonumenta.plugins.bosses.bosses.RKitxet;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellEndlessAgony extends Spell {
	public static final Particle.DustOptions ENDLESS_AGONY_COLOR = new Particle.DustOptions(Color.fromRGB(214, 58, 166), 1.65f);
	public static final double RADIUS = 3;
	private static final int MOVEMENT_TIME = 4 * 20;
	private static final int WAIT_UNTIL_DAMAGE_TIME = (int) (1.5 * 20);
	private static final int MAX_COUNT = 25;
	public static final String SPELL_NAME = "Endless Agony";

	private final Plugin mPlugin;
	private final RKitxet mRKitxet;
	private final double mRange;
	private final Location mCenter;
	private int mCount;
	private final ChargeUpManager mChargeUp;
	private final int mCooldown;

	public SpellEndlessAgony(Plugin plugin, RKitxet rKitxet, Location center, double range, int cooldown) {
		mPlugin = plugin;
		mRKitxet = rKitxet;
		mRange = range;
		mCenter = center;
		mCount = 0;
		mCooldown = cooldown;

		mChargeUp = new ChargeUpManager(mRKitxet.getEntity(), MOVEMENT_TIME + WAIT_UNTIL_DAMAGE_TIME, Component.text("Forming " + SPELL_NAME + "...", NamedTextColor.DARK_PURPLE),
			BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_10, RKitxet.detectionRange);
	}

	@Override
	public void run() {
		mRKitxet.useSpell(SPELL_NAME);

		World world = mCenter.getWorld();

		List<Player> players = PlayerUtils.playersInRange(mCenter, mRange, false);
		if (players.size() == 0) {
			return;
		}
		if (players.size() > 1 && mRKitxet.getFuryTarget() != null) {
			players.remove(mRKitxet.getFuryTarget());
		}
		Collections.shuffle(players);
		Player target = players.get(0);
		mRKitxet.setAgonyTarget(target);

		mCount++;

		world.playSound(target.getLocation(), Sound.BLOCK_BASALT_STEP, SoundCategory.HOSTILE, 3, 0.8f);
		world.playSound(target.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 2, 1);
		target.playSound(target.getLocation(), Sound.ENTITY_SLIME_SQUISH, SoundCategory.HOSTILE, 3, 1);
		target.sendMessage(Component.text("Pain and suffering haunt you with every step you take.", NamedTextColor.LIGHT_PURPLE));

		PPCircle indicator = new PPCircle(Particle.REDSTONE, target.getLocation(), RADIUS).count(30).delta(0.1, 0.05, 0.1).data(ENDLESS_AGONY_COLOR);

		mChargeUp.setTitle(Component.text("Forming " + SPELL_NAME + "...", NamedTextColor.DARK_PURPLE));
		mChargeUp.setColor(BossBar.Color.PURPLE);

		BukkitRunnable movementRunnable = new BukkitRunnable() {
			Location mLoc = target.getLocation();

			@Override
			public void run() {
				mChargeUp.nextTick(5);

				Location targetLoc = target.getLocation();
				//If they die, stop
				if (!targetLoc.getWorld().equals(mRKitxet.getBossLocation().getWorld())
					    || targetLoc.distance(mRKitxet.getBossLocation()) > RKitxet.detectionRange) {
					mChargeUp.reset();
					this.cancel();
					return;
				}

				if (mChargeUp.getTime() <= MOVEMENT_TIME) {
					mLoc = targetLoc;
					mLoc.setY((int) mLoc.getY());
				}

				indicator.location(mLoc).spawnAsBoss();

				if (mChargeUp.getTime() == MOVEMENT_TIME) {
					world.playSound(targetLoc, Sound.BLOCK_BELL_RESONATE, SoundCategory.HOSTILE, 2f, 0.3f);
					mChargeUp.setTitle(Component.text("Channeling " + SPELL_NAME + "...", NamedTextColor.RED));
					mChargeUp.setColor(BossBar.Color.RED);
				}

				if (mChargeUp.getTime() >= MOVEMENT_TIME + WAIT_UNTIL_DAMAGE_TIME) {
					world.playSound(targetLoc, Sound.ENTITY_TURTLE_EGG_HATCH, SoundCategory.HOSTILE, 4, 0.8f);
					mRKitxet.mAgonyLocations.add(mLoc);
					mRKitxet.setAgonyTarget(null);
					mChargeUp.reset();
					this.cancel();
				}
			}

		};
		movementRunnable.runTaskTimer(mPlugin, 0, 5);
		mActiveRunnables.add(movementRunnable);
	}

	@Override
	public boolean canRun() {
		return mCount < MAX_COUNT && PlayerUtils.playersInRange(mCenter, mRange, false).size() > 0 && mRKitxet.canUseSpell(SPELL_NAME);
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
