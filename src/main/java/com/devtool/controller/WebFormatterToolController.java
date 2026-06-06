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
import com.devtool.service.WebFormatterToolService;

@RestController
@RequestMapping("/api/web")
public class WebFormatterToolController {

	private final WebFormatterToolService webFormatterToolService;
	private final ApiResponseFactory apiResponseFactory;

	public WebFormatterToolController(WebFormatterToolService webFormatterToolService,
			ApiResponseFactory apiResponseFactory) {
		this.webFormatterToolService = webFormatterToolService;
		this.apiResponseFactory = apiResponseFactory;
	}

	@GetMapping("/tools")
	public ApiResponse<Map<String, List<String>>> supportedTools() {
		return apiResponseFactory.success(webFormatterToolService.supportedTools(), "Web formatter tools fetched successfully");
	}

	@PostMapping("/html/beautify")
	public ApiResponse<ValueResponse> beautifyHtml(@RequestBody ValueRequest request) {
		return success("HTML Beautify", webFormatterToolService.beautifyHtml(request.value()),
				"HTML beautified successfully");
	}

	@PostMapping("/html/minify")
	public ApiResponse<ValueResponse> minifyHtml(@RequestBody ValueRequest request) {
		return success("HTML Minify", webFormatterToolService.minifyHtml(request.value()), "HTML minified successfully");
	}

	@PostMapping("/html/escape")
	public ApiResponse<ValueResponse> escapeHtml(@RequestBody ValueRequest request) {
		return success("HTML Escape", webFormatterToolService.escapeHtml(request.value()), "HTML escaped successfully");
	}

	@PostMapping("/html/unescape")
	public ApiResponse<ValueResponse> unescapeHtml(@RequestBody ValueRequest request) {
		return success("HTML Unescape", webFormatterToolService.unescapeHtml(request.value()), "HTML unescaped successfully");
	}

	@PostMapping("/css/beautify")
	public ApiResponse<ValueResponse> beautifyCss(@RequestBody ValueRequest request) {
		return success("CSS Beautify", webFormatterToolService.beautifyCss(request.value()), "CSS beautified successfully");
	}

	@PostMapping("/css/minify")
	public ApiResponse<ValueResponse> minifyCss(@RequestBody ValueRequest request) {
		return success("CSS Minify", webFormatterToolService.minifyCss(request.value()), "CSS minified successfully");
	}

	@PostMapping("/js/beautify")
	public ApiResponse<ValueResponse> beautifyJs(@RequestBody ValueRequest request) {
		return success("JavaScript Beautify", webFormatterToolService.beautifyJs(request.value()),
				"JavaScript beautified successfully");
	}

	@PostMapping("/js/minify")
	public ApiResponse<ValueResponse> minifyJs(@RequestBody ValueRequest request) {
		return success("JavaScript Minify", webFormatterToolService.minifyJs(request.value()),
				"JavaScript minified successfully");
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
