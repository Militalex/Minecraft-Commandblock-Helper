package com.github.militalex.main;

import com.github.militalex.commands.MusicScanCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

/**
 * This Plugin enables several helpful features to command block developers. Currently only one command exists, but
 * this plugin will become more functionalities like advanced data commands and so on.
 *
 * @author Militalex
 * @version 1.0
 */
public final class CommandBlockHelper extends JavaPlugin {

	/**
	 * Logger of plugin which should not be overridden.
	 */
	public static Logger LOGGER;

	/**
	 * Reference to this plugin which should also not be overridden.
	 */
	public static CommandBlockHelper PLUGIN;

	@Override
	public void onEnable() {
		// Init Staff
		PLUGIN = this;
		LOGGER = getLogger();

		// Register commands
		MusicScanCommand.register();

		this.getLogger().info("--------- Command Block Helper successfully enabled. ---------");
	}

	@Override
	public void onDisable() {
		this.getLogger().info("--------- Command Block Helper successfully disabled. ---------");
	}
}
