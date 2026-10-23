package com.survey.file.controller;

import com.survey.common.ApiResponse;
import com.survey.file.dto.*;
import com.survey.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/presign")
    public ApiResponse<PresignResponse> presign(@RequestBody PresignRequest req,
                                                @RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(fileService.presign(req));
    }

    @PostMapping("/confirm")
    public ApiResponse<ConfirmResponse> confirm(@RequestBody ConfirmRequest req,
                                                @RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(fileService.confirm(req, userId));
    }

    @GetMapping("/url")
    public ApiResponse<FileUrlResponse> url(@RequestParam String objectKey) {
        return ApiResponse.ok(fileService.getUrl(objectKey));
    }
}