package com.github.pfichtner.durationformatter;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Predicates.equalTo;
import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Iterables.indexOf;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

/**
 * A Formatter for durations. This class is threadsafe. Instances can be created
 * using the {@link Builder} class.
 * 
 * @author Peter Fichtner
 */
@ThreadSafe
public class DurationFormatter {

	/**
	 * Helper class to create {@link DurationFormatter}s. This class is
	 * threadsafe so each method call will return a new instance!
	 * 
	 * @author Peter Fichtner
	 */
	@ThreadSafe
	public static class Builder implements Cloneable {

		private static final Builder BASE = new Builder().minimum(SECONDS)
				.maximum(HOURS).format(TimeUnit.MICROSECONDS, "%03d")
				.format(TimeUnit.MICROSECONDS, "%03d")
				.format(TimeUnit.NANOSECONDS, "%03d");

		/**
		 * Default instance that formats seconds to hours using digits (e.g.
		 * <code>01:12:33</code>)
		 */
		public static final Builder DIGITS = BASE;

		/**
		 * Default instance that formats seconds to hours using digits (e.g.
		 * <code>01:12:33</code>)
		 */
		public static final Builder SYMBOLS = BASE.separator(" ").format("%d")
				.symbol(NANOSECONDS, "ns").symbol(MICROSECONDS, "µs")
				.symbol(MILLISECONDS, "ms").symbol(SECONDS, "s")
				.symbol(MINUTES, "min").symbol(HOURS, "h").symbol(DAYS, "d");

		private String separator = ":";
		private TimeUnit minimum = MILLISECONDS;
		private TimeUnit maximum = HOURS;
		private boolean round = true;
		private boolean stripLeadingZeros;
		private HashMap<TimeUnit, String> formats = Maps.newHashMap();
		private HashMap<TimeUnit, String> symbols = Maps.newHashMap();

		public DurationFormatter build() {
			return new DurationFormatter(this);
		}

		public Builder maximum(TimeUnit maximum) {
			Builder clone = clone();
			clone.maximum = maximum;
			return clone;
		}

		public Builder minimum(TimeUnit minimum) {
			Builder clone = clone();
			clone.minimum = minimum;
			return clone;
		}

		public Builder round(boolean round) {
			Builder clone = clone();
			clone.round = round;
			return clone;
		}

		public Builder separator(String separator) {
			Builder clone = clone();
			clone.separator = separator;
			return clone;
		}

		public Builder format(String format) {
			Builder clone = clone();
			for (TimeUnit timeUnit : timeUnits) {
				clone = clone.format(timeUnit, format);
			}
			return clone;
		}

		public Builder format(TimeUnit timeUnit, String format) {
			Builder clone = clone();
			clone.formats.put(timeUnit, format);
			return clone;
		}

		public Builder symbol(TimeUnit timeUnit, String symbol) {
			Builder clone = clone();
			clone.symbols.put(timeUnit, symbol);
			return clone;
		}

		public Builder stripLeadingZeros(boolean stripLeadingZeros) {
			Builder clone = clone();
			clone.stripLeadingZeros = stripLeadingZeros;
			return clone;
		}

		@Override
		protected Builder clone() {
			try {
				Builder clone = (Builder) super.clone();
				clone.formats = Maps.newHashMap(this.formats);
				clone.symbols = Maps.newHashMap(this.symbols);
				return clone;
			} catch (CloneNotSupportedException e) {
				throw Throwables.propagate(e);
			}
		}

	}

	private static final List<TimeUnit> timeUnits = ImmutableList
			.copyOf(Ordering.natural().reverse()
					.sortedCopy(Arrays.asList(TimeUnit.values())));
	private static final TimeUnit highestPrecision = getLast(timeUnits);

	public static final DurationFormatter DIGITS = Builder.DIGITS.build();;

	public static final DurationFormatter SYMBOLS = Builder.SYMBOLS.build();

	private static final Integer ZERO = Integer.valueOf(0);

	private static final String DEFAULT_FORMAT = "%02d";

	private static String floor(long d, long n) {
		return Integer.toString((int) Math.floor((double) d / n));
	}

	private final boolean round;
	private final List<TimeUnit> usedTimeUnits;
	private final String separator;
	private int idxMin;
	private TimeUnit timeUnitMin;
	private Map<TimeUnit, String> formats;
	private Map<TimeUnit, String> symbols;
	private boolean stripLeadingZeros;

	public DurationFormatter(Builder builder) {
		this.round = builder.round;
		checkState(builder.minimum.compareTo(builder.maximum) <= 0);
		this.idxMin = indexOf(timeUnits, equalTo(builder.minimum));
		int idxMax = indexOf(timeUnits, equalTo(builder.maximum));
		checkState(this.idxMin > idxMax, "min must not be greater than max");
		this.timeUnitMin = timeUnits.get(this.idxMin);
		this.usedTimeUnits = timeUnits.subList(idxMax, this.idxMin + 1);
		this.separator = builder.separator;
		this.stripLeadingZeros = builder.stripLeadingZeros;
		this.formats = ImmutableMap.copyOf(builder.formats);
		this.symbols = ImmutableMap.copyOf(builder.symbols);
	}

	/**
	 * Format the passed milliseconds to the format specified.
	 * 
	 * @param value
	 *            the millis to format
	 * @return String containing the duration
	 * @see #format(long, TimeUnit)
	 */
	public String formatMillis(long value) {
		return format(value, MILLISECONDS);
	}

	/**
	 * Format the passed duration to the format specified.
	 * 
	 * @param value
	 *            the duration to format
	 * @return String containing the duration
	 */
	public String format(long value, TimeUnit timeUnit) {
		long nanos = NANOSECONDS.convert(value, timeUnit);
		List<String> values = getValues(this.round
				&& !highestPrecision.equals(this.timeUnitMin) ? calculateRounded(nanos)
				: nanos);
		return Joiner.on(this.separator).join(values);
	}

	private long calculateRounded(long value) {
		TimeUnit smaller = timeUnits.get(this.idxMin + 1);
		long add = smaller.convert(1, this.timeUnitMin) / 2;
		return value + NANOSECONDS.convert(add, smaller);
	}

	private List<String> getValues(long delta) {
		List<String> strings = Lists.newArrayList();
		long actual = delta;

		TimeUnit last = getLast(this.usedTimeUnits);
		boolean added = false;
		for (TimeUnit timeUnit : this.usedTimeUnits) {
			long longVal = timeUnit.toNanos(1);
			String format = this.formats.get(timeUnit);
			boolean fits = actual >= longVal;
			if (added || fits || !this.stripLeadingZeros
					|| timeUnit.equals(last)) {
				String value = String.format(format == null ? DEFAULT_FORMAT
						: format,
						fits ? Integer.valueOf(floor(actual, longVal)) : ZERO);
				String symbol = this.symbols.get(timeUnit);
				strings.add(symbol == null ? value : value + symbol);
				added = true;
			}
			actual %= longVal;
		}
		return strings;
	}

}
