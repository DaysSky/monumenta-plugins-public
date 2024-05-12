package com.playmonumenta.plugins.minigames.chess;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.BoardState;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.ChessPiece;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.ChessPieceType;
import com.playmonumenta.plugins.minigames.chess.ChessBoard.ChessTeam;
import com.playmonumenta.plugins.minigames.chess.ChessInterface.InterfaceType;
import com.playmonumenta.plugins.minigames.chess.events.ChessEvent;
import com.playmonumenta.plugins.minigames.chess.events.EndGameChessEvent;
import com.playmonumenta.plugins.minigames.chess.events.MovePieceChessEvent;
import com.playmonumenta.plugins.minigames.chess.events.PromotingChessEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;

@SuppressWarnings("NullAway")
public class ChessManager implements Listener {

	public static final String FEN_DEFAULT_BOARD_STRING = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

	private static final String[] CHESSBOARD_COMMANDS = {"chessboard", "chess", "cb"};

	public enum ChessBoardType {
		PVP,
		PVAI
	}

	private static Plugin mPlugin;
	private static final Map<String, ChessBoard> mBoards = new HashMap<>();
	private static final Map<ChessBoard, List<ChessInterface>> mBoardsInterfaces = new HashMap<>();

	private static final Boolean DEBUG = false;

