package com.github.pfichtner.durationformatter;

import static com.google.common.base.Preconditions.checkState;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.Throwables;

/**
 * A Formatter for durations. All implementing classes have to been threadsafe.
 * Instances can be created using the {@link Builder} class.
 * 
 * @author Peter Fichtner
 */
@ThreadSafe
public interface DurationFormatter {

	public enum SuppressZeros {
		LEADING, TRAILING, MIDDLE
	}

	/**
	 * Default instance, for format-string see {@link Builder#DIGITS}.
	 */
	DurationFormatter DIGITS = Builder.DIGITS.build();;

	/**
	 * Default instance, for format-string see {@link Builder#SYMBOLS}.
	 */
	DurationFormatter SYMBOLS = Builder.SYMBOLS.build();

	/**
	 * Format the passed milliseconds to the format specified.
	 * 
	 * @param value
	 *            the millis to format
	 * @return String containing the duration
	 * @see #format(long, TimeUnit)
	 */
	String formatMillis(long value);

	/**
	 * Format the passed duration to the format specified.
	 * 
	 * @param value
	 *            the duration to format
	 * @return String containing the duration
	 */
	String format(long value, TimeUnit timeUnit);

	/**
	 * Helper class to create {@link DefaultDurationFormatter}s. This class is
	 * threadsafe so each method call will return a new instance of the Builder!
	 * 
	 * @author Peter Fichtner
	 */
	@ThreadSafe
	public static class Builder implements Cloneable {

		private static final List<TimeUnit> timeUnits = Collections
				.unmodifiableList(new ArrayList<TimeUnit>(
						orderingNaturalReverse(Arrays.asList(TimeUnit.values()))));

		private static final EnumSet<SuppressZeros> DEFAULT_SUPPRESS_MODE = EnumSet
				.noneOf(SuppressZeros.class);

