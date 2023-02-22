package com.github.militalex.util;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTTileEntity;
import de.tr7zw.nbtapi.NBTType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.help.HelpTopic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Alexander Ley
 * @version 1.3
 *
 * This class includes some Utils used by commands.
 *
 */
public class CommandUtil {

    /**
     * List of all available commands
     */
    public static final List<String> COMMANDS = Bukkit.getServer().getHelpMap().getHelpTopics()
            .stream()
            .map(HelpTopic::getName)
            .collect(Collectors.toList());

    // [Debug]
    //public static final List<String> COMMANDS = null;

    /**
     * @return Returns the coordinates of the Block the Player is looking on.
     */
    public static Location getPlayerLookBlockPos(Player player){
        return player.getTargetBlock(Arrays.stream(Material.values())
                                             .filter(material -> !material.isBlock() || material.isAir() || !material.isSolid())
                                             .collect(Collectors.toSet()), 10).getLocation();
    }

    /**
     * Align from- and to Location to xyz coordinates
     */
    public static HomogenTuple<Location> transformLocation(Location from, Location to){
        final Location newFrom = new Location(from.getWorld(), Math.min(from.getBlockX(), to.getBlockX()), Math.min(from.getBlockY(), to.getBlockY()), Math.min(from.getBlockZ(), to.getBlockZ()));
        final Location newToo = new Location(from.getWorld(), Math.max(from.getBlockX(), to.getBlockX()), Math.max(from.getBlockY(), to.getBlockY()), Math.max(from.getBlockZ(), to.getBlockZ()));
        return new HomogenTuple<>(newFrom, newToo);
    }

    /**
     * @return Returns world from sender.
     */
    public static World getWorldFromSender(CommandSender sender){
        if (sender instanceof ConsoleCommandSender) throw new IllegalArgumentException("Cannot get World from Console.");

        if (sender instanceof BlockCommandSender) return ((BlockCommandSender) sender).getBlock().getWorld();
        return ((Player)sender).getWorld();
    }

    /**
     * Changes command in command Block.
     */
    public static void setCMDinBlock(Block commandBlock, String newCommand){
        if (!(commandBlock.getState() instanceof CommandBlock)) throw new IllegalArgumentException(commandBlock + " is not an Command Block");

        NBTTileEntity nbtTileEntity = new NBTTileEntity(commandBlock.getState());
        nbtTileEntity.setString("Command", newCommand);
    }

    /**
     * @return Returns HashaMap with all blockStates of a block (e.g. facing -> south)
     */
    public static HashMap<String, String> getBlockStates(BlockState state){
        String blockString = state.getBlockData().getAsString();
        blockString = blockString.substring(blockString.indexOf("[") + 1, blockString.lastIndexOf("]"));
        final List<String> stateList = Arrays.stream(blockString.split(",")).toList();

        final HashMap<String, String> states = new HashMap<>();

        stateList.forEach(s -> {
            final String[] data = s.split("=");
            states.put(data[0], data[1]);
        });

        return states;
    }

    /**
     * Collapse argument strings and put it together and returns it as command.
     */
    public static String getCommand(@NotNull String label, @NotNull String[] args){
        final StringBuilder argString = new StringBuilder("/" + label);
        for (String arg : args){
            argString.append(" ").append(arg);
        }

        return argString.toString();
    }

    /**
     * This method extracts an integer from a location command argument, that can be either a ~, ~x (e.g. ~3) or a plain number.
     * @param sender Sender integer. It can be x, y or z.
     * @param posTilde Location tile command argument.
     * @return Returns either plain number or relative to sender, when "~ - values" were given.
     */
    public static int getCordFromTildeOrNumber(int sender, String posTilde){
        if (NumberUtil.isNumber(posTilde)) return Integer.parseInt(posTilde);
        if (!posTilde.startsWith("~")) throw new IllegalArgumentException(posTilde + " is no valid argument.");
        posTilde = posTilde.substring(1);

        if (posTilde.equals("")) return sender;
        if (!NumberUtil.isInteger(posTilde)) throw new IllegalArgumentException(posTilde + " is no valid argument.");

        sender += Integer.parseInt(posTilde);
        return sender;

    }

    /**
     * Copy all NBTCompounds from source to target
     */
    public static void copyNBTData(NBTCompound source, NBTCompound target){
        source.getKeys().forEach(s -> {
            final NBTType type = source.getType(s);
            switch (type){
                case NBTTagByte -> target.setByte(s, source.getByte(s));
                case NBTTagInt -> target.setInteger(s, source.getInteger(s));
                case NBTTagByteArray -> target.setByteArray(s, source.getByteArray(s));
                case NBTTagLong -> target.setLong(s, source.getLong(s));
                case NBTTagFloat -> target.setFloat(s, source.getFloat(s));
                case NBTTagShort -> target.setShort(s, source.getShort(s));
                case NBTTagDouble -> target.setDouble(s, source.getDouble(s));
                case NBTTagString -> target.setString(s, source.getString(s));
                case NBTTagIntArray -> target.setIntArray(s, source.getIntArray(s));
                default -> {
                    try {
                        target.removeKey(s);
                    }
                    catch (Exception ignored){ }
                    target.addCompound(s);
                    target.getCompound(s).mergeCompound(source.getCompound(s));
                }
            }
        });
    }

