package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ZoneUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class TurtleEggOverride extends BaseOverride {
	@Override
	public boolean physicalBlockInteraction(Plugin plugin, Player player, Action action, Block block, PlayerInteractEvent event) {
		if (player == null) {
			return true;
		}
		Location loc = block.getLocation();
		return player.getGameMode() == GameMode.CREATIVE || !ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.ADVENTURE_MODE);
	}
}

