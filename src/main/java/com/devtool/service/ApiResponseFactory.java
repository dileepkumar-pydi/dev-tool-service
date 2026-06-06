package com.devtool.service;

import org.springframework.stereotype.Service;

import com.devtool.dto.ApiResponse;
import com.devtool.dto.ApiStatus;

@Service
public class ApiResponseFactory {

	public <T> ApiResponse<T> success(T data, String description) {
		return new ApiResponse<>(new ApiStatus("200", "SUCESS", description), data);
	}

	public <T> ApiResponse<T> badRequest(T data, String description) {
		return new ApiResponse<>(new ApiStatus("400", "FAILED", description), data);
	}
}
