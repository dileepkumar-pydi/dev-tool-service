package com.devtool.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;

@Service
public class EscapeToolService {

	public Map<String, List<String>> supportedTools() {
		Map<String, List<String>> tools = new LinkedHashMap<>();
		tools.put("escapeTools", List.of("HTML Escape", "HTML Unescape", "JSON Escape", "JSON Unescape",
				"Java Escape", "Java Unescape", "SQL Escape", "SQL Unescape", "XML Escape", "XML Unescape"));
		return tools;
	}

	public String escapeHtml(String value) {
		return StringEscapeUtils.escapeHtml4(safeValue(value));
	}

	public String unescapeHtml(String value) {
		return StringEscapeUtils.unescapeHtml4(safeValue(value));
	}

	public String escapeJson(String value) {
		return StringEscapeUtils.escapeJson(safeValue(value));
	}

	public String unescapeJson(String value) {
		return StringEscapeUtils.unescapeJson(safeValue(value));
	}

	public String escapeJava(String value) {
		return StringEscapeUtils.escapeJava(safeValue(value));
	}

	public String unescapeJava(String value) {
		return StringEscapeUtils.unescapeJava(safeValue(value));
	}

	public String escapeSql(String value) {
		return safeValue(value).replace("'", "''");
	}

	public String unescapeSql(String value) {
		return safeValue(value).replace("''", "'");
	}

	public String escapeXml(String value) {
		return StringEscapeUtils.escapeXml11(safeValue(value));
	}

	public String unescapeXml(String value) {
		return StringEscapeUtils.unescapeXml(safeValue(value));
	}

	private String safeValue(String value) {
		return value == null ? "" : value;
	}
}
