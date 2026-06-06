package com.devtool.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EscapeToolServiceTest {

	private final EscapeToolService service = new EscapeToolService();

	@Test
	void shouldEscapeAndUnescapeHtml() {
		assertEquals("&lt;b&gt;x&lt;/b&gt;", service.escapeHtml("<b>x</b>"));
		assertEquals("<b>x</b>", service.unescapeHtml("&lt;b&gt;x&lt;/b&gt;"));
	}

	@Test
	void shouldEscapeAndUnescapeSql() {
		assertEquals("O''Reilly", service.escapeSql("O'Reilly"));
		assertEquals("O'Reilly", service.unescapeSql("O''Reilly"));
	}
}
