package com.survey.survey.controller;

import com.survey.common.ApiResponse;
import com.survey.survey.dto.QuestionView;
import com.survey.survey.dto.SaveSurveyRequest;
import com.survey.survey.service.SurveyDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
public class SurveyDraftController {

    private final SurveyDraftService draftService;

    @PutMapping("/{id}")
    public ApiResponse<List<QuestionView>> saveDraft(
            @PathVariable Long id,
            @RequestBody SaveSurveyRequest request,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(draftService.saveDraft(id, request, userId));
    }

    @GetMapping("/{id}/draft")
    public ApiResponse<List<QuestionView>> getDraft(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(draftService.listCurrentDraft(id, userId));
    }
}