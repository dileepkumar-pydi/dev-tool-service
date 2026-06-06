package com.devtool.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.io.JsonStringEncoder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@Service
public class JsonToolService {

	private final ObjectMapper objectMapper;
	private final ObjectMapper yamlMapper;

	public JsonToolService(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
		this.yamlMapper = new ObjectMapper(new YAMLFactory());
	}

	public Map<String, List<String>> supportedTools() {
		Map<String, List<String>> tools = new LinkedHashMap<>();
		tools.put("jsonTools", List.of("JSON Format", "JSON Minify", "JSON Validate", "JSON to YAML", "YAML to JSON",
				"JSON Escape", "JSON Unescape", "JSON Path Query"));
		return tools;
	}

	public String formatJson(String json) {
		return writePretty(readJsonNode(json));
	}

	public String minifyJson(String json) {
		return writeCompact(readJsonNode(json));
	}

	public boolean isValidJson(String json) {
		try {
			readJsonNode(json);
			return true;
		} catch (IllegalArgumentException ex) {
			return false;
		}
	}

	public String jsonToYaml(String json) {
		JsonNode node = readJsonNode(json);
		try {
			return yamlMapper.writeValueAsString(node);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Unable to convert JSON to YAML.", ex);
		}
	}

	public String yamlToJson(String yaml) {
		try {
			JsonNode node = yamlMapper.readTree(safeValue(yaml));
			return writePretty(node);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Invalid YAML input.", ex);
		}
	}

	public String escapeJson(String value) {
		return new String(JsonStringEncoder.getInstance().quoteAsString(safeValue(value)));
	}

	public String unescapeJson(String value) {
		try {
			return objectMapper.readValue("\"" + safeValue(value).replace("\"", "\\\"") + "\"", String.class);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Invalid escaped JSON string.", ex);
		}
	}

	public String queryJsonPath(String json, String path) {
		String normalizedPath = safeValue(path).trim();
		if (normalizedPath.isEmpty()) {
			throw new IllegalArgumentException("JSON path is required.");
		}

		JsonNode currentNode = readJsonNode(json).at(normalizedPath);
		if (currentNode.isMissingNode()) {
			throw new IllegalArgumentException("No JSON value found at path: " + normalizedPath);
		}

		return currentNode.isValueNode() ? currentNode.asText() : writePretty(currentNode);
	}

	private JsonNode readJsonNode(String json) {
		try {
			return objectMapper.readTree(safeValue(json));
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Invalid JSON input.", ex);
		}
	}

	private String writePretty(JsonNode node) {
		try {
			return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Unable to format JSON output.", ex);
		}
	}

	private String writeCompact(JsonNode node) {
		try {
			return objectMapper.writeValueAsString(node);
		} catch (JsonProcessingException ex) {
			throw new IllegalArgumentException("Unable to minify JSON output.", ex);
		}
	}

	private String safeValue(String value) {
		return value == null ? "" : value;
	}
}
