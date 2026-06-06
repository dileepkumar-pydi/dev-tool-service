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
import com.devtool.dto.HashRequest;
import com.devtool.dto.HashResultResponse;
import com.devtool.dto.HashVerifyRequest;
import com.devtool.dto.HashVerifyResponse;
import com.devtool.dto.HmacRequest;
import com.devtool.dto.HmacVerifyRequest;
import com.devtool.service.ApiResponseFactory;
import com.devtool.service.HashToolService;

@RestController
@RequestMapping("/api/hash")
public class HashToolController {

	private final HashToolService hashToolService;
	private final ApiResponseFactory apiResponseFactory;

	public HashToolController(HashToolService hashToolService, ApiResponseFactory apiResponseFactory) {
		this.hashToolService = hashToolService;
		this.apiResponseFactory = apiResponseFactory;
	}

	@GetMapping("/tools")
	public ApiResponse<Map<String, List<String>>> supportedTools() {
		return apiResponseFactory.success(hashToolService.supportedTools(), "Hash tools fetched successfully");
	}

	@PostMapping("/digest")
	public ApiResponse<HashResultResponse> digest(@RequestBody HashRequest request) {
		return apiResponseFactory.success(
				new HashResultResponse("Hash Generator", request.algorithm(),
						hashToolService.hash(request.value(), request.algorithm())),
				"Hash generated successfully");
	}

	@PostMapping("/verify")
	public ApiResponse<HashVerifyResponse> verify(@RequestBody HashVerifyRequest request) {
		String actualHash = hashToolService.hash(request.value(), request.algorithm());
		return apiResponseFactory.success(new HashVerifyResponse("Hash Validator", request.algorithm(), request.expectedHash(),
				actualHash, actualHash.equalsIgnoreCase(request.expectedHash())), "Hash validation completed successfully");
	}

	@PostMapping("/hmac/generate")
	public ApiResponse<HashResultResponse> hmac(@RequestBody HmacRequest request) {
		return apiResponseFactory.success(
				new HashResultResponse("HMAC Generator", request.algorithm(),
						hashToolService.hmac(request.value(), request.secret(), request.algorithm())),
				"HMAC generated successfully");
	}

	@PostMapping("/hmac/verify")
	public ApiResponse<HashVerifyResponse> verifyHmac(@RequestBody HmacVerifyRequest request) {
		String actualHash = hashToolService.hmac(request.value(), request.secret(), request.algorithm());
		return apiResponseFactory.success(new HashVerifyResponse("HMAC Validator", request.algorithm(), request.expectedHash(),
				actualHash, actualHash.equalsIgnoreCase(request.expectedHash())), "HMAC validation completed successfully");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest()
				.body(apiResponseFactory.badRequest(new ErrorResponse(exception.getMessage()), exception.getMessage()));
	}
}
