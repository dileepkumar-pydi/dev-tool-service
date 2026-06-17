package com.devtool.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.devtool.dto.PdfBatchResponse;
import com.devtool.dto.PdfFileResultResponse;

@Service
public class PdfToolService {

	public Map<String, List<String>> supportedTools() {
		Map<String, List<String>> tools = new LinkedHashMap<>();
		tools.put("pdfTools", List.of("Merge PDF", "Split PDF", "Compress PDF", "Rotate Pages", "Remove Pages",
				"Reorder Pages", "Page Numbers"));
		return tools;
	}

	public PdfBatchResponse merge(List<MultipartFile> files) {
		if (files == null || files.size() < 2) {
			throw new IllegalArgumentException("At least two PDF files are required for merge.");
		}

		try {
			PDFMergerUtility merger = new PDFMergerUtility();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			merger.setDestinationStream(outputStream);

			for (MultipartFile file : files) {
				validatePdf(file);
				merger.addSource(file.getInputStream());
			}

			merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
			byte[] bytes = outputStream.toByteArray();
			int pageCount;
			try (PDDocument merged = PDDocument.load(bytes)) {
				pageCount = merged.getNumberOfPages();
			}
			return batch("merge", List.of(result("merged-document.pdf", bytes, pageCount)));
		} catch (IOException exception) {
			throw new IllegalArgumentException("Unable to merge uploaded PDF files.", exception);
		}
	}

	public PdfBatchResponse split(MultipartFile file, String mode, String ranges) {
		validatePdf(file);
		try (PDDocument source = PDDocument.load(file.getBytes())) {
			List<PdfFileResultResponse> outputs = new ArrayList<>();
			if ("every-page".equalsIgnoreCase(mode)) {
				for (int pageIndex = 0; pageIndex < source.getNumberOfPages(); pageIndex++) {
					outputs.add(singlePageDocument(source, pageIndex, fileStem(file) + "-page-" + (pageIndex + 1) + ".pdf"));
				}
			} else {
				List<int[]> pageRanges = parseRanges(ranges, source.getNumberOfPages());
				for (int index = 0; index < pageRanges.size(); index++) {
					int[] range = pageRanges.get(index);
					outputs.add(rangeDocument(source, range[0], range[1], fileStem(file) + "-part-" + (index + 1) + ".pdf"));
				}
			}
			return batch("split", outputs);
		} catch (IOException exception) {
			throw new IllegalArgumentException("Unable to split uploaded PDF file.", exception);
		}
	}

	public PdfBatchResponse compress(MultipartFile file, String level) {
		validatePdf(file);
		try (PDDocument source = PDDocument.load(file.getBytes())) {
			source.setAllSecurityToBeRemoved(true);
			byte[] bytes = save(source);
			return batch("compress", List.of(result(fileStem(file) + "-compressed.pdf", bytes, source.getNumberOfPages())));
		} catch (IOException exception) {
			throw new IllegalArgumentException("Unable to compress uploaded PDF file.", exception);
		}
	}

	public PdfBatchResponse rotate(MultipartFile file, int angle) {
		validatePdf(file);
		if (!List.of(90, 180, 270).contains(angle)) {
			throw new IllegalArgumentException("Rotation angle must be 90, 180, or 270.");
		}

		try (PDDocument source = PDDocument.load(file.getBytes())) {
			for (PDPage page : source.getPages()) {
				int current = page.getRotation();
				page.setRotation((current + angle) % 360);
			}
			byte[] bytes = save(source);
			return batch("rotate", List.of(result(fileStem(file) + "-rotated.pdf", bytes, source.getNumberOfPages())));
		} catch (IOException exception) {
			throw new IllegalArgumentException("Unable to rotate uploaded PDF file.", exception);
		}
	}

	public PdfBatchResponse removePages(MultipartFile file, String pages) {
		validatePdf(file);
		try (PDDocument source = PDDocument.load(file.getBytes())) {
			Set<Integer> pageNumbers = parsePageNumbers(pages, source.getNumberOfPages());
			if (pageNumbers.size() >= source.getNumberOfPages()) {
				throw new IllegalArgumentException("At least one page must remain after removal.");
			}

			List<Integer> descending = pageNumbers.stream().sorted((left, right) -> right.compareTo(left)).toList();
			for (Integer pageNumber : descending) {
				source.removePage(pageNumber - 1);
			}

			byte[] bytes = save(source);
			return batch("remove-pages",
					List.of(result(fileStem(file) + "-pages-removed.pdf", bytes, source.getNumberOfPages())));
		} catch (IOException exception) {
			throw new IllegalArgumentException("Unable to remove pages from uploaded PDF file.", exception);
		}
	}

	public PdfBatchResponse reorderPages(MultipartFile file, String order) {
		validatePdf(file);
		try (PDDocument source = PDDocument.load(file.getBytes()); PDDocument reordered = new PDDocument()) {
			List<Integer> pageOrder = parsePageOrder(order, source.getNumberOfPages());
			for (Integer pageNumber : pageOrder) {
				reordered.importPage(source.getPage(pageNumber - 1));
			}

			byte[] bytes = save(reordered);
			return batch("reorder-pages",
					List.of(result(fileStem(file) + "-reordered.pdf", bytes, reordered.getNumberOfPages())));
		} catch (IOException exception) {
			throw new IllegalArgumentException("Unable to reorder pages for uploaded PDF file.", exception);
		}
	}

