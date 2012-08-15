package com.github.pfichtner.durationformatter;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.github.pfichtner.durationformatter.DurationFormatter.Builder;
import com.github.pfichtner.durationformatter.DurationFormatter.SuppressZeros;

public class DurationFormatterTest {

	@Test
	public void testDigits() {
		DurationFormatter df = DurationFormatter.DIGITS;
		assertEquals(
				"01:02:03",
				df.formatMillis(HOURS.toMillis(1) + MINUTES.toMillis(2)
						+ SECONDS.toMillis(3)));
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
		Builder builder = Builder.DIGITS.minimum(TimeUnit.NANOSECONDS).maximum(
				MILLISECONDS);
		DurationFormatter roundOn = builder.build();
		DurationFormatter stripLeft = builder.suppressZeros(
				SuppressZeros.LEADING).build();
		DurationFormatter stripRight = builder.suppressZeros(
				SuppressZeros.TRAILING).build();
		DurationFormatter stripMiddle = builder.suppressZeros(
				SuppressZeros.MIDDLE).build();

		assertEquals("00:000:000", roundOn.format(0, TimeUnit.NANOSECONDS));
		assertEquals("000", stripLeft.format(0, TimeUnit.NANOSECONDS));
		assertEquals("00", stripRight.format(0, TimeUnit.NANOSECONDS));
		assertEquals("00:000:000", stripMiddle.format(0, TimeUnit.NANOSECONDS));

		assertEquals("00:000:499", roundOn.format(499, TimeUnit.NANOSECONDS));
		assertEquals("499", stripLeft.format(499, TimeUnit.NANOSECONDS));
		assertEquals("00:000:499", stripRight.format(499, TimeUnit.NANOSECONDS));
		assertEquals("00:000:499",
				stripMiddle.format(499, TimeUnit.NANOSECONDS));

		assertEquals("00:000:500", roundOn.format(500, TimeUnit.NANOSECONDS));
		assertEquals("500", stripLeft.format(500, TimeUnit.NANOSECONDS));
		assertEquals("00:000:500", stripRight.format(500, TimeUnit.NANOSECONDS));
		assertEquals("00:000:500", stripMiddle.format(500, TimeUnit.NANOSECONDS));

		assertEquals("00:000:999", roundOn.format(999, TimeUnit.NANOSECONDS));
		assertEquals("999", stripLeft.format(999, TimeUnit.NANOSECONDS));
		assertEquals("00:000:999", stripRight.format(999, TimeUnit.NANOSECONDS));
		assertEquals("00:000:999", stripMiddle.format(999, TimeUnit.NANOSECONDS));

		assertEquals("00:001:499", roundOn.format(1499, TimeUnit.NANOSECONDS));
		assertEquals("001:499", stripLeft.format(1499, TimeUnit.NANOSECONDS));
		assertEquals("00:001:499", stripRight.format(1499, TimeUnit.NANOSECONDS));
		assertEquals("00:001:499", stripMiddle.format(1499, TimeUnit.NANOSECONDS));

		assertEquals("00:000:001", roundOn.format(1, TimeUnit.NANOSECONDS));
		assertEquals("001", stripLeft.format(1, TimeUnit.NANOSECONDS));
		assertEquals("00:000:001", stripRight.format(1, TimeUnit.NANOSECONDS));
		assertEquals("00:000:001", stripMiddle.format(1, TimeUnit.NANOSECONDS));

		assertEquals("00:001:000", roundOn.format(1, TimeUnit.MICROSECONDS));
		assertEquals("001:000", stripLeft.format(1, TimeUnit.MICROSECONDS));
		assertEquals("00:001", stripRight.format(1, TimeUnit.MICROSECONDS));
		assertEquals("00:001:000", stripMiddle.format(1, TimeUnit.MICROSECONDS));

		assertEquals("01:000:000", roundOn.format(1, TimeUnit.MILLISECONDS));
		assertEquals("01:000:000", stripLeft.format(1, TimeUnit.MILLISECONDS));
		assertEquals("01", stripRight.format(1, TimeUnit.MILLISECONDS));
		assertEquals("01:000:000", stripMiddle.format(1, TimeUnit.MILLISECONDS));
	}

	@Test
	public void testDefaultSymbols() {
		DurationFormatter df = DurationFormatter.SYMBOLS;
		assertEquals("0h 0min 33s", df.formatMillis(SECONDS.toMillis(33)));
		assertEquals("0h 33min 0s", df.formatMillis(MINUTES.toMillis(33)));
		assertEquals("33h 0min 0s", df.formatMillis(HOURS.toMillis(33)));
		assertEquals("792h 0min 0s", df.formatMillis(DAYS.toMillis(33)));
		assertEquals("792h 0min 33s",
				df.formatMillis(DAYS.toMillis(33) + SECONDS.toMillis(33)));
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
				df.formatMillis(SECONDS.toMillis(33)
						+ MILLISECONDS.toMillis(777)));

		df = base.minimum(MILLISECONDS).valueSymbolSeparator(" ").build();
		assertEquals("0 h, 0 m, 33 s, 0 ms",
				df.formatMillis(SECONDS.toMillis(33)));

		df = base.valueSymbolSeparator(" ").build();
		assertEquals("0 h, 0 m, 33 s", df.formatMillis(SECONDS.toMillis(33)));
		assertEquals(
				"0 h, 0 m, 34 s",
				df.formatMillis(SECONDS.toMillis(33)
						+ MILLISECONDS.toMillis(777)));

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
				.minimum(TimeUnit.NANOSECONDS).maximum(MILLISECONDS)
				.suppressZeros(SuppressZeros.LEADING).valueSymbolSeparator(" ")
				.build();
		assertEquals("360 ns", df.format(360, TimeUnit.NANOSECONDS));
		assertEquals("1500 ms 0 μs 0 ns",
				df.format(1500000000, TimeUnit.NANOSECONDS));
		assertEquals("60 ms 1 μs 3 ns",
				df.format(60001003, TimeUnit.NANOSECONDS));
	}

	@Test
	public void testCrazy() {
		DurationFormatter df = new Builder().maximum(TimeUnit.DAYS)
				.minimum(TimeUnit.NANOSECONDS).separator("|||||")
				.valueSymbolSeparator("xXx").symbol(HOURS, "<<H>>")
				.symbol(SECONDS, "<<S>>").build();
		assertEquals(
				"00|||||01xXx<<H>>|||||02|||||03xXx<<S>>|||||00|||||00|||||00",
				df.formatMillis(HOURS.toMillis(1) + MINUTES.toMillis(2)
						+ SECONDS.toMillis(3)));
	}

}
