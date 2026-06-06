package com.devtool.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class UuidToolService {

	public Map<String, List<String>> supportedTools() {
		Map<String, List<String>> tools = new LinkedHashMap<>();
		tools.put("uuidTools", List.of("Generate UUID", "Validate UUID", "Remove Hyphens"));
		return tools;
	}

	public String generate() {
		return UUID.randomUUID().toString();
	}

	public String normalize(String value) {
		try {
			return UUID.fromString(safeValue(value).trim()).toString();
		} catch (Exception ex) {
			throw new IllegalArgumentException("Invalid UUID value.", ex);
		}
	}

	public boolean isValid(String value) {
		try {
			UUID.fromString(safeValue(value).trim());
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	public String removeHyphens(String value) {
		return normalize(value).replace("-", "");
	}

	private String safeValue(String value) {
		return value == null ? "" : value;
	}
}
