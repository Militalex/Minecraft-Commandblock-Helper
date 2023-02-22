package com.github.militalex.main;

import com.github.militalex.commands.MusicScanCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class CommandBlockHelper extends JavaPlugin {

	public static Logger LOGGER;
	public static CommandBlockHelper PLUGIN;

	@Override
	public void onEnable() {
		PLUGIN = this;
		LOGGER = getLogger();

		MusicScanCommand.register();
		this.getLogger().info("--------- Command Block Helper successfully enabled. ---------");
	}

	@Override
	public void onDisable() {
		this.getLogger().info("--------- Command Block Helper successfully disabled. ---------");
	}
}
