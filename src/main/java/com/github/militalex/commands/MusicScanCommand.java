package com.github.militalex.commands;

import com.github.militalex.main.CommandBlockHelper;
import com.github.militalex.util.HomogenTuple;
import com.github.militalex.util.datapack.Datapack;
import com.github.militalex.util.datapack.DatapackManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.StringArgument;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * This class registers a command which enables scanning of built music as a contraption into a datapack.
 *
 * @author Militalex
 * @version 1.0
 */
public class MusicScanCommand {

	public static void register(){
		new CommandAPICommand("savemusic")
				.withShortDescription("Saves given music contraption as a datapack.")
				.withFullDescription("This command can generate asynchronously a datapack from your music. " +
						"Note that your music contraption will be deleted when doing so. " +
						"Therefore it is highly recommended to make a backup of your world firstly" +
						" and executes the command only on a copy of your music to prevent irreversible damage." + ChatColor.RED +
						" This command can only executed by players!")
				.withPermission(CommandPermission.OP)
				// Arguments
				.withArguments(new LocationArgument("Redstone wire", LocationType.BLOCK_POSITION))
				.withArguments(new StringArgument("Datapack Name"))
				// can only be executed by players
				.executesPlayer((player, args) -> {
					// Get name of datapack
					final String name = ((String) args[1]).toLowerCase();

					// Command have to point to redstone wire
					final Location startLoc = (Location) args[0];
					if (startLoc.getBlock().getType() != Material.REDSTONE_WIRE){
						player.sendMessage(ChatColor.RED + "Your given location does not point to redstone wire!");
						return;
					}

					// Scan start message
					player.sendMessage(ChatColor.AQUA + "[/savemusic] " + ChatColor.GOLD + "Starting to scan ...");

					// Start async scanning
					final MusicScanTickable tickable = new MusicScanTickable(name, unused -> {
						// Check if functions are created. If not -> message
						if (!DatapackManager.DATAPACK_FOLDER.resolve(name).resolve("data").resolve(name).resolve("functions").toFile().exists()){
							player.sendMessage(ChatColor.AQUA + "[/savemusic] " + ChatColor.RED + " No music blocks are found. Cannot create Datapack");
							return;
						}

						// Create function tree
						try {
							calcTreeFunctions(name);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}

						// Scan finished msg
						player.sendMessage(ChatColor.AQUA + "[/savemusic] " + ChatColor.GOLD + "Scan finished!");

						// Reloading datapacks with fancy messages
						player.sendMessage(ChatColor.DARK_AQUA + "[/reload] " + ChatColor.GOLD + "Now reloading Datapacks ...");
						DatapackManager.getInstance().reload();
						player.sendMessage(ChatColor.DARK_AQUA + "[/reload] " + ChatColor.GOLD + "Reloading of Datapacks finished. " +
								"Your Datapack is " + ChatColor.YELLOW + name + ChatColor.GOLD + " is now available.");
					});
					tickable.addToQueue(startLoc, 0, 1, true);
					tickable.start(0, 0, true);
				})
				.register();
	}

	// Tree creating functions

	/**
	 * Defines the maximum amount of children a node can have.
	 */
	public static final int CHILD_AMOUNT = 8;

