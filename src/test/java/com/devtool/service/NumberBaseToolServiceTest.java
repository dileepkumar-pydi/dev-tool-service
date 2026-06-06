package com.devtool.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NumberBaseToolServiceTest {

	private final NumberBaseToolService service = new NumberBaseToolService();

	@Test
	void shouldConvertBetweenBases() {
		assertEquals("ff", service.convertBase("255", 10, 16));
	}

	@Test
	void shouldConvertAsciiAndUnicodeHelpers() {
		assertEquals("65 66", service.textToAsciiCodes("AB"));
		assertEquals("AB", service.asciiCodesToText("65 66"));
		assertEquals("U+0041 U+0042", service.textToUnicodeCodes("AB"));
		assertEquals("AB", service.unicodeCodesToText("U+0041 U+0042"));
	}
}
