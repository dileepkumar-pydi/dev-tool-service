package com.devtool.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class TimestampToolService {

	private static final DateTimeFormatter OUTPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

	public Map<String, List<String>> supportedTools() {
		Map<String, List<String>> tools = new LinkedHashMap<>();
		tools.put("timestampTools", List.of("Unix Timestamp to Date", "Date to Timestamp", "Timezone Converter"));
		return tools;
	}

	public String timestampToDate(String timestamp, String zoneId) {
		long seconds = parseLong(timestamp, "Invalid Unix timestamp.");
		ZoneId zone = resolveZone(zoneId);
		return Instant.ofEpochSecond(seconds).atZone(zone).format(OUTPUT_FORMAT);
	}

	public String dateToTimestamp(String dateTime, String zoneId) {
		ZoneId zone = resolveZone(zoneId);
		try {
			return String.valueOf(LocalDateTime.parse(safeValue(dateTime)).atZone(zone).toEpochSecond());
		} catch (Exception ex) {
			throw new IllegalArgumentException("Invalid date-time. Use ISO local date-time like 2026-06-06T12:30:00.", ex);
		}
	}

	public String convertTimezone(String dateTime, String sourceZoneId, String targetZoneId) {
		ZoneId source = resolveZone(sourceZoneId);
		ZoneId target = resolveZone(targetZoneId);
		try {
			ZonedDateTime sourceTime = LocalDateTime.parse(safeValue(dateTime)).atZone(source);
			return sourceTime.withZoneSameInstant(target).format(OUTPUT_FORMAT);
		} catch (Exception ex) {
			throw new IllegalArgumentException("Invalid date-time. Use ISO local date-time like 2026-06-06T12:30:00.", ex);
		}
	}

	private ZoneId resolveZone(String zoneId) {
		try {
			return ZoneId.of(safeValue(zoneId).isBlank() ? "UTC" : zoneId);
		} catch (Exception ex) {
			throw new IllegalArgumentException("Invalid timezone: " + zoneId, ex);
		}
	}

	private long parseLong(String value, String message) {
		try {
			return Long.parseLong(safeValue(value).trim());
		} catch (Exception ex) {
			throw new IllegalArgumentException(message, ex);
		}
	}

	private String safeValue(String value) {
		return value == null ? "" : value;
	}
}
