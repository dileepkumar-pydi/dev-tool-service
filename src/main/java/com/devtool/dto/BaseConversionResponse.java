package com.devtool.dto;

public record BaseConversionResponse(String tool, String value, int sourceBase, int targetBase, String result) {
}
