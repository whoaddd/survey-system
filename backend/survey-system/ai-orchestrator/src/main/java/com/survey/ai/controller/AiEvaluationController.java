package com.survey.ai.controller;

import com.survey.ai.dto.AiScoreResult;
import com.survey.ai.dto.EvaluateRequest;
import com.survey.ai.service.AiEvaluationService;
import com.survey.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai/responses")
@RequiredArgsConstructor
public class AiEvaluationController {

    private final AiEvaluationService aiEvaluationService;

    @PostMapping("/{id}/evaluate")
    public ApiResponse<AiScoreResult> evaluate(
            @PathVariable Long id,
            @RequestBody EvaluateRequest req) {
        return ApiResponse.ok(aiEvaluationService.evaluate(id, req));
    }
}
