package com.github.pfichtner.durationformatter;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.TimeUnit;

class TimeValueAdder {

	private long nanos;

	static TimeValueAdder get(int i, TimeUnit timeUnit) {
		return new TimeValueAdder().and(i, timeUnit);
	}

	TimeValueAdder and(int i, TimeUnit timeUnit) {
		this.nanos += timeUnit.toNanos(i);
		return this;
	}

	long as(TimeUnit timeUnit) {
		return timeUnit.convert(this.nanos, NANOSECONDS);
	}

}