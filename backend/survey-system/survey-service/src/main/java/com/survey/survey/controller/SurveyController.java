package com.survey.survey.controller;

import com.survey.common.ApiResponse;
import com.survey.common.PageResult;
import com.survey.survey.dto.CreateSurveyRequest;
import com.survey.survey.dto.SurveyDetailResponse;
import com.survey.survey.entity.Survey;
import com.survey.survey.service.SurveyDeleteService;
import com.survey.survey.service.SurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;
    private final SurveyDeleteService deleteService;

    @PostMapping
    public ApiResponse<SurveyDetailResponse> create(
            @RequestBody CreateSurveyRequest request,
            @RequestAttribute("userId") Long userId) {
        Survey survey = surveyService.create(request, userId);
        return ApiResponse.ok(surveyService.toDetail(survey));
    }

    @GetMapping
    public ApiResponse<PageResult<SurveyDetailResponse>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(surveyService.list(userId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<SurveyDetailResponse> detail(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(surveyService.detail(id, userId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    @RequestAttribute("userId") Long userId) {
        deleteService.delete(id, userId);
        return ApiResponse.ok();
    }
}