		/**
		 * A Formatter for durations. This class is threadsafe. Instances can be
		 * created using the {@link Builder} class.
		 * 
		 * @author Peter Fichtner
		 */
		@ThreadSafe
		private static class DefaultDurationFormatter implements
				DurationFormatter {

			private final TimeUnit highestPrecision = getLast(timeUnits);

			private static final Integer ZERO = Integer.valueOf(0);

			private static final String DEFAULT_FORMAT = "%02d";

			private final boolean round;
			private final List<TimeUnit> usedTimeUnits;
			private final String separator;
			private final String valueSymbolSeparator;
			private final boolean suppressLeading;
			private final boolean suppressTrailing;
			private final boolean suppressMiddle;
			private final int idxMin;
			private final TimeUnit timeUnitMin;
			private final Map<TimeUnit, String> formats;
			private final Map<TimeUnit, String> symbols;

			public DefaultDurationFormatter(Builder builder) {
				this.round = builder.round;
				checkState(builder.minimum.compareTo(builder.maximum) <= 0);
				this.idxMin = indexOf(timeUnits, builder.minimum);
				int idxMax = indexOf(timeUnits, builder.maximum);
				checkState(this.idxMin > idxMax,
						"min must not be greater than max");
				this.timeUnitMin = timeUnits.get(this.idxMin);
				this.usedTimeUnits = timeUnits.subList(idxMax, this.idxMin + 1);
				this.separator = builder.separator;
				this.valueSymbolSeparator = builder.valueSymbolSeparator;
				this.suppressLeading = builder.suppressZeros
						.contains(SuppressZeros.LEADING);
				this.suppressTrailing = builder.suppressZeros
						.contains(SuppressZeros.TRAILING);
				this.suppressMiddle = builder.suppressZeros
						.contains(SuppressZeros.MIDDLE);
				this.formats = Collections
						.unmodifiableMap(new HashMap<TimeUnit, String>(
								builder.formats));
				this.symbols = Collections
						.unmodifiableMap(new HashMap<TimeUnit, String>(
								builder.symbols));
			}

			private static String floor(long d, long n) {
				return Integer.toString((int) Math.floor((double) d / n));
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
			 * @param longVal
			 *            the duration to format
			 * @return String containing the duration
			 */
			public String format(long longVal, TimeUnit srcTu) {
				long nanos = NANOSECONDS.convert(longVal, srcTu);

				StringBuilder sb = new StringBuilder();
				LinkedHashMap<TimeUnit, Integer> values = getValues((this.round
						&& !highestPrecision.equals(this.timeUnitMin) ? calculateRounded(nanos)
						: nanos));

				Set<Entry<TimeUnit, Integer>> entrySet = values.entrySet();

				// TODO only call when needed
				int firstNonNull = findFirstNonNull(entrySet);
				int lastNonNull = findLastNonNull(entrySet);
				boolean allNull = firstNonNull - lastNonNull == entrySet.size();

				int idx = 0;
				for (Iterator<Entry<TimeUnit, Integer>> iterator = entrySet
						.iterator(); iterator.hasNext();) {
					Entry<TimeUnit, Integer> entry = iterator.next();
					boolean isZero = ZERO.equals(entry.getValue());

					boolean suppA = isZero && this.suppressLeading
							&& idx < firstNonNull
							&& !(allNull && idx == entrySet.size() - 1);
					boolean suppB = isZero && this.suppressTrailing
							&& idx > lastNonNull & !(allNull && idx == 0);
					boolean suppC = isZero && this.suppressMiddle
							&& idx > firstNonNull && idx < lastNonNull;
					boolean filter = suppA || suppB || suppC;
					if (!filter) {
						sb.append(
								getValueString(entry.getValue(), entry.getKey()))
								.append(this.separator);
					}
					idx++;
				}
				return sb.length() == 0 ? "" : sb.delete(
						sb.length() - this.separator.length(), sb.length())
						.toString();
			}

			private int findFirstNonNull(Set<Entry<TimeUnit, Integer>> entrySet) {
				int idx = 0;
				for (Entry<TimeUnit, Integer> entry : entrySet) {
					if (!isZero(entry.getValue())) {
						return idx;
					}
					idx++;
				}
				return entrySet.size();
			}

			private int findLastNonNull(Set<Entry<TimeUnit, Integer>> entrySet) {
				int idx = 0, result = 0;
				for (Entry<TimeUnit, Integer> entry : entrySet) {
					if (!isZero(entry.getValue())) {
						result = idx;
					}
					idx++;
				}
				return result;
			}

			private static boolean isZero(Integer v) {
				return ZERO.equals(v);
			}

			private String getValueString(Integer value, TimeUnit timeUnit) {
				String format = this.formats.get(timeUnit);
				String symbol = this.symbols.get(timeUnit);
				String stringVal = String.format(
						format == null ? DEFAULT_FORMAT : format, value);
				return symbol == null ? stringVal : stringVal
						+ this.valueSymbolSeparator + symbol;
			}

			private long calculateRounded(long value) {
				TimeUnit smaller = timeUnits.get(this.idxMin + 1);
				long add = smaller.convert(1, this.timeUnitMin) / 2;
				return value + NANOSECONDS.convert(add, smaller);
			}

			private LinkedHashMap<TimeUnit, Integer> getValues(long lonVal) {
				LinkedHashMap<TimeUnit, Integer> strings = new LinkedHashMap<TimeUnit, Integer>(
						this.usedTimeUnits.size(), 1f);
				long actual = lonVal;
				for (TimeUnit timeUnit : this.usedTimeUnits) {
					long longVal = timeUnit.toNanos(1);
					strings.put(
							timeUnit,
							actual >= longVal ? Integer.valueOf(floor(actual,
									longVal)) : ZERO);
					actual %= longVal;
				}
				return strings;
			}

		}

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
				.symbol(NANOSECONDS, "ns").symbol(MICROSECONDS, "μs")
				.symbol(MILLISECONDS, "ms").symbol(SECONDS, "s")
				.symbol(MINUTES, "min").symbol(HOURS, "h").symbol(DAYS, "d");

		private String separator = ":";
		private String valueSymbolSeparator = "";
		private TimeUnit minimum = MILLISECONDS;
		private TimeUnit maximum = HOURS;
		private boolean round = true;
		private Set<SuppressZeros> suppressZeros = DEFAULT_SUPPRESS_MODE;
		private Map<TimeUnit, String> formats = new HashMap<TimeUnit, String>();
		private Map<TimeUnit, String> symbols = new HashMap<TimeUnit, String>();

		public DefaultDurationFormatter build() {
			return new DefaultDurationFormatter(this);
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

		/**
		 * Sets the minimum and maximum to the passed TimeUnit by calling
		 * {@link #minimum(TimeUnit)} and {@link #maximum(TimeUnit)},
		 * 
		 * @param timeUnit
		 *            TimeUnit to use for minimum and maximum
		 * @return new Builder instance
		 */
		public Builder useOnly(TimeUnit timeUnit) {
			return minimum(timeUnit).maximum(timeUnit);
		}

		public Builder round(boolean round) {
			Builder clone = clone();
			clone.round = round;
			return clone;
		}

		/**
		 * Sets the separator between the "value timeunit" pairs
		 * 
		 * @param separator
		 *            separator to use
		 * @return new Builder instance
		 */
		public Builder separator(String separator) {
			Builder clone = clone();
			clone.separator = separator;
			return clone;
		}

		/**
		 * Sets the separator between the value and timeunit
		 * 
		 * @param separator
		 *            separator to use
		 * @return new Builder instance
		 */
		public Builder valueSymbolSeparator(String valueSymbolSeparator) {
			Builder clone = clone();
			clone.valueSymbolSeparator = valueSymbolSeparator;
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

		public Builder suppressZeros(SuppressZeros suppressZeros) {
			return suppressZeros(suppressZeros == null ? DEFAULT_SUPPRESS_MODE
					: EnumSet.of(suppressZeros));
		}

		public Builder suppressZeros(Set<SuppressZeros> suppressZeros) {
			Builder clone = clone();
			clone.suppressZeros = suppressZeros == null ? DEFAULT_SUPPRESS_MODE
					: suppressZeros;
			return clone;
		}

		@Override
		protected Builder clone() {
			try {
				Builder clone = (Builder) super.clone();
				clone.formats = new HashMap<TimeUnit, String>(this.formats);
				clone.symbols = new HashMap<TimeUnit, String>(this.symbols);
				return clone;
			} catch (CloneNotSupportedException e) {
				throw Throwables.propagate(e);
			}
		}

		// -------------------------------------------------------------------------
		// - methods primarily found in google guava but redefined to
		// minimize jar -
		// - size -
		// -------------------------------------------------------------------------

		private static <T> T getLast(List<T> ts) {
			int size = ts.size();
			return size == 0 ? null : ts.get(size - 1);
		}

		private static <T> int indexOf(List<T> ts, Object search) {
			int i = 0;
			for (T t : ts) {
				if (search.equals(t)) {
					return i;
				}
				i++;
			}
			return -1;
		}

		private static <T> List<T> orderingNaturalReverse(List<T> ts) {
			List<T> result = new ArrayList<T>(ts);
			Collections.sort(result, Collections.<T> reverseOrder());
			return result;
		}

	}

}
