package com.devtool.dto;

import java.util.List;

public record RegexTestResponse(String tool, String pattern, boolean matched, int matchCount,
		List<RegexMatchResponse> matches) {
}
