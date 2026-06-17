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
import com.devtool.dto.PdfBatchResponse;
import com.devtool.service.ApiResponseFactory;
import com.devtool.service.PdfToolService;

@RestController
@RequestMapping("/api/pdf")
public class PdfToolController {

	private final PdfToolService pdfToolService;
	private final ApiResponseFactory apiResponseFactory;

	public PdfToolController(PdfToolService pdfToolService, ApiResponseFactory apiResponseFactory) {
		this.pdfToolService = pdfToolService;
		this.apiResponseFactory = apiResponseFactory;
	}

	@GetMapping("/tools")
	public ApiResponse<Map<String, List<String>>> supportedTools() {
		return apiResponseFactory.success(pdfToolService.supportedTools(), "PDF tools fetched successfully");
	}

	@PostMapping(path = "/merge", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<PdfBatchResponse> merge(@RequestParam("files") List<MultipartFile> files) {
		return apiResponseFactory.success(pdfToolService.merge(files), "PDF merged successfully");
	}

	@PostMapping(path = "/split", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<PdfBatchResponse> split(@RequestParam("file") MultipartFile file,
			@RequestParam(name = "mode", defaultValue = "range") String mode,
			@RequestParam(name = "ranges", required = false) String ranges) {
		return apiResponseFactory.success(pdfToolService.split(file, mode, ranges), "PDF split successfully");
	}

	@PostMapping(path = "/compress", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<PdfBatchResponse> compress(@RequestParam("file") MultipartFile file,
			@RequestParam(name = "level", defaultValue = "balanced") String level) {
		return apiResponseFactory.success(pdfToolService.compress(file, level), "PDF compressed successfully");
	}

	@PostMapping(path = "/rotate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<PdfBatchResponse> rotate(@RequestParam("file") MultipartFile file,
			@RequestParam(name = "angle", defaultValue = "90") int angle) {
		return apiResponseFactory.success(pdfToolService.rotate(file, angle), "PDF rotated successfully");
	}

	@PostMapping(path = "/remove-pages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<PdfBatchResponse> removePages(@RequestParam("file") MultipartFile file,
			@RequestParam("pages") String pages) {
		return apiResponseFactory.success(pdfToolService.removePages(file, pages), "PDF pages removed successfully");
	}

	@PostMapping(path = "/reorder-pages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<PdfBatchResponse> reorderPages(@RequestParam("file") MultipartFile file,
			@RequestParam("order") String order) {
		return apiResponseFactory.success(pdfToolService.reorderPages(file, order), "PDF pages reordered successfully");
	}

	@PostMapping(path = "/page-numbers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<PdfBatchResponse> pageNumbers(@RequestParam("file") MultipartFile file,
			@RequestParam(name = "position", defaultValue = "bottom-right") String position,
			@RequestParam(name = "startNumber", defaultValue = "1") int startNumber) {
		return apiResponseFactory.success(pdfToolService.addPageNumbers(file, position, startNumber),
				"PDF page numbers added successfully");
	}

	@PostMapping(path = "/jpg-to-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<PdfBatchResponse> jpgToPdf(@RequestParam("files") List<MultipartFile> files) {
		return apiResponseFactory.success(pdfToolService.jpgToPdf(files), "JPG converted to PDF successfully");
	}

	@PostMapping(path = "/pdf-to-jpg", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<PdfBatchResponse> pdfToJpg(@RequestParam("file") MultipartFile file,
			@RequestParam(name = "dpi", defaultValue = "144") int dpi) {
		return apiResponseFactory.success(pdfToolService.pdfToJpg(file, dpi), "PDF converted to JPG successfully");
	}

	@PostMapping(path = "/html-to-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<PdfBatchResponse> htmlToPdf(@RequestParam("file") MultipartFile file) {
		return apiResponseFactory.success(pdfToolService.htmlToPdf(file), "HTML converted to PDF successfully");
	}

	@PostMapping(path = "/pdf-to-pdfa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<PdfBatchResponse> pdfToPdfA(@RequestParam("file") MultipartFile file) {
		return apiResponseFactory.success(pdfToolService.pdfToPdfA(file), "PDF converted to PDF/A successfully");
	}

	@PostMapping(path = "/word-to-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<PdfBatchResponse> wordToPdf(@RequestParam("file") MultipartFile file) {
		return apiResponseFactory.success(pdfToolService.wordToPdf(file), "Word converted to PDF successfully");
	}

	@PostMapping(path = "/powerpoint-to-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<PdfBatchResponse> powerpointToPdf(@RequestParam("file") MultipartFile file) {
		return apiResponseFactory.success(pdfToolService.powerpointToPdf(file), "PowerPoint converted to PDF successfully");
	}

	@PostMapping(path = "/excel-to-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<PdfBatchResponse> excelToPdf(@RequestParam("file") MultipartFile file) {
		return apiResponseFactory.success(pdfToolService.excelToPdf(file), "Excel converted to PDF successfully");
	}

	@PostMapping(path = "/pdf-to-word", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<PdfBatchResponse> pdfToWord(@RequestParam("file") MultipartFile file) {
		return apiResponseFactory.success(pdfToolService.pdfToWord(file), "PDF converted to Word successfully");
	}

	@PostMapping(path = "/pdf-to-powerpoint", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<PdfBatchResponse> pdfToPowerpoint(@RequestParam("file") MultipartFile file) {
		return apiResponseFactory.success(pdfToolService.pdfToPowerpoint(file), "PDF converted to PowerPoint successfully");
	}

	@PostMapping(path = "/pdf-to-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<PdfBatchResponse> pdfToExcel(@RequestParam("file") MultipartFile file) {
		return apiResponseFactory.success(pdfToolService.pdfToExcel(file), "PDF converted to Excel successfully");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<ErrorResponse>> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest()
				.body(apiResponseFactory.badRequest(new ErrorResponse(exception.getMessage()), exception.getMessage()));
	}
}