	public PdfBatchResponse addPageNumbers(MultipartFile file, String position, int startNumber) {
		validatePdf(file);
		if (startNumber < 0) {
			throw new IllegalArgumentException("Start number must be zero or greater.");
		}

		try (PDDocument source = PDDocument.load(file.getBytes())) {
			for (int pageIndex = 0; pageIndex < source.getNumberOfPages(); pageIndex++) {
				PDPage page = source.getPage(pageIndex);
				String text = String.valueOf(startNumber + pageIndex);
				addPageNumber(source, page, text, position);
			}

			byte[] bytes = save(source);
			return batch("page-numbers",
					List.of(result(fileStem(file) + "-numbered.pdf", bytes, source.getNumberOfPages())));
		} catch (IOException exception) {
			throw new IllegalArgumentException("Unable to add page numbers to uploaded PDF file.", exception);
		}
	}

	private PdfBatchResponse batch(String operation, List<PdfFileResultResponse> files) {
		return new PdfBatchResponse(operation, files);
	}

	private PdfFileResultResponse singlePageDocument(PDDocument source, int pageIndex, String fileName) throws IOException {
		try (PDDocument output = new PDDocument()) {
			output.importPage(source.getPage(pageIndex));
			byte[] bytes = save(output);
			return result(fileName, bytes, output.getNumberOfPages());
		}
	}

	private PdfFileResultResponse rangeDocument(PDDocument source, int startPage, int endPage, String fileName) throws IOException {
		try (PDDocument output = new PDDocument()) {
			for (int pageIndex = startPage - 1; pageIndex < endPage; pageIndex++) {
				output.importPage(source.getPage(pageIndex));
			}
			byte[] bytes = save(output);
			return result(fileName, bytes, output.getNumberOfPages());
		}
	}

	private PdfFileResultResponse result(String fileName, byte[] bytes, int pageCount) {
		return new PdfFileResultResponse(fileName, "application/pdf", bytes.length, pageCount,
				Base64.getEncoder().encodeToString(bytes));
	}

	private byte[] save(PDDocument document) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		document.save(outputStream);
		return outputStream.toByteArray();
	}

	private void validatePdf(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("A PDF file is required.");
		}

		String fileName = file.getOriginalFilename();
		if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
			throw new IllegalArgumentException("Only PDF files are supported.");
		}
	}

	private String fileStem(MultipartFile file) {
		String fileName = file.getOriginalFilename();
		if (fileName == null) {
			return "document";
		}
		return fileName.replaceFirst("(?i)\\.pdf$", "");
	}

	private List<int[]> parseRanges(String ranges, int pageCount) {
		if (ranges == null || ranges.isBlank()) {
			throw new IllegalArgumentException("Page ranges are required for range split mode.");
		}

		List<int[]> parsed = new ArrayList<>();
		for (String segment : ranges.split(",")) {
			String value = segment.trim();
			if (value.isBlank()) {
				continue;
			}
			if (value.contains("-")) {
				String[] parts = value.split("-");
				if (parts.length != 2) {
					throw new IllegalArgumentException("Invalid page range: " + value);
				}
				int start = parsePage(parts[0].trim(), pageCount);
				int end = parsePage(parts[1].trim(), pageCount);
				if (start > end) {
					throw new IllegalArgumentException("Invalid page range: " + value);
				}
				parsed.add(new int[] { start, end });
			} else {
				int page = parsePage(value, pageCount);
				parsed.add(new int[] { page, page });
			}
		}

		if (parsed.isEmpty()) {
			throw new IllegalArgumentException("At least one valid page range is required.");
		}
		return parsed;
	}

	private Set<Integer> parsePageNumbers(String pages, int pageCount) {
		Set<Integer> numbers = new TreeSet<>();
		for (int[] range : parseRanges(pages, pageCount)) {
			for (int page = range[0]; page <= range[1]; page++) {
				numbers.add(page);
			}
		}
		return numbers;
	}

	private List<Integer> parsePageOrder(String order, int pageCount) {
		if (order == null || order.isBlank()) {
			throw new IllegalArgumentException("Page order is required.");
		}

		List<Integer> values = new ArrayList<>();
		for (String segment : order.split(",")) {
			String value = segment.trim();
			if (!value.isBlank()) {
				values.add(parsePage(value, pageCount));
			}
		}

		if (values.size() != pageCount || Set.copyOf(values).size() != pageCount) {
			throw new IllegalArgumentException("Page order must include each page exactly once.");
		}
		return values;
	}

	private int parsePage(String value, int pageCount) {
		try {
			int page = Integer.parseInt(value);
			if (page < 1 || page > pageCount) {
				throw new IllegalArgumentException("Page number out of range: " + value);
			}
			return page;
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("Invalid page number: " + value, exception);
		}
	}

	private void addPageNumber(PDDocument document, PDPage page, String text, String position) throws IOException {
		PDRectangle mediaBox = page.getMediaBox();
		float fontSize = 12f;
		float margin = 24f;
		float textWidth = PDType1Font.HELVETICA.getStringWidth(text) / 1000 * fontSize;
		float x = switch (position.toLowerCase()) {
		case "top-left", "bottom-left" -> margin;
		case "top-right", "bottom-right" -> mediaBox.getWidth() - textWidth - margin;
		default -> throw new IllegalArgumentException("Unsupported page number position: " + position);
		};
		float y = switch (position.toLowerCase()) {
		case "top-left", "top-right" -> mediaBox.getHeight() - margin;
		case "bottom-left", "bottom-right" -> margin;
		default -> throw new IllegalArgumentException("Unsupported page number position: " + position);
		};

		try (PDPageContentStream contentStream = new PDPageContentStream(document, page,
				PDPageContentStream.AppendMode.APPEND, true, true)) {
			contentStream.beginText();
			contentStream.setFont(PDType1Font.HELVETICA, fontSize);
			contentStream.newLineAtOffset(x, y);
			contentStream.showText(text);
			contentStream.endText();
		}
	}
}
