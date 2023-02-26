package com.github.militalex.commands;

import com.github.militalex.main.CommandBlockHelper;
import com.github.militalex.util.CommandUtil;
import com.github.militalex.util.Tuple;
import com.github.militalex.util.datapack.Datapack;
import com.github.militalex.util.datapack.DatapackManager;
import com.github.militalex.util.tickables.CollectionTickable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.CommandBlock;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * This class is used by /savemusic to run an asynchronous latitude search from given redstone location. This class will collect
 * sound effects (Note blocks not yet) and stores them into functions in datapack.
 *
 * @author Militalex
 * @version 1.0
 */
public final class MusicScanTickable extends CollectionTickable<Object[]> {

	/**
	 * Defines how many score/delays should be contained in a function.
	 */
	public static final int MAX_SCORE_PER_FUNCTION = 20;

	/**
	 * Contains all sounds to distinguish between minecraft sounds and custom sounds
	 */
	private static final List<String> SOUNDS = Arrays.stream(Sound.values()).map(sound -> sound.getKey().toString()).toList();

	/**
	 * Name of datapack which will be created.
	 */
	private final String name;

	/**
	 * Code that should be executed after scanning.
	 */
	private final Consumer<Void> endConsumer;

	/**
	 * @param name Name of datapack which will be created.
	 * @param endConsumer Code that should be executed after scanning.
	 */
	MusicScanTickable(@NotNull String name, @NotNull Consumer<Void> endConsumer) {
		super(CommandBlockHelper.PLUGIN, new ArrayDeque<>());
		this.name = name;
		this.endConsumer = endConsumer;
	}

	/**
	 * Overrides cancel behaviour to flush rest in buffer nd executes endConsumer.
	 */
	@Override
	public void cancel() {
		super.cancel();
		// Saves rest in BufferList into datapack functions
		flushAll(name);

		// Executes something in the end
		endConsumer.accept(null);
	}

	/**
	 * Performs latitude search.
	 * @param data Current value which is going to be processed.
	 */
	@Override
	protected void runOn(Object @NotNull [] data) {
		// Remove from queue and extract data
		remove(data);
		final Location curLoc = (Location) data[0];
		final int score = (int) data[1];
		final int sLength = (int) data[2];
		final boolean propagate = (boolean) data[3];	// Defines if natural block should propagate processing

		// Material depend behavior decision
		boolean delFlag = false;	// Defines if processed block should be replaced with air
		final Material curMat = curLoc.getBlock().getType();
		if (curMat.isAir()) return;	// Skip air

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
		else if (curMat == Material.COMMAND_BLOCK || curMat == Material.REPEATING_COMMAND_BLOCK || curMat == Material.CHAIN_COMMAND_BLOCK) {
			// Extract CmdBlock and Command
			final CommandBlock commandBlock = (CommandBlock) curLoc.getBlock().getState();
			String command = commandBlock.getCommand();
			if (command.startsWith("/")) command = command.substring(1);

			// Process commandblock in general
			processCommandblock(commandBlock, score, sLength);

			// Linphator Music-Slider
			if ((command.startsWith("setblock") && (command.contains("redstone_block") || command.contains("air"))) || command.contains("clone"))
				processMusicSlider(curLoc, score, command);

				// finally playsound command handler
			else if (command.contains("playsound")) processPlaySoundCommandBlock(name, score, sLength, command, curMat == Material.REPEATING_COMMAND_BLOCK);

			delFlag = true;
		}

		// TODO: Implement Noteblock support

		// Delete block if flag is set
		if (delFlag){
			curLoc.getBlock().setType(Material.AIR, false);
		}
	}

	/**
	 * Adds a new location to process in queue if it is not air
	 * @param newLoc New Location which should be processed.
	 * @param score Score/Delay of next location.
	 * @param sLength Current length of signal.
	 * @param propagate Defines if block should trigger more processing.
	 */
	public void addToQueue(@NotNull Location newLoc, int score, int sLength, boolean propagate){
		if (newLoc.getBlock().getType().isAir()) return;
		add(new Object[]{newLoc, score, sLength, propagate});
	}

