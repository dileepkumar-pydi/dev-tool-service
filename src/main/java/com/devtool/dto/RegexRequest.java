package com.devtool.dto;

public record RegexRequest(String pattern, String input, boolean caseInsensitive) {
}
