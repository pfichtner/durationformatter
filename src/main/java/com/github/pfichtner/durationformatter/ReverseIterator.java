package com.github.pfichtner.durationformatter;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ReverseIterator<T> implements Iterator<T> {

	private final ListIterator<T> delegate;

	public ReverseIterator(List<T> list) {
		this(list.listIterator(list.size()));
	}

	public ReverseIterator(ListIterator<T> delegate) {
		this.delegate = delegate;
	}

	public boolean hasNext() {
		return this.delegate.hasPrevious();
	}

	public T next() {
		return this.delegate.previous();
	}

	public void remove() {
		this.delegate.remove();
	}

}
