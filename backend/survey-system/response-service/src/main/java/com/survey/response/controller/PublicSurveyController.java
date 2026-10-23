package com.survey.response.controller;

import com.survey.common.ApiResponse;
import com.survey.response.dto.PublicSurveyResponse;
import com.survey.response.service.PublicSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/surveys")
@RequiredArgsConstructor
public class PublicSurveyController {

    private final PublicSurveyService publicSurveyService;

    @GetMapping("/{shareCode}")
    public ApiResponse<PublicSurveyResponse> get(@PathVariable String shareCode) {
        return ApiResponse.ok(publicSurveyService.getByShareCode(shareCode));
    }
}