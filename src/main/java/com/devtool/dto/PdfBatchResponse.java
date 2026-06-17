package com.devtool.dto;

import java.util.List;

public record PdfBatchResponse(String operation, List<PdfFileResultResponse> files) {
}
