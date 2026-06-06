package com.devtool.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TimestampToolServiceTest {

	private final TimestampToolService service = new TimestampToolService();

	@Test
	void shouldConvertTimestampToDate() {
		assertTrue(service.timestampToDate("0", "UTC").startsWith("1970-01-01 00:00:00"));
	}

	@Test
	void shouldConvertDateToTimestamp() {
		assertEquals("0", service.dateToTimestamp("1970-01-01T00:00:00", "UTC"));
	}
}
