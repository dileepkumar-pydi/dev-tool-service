package com.devtool.dto;

public record HmacRequest(String value, String secret, String algorithm) {
}
