package com.devtool.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class JsonToolServiceTest {

	private final JsonToolService service = new JsonToolService(new ObjectMapper());

	@Test
	void shouldFormatJson() {
		String formatted = service.formatJson("{\"name\":\"DevTool\",\"active\":true}");
		assertTrue(formatted.contains("\n"));
		assertTrue(formatted.contains("\"name\""));
	}

	@Test
	void shouldMinifyJson() {
		assertEquals("{\"name\":\"DevTool\",\"active\":true}",
				service.minifyJson("{\n  \"name\": \"DevTool\",\n  \"active\": true\n}"));
	}

	@Test
	void shouldValidateJson() {
		assertTrue(service.isValidJson("{\"hello\":\"world\"}"));
		assertFalse(service.isValidJson("{hello"));
	}

	@Test
	void shouldConvertJsonToYaml() {
		String yaml = service.jsonToYaml("{\"name\":\"DevTool\",\"count\":2}");
		assertTrue(yaml.contains("name: \"DevTool\""));
		assertTrue(yaml.contains("count: 2"));
	}

	@Test
	void shouldConvertYamlToJson() {
		String json = service.yamlToJson("name: DevTool\ncount: 2\n");
		assertTrue(json.contains("\"name\""));
		assertTrue(json.contains("\"count\""));
	}

	@Test
	void shouldEscapeAndUnescapeJsonString() {
		String escaped = service.escapeJson("hello\nworld");
		assertEquals("hello\\nworld", escaped);
		assertEquals("hello\nworld", service.unescapeJson(escaped));
	}

	@Test
	void shouldQueryJsonPath() {
		String result = service.queryJsonPath("{\"user\":{\"name\":\"Asha\"}}", "/user/name");
		assertEquals("Asha", result);
	}

	@Test
	void shouldRejectMissingJsonPath() {
		assertThrows(IllegalArgumentException.class,
				() -> service.queryJsonPath("{\"user\":{\"name\":\"Asha\"}}", "/user/id"));
	}
}
