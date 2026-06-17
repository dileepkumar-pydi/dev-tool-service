package com.devtool.service;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.xml.XmpSerializer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.devtool.dto.PdfBatchResponse;
import com.devtool.dto.PdfFileResultResponse;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

@Service
public class PdfToolService {

	private static final Set<String> PDF_EXTENSIONS = Set.of("pdf");
	private static final Set<String> JPG_EXTENSIONS = Set.of("jpg", "jpeg");
	private static final Set<String> HTML_EXTENSIONS = Set.of("html", "htm");
	private static final Set<String> WORD_EXTENSIONS = Set.of("doc", "docx");
	private static final Set<String> POWERPOINT_EXTENSIONS = Set.of("ppt", "pptx");
	private static final Set<String> EXCEL_EXTENSIONS = Set.of("xls", "xlsx");

	public Map<String, List<String>> supportedTools() {
		Map<String, List<String>> tools = new LinkedHashMap<>();
		tools.put("pdfTools", List.of("Merge PDF", "Split PDF", "Compress PDF", "Rotate Pages", "Remove Pages",
				"Reorder Pages", "Page Numbers", "JPG to PDF", "PDF to JPG", "Word to PDF", "PowerPoint to PDF",
				"Excel to PDF", "HTML to PDF", "PDF to Word", "PDF to PowerPoint", "PDF to Excel", "PDF to PDF/A"));
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
				validateFile(file, PDF_EXTENSIONS, "PDF");
				merger.addSource(file.getInputStream());
			}

