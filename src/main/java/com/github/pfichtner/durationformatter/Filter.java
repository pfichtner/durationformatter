package com.github.pfichtner.durationformatter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface Filter {

	Filter NULL = new Filter() {
		public Map<TimeUnit, Integer> filter(
				LinkedHashMap<TimeUnit, Integer> map) {
			return map;
		}
	};

	Map<TimeUnit, Integer> filter(LinkedHashMap<TimeUnit, Integer> map);

}