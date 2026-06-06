package com.devtool.dto;

public record ApiResponse<T>(ApiStatus status, T data) {
}
