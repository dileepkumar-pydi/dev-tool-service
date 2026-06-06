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
import com.devtool.dto.BaseConversionResponse;
import com.devtool.dto.BaseConvertRequest;
import com.devtool.dto.CodePointResponse;
import com.devtool.dto.ErrorResponse;
import com.devtool.dto.ValueRequest;
import com.devtool.service.ApiResponseFactory;
import com.devtool.service.NumberBaseToolService;

@RestController
@RequestMapping("/api/number-base")
public class NumberBaseToolController {

	private final NumberBaseToolService numberBaseToolService;
	private final ApiResponseFactory apiResponseFactory;

	public NumberBaseToolController(NumberBaseToolService numberBaseToolService, ApiResponseFactory apiResponseFactory) {
		this.numberBaseToolService = numberBaseToolService;
		this.apiResponseFactory = apiResponseFactory;
	}

	@GetMapping("/tools")
	public ApiResponse<Map<String, List<String>>> supportedTools() {
		return apiResponseFactory.success(numberBaseToolService.supportedTools(), "Number/base tools fetched successfully");
	}

	@PostMapping("/convert")
	public ApiResponse<BaseConversionResponse> convert(@RequestBody BaseConvertRequest request) {
		return apiResponseFactory.success(
				new BaseConversionResponse("Base Converter", request.value(), request.sourceBase(), request.targetBase(),
						numberBaseToolService.convertBase(request.value(), request.sourceBase(), request.targetBase())),
				"Base conversion completed successfully");
	}

	@PostMapping("/ascii/to-codes")
	public ApiResponse<CodePointResponse> textToAscii(@RequestBody ValueRequest request) {
		return apiResponseFactory.success(new CodePointResponse("ASCII Helper", request.value(),
				numberBaseToolService.textToAsciiCodes(request.value())), "ASCII codes generated successfully");
	}

	@PostMapping("/ascii/to-text")
	public ApiResponse<CodePointResponse> asciiToText(@RequestBody ValueRequest request) {
		return apiResponseFactory.success(new CodePointResponse("ASCII Helper", request.value(),
				numberBaseToolService.asciiCodesToText(request.value())), "ASCII text restored successfully");
	}

	@PostMapping("/unicode/to-codes")
	public ApiResponse<CodePointResponse> textToUnicode(@RequestBody ValueRequest request) {
		return apiResponseFactory.success(new CodePointResponse("Unicode Helper", request.value(),
				numberBaseToolService.textToUnicodeCodes(request.value())), "Unicode codes generated successfully");
	}

	@PostMapping("/unicode/to-text")
	public ApiResponse<CodePointResponse> unicodeToText(@RequestBody ValueRequest request) {
		return apiResponseFactory.success(new CodePointResponse("Unicode Helper", request.value(),
				numberBaseToolService.unicodeCodesToText(request.value())), "Unicode text restored successfully");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest()
				.body(apiResponseFactory.badRequest(new ErrorResponse(exception.getMessage()), exception.getMessage()));
	}
}
