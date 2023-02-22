package com.github.militalex.util;

import lombok.Getter;
import lombok.Setter;

import java.util.function.Consumer;
import java.util.stream.Stream;

public class Triple<F, S, T> {
	@Getter @Setter private F first;
	@Getter @Setter private S second;
	@Getter @Setter private T third;

	public Triple(F first, S second, T third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}

	public boolean contains(Object obj){
		return obj.equals(first) || obj.equals(second) || obj.equals(third);
	}

	public boolean isEmpty(){
		return first == null && second == null && third == null;
	}

	public void clear(){
		first = null;
		second = null;
		third = null;
	}

	public Stream<Object> stream(){
		return Stream.of(first, second, third);
	}

	public void forEach(Consumer<Object> consumer){
		stream().forEach(consumer);
	}
}
