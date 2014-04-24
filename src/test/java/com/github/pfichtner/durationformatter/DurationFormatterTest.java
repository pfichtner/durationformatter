package com.github.pfichtner.durationformatter;

import static com.github.pfichtner.durationformatter.TimeValueAdder.get;
import static com.github.pfichtner.durationformatter.DurationFormatter.SuppressZeros.LEADING;
import static com.github.pfichtner.durationformatter.DurationFormatter.SuppressZeros.MIDDLE;
import static com.github.pfichtner.durationformatter.DurationFormatter.SuppressZeros.TRAILING;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.github.pfichtner.durationformatter.DurationFormatter.Builder;

public class DurationFormatterTest {

	@Test
	public void testDigits() {
		DurationFormatter df = DurationFormatter.DIGITS;
		assertEquals(
				"01:02:03",
				df.formatMillis(get(1, HOURS).and(2, MINUTES).and(3, SECONDS)
						.as(MILLISECONDS)));
		assertEquals("8760:00:00", df.formatMillis(DAYS.toMillis(365)));
		assertEquals("24000:00:00", df.formatMillis(DAYS.toMillis(1000)));
	}

	@Test
	public void testRounding() {
		DurationFormatter roundOn = DurationFormatter.DIGITS;
		DurationFormatter roundOff = Builder.DIGITS.round(false).build();
		assertEquals("00:00:00", roundOff.formatMillis(499));
		assertEquals("00:00:00", roundOn.formatMillis(499));

		assertEquals("00:00:00", roundOff.formatMillis(500));
		assertEquals("00:00:01", roundOn.formatMillis(500));

		assertEquals("00:00:00", roundOff.formatMillis(999));
		assertEquals("00:00:01", roundOn.formatMillis(999));

		assertEquals("00:00:01", roundOff.formatMillis(1499));
		assertEquals("00:00:01", roundOn.formatMillis(1499));

	}

	@Test
	public void testNanos() {
		Builder builder = Builder.DIGITS.minimum(NANOSECONDS).maximum(
				MILLISECONDS);
		DurationFormatter roundOn = builder.build();
		DurationFormatter stripLeft = builder.suppressZeros(LEADING).build();
		DurationFormatter stripRight = builder.suppressZeros(TRAILING).build();
		DurationFormatter stripMiddle = builder.suppressZeros(MIDDLE).build();

		assertEquals("000:000:000", roundOn.format(0, NANOSECONDS));
		assertEquals("000", stripLeft.format(0, NANOSECONDS));
		assertEquals("000", stripRight.format(0, NANOSECONDS));
		assertEquals("000:000:000", stripMiddle.format(0, NANOSECONDS));

		assertEquals("000:000:499", roundOn.format(499, NANOSECONDS));
		assertEquals("499", stripLeft.format(499, NANOSECONDS));
		assertEquals("000:000:499", stripRight.format(499, NANOSECONDS));
		assertEquals("000:000:499", stripMiddle.format(499, NANOSECONDS));

		assertEquals("000:000:500", roundOn.format(500, NANOSECONDS));
		assertEquals("500", stripLeft.format(500, NANOSECONDS));
		assertEquals("000:000:500", stripRight.format(500, NANOSECONDS));
		assertEquals("000:000:500", stripMiddle.format(500, NANOSECONDS));

		assertEquals("000:000:999", roundOn.format(999, NANOSECONDS));
		assertEquals("999", stripLeft.format(999, NANOSECONDS));
		assertEquals("000:000:999", stripRight.format(999, NANOSECONDS));
		assertEquals("000:000:999", stripMiddle.format(999, NANOSECONDS));

		assertEquals("000:001:499", roundOn.format(1499, NANOSECONDS));
		assertEquals("001:499", stripLeft.format(1499, NANOSECONDS));
		assertEquals("000:001:499", stripRight.format(1499, NANOSECONDS));
		assertEquals("000:001:499", stripMiddle.format(1499, NANOSECONDS));

		assertEquals("000:000:001", roundOn.format(1, NANOSECONDS));
		assertEquals("001", stripLeft.format(1, NANOSECONDS));
		assertEquals("000:000:001", stripRight.format(1, NANOSECONDS));
		assertEquals("000:000:001", stripMiddle.format(1, NANOSECONDS));

		assertEquals("000:001:000", roundOn.format(1, MICROSECONDS));
		assertEquals("001:000", stripLeft.format(1, MICROSECONDS));
		assertEquals("000:001", stripRight.format(1, MICROSECONDS));
		assertEquals("000:001:000", stripMiddle.format(1, MICROSECONDS));

		assertEquals("001:000:000", roundOn.format(1, MILLISECONDS));
		assertEquals("001:000:000", stripLeft.format(1, MILLISECONDS));
		assertEquals("001", stripRight.format(1, MILLISECONDS));
		assertEquals("001:000:000", stripMiddle.format(1, MILLISECONDS));
	}

