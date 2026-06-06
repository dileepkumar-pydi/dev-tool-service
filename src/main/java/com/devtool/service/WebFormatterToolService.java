package com.devtool.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document.OutputSettings;
import org.springframework.stereotype.Service;

@Service
public class WebFormatterToolService {

	public Map<String, List<String>> supportedTools() {
		Map<String, List<String>> tools = new LinkedHashMap<>();
		tools.put("webTools", List.of("HTML Beautify", "HTML Minify", "HTML Escape", "HTML Unescape", "CSS Beautify",
				"CSS Minify", "JavaScript Beautify", "JavaScript Minify"));
		return tools;
	}

	public String beautifyHtml(String value) {
		return Jsoup.parseBodyFragment(safeValue(value)).outputSettings(new OutputSettings().prettyPrint(true).indentAmount(2))
				.body().html();
	}

	public String minifyHtml(String value) {
		return Jsoup.parseBodyFragment(safeValue(value)).outputSettings(new OutputSettings().prettyPrint(false)).body().html();
	}

	public String escapeHtml(String value) {
		return StringEscapeUtils.escapeHtml4(safeValue(value));
	}

	public String unescapeHtml(String value) {
		return StringEscapeUtils.unescapeHtml4(safeValue(value));
	}

	public String beautifyCss(String value) {
		return prettyBraceLanguage(value);
	}

	public String minifyCss(String value) {
		return minifyBraceLanguage(value);
	}

	public String beautifyJs(String value) {
		return prettyBraceLanguage(value);
	}

	public String minifyJs(String value) {
		return minifyBraceLanguage(value);
	}

	private String prettyBraceLanguage(String value) {
		String input = safeValue(value);
		StringBuilder builder = new StringBuilder();
		int indent = 0;
		for (int i = 0; i < input.length(); i++) {
			char current = input.charAt(i);
			switch (current) {
			case '{' -> {
				builder.append(" {\n");
				indent++;
				appendIndent(builder, indent);
			}
			case '}' -> {
				builder.append('\n');
				indent = Math.max(0, indent - 1);
				appendIndent(builder, indent);
				builder.append('}');
			}
			case ';' -> {
				builder.append(";\n");
				appendIndent(builder, indent);
			}
			default -> builder.append(current);
			}
		}
		return builder.toString().replaceAll("[ \\t]+\\n", "\n").trim();
	}

	private String minifyBraceLanguage(String value) {
		return safeValue(value).replaceAll("/\\*.*?\\*/", "").replaceAll("\\s+", " ")
				.replaceAll("\\s*([{};:,])\\s*", "$1").trim();
	}

	private void appendIndent(StringBuilder builder, int indent) {
		builder.append("  ".repeat(Math.max(0, indent)));
	}

	private String safeValue(String value) {
		return value == null ? "" : value;
	}
}