	/**
	 * Adds locations to queue which are direct surrounded by given location, without diagonal connection.
	 *
	 * @param curLoc Location where surrounding is added.
	 * @param score  Score/Delay of next locations.
	 */
	private void addSurrounding(@NotNull Location curLoc, int score) {
		addToQueue(curLoc.clone().add(-1, 0, 0), score, 1, false);
		addToQueue(curLoc.clone().add(1, 0, 0), score, 1, false);
		addToQueue(curLoc.clone().add(0, 1, 0), score, 1, false);
		addToQueue(curLoc.clone().add(0, -1, 0), score, 1, false);
		addToQueue(curLoc.clone().add(0, 0, 1), score, 1, false);
		addToQueue(curLoc.clone().add(0, 0, -1), score, 1, false);
	}

	// Block processing

	private void processNaturalBlock(@NotNull Location curLoc, int score, int signalLength){
		if (!curLoc.getBlock().getType().isBlock() || !curLoc.getBlock().getType().isSolid())
			throw new IllegalArgumentException("Only natural solid redstone electrified " +
					"blocks are processed here, nothing else. But it was " + curLoc.getBlock().getType());

		// Simulate Piston signal shortener
		if (curLoc.clone().subtract(0, 1, 0).getBlock().getType() == Material.STICKY_PISTON){
			final var states = CommandUtil.getBlockStates(curLoc.clone().subtract(0, 1, 0).getBlock().getState());
			if (states.get("facing").equals("up")) signalLength = 1;
		}

		// Complex for-loop enable iterating only over direct block neighbors without diagonal neighbours
		for (int dx = -1; dx <= 1; dx ++){
			for (int dy = -1; dy <= 1; dy ++){
				if (dx!= 0 && Math.abs(dx) == Math.abs(dy)) continue;
				for (int dz = -1; dz <= 1; dz ++){
					if (dx == 0 && dy == 0 && dz == 0) continue;
					if (dx!= 0 && Math.abs(dx) == Math.abs(dz)) continue;
					if (dx == 0 && dy!= 0 && (Math.abs(dy) == Math.abs(dz))) continue;

					final Location newLoc = curLoc.clone().add(dx, dy, dz);

					// Chain cmd blocks are disabled because they can only triggerd by commandblocks
					if (newLoc.getBlock().getType() != Material.CHAIN_COMMAND_BLOCK) {
						addToQueue(newLoc, score, signalLength, false);
					}
				}
			}
		}
	}

