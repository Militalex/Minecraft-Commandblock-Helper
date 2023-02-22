package com.github.militalex.commands;

import com.github.militalex.main.CommandBlockHelper;
import com.github.militalex.util.CommandUtil;
import com.github.militalex.util.Tuple;
import com.github.militalex.util.datapack.Datapack;
import com.github.militalex.util.datapack.DatapackManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.StringArgument;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.CommandBlock;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;

public class MusicScanCommand {
	public static void register(){
		/*		new CommandAPICommand("savemusic-help")
				.executes((sender, args) -> {
					sender.sendMessage(ChatColor.AQUA + "Syntax: " + ChatColor.GOLD + "/savemusic " +
							ChatColor.YELLOW + "<Position> <name>\n" +
							ChatColor.AQUA + "Usage: " + ChatColor.GOLD + "This command saves your music to an playable datapack " +
							"by following from the given position all redstone components. " +
							"The name of your music datapack will be your given name. The function itself will called play.mcfunction");
				}).register();*/

		// add savemusic command
		new CommandAPICommand("savemusic")
				// Aguments: <Location> <Name of dPack>
				.withArguments(new LocationArgument("Position of Redstone", LocationType.BLOCK_POSITION))
				.withArguments(new StringArgument("datapack name"))
				// can only executed by players
				.executesPlayer((player, args) -> {
					try {
						scanMusic(player, (Location) args[0], (String) args[1]);
					} catch (Exception e){
						// Print player error message.
						player.sendMessage(ChatColor.RED + "[Error]: " + e.getMessage());
						throw e;
					}
				})
				.register();
	}

	/**
	 * This method saves music build as redstone contraption to datapack.
	 */
	private static void scanMusic(@NotNull Player player, @NotNull Location startLoc, @NotNull String name){
		// Command have to point to redstone wire
		if (startLoc.getBlock().getType() != Material.REDSTONE_WIRE){
			player.sendMessage(ChatColor.RED + "Your given location does not point to redstone wire!");
		}

		// Queue setup for latitude search (Breitensuche)
		queue = new ArrayDeque<>();
		addToQueue(startLoc, 0, 1, true);

		player.sendMessage(ChatColor.AQUA + "[/savemusic] " + ChatColor.GOLD + "Starting to scan ...");

		// Latitude search
		while (!queue.isEmpty()){
			// Remove from queue and extract data
			final Object[] data = queue.remove();
			final Location curLoc = (Location) data[0];
			final int score = (int) data[1];
			final int sLength = (int) data[2];
			final boolean propagate = (boolean) data[3];

			// Material depend behavior decision
			boolean delFlag = false;	// Defines if processed block should be replaced with air
			final Material curMat = curLoc.getBlock().getType();
			if (curMat.isAir()) continue;	// Skip air

			//CommandBlockHelper.LOGGER.log(Level.INFO, curMat + " at " + curLoc );

			// general block redstone behavior
			if (curMat.isBlock() && curMat.isOccluding() && propagate) {
				processNaturalBlock(curLoc, score, sLength);
				delFlag = true;
			}

			// Redstone behaviour
			if (curMat == Material.REDSTONE_WIRE) {
				processRedstoneWire(curLoc, score, sLength);
				delFlag = true;
			}
			// Redstone repeater behaviour
			else if (curMat == Material.REPEATER) {
				processRepeater(curLoc, score, sLength);
				delFlag = true;
			}
			// Redstone comparator behaviour is not supported
			else if (curMat == Material.COMPARATOR) CommandBlockHelper.LOGGER.log(Level.WARNING, "Comparator are not allowed!");
			// Command block behaviour
			else if (curMat == Material.COMMAND_BLOCK || curMat == Material.REPEATING_COMMAND_BLOCK || curMat == Material.CHAIN_COMMAND_BLOCK){
				// Extract CmdBlock and Command
				final CommandBlock commandBlock = (CommandBlock) curLoc.getBlock().getState();
				String command = commandBlock.getCommand();
				if (command.startsWith("/")) command = command.substring(1);

				// Process commandblock in general
				processCommandblock(commandBlock, score, sLength);

				// Linphator Music-Slider
				if (command.startsWith("setblock") && command.contains("redstone_block")) processMusicSliderStart(curLoc, score, command);
				else if (command.startsWith("clone")) processMusicSliderMiddle(curLoc, score, command);
				else if (command.startsWith("setblock") && command.contains("air")) processMusicSliderEnd(curLoc, score, command);

				// finally playsound command handler
				else if (command.contains("playsound")) processPlaysoundCommand(name, score,
						sLength, command, curMat == Material.REPEATING_COMMAND_BLOCK);

				delFlag = true;
			}

			// Delete block if flag is set
			if (delFlag){
				curLoc.getBlock().setType(Material.AIR, false);
			}
		}

		// Saves rest in BufferList into datapack functions
		flushAll(name);

		// Clear data
		datapack = null;
		BUFFER_LIST.clear();

		// Scan finished msg
		player.sendMessage(ChatColor.AQUA + "[/savemusic] " + ChatColor.GOLD + "Scan finished!");

		// Reloading datapacks with fancy messages
		player.sendMessage(ChatColor.DARK_AQUA + "[/reload] " + ChatColor.GOLD + "Now reloading Datapacks ...");
		DatapackManager.getInstance().reload();
		player.sendMessage(ChatColor.DARK_AQUA + "[/reload] " + ChatColor.GOLD + "Reloading of Datapacks finished. " +
				"Your Datapack is " + ChatColor.YELLOW + name + " is now available.");
	}

