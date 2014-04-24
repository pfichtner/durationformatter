package com.github.pfichtner.durationformatter;

import static com.github.pfichtner.durationformatter.TimeValueAdder.get;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * These are the tests I did use while I did completely rewrite the whole
 * library test-driven (TDD). Because
 * {@link DurationFormatter.Builder#maximumAmountOfUnitsToShow(int)} is a
 * special case I started early to implement this feature since otherwise it
 * would have been impossible again to include this feature in the existing
 * library code.
 * 
 * @author Peter Fichtner
 */
public class DurationFormatterTest2 {

	@Test
	public void testDefaultDigits_Zero() {
		// hh:mm:ss
		assertEquals("00:00:00", DurationFormatter.DIGITS.formatMillis(0));
	}

	@Test
	public void testDefaultDigits_OneSecond() {
		// hh:mm:ss
		assertEquals("00:00:01", DurationFormatter.DIGITS.format(1, SECONDS));
	}

	@Test
	public void testLimit_Zero() {
		// hh:mm:ss
		assertEquals("00:00", limitAndRound().formatMillis(0));
	}

	@Test
	public void testLimit_29secondsRoundDown() {
		// hh:mm:ss

		long val1 = get(29, SECONDS).as(SECONDS);
		assertEquals("00:00", limitAndRound().format(val1, SECONDS));

		long val2 = get(29, SECONDS).and(1, MINUTES).as(SECONDS);
		assertEquals("00:01", limitAndRound().format(val2, SECONDS));
	}

	@Test
	public void testLimit_30secondsRoundUp() {
		// hh:mm:ss
		long val1 = get(30, SECONDS).as(SECONDS);
		assertEquals("00:01", limitAndRound().format(val1, SECONDS));

		long val2 = get(30, SECONDS).and(1, MINUTES).as(SECONDS);
		assertEquals("00:02", limitAndRound().format(val2, SECONDS));
	}

	@Test
	public void testLimit_30secondsRoundUp2() {
		// hh:mm:ss
		long val = get(30, SECONDS).and(59, MINUTES).as(SECONDS);
		assertEquals("01:00", limitAndRound().format(val, SECONDS));
	}

	private DurationFormatter limitAndRound() {
		return DurationFormatter.Builder.DIGITS.maximum(HOURS).minimum(SECONDS)
				.maximumAmountOfUnitsToShow(2).build();
	}

}
