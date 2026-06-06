package com.devtool.dto;

public record HashVerifyResponse(String tool, String algorithm, String expectedHash, String actualHash, boolean valid) {
}
