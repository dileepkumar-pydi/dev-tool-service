package com.devtool.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.springframework.stereotype.Service;

import com.devtool.dto.DiffLineResponse;
import com.devtool.dto.RegexMatchResponse;

@Service
public class TextToolService {

	public Map<String, List<String>> supportedTools() {
		Map<String, List<String>> tools = new LinkedHashMap<>();
		tools.put("textTools", List.of("Diff Checker", "Case Converter", "Slug Generator", "Regex Tester",
				"Word Counter", "Character Counter"));
		return tools;
	}

	public List<DiffLineResponse> diff(String left, String right) {
		String[] leftLines = normalizeLines(left);
		String[] rightLines = normalizeLines(right);
		int[][] lcs = new int[leftLines.length + 1][rightLines.length + 1];

		for (int i = leftLines.length - 1; i >= 0; i--) {
			for (int j = rightLines.length - 1; j >= 0; j--) {
				if (leftLines[i].equals(rightLines[j])) {
					lcs[i][j] = lcs[i + 1][j + 1] + 1;
				} else {
					lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
				}
			}
		}

		List<DiffLineResponse> lines = new ArrayList<>();
		int i = 0;
		int j = 0;
		while (i < leftLines.length && j < rightLines.length) {
			if (leftLines[i].equals(rightLines[j])) {
				lines.add(new DiffLineResponse("UNCHANGED", leftLines[i]));
				i++;
				j++;
			} else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
				lines.add(new DiffLineResponse("REMOVED", leftLines[i++]));
			} else {
				lines.add(new DiffLineResponse("ADDED", rightLines[j++]));
			}
		}

		while (i < leftLines.length) {
			lines.add(new DiffLineResponse("REMOVED", leftLines[i++]));
		}
		while (j < rightLines.length) {
			lines.add(new DiffLineResponse("ADDED", rightLines[j++]));
		}
		return lines;
	}

	public String convertCase(String value, String targetCase) {
		List<String> words = splitWords(value);
		if (words.isEmpty()) {
			return "";
		}

		return switch (safeValue(targetCase).trim().toLowerCase(Locale.ROOT)) {
		case "lower" -> String.join(" ", words).toLowerCase(Locale.ROOT);
		case "upper" -> String.join(" ", words).toUpperCase(Locale.ROOT);
		case "camel" -> lowerFirst(joinCapitalized(words));
		case "pascal" -> joinCapitalized(words);
		case "snake" -> String.join("_", lowercaseWords(words));
		case "kebab" -> String.join("-", lowercaseWords(words));
		case "constant" -> String.join("_", uppercaseWords(words));
		case "title" -> String.join(" ", capitalizeWords(words));
		default -> throw new IllegalArgumentException("Unsupported target case: " + targetCase);
		};
	}

	public String slugify(String value) {
		String normalized = Normalizer.normalize(safeValue(value), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
	}

	public List<RegexMatchResponse> regexMatches(String pattern, String input, boolean caseInsensitive) {
		try {
			int flags = caseInsensitive ? Pattern.CASE_INSENSITIVE : 0;
			Matcher matcher = Pattern.compile(safeValue(pattern), flags).matcher(safeValue(input));
			List<RegexMatchResponse> matches = new ArrayList<>();
			while (matcher.find()) {
				matches.add(new RegexMatchResponse(matcher.start(), matcher.end(), matcher.group()));
			}
			return matches;
		} catch (PatternSyntaxException ex) {
			throw new IllegalArgumentException("Invalid regex pattern.", ex);
		}
	}

	public int wordCount(String value) {
		String trimmed = safeValue(value).trim();
		return trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
	}

	public int charCount(String value) {
		return safeValue(value).length();
	}

	public int charCountWithoutSpaces(String value) {
		return safeValue(value).replaceAll("\\s+", "").length();
	}

	public int lineCount(String value) {
		String normalized = safeValue(value);
		return normalized.isEmpty() ? 0 : normalized.split("\\R", -1).length;
	}

	private String[] normalizeLines(String value) {
		String normalized = safeValue(value);
		return normalized.isEmpty() ? new String[0] : normalized.split("\\R", -1);
	}

	private List<String> splitWords(String value) {
		String normalized = safeValue(value).replaceAll("([a-z0-9])([A-Z])", "$1 $2").replaceAll("[_\\-]+", " ")
				.trim();
		if (normalized.isEmpty()) {
			return List.of();
		}
		return List.of(normalized.split("\\s+"));
	}

	private String joinCapitalized(List<String> words) {
		return words.stream().map(this::capitalize).reduce("", String::concat);
	}

	private List<String> lowercaseWords(List<String> words) {
		return words.stream().map(word -> word.toLowerCase(Locale.ROOT)).toList();
	}

	private List<String> uppercaseWords(List<String> words) {
		return words.stream().map(word -> word.toUpperCase(Locale.ROOT)).toList();
	}

	private List<String> capitalizeWords(List<String> words) {
		return words.stream().map(this::capitalize).toList();
	}

	private String capitalize(String word) {
		String lower = word.toLowerCase(Locale.ROOT);
		return lower.substring(0, 1).toUpperCase(Locale.ROOT) + lower.substring(1);
	}

	private String lowerFirst(String value) {
		return value.substring(0, 1).toLowerCase(Locale.ROOT) + value.substring(1);
	}

	private String safeValue(String value) {
		return value == null ? "" : value;
	}
}