	public ChessManager(Plugin plugin) {
		mPlugin = plugin;

		//Register commands.
		CommandPermission perms = CommandPermission.fromString("monumenta.command.chess");
		List<Argument<?>> arguments = new ArrayList<>();

		for (String command : CHESSBOARD_COMMANDS) {

			arguments.clear();
			arguments.add(new StringArgument("Board Name"));
			arguments.add(new LiteralArgument("create"));
			arguments.add(new MultiLiteralArgument("type",
					ChessBoardType.PVP.name(),
					ChessBoardType.PVAI.name()));

			new CommandAPICommand(command)
					.withPermission(perms)
					.withArguments(arguments)
				.withOptionalArguments(new GreedyStringArgument("fen String"))
					.executes((sender, args) -> {
						createBoard(args.getUnchecked("Board Name"), ChessBoardType.valueOf(args.getUnchecked("type")), args.getOrDefaultUnchecked("fen String", FEN_DEFAULT_BOARD_STRING));
					}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions(ArgumentSuggestions.strings(
				(info) -> mBoards.keySet().toArray(new String[0]))));
			arguments.add(new LiteralArgument("get"));
			arguments.add(new LiteralArgument("fen"));
			new CommandAPICommand(command)
					.withPermission(perms)
					.withArguments(arguments)
					.executes((sender, args) -> {
						printCurrentChessBoardFenString(sender, args.getUnchecked("Board Name"));
					}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions(ArgumentSuggestions.strings(
				(info) -> mBoards.keySet().toArray(new String[0]))));
			arguments.add(new LiteralArgument("restart"));
			new CommandAPICommand(command)
					.withPermission(perms)
					.withArguments(arguments)
					.executes((sender, args) -> {
						restartBoard(args.getUnchecked("Board Name"));
					}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions(ArgumentSuggestions.strings(
				(info) -> mBoards.keySet().toArray(new String[0]))));
			arguments.add(new LiteralArgument("set"));
			arguments.add(new LiteralArgument("fen"));
			arguments.add(new GreedyStringArgument("fen String"));
			new CommandAPICommand(command)
					.withPermission(perms)
					.withArguments(arguments)
					.executes((sender, args) -> {
						String boardName = args.getUnchecked("Board Name");
						ChessBoard board = mBoards.get(boardName);
						if (board == null) {
							throw CommandAPI.failWithString("Invalid name, Board: " + boardName + " doesn't exists");
						}
						board.buildBoardFromString(args.getUnchecked("fen String"));

						for (ChessInterface chessInterface : mBoardsInterfaces.get(board)) {
							chessInterface.refresh();
						}
					}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions(ArgumentSuggestions.strings(
				(info) -> mBoards.keySet().toArray(new String[0]))));
			arguments.add(new LiteralArgument("delete"));
			new CommandAPICommand(command)
					.withPermission(perms)
					.withArguments(arguments)
					.executes((sender, args) -> {
						deleteBoard(args.getUnchecked("Board Name"));
					}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions(ArgumentSuggestions.strings(
				(info) -> mBoards.keySet().toArray(new String[0]))));
			arguments.add(new LiteralArgument("refresh"));
			new CommandAPICommand(command)
					.withPermission(perms)
					.withArguments(arguments)
					.executes((sender, args) -> {
						refreshGuis(args.getUnchecked("Board Name"));
					}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions(ArgumentSuggestions.strings(
				(info) -> mBoards.keySet().toArray(new String[0]))));
			arguments.add(new LiteralArgument("set"));
			arguments.add(new LiteralArgument("piece"));
			arguments.add(new MultiLiteralArgument("piecetype",
					ChessPieceType.BISHOPS.name(),
					ChessPieceType.KING.name(),
					ChessPieceType.KNIGHTS.name(),
					ChessPieceType.PAWNS.name(),
					ChessPieceType.QUEEN.name(),
					ChessPieceType.ROOKS.name()));

			arguments.add(new MultiLiteralArgument("team",
					ChessTeam.BLACK.name(),
					ChessTeam.WHITE.name()));
			arguments.add(new IntegerArgument("Position", 0, 63));

			new CommandAPICommand(command)
					.withPermission(perms)
					.withArguments(arguments)
					.executes((sender, args) -> {
						setPiece(args.getUnchecked("Board Name"), args.getUnchecked("piecetype"), args.getUnchecked("team"), args.getUnchecked("Position"));
					}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions(ArgumentSuggestions.strings(
				(info) -> mBoards.keySet().toArray(new String[0]))));
			arguments.add(new LiteralArgument("set"));
			arguments.add(new LiteralArgument("player"));
			arguments.add(new MultiLiteralArgument("team", "white", "black"));
			arguments.add(new PlayerArgument("player"));
			new CommandAPICommand(command)
					.withPermission(perms)
					.withArguments(arguments)
					.executes((sender, args) -> {
						setPlayer(args.getUnchecked("Board Name"), args.getUnchecked("player"), args.getUnchecked("team"));
					}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions(ArgumentSuggestions.strings(
				(info) -> mBoards.keySet().toArray(new String[0]))));
			arguments.add(new LiteralArgument("set"));
			arguments.add(new LiteralArgument("gui"));
			arguments.add(new MultiLiteralArgument("interfacetype",
					ChessInterface.InterfaceType.WHITEPLAYER.name().toLowerCase(Locale.getDefault()),
					ChessInterface.InterfaceType.BLACKPLAYER.name().toLowerCase(Locale.getDefault()),
					ChessInterface.InterfaceType.SPECTATOR.name().toLowerCase(Locale.getDefault())));
			arguments.add(new LocationArgument("starting positions", LocationType.BLOCK_POSITION));
			arguments.add(new MultiLiteralArgument("facing",
					ChessInterface.FacingPosition.NORTH.getLabel(),
					ChessInterface.FacingPosition.SOUTH.getLabel()));
			new CommandAPICommand(command)
					.withPermission(perms)
					.withArguments(arguments)
					.executes((sender, args) -> {
						createGui(args.getUnchecked("Board Name"), args.getUnchecked("interfacetype"), args.getUnchecked("starting positions"), args.getUnchecked("facing"));
					}).register();

			arguments.clear();
			arguments.add(new StringArgument("Board Name").replaceSuggestions(ArgumentSuggestions.strings(
				(info) -> mBoards.keySet().toArray(new String[0]))));
			arguments.add(new LiteralArgument("surrend"));
			arguments.add(new PlayerArgument("Surrender"));
			new CommandAPICommand(command)
					.withPermission(perms)
					.withArguments(arguments)
					.executes((sender, args) -> {
						surrender(args.getUnchecked("Board Name"), args.getUnchecked("Surrender"));
					}).register();
		}
	}

