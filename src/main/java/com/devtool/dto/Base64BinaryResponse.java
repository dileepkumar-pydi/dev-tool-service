package com.devtool.dto;

public record Base64BinaryResponse(String tool, String fileName, String contentType, String base64) {
}
