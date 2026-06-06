package com.devtool.dto;

public record ColorConversionResponse(String tool, String hex, int red, int green, int blue, double hue,
		double saturation, double lightness) {
}
