package com.github.pfichtner.durationformatter;

import java.util.Iterator;
import java.util.List;

public class ReverseIterable<T> implements Iterable<T> {

	private List<T> list;

	public ReverseIterable(List<T> list) {
		this.list = list;
	}

	public Iterator<T> iterator() {
		return new ReverseIterator<T>(this.list);
	}

}
