package com.bible.reference.explorer.api.Utils;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BRECommonUtil {

	public static <T> T timer(Supplier<T> func, String message) {
		Instant start = Instant.now();
		T t = func.get();
		Instant stop = Instant.now();
		log.debug("{} duration={}ns", message, Duration.between(start, stop).toNanos());
		return t;
	}

}
