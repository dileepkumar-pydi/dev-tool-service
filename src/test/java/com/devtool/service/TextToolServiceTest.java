package com.devtool.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextToolServiceTest {

	private final TextToolService service = new TextToolService();

	@Test
	void shouldConvertCase() {
		assertEquals("helloWorld", service.convertCase("hello world", "camel"));
		assertEquals("Hello world", service.convertCase("HELLO WORLD", "sentence"));
		assertEquals("cba", service.convertCase("abc", "reverse"));
	}

	@Test
	void shouldGenerateSlug() {
		assertEquals("hello-dev-tool", service.slugify("Hello Dev Tool!"));
	}

	@Test
	void shouldFindRegexMatches() {
		assertEquals(2, service.regexMatches("\\d+", "A1 B22", false).size());
	}

	@Test
	void shouldCountWordsAndChars() {
		assertEquals(2, service.wordCount("hello world"));
		assertEquals(11, service.charCount("hello world"));
		assertEquals(10, service.charCountWithoutSpaces("hello world"));
		assertFalse(service.diff("a", "b").isEmpty());
	}
}
