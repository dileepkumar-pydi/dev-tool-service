package com.devtool.dto;

public record HashVerifyRequest(String value, String expectedHash, String algorithm) {
}