	/**
	 * Calculate from leave nodes corresponding node until root is reached.
	 * @param name Name of the datapack containing leave nodes.
	 * @throws IOException Throws an IOException if an I/O error occurs when opening the directory where the functions are located.
	 */
	private static void calcTreeFunctions(String name) throws IOException {
		// Datapack folder functions -> Assuming datapack and functions are created before
		final Datapack datapack = DatapackManager.getInstance().getOrCreate(name);
		final Path fPath = DatapackManager.DATAPACK_FOLDER.resolve(name).resolve("data").resolve(name).resolve("functions");

		final Comparator<File> functionComp = (f1, f2) -> {
			// Name looker
			final String f1Name = f1.getName();
			final String f2Name = f2.getName();

			// functions which are leaves are prioritized
			int sameType = Character.compare(f2Name.charAt(0), f1Name.charAt(0));
			if (sameType != 0) return sameType;

			// calculate first and last scores
			final var tuple1 = getTickArea(f1Name);
			final var tuple2 = getTickArea(f2Name);

			// functions with lower first scores are prioritized
			final int sameStart = Integer.compare(tuple1.getKey(), tuple2.getKey());
			if (sameStart != 0) return sameStart;

			// functions with smaller distance are prioritized
			return Integer.compare(tuple1.getValue() - tuple1.getKey(), tuple2.getValue() - tuple2.getKey());
		};

		// add leave, Minecraft functions into first currentFloor
		List<File> currentFloor;
		try (Stream<Path> dirContent = Files.list(fPath)) {
			final List<File> rootFunctions = dirContent
					.map(Path::toFile)
					.filter(file -> file.isFile() && file.getName().endsWith(".mcfunction"))
					.sorted(functionComp)
					.toList();

			currentFloor = new ArrayList<>(rootFunctions);
		}

		// process floor by floor
		while (!currentFloor.isEmpty()){
			// process root floor and break loop
			if (currentFloor.size() == 1){
				// Calculate file before and after renaming
				File function = currentFloor.remove(0);
				var tickArea = getTickArea(function);
				final File renameFunction = datapack.getPath().resolve("data").resolve(name).resolve("functions")
						.resolve("play_" + tickArea.getKey() + "_" + tickArea.getValue() + ".mcfunction").toFile();

				// Renaming and store if it was successfully
				boolean succ = function.renameTo(renameFunction);

				// update function reference to new file
				if (!succ) CommandBlockHelper.LOGGER.log(Level.WARNING, "Cannot rename function!");
				else {
					function = renameFunction;
				}

				// add scoreboard command to play function
				datapack.putCommandsIntoFunc(name, function.getName().replace(".mcfunction", ""),
						"scoreboard players add " + name + " musik 1");

				// currentFloor is now empty -> loop will end
				continue;
			}

			// Constructing next floor
			final List<File> nextFloor = new ArrayList<>();

			// Stack CHILD_AMOUNT functions together and creates a selection function. Repeat until only one function is left.
			while (!currentFloor.isEmpty()) {
				// Stack functions together
				final List<File> fStacker = new ArrayList<>();
				for (int i = 0; i < CHILD_AMOUNT && !currentFloor.isEmpty(); i++) {
					fStacker.add(currentFloor.remove(0));
				}

				// Information for selection function which are calculated below
				final StringBuilder commands = new StringBuilder();
				int fScore = Integer.MAX_VALUE, lScore = Integer.MIN_VALUE;

				for (File func : fStacker.stream().toList()) {
					final String fName = func.getName();

					// calculate score interval selector function is operating on
					final var tuple = getTickArea(fName);

					final int fileFScore = tuple.getKey();
					final int fileLScore = tuple.getValue();
					if (fileFScore < fScore) fScore = fileFScore;
					if (fileLScore > lScore) lScore = fileLScore;

					// add selector command into selection function
					commands.append("execute if score ").append(name).append(" musik matches ").append(fileFScore).append("..")
							.append(fileLScore).append(" run function ").append(name).append(":").append(fName.replace(".mcfunction", "")).append("\n");
				}
				nextFloor.add(datapack.putCommandsIntoFunc(name, "kmusik_" + fScore + "_" + lScore, commands.toString()).toFile());
			}
			// sort set current to next Floor
			nextFloor.sort(functionComp);
			currentFloor = nextFloor;
		}
	}

	/**
	 * Calculate tick area from given file which has tick area encoded in its name.
	 * @param function File which has tick area encoded in file name. <i>(Format of name: < name >_< first Tick >_< last Tick >[.mcfunction])</i>
	 * @return Returns a tuple (first Tick, last Tick).
	 */
	private static HomogenTuple<Integer> getTickArea(File function){
		return getTickArea(function.getName());
	}

	/**
	 * Calculate tick area from given file name which has tick area encoded in its name.
	 * @param fName File name which has tick area encoded in. <i>(Format: < name >_< first Tick >_< last Tick >[.mcfunction])</i>
	 * @return Returns a tuple (first Tick, last Tick).
	 */
	private static HomogenTuple<Integer> getTickArea(String fName){
		final Scanner scanner = new Scanner(fName.replace("_", " ").replace(".mcfunction", ""));
		scanner.next();

		// calculate score interval selector function is operating on
		return new HomogenTuple<>(scanner.nextInt(), scanner.nextInt());
	}
}