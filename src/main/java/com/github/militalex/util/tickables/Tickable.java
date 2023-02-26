package com.github.militalex.util.tickables;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class is used to execute delayed, periodic and (a)synchronous tasks.
 *
 * @author Militalex
 * @version 2.0
 */
public abstract class Tickable {

	/**
	 * The Plugin the tickable is working for. This is needed by the BukkitScheduler.
	 */
	private final Plugin plugin;

	/**
	 * The task the BukkitScheduler delivers when creating the task. If null no task is currently running.
	 */
	@Nullable private BukkitTask task;

	/**
	 * Initializes a new Tickable for given plugin.
	 * @param plugin The Plugin the tickable is working for. This is needed by the BukkitScheduler.
	 */
	protected Tickable(@NotNull Plugin plugin) {
		this.plugin = plugin;
	}

	/**
	 * Starts tickable task to run if it is not already started.
	 * @param delay Amount of ticks task should be delayed.
	 * @param period Period the task is repeated. Period will be ignored if you set {@code synchron} to false.
	 * @param synchron Defines if task is handled asynchronous or synchronous.
	 */
	public void start(long delay, long period, boolean synchron){
		if (task != null) throw new IllegalStateException("Task cannot started again!");

		if (synchron) task = Bukkit.getScheduler().runTaskTimer(plugin, this::run, delay, period);
		else task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, this::run, delay);
	}

	/**
	 * @return Returns if task have been started is running.
	 */
	public boolean isRunning(){
		return task != null && !task.isCancelled();
	}

	/**
	 * Defines task that should be run periodically.
	 */
	protected abstract void run();

	/**
	 * Will attempt to cancel the task if is cancelable.
	 */
	public void cancel(){
		if (task == null) throw new IllegalStateException("Task cannot canceled without starting");
		task.cancel();
		task = null;
	}
}
