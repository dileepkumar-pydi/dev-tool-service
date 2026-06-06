package com.devtool.dto;

import java.util.List;

public record DiffResponse(String tool, List<DiffLineResponse> lines) {
}