	@Test
	public void testShowOnlyHighestUnits() {
		DurationFormatter fa1 = Builder.DIGITS.maximum(DAYS).build();
		DurationFormatter fa2 = Builder.DIGITS.maximum(DAYS)
				.maximumAmountOfUnitsToShow(2).build();
		DurationFormatter fa3 = Builder.DIGITS.maximum(DAYS)
				.maximumAmountOfUnitsToShow(1).build();

		DurationFormatter fb1 = Builder.SYMBOLS.maximum(DAYS).build();
		DurationFormatter fb2 = Builder.SYMBOLS.maximum(DAYS)
				.maximumAmountOfUnitsToShow(2).build();
		DurationFormatter fb3 = Builder.SYMBOLS.maximum(DAYS)
				.maximumAmountOfUnitsToShow(1).build();

		{
			long value = get(3, DAYS).as(MILLISECONDS);

			assertEquals("03:00:00:00", fa1.formatMillis(value));
			assertEquals("03:00", fa2.formatMillis(value));
			assertEquals("03", fa3.formatMillis(value));

			assertEquals("3d 0h 0min 0s", fb1.formatMillis(value));
			assertEquals("3d 0h", fb2.formatMillis(value));
			assertEquals("3d", fb3.formatMillis(value));
		}

		{
			long value = get(3, DAYS).and(1, SECONDS).as(MILLISECONDS);

			assertEquals("03:00:00:01", fa1.formatMillis(value));
			assertEquals("03:00", fa2.formatMillis(value));
			assertEquals("03", fa3.formatMillis(value));

			assertEquals("3d 0h 0min 1s", fb1.formatMillis(value));
			assertEquals("3d 0h", fb2.formatMillis(value));
			assertEquals("3d", fb3.formatMillis(value));
		}

		{
			long value = get(3, DAYS).and(2, HOURS).and(1, SECONDS)
					.as(MILLISECONDS);

			assertEquals("03:02:00:01", fa1.formatMillis(value));
			assertEquals("03:02", fa2.formatMillis(value));
			assertEquals("03", fa3.formatMillis(value));

			assertEquals("3d 2h 0min 1s", fb1.formatMillis(value));
			assertEquals("3d 2h", fb2.formatMillis(value));
			assertEquals("3d", fb3.formatMillis(value));
		}

		{
			long value = get(3, DAYS).and(12, HOURS).and(31, MINUTES)
					.and(1, SECONDS).as(MILLISECONDS);

			assertEquals("03:12:31:01", fa1.formatMillis(value));
			assertEquals("03:13", fa2.formatMillis(value));
			assertEquals("04", fa3.formatMillis(value));

			assertEquals("3d 12h 31min 1s", fb1.formatMillis(value));
			assertEquals("3d 13h", fb2.formatMillis(value));
			assertEquals("4d", fb3.formatMillis(value));
		}

	}

	@Test
	public void testDefaultSymbols() {
		DurationFormatter df = DurationFormatter.SYMBOLS;
		assertEquals("0h 0min 33s", df.formatMillis(SECONDS.toMillis(33)));
		assertEquals("0h 33min 0s", df.formatMillis(MINUTES.toMillis(33)));
		assertEquals("33h 0min 0s", df.formatMillis(HOURS.toMillis(33)));
		assertEquals("792h 0min 0s", df.formatMillis(DAYS.toMillis(33)));
		assertEquals("792h 0min 33s", df.formatMillis(get(33, DAYS).and(33,
				SECONDS).as(MILLISECONDS)));
	}

