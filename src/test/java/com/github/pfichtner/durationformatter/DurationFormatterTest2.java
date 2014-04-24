package com.github.pfichtner.durationformatter;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

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
		assertEquals("00:00:01",
				DurationFormatter.DIGITS.format(1, TimeUnit.SECONDS));
	}

	@Test
	public void testLimit_Zero() {
		// hh:mm:ss
		assertEquals("00:00", limitAndRound().formatMillis(0));
	}

	@Test
	public void testLimit_29secondsRoundDown() {
		// hh:mm:ss
		assertEquals("00:00", limitAndRound().format(29, TimeUnit.SECONDS));
		assertEquals("00:01", limitAndRound().format(29 + 60, TimeUnit.SECONDS));
	}

	@Test
	public void testLimit_30secondsRoundUp() {
		// hh:mm:ss
		assertEquals("00:01", limitAndRound().format(30, TimeUnit.SECONDS));
		assertEquals("00:02", limitAndRound().format(30 + 60, TimeUnit.SECONDS));
	}

	@Test
	public void testLimit_30secondsRoundUp2() {
		// hh:mm:ss
		assertEquals("01:00",
				limitAndRound().format(30 + (59 * 60), TimeUnit.SECONDS));
	}

	private DurationFormatter limitAndRound() {
		return DurationFormatter.Builder.DIGITS.maximum(TimeUnit.HOURS)
				.minimum(TimeUnit.SECONDS).maximumAmountOfUnitsToShow(2)
				.build();
	}

}
