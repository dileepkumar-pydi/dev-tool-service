package com.devtool.dto;

public record TextStatsResponse(String tool, int characters, int charactersWithoutSpaces, int words, int lines) {
}
