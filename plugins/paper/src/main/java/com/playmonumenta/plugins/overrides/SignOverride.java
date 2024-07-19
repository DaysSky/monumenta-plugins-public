package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SignOverride extends BaseOverride {
	public static final String COPIED_SIGN_HEADER = "Sign contents:";
	public static final String SIGN_IS_GLOWING = "Sign is glowing";

	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		if (player == null || player.getGameMode().equals(GameMode.CREATIVE)) {
			return true;
		}

		List<String> loreLines = ItemUtils.getPlainLoreIfExists(item);
		if (loreLines.isEmpty()) {
			return true;
		}

		// If possible, copy the contents of a sign item to the block
		if (COPIED_SIGN_HEADER.equals(loreLines.get(0))) {
			if (item.hasItemMeta()) {
				ItemMeta meta = item.getItemMeta();
				if (meta instanceof BlockStateMeta blockStateMeta) {
					BlockState blockState = blockStateMeta.getBlockState();
					if (blockState instanceof Sign signItem) {
						Block placedBlock = event.getBlockPlaced();
						final Location loc = event.getBlock().getLocation();
						final Material blockType = placedBlock.getType();
						final BlockData originalBlockData = placedBlock.getBlockData();
						final @Nullable DyeColor signColor = signItem.getColor();
						final boolean glowing = signItem.isGlowingText();
						final List<Component> signLines = signItem.lines();

						Component allSignLines = MessagingUtils.concatenateComponents(signLines, Component.newline());
						if (MonumentaNetworkChatIntegration.hasBadWord(player, allSignLines)) {
							AuditListener.logSevere(player.getName()
									+ " attempted to place a sign with a bad word: `/s "
									+ ServerProperties.getShardName()
									+ "` `/world " + loc.getWorld().getName()
									+ "` `/tp @s " + loc.getBlockX()
									+ " " + loc.getBlockY()
									+ " " + loc.getBlockZ()
									+ "`"
							);
							item.setAmount(0);
							return false;
						}

						new BukkitRunnable() {
							@Override
							public void run() {
								Block signBlock = loc.getBlock();
								signBlock.setType(blockType);
								signBlock.setBlockData(originalBlockData, true);
								Sign sign = (Sign) signBlock.getState();
								sign.setColor(signColor);
								sign.setGlowingText(glowing && !ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.SHOPS_POSSIBLE));
								for (int lineNum = 0; lineNum < signLines.size(); ++lineNum) {
									sign.line(lineNum, signLines.get(lineNum));
								}
								sign.update();
								loc.getWorld().playSound(loc, Sound.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 1.0f, 1.0f);
								CoreProtectIntegration.logPlacement(player, signBlock);
							}
						}.runTask(plugin);
						item.subtract();
						return false;
					}
				}
			}
			return true;
		}

		return false;
	}

	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, Block block, PlayerInteractEvent event) {
		if (item != null && item.getType() == Material.GLOW_INK_SAC && ZoneUtils.hasZoneProperty(block.getLocation(), ZoneUtils.ZoneProperty.SHOPS_POSSIBLE)) {
			return false;
		}

		Sign sign = (Sign) block.getState();
		boolean output = item == null || !(item.hasItemMeta() && item.getItemMeta().hasLore() && (ItemUtils.isDye(item.getType()) || item.getType() == Material.GLOW_INK_SAC));

		// Compile all the lines of text together and make sure it is not a leaderboard that is being clicked
		boolean hasText = false;
		Component display = Component.empty();
		for (Component component : sign.lines()) {
			if (component.clickEvent() != null) {
				return output;
			}
			String line = MessagingUtils.PLAIN_SERIALIZER.serialize(component).trim();
			if (line.matches("^[-=+~]*$")) {
				// When dumping signs to chat, skip decoration lines
				continue;
			}
			line = line.replaceAll("[${}]", "");
			Component part;
			if (component.hasDecoration(TextDecoration.OBFUSCATED)) {
				part = Component.text("no spoiler for you")
					.decoration(TextDecoration.OBFUSCATED, true);
			} else {
				part = Component.text(line);
			}
			hasText = true;
			display = display.append(part).append(Component.space());
		}

		if (hasText) {
			player.sendMessage(display);
		}

		if (player.isSneaking()) {
			return output;
		}

		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			// If clicking a sign with another sign, copy the contents to the item
			if (!meta.hasDisplayName()) {
				if (ItemUtils.isSign(item.getType()) &&
				    meta instanceof BlockStateMeta blockStateMeta) {
					BlockState blockState = blockStateMeta.getBlockState();
					if (blockState instanceof Sign signItem) {
						DyeColor dyeColor = sign.getColor();
						List<Component> signLines = sign.lines();
						List<Component> loreLines = new ArrayList<>();
						loreLines.add(Component.text(COPIED_SIGN_HEADER, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
						Component loreHeader = Component.text("> ").color(NamedTextColor.BLACK).decoration(TextDecoration.ITALIC, false);
						Component loreBase = Component.text("").color(NamedTextColor.BLACK);

						Component allSignLines = MessagingUtils.concatenateComponents(signLines, Component.newline());
						if (MonumentaNetworkChatIntegration.hasBadWord(player, allSignLines)) {
							Location loc = block.getLocation();
							AuditListener.logSevere(player.getName()
								+ " attempted to copy a sign with a bad word: `/s "
								+ ServerProperties.getShardName()
								+ "` `/world " + loc.getWorld().getName()
								+ "` `/tp @s " + loc.getBlockX()
								+ " " + loc.getBlockY()
								+ " " + loc.getBlockZ()
								+ "`"
							);
							item.setAmount(0);
							return false;
						}

						signItem.setColor(dyeColor);
						Color color = dyeColor.getColor();
						float r = color.getRed() / 255.0f;
						float g = color.getGreen() / 255.0f;
						float b = color.getBlue() / 255.0f;
						loreHeader = loreHeader.color(TextColor.color(r, g, b));
						loreBase = loreBase.color(TextColor.color(
							0.2f + 0.8f * r,
							0.2f + 0.8f * g,
							0.2f + 0.8f * b
						));

						for (int lineNum = 0; lineNum < signLines.size(); ++lineNum) {
							Component line = signLines.get(lineNum);
							signItem.line(lineNum, line);
							Component loreLine = loreHeader.append(loreBase.append(line));
							loreLines.add(loreLine);
						}
						boolean signGlowing = sign.isGlowingText();
						signItem.setGlowingText(signGlowing);
						if (signGlowing) {
							loreLines.add(Component.text(SIGN_IS_GLOWING, NamedTextColor.WHITE));
						}
						blockStateMeta.setBlockState(blockState);
						blockStateMeta.lore(loreLines);
						item.setItemMeta(blockStateMeta);
						ItemUtils.setPlainLore(item);
						player.sendMessage(Component.text("Copied sign data."));
					}
				}
			}

			// If clicking with an unnamed book, allow editing the sign
			if (item.getType().equals(Material.BOOK) && !meta.hasLore()) {
				boolean isCreative = player.getGameMode().equals(GameMode.CREATIVE);
				if (isCreative ||
					ZoneUtils.playerCanMineBlock(player, block.getLocation())) {
					SignUtils.edit(block, player, isCreative);
				}
			}
		}

		return output;
	}
}
