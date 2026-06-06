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

import com.devtool.dto.Base64BasicAuthResponse;
import com.devtool.dto.Base64BinaryResponse;
import com.devtool.dto.Base64ContentRequest;
import com.devtool.dto.Base64ValueRequest;
import com.devtool.dto.Base64ValueResponse;
import com.devtool.service.Base64ToolService;
import com.devtool.service.Base64ToolService.BasicAuthParts;
import com.devtool.service.Base64ToolService.DecodedFile;

@RestController
@RequestMapping("/api/base64")
public class Base64ToolController {

	private final Base64ToolService base64ToolService;

	public Base64ToolController(Base64ToolService base64ToolService) {
		this.base64ToolService = base64ToolService;
	}

	@GetMapping("/tools")
	public Map<String, java.util.List<String>> supportedTools() {
		return base64ToolService.supportedTools();
	}

	@PostMapping("/encode/text")
	public Base64ValueResponse textToBase64(@RequestBody Base64ValueRequest request) {
		return new Base64ValueResponse("Text to Base64", base64ToolService.encodeText(request.value()));
	}

	@PostMapping("/encode/html")
	public Base64ValueResponse htmlToBase64(@RequestBody Base64ValueRequest request) {
		return new Base64ValueResponse("HTML to Base64", base64ToolService.encodeHtml(request.value()));
	}

	@PostMapping("/encode/css")
	public Base64ValueResponse cssToBase64(@RequestBody Base64ValueRequest request) {
		return new Base64ValueResponse("CSS to Base64", base64ToolService.encodeCss(request.value()));
	}

	@PostMapping("/encode/url")
	public Base64ValueResponse urlToBase64(@RequestBody Base64ValueRequest request) {
		return new Base64ValueResponse("URL to Base64", base64ToolService.encodeUrl(request.value()));
	}

	@PostMapping("/encode/hex")
	public Base64ValueResponse hexToBase64(@RequestBody Base64ValueRequest request) {
		return new Base64ValueResponse("Hex to Base64", base64ToolService.encodeHex(request.value()));
	}

	@PostMapping(path = "/encode/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Base64BinaryResponse fileToBase64(@RequestParam("file") MultipartFile file) {
		return buildBinaryResponse("File to Base64", file, base64ToolService.encodeFile(file));
	}

	@PostMapping(path = "/encode/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Base64BinaryResponse imageToBase64(@RequestParam("file") MultipartFile file) {
		return buildBinaryResponse("Image to Base64", file, base64ToolService.encodeFile(file));
	}

	@PostMapping(path = "/encode/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Base64BinaryResponse audioToBase64(@RequestParam("file") MultipartFile file) {
		return buildBinaryResponse("Audio to Base64", file, base64ToolService.encodeFile(file));
	}

	@PostMapping(path = "/encode/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Base64BinaryResponse videoToBase64(@RequestParam("file") MultipartFile file) {
		return buildBinaryResponse("Video to Base64", file, base64ToolService.encodeFile(file));
	}

	@PostMapping(path = "/encode/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Base64BinaryResponse pdfToBase64(@RequestParam("file") MultipartFile file) {
		return buildBinaryResponse("PDF to Base64", file, base64ToolService.encodeFile(file));
	}

	@PostMapping("/decode/text")
	public Base64ValueResponse base64ToText(@RequestBody Base64ValueRequest request) {
		return new Base64ValueResponse("Base64 to Text", base64ToolService.decodeText(request.value()));
	}

	@PostMapping("/decode/ascii")
	public Base64ValueResponse base64ToAscii(@RequestBody Base64ValueRequest request) {
		return new Base64ValueResponse("Base64 to ASCII", base64ToolService.decodeAscii(request.value()));
	}

	@PostMapping("/decode/hex")
	public Base64ValueResponse base64ToHex(@RequestBody Base64ValueRequest request) {
		return new Base64ValueResponse("Base64 to Hex", base64ToolService.decodeHex(request.value()));
	}

	@PostMapping("/decode/basic-auth")
	public Base64BasicAuthResponse decodeBasicAuth(@RequestBody Base64ValueRequest request) {
		BasicAuthParts parts = base64ToolService.decodeBasicAuth(request.value());
		return new Base64BasicAuthResponse("Basic Auth Decode", parts.username(), parts.password());
	}

	@PostMapping("/decode/url")
	public Base64ValueResponse decodeUrlEncodedText(@RequestBody Base64ValueRequest request) {
		return new Base64ValueResponse("URL Decode", base64ToolService.decodeUrlEncodedText(request.value()));
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
	public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
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
