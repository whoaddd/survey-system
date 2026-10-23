package com.survey.response.controller;

import com.survey.common.ApiResponse;
import com.survey.common.PageResult;
import com.survey.response.dto.ResponseSummary;
import com.survey.response.dto.SurveyStatistics;
import com.survey.response.service.ResponseListService;
import com.survey.response.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
public class SurveyResultController {

    private final ResponseListService responseListService;
    private final StatisticsService statisticsService;

    @GetMapping("/{id}/responses")
    public ApiResponse<PageResult<ResponseSummary>> listResponses(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(responseListService.list(id, userId, page, size));
    }

    @GetMapping("/{id}/statistics")
    public ApiResponse<SurveyStatistics> statistics(@PathVariable Long id,
                                                    @RequestAttribute("userId") Long userId) {
        return ApiResponse.ok(statisticsService.statistics(id, userId));
    }
}