	private static ArrayDeque<Object[]> queue;
	private static final List<String> SOUNDS = Arrays.stream(Sound.values())
			.map(sound -> sound.getKey().toString()).toList();

	public static void addToQueue(Location newLoc, int score, int signalLength, boolean propagate){
		if (newLoc.getBlock().getType().isAir()) return;
		queue.add(new Object[]{newLoc, score, signalLength, propagate});
	}

	public static void processNaturalBlock(Location curLoc, int score, int signalLength){
		if (!curLoc.getBlock().getType().isBlock() || !curLoc.getBlock().getType().isSolid())
			throw new IllegalArgumentException("Only natural solid redstone electrified " +
					"blocks are processed here, nothing else. But it was " + curLoc.getBlock().getType());

		// Complex for loop enable iterating only over direct block neighbors without diagonal neighbours
		for (int dx = -1; dx <= 1; dx ++){
			for (int dy = -1; dy <= 1; dy ++){
				if (dx!= 0 && Math.abs(dx) == Math.abs(dy)) continue;
				for (int dz = -1; dz <= 1; dz ++){
					if (dx == 0 && dy == 0 && dz == 0) continue;
					if (dx!= 0 && Math.abs(dx) == Math.abs(dz)) continue;
					if (dx == 0 && dy!= 0 && (Math.abs(dy) == Math.abs(dz))) continue;

					final Location newLoc = curLoc.clone().add(dx, dy, dz);

					if (newLoc.getBlock().getType() != Material.CHAIN_COMMAND_BLOCK) {
						addToQueue(newLoc, score, signalLength, false);
					}
				}
			}
		}
	}

