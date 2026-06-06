package com.devtool.dto;

public record UuidValidationResponse(String tool, String value, boolean valid, String normalized) {
}
