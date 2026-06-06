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
import com.devtool.dto.CaseConvertRequest;
import com.devtool.dto.DiffResponse;
import com.devtool.dto.ErrorResponse;
import com.devtool.dto.PairTextRequest;
import com.devtool.dto.RegexRequest;
import com.devtool.dto.RegexTestResponse;
import com.devtool.dto.TextCaseResponse;
import com.devtool.dto.TextStatsResponse;
import com.devtool.dto.ValueRequest;
import com.devtool.dto.ValueResponse;
import com.devtool.service.ApiResponseFactory;
import com.devtool.service.TextToolService;

@RestController
@RequestMapping("/api/text")
public class TextToolController {

	private final TextToolService textToolService;
	private final ApiResponseFactory apiResponseFactory;

	public TextToolController(TextToolService textToolService, ApiResponseFactory apiResponseFactory) {
		this.textToolService = textToolService;
		this.apiResponseFactory = apiResponseFactory;
	}

	@GetMapping("/tools")
	public ApiResponse<Map<String, List<String>>> supportedTools() {
		return apiResponseFactory.success(textToolService.supportedTools(), "Text tools fetched successfully");
	}

	@PostMapping("/diff")
	public ApiResponse<DiffResponse> diff(@RequestBody PairTextRequest request) {
		return apiResponseFactory.success(new DiffResponse("Diff Checker", textToolService.diff(request.left(), request.right())),
				"Text diff generated successfully");
	}

	@PostMapping("/case")
	public ApiResponse<TextCaseResponse> convertCase(@RequestBody CaseConvertRequest request) {
		return apiResponseFactory.success(new TextCaseResponse("Case Converter", request.targetCase(),
				textToolService.convertCase(request.value(), request.targetCase())), "Text case converted successfully");
	}

	@PostMapping("/slug")
	public ApiResponse<ValueResponse> slugify(@RequestBody ValueRequest request) {
		return apiResponseFactory.success(new ValueResponse("Slug Generator", textToolService.slugify(request.value())),
				"Slug generated successfully");
	}

	@PostMapping("/regex")
	public ApiResponse<RegexTestResponse> regex(@RequestBody RegexRequest request) {
		List<com.devtool.dto.RegexMatchResponse> matches = textToolService.regexMatches(request.pattern(), request.input(),
				request.caseInsensitive());
		return apiResponseFactory.success(
				new RegexTestResponse("Regex Tester", request.pattern(), !matches.isEmpty(), matches.size(), matches),
				"Regex evaluation completed successfully");
	}

	@PostMapping("/stats")
	public ApiResponse<TextStatsResponse> stats(@RequestBody ValueRequest request) {
		return apiResponseFactory.success(
				new TextStatsResponse("Word/Character Counter", textToolService.charCount(request.value()),
						textToolService.charCountWithoutSpaces(request.value()), textToolService.wordCount(request.value()),
						textToolService.lineCount(request.value())),
				"Text statistics generated successfully");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest()
				.body(apiResponseFactory.badRequest(new ErrorResponse(exception.getMessage()), exception.getMessage()));
	}
}
