package com.devtool.dto;

import java.util.List;

public record GradientResponse(String tool, String cssGradient, List<String> palette) {
}