	public static void processRedstoneWire(Location curLoc, int score, int signalLength){
		if (curLoc.getBlock().getType() != Material.REDSTONE_WIRE) throw new IllegalArgumentException("Redstone wire is processed here, nothing else.");

		// Get BlockStates
		final HashMap<String, String> redstoneState = CommandUtil.getBlockStates(curLoc.getBlock().getState()); ;

		// Add Block Redstone is lying on to queue
		addToQueue(curLoc.clone().subtract(0, 1, 0), score, signalLength, true);

		// Add Block to queue which Redstone would power by running into
		redstoneState.forEach((key, value) -> {
			// Ignore power and unconnected sides
			if (key.equals("power")) return;
			if (value.equals("none")) return;

			// calculate direction step depending on side
			int addX = key.equals("east") ? 1 : key.equals("west") ? -1 : 0;
			int addZ = key.equals("south") ? 1 : key.equals("north") ? -1 : 0;

			// create new Location
			final Location newLoc = curLoc.clone();

			if (value.equals("side")){
				// add Block to queue Redstone is facing in
				newLoc.add(addX, 0, addZ);
				addToQueue(newLoc, score, signalLength, true);

				// enables redstone to go downwards if not on glass or sth. like that
				if (!curLoc.getBlock().getType().isOccluding()) return;

				final Location newLoc2 = newLoc.clone();
				newLoc2.subtract(0, 1, 0);
				addToQueue(newLoc2, score, signalLength, true);
			}
			else {	// Invariant: value == "up"
				// enables redstone to go upwards
				newLoc.add(addX, 1, addZ);
				addToQueue(newLoc, score, signalLength, true);
			}
		});
	}

	public static void processRepeater(Location curLoc, int score, int signalLength){
		if (curLoc.getBlock().getType() != Material.REPEATER) throw new IllegalArgumentException("Redstone wire is processed here, nothing else.");

		// Get BlockStates
		final HashMap<String, String> repeaterStates = CommandUtil.getBlockStates(curLoc.getBlock().getState());
		if (repeaterStates.get("locked").equals("true")){
			curLoc.getBlock().setType(Material.AIR, false);
			return;
		}
		final String facing = repeaterStates.get("facing");
		final int delay = Integer.parseInt(repeaterStates.get("delay"));

		// Calculate Repeater signal stretch
		switch (delay) {
			case 1 -> { if (signalLength < 2) signalLength = 2; }
			case 2 -> { if (signalLength < 4) signalLength = 4; }
			case 3 -> { if (signalLength < 6) signalLength = 6; }
			case 4 -> { if (signalLength < 8) signalLength = 8; }
		}

		// Calculate direction step depending on facing
		int addX = facing.equals("east") ? -1 : facing.equals("west") ? 1 : 0;
		int addZ = facing.equals("south") ? -1 : facing.equals("north") ? 1 : 0;

		// Create new Location
		final Location newLoc = curLoc.clone().add(addX, 0, addZ);
		addToQueue(newLoc, score + 2 * delay, signalLength, true);
	}

	public static void processCommandblock(CommandBlock block, int score, int sLength){
		final HashMap<String,String> blockStates = CommandUtil.getBlockStates(block);
		final String facing = blockStates.get("facing");

		if (block.getCommand().startsWith("say")) System.out.println(block.getCommand() + " at score " + score + " with length " + sLength);

		// calculate direction step depending on side
		int addX = facing.equals("east") ? 1 : facing.equals("west") ? -1 : 0;
		int addY = facing.equals("up") ? 1 : facing.equals("down") ? -1 : 0;
		int addZ = facing.equals("south") ? 1 : facing.equals("north") ? -1 : 0;

		// Chain commandblocks
		final Location newLoc = block.getLocation().clone().add(addX, addY, addZ);
		if (newLoc.getBlock().getType() == Material.CHAIN_COMMAND_BLOCK){
			addToQueue(newLoc, score, sLength, false);
		}
	}

