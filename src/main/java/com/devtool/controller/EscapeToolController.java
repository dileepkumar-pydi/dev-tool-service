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
import com.devtool.dto.ValueRequest;
import com.devtool.dto.ValueResponse;
import com.devtool.service.ApiResponseFactory;
import com.devtool.service.EscapeToolService;

@RestController
@RequestMapping("/api/escape")
public class EscapeToolController {

	private final EscapeToolService escapeToolService;
	private final ApiResponseFactory apiResponseFactory;

	public EscapeToolController(EscapeToolService escapeToolService, ApiResponseFactory apiResponseFactory) {
		this.escapeToolService = escapeToolService;
		this.apiResponseFactory = apiResponseFactory;
	}

	@GetMapping("/tools")
	public ApiResponse<Map<String, List<String>>> supportedTools() {
		return apiResponseFactory.success(escapeToolService.supportedTools(), "Escape tools fetched successfully");
	}

	@PostMapping("/url/escape")
	public ApiResponse<ValueResponse> escapeUrl(@RequestBody ValueRequest request) {
		return success("URL Escape", escapeToolService.escapeUrl(request.value()), "URL escaped successfully");
	}

	@PostMapping("/url/unescape")
	public ApiResponse<ValueResponse> unescapeUrl(@RequestBody ValueRequest request) {
		return success("URL Unescape", escapeToolService.unescapeUrl(request.value()), "URL unescaped successfully");
	}

	@PostMapping("/html/escape")
	public ApiResponse<ValueResponse> escapeHtml(@RequestBody ValueRequest request) {
		return success("HTML Escape", escapeToolService.escapeHtml(request.value()), "HTML escaped successfully");
	}

	@PostMapping("/html/unescape")
	public ApiResponse<ValueResponse> unescapeHtml(@RequestBody ValueRequest request) {
		return success("HTML Unescape", escapeToolService.unescapeHtml(request.value()), "HTML unescaped successfully");
	}

	@PostMapping("/json/escape")
	public ApiResponse<ValueResponse> escapeJson(@RequestBody ValueRequest request) {
		return success("JSON Escape", escapeToolService.escapeJson(request.value()), "JSON escaped successfully");
	}

	@PostMapping("/json/unescape")
	public ApiResponse<ValueResponse> unescapeJson(@RequestBody ValueRequest request) {
		return success("JSON Unescape", escapeToolService.unescapeJson(request.value()), "JSON unescaped successfully");
	}

	@PostMapping("/java/escape")
	public ApiResponse<ValueResponse> escapeJava(@RequestBody ValueRequest request) {
		return success("Java Escape", escapeToolService.escapeJava(request.value()), "Java string escaped successfully");
	}

	@PostMapping("/java/unescape")
	public ApiResponse<ValueResponse> unescapeJava(@RequestBody ValueRequest request) {
		return success("Java Unescape", escapeToolService.unescapeJava(request.value()), "Java string unescaped successfully");
	}

	@PostMapping("/javascript/escape")
	public ApiResponse<ValueResponse> escapeJavaScript(@RequestBody ValueRequest request) {
		return success("JavaScript Escape", escapeToolService.escapeJavaScript(request.value()),
				"JavaScript string escaped successfully");
	}

	@PostMapping("/javascript/unescape")
	public ApiResponse<ValueResponse> unescapeJavaScript(@RequestBody ValueRequest request) {
		return success("JavaScript Unescape", escapeToolService.unescapeJavaScript(request.value()),
				"JavaScript string unescaped successfully");
	}

	@PostMapping("/sql/escape")
	public ApiResponse<ValueResponse> escapeSql(@RequestBody ValueRequest request) {
		return success("SQL Escape", escapeToolService.escapeSql(request.value()), "SQL escaped successfully");
	}

	@PostMapping("/sql/unescape")
	public ApiResponse<ValueResponse> unescapeSql(@RequestBody ValueRequest request) {
		return success("SQL Unescape", escapeToolService.unescapeSql(request.value()), "SQL unescaped successfully");
	}

	@PostMapping("/xml/escape")
	public ApiResponse<ValueResponse> escapeXml(@RequestBody ValueRequest request) {
		return success("XML Escape", escapeToolService.escapeXml(request.value()), "XML escaped successfully");
	}

	@PostMapping("/xml/unescape")
	public ApiResponse<ValueResponse> unescapeXml(@RequestBody ValueRequest request) {
		return success("XML Unescape", escapeToolService.unescapeXml(request.value()), "XML unescaped successfully");
	}

	private ApiResponse<ValueResponse> success(String tool, String result, String description) {
		return apiResponseFactory.success(new ValueResponse(tool, result), description);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest()
				.body(apiResponseFactory.badRequest(new ErrorResponse(exception.getMessage()), exception.getMessage()));
	}
}
