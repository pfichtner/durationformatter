package com.github.pfichtner.durationformatter;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.github.pfichtner.durationformatter.DurationFormatter.SuppressZeros;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Maps;

public class FilterTest {

	@Test
	public void testSuppressLeadingAllZero() {
		Filter filter = new DefaultFilter(EnumSet.of(SuppressZeros.LEADING));

		assertEquals(reverse(ImmutableSortedMap.of(TimeUnit.DAYS, 0)),
				filter.filter(reverse(Maps.newLinkedHashMap(ImmutableSortedMap
						.of(TimeUnit.DAYS, 0)))));

		assertEquals(reverse(ImmutableSortedMap.of(TimeUnit.SECONDS, 0)),
				filter.filter(reverse(Maps.newLinkedHashMap(ImmutableSortedMap
						.of(TimeUnit.DAYS, 0, TimeUnit.HOURS, 0,
								TimeUnit.MINUTES, 0, TimeUnit.SECONDS, 0)))));

	}

	@Test
	public void testSuppressLeading() {
		Filter filter = new DefaultFilter(EnumSet.of(SuppressZeros.LEADING));

		assertEquals(reverse(ImmutableSortedMap.of(TimeUnit.DAYS, 5)),
				filter.filter(reverse(Maps.newLinkedHashMap(ImmutableSortedMap
						.of(TimeUnit.DAYS, 5)))));

		assertEquals(reverse(ImmutableSortedMap.of(TimeUnit.SECONDS, 5)),
				filter.filter(reverse(Maps.newLinkedHashMap(ImmutableSortedMap
						.of(TimeUnit.DAYS, 0, TimeUnit.HOURS, 0,
								TimeUnit.MINUTES, 0, TimeUnit.SECONDS, 5)))));
		assertEquals(reverse(ImmutableSortedMap.of(TimeUnit.HOURS, 1,
				TimeUnit.MINUTES, 0, TimeUnit.SECONDS, 3)),
				filter.filter(reverse(Maps.newLinkedHashMap(ImmutableSortedMap
						.of(TimeUnit.DAYS, 0, TimeUnit.HOURS, 1,
								TimeUnit.MINUTES, 0, TimeUnit.SECONDS, 3)))));
		assertEquals(reverse(Maps.newLinkedHashMap(ImmutableSortedMap.of(
				TimeUnit.DAYS, 1, TimeUnit.HOURS, 2, TimeUnit.MINUTES, 3,
				TimeUnit.SECONDS, 4))), filter.filter(reverse(Maps
				.newLinkedHashMap(ImmutableSortedMap.of(TimeUnit.DAYS, 1,
						TimeUnit.HOURS, 2, TimeUnit.MINUTES, 3,
						TimeUnit.SECONDS, 4)))));

	}

	// ----------------------------------------------------------------------------------
	@Test
	public void testSuppressTrailingAllZero() {
		Filter filter = new DefaultFilter(EnumSet.of(SuppressZeros.TRAILING));

		assertEquals(reverse(ImmutableSortedMap.of(TimeUnit.DAYS, 0)),
				filter.filter(reverse(Maps.newLinkedHashMap(ImmutableSortedMap
						.of(TimeUnit.DAYS, 0)))));

		assertEquals(reverse(ImmutableSortedMap.of(TimeUnit.DAYS, 0)),
				filter.filter(reverse(Maps.newLinkedHashMap(ImmutableSortedMap
						.of(TimeUnit.DAYS, 0, TimeUnit.HOURS, 0,
								TimeUnit.MINUTES, 0, TimeUnit.SECONDS, 0)))));

	}

	@Test
	public void testSuppressTrailing() {
		Filter filter = new DefaultFilter(EnumSet.of(SuppressZeros.TRAILING));

		assertEquals(reverse(ImmutableSortedMap.of(TimeUnit.DAYS, 5)),
				filter.filter(reverse(Maps.newLinkedHashMap(ImmutableSortedMap
						.of(TimeUnit.DAYS, 5)))));

		assertEquals(reverse(ImmutableSortedMap.of(TimeUnit.DAYS, 5)),
				filter.filter(reverse(Maps.newLinkedHashMap(ImmutableSortedMap
						.of(TimeUnit.DAYS, 5, TimeUnit.HOURS, 0,
								TimeUnit.MINUTES, 0, TimeUnit.SECONDS, 0)))));
		assertEquals(reverse(ImmutableSortedMap.of(TimeUnit.DAYS, 3,
				TimeUnit.HOURS, 2, TimeUnit.MINUTES, 1)),
				filter.filter(reverse(Maps.newLinkedHashMap(ImmutableSortedMap
						.of(TimeUnit.DAYS, 3, TimeUnit.HOURS, 2,
								TimeUnit.MINUTES, 1, TimeUnit.SECONDS, 0)))));
		assertEquals(reverse(Maps.newLinkedHashMap(ImmutableSortedMap.of(
				TimeUnit.DAYS, 1, TimeUnit.HOURS, 2, TimeUnit.MINUTES, 3,
				TimeUnit.SECONDS, 4))), filter.filter(reverse(Maps
				.newLinkedHashMap(ImmutableSortedMap.of(TimeUnit.DAYS, 1,
						TimeUnit.HOURS, 2, TimeUnit.MINUTES, 3,
						TimeUnit.SECONDS, 4)))));

	}

	// ----------------------------------------------------------------------------------

	private static LinkedHashMap<TimeUnit, Integer> reverse(
			Map<TimeUnit, Integer> map) {
		Set<Entry<TimeUnit, Integer>> entrySet = map.entrySet();
		LinkedHashMap<TimeUnit, Integer> result = new LinkedHashMap<TimeUnit, Integer>(
				entrySet.size(), 1f);
		for (Entry<TimeUnit, Integer> entry : revserse(new ArrayList<Entry<TimeUnit, Integer>>(
				entrySet))) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	private static <T> List<T> revserse(List<T> list) {
		Collections.reverse(list);
		return list;
	}

}
