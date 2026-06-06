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
import com.devtool.dto.ColorConversionResponse;
import com.devtool.dto.ErrorResponse;
import com.devtool.dto.GradientRequest;
import com.devtool.dto.GradientResponse;
import com.devtool.dto.HslColorRequest;
import com.devtool.dto.RgbColorRequest;
import com.devtool.dto.ValueRequest;
import com.devtool.service.ApiResponseFactory;
import com.devtool.service.ColorToolService;

@RestController
@RequestMapping("/api/color")
public class ColorToolController {

	private final ColorToolService colorToolService;
	private final ApiResponseFactory apiResponseFactory;

	public ColorToolController(ColorToolService colorToolService, ApiResponseFactory apiResponseFactory) {
		this.colorToolService = colorToolService;
		this.apiResponseFactory = apiResponseFactory;
	}

	@GetMapping("/tools")
	public ApiResponse<Map<String, List<String>>> supportedTools() {
		return apiResponseFactory.success(colorToolService.supportedTools(), "Color tools fetched successfully");
	}

	@PostMapping("/from-hex")
	public ApiResponse<ColorConversionResponse> fromHex(@RequestBody ValueRequest request) {
		return apiResponseFactory.success(colorToolService.fromHex(request.value()), "HEX color converted successfully");
	}

	@PostMapping("/from-rgb")
	public ApiResponse<ColorConversionResponse> fromRgb(@RequestBody RgbColorRequest request) {
		return apiResponseFactory.success(colorToolService.fromRgb(request.red(), request.green(), request.blue()),
				"RGB color converted successfully");
	}

	@PostMapping("/from-hsl")
	public ApiResponse<ColorConversionResponse> fromHsl(@RequestBody HslColorRequest request) {
		return apiResponseFactory.success(
				colorToolService.fromHsl(request.hue(), request.saturation(), request.lightness()),
				"HSL color converted successfully");
	}

	@PostMapping("/gradient")
	public ApiResponse<GradientResponse> gradient(@RequestBody GradientRequest request) {
		return apiResponseFactory.success(colorToolService.gradient(request.startHex(), request.endHex(), request.steps()),
				"Gradient generated successfully");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest()
				.body(apiResponseFactory.badRequest(new ErrorResponse(exception.getMessage()), exception.getMessage()));
	}
}