    /**
     * @return Returns if command in commandblock is a playsound command (Supports execute commands too).
     */
    public static boolean isCommandWithPlaysound(CommandSender sender, CommandBlock block){
        //Get Command String
        final String cmd = block.getCommand();

        //Split Command into arguments
        List<String> cmdArgs = Arrays.asList(cmd.split(" "));

        if (cmdArgs.get(0).equals("")){
            return false;
        }

        //Make compatible with execute
        if (cmdArgs.get(0).equals("execute") || cmdArgs.get(0).equals("/execute")) {

            if (!cmdArgs.contains("run")) {
                sender.sendMessage(ChatColor.RED + "Execute command is not complete. Missing \"run\" argument.");
                return false;
            }

            cmdArgs = new ArrayList<>(cmdArgs);

            //Removes execute command from cmdArgs
            for (int i = 0; i < cmdArgs.size(); ) {
                //Append until "run"
                if (cmdArgs.get(i).equals("run")) {
                    cmdArgs.remove(i);
                    break;
                }
                cmdArgs.remove(i);
            }
        }

        //Removes /
        if (cmdArgs.get(0).charAt(0) == '/') {
            cmdArgs.set(0, cmdArgs.get(0).replaceFirst("/", ""));
        }

        return cmdArgs.get(0).equals("playsound");
    }

    /**
     * @return Return volume and Pitch of a commandblock as Tuple, where
     * "key" is volume and "value" is pitch.
     */
    public static @NotNull HomogenTuple<Double> extractVolumePitch(CommandBlock block){
        final HomogenTuple<Double> volpitch = new HomogenTuple<>(1.0, 1.0);

        //Get Command String
        final String cmd = block.getCommand();

        //Split Command into arguments
        List<String> cmdArgs = Arrays.asList(cmd.split(" "));

        if (cmdArgs.get(0).equals("")){
            return volpitch;
        }

        //Make compatible with execute
        if (cmdArgs.get(0).equals("execute") || cmdArgs.get(0).equals("/execute")) {

            if (!cmdArgs.contains("run")) {
                return volpitch;
            }

            cmdArgs = new ArrayList<>(cmdArgs);

            //Removes execute command from cmdArgs
            for (int i = 0; i < cmdArgs.size(); ) {
                //Append until "run"
                if (cmdArgs.get(i).equals("run")) {
                    cmdArgs.remove(i);
                    break;
                }
                cmdArgs.remove(i);
            }
        }

        //Removes /
        if (cmdArgs.get(0).charAt(0) == '/') {
            cmdArgs.set(0, cmdArgs.get(0).replaceFirst("/", ""));
        }

        try {
            if (cmdArgs.get(0).equals("playsound")) {
                if (cmdArgs.size() >= 8) {
                    //Modify the command
                    volpitch.setKey(Double.valueOf(cmdArgs.get(7)));
                }

                if (cmdArgs.size() >= 9) {
                    //Modify the command
                    volpitch.setValue(Double.valueOf(cmdArgs.get(8)));
                }
            }
        }
        catch (Exception ignored){}

        return volpitch;
    }

    /**
     * Change volume of a playsound command.
     */
    @Nullable
    public static Boolean changeVolume(CommandSender sender, CommandBlock block, double newValue) {
        //Get Command String
        final String cmd = block.getCommand();

        //Split Command into arguments
        List<String> cmdArgs = Arrays.asList(cmd.split(" "));

        if (cmdArgs.get(0).equals("")){
            return null;
        }

        //Make compatible with execute
        final StringBuilder executePrepend = new StringBuilder();
        if (cmdArgs.get(0).equals("execute") || cmdArgs.get(0).equals("/execute")) {

            if (!cmdArgs.contains("run")) {
                sender.sendMessage(ChatColor.RED + "Execute command is not complete. Missing \"run\" argument.");
                return false;
            }

            cmdArgs = new ArrayList<>(cmdArgs);

            //Removes execute command from cmdArgs and store it into executePrepend
            for (int i = 0; i < cmdArgs.size(); ) {
                //Append until "run"
                if (cmdArgs.get(i).equals("run")) {
                    executePrepend.append(cmdArgs.get(i)).append(" ");
                    cmdArgs.remove(i);
                    break;
                }
                executePrepend.append(cmdArgs.get(i)).append(" ");
                cmdArgs.remove(i);
            }
        }

        //Removes temporarily / (only from playsound commands) and saves that / was removed
        boolean haveSlash = false;
        if (cmdArgs.get(0).charAt(0) == '/') {
            cmdArgs.set(0, cmdArgs.get(0).replaceFirst("/", ""));
            haveSlash = true;
        }

        if (cmdArgs.get(0).equals("playsound") && cmdArgs.size() > 7) {
            //Modify the command
            cmdArgs.set(7, "" + newValue);

            StringBuilder builder = new StringBuilder();
            //Prepend execute command
            builder.append(executePrepend);
            //Puts "/" back to the front
            if (haveSlash) builder.append("/");

            //Reconstructs the playsound command
            for (int i = 0; i < cmdArgs.size(); i++) {
                builder.append(cmdArgs.get(i));
                if (i != cmdArgs.size() - 1) builder.append(" ");
            }

            //Sets new command to command Block.
            CommandUtil.setCMDinBlock(block.getBlock(), builder.toString());
            return true;
        }
        return null;
    }

