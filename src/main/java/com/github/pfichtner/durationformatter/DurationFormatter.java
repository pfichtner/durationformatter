package com.github.pfichtner.durationformatter;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

	static class StrategyBuilder {

		private List<Strategy> strategies = new ArrayList<Strategy>();

		public StrategyBuilder add(Strategy strategy) {
			this.strategies.add(strategy);
			return this;
		}

		public Strategy build() {
			return new ComposedStrategy(this.strategies);
		}

	}

	static interface Strategy {

		Strategy NULL = new Strategy() {
			public TimeValues apply(TimeValues values) {
				return values;
			}
		};

		TimeValues apply(TimeValues values);

	}

	static class ComposedStrategy implements Strategy {

		private List<Strategy> strategies;

		ComposedStrategy(List<Strategy> strategies) {
			this.strategies = strategies;
		}

		public TimeValues apply(TimeValues values) {
			TimeValues result = values;
			for (Strategy strategy : strategies) {
				result = strategy.apply(result);
			}
			return result;
		}

	}

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

			/**
			 * This strategy marks fields between {@link #minimum} and
			 * {@link #maximum} as visible all others as invisible.
			 * 
			 * @author Peter Fichtner
			 */
			private static class SetUnusedTimeUnitsInvisibleStrategy implements
					Strategy {

				private final TimeUnit minimum;
				private final TimeUnit maximum;

				public SetUnusedTimeUnitsInvisibleStrategy(TimeUnit minimum,
						TimeUnit maximum) {
					this.minimum = minimum;
					this.maximum = maximum;
				}

				public TimeValues apply(TimeValues values) {
					for (Bucket bucket : values) {
						TimeUnit timeUnit = bucket.getTimeUnit();
						boolean b1 = timeUnit.compareTo(this.minimum) >= 0;
						boolean b2 = timeUnit.compareTo(this.maximum) <= 0;
						bucket.setVisible(b1 && b2);
					}
					return values;
				}

			}

			/**
			 * An abstract strategy that marks zeros as invisible.
			 * 
			 * @author Peter Fichtner
			 */
			private static abstract class RemoveZerosStrategy implements
					Strategy {

				private final TimeUnit minimum;
				private final TimeUnit maximum;

				public RemoveZerosStrategy(TimeUnit minimum, TimeUnit maximum) {
					this.minimum = minimum;
					this.maximum = maximum;
				}

				protected TimeUnit getMinimum() {
					return this.minimum;
				}

				protected TimeUnit getMaximum() {
					return this.maximum;
				}

				protected TimeValues removeZeros(TimeValues values,
						Iterable<Bucket> buckets) {
					for (Bucket bucket : buckets) {
						if (bucket.isVisible() && bucket.getValue() != 0) {
							break;
						}
						bucket.setVisible(false);

					}
					return values;
				}

			}

			/**
			 * Strategy that marks leading zeros as invisible.
			 * 
			 * @author Peter Fichtner
			 */
			private static class RemoveLeadingZerosStrategy extends
					RemoveZerosStrategy {

				public RemoveLeadingZerosStrategy(TimeUnit minimum,
						TimeUnit maximum) {
					super(minimum, maximum);
				}

				public TimeValues apply(TimeValues values) {
					return removeZeros(values,
							values.sequence(getMaximum(), getMinimum()));
				}

			}

			/**
			 * Strategy that marks trailing zeros as invisible.
			 * 
			 * @author Peter Fichtner
			 */
			private static class RemoveTrailingZerosStrategy extends
					RemoveZerosStrategy {

				public RemoveTrailingZerosStrategy(TimeUnit minimum,
						TimeUnit maximum) {
					super(minimum, maximum);
				}

				public TimeValues apply(TimeValues values) {
					return removeZeros(values,
							values.sequence(getMinimum(), getMaximum()));
				}

			}

			/**
			 * Strategy that marks zeros that are <b>not</b> leading and
			 * <b>not</b> trailing as invisible.
			 * 
			 * @author Peter Fichtner
			 */
			private static class RemoveMiddleZerosStrategy extends
					RemoveZerosStrategy {

				public RemoveMiddleZerosStrategy(TimeUnit minimum,
						TimeUnit maximum) {
					super(minimum, maximum);
				}

				public TimeValues apply(TimeValues values) {
					Iterable<Bucket> sequence = values.sequence(getMaximum(),
							getMinimum());
					TimeUnit firstNonZero = findFirstVisibleNonZero(sequence);
					TimeUnit lastNonZero = findFirstVisibleNonZero(values
							.sequence(getMinimum(), getMaximum()));
					if (firstNonZero != null && lastNonZero != null) {
						for (Bucket bucket : values.sequenceInclude(
								firstNonZero, lastNonZero)) {
							if (bucket.isVisible() && bucket.getValue() == 0) {
								bucket.setVisible(false);
							}
						}
					}
					return values;
				}

				private TimeUnit findFirstVisibleNonZero(
						Iterable<Bucket> buckets) {
					for (Bucket bucket : buckets) {
						if (bucket.isVisible() && bucket.getValue() != 0) {
							return bucket.getTimeUnit();
						}
					}
					return null;
				}

			}

			/**
			 * Strategy that marks only the first n elements as visible all
			 * others as invisible.
			 * 
			 * @author Peter Fichtner
			 */
			private static class LimitStrategy implements Strategy {

				private final int limit;

				public LimitStrategy(int limit) {
					this.limit = limit;
				}

				public TimeValues apply(TimeValues values) {
					int visibles = 0;
					for (Bucket bucket : values) {
						boolean visible = bucket.isVisible()
								&& visibles < this.limit;
						if (visible) {
							visibles++;
						}
						bucket.setVisible(visible);
					}
					return values;
				}

			}

			/**
			 * Strategy that rounds the last visible bucket.
			 * 
			 * @author Peter Fichtner
			 */
			private static class RoundingStrategy implements Strategy {

				public TimeValues apply(TimeValues values) {
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
					return values;
				}

			}

			/**
			 * Strategy that searches first visible bucket and pulls all buckets
			 * (higher values) to the bucket found.
			 * 
			 * @author Peter Fichtner
			 */
			private static class PullFromLeftStrategy implements Strategy {

				public TimeValues apply(TimeValues values) {
					// findFirstVisible and pull from left
					for (Bucket bucket : values) {
						if (bucket.isVisible()) {
							bucket.pollFromLeft();
							break;
						}
					}
					return values;
				}

			}

			/**
			 * Strategy that sets smallest unit visible if no bucket it visble.
			 * 
			 * @author Peter Fichtner
			 */
			private static class SetAtLeastOneBucketVisibleStrategy implements
					Strategy {

				private final TimeUnit minimum;

				public SetAtLeastOneBucketVisibleStrategy(TimeUnit minimum) {
					this.minimum = minimum;
				}

				public TimeValues apply(TimeValues values) {
					for (Bucket bucket : values) {
						if (bucket.isVisible()) {
							return values;
						}
					}
					values.getBucket(minimum).setVisible(true);
					return values;
				}

			}

			private final String separator;
			private final String valueSymbolSeparator;
			private final Map<TimeUnit, String> formats;
			private final Map<TimeUnit, String> symbols;

			private final Strategy strategy;

			public DefaultDurationFormatter(Builder builder) {
				checkState(builder.minimum.compareTo(builder.maximum) <= 0,
						"maximum must not be smaller than minimum");
				int idxMin = indexOf(TimeUnits.timeUnits, builder.minimum);
				int idxMax = indexOf(TimeUnits.timeUnits, builder.maximum);
				checkState(idxMin > idxMax, "min must not be greater than max");
				this.separator = builder.separator;
				this.valueSymbolSeparator = builder.valueSymbolSeparator;

				this.strategy = createStrategy(builder);
				this.formats = Collections.unmodifiableMap(createFormats(
						builder, idxMin, idxMax));
				this.symbols = Collections
						.unmodifiableMap(new HashMap<TimeUnit, String>(
								builder.symbols));
			}

			private Map<TimeUnit, String> createFormats(Builder builder,
					int idxMin, int idxMax) {
				Map<TimeUnit, String> formats = new HashMap<TimeUnit, String>(
						builder.formats);
				for (TimeUnit timeUnit : TimeUnits.timeUnits.subList(idxMax,
						idxMin + 1)) {
					String format = builder.formats.get(timeUnit);
					formats.put(timeUnit, format == null ? formatFor(timeUnit)
							: format);
				}
				return formats;
			}

			public Strategy createStrategy(Builder builder) {
				StrategyBuilder sb = new StrategyBuilder()
						.add(new SetUnusedTimeUnitsInvisibleStrategy(
								builder.minimum, builder.maximum));
				sb = builder.suppressZeros.contains(SuppressZeros.LEADING) ? sb
						.add(new RemoveLeadingZerosStrategy(builder.minimum,
								builder.maximum)) : sb;
				sb = builder.suppressZeros.contains(SuppressZeros.TRAILING) ? sb
						.add(new RemoveTrailingZerosStrategy(builder.minimum,
								builder.maximum)) : sb;
				sb = builder.suppressZeros.contains(SuppressZeros.MIDDLE) ? sb
						.add(new RemoveMiddleZerosStrategy(builder.minimum,
								builder.maximum)) : sb;
				sb = builder.maximumAmountOfUnitsToShow > 0 ? sb
						.add(new LimitStrategy(
								builder.maximumAmountOfUnitsToShow)) : sb;
				sb = builder.round ? sb.add(new RoundingStrategy()) : sb;
				return sb
						.add(new PullFromLeftStrategy())
						.add(new SetAtLeastOneBucketVisibleStrategy(
								builder.minimum)).build();
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
				return join(strategy.apply(new TimeValues(longVal, timeUnit)));
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

			private String getValueString(long value, TimeUnit timeUnit) {
				String format = this.formats.get(timeUnit);
				String symbol = this.symbols.get(timeUnit);
				String stringVal = String.format(format, value);
				return symbol == null ? stringVal : stringVal
						+ this.valueSymbolSeparator + symbol;
			}

		}

		private static final Builder BASE = new Builder().minimum(SECONDS)
				.maximum(HOURS);

		public static String formatFor(TimeUnit timeUnit) {
			int i = timeUnit == DAYS ? 2 : String.valueOf(
					TimeUnits.maxValues.get(timeUnit) - 1).length();
			return "%0" + i + "d";
		}

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

		public Builder suppressZeros(SuppressZeros... suppressZeros) {
			return suppressZeros(Arrays.asList(suppressZeros));
		}

		public Builder suppressZeros(Collection<SuppressZeros> suppressZeros) {
			Builder clone = clone();
			clone.suppressZeros = suppressZeros == null ? DEFAULT_SUPPRESS_MODE
					: EnumSet.copyOf(suppressZeros);
			return clone;
		}

		/**
		 * Cut of after n units, e.g. if there is a long running jobs (e.g.
		 * download, convert-job, ...) you want not to display 03:00:00:00:01
		 * but display 03:00 (or 03) instead.
		 * 
		 * @param maximumAmountOfUnitsToShow
		 *            how many units (from the left) should be shown, remaining
		 *            units will be cut of (but rounded if rounding is enabled)
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