	public static void printCurrentChessBoardFenString(CommandSender sender, String boardname) throws WrapperCommandSyntaxException {
		if (!mBoards.containsKey(boardname)) {
			throw CommandAPI.failWithString("Invalid name, Board: " + boardname + " doesn't exists");
		}
		ChessBoard board = mBoards.get(boardname);
		String fenString = board.convertBoardToFenString();

		sender.sendMessage(Component.text("Chessboard: ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).append(Component.text(boardname, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, false)));
		sender.sendMessage(Component.text("FEN: ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).append(Component.text(fenString, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, false)).clickEvent(ClickEvent.copyToClipboard(fenString)));
	}

	public static void createGui(String name, String type, Location startingLoc, String facing) throws WrapperCommandSyntaxException {
		if (!mBoards.containsKey(name)) {
			throw CommandAPI.failWithString("Invalid name, Board: " + name + " doesn't exists");
		}
		ChessBoard board = mBoards.get(name);
		final ChessInterface newChessInterface = new ChessInterface(board, InterfaceType.valueOf(type.toUpperCase(Locale.getDefault())));
		for (ChessInterface chessInterface : new HashSet<>(mBoardsInterfaces.get(board))) {
			if (chessInterface.mType == newChessInterface.mType) {
				chessInterface.removeFrames();
				mBoardsInterfaces.get(board).remove(chessInterface);
			}
		}
		//annoing stuff.... we need to delay the placing of frames so minecraft don't fucked up.
		new BukkitRunnable() {
			@Override
			public void run() {
				newChessInterface.buildBoard(startingLoc, ChessInterface.FacingPosition.valueOfLabel(facing));
				mBoardsInterfaces.get(board).add(newChessInterface);
			}
		}.runTaskLater(mPlugin, 1);
	}

	public static void createBoard(String name, ChessBoardType type, String fenString) throws WrapperCommandSyntaxException {
		if (mBoards.containsKey(name)) {
			throw CommandAPI.failWithString("Invalid name, name " + name + " already used for a different board");
		}

		ChessBoard board = new ChessBoard(name, type);
		board.buildBoardFromString(fenString);
		mBoards.put(name, board);
		mBoardsInterfaces.put(board, new ArrayList<>());
	}

	public static void deleteBoard(String name) throws WrapperCommandSyntaxException {
		if (!mBoards.containsKey(name)) {
			throw CommandAPI.failWithString("Invalid name, Board: " + name + " doesn't exists");
		}

		ChessBoard board = mBoards.get(name);
		List<ChessInterface> chessInterfaces = mBoardsInterfaces.get(board);
		if (chessInterfaces != null) {
			for (ChessInterface ci: chessInterfaces) {
				ci.destroy();
			}
			chessInterfaces.clear();
		}
		mBoardsInterfaces.remove(board);
	}

	public static void restartBoard(String name) throws WrapperCommandSyntaxException {
		if (!mBoards.containsKey(name)) {
			throw CommandAPI.failWithString("Invalid name, Board: " + name + " doesn't exists");
		}

		ChessBoard board = mBoards.get(name);
		board.restart();
		List<ChessInterface> chessInterfaces = mBoardsInterfaces.get(board);
		for (ChessInterface ci: chessInterfaces) {
			ci.removeRunnable();
			ci.refresh();
		}
	}

	public static void surrender(String name, Player loser) throws WrapperCommandSyntaxException {
		if (!mBoards.containsKey(name)) {
			throw CommandAPI.failWithString("Invalid name, Board: " + name + " doesn't exists");
		}

		ChessBoard board = mBoards.get(name);
		board.updateState(BoardState.ENDED);
		ChessPlayer whitePlayer = board.getPlayer(ChessTeam.WHITE);
		ChessPlayer blackPlayer = board.getPlayer(ChessTeam.BLACK);

		EndGameChessEvent event = new EndGameChessEvent(board, whitePlayer, blackPlayer);


		if (whitePlayer != null && whitePlayer.mPlayer.equals(loser)) {
			event.setEndGameScore(1);
		} else if (blackPlayer != null && blackPlayer.mPlayer.equals(loser)) {
			event.setEndGameScore(0);
		}

		Bukkit.getPluginManager().callEvent(event);
	}

	public static void refreshGuis(String name) throws WrapperCommandSyntaxException {
		if (!mBoards.containsKey(name)) {
			throw CommandAPI.failWithString("Invalid name, Board: " + name + " doesn't exists");
		}

		ChessBoard board = mBoards.get(name);
		for (ChessInterface ci: mBoardsInterfaces.get(board)) {
			ci.refresh();
		}
	}

	public static void setPlayer(String name, Player player, String role) throws WrapperCommandSyntaxException {
		if (!mBoards.containsKey(name)) {
			throw CommandAPI.failWithString("Invalid name, Board: " + name + " doesn't exists");
		}

		ChessBoard board = mBoards.get(name);
		board.setPlayer(player, ChessTeam.valueOf(role.toUpperCase(Locale.getDefault())));
	}

	public static void setPiece(String name, String piece, String teamString, int pos) throws WrapperCommandSyntaxException {
		if (!mBoards.containsKey(name)) {
			throw CommandAPI.failWithString("Invalid name, Board: " + name + " doesn't exists");
		}

		ChessPieceType type = ChessPieceType.valueOf(piece.toUpperCase(Locale.getDefault()));
		ChessTeam team = ChessTeam.valueOf(teamString.toUpperCase(Locale.getDefault()));
		if (type == null || pos < 0 || pos > 63) {
			throw CommandAPI.failWithString("Error. type: " + type + " team: " + team + " pos: " + pos);
		}

		ChessPiece newPiece = new ChessPiece(type, team, pos);
		mBoards.get(name).setPiece(newPiece, pos);
	}

	public static void updateGuiSlot(ChessBoard board, int slot) {
		for (ChessInterface ci : mBoardsInterfaces.get(board)) {
			ci.updateSlot(slot);
		}
	}

	public static void updateGuiInteractable(ChessBoard board) {
		for (ChessInterface cInterface : mBoardsInterfaces.get(board)) {
			cInterface.updateInteractable();
		}
	}

	public static void animationWin(Player player) {
		Location loc = player.getLocation();

		new BukkitRunnable() {
			int mTimer = 0;

			@Override
			public void run() {
				if (mTimer >= 15) {
					cancel();
					return;
				}

				EntityUtils.fireworkAnimation(loc, List.of(Color.LIME, Color.YELLOW, Color.ORANGE, Color.AQUA), FireworkEffect.Type.BURST, 0);

				mTimer += 5;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 5);

	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void onPlayerClicks(PlayerInteractEvent event) {
		if (event.useInteractedBlock() == Event.Result.DENY && event.useItemInHand() == Event.Result.DENY) {
			return;
		}
		Player player = event.getPlayer();
		if (ChessPlayer.isChessPlayer(player) && (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
			Entity entity = player.getTargetEntity(20);
			if (entity instanceof ItemFrame) {
				ChessBoard board = null;
				Set<String> tags = entity.getScoreboardTags();
				for (String boardName : mBoards.keySet()) {
					if (tags.contains(boardName)) {
						board = mBoards.get(boardName);
						break;
					}
				}

				if (board == null) {
					return;
				}

				for (ChessInterface chessInterface : mBoardsInterfaces.get(board)) {
					if (chessInterface.playerInteract((ItemFrame)entity, (Player)player)) {
						break;
					}
				}
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	public static void onChessEvent(ChessEvent event) {
		final ChessBoard board = event.getBoard();
		if (board != null) {
			String msg = "[ChessManager] On board: " + board.getName();
			if (event instanceof MovePieceChessEvent) {

				int from = ((MovePieceChessEvent) event).getOldPos();
				int to = ((MovePieceChessEvent) event).getNewPos();

				msg += " | MovePieceEvent (" + board.getChessPiece(to).toString() + ")";

				for (ChessInterface cInterface : mBoardsInterfaces.get(board)) {
					cInterface.moveEvent(from, to);
				}

				ChessTeam teamPlaying = board.getChessPiece(to).getPieceTeam();
				ChessPlayer nextPlayer = teamPlaying == ChessTeam.WHITE ? event.getBlackPlayer() : event.getWhitePlayer();

				if (nextPlayer != null && nextPlayer.mPlayer != null) {
					nextPlayer.mPlayer.playSound(nextPlayer.mPlayer.getLocation(), Sound.ENTITY_ARMOR_STAND_HIT, SoundCategory.PLAYERS, 10f, 0.6f);
				}

				board.updateState(teamPlaying == ChessTeam.WHITE ? BoardState.BLACK_TURN : BoardState.WHITE_TURN);

				for (ChessInterface cInterface : mBoardsInterfaces.get(board)) {
					cInterface.updateInteractable();
				}

				if (DEBUG) {
					Plugin.getInstance().getLogger().warning(msg);
				}

			} else if (event instanceof EndGameChessEvent) {
				ChessPlayer winnerPlayer = ((EndGameChessEvent) event).getWinner();
				ChessPlayer loserPlayer = ((EndGameChessEvent) event).getLoser();
				float result = ((EndGameChessEvent) event).getEndGameScore();

				msg += " | EndGameEvent Players-> W: " + (winnerPlayer != null ? winnerPlayer.mPlayer.getName() : "null") + " L: " + (loserPlayer != null ? loserPlayer.mPlayer.getName() : "null") + " Result: " + result;

				if (DEBUG) {
					Plugin.getInstance().getLogger().warning(msg);
				}

				if (winnerPlayer != null && winnerPlayer.mPlayer != null) {
					animationWin(winnerPlayer.mPlayer);
				}

				ChessPlayer.removeChessPlayer(winnerPlayer);
				ChessPlayer.removeChessPlayer(loserPlayer);

			} else if (event instanceof PromotingChessEvent) {
				ChessPlayer promotingPlayer = ((PromotingChessEvent)event).getPlayer();
				ChessPiece promotingPiece = ((PromotingChessEvent)event).getPiece();
				if (promotingPlayer != null && promotingPlayer.mPlayer != null) {
					new ChessPromotingCustomInventory(promotingPlayer.mPlayer, board, promotingPiece).openInventory(promotingPlayer.mPlayer, mPlugin);
					msg += " | PromotingEvent Player: " + promotingPlayer.mPlayer.getName() + " (" + promotingPiece + ")";

				} else {
					board.changePieceType(promotingPiece, ChessPieceType.QUEEN);
				}


				if (DEBUG) {
					Plugin.getInstance().getLogger().warning(msg);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public static void onChunkUnload(ChunkUnloadEvent event) {
		boolean shouldCheckInterfaces = false;
		for (Entity entity : event.getChunk().getEntities()) {
			if (entity instanceof ItemFrame && entity.getScoreboardTags().contains(ChessInterface.ITEM_FRAME_TAG)) {
				Bukkit.getScheduler().runTask(mPlugin, entity::remove);
				shouldCheckInterfaces = true;
			}
		}

		if (shouldCheckInterfaces) {
			for (ChessBoard board : mBoards.values()) {
				for (ChessInterface chessInterface : new HashSet<>(mBoardsInterfaces.get(board))) {
					if (chessInterface.shouldDestroy()) {
						chessInterface.destroy();
						mBoardsInterfaces.get(board).remove(chessInterface);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public static void onPlayerQuit(PlayerQuitEvent event) {
		//this is only to be sure that we are not gonna have infinite chessplayer
		Player player = event.getPlayer();
		ChessPlayer.removeChessPlayer(player);
	}

	public void unloadAll() {
		for (ChessBoard board : mBoardsInterfaces.keySet()) {
			for (ChessInterface chessInterface : mBoardsInterfaces.get(board)) {
				chessInterface.unload();
			}
		}
		mBoards.clear();
		mBoardsInterfaces.clear();
	}
}
