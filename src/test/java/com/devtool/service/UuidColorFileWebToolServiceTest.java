package com.devtool.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class UuidColorFileWebToolServiceTest {

	private final UuidToolService uuidToolService = new UuidToolService();
	private final ColorToolService colorToolService = new ColorToolService();
	private final FileToolService fileToolService = new FileToolService();
	private final WebFormatterToolService webFormatterToolService = new WebFormatterToolService();

	@Test
	void shouldValidateUuid() {
		assertTrue(uuidToolService.isValid("123e4567-e89b-12d3-a456-426614174000"));
		assertEquals("123e4567e89b12d3a456426614174000",
				uuidToolService.removeHyphens("123e4567-e89b-12d3-a456-426614174000"));
	}

	@Test
	void shouldConvertColorAndGradient() {
		assertEquals("#FF0000", colorToolService.fromRgb(255, 0, 0).hex());
		assertEquals(3, colorToolService.gradient("#000000", "#FFFFFF", 3).palette().size());
	}

	@Test
	void shouldInspectFileAndMinifyWebContent() {
		MockMultipartFile file = new MockMultipartFile("file", "hello.txt", "text/plain",
				"hello".getBytes(StandardCharsets.UTF_8));
		assertEquals("txt", fileToolService.inspect(file).extension());
		assertEquals("a{color:red;}", webFormatterToolService.minifyCss("a { color: red; }"));
	}
}
