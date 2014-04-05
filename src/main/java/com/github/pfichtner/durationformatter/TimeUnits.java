package com.github.pfichtner.durationformatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

// TODO Reduce to one reference and inline
class TimeUnits {

	private TimeUnits() {
		super();
	}

	public static final List<TimeUnit> timeUnits = Collections
			.unmodifiableList(orderingNatural(Arrays.asList(TimeUnit.values())));

	private static <T extends Comparable<T>> List<T> orderingNatural(List<T> ts) {
		List<T> result = new ArrayList<T>(ts);
		Collections.sort(result, Collections.reverseOrder());
		return result;
	}

}