    /**
     * Converts amount of noteblock clicks into pitch for commandblock playsound
     */
    public static double getPitchFromClicks(int clicks){
        return switch (clicks){
          case 0 -> 0.5;
          case 1 -> 0.53;
          case 2 -> 0.56;
          case 3 -> 0.6;
          case 4 -> 0.63;
          case 5 -> 0.67;
          case 6 -> 0.7;
          case 7 -> 0.75;
          case 8 -> 0.8;
          case 9 -> 0.85;
          case 10 -> 0.9;
          case 11 -> 0.95;
          case 12 -> 1.0;
          case 13 -> 1.05;
          case 14 -> 1.1;
          case 15 -> 1.2;
          case 16 -> 1.25;
          case 17 -> 1.32;
          case 18 -> 1.4;
          case 19 -> 1.5;
          case 20 -> 1.6;
          case 21 -> 1.7;
          case 22 -> 1.8;
          case 23 -> 1.9;
          case 24 -> 2.0;
          default -> throw new IllegalArgumentException(clicks + " is not valid for noteblocks/playsound.");
        };
    }

    /**
     * Converts String of block/material name into instrument.
     */
    public static String getInstrumentFromBlock(String block){
        return switch (block){
          case "Wood" -> "bass";
          case "Sand" -> "snare";
          case "Glass" -> "hat";
          case "Stone" -> "basedrum";
          case "Gold" -> "bell";
          case "Clay" -> "flute";
          case "PackedIce" -> "chime";
          case "Wool" -> "guitar";
          case "BoneBlock" -> "xylophone";
          case "Iron" -> "iron_xylophone";
          case "SoulSand" -> "cow_bell";
          case "Pumpkin" -> "didgeridoo";
          case "Emerald" -> "bit";
          case "Hay" -> "banjo";
          case "Glowstone" -> "pling";
          case "Air" -> "harp";
          default -> throw new IllegalArgumentException(block + " is not a valid instrument.");
        };
    }

    /**
     * Converts actual note to noteblock clicks.
     */
    public static int getClickFromNote(String note){
        return switch (note) {
            case "F1#", "G1b", "0" -> 0;
            case "G1", "1" -> 1;
            case "G1#", "A1b", "2" -> 2;
            case "A1", "3" -> 3;
            case "A1#", "B1", "4" -> 4;
            case "H1", "5" -> 5;
            case "C1", "6" -> 6;
            case "C1#", "D1b", "7" -> 7;
            case "D1", "8" -> 8;
            case "D1#", "E1b", "9" -> 9;
            case "E1", "10" -> 10;
            case "F2", "11" -> 11;
            case "F2#", "G2b", "12" -> 12;
            case "G2", "13" -> 13;
            case "G2#", "A2b", "14" -> 14;
            case "A2", "15" -> 15;
            case "A2#", "B2", "16" -> 16;
            case "H2", "17" -> 17;
            case "C2", "18" -> 18;
            case "C2#", "D2b", "19" -> 19;
            case "D2", "20" -> 20;
            case "D2#", "E2b", "21" -> 21;
            case "E2", "22" -> 22;
            case "F3", "23" -> 23;
            case "F3#", "G3b", "24" -> 24;
            default -> throw new IllegalArgumentException(note + " is not valid.");
        };
    }

    /**
     * Converts Note to pitch.
     */
    public static String getNoteFromPitch(double pitch){
        return switch ("" + pitch){
            case "0.5" -> "F1#";
            case "0.53" -> "G1";
            case "0.56" -> "G1#";
            case "0.6" -> "A1";
            case "0.63" -> "A1#";
            case "0.67" -> "H1";
            case "0.7" -> "C1";
            case "0.75" -> "C1#";
            case "0.8" -> "D1";
            case "0.85" -> "D1#";
            case "0.9" ->  "E1";
            case "0.95" -> "F2";
            case "1.0" -> "F2#";
            case "1.05" -> "G2";
            case "1.1" -> "G2#";
            case "1.2" -> "A2";
            case "1.25" -> "A2#";
            case "1.32" -> "H2";
            case "1.4" -> "C2";
            case "1.5" -> "C2#";
            case "1.6" -> "D2";
            case "1.7" -> "D2#";
            case "1.8" -> "E2";
            case "1.9" -> "F3";
            case "2.0" -> "F3#";
            default -> "";
        };
    }
}
