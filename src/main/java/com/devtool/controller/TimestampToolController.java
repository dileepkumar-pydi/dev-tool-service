package com.devtool.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devtool.dto.ApiResponse;
import com.devtool.dto.DateTimeRequest;
import com.devtool.dto.ErrorResponse;
import com.devtool.dto.TimestampRequest;
import com.devtool.dto.TimestampResponse;
import com.devtool.dto.TimezoneConvertRequest;
import com.devtool.service.ApiResponseFactory;
import com.devtool.service.TimestampToolService;

@RestController
@RequestMapping("/api/timestamp")
public class TimestampToolController {

	private final TimestampToolService timestampToolService;
	private final ApiResponseFactory apiResponseFactory;

	public TimestampToolController(TimestampToolService timestampToolService, ApiResponseFactory apiResponseFactory) {
		this.timestampToolService = timestampToolService;
		this.apiResponseFactory = apiResponseFactory;
	}

	@GetMapping("/tools")
	public ApiResponse<Map<String, List<String>>> supportedTools() {
		return apiResponseFactory.success(timestampToolService.supportedTools(), "Timestamp tools fetched successfully");
	}

	@PostMapping("/to-date")
	public ApiResponse<TimestampResponse> toDate(@RequestBody TimestampRequest request) {
		return apiResponseFactory.success(
				new TimestampResponse("Unix Timestamp to Date", request.timestamp(), request.zoneId(),
						timestampToolService.timestampToDate(request.timestamp(), request.zoneId())),
				"Timestamp converted to date successfully");
	}

	@PostMapping("/to-timestamp")
	public ApiResponse<TimestampResponse> toTimestamp(@RequestBody DateTimeRequest request) {
		return apiResponseFactory.success(
				new TimestampResponse("Date to Timestamp", request.dateTime(), request.zoneId(),
						timestampToolService.dateToTimestamp(request.dateTime(), request.zoneId())),
				"Date converted to timestamp successfully");
	}

	@PostMapping("/convert-zone")
	public ApiResponse<TimestampResponse> convertZone(@RequestBody TimezoneConvertRequest request) {
		return apiResponseFactory.success(
				new TimestampResponse("Timezone Converter", request.dateTime(), request.targetZoneId(),
						timestampToolService.convertTimezone(request.dateTime(), request.sourceZoneId(),
								request.targetZoneId())),
				"Timezone converted successfully");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest()
				.body(apiResponseFactory.badRequest(new ErrorResponse(exception.getMessage()), exception.getMessage()));
	}
}
