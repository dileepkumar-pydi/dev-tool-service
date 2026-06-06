package com.devtool.dto;

public record HmacVerifyRequest(String value, String expectedHash, String secret, String algorithm) {
}
