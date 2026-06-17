package com.devtool.dto;

public record PdfFileResultResponse(String fileName, String contentType, long size, int pageCount, String base64Content) {
}
