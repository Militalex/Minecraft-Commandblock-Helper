package com.github.militalex.util.tickables;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * This class offers functionalities to tick delayed, periodic and (a)synchronous on non-null Objects inside a collection, by wrappers a given collection.
 * This class does not start automatically to tick, but it will stop if no object is anymore in collection.
 *
 * @param <T> Type of objects in wrapped collection.
 *
 * @author Militalex
 * @version 2.0
 */
public abstract class CollectionTickable<T> extends Tickable{

	/**
	 * Collection to tick on.
	 */
	@NotNull private final Collection<@NotNull T> collection;

	/**
	 * @param plugin The Plugin the tickable is working for. This is needed by the BukkitScheduler.
	 * @param collection Collection to tick on.
	 */
	public CollectionTickable(@NotNull Plugin plugin, @NotNull Collection<T> collection) {
		super(plugin);
		this.collection = collection;
	}

	// Normal collection actions

	/**
	 * (See {@link Collection#add(Object)} for more information)
	 * @param value Value to add to collection.
	 * @return Returns {@code true} if this collection changed as a result of the call.
	 */
	public boolean add(@NotNull T value){
		return collection.add(value);
	}

	/**
	 * (See {@link Collection#addAll(Collection)} for more information)
	 * @param collection Collection where all contents should be added to collection.
	 * @return Returns {@code true} if this collection changed as a result of the call.
	 */
	public boolean addAll(@NotNull Collection<T> collection){
		return this.collection.addAll(collection);
	}

	/**
	 * (See {@link Collection#contains(Object)} for more information)
	 * @param value Value to check if it is inside collection.
	 * @return Returns true if {@code value} is inside collection.
	 */
	public boolean contains(@NotNull T value){
		return collection.contains(value);
	}

	/**
	 * (See {@link Collection#remove(Object)} for more information)
	 * @param value Value to remove from collection.
	 * @return Returns {@code true} if an element was removed as a result of this call.
	 */
	public boolean remove(@NotNull T value){
		return collection.remove(value);
	}

	/**
	 * (See {@link Collection#removeAll(Collection)} for more information)
	 * @param collection  Collection where all contents should be removed from collection.
	 * @return Returns {@code true} if this collection changed as a result of the call.
	 */
	public boolean removeAll(@NotNull Collection<T> collection){
		return this.collection.removeAll(collection);
	}

	/**
	 * Removes all the elements from this collection. The collection will be empty after this method returns and
	 * tickable will stop running if collection is still empty on next execution.
	 */
	public void clear(){
		collection.clear();
	}

	// Tickable stuff

	/**
	 * Ensures collection is not empty before running the actual task. If so the tickable will be canceled.
	 * This method calls the {@link CollectionTickable#runOn(T value)} in a way, so that it makes it possible
	 * to remove {@code value} from collection without any problems.
	 */
	@Override
	protected final void run() {
		if (collection.isEmpty()) cancel();
		else {
			final Set<T> set = new HashSet<>(collection);
			set.forEach(this::runOn);
		}
	}

	/**
	 * Task which is periodically done for every object {@code value} in collection.
	 * @param value Current value which is going to be processed.
	 */
	protected abstract void runOn(@NotNull T value);
}
