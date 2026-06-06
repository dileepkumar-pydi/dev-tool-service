package com.devtool.dto;

public record FileMetadataResponse(String tool, String fileName, String contentType, String extension, long size,
		String md5, String sha1, String sha256) {
}