	@Test
	public void testOwnSymbols() {
		// use "m" instead of "min" for minutes

		Builder base = Builder.SYMBOLS.separator(", ").symbol(MINUTES, "m");
		DurationFormatter df = base.build();
		assertEquals("0h, 0m, 33s", df.formatMillis(SECONDS.toMillis(33)));

		df = base.valueSymbolSeparator(" ").build();
		assertEquals("0 h, 0 m, 33 s", df.formatMillis(SECONDS.toMillis(33)));
		assertEquals(
				"0 h, 0 m, 34 s",
				df.formatMillis(get(33, SECONDS).and(777, MILLISECONDS).as(
						MILLISECONDS)));

		df = base.minimum(MILLISECONDS).valueSymbolSeparator(" ").build();
		assertEquals("0 h, 0 m, 33 s, 0 ms",
				df.formatMillis(SECONDS.toMillis(33)));

		df = base.valueSymbolSeparator(" ").build();
		assertEquals("0 h, 0 m, 33 s", df.formatMillis(SECONDS.toMillis(33)));
		assertEquals(
				"0 h, 0 m, 34 s",
				df.formatMillis(get(33, SECONDS).and(777, MILLISECONDS).as(
						MILLISECONDS)));

		df = Builder.SYMBOLS.minimum(MILLISECONDS).symbol(MINUTES, "m")
				.maximum(DAYS).valueSymbolSeparator(" ").build();
		assertEquals("1 d 6 h 51 m 51 s 111 ms", df.formatMillis(111111111));
		long timestamp = 1344171582461L;
		assertEquals("15557 d 12 h 59 m 42 s 461 ms",
				df.formatMillis(timestamp));
	}

	@Test
	public void testMoreSymbols() {
		DurationFormatter df = Builder.SYMBOLS.round(false)
				.minimum(NANOSECONDS).maximum(MILLISECONDS)
				.suppressZeros(LEADING).valueSymbolSeparator(" ").build();
		assertEquals("360 ns", df.format(360, NANOSECONDS));
		assertEquals("1500 ms 0 μs 0 ns", df.format(1500000000, NANOSECONDS));
		assertEquals("60 ms 1 μs 3 ns", df.format(60001003, NANOSECONDS));
	}

	@Test
	public void testSuppressZeros() {
		Builder builder = Builder.SYMBOLS.minimum(NANOSECONDS).maximum(DAYS);
		DurationFormatter l = builder.suppressZeros(LEADING).build();
		DurationFormatter r = builder.suppressZeros(TRAILING).build();
		DurationFormatter m = builder.suppressZeros(MIDDLE).build();
		DurationFormatter lm = builder.suppressZeros(LEADING, MIDDLE).build();
		DurationFormatter rm = builder.suppressZeros(TRAILING, MIDDLE).build();
		DurationFormatter lrm = builder
				.suppressZeros(TRAILING, MIDDLE, LEADING).build();

		long n0000001 = 1;
		long n1000000 = DAYS.toNanos(1);
		long n1000001 = DAYS.toNanos(1) + 1;
		long n0100010 = HOURS.toNanos(1) + MICROSECONDS.toNanos(1);
		long n0010100 = MINUTES.toNanos(1) + MILLISECONDS.toNanos(1);
		long n0011100 = MINUTES.toNanos(1) + SECONDS.toNanos(1)
				+ MILLISECONDS.toNanos(1);
		long n1011101 = DAYS.toNanos(1) + MINUTES.toNanos(1)
				+ SECONDS.toNanos(1) + MILLISECONDS.toNanos(1) + 1;

		assertEquals("1ns", l.format(n0000001, NANOSECONDS));
		assertEquals("1d 0h 0min 0s 0ms 0μs 0ns",
				l.format(n1000000, NANOSECONDS));
		assertEquals("1d 0h 0min 0s 0ms 0μs 1ns",
				l.format(n1000001, NANOSECONDS));
		assertEquals("1h 0min 0s 0ms 1μs 0ns", l.format(n0100010, NANOSECONDS));
		assertEquals("1min 0s 1ms 0μs 0ns", l.format(n0010100, NANOSECONDS));
		assertEquals("1min 1s 1ms 0μs 0ns", l.format(n0011100, NANOSECONDS));
		assertEquals("1d 0h 1min 1s 1ms 0μs 1ns",
				l.format(n1011101, NANOSECONDS));

		assertEquals("0d 0h 0min 0s 0ms 0μs 1ns",
				r.format(n0000001, NANOSECONDS));
		assertEquals("1d", r.format(n1000000, NANOSECONDS));
		assertEquals("1d 0h 0min 0s 0ms 0μs 1ns",
				r.format(n1000001, NANOSECONDS));
		assertEquals("0d 1h 0min 0s 0ms 1μs", r.format(n0100010, NANOSECONDS));
		assertEquals("0d 0h 1min 0s 1ms", r.format(n0010100, NANOSECONDS));
		assertEquals("0d 0h 1min 1s 1ms", r.format(n0011100, NANOSECONDS));
		assertEquals("1d 0h 1min 1s 1ms 0μs 1ns",
				r.format(n1011101, NANOSECONDS));

		assertEquals("0d 0h 0min 0s 0ms 0μs 1ns",
				m.format(n0000001, NANOSECONDS));
		assertEquals("1d 0h 0min 0s 0ms 0μs 0ns",
				m.format(n1000000, NANOSECONDS));
		assertEquals("1d 1ns", m.format(n1000001, NANOSECONDS));
		assertEquals("0d 1h 1μs 0ns", m.format(n0100010, NANOSECONDS));
		assertEquals("0d 0h 1min 1ms 0μs 0ns", m.format(n0010100, NANOSECONDS));
		assertEquals("0d 0h 1min 1s 1ms 0μs 0ns",
				m.format(n0011100, NANOSECONDS));
		assertEquals("1d 1min 1s 1ms 1ns", m.format(n1011101, NANOSECONDS));

		assertEquals("1ns", lm.format(n0000001, NANOSECONDS));
		assertEquals("1d 0h 0min 0s 0ms 0μs 0ns",
				lm.format(n1000000, NANOSECONDS));
		assertEquals("1d 1ns", lm.format(n1000001, NANOSECONDS));
		assertEquals("1h 1μs 0ns", lm.format(n0100010, NANOSECONDS));
		assertEquals("1min 1ms 0μs 0ns", lm.format(n0010100, NANOSECONDS));
		assertEquals("1min 1s 1ms 0μs 0ns", lm.format(n0011100, NANOSECONDS));
		assertEquals("1d 1min 1s 1ms 1ns", lm.format(n1011101, NANOSECONDS));

		assertEquals("0d 0h 0min 0s 0ms 0μs 1ns",
				rm.format(n0000001, NANOSECONDS));
		assertEquals("1d", rm.format(n1000000, NANOSECONDS));
		assertEquals("1d 1ns", rm.format(n1000001, NANOSECONDS));
		assertEquals("0d 1h 1μs", rm.format(n0100010, NANOSECONDS));
		assertEquals("0d 0h 1min 1ms", rm.format(n0010100, NANOSECONDS));
		assertEquals("0d 0h 1min 1s 1ms", rm.format(n0011100, NANOSECONDS));
		assertEquals("1d 1min 1s 1ms 1ns", rm.format(n1011101, NANOSECONDS));

		assertEquals("1ns", lrm.format(n0000001, NANOSECONDS));
		assertEquals("1d", lrm.format(n1000000, NANOSECONDS));
		assertEquals("1d 1ns", lrm.format(n1000001, NANOSECONDS));
		assertEquals("1h 1μs", lrm.format(n0100010, NANOSECONDS));
		assertEquals("1min 1ms", lrm.format(n0010100, NANOSECONDS));
		assertEquals("1min 1s 1ms", lrm.format(n0011100, NANOSECONDS));
		assertEquals("1d 1min 1s 1ms 1ns", lrm.format(n1011101, NANOSECONDS));
	}

