package com.github.pfichtner.durationformatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// TODO Reduce to one reference and inline
class TimeUnits {

	private TimeUnits() {
		super();
	}

	public static final List<TimeUnit> timeUnits = Collections
			.unmodifiableList(orderingNatural(Arrays.asList(TimeUnit.values())));

	public static final Map<TimeUnit, Long> maxValues = Collections
			.unmodifiableMap(maxValuesFor(timeUnits));

	private static Map<TimeUnit, Long> maxValuesFor(List<TimeUnit> list) {
		Map<TimeUnit, Long> maxValues = new HashMap<TimeUnit, Long>(list.size());
		TimeUnit previous = null;
		for (TimeUnit timeUnit : list) {
			Long maxValue = Long.valueOf((previous == null ? Long.MAX_VALUE
					: timeUnit.convert(1, previous)));
			maxValues.put(timeUnit, maxValue);
			previous = timeUnit;
		}
		return maxValues;
	}

	private static <T extends Comparable<T>> List<T> orderingNatural(List<T> ts) {
		List<T> result = new ArrayList<T>(ts);
		Collections.sort(result, Collections.reverseOrder());
		return result;
	}

}
