package com.devtool.service;

import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class NumberBaseToolService {

	public Map<String, List<String>> supportedTools() {
		Map<String, List<String>> tools = new LinkedHashMap<>();
		tools.put("numberBaseTools", List.of("Binary Converter", "Octal Converter", "Decimal Converter",
				"Hex Converter", "ASCII Helper", "Unicode Helper"));
		return tools;
	}

	public String convertBase(String value, int sourceBase, int targetBase) {
		validateBase(sourceBase);
		validateBase(targetBase);
		try {
			return new BigInteger(safeValue(value).trim(), sourceBase).toString(targetBase);
		} catch (Exception ex) {
			throw new IllegalArgumentException("Invalid value for base " + sourceBase + ".", ex);
		}
	}

	public String textToAsciiCodes(String value) {
		return safeValue(value).chars().mapToObj(String::valueOf).collect(Collectors.joining(" "));
	}

	public String asciiCodesToText(String value) {
		if (safeValue(value).isBlank()) {
			return "";
		}
		try {
			StringBuilder builder = new StringBuilder();
			for (String token : safeValue(value).trim().split("\\s+")) {
				builder.append((char) Integer.parseInt(token));
			}
			return builder.toString();
		} catch (Exception ex) {
			throw new IllegalArgumentException("Invalid ASCII code sequence.", ex);
		}
	}

	public String textToUnicodeCodes(String value) {
		return safeValue(value).codePoints().mapToObj(codePoint -> String.format("U+%04X", codePoint))
				.collect(Collectors.joining(" "));
	}

	public String unicodeCodesToText(String value) {
		if (safeValue(value).isBlank()) {
			return "";
		}
		try {
			StringBuilder builder = new StringBuilder();
			for (String token : safeValue(value).trim().split("\\s+")) {
				String normalized = token.toUpperCase().startsWith("U+") ? token.substring(2) : token;
				builder.appendCodePoint(Integer.parseInt(normalized, 16));
			}
			return builder.toString();
		} catch (Exception ex) {
			throw new IllegalArgumentException("Invalid Unicode code point sequence.", ex);
		}
	}

	private void validateBase(int base) {
		if (base < 2 || base > 36) {
			throw new IllegalArgumentException("Base must be between 2 and 36.");
		}
	}

	private String safeValue(String value) {
		return value == null ? "" : value;
	}
}
