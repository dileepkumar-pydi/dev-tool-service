package com.devtool.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.devtool.dto.ApiResponse;
import com.devtool.dto.ErrorResponse;
import com.devtool.dto.FileMetadataResponse;
import com.devtool.service.ApiResponseFactory;
import com.devtool.service.FileToolService;

@RestController
@RequestMapping("/api/file")
public class FileToolController {

	private final FileToolService fileToolService;
	private final ApiResponseFactory apiResponseFactory;

	public FileToolController(FileToolService fileToolService, ApiResponseFactory apiResponseFactory) {
		this.fileToolService = fileToolService;
		this.apiResponseFactory = apiResponseFactory;
	}

	@GetMapping("/tools")
	public ApiResponse<Map<String, List<String>>> supportedTools() {
		return apiResponseFactory.success(fileToolService.supportedTools(), "File tools fetched successfully");
	}

	@PostMapping(path = "/inspect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<FileMetadataResponse> inspect(@RequestParam("file") MultipartFile file) {
		return apiResponseFactory.success(fileToolService.inspect(file), "File metadata generated successfully");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest()
				.body(apiResponseFactory.badRequest(new ErrorResponse(exception.getMessage()), exception.getMessage()));
	}
}
