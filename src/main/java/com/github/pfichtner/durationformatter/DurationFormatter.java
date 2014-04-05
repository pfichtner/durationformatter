package com.github.pfichtner.durationformatter;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.github.pfichtner.durationformatter.TimeValues.Bucket;

/**
 * A Formatter for durations. All implementing classes have to be threadsafe.
 * Instances can be created using the {@link Builder} class.
 * 
 * @author Peter Fichtner
 */
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
	 * @param timeUnit
	 *            the TimeUnit of <code>value</code>
	 * @return String containing the duration
	 */
	String format(long value, TimeUnit timeUnit);

	/**
	 * Helper class to create {@link DefaultDurationFormatter}s. This class is
	 * threadsafe so each method call will return a new instance of the Builder!
	 * 
	 * @author Peter Fichtner
	 */
	public static class Builder implements Cloneable {

		private static final EnumSet<SuppressZeros> DEFAULT_SUPPRESS_MODE = EnumSet
				.noneOf(SuppressZeros.class);

		/**
		 * A Formatter for durations. This class is threadsafe. Instances can be
		 * created using the {@link Builder} class.
		 * 
		 * @author Peter Fichtner
		 */
		private static class DefaultDurationFormatter implements
				DurationFormatter {

			private List<TimeUnit> timeUnits = TimeUnits.timeUnits;

			private static final String DEFAULT_FORMAT = "%02d";

			private final boolean round;
			private final List<TimeUnit> usedTimeUnits;
			private final String separator;
			private final String valueSymbolSeparator;
			private final boolean suppressLeading;
			private final boolean suppressTrailing;
			private final boolean suppressMiddle;
			private final int maximumAmountOfUnitsToShow;
			private final int idxMin;
			private final TimeUnit timeUnitMin;
			private final TimeUnit timeUnitMax;
			private final Map<TimeUnit, String> formats;
			private final Map<TimeUnit, String> symbols;

			public DefaultDurationFormatter(Builder builder) {
				this.round = builder.round;
				checkState(builder.minimum.compareTo(builder.maximum) <= 0,
						"maximum must not be smaller than minimum");
				this.idxMin = indexOf(timeUnits, builder.minimum);
				int idxMax = indexOf(timeUnits, builder.maximum);
				checkState(this.idxMin > idxMax,
						"min must not be greater than max");
				this.timeUnitMin = timeUnits.get(this.idxMin);
				this.timeUnitMax = timeUnits.get(idxMax);
				this.usedTimeUnits = timeUnits
						.subList(idxMax, timeUnits.size());
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
				this.maximumAmountOfUnitsToShow = builder.maximumAmountOfUnitsToShow;
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
			public String format(long longVal, TimeUnit timeUnit) {
				return join(pullFromLeft(round(limit(setZerosInvisible(restrictToSetUnitRange(new TimeValues(
						longVal, timeUnit)))))));
			}

			private TimeValues pullFromLeft(TimeValues values) {
				// findFirstVisible and pull from left
				for (Bucket bucket : values) {
					boolean visible = bucket.isVisible();
					if (visible) {
						bucket.pollFromLeft();
						break;
					}
				}
				return values;
			}

			private TimeValues restrictToSetUnitRange(TimeValues values) {
				for (Bucket bucket : values) {
					bucket.setVisible(this.usedTimeUnits.contains(bucket
							.getTimeUnit()));
				}
				return values;
			}

			private TimeValues round(TimeValues values) {
				if (round) {
					// search first invisible
					boolean visibleFound = false;
					for (Bucket bucket : values) {
						boolean bucketIsVisible = bucket.isVisible();
						if (!bucketIsVisible && visibleFound) {
							bucket.pushLeftRounded();
							break;
						}
						visibleFound |= bucketIsVisible;
					}
				}
				return values;
			}

			private String join(TimeValues values) {
				StringBuilder sb = new StringBuilder();
				for (Bucket bucket : values) {
					if (bucket.isVisible()) {
						sb.append(
								getValueString(bucket.getValue(),
										bucket.getTimeUnit())).append(
								this.separator);
					}
				}
				int len = sb.length();
				return len == 0 ? "" : sb.delete(len - separator.length(), len)
						.toString();
			}

			private TimeValues setZerosInvisible(TimeValues values) {
				return removeMiddle(removeTrailingZeros(removeLeadingZeros(values)));
			}

			private TimeValues removeMiddle(TimeValues values) {
				if (suppressMiddle) {
					Iterable<Bucket> sequence = values.sequence(timeUnitMax,
							timeUnitMin);
					TimeUnit firstNonZero = findFirstVisibleNonZero(sequence);
					TimeUnit lastNonZero = findFirstVisibleNonZero(values
							.sequence(timeUnitMin, timeUnitMax));
					if (firstNonZero != null && lastNonZero != null) {
						for (Bucket bucket : values.sequence(firstNonZero,
								lastNonZero)) {
							if (bucket.isVisible() && bucket.getValue() == 0) {
								bucket.setVisible(false);
							}
						}
					}
				}
				return values;
			}

			private TimeUnit findFirstVisibleNonZero(Iterable<Bucket> buckets) {
				int idx = 0;
				int hi = this.usedTimeUnits.size();
				for (Bucket bucket : buckets) {
					if (++idx == hi) {
						return null;
					} else if (bucket.isVisible() && bucket.getValue() != 0) {
						return bucket.getTimeUnit();
					}
				}
				return null;
			}

			private TimeValues removeTrailingZeros(TimeValues values) {
				return suppressTrailing ? removeZeros(values,
						values.sequence(timeUnitMin, timeUnitMax)) : values;
			}

			private TimeValues removeLeadingZeros(TimeValues values) {
				return suppressLeading ? removeZeros(values,
						values.sequence(timeUnitMax, timeUnitMin)) : values;
			}

			public TimeValues removeZeros(TimeValues values,
					Iterable<Bucket> buckets) {
				int idx = 0;
				int hi = this.usedTimeUnits.size();
				for (Bucket bucket : buckets) {
					if (++idx == hi || bucket.isVisible()
							&& bucket.getValue() != 0) {
						break;
					}
					bucket.setVisible(false);

				}
				return values;
			}

			private TimeValues limit(TimeValues values) {
				int vs = 0;
				for (Bucket bucket : values) {
					if (bucket != null) {
						boolean visible = bucket.isVisible()
								&& vs < this.maximumAmountOfUnitsToShow
								&& this.timeUnitMin.compareTo(bucket
										.getTimeUnit()) <= 0;
						if (visible) {
							vs++;
						}
						bucket.setVisible(visible);
					}
				}
				return values;
			}

			private String getValueString(long value, TimeUnit timeUnit) {
				String format = this.formats.get(timeUnit);
				String symbol = this.symbols.get(timeUnit);
				String stringVal = String.format(
						format == null ? DEFAULT_FORMAT : format, value);
				return symbol == null ? stringVal : stringVal
						+ this.valueSymbolSeparator + symbol;
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
		 * Default instance that formats seconds to hours using symbols (e.g.
		 * <code>0h 12m 33s</code>)
		 */
		public static final Builder SYMBOLS = BASE.separator(" ").format("%d")
				.symbol(NANOSECONDS, "ns").symbol(MICROSECONDS, "μs")
				.symbol(MILLISECONDS, "ms").symbol(SECONDS, "s")
				.symbol(MINUTES, "min").symbol(HOURS, "h").symbol(DAYS, "d");

		private int maximumAmountOfUnitsToShow = Integer.MAX_VALUE;
		private String separator = ":";
		private String valueSymbolSeparator = "";
		private TimeUnit minimum = MILLISECONDS;
		private TimeUnit maximum = HOURS;
		private boolean round = true;
		private Set<SuppressZeros> suppressZeros = DEFAULT_SUPPRESS_MODE;
		private Map<TimeUnit, String> formats = new HashMap<TimeUnit, String>();
		private Map<TimeUnit, String> symbols = new HashMap<TimeUnit, String>();

		public DurationFormatter build() {
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
		 * @param valueSymbolSeparator
		 *            symbolSeparator to use
		 * @return new Builder instance
		 */
		public Builder valueSymbolSeparator(String valueSymbolSeparator) {
			Builder clone = clone();
			clone.valueSymbolSeparator = valueSymbolSeparator;
			return clone;
		}

		public Builder format(String format) {
			Builder clone = clone();
			for (TimeUnit timeUnit : TimeUnits.timeUnits) {
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

		/**
		 * If you have lung running jobs (e.g. download, convention, ...) you
		 * want not to display 03:00:00:00:01 but display 03 or 03:00
		 * 
		 * @param maximumAmountOfUnitsToShow
		 *            how many units (from the left) should be shown
		 * @return this Builder
		 */
		public Builder maximumAmountOfUnitsToShow(int maximumAmountOfUnitsToShow) {
			Builder clone = clone();
			clone.maximumAmountOfUnitsToShow = maximumAmountOfUnitsToShow;
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
				throw new RuntimeException(e);
			}
		}

		// -------------------------------------------------------------------------
		// - methods primarily found in google guava but redefined to
		// minimize jar -
		// - size -
		// -------------------------------------------------------------------------

		private static void checkState(boolean state, String errorMessage) {
			if (!state) {
				throw new IllegalStateException(errorMessage);
			}
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

	}

}