	public static void processMusicSliderStart(Location curLoc, int score, String command){
		if (!command.startsWith("setblock") || !command.contains("redstone_block"))
			throw new IllegalArgumentException(command + " cannot be a start of a music slider.");

		// Extract position from command
		int x, y, z;
		final Scanner scanner = new Scanner(command);
		scanner.next();
		x = CommandUtil.getCordFromTildeOrNumber(curLoc.getBlockX(),  scanner.next());
		y = CommandUtil.getCordFromTildeOrNumber(curLoc.getBlockY(),  scanner.next());
		z = CommandUtil.getCordFromTildeOrNumber(curLoc.getBlockZ(),  scanner.next());

		// Add hopefully first clone command to queue
		addToQueue(new Location(curLoc.getWorld(), x, y - 1, z), score + 1, 1, false);

		// Add Surrounding
		addToQueue(new Location(curLoc.getWorld(), x + 1, y, z), score + 1, 1, false);
		addToQueue(new Location(curLoc.getWorld(), x - 1, y, z), score + 1, 1, false);
		addToQueue(new Location(curLoc.getWorld(), x , y, z + 1), score + 1, 1, false);
		addToQueue(new Location(curLoc.getWorld(), x , y, z - 1), score + 1, 1, false);
		addToQueue(new Location(curLoc.getWorld(), x , y + 1, z), score + 1, 1, false);
	}

	public static void processMusicSliderMiddle(Location curLoc, int score, String command){
		if (!command.startsWith("clone"))
			throw new IllegalArgumentException(command + " cannot be a middle commandblock of a music slider");

		// Extract position from command
		int x, y, z;
		final Scanner scanner = new Scanner(command);
		scanner.next(); scanner.next(); scanner.next(); scanner.next(); scanner.next(); scanner.next(); scanner.next();
		x = CommandUtil.getCordFromTildeOrNumber(curLoc.getBlockX(),  scanner.next());
		y = CommandUtil.getCordFromTildeOrNumber(curLoc.getBlockY(),  scanner.next());
		z = CommandUtil.getCordFromTildeOrNumber(curLoc.getBlockZ(),  scanner.next());

		// Add hopefully next clone command to queue
		addToQueue(new Location(curLoc.getWorld(), x, y - 1, z), score + 1, 1, false);

		// Add Surrounding
		addToQueue(new Location(curLoc.getWorld(), x + 1, y, z), score + 1, 1, false);
		addToQueue(new Location(curLoc.getWorld(), x - 1, y, z), score + 1, 1, false);
		addToQueue(new Location(curLoc.getWorld(), x , y, z + 1), score + 1, 1, false);
		addToQueue(new Location(curLoc.getWorld(), x , y, z - 1), score + 1, 1, false);
		addToQueue(new Location(curLoc.getWorld(), x , y + 1, z), score + 1, 1, false);
	}

	public static void processMusicSliderEnd(Location curLoc, int score, String command){
		if (!command.startsWith("setblock") || !command.contains("air"))
			throw new IllegalArgumentException(command + " cannot be a start of a music slider.");

		// Extract position from command
		int x, y, z;
		final Scanner scanner = new Scanner(command);
		scanner.next();
		x = CommandUtil.getCordFromTildeOrNumber(curLoc.getBlockX(),  scanner.next());
		y = CommandUtil.getCordFromTildeOrNumber(curLoc.getBlockY(),  scanner.next());
		z = CommandUtil.getCordFromTildeOrNumber(curLoc.getBlockZ(),  scanner.next());

		// Add Surrounding
		addToQueue(new Location(curLoc.getWorld(), x + 1, y, z), score + 1, 1, false);
		addToQueue(new Location(curLoc.getWorld(), x - 1, y, z), score + 1, 1, false);
		addToQueue(new Location(curLoc.getWorld(), x , y, z + 1), score + 1, 1, false);
		addToQueue(new Location(curLoc.getWorld(), x , y, z - 1), score + 1, 1, false);
		addToQueue(new Location(curLoc.getWorld(), x , y + 1, z), score + 1, 1, false);
	}

