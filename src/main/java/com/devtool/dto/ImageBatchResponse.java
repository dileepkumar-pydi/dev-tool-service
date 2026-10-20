package com.devtool.dto;

import java.util.List;

public record ImageBatchResponse(String operation, List<ImageFileResultResponse> files, List<String> warnings) {}
