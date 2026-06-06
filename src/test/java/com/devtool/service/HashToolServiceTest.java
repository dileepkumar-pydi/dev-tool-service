package com.devtool.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HashToolServiceTest {

	private final HashToolService service = new HashToolService();

	@Test
	void shouldGenerateSha256Hash() {
		assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
				service.hash("hello", "SHA-256"));
	}

	@Test
	void shouldGenerateHmac() {
		assertEquals("b82fcb791acec57859b989b430a826488ce2e479fdf92326bd0a2e8375a42ba4",
				service.hmac("payload", "secret", "SHA-256"));
	}

	@Test
	void shouldVerifyHash() {
		assertTrue(service.verify("hello",
				"2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", "SHA-256"));
	}
}
