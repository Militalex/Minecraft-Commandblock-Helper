package com.github.militalex.util;

import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * This class offers methods to easily execute commands.
 *
 * @author Militalex
 * @version 1.0
 */
public class CommandExecutor {

	/**
	 * @param entity entity which should be addressed via selector.
	 * @return Returns selector String that only address given {@code entity}.
	 */
	public static String toSelector(@NotNull Entity entity){
		final NBTContainer uuid = new NBTContainer("{}");
		uuid.setUUID("UUID", new NBTEntity(entity).getUUID("UUID"));

		return "@e[nbt=" + uuid + ",limit=1]";
	}

	/**
	 * Executes {@code cmd} as normal command.
	 * @param cmd command that should be executed. It can start with "/".
	 */
	public static void executeCommand(@NotNull String cmd){
		if (cmd.charAt(0) == '/'){
			cmd = cmd.substring(1);
		}
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
	}

	/**
	 * Executes given {@code cmd} as given {@code entity} and at given {@code entity}. <br>
	 * More specifically, the following is stated: {@code execute as <entity> at @s run <cmd>} <br>
	 * Here the Server will be the CommandSender.
	 * @param entity Entity that should execute the command.
	 * @param cmd command that should be executed. It can start with "/".
	 */
	public static void executeCommandAs(@NotNull Entity entity, @NotNull String cmd){
		if (cmd.charAt(0) == '/'){
			cmd = cmd.substring(1);
		}
		if (entity instanceof Player){
			Bukkit.dispatchCommand(entity, cmd);
		}
		else {
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as " + toSelector(entity) + " at @s run " + cmd);
		}
	}

	/**
	 * Executes given {@code cmd} as given {@code entity} and at given {@code entity} but with given {@code player} as CommandSender. <br>
	 * More specifically, the following is stated: {@code execute as <entity> at @s run <cmd>} <br>
	 * Here the given {@code player} will be the CommandSender.
	 * @param entity Entity that should execute the command.
	 * @param cmd command that should be executed. It can start with "/".
	 */
	public static void executeCommandAs(@NotNull Player player, @NotNull Entity entity, @NotNull String cmd){
		if (cmd.charAt(0) == '/'){
			cmd = cmd.substring(1);
		}
		Bukkit.dispatchCommand(player, "execute as " + toSelector(entity) + " at @s run " + cmd);
	}
}
