package com.devtool.dto;

public record ImageFileResultResponse(String fileName, String contentType, long size, long originalSize,
        int width, int height, String base64Content) {}
