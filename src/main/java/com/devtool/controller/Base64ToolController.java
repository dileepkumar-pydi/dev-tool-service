package com.devtool.controller;

import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.devtool.dto.ApiResponse;
import com.devtool.dto.Base64BasicAuthResponse;
import com.devtool.dto.Base64BinaryResponse;
import com.devtool.dto.Base64ContentRequest;
import com.devtool.dto.Base64ValueRequest;
import com.devtool.dto.Base64ValueResponse;
import com.devtool.dto.ErrorResponse;
import com.devtool.service.ApiResponseFactory;
import com.devtool.service.Base64ToolService;
import com.devtool.service.Base64ToolService.BasicAuthParts;
import com.devtool.service.Base64ToolService.DecodedFile;

@RestController
@RequestMapping("/api/base64")
public class Base64ToolController {

	private final Base64ToolService base64ToolService;
	private final ApiResponseFactory apiResponseFactory;

	public Base64ToolController(Base64ToolService base64ToolService, ApiResponseFactory apiResponseFactory) {
		this.base64ToolService = base64ToolService;
		this.apiResponseFactory = apiResponseFactory;
	}

	@GetMapping("/tools")
	public ApiResponse<Map<String, java.util.List<String>>> supportedTools() {
		return apiResponseFactory.success(base64ToolService.supportedTools(), "Base64 tools fetched successfully");
	}

	@PostMapping("/encode/text")
	public ApiResponse<Base64ValueResponse> textToBase64(@RequestBody Base64ValueRequest request) {
		return apiResponseFactory.success(
				new Base64ValueResponse("Text to Base64", base64ToolService.encodeText(request.value())),
				"Text encoded to Base64 successfully");
	}

	@PostMapping("/encode/html")
	public ApiResponse<Base64ValueResponse> htmlToBase64(@RequestBody Base64ValueRequest request) {
		return apiResponseFactory.success(
				new Base64ValueResponse("HTML to Base64", base64ToolService.encodeHtml(request.value())),
				"HTML encoded to Base64 successfully");
	}

	@PostMapping("/encode/css")
	public ApiResponse<Base64ValueResponse> cssToBase64(@RequestBody Base64ValueRequest request) {
		return apiResponseFactory.success(
				new Base64ValueResponse("CSS to Base64", base64ToolService.encodeCss(request.value())),
				"CSS encoded to Base64 successfully");
	}

	@PostMapping("/encode/url")
	public ApiResponse<Base64ValueResponse> urlToBase64(@RequestBody Base64ValueRequest request) {
		return apiResponseFactory.success(
				new Base64ValueResponse("URL to Base64", base64ToolService.encodeUrl(request.value())),
				"URL encoded to Base64 successfully");
	}

	@PostMapping("/encode/hex")
	public ApiResponse<Base64ValueResponse> hexToBase64(@RequestBody Base64ValueRequest request) {
		return apiResponseFactory.success(
				new Base64ValueResponse("Hex to Base64", base64ToolService.encodeHex(request.value())),
				"Hex encoded to Base64 successfully");
	}

	@PostMapping(path = "/encode/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<Base64BinaryResponse> fileToBase64(@RequestParam("file") MultipartFile file) {
		return apiResponseFactory.success(buildBinaryResponse("File to Base64", file, base64ToolService.encodeFile(file)),
				"File encoded to Base64 successfully");
	}

	@PostMapping(path = "/encode/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<Base64BinaryResponse> imageToBase64(@RequestParam("file") MultipartFile file) {
		return apiResponseFactory.success(buildBinaryResponse("Image to Base64", file, base64ToolService.encodeFile(file)),
				"Image encoded to Base64 successfully");
	}

	@PostMapping(path = "/encode/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<Base64BinaryResponse> audioToBase64(@RequestParam("file") MultipartFile file) {
		return apiResponseFactory.success(buildBinaryResponse("Audio to Base64", file, base64ToolService.encodeFile(file)),
				"Audio encoded to Base64 successfully");
	}

	@PostMapping(path = "/encode/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<Base64BinaryResponse> videoToBase64(@RequestParam("file") MultipartFile file) {
		return apiResponseFactory.success(buildBinaryResponse("Video to Base64", file, base64ToolService.encodeFile(file)),
				"Video encoded to Base64 successfully");
	}

