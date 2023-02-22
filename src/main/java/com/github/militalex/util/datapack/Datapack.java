package com.github.militalex.util.datapack;


import com.github.militalex.util.CommandExecutor;
import com.github.militalex.util.FileUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * This class represents an abstract datapack with currently limited functions, because this Plugin currently need more.
 *
 * @author Militalex
 * @version 1.0
 */
public class Datapack {

	/**
	 * Main directory of Datapack.
	 */
	private final Path path;

	/**
	 * Name of Datapack, which have to be similar than Datapacks directory name.
	 */
	private final String name;

	/**
	 * Will creates a new Datapack in Datapack folder eith given {@code name}.
	 * @param name Name of new created Datapack.
	 */
	Datapack(String name){
		this.name = name;
		this.path = DatapackManager.DATAPACK_FOLDER.resolve(name);
		try {
			Files.createDirectory(path);

			Files.writeString(path.resolve("pack.mcmeta"), """
				{
				    "pack": {
				        "description": "Data pack for resources provided by Bukkit plugins",
				        "pack_format": 9
				    }
				}
				""", StandardOpenOption.CREATE);
		} catch (IOException e) {
			throw new IllegalArgumentException("Datapack creation of " + name + " failed!", e);
		}

	}

	/**
	 * Constructs already existent Datapack.
	 * @param path Main directory of Datapack.
	 */
	Datapack(Path path){
		this.path = path;
		this.name = path.toFile().getName();
	}

	/**
	 * Creates a new mcfunction in Datapack with given {@code nameSpace} and {@code fname} containing given commands.
	 * To use the function the server Datapacks should be reloaded.
	 * @param fname Name of mcfunction.
	 * @param nameSpace	Namespace of function (namespace:fname).
	 * @param cmds Commands which should be added line by line into function. <i>(You can also include comments.)</i>
	 */
	public void createFunction(String nameSpace, String fname, String... cmds){
		final Path funcPath = path.resolve("data").resolve(nameSpace).resolve("functions").resolve(fname + ".mcfunction");

		if (!FileUtil.createIfNotExists(funcPath)) throw new IllegalArgumentException(funcPath + " cannot created!");

		for (String cmd : cmds) {
			try {
				Files.writeString(funcPath, cmd + "\n", StandardOpenOption.APPEND);
			} catch (IOException e) {
				throw new IllegalArgumentException(cmd + " cannot written to file!", e);
			}
		}
	}

	/**
	 * Enables datapack with "/datapack enable" command.
	 */
	public void enable(){
		// TODO testing
		CommandExecutor.executeCommand("/datapack enable " + name);
	}

	/**
	 * Disabled datapack with "/datapack disable" command.
	 */
	public void disable(){
		// TODO testing
		CommandExecutor.executeCommand("/datapack disable " + name);
	}
}
