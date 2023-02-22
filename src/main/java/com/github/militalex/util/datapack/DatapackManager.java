package com.github.militalex.util.datapack;

import com.github.militalex.main.ServerPropertiesManager;
import org.bukkit.Bukkit;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class is a simple abstract interface for working with Datapacks. Currently, the amount functions are low, because
 * this Plugin does not need so many functions for managing Datapacks, but offers the opportunity for me to add more functions if needed.
 * <br><b>
 * Currently, the class only supports non-zipped Datapacks!
 * </b><br>
 * @author Militalex
 * @version 1.0
 */
public class DatapackManager {
	// Singleton Pattern
	private static DatapackManager manager;

	/**
	 * @return Returns distinct DatapackManager (Singleton Pattern).
	 */
	public static DatapackManager getInstance() {
		if (manager == null) manager = new DatapackManager();
		return manager;
	}
	// Singleton Pattern End

	/**
	 * Folder to Datapacks.
	 */
	public static final Path DATAPACK_FOLDER = Paths.get(ServerPropertiesManager.getInstance().getProperty("level-name") + "/datapacks");

	private DatapackManager(){ }

	public boolean exists(String name){
		return DATAPACK_FOLDER.resolve(name).toFile().exists();
	}

	/**
	 * If {@code name} corresponds to a Datapack, this will be returned. Otherwise, the method will create a new one
	 * with the given name, by creating a Datapack folder.
	 * @param name Name of existent/created Datapack.
	 * @return Returns a Datapack corresponding to given name.
	 */
	public Datapack getOrCreate(String name){
		final Path path = DATAPACK_FOLDER.resolve(name);

		if (path.toFile().exists()) return new Datapack(path);
		else return new Datapack(name);
	}

	/**
	 * Reloads Datapacks similar to the known command /reload.
	 * The method has the difference that it only reloads Datapacks not the whole Server itself.
	 */
	public void reload(){
		Bukkit.getServer().reloadData();
	}
}
