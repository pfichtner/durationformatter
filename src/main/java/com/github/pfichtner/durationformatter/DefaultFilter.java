package com.github.pfichtner.durationformatter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.github.pfichtner.durationformatter.DurationFormatter.SuppressZeros;

public class DefaultFilter implements Filter {

	public static class StartStop {
		private int start, stop;

		public StartStop(int start, int stop) {
			this.start = start;
			this.stop = stop;
		}

		public int getStart() {
			return start;
		}

		public int getStop() {
			return stop;
		}
	}

	private static final Integer ZERO = Integer.valueOf(0);

	private final Set<SuppressZeros> settings;

	public DefaultFilter(Set<SuppressZeros> settings) {
		this.settings = settings;
	}

	public Map<TimeUnit, Integer> filter(LinkedHashMap<TimeUnit, Integer> map) {
		return repack(map, fix(calc(map.entrySet()), map));
	}

	private StartStop fix(StartStop startStop,
			LinkedHashMap<TimeUnit, Integer> map) {
		boolean stripTrailing = settings.contains(SuppressZeros.TRAILING);
		boolean stripLeading = settings.contains(SuppressZeros.LEADING);
		int last = map.size();
		if (startStop.start < 0 || startStop.stop == startStop.start) {
			// non non-zero value found
			if (stripLeading) {
				startStop.start = last - 1;
				startStop.stop = last;
			} else if (stripTrailing) {
				startStop.start = 0;
				startStop.stop = 1;
			}
		}

		if (!stripLeading) {
			startStop.start = 0;
		}
		if (!stripTrailing) {
			startStop.stop = last;
		}

		return startStop;
	}

	private LinkedHashMap<TimeUnit, Integer> repack(
			LinkedHashMap<TimeUnit, Integer> map, StartStop startStop) {
		return startStop.start == 0 && startStop.stop == map.size() ? map
				: subMap(map, startStop.start, startStop.stop);
	}

	private static LinkedHashMap<TimeUnit, Integer> subMap(
			LinkedHashMap<TimeUnit, Integer> map, int start, int stop) {
		LinkedHashMap<TimeUnit, Integer> result = new LinkedHashMap<TimeUnit, Integer>(
				map.size(), 1f);
		for (Entry<TimeUnit, Integer> entry : new ArrayList<Entry<TimeUnit, Integer>>(
				map.entrySet()).subList(start, stop)) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	private static <T, S> StartStop calc(Collection<Entry<T, S>> entries) {
		int firstNonNull = -1;
		int lastNonNull = -1;
		boolean gotNonNull = false;
		int cnt = 0;
		for (Entry<T, S> entry : entries) {
			boolean isZero = ZERO.equals(entry.getValue());
			if (!isZero) {
				if (!gotNonNull) {
					firstNonNull = cnt;
				}
				lastNonNull = cnt + 1;
			}
			gotNonNull = gotNonNull || !isZero;
			cnt++;
		}
		return new StartStop(firstNonNull, lastNonNull);
	}

}
