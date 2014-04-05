package com.github.pfichtner.durationformatter;

import static java.util.concurrent.TimeUnit.*;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.github.pfichtner.durationformatter.TimeValues.Bucket;

public class TimeValuesTest {

	@Test
	public void testNoOverflow() {
		TimeValues values = new TimeValues().add(59, SECONDS);
		assertEquals("0:0:0:59:0:0:0", getString(values));
	}

	@Test
	public void testOverflow1() {
		TimeValues values = new TimeValues().add(60, SECONDS);
		assertEquals("0:0:1:0:0:0:0", getString(values));
	}

	@Test
	public void testOverflow2() {
		TimeValues values = new TimeValues().add(123, SECONDS);
		assertEquals("0:0:2:3:0:0:0", getString(values));
	}

	@Test
	public void testOverflow3() {
		TimeValues values = new TimeValues().add(
				(int) (123 + SECONDS.convert(4, HOURS)), SECONDS);
		assertEquals("0:4:2:3:0:0:0", getString(values));
	}

	@Test
	public void testOverflow4() {
		TimeValues values = new TimeValues().add(DAYS.toNanos(1) + 2,
				NANOSECONDS);
		assertEquals("1:0:0:0:0:0:2", getString(values));
	}

	@Test
	public void testAddNotInitializedNoOverflow() {
		TimeValues values = new TimeValues().add(2, SECONDS);
		assertEquals("0:0:0:2:0:0:0", getString(values));
	}

	@Test
	public void testAddNoOverflow() {
		TimeValues values = new TimeValues().add(1, SECONDS).add(2, SECONDS);
		assertEquals("0:0:0:3:0:0:0", getString(values));
	}

	@Test
	public void testAddOverflow() {
		TimeValues values = new TimeValues().add(59, SECONDS).add(1, SECONDS);
		assertEquals("0:0:1:0:0:0:0", getString(values));
	}

	@Test
	public void testOverflowWithPreconfiguredMinutes() {
		TimeValues values = new TimeValues().add(5, MINUTES).add(59, SECONDS)
				.add(1, SECONDS);
		assertEquals("0:0:6:0:0:0:0", getString(values));
	}

	@Test
	public void testEliminateAndRoundIsEliminated() {
		TimeValues values = new TimeValues().add(29, SECONDS).pushLeftRounded(
				SECONDS);
		assertEquals("0:0:0:0:0:0:0", getString(values));
	}

	@Test
	public void testEliminateAndRoundIsRounded() {
		TimeValues values = new TimeValues().add(30, SECONDS).pushLeftRounded(
				SECONDS);
		assertEquals("0:0:1:0:0:0:0", getString(values));
	}

	@Test
	public void testPollFromLeft1() {
		TimeValues values = new TimeValues().add(27, HOURS).pollFromLeft(HOURS);
		assertEquals("0:27:0:0:0:0:0", getString(values));
	}

	@Test
	public void testPollFromLeft2() {
		long oneDayInSeconds = DAYS.toSeconds(1);
		TimeValues values = new TimeValues().add(oneDayInSeconds, SECONDS)
				.pollFromLeft(MINUTES);
		assertEquals("0:0:" + oneDayInSeconds / 60 + ":0:0:0:0",
				getString(values));
	}

	// -----------------------------------------------------------------------

	@Test
	public void testSequenceDefaultOrder() {
		assertEquals(
				Arrays.asList(DAYS, HOURS, MINUTES),
				extract(collect(new TimeValues().sequenceInclude(DAYS, MINUTES))));
		assertEquals(Arrays.asList(MILLISECONDS, MICROSECONDS, NANOSECONDS),
				extract(collect(new TimeValues().sequenceInclude(MILLISECONDS,
						NANOSECONDS))));

		assertEquals(Arrays.asList(DAYS, HOURS),
				extract(collect(new TimeValues().sequence(DAYS, MINUTES))));
		assertEquals(Arrays.asList(MILLISECONDS, MICROSECONDS),
				extract(collect(new TimeValues().sequence(MILLISECONDS,
						NANOSECONDS))));

	}

	@Test
	public void testSequenceReverseOrder() {
		assertEquals(
				Arrays.asList(MINUTES, HOURS, DAYS),
				extract(collect(new TimeValues().sequenceInclude(MINUTES, DAYS))));
		assertEquals(Arrays.asList(NANOSECONDS, MICROSECONDS, MILLISECONDS),
				extract(collect(new TimeValues().sequenceInclude(NANOSECONDS,
						MILLISECONDS))));

		assertEquals(Arrays.asList(MINUTES, HOURS),
				extract(collect(new TimeValues().sequence(MINUTES, DAYS))));
		assertEquals(Arrays.asList(NANOSECONDS, MICROSECONDS),
				extract(collect(new TimeValues().sequence(NANOSECONDS,
						MILLISECONDS))));
	}

	// -----------------------------------------------------------------------

	private static <T> List<T> collect(Iterable<T> iterable) {
		List<T> result = new ArrayList<T>();
		for (T t : iterable) {
			result.add(t);
		}
		return result;
	}

	private List<TimeUnit> extract(List<Bucket> buckets) {
		List<TimeUnit> result = new ArrayList<TimeUnit>();
		for (Bucket bucket : buckets) {
			result.add(bucket.getTimeUnit());
		}
		return result;
	}

	private static String getString(TimeValues values) {
		StringBuilder sb = new StringBuilder();
		for (Bucket bucket : values) {
			sb.append(bucket.getValue()).append(":");
		}
		return (sb.length() == 0 ? sb : sb.deleteCharAt(sb.length() - 1))
				.toString();
	}

}
