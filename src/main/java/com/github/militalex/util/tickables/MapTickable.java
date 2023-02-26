package com.github.militalex.util.tickables;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * This class offers functionalities to tick delayed, periodic and (a)synchronous on non-null Objects inside a map, by wrappers a given map.
 * This class does not start automatically to tick, but it will stop if no object is anymore in map.
 *
 * @param <K> Type of key objects.
 * @param <V> Type of value objects.
 *
 * @author Militalex
 * @version 2.0
 */
public abstract class MapTickable<K, V> extends Tickable{

	/**
	 * Map to tick on.
	 */
	@NotNull private final Map<@NotNull K, @NotNull V> map;

	/**
	 * Initializes a new Tickable for given plugin.
	 *
	 * @param plugin The Plugin the tickable is working for. This is needed by the BukkitScheduler.
	 */
	protected MapTickable(@NotNull Plugin plugin, @NotNull Map<K, V> map) {
		super(plugin);
		this.map = map;
	}

	// Normal map actions

	/**
	 * (See {@link Map#put(K,V)}} for more information)
	 * @param key Key with which the specified value is to be associated.
	 * @param value Value to be associated with the specified key.
	 * @return Returns the previous value associated with key, or null if there was no mapping for key.
	 */
	public @Nullable V put(@NotNull K key, @NotNull V value){
		return map.put(key, value);
	}

	/**
	 * (See {@link Map#putAll(Map)}} for more information)
	 * @param map Mappings that will be stored in map.
	 */
	public void putAll(@NotNull Map<? extends K, ? extends V> map){
		this.map.putAll(map);
	}

	/**
	 * (See {@link Map#remove(K)}} for more information)
	 * @param key Key whose mapping is to be removed from the map.
	 * @return Returns the previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}.
	 */
	public @Nullable V remove(@NotNull K key){
		return map.remove(key);
	}

	/**
	 * (See {@link Map#remove(K,V)}} for more information)
	 * @param key Key with which the specified value is associated.
	 * @param value Value expected to be associated with the specified key.
	 * @return Returns true if the value was removed
	 */
	public boolean remove(@NotNull K key, @NotNull V value){
		return map.remove(key, value);
	}

	/**
	 * Removes all the mappings from map. The map will be empty after this call returns and
	 * tickable will stop running if collection is still empty on next execution.
	 */
	public void clear(){
		map.clear();
	}

	// Tickable stuff

	/**
	 * Ensures collection is not empty before running the actual task. If so the tickable will be canceled.
	 * This method calls the {@link MapTickable#runOn(K key, V value)} in a way, so that it makes it possible
	 * to remove {@code value} from collection without any problems.
	 */
	@Override
	protected final void run() {
		if (map.isEmpty()) cancel();
		else {
			final Set<Map.Entry<K, V>> entrySet = map.entrySet();
			entrySet.forEach(kvEntry -> runOn(kvEntry.getKey(), kvEntry.getValue()));
		}
	}

	/**
	 * Task which is periodically done for every object {@code value} in collection.
	 * @param value Current value which is going to be processed.
	 */
	protected abstract void runOn(@NotNull K key, @NotNull V value);
}
