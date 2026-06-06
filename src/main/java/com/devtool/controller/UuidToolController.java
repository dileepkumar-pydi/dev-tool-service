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
import com.devtool.dto.ErrorResponse;
import com.devtool.dto.UuidResponse;
import com.devtool.dto.UuidValidationResponse;
import com.devtool.dto.ValueRequest;
import com.devtool.service.ApiResponseFactory;
import com.devtool.service.UuidToolService;

@RestController
@RequestMapping("/api/uuid")
public class UuidToolController {

	private final UuidToolService uuidToolService;
	private final ApiResponseFactory apiResponseFactory;

	public UuidToolController(UuidToolService uuidToolService, ApiResponseFactory apiResponseFactory) {
		this.uuidToolService = uuidToolService;
		this.apiResponseFactory = apiResponseFactory;
	}

	@GetMapping("/tools")
	public ApiResponse<Map<String, List<String>>> supportedTools() {
		return apiResponseFactory.success(uuidToolService.supportedTools(), "UUID tools fetched successfully");
	}

	@PostMapping("/generate")
	public ApiResponse<UuidResponse> generate() {
		return apiResponseFactory.success(new UuidResponse("Generate UUID", uuidToolService.generate()),
				"UUID generated successfully");
	}

	@PostMapping("/validate")
	public ApiResponse<UuidValidationResponse> validate(@RequestBody ValueRequest request) {
		boolean valid = uuidToolService.isValid(request.value());
		return apiResponseFactory.success(new UuidValidationResponse("Validate UUID", request.value(), valid,
				valid ? uuidToolService.normalize(request.value()) : null), "UUID validation completed successfully");
	}

	@PostMapping("/remove-hyphens")
	public ApiResponse<UuidResponse> removeHyphens(@RequestBody ValueRequest request) {
		return apiResponseFactory.success(new UuidResponse("Remove Hyphens", uuidToolService.removeHyphens(request.value())),
				"UUID hyphens removed successfully");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest()
				.body(apiResponseFactory.badRequest(new ErrorResponse(exception.getMessage()), exception.getMessage()));
	}
}
