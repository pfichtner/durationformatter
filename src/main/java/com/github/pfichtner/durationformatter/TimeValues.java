package com.github.pfichtner.durationformatter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.github.pfichtner.durationformatter.TimeValues.Bucket;

public class TimeValues implements Iterable<Bucket> {

	private final Bucket[] buckets = initialize();

	private static final List<TimeUnit> timeUnits = TimeUnits.timeUnits;

	private static final Map<TimeUnit, Long> maxValues = TimeUnits.maxValues;

	public static class Bucket {

		// the next greater Bucket (TimeUnit), previous because hh:mm:ss: ...
		private final Bucket previous;
		private final TimeUnit timeUnit;
		private final long maxValue;
		private long value;
		private boolean visible = true;

		Bucket(Bucket previous, TimeUnit timeUnit, long maxValue) {
			this.previous = previous;
			this.timeUnit = timeUnit;
			this.maxValue = maxValue;
		}

		private void addToValue(long toadd) {
			long newValue = toadd + this.value;
			setValue(newValue % maxValue);
			long rest = newValue - this.value;
			if (rest > 0 && previous != null) {
				// overflow
				previous.addToValue(previous.timeUnit.convert(rest, timeUnit));
			}
		}

		void pushLeftRounded() {
			long half = this.maxValue / 2;
			if (this.value + half >= maxValue) {
				addToValue(half);
			} else {
				setValue(0);
			}
		}

		void pollFromLeft() {
			if (previous != null) {
				previous.pollFromLeft();
				setValue(getValue()
						+ timeUnit.convert(previous.getValue(),
								previous.getTimeUnit()));
				previous.setValue(0);
			}
		}

		private void setValue(long value) {
			this.value = value;
		}

		public long getValue() {
			return value;
		}

		public TimeUnit getTimeUnit() {
			return timeUnit;
		}

		public boolean isVisible() {
			return visible;
		}

		public void setVisible(boolean visible) {
			this.visible = visible;
		}

		@Override
		public String toString() {
			return "Bucket [timeUnit=" + timeUnit + ", value=" + value
					+ ", visible=" + visible + "]";
		}

	}

	public TimeValues() {
		super();
	}

	public TimeValues(long value, TimeUnit timeUnit) {
		getBucket(timeUnit).addToValue(value);
	}

	public TimeValues add(long l, TimeUnit timeUnit) {
		getBucket(timeUnit).addToValue(l);
		return this;
	}

	public TimeValues pushLeftRounded(TimeUnit timeUnit) {
		getBucket(timeUnit).pushLeftRounded();
		return this;
	}

	public TimeValues pollFromLeft(TimeUnit timeUnit) {
		getBucket(timeUnit).pollFromLeft();
		return this;
	}

	public Bucket getBucket(TimeUnit timeUnit) {
		return buckets[bucketIdx(timeUnit)];
	}

	private int bucketIdx(TimeUnit timeUnit) {
		return timeUnits.indexOf(timeUnit);
	}

	private static Bucket[] initialize() {
		Bucket[] buckets = new Bucket[timeUnits.size()];
		Bucket previous = null;
		for (int i = 0; i < timeUnits.size(); i++) {
			TimeUnit timeUnit = timeUnits.get(i);
			buckets[i] = new Bucket(previous, timeUnit, maxValues.get(timeUnit));
			previous = buckets[i];
		}
		return buckets;
	}

	public Iterator<Bucket> iterator() {
		return iterable().iterator();
	}

	private List<Bucket> iterable() {
		return Arrays.asList(this.buckets);
	}

	public Iterator<Bucket> reverseIterator() {
		return new ReverseIterator<Bucket>(iterable().listIterator(size()));
	}

	public Iterable<Bucket> sequence(TimeUnit timeUnit1, TimeUnit timeUnit2) {
		final int startIdx = bucketIdx(timeUnit1);
		final int endIdx = bucketIdx(timeUnit2);
		return startIdx <= endIdx ? iterable().subList(startIdx, endIdx)
				: new ReverseIterable<Bucket>(iterable().subList(endIdx + 1,
						startIdx + 1));
	}

	public Iterable<Bucket> sequenceInclude(TimeUnit timeUnit1,
			TimeUnit timeUnit2) {
		final int startIdx = bucketIdx(timeUnit1);
		final int endIdx = bucketIdx(timeUnit2);
		return startIdx <= endIdx ? iterable().subList(startIdx, endIdx + 1)
				: new ReverseIterable<Bucket>(iterable().subList(endIdx,
						startIdx + 1));
	}

	public int size() {
		return this.buckets.length;
	}

	@Override
	public String toString() {
		return "Buckets [buckets=" + Arrays.toString(buckets) + "]";
	}

}