			merger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
			byte[] bytes = outputStream.toByteArray();
			return batch("merge", List.of(result("merged-document.pdf", "application/pdf", bytes, pageCount(bytes))));
		} catch (IOException exception) {
			throw new IllegalArgumentException("Unable to merge uploaded PDF files.", exception);
		}
	}

	public PdfBatchResponse split(MultipartFile file, String mode, String ranges) {
		validateFile(file, PDF_EXTENSIONS, "PDF");
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
		validateFile(file, PDF_EXTENSIONS, "PDF");
		try (PDDocument source = PDDocument.load(file.getBytes())) {
			source.setAllSecurityToBeRemoved(true);
			byte[] bytes = save(source);
			return batch("compress", List.of(result(fileStem(file) + "-compressed.pdf", "application/pdf", bytes,
					source.getNumberOfPages())));
		} catch (IOException exception) {
			throw new IllegalArgumentException("Unable to compress uploaded PDF file.", exception);
		}
	}

	public PdfBatchResponse rotate(MultipartFile file, int angle) {
		validateFile(file, PDF_EXTENSIONS, "PDF");
		if (!List.of(90, 180, 270).contains(angle)) {
			throw new IllegalArgumentException("Rotation angle must be 90, 180, or 270.");
		}

		try (PDDocument source = PDDocument.load(file.getBytes())) {
			for (PDPage page : source.getPages()) {
				int current = page.getRotation();
				page.setRotation((current + angle) % 360);
			}
			byte[] bytes = save(source);
			return batch("rotate", List.of(result(fileStem(file) + "-rotated.pdf", "application/pdf", bytes,
					source.getNumberOfPages())));
		} catch (IOException exception) {
			throw new IllegalArgumentException("Unable to rotate uploaded PDF file.", exception);
		}
	}

	public PdfBatchResponse removePages(MultipartFile file, String pages) {
		validateFile(file, PDF_EXTENSIONS, "PDF");
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
			return batch("remove-pages", List.of(result(fileStem(file) + "-pages-removed.pdf", "application/pdf", bytes,
					source.getNumberOfPages())));
		} catch (IOException exception) {
			throw new IllegalArgumentException("Unable to remove pages from uploaded PDF file.", exception);
		}
	}

	public PdfBatchResponse reorderPages(MultipartFile file, String order) {
		validateFile(file, PDF_EXTENSIONS, "PDF");
		try (PDDocument source = PDDocument.load(file.getBytes()); PDDocument reordered = new PDDocument()) {
			List<Integer> pageOrder = parsePageOrder(order, source.getNumberOfPages());
			for (Integer pageNumber : pageOrder) {
				reordered.importPage(source.getPage(pageNumber - 1));
			}

			byte[] bytes = save(reordered);
			return batch("reorder-pages", List.of(result(fileStem(file) + "-reordered.pdf", "application/pdf", bytes,
					reordered.getNumberOfPages())));
		} catch (IOException exception) {
			throw new IllegalArgumentException("Unable to reorder pages for uploaded PDF file.", exception);
		}
	}

	public PdfBatchResponse addPageNumbers(MultipartFile file, String position, int startNumber) {
		validateFile(file, PDF_EXTENSIONS, "PDF");
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
			return batch("page-numbers", List.of(result(fileStem(file) + "-numbered.pdf", "application/pdf", bytes,
					source.getNumberOfPages())));
		} catch (IOException exception) {
			throw new IllegalArgumentException("Unable to add page numbers to uploaded PDF file.", exception);
		}
	}

	public PdfBatchResponse jpgToPdf(List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			throw new IllegalArgumentException("At least one JPG file is required.");
		}

		try (PDDocument document = new PDDocument()) {
			for (MultipartFile file : files) {
				validateFile(file, JPG_EXTENSIONS, "JPG");
				BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
				if (image == null) {
					throw new IllegalArgumentException("Unable to read uploaded JPG file: " + file.getOriginalFilename());
				}

				PDPage page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
				document.addPage(page);
				PDImageXObject pdImage = LosslessFactory.createFromImage(document, image);
				try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
					contentStream.drawImage(pdImage, 0, 0, image.getWidth(), image.getHeight());
				}
			}

			byte[] bytes = save(document);
			return batch("jpg-to-pdf",
					List.of(result("images-to-pdf.pdf", "application/pdf", bytes, document.getNumberOfPages())));
		} catch (IOException exception) {
			throw new IllegalArgumentException("Unable to convert JPG to PDF.", exception);
		}
	}

	public PdfBatchResponse pdfToJpg(MultipartFile file, int dpi) {
		validateFile(file, PDF_EXTENSIONS, "PDF");
		if (dpi < 72 || dpi > 300) {
			throw new IllegalArgumentException("DPI must be between 72 and 300.");
		}

		try (PDDocument document = PDDocument.load(file.getBytes())) {
			PDFRenderer renderer = new PDFRenderer(document);
			List<PdfFileResultResponse> outputs = new ArrayList<>();

			for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
				BufferedImage image = renderer.renderImageWithDPI(pageIndex, dpi);
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				ImageIO.write(image, "jpg", outputStream);
				outputs.add(result(fileStem(file) + "-page-" + (pageIndex + 1) + ".jpg", "image/jpeg",
						outputStream.toByteArray(), 1));
			}

			return batch("pdf-to-jpg", outputs);
		} catch (IOException exception) {
			throw new IllegalArgumentException("Unable to convert PDF to JPG.", exception);
		}
	}

	public PdfBatchResponse htmlToPdf(MultipartFile file) {
		validateFile(file, HTML_EXTENSIONS, "HTML");
		String html = readUtf8(file);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		try {
			PdfRendererBuilder builder = new PdfRendererBuilder();
			builder.useFastMode();
			builder.withHtmlContent(html, null);
			builder.toStream(outputStream);
			builder.run();
			byte[] bytes = outputStream.toByteArray();
			return batch("html-to-pdf", List.of(result(fileStem(file) + ".pdf", "application/pdf", bytes, pageCount(bytes))));
		} catch (Exception exception) {
			throw new IllegalArgumentException("Unable to convert HTML to PDF.", exception);
		}
	}

	public PdfBatchResponse pdfToPdfA(MultipartFile file) {
		validateFile(file, PDF_EXTENSIONS, "PDF");

		try (PDDocument document = PDDocument.load(file.getBytes())) {
			PDDocumentInformation information = document.getDocumentInformation();
			information.setProducer("DevTools PDF Service");
			information.setTitle(fileStem(file));

			PDDocumentCatalog catalog = document.getDocumentCatalog();
			XMPMetadata xmp = XMPMetadata.createXMPMetadata();
			PDFAIdentificationSchema pdfaSchema = xmp.createAndAddPDFAIdentificationSchema();
			pdfaSchema.setPart(1);
			pdfaSchema.setConformance("B");

			ByteArrayOutputStream metadataStream = new ByteArrayOutputStream();
			new XmpSerializer().serialize(xmp, metadataStream, true);
			PDMetadata metadata = new PDMetadata(document);
			metadata.importXMPMetadata(metadataStream.toByteArray());
			catalog.setMetadata(metadata);

			byte[] iccBytes = ICC_Profile.getInstance(ColorSpace.CS_sRGB).getData();
			PDOutputIntent outputIntent = new PDOutputIntent(document, new ByteArrayInputStream(iccBytes));
			outputIntent.setInfo("sRGB IEC61966-2.1");
			outputIntent.setOutputCondition("sRGB IEC61966-2.1");
			outputIntent.setOutputConditionIdentifier("sRGB IEC61966-2.1");
			outputIntent.setRegistryName("https://www.color.org");
			catalog.addOutputIntent(outputIntent);

			byte[] bytes = save(document);
			return batch("pdf-to-pdfa",
					List.of(result(fileStem(file) + "-pdfa.pdf", "application/pdf", bytes, document.getNumberOfPages())));
		} catch (Exception exception) {
			throw new IllegalArgumentException("Unable to convert PDF to PDF/A.", exception);
		}
	}

	public PdfBatchResponse wordToPdf(MultipartFile file) {
		return officeToPdf(file, WORD_EXTENSIONS, "word-to-pdf");
	}

	public PdfBatchResponse powerpointToPdf(MultipartFile file) {
		return officeToPdf(file, POWERPOINT_EXTENSIONS, "powerpoint-to-pdf");
	}

	public PdfBatchResponse excelToPdf(MultipartFile file) {
		return officeToPdf(file, EXCEL_EXTENSIONS, "excel-to-pdf");
	}

	public PdfBatchResponse pdfToWord(MultipartFile file) {
		return officeConvert(file, PDF_EXTENSIONS, "docx", DOCX_CONTENT_TYPE, "pdf-to-word");
	}

	public PdfBatchResponse pdfToPowerpoint(MultipartFile file) {
		return officeConvert(file, PDF_EXTENSIONS, "pptx", PPTX_CONTENT_TYPE, "pdf-to-powerpoint");
	}

	public PdfBatchResponse pdfToExcel(MultipartFile file) {
		return officeConvert(file, PDF_EXTENSIONS, "xlsx", XLSX_CONTENT_TYPE, "pdf-to-excel");
	}

	private static final String DOCX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	private static final String PPTX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
	private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

	private PdfBatchResponse officeToPdf(MultipartFile file, Set<String> allowedExtensions, String operation) {
		return officeConvert(file, allowedExtensions, "pdf", "application/pdf", operation);
	}

	private PdfBatchResponse officeConvert(MultipartFile file, Set<String> allowedExtensions, String targetExtension,
			String contentType, String operation) {
		validateFile(file, allowedExtensions, extensionLabel(allowedExtensions));

		Path tempDirectory = null;
		try {
			tempDirectory = Files.createTempDirectory("devtool-office-" + randomSuffix());
			String inputName = sanitizeFileName(file.getOriginalFilename());
			Path inputFile = tempDirectory.resolve(inputName);
			Files.write(inputFile, file.getBytes());

			ProcessBuilder processBuilder = new ProcessBuilder(resolveSofficeExecutable(), "--headless", "--convert-to",
					targetExtension, "--outdir", tempDirectory.toString(), inputFile.toString());
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();
			String outputLog = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			int exitCode = process.waitFor();

			Path outputFile = tempDirectory.resolve(replaceExtension(inputName, targetExtension));
			if (exitCode != 0 || Files.notExists(outputFile)) {
				throw new IllegalArgumentException(
						"LibreOffice conversion is unavailable or failed for this file type. " + outputLog.strip());
			}

			byte[] bytes = Files.readAllBytes(outputFile);
			int pageCount = "pdf".equalsIgnoreCase(targetExtension) ? pageCount(bytes) : 0;
			return batch(operation, List.of(result(outputFile.getFileName().toString(), contentType, bytes, pageCount)));
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalArgumentException("Office conversion was interrupted.", exception);
		} catch (IOException exception) {
			throw new IllegalArgumentException("Office conversion is unavailable in this environment.", exception);
		} finally {
			deleteDirectoryQuietly(tempDirectory);
		}
	}

	private String resolveSofficeExecutable() {
		String configured = System.getenv("SOFFICE_PATH");
		return configured == null || configured.isBlank() ? "soffice" : configured;
	}

	private PdfBatchResponse batch(String operation, List<PdfFileResultResponse> files) {
		return new PdfBatchResponse(operation, files);
	}

	private PdfFileResultResponse singlePageDocument(PDDocument source, int pageIndex, String fileName) throws IOException {
		try (PDDocument output = new PDDocument()) {
			output.importPage(source.getPage(pageIndex));
			byte[] bytes = save(output);
			return result(fileName, "application/pdf", bytes, output.getNumberOfPages());
		}
	}

	private PdfFileResultResponse rangeDocument(PDDocument source, int startPage, int endPage, String fileName) throws IOException {
		try (PDDocument output = new PDDocument()) {
			for (int pageIndex = startPage - 1; pageIndex < endPage; pageIndex++) {
				output.importPage(source.getPage(pageIndex));
			}
			byte[] bytes = save(output);
			return result(fileName, "application/pdf", bytes, output.getNumberOfPages());
		}
	}

	private PdfFileResultResponse result(String fileName, String contentType, byte[] bytes, int pageCount) {
		return new PdfFileResultResponse(fileName, contentType, bytes.length, pageCount,
				Base64.getEncoder().encodeToString(bytes));
	}

	private byte[] save(PDDocument document) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		document.save(outputStream);
		return outputStream.toByteArray();
	}

	private int pageCount(byte[] pdfBytes) throws IOException {
		try (PDDocument document = PDDocument.load(pdfBytes)) {
			return document.getNumberOfPages();
		}
	}

	private void validateFile(MultipartFile file, Set<String> extensions, String label) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("A " + label + " file is required.");
		}

		String extension = extension(file.getOriginalFilename());
		if (extension == null || !extensions.contains(extension.toLowerCase())) {
			throw new IllegalArgumentException("Only " + label + " files are supported.");
		}
	}

	private String extensionLabel(Set<String> extensions) {
		return String.join("/", extensions).toUpperCase();
	}

	private String extension(String fileName) {
		if (fileName == null || !fileName.contains(".")) {
			return null;
		}
		return fileName.substring(fileName.lastIndexOf('.') + 1);
	}

	private String fileStem(MultipartFile file) {
		String fileName = sanitizeFileName(file.getOriginalFilename());
		int lastDot = fileName.lastIndexOf('.');
		return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
	}

	private String sanitizeFileName(String fileName) {
		if (fileName == null || fileName.isBlank()) {
			return "document";
		}
		return Path.of(fileName).getFileName().toString();
	}

	private String replaceExtension(String fileName, String targetExtension) {
		int lastDot = fileName.lastIndexOf('.');
		String stem = lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
		return stem + "." + targetExtension;
	}

	private String readUtf8(MultipartFile file) {
		try {
			return new String(file.getBytes(), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			throw new IllegalArgumentException("Unable to read uploaded file.", exception);
		}
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

		try (PDPageContentStream contentStream = new PDPageContentStream(document, page, AppendMode.APPEND, true, true)) {
			contentStream.beginText();
			contentStream.setFont(PDType1Font.HELVETICA, fontSize);
			contentStream.newLineAtOffset(x, y);
			contentStream.showText(text);
			contentStream.endText();
		}
	}

	private void deleteDirectoryQuietly(Path directory) {
		if (directory == null) {
			return;
		}
		try (var stream = Files.walk(directory)) {
			stream.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
				try {
					Files.deleteIfExists(path);
				} catch (IOException ignored) {
				}
			});
		} catch (IOException ignored) {
		}
	}

	private String randomSuffix() {
		return Long.toHexString(new SecureRandom().nextLong());
	}
}
