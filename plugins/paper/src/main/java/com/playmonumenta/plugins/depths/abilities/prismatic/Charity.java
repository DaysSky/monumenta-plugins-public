package com.playmonumenta.plugins.depths.abilities.prismatic;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

public class Charity extends DepthsAbility {

	public static final String ABILITY_NAME = "Charity";
	private static final int[] ABSORPTION = {4, 5, 6, 7, 8, 10};
	private static final int DURATION = 10 * 20;
	public static final double[] REVIVE_POWER = {1.4, 1.5, 1.6, 1.7, 1.8, 2.0};

	public static final DepthsAbilityInfo<Charity> INFO =
		new DepthsAbilityInfo<>(Charity.class, ABILITY_NAME, Charity::new, DepthsTree.PRISMATIC, DepthsTrigger.PASSIVE)
			.displayItem(Material.STONE_SHOVEL)
			.descriptions(Charity::getDescription);

	public Charity(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static Description<Charity> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Charity>(color)
			.add("You revive players ")
			.addPercent(a -> REVIVE_POWER[rarity - 1] - 1, REVIVE_POWER[rarity - 1] - 1, false, true)
			.add(" faster. After you help revive a player, both you and the revived player gain ")
			.add(a -> ABSORPTION[rarity - 1], ABSORPTION[rarity - 1], false, null, true)
			.add(" absorption health for ")
			.addDuration(DURATION)
			.add(" seconds.");
	}

	public static void onCharityRevive(Player revivedPlayer, List<Player> revivers) {
		revivedPlayer.getWorld().playSound(revivedPlayer.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1, 0.85f);

		int highestAbsorption = 0;
		for (Player reviver : revivers) {
			int level = DepthsManager.getInstance().getPlayerLevelInAbility(ABILITY_NAME, reviver);
			if (level == 0) {
				break;
			}
			int absorption = ABSORPTION[level - 1];
			highestAbsorption = Math.max(absorption, highestAbsorption);
			AbsorptionUtils.addAbsorption(reviver, absorption, absorption, DURATION);
		}
		if (highestAbsorption > 0) {
			AbsorptionUtils.addAbsorption(revivedPlayer, highestAbsorption, highestAbsorption, DURATION);
		}
	}
}
