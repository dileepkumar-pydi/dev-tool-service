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
import com.devtool.dto.JsonPathRequest;
import com.devtool.dto.JsonPathResponse;
import com.devtool.dto.JsonValidationResponse;
import com.devtool.dto.JsonValueRequest;
import com.devtool.dto.JsonValueResponse;
import com.devtool.service.ApiResponseFactory;
import com.devtool.service.JsonToolService;

@RestController
@RequestMapping("/api/json")
public class JsonToolController {

	private final JsonToolService jsonToolService;
	private final ApiResponseFactory apiResponseFactory;

	public JsonToolController(JsonToolService jsonToolService, ApiResponseFactory apiResponseFactory) {
		this.jsonToolService = jsonToolService;
		this.apiResponseFactory = apiResponseFactory;
	}

	@GetMapping("/tools")
	public ApiResponse<Map<String, List<String>>> supportedTools() {
		return apiResponseFactory.success(jsonToolService.supportedTools(), "JSON tools fetched successfully");
	}

	@PostMapping("/format")
	public ApiResponse<JsonValueResponse> formatJson(@RequestBody JsonValueRequest request) {
		return apiResponseFactory.success(
				new JsonValueResponse("JSON Format", jsonToolService.formatJson(request.value())),
				"JSON formatted successfully");
	}

	@PostMapping("/minify")
	public ApiResponse<JsonValueResponse> minifyJson(@RequestBody JsonValueRequest request) {
		return apiResponseFactory.success(
				new JsonValueResponse("JSON Minify", jsonToolService.minifyJson(request.value())),
				"JSON minified successfully");
	}

	@PostMapping("/validate")
	public ApiResponse<JsonValidationResponse> validateJson(@RequestBody JsonValueRequest request) {
		boolean valid = jsonToolService.isValidJson(request.value());
		return apiResponseFactory.success(
				new JsonValidationResponse("JSON Validate", valid, valid ? "Valid JSON" : "Invalid JSON"),
				"JSON validation completed successfully");
	}

	@PostMapping("/to-yaml")
	public ApiResponse<JsonValueResponse> jsonToYaml(@RequestBody JsonValueRequest request) {
		return apiResponseFactory.success(
				new JsonValueResponse("JSON to YAML", jsonToolService.jsonToYaml(request.value())),
				"JSON converted to YAML successfully");
	}

	@PostMapping("/from-yaml")
	public ApiResponse<JsonValueResponse> yamlToJson(@RequestBody JsonValueRequest request) {
		return apiResponseFactory.success(
				new JsonValueResponse("YAML to JSON", jsonToolService.yamlToJson(request.value())),
				"YAML converted to JSON successfully");
	}

	@PostMapping("/escape")
	public ApiResponse<JsonValueResponse> escapeJson(@RequestBody JsonValueRequest request) {
		return apiResponseFactory.success(
				new JsonValueResponse("JSON Escape", jsonToolService.escapeJson(request.value())),
				"JSON string escaped successfully");
	}

	@PostMapping("/unescape")
	public ApiResponse<JsonValueResponse> unescapeJson(@RequestBody JsonValueRequest request) {
		return apiResponseFactory.success(
				new JsonValueResponse("JSON Unescape", jsonToolService.unescapeJson(request.value())),
				"JSON string unescaped successfully");
	}

	@PostMapping("/path")
	public ApiResponse<JsonPathResponse> jsonPath(@RequestBody JsonPathRequest request) {
		return apiResponseFactory.success(new JsonPathResponse("JSON Path Query", request.path(),
				jsonToolService.queryJsonPath(request.json(), request.path())), "JSON path query completed successfully");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest().body(
				apiResponseFactory.badRequest(new ErrorResponse(exception.getMessage()), exception.getMessage()));
	}
}
