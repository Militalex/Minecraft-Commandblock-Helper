package com.github.militalex.util;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author Alexander Ley
 * @version 1.3
 *
 * This Class simulates a 2-Tuple.
 * @param <K> First Type
 * @param <V> Second Type
 */
public class Tuple<K, V> implements Map<K, V> {
    private K key;
    private V value;

    public Tuple(){
    }

    /**
     * Creates a new Tuple.
     * @param key First Element
     * @param value Second Element
     */
    public Tuple(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Creates a new tuple from another tuple (shallow copy).
     */
    public Tuple(Tuple<K, V> tuple){
        this(tuple.key, tuple.value);
    }

    /**
     * @return Returns the First Element (Key).
     */
    public K getKey() {
        return key;
    }

    /**
     *  Set Element 1 (Key).
     */
    public void setKey(K key) {
        this.key = key;
    }

    /**
     * @return Returns the Second Element (Value).
     */
    public V getValue() {
        return value;
    }

    /**
     *  Set Element 2. (Value)
     */
    public void setValue(V value) {
        this.value = value;
    }

    /**
     * Checks if tuple contains obj.
     */
    public boolean contains(Object obj){
        return key.equals(obj) || value.equals(obj);
    }

    /**
     * @return Returns true if at least one element is null.
     */
    public boolean containsNull(){
        return size() < 2;
    }

    /**
     * @return Returns 0 if empty, 1 if one element is null and 2 if no element is null.
     */
    @Override
    public int size() {
        return isEmpty() ? 0 : (key != null && value != null) ? 2 : 1;
    }

    /**
     *  Checks if tuple is empty.
     */
    @Override
    public boolean isEmpty() {
        return key == null && value == null;
    }

    /**
     *  Checks if @param key is equal to first element (Key).
     */
    @Override
    public boolean containsKey(Object key) {
        return this.key.equals(key);
    }

    /**
     *  Checks if @param value is equal to first element (Value).
     */
    @Override
    public boolean containsValue(Object value) {
        return this.value.equals(value);
    }

    /**
     * @return Returns second element (Value).
     */
    @Override
    public V get(Object key) {
        return getValue();
    }

    /**
     *  Sets first and second element (Key and Value).
     * @return Returns the last value.
     */
    @Override
    public V put(K key, V value) {
        final V val = this.value;
        setKey(key);
        setValue(value);
        return val;
    }

    /**
     *  Clears tuple (Sets all Elements to null).
     * @return Returns the last value.
     */
    @Override
    public V remove(Object key) {
        final V val = this.value;
        setKey(null);
        setValue(null);
        return val;
    }

    /**
     *  Sets tuple values to the last element in m.forEach(..).
     */
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    /**
     * Clears tuple (Sets all Elements to null).
     */
    @Override
    public void clear() {
        setKey(null);
        setValue(null);
    }

    /**
     * @return Returns a single item set with key.
     */
    @Override
    public @NotNull Set<K> keySet() {
       return Collections.singleton(key);
    }

    /**
     * @return Returns a single item list with value.
     */
    @Override
    public @NotNull Collection<V> values() {
        return Collections.singletonList(value);
    }

    /**
     * @return Returns a single item set with Entry<K, V>.
     */
    @Override
    public @NotNull Set<Entry<K, V>> entrySet() {
        return Collections.singleton(new AbstractMap.SimpleEntry<>(key, value));
    }

    /**
     * @return Returns tuple as string.
     */
    @Override
    public String toString() {
        return "Tuple{" +
                "key=" + key +
                ", value=" + value +
                '}';
    }

    /**
     * Checks if two tuples are equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple<?, ?> tuple = (Tuple<?, ?>) o;
        return Objects.equals(key, tuple.key) && Objects.equals(value, tuple.value);
    }

    /**
     * @return Returns a hash depending on key and value.
     */
    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }
}
