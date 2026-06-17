package com.devtool.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.devtool.dto.PdfBatchResponse;
import com.devtool.dto.PdfFileResultResponse;

class PdfToolServiceTest {

	private final PdfToolService service = new PdfToolService();

	@Test
	void shouldMergePdfFiles() throws IOException {
		PdfBatchResponse response = service.merge(
				java.util.List.of(pdfFile("one.pdf", 1), pdfFile("two.pdf", 2)));

		assertEquals("merge", response.operation());
		assertEquals(1, response.files().size());
		assertEquals(3, pageCount(response.files().get(0)));
	}

	@Test
	void shouldSplitPdfFileByRanges() throws IOException {
		PdfBatchResponse response = service.split(pdfFile("source.pdf", 4), "range", "1-2,4");

		assertEquals(2, response.files().size());
		assertEquals(2, pageCount(response.files().get(0)));
		assertEquals(1, pageCount(response.files().get(1)));
	}

	@Test
	void shouldRotateAndRemoveAndReorderPages() throws IOException {
		PdfFileResultResponse rotated = service.rotate(pdfFile("rotate.pdf", 2), 90).files().get(0);
		try (PDDocument document = load(rotated)) {
			assertEquals(90, document.getPage(0).getRotation());
		}

		PdfFileResultResponse removed = service.removePages(pdfFile("remove.pdf", 4), "2-3").files().get(0);
		assertEquals(2, pageCount(removed));

		PdfFileResultResponse reordered = service.reorderPages(pdfFile("reorder.pdf", 3), "3,1,2").files().get(0);
		assertEquals(3, pageCount(reordered));
	}

	@Test
	void shouldCompressAndAddPageNumbers() throws IOException {
		PdfFileResultResponse compressed = service.compress(pdfFile("compress.pdf", 2), "balanced").files().get(0);
		assertTrue(compressed.size() > 0);

		PdfFileResultResponse numbered = service.addPageNumbers(pdfFile("number.pdf", 2), "bottom-right", 5).files().get(0);
		assertEquals(2, pageCount(numbered));
		assertFalse(numbered.base64Content().isBlank());
	}

	@Test
	void shouldSupportEveryPageSplitAndToolListing() throws IOException {
		PdfBatchResponse response = service.split(pdfFile("pages.pdf", 3), "every-page", null);
		assertEquals(3, response.files().size());
		assertTrue(service.supportedTools().get("pdfTools").contains("Merge PDF"));
	}

	@Test
	void shouldConvertJpgAndHtmlAndPdfA() throws IOException {
		PdfFileResultResponse jpgToPdf = service.jpgToPdf(java.util.List.of(jpgFile("photo.jpg"))).files().get(0);
		assertEquals(1, pageCount(jpgToPdf));

		PdfBatchResponse pdfToJpg = service.pdfToJpg(pdfFile("sample.pdf", 2), 144);
		assertEquals(2, pdfToJpg.files().size());
		assertEquals("image/jpeg", pdfToJpg.files().get(0).contentType());

		PdfFileResultResponse htmlToPdf = service.htmlToPdf(
				new MockMultipartFile("file", "page.html", "text/html", "<html><body><h1>Hello</h1></body></html>".getBytes()))
				.files().get(0);
		assertEquals(1, pageCount(htmlToPdf));

		PdfFileResultResponse pdfA = service.pdfToPdfA(pdfFile("pdfa.pdf", 1)).files().get(0);
		assertEquals(1, pageCount(pdfA));
	}

	@Test
	void shouldRejectInvalidRequests() throws IOException {
		assertThrows(IllegalArgumentException.class, () -> service.merge(java.util.List.of(pdfFile("one.pdf", 1))));
		assertThrows(IllegalArgumentException.class, () -> service.rotate(pdfFile("rotate.pdf", 1), 45));
		assertThrows(IllegalArgumentException.class, () -> service.reorderPages(pdfFile("reorder.pdf", 2), "1,1"));
	}

	private MockMultipartFile pdfFile(String fileName, int pageCount) throws IOException {
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			for (int index = 0; index < pageCount; index++) {
				document.addPage(new PDPage());
			}
			document.save(output);
			return new MockMultipartFile("file", fileName, "application/pdf", output.toByteArray());
		}
	}

	private int pageCount(PdfFileResultResponse response) throws IOException {
		try (PDDocument document = load(response)) {
			return document.getNumberOfPages();
		}
	}

	private PDDocument load(PdfFileResultResponse response) throws IOException {
		return PDDocument.load(Base64.getDecoder().decode(response.base64Content()));
	}

	private MockMultipartFile jpgFile(String fileName) throws IOException {
		BufferedImage image = new BufferedImage(120, 80, BufferedImage.TYPE_INT_RGB);
		var graphics = image.createGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, 120, 80);
		graphics.setColor(Color.BLUE);
		graphics.fillRect(10, 10, 100, 60);
		graphics.dispose();

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		ImageIO.write(image, "jpg", output);
		return new MockMultipartFile("file", fileName, "image/jpeg", output.toByteArray());
	}
}