	@PostMapping(path = "/encode/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<Base64BinaryResponse> pdfToBase64(@RequestParam("file") MultipartFile file) {
		return apiResponseFactory.success(buildBinaryResponse("PDF to Base64", file, base64ToolService.encodeFile(file)),
				"PDF encoded to Base64 successfully");
	}

	@PostMapping("/decode/text")
	public ApiResponse<Base64ValueResponse> base64ToText(@RequestBody Base64ValueRequest request) {
		return apiResponseFactory.success(
				new Base64ValueResponse("Base64 to Text", base64ToolService.decodeText(request.value())),
				"Base64 decoded to text successfully");
	}

	@PostMapping("/decode/ascii")
	public ApiResponse<Base64ValueResponse> base64ToAscii(@RequestBody Base64ValueRequest request) {
		return apiResponseFactory.success(
				new Base64ValueResponse("Base64 to ASCII", base64ToolService.decodeAscii(request.value())),
				"Base64 decoded to ASCII successfully");
	}

	@PostMapping("/decode/hex")
	public ApiResponse<Base64ValueResponse> base64ToHex(@RequestBody Base64ValueRequest request) {
		return apiResponseFactory.success(
				new Base64ValueResponse("Base64 to Hex", base64ToolService.decodeHex(request.value())),
				"Base64 decoded to hex successfully");
	}

	@PostMapping("/decode/basic-auth")
	public ApiResponse<Base64BasicAuthResponse> decodeBasicAuth(@RequestBody Base64ValueRequest request) {
		BasicAuthParts parts = base64ToolService.decodeBasicAuth(request.value());
		return apiResponseFactory.success(
				new Base64BasicAuthResponse("Basic Auth Decode", parts.username(), parts.password()),
				"Basic auth decoded successfully");
	}

	@PostMapping("/decode/url")
	public ApiResponse<Base64ValueResponse> decodeUrlEncodedText(@RequestBody Base64ValueRequest request) {
		return apiResponseFactory.success(
				new Base64ValueResponse("URL Decode", base64ToolService.decodeUrlEncodedText(request.value())),
				"URL decoded successfully");
	}

	@PostMapping("/decode/file")
	public ResponseEntity<byte[]> base64ToFile(@RequestBody Base64ContentRequest request) {
		return binaryDownload("Base64 to File", request, MediaType.APPLICATION_OCTET_STREAM_VALUE);
	}

	@PostMapping("/decode/image")
	public ResponseEntity<byte[]> base64ToImage(@RequestBody Base64ContentRequest request) {
		return binaryDownload("Base64 to Image", request, MediaType.IMAGE_JPEG_VALUE);
	}

	@PostMapping("/decode/audio")
	public ResponseEntity<byte[]> base64ToAudio(@RequestBody Base64ContentRequest request) {
		return binaryDownload("Base64 to Audio", request, "audio/mpeg");
	}

	@PostMapping("/decode/video")
	public ResponseEntity<byte[]> base64ToVideo(@RequestBody Base64ContentRequest request) {
		return binaryDownload("Base64 to Video", request, "video/mp4");
	}

	@PostMapping("/decode/pdf")
	public ResponseEntity<byte[]> base64ToPdf(@RequestBody Base64ContentRequest request) {
		return binaryDownload("Base64 to PDF", request, MediaType.APPLICATION_PDF_VALUE);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest().body(
				apiResponseFactory.badRequest(new ErrorResponse(exception.getMessage()), exception.getMessage()));
	}

	private Base64BinaryResponse buildBinaryResponse(String tool, MultipartFile file, String base64Value) {
		return new Base64BinaryResponse(tool, file.getOriginalFilename(), file.getContentType(), base64Value);
	}

	private ResponseEntity<byte[]> binaryDownload(String tool, Base64ContentRequest request, String contentType) {
		DecodedFile decodedFile = base64ToolService.decodeBinaryFile(request.base64(), request.fileName(),
				request.contentType(), contentType);
		MediaType mediaType = base64ToolService.resolveMediaType(decodedFile.contentType());
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(mediaType);
		headers.setContentDisposition(ContentDisposition.attachment().filename(decodedFile.fileName()).build());
		headers.add("X-DevTool-Operation", tool);
		return ResponseEntity.ok().headers(headers).body(decodedFile.content());
	}
}
