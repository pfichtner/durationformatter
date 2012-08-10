package com.github.pfichtner.durationformatter;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.github.pfichtner.durationformatter.DurationFormatter;
import com.github.pfichtner.durationformatter.DurationFormatter.Builder;

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
		DurationFormatter roundOff = DurationFormatter.Builder.DIGITS.round(
				false).build();
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
		Builder builder = DurationFormatter.Builder.DIGITS.minimum(
				TimeUnit.NANOSECONDS).maximum(MILLISECONDS);
		DurationFormatter roundOn = builder.build();
		DurationFormatter strip = builder.stripLeadingZeros(true).build();
		assertEquals("00:000:000", roundOn.format(0, TimeUnit.NANOSECONDS));
		assertEquals("000", strip.format(0, TimeUnit.NANOSECONDS));

		assertEquals("00:000:499", roundOn.format(499, TimeUnit.NANOSECONDS));
		assertEquals("499", strip.format(499, TimeUnit.NANOSECONDS));
		assertEquals("00:000:500", roundOn.format(500, TimeUnit.NANOSECONDS));
		assertEquals("500", strip.format(500, TimeUnit.NANOSECONDS));
		assertEquals("00:000:999", roundOn.format(999, TimeUnit.NANOSECONDS));
		assertEquals("999", strip.format(999, TimeUnit.NANOSECONDS));
		assertEquals("00:001:499", roundOn.format(1499, TimeUnit.NANOSECONDS));
		assertEquals("001:499", strip.format(1499, TimeUnit.NANOSECONDS));

		assertEquals("00:000:001", roundOn.format(1, TimeUnit.NANOSECONDS));
		assertEquals("001", strip.format(1, TimeUnit.NANOSECONDS));
		assertEquals("00:001:000", roundOn.format(1, TimeUnit.MICROSECONDS));
		assertEquals("001:000", strip.format(1, TimeUnit.MICROSECONDS));
		assertEquals("01:000:000", roundOn.format(1, TimeUnit.MILLISECONDS));
		assertEquals("01:000:000", strip.format(1, TimeUnit.MILLISECONDS));
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
		DurationFormatter df = Builder.SYMBOLS.separator(", ").build();
		assertEquals("0h, 0min, 33s", df.formatMillis(SECONDS.toMillis(33)));

		df = Builder.SYMBOLS.separator(", ").symbol(SECONDS, " s")
				.symbol(MINUTES, " m").symbol(HOURS, " h").build();
		assertEquals("0 h, 0 m, 33 s", df.formatMillis(SECONDS.toMillis(33)));
		assertEquals(
				"0 h, 0 m, 34 s",
				df.formatMillis(SECONDS.toMillis(33)
						+ MILLISECONDS.toMillis(777)));

		df = Builder.SYMBOLS.separator(", ").minimum(MILLISECONDS)
				.symbol(SECONDS, " s").symbol(MINUTES, " m")
				.symbol(HOURS, " h").symbol(MILLISECONDS, " ms").build();
		assertEquals("0 h, 0 m, 33 s, 0 ms",
				df.formatMillis(SECONDS.toMillis(33)));

		df = Builder.SYMBOLS.separator(", ").symbol(SECONDS, " s")
				.symbol(MINUTES, " m").symbol(HOURS, " h")
				.symbol(MILLISECONDS, " ms").build();
		assertEquals("0 h, 0 m, 33 s", df.formatMillis(SECONDS.toMillis(33)));
		assertEquals(
				"0 h, 0 m, 34 s",
				df.formatMillis(SECONDS.toMillis(33)
						+ MILLISECONDS.toMillis(777)));

		df = Builder.SYMBOLS.minimum(MILLISECONDS).maximum(DAYS)
				.symbol(MILLISECONDS, " ms").symbol(SECONDS, " s")
				.symbol(MINUTES, " m").symbol(HOURS, " h").symbol(DAYS, " d")
				.build();
		assertEquals("1 d 6 h 51 m 51 s 111 ms", df.formatMillis(111111111));
		long timestamp = 1344171582461L;
		assertEquals("15557 d 12 h 59 m 42 s 461 ms",
				df.formatMillis(timestamp));

	}

}
