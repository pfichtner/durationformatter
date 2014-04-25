package com.github.pfichtner.durationformatter;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.text.DecimalFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
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
			for (Strategy strategy : this.strategies) {
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
	DurationFormatter DIGITS = Builder.DIGITS.build();

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

		private static interface FormatGenerator {
			Format generate(FormatGenerators formatGenerators);
		}

		private static abstract class AbstractFormatGenerator implements
				FormatGenerator {
		}

		private static class AppendSymbolFormatGenerator extends
				AbstractFormatGenerator {

			private final String symbol;

			public AppendSymbolFormatGenerator(String symbol) {
				this.symbol = symbol;
			}

			public Format generate(FormatGenerators formatGenerators) {
				return new DecimalFormat("0"
						+ formatGenerators.valueSymbolSeparator + this.symbol);
			}

		}

		private static class ChoiceSymbolFormatGenerator extends
				AbstractFormatGenerator {

			private final String singular;
			private final String plural;

			public ChoiceSymbolFormatGenerator(String singular, String plural) {
				this.singular = singular;
				this.plural = plural;
			}

			public Format generate(FormatGenerators formatGenerators) {
				return new MessageFormat("{0}"
						+ formatGenerators.valueSymbolSeparator
						+ "{0,choice,0#" + this.plural + "|1#" + this.singular
						+ "|1<" + this.plural + "}");
			}
		}

		private static class FormatGenerators implements Cloneable {

			private Map<TimeUnit, FormatGenerator> generators = new HashMap<TimeUnit, FormatGenerator>();
			private String valueSymbolSeparator = "";
			public boolean leadingZeros = true;

			public void valueSymbolSeparator(String valueSymbolSeparator) {
				this.valueSymbolSeparator = valueSymbolSeparator;
			}

			public void useFormatGenerator(TimeUnit timeUnit,
					FormatGenerator value) {
				this.generators.put(timeUnit, value);
			}

			@Override
			protected FormatGenerators clone()
					throws CloneNotSupportedException {
				FormatGenerators clone = (FormatGenerators) super.clone();
				clone.generators = new HashMap<TimeUnit, FormatGenerator>(
						this.generators);
				return clone;
			}

			private Map<TimeUnit, Format> createFormats(Builder builder,
					int idxMin, int idxMax) {
				Map<TimeUnit, Format> result = new HashMap<TimeUnit, Format>(
						this.generators.size());
				// TODO Reuse formatters: Since they are created per instance
				// they can be used unsynchronized
				for (TimeUnit timeUnit : TimeUnits.timeUnits.subList(idxMax,
						idxMin + 1)) {
					FormatGenerator generator = this.generators.get(timeUnit);
					result.put(
							timeUnit,
							generator == null ? formatFor(timeUnit) : generator
									.generate(this));
				}
				return result;
			}

			private NumberFormat formatFor(TimeUnit timeUnit) {
				return formatFor(timeUnit == DAYS ? 2 : String.valueOf(
						TimeUnits.maxValues.get(timeUnit) - 1).length());
			}

			private DecimalFormat formatFor(int len) {
				DecimalFormat format = new DecimalFormat();
				format.setMaximumFractionDigits(0);
				format.setGroupingUsed(false);
				format.setMinimumIntegerDigits(this.leadingZeros ? len : 1);
				return format;
			}

		}

		private static final EnumSet<SuppressZeros> DEFAULT_SUPPRESS_MODE = EnumSet
				.noneOf(SuppressZeros.class);

		private FormatGenerators formatGenerators = new FormatGenerators();

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
			 * Strategy that sets smallest unit visible if no bucket it visible.
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
					values.getBucket(this.minimum).setVisible(true);
					return values;
				}

			}

			private final String separator;

			private final Strategy strategy;

			private Map<TimeUnit, Format> formats;

			public DefaultDurationFormatter(Builder builder) {
				checkState(builder.minimum.compareTo(builder.maximum) <= 0,
						"maximum must not be smaller than minimum");
				int idxMin = indexOf(TimeUnits.timeUnits, builder.minimum);
				int idxMax = indexOf(TimeUnits.timeUnits, builder.maximum);
				checkState(idxMin > idxMax, "min must not be greater than max");
				this.separator = builder.separator;

				this.strategy = createStrategy(builder);
				this.formats = Collections
						.unmodifiableMap(builder.formatGenerators
								.createFormats(builder, idxMin, idxMax));
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
				return join(this.strategy.apply(new TimeValues(longVal,
						timeUnit)));
			}

			private String join(TimeValues values) {
				StringBuilder sb = new StringBuilder();
				// since we use non-threadsafe Formaters we have to synchronize
				synchronized (this.formats) {
					for (Bucket bucket : values) {
						if (bucket.isVisible()) {
							sb.append(
									getValueString(bucket.getValue(),
											bucket.getTimeUnit())).append(
									this.separator);
						}
					}
				}
				int len = sb.length();
				return len == 0 ? "" : sb.delete(len - this.separator.length(),
						len).toString();
			}

			private String getValueString(long value, TimeUnit timeUnit) {
				Format format = this.formats.get(timeUnit);
				return format instanceof MessageFormat ? ((MessageFormat) format)
						.format(new Object[] { value }) : format.format(value);
			}

		}

		private static final Builder BASE = new Builder().minimum(SECONDS)
				.maximum(HOURS);

		/**
		 * Default instance that formats seconds to hours using digits (e.g.
		 * <code>01:12:33</code>)
		 */
		public static final Builder DIGITS = BASE;

		/**
		 * Default instance that formats seconds to hours using symbols (e.g.
		 * <code>0h 12m 33s</code>)
		 */
		public static final Builder SYMBOLS = BASE.separator(" ")
				.symbol(NANOSECONDS, "ns").symbol(MICROSECONDS, "μs")
				.symbol(MILLISECONDS, "ms").symbol(SECONDS, "s")
				.symbol(MINUTES, "min").symbol(HOURS, "h").symbol(DAYS, "d");

		private int maximumAmountOfUnitsToShow = Integer.MAX_VALUE;
		private String separator = ":";
		private TimeUnit minimum = MILLISECONDS;
		private TimeUnit maximum = HOURS;
		private boolean round = true;
		private Set<SuppressZeros> suppressZeros = DEFAULT_SUPPRESS_MODE;

		public DurationFormatter build() {
			return new DefaultDurationFormatter(this);
		}

		public Builder valueSymbolSeparator(String separator) {
			Builder clone = clone();
			clone.formatGenerators.valueSymbolSeparator(separator);
			return clone;
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

		public Builder leadingZeros(boolean leadingZeros) {
			Builder clone = clone();
			clone.formatGenerators.leadingZeros = leadingZeros;
			return clone;
		}

		public Builder symbol(TimeUnit timeUnit, String symbol) {
			return useFormatGenerator(timeUnit,
					new AppendSymbolFormatGenerator(symbol));
		}

		public Builder symbolChoice(TimeUnit timeUnit, String singular,
				String plural) {
			return useFormatGenerator(timeUnit,
					new ChoiceSymbolFormatGenerator(singular, plural));
		}

		public Builder useFormatGenerator(TimeUnit timeUnit,
				FormatGenerator value) {
			Builder clone = clone();
			clone.formatGenerators.useFormatGenerator(timeUnit, value);
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
				clone.formatGenerators = this.formatGenerators.clone();
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
