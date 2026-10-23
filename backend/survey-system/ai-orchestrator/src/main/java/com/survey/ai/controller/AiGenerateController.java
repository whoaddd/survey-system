package com.survey.ai.controller;

import com.survey.ai.dto.GeneratedSurvey;
import com.survey.ai.service.AiGenerateService;
import com.survey.common.ApiResponse;
import com.survey.common.BusinessException;
import com.survey.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/surveys")
@RequiredArgsConstructor
public class AiGenerateController {

    private final AiGenerateService aiGenerateService;

    @PostMapping("/generate")
    public ApiResponse<GeneratedSurvey> generate(@RequestBody GenerateRequest req) {
        if (req.topic() == null || req.topic().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "主题不能为空");
        }
        int count = req.count() == null ? 5 : req.count();
        return ApiResponse.ok(aiGenerateService.generate(req.topic(), count));
    }

    public record GenerateRequest(String topic, Integer count) {}
}