	@Test
	public void testCrazy() {
		DurationFormatter df = new Builder().maximum(DAYS).minimum(NANOSECONDS)
				.separator("|").valueSymbolSeparator("_")
				.symbol(HOURS, "hours").symbol(SECONDS, "seconds")
				.symbol(TimeUnit.MICROSECONDS, "micros").build();
		assertEquals(
				"00|01_hours|02|03_seconds|000|000_micros|000",
				df.formatMillis(get(1, HOURS).and(2, MINUTES).and(3, SECONDS)
						.as(MILLISECONDS)));
	}

	@Test
	public void testAtLeastOneBucketVisible() {
		Builder baseBuilder = DurationFormatter.Builder.SYMBOLS
				.minimum(TimeUnit.SECONDS).maximum(TimeUnit.DAYS)
				.maximumAmountOfUnitsToShow(2);

		DurationFormatter withZeros = baseBuilder.build();
		DurationFormatter withoutZeros = baseBuilder.suppressZeros(LEADING,
				MIDDLE, TRAILING).build();

		assertEquals("0d 0h", withZeros.format(1, SECONDS));
		assertEquals("0d 0h", withZeros.format(1, MINUTES));
		assertEquals("0d 0h", withZeros.format(0, SECONDS));
		assertEquals("0d 0h", withZeros.format(0, MINUTES));

		assertEquals("1s", withoutZeros.format(1, SECONDS));
		assertEquals("1min", withoutZeros.format(1, MINUTES));
		assertEquals("0s", withoutZeros.format(0, SECONDS));
		assertEquals("0s", withoutZeros.format(0, MINUTES));
	}

}