	public void processRedstoneWire(Location curLoc, int score, int signalLength){
		if (curLoc.getBlock().getType() != Material.REDSTONE_WIRE) throw new IllegalArgumentException("Redstone wire is processed here, nothing else.");

		// Get BlockStates
		final HashMap<String, String> redstoneState = CommandUtil.getBlockStates(curLoc.getBlock().getState());

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

	public void processRepeater(Location curLoc, int score, int signalLength){
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

	public void processCommandblock(CommandBlock block, int score, int sLength){
		final HashMap<String,String> blockStates = CommandUtil.getBlockStates(block);
		final String facing = blockStates.get("facing");

		// calculate direction step depending on side
		int addX = facing.equals("east") ? 1 : facing.equals("west") ? -1 : 0;
		int addY = facing.equals("up") ? 1 : facing.equals("down") ? -1 : 0;
		int addZ = facing.equals("south") ? 1 : facing.equals("north") ? -1 : 0;

		// Chain cmd blocks are triggered
		final Location newLoc = block.getLocation().clone().add(addX, addY, addZ);
		if (newLoc.getBlock().getType() == Material.CHAIN_COMMAND_BLOCK){
			addToQueue(newLoc, score, sLength, false);
		}
	}

	public void processMusicSlider(Location curLoc, int score, String command){
		if ((!command.startsWith("setblock") || (!command.contains("redstone_block") && !command.contains("air"))) && !command.startsWith("clone"))
			throw new IllegalArgumentException(command + " is not part of music slider.");

		// Extract position from command
		int x, y, z;
		final Scanner scanner = new Scanner(command);
		scanner.next();
		if (command.startsWith("clone")) { scanner.next(); scanner.next(); scanner.next(); scanner.next(); scanner.next(); scanner.next(); }
		x = CommandUtil.getCordFromTildeOrNumber(curLoc.getBlockX(),  scanner.next());
		y = CommandUtil.getCordFromTildeOrNumber(curLoc.getBlockY(),  scanner.next());
		z = CommandUtil.getCordFromTildeOrNumber(curLoc.getBlockZ(),  scanner.next());
		final Location newLoc = new Location(curLoc.getWorld(), x, y, z);

		// Add Surrounding (inclusive clone underneath)
		addSurrounding(newLoc, score + 1);
	}

	public void processPlaySoundCommandBlock(String name, int score, int sLength, String command, boolean isRepeating){
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
		final String soundEffect = scanner.next().replace("minecraft:", "");
		modCmdBuilder.append(soundEffect).append(" ");

		// Extract sound fader and set it to record or voice
		scanner.next();
		if (SOUNDS.contains("minecraft:" + soundEffect) || soundEffect.startsWith("ins_")
				|| soundEffect.startsWith("fx_") || soundEffect.startsWith("klang_")) modCmdBuilder.append("record ");
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

	// Datapack stuff

	/**
	 * Reference to datapack corresponding.
	 */
	private Datapack datapack;

	/**
	 * Add given command to datapack, by buffering it and flushing it later when at least a function can be flushed.
	 */
	private void addToDatapack(String name, int score, String command){
		// Error when no datapack was created and it already exists
		if (datapack == null && DatapackManager.getInstance().exists(name)) {
			throw new IllegalArgumentException("Datapack " + name + " already exists. It is not possible to modify existent Datapack.");
		}
		// Creating new datapack
		if (datapack == null) {
			datapack = DatapackManager.getInstance().getOrCreate(name);
		}

		// Buffers command, method below flush's if needed.
		addToBuffer(name, command, score);
	}

	// Buffer functions

	/**
	 * Buffers a certain number of commands, sorts them by scores and enables function-wise flushing.
	 */
	private final PriorityQueue<Tuple<Integer,String>> BUFFER_LIST = new PriorityQueue<>(Comparator.comparingInt(Tuple::getKey));

	/**
	 * Counts amount of times buffer has flushed to determine point in time buffer should be flushed.
	 */
	private int fFlushPtr = 0;

	/**
	 * Add given command to buffer and flushes it automatically if enough commands are stored in buffer.
	 */
	private void addToBuffer(String name, String command, int score){
		// Sets flush pointer to first if buffer was empty
		if (BUFFER_LIST.isEmpty()) fFlushPtr = score;

		// Add command to buffer
		BUFFER_LIST.add(new Tuple<>(score, command));

		// flushes three functions if commands of six functions are buffered
		if (BUFFER_LIST.element().getKey() > fFlushPtr + MAX_SCORE_PER_FUNCTION * 6) {
			flushFunction(name);
			flushFunction(name);
			flushFunction(name);

			// Sets flush pointer to first if not empty
			if (!BUFFER_LIST.isEmpty()){
				fFlushPtr = BUFFER_LIST.element().getKey();
			}
		}
	}

	/**
	 * flushes complete buffer
	 */
	public void flushAll(String name){
		while (!BUFFER_LIST.isEmpty()){
			flushFunction(name);
		}
	}

	/**
	 * Flushes only commands corresponding to a function from buffer.
	 */
	public void flushFunction(String name){
		final StringBuilder fBuilder = new StringBuilder();

		// Determines intervall of scores function covers
		final int fScore = BUFFER_LIST.element().getKey();	// first added score -> does not change anymore
		int lScore = BUFFER_LIST.element().getKey();		// last added score  -> will be calculated below
		int localMax = fScore + MAX_SCORE_PER_FUNCTION;		// relative maximum score

		// Adds commands to the builder until the function has added all commands with interval [fscore,localMax] to the function.
		while (!BUFFER_LIST.isEmpty() && BUFFER_LIST.element().getKey() < localMax) {
			final var tuple = BUFFER_LIST.remove();
			lScore = tuple.getKey();
			final String cmd = tuple.getValue();

			// modify command and make it an execute if score command with scores stored in tuples
			fBuilder.append("execute if score ").append(name).append(" musik matches ").append(lScore).append(" ").append(cmd).append("\n");
		}

		// Create function
		assert datapack != null;
		if (!fBuilder.toString().equals("")){
			datapack.putCommandsIntoFunc(name, "musik_" + fScore + "_" + lScore, fBuilder.toString());
		}
	}
}