	public static void processPlaysoundCommand(String name, int score, int sLength, String command, boolean isRepeating){
		if (!command.contains("playsound"))
			throw new IllegalArgumentException(command + " cannot be a playsound command.");

		// Playsound rearrangement
		final StringBuilder modCmdBuilder = new StringBuilder("as @a[tag=musik_play] at @s run ");

		// Remove prefixed execute stuff
		final Scanner scanner = new Scanner(command);
		while (scanner.hasNext()){
			if (scanner.next().equals("playsound")) {
				modCmdBuilder.append("playsound ");
				break;
			}
		}

		// Extract sound effect
		final String soundEffect = scanner.next();
		modCmdBuilder.append(soundEffect).append(" ");

		// Extract sound fader and set it to record or voice
		scanner.next();
		if (SOUNDS.contains(soundEffect) || soundEffect.startsWith("ins_") || soundEffect.startsWith("fx_")) modCmdBuilder.append("record ");
		else modCmdBuilder.append("voice ");

		// Extract executor and set it to @s
		scanner.next();
		modCmdBuilder.append("@s ");

		// Extract location and set it to 3x ~
		for (int i = 0; i < 3; i++) {
			scanner.next();
			modCmdBuilder.append("~ ");
		}

		// Extract volume and pith if information is given
		for (int i = 0; i < 2 && scanner.hasNext(); i++){
			modCmdBuilder.append(scanner.next()).append(" ");
		}

		// Add Command to corresponding datapack function
		final String modCmd = modCmdBuilder.toString();
		if (!isRepeating) addToDatapack(name, score, modCmd);
		else {
			for (int i = 0; i < sLength; i++){
				addToDatapack(name, score + i, modCmd);
			}
		}
	}

	// Datapack handling

	private static Datapack datapack;
	private static final int MAX_SCORE_PER_FUNCTION = 20;

	private static final PriorityQueue<Tuple<Integer,String>> BUFFER_LIST = new PriorityQueue<>(Comparator.comparingInt(Tuple::getKey));

	public static void addToDatapack(String name, int score, String command){
		if (datapack == null && DatapackManager.getInstance().exists(name)) {
			throw new IllegalArgumentException("Datapack " + name + " already exists. It is not possible to modify existent Datapack.");
		}
		if (datapack == null) {
			datapack = DatapackManager.getInstance().getOrCreate(name);
		}

		addToBuffer(name, command, score);
	}

	public static void calcTreeFunctions(String name){
		final Path fPath = DatapackManager.DATAPACK_FOLDER.resolve(name).resolve("data").resolve(name).resolve("functions");
		final ArrayDeque<>

		Files.list(fPath).map(Path::toFile).filter(File::isFile).
	}

	private static int fFlushPtr = 0;
	public static void addToBuffer(String name, String command, int score){
		boolean wasEmpty = BUFFER_LIST.isEmpty();

		BUFFER_LIST.add(new Tuple<>(score, command));

		if (wasEmpty){
			fFlushPtr = BUFFER_LIST.element().getKey();
		}

		if (BUFFER_LIST.element().getKey() > fFlushPtr + MAX_SCORE_PER_FUNCTION * 6) {
			flushFunction(name);
			flushFunction(name);
			flushFunction(name);

			if (!BUFFER_LIST.isEmpty()){
				fFlushPtr = BUFFER_LIST.element().getKey();
			}
		}
	}

	public static void flushAll(String name){
		while (!BUFFER_LIST.isEmpty()){
			flushFunction(name);
		}
	}

	public static void flushFunction(String name){
		final StringBuilder fBuilder = new StringBuilder();

		int fScore = BUFFER_LIST.element().getKey(), lScore = BUFFER_LIST.element().getKey();
		int localMax = fScore + MAX_SCORE_PER_FUNCTION;

		while (!BUFFER_LIST.isEmpty() && BUFFER_LIST.element().getKey() < localMax) {
			final var tuple = BUFFER_LIST.remove();
			lScore = tuple.getKey();
			final String cmd = tuple.getValue();

			fBuilder.append("execute if score ").append(name).append(" musik matches ").append(lScore).append(" ").append(cmd).append("\n");
		}

		assert datapack != null;
		if (!fBuilder.toString().equals("")){
			datapack.createFunction(name, "musik_" + fScore + "_" + lScore, fBuilder.toString());
		}
	}
}
