package com.devtool.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.devtool.dto.*;
import com.devtool.service.ApiResponseFactory;
import com.devtool.service.ImageToolService;

@RestController
@RequestMapping("/api/image")
public class ImageToolController {
    private final ImageToolService service;
    private final ApiResponseFactory responses;

    public ImageToolController(ImageToolService service, ApiResponseFactory responses) {
        this.service = service;
        this.responses = responses;
    }

    @GetMapping("/tools")
    public ApiResponse<List<String>> tools() {
        return responses.success(ImageToolService.OPERATIONS, "Image tools fetched successfully");
    }

    @PostMapping(path = "/{operation}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImageBatchResponse> process(@PathVariable String operation,
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam Map<String, String> options,
            @RequestParam(name = "logo", required = false) MultipartFile logo) {
        return responses.success(service.process(operation, files, options, logo), "Images processed successfully");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> invalid(IllegalArgumentException error) {
        return ResponseEntity.badRequest().body(responses.badRequest(new ErrorResponse(error.getMessage()), error.getMessage()));
    }
}
