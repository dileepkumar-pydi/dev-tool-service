package com.devtool.dto;

public record TimezoneConvertRequest(String dateTime, String sourceZoneId, String targetZoneId) {
}
