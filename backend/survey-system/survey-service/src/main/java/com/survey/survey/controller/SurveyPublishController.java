package com.survey.survey.controller;

import com.survey.common.ApiResponse;
import com.survey.survey.dto.SurveyDetailResponse;
import com.survey.survey.entity.Survey;
import com.survey.survey.service.SurveyPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/surveys")
@RequiredArgsConstructor
public class SurveyPublishController {

    private final SurveyPublishService publishService;

    @PostMapping("/{id}/publish")
    public ApiResponse<SurveyDetailResponse> publish(@PathVariable Long id,
                                                     @RequestAttribute("userId") Long userId) {
        Survey survey = publishService.publish(id, userId);
        return ApiResponse.ok(toDetail(survey));
    }

    @PostMapping("/{id}/close")
    public ApiResponse<SurveyDetailResponse> close(@PathVariable Long id,
                                                   @RequestAttribute("userId") Long userId) {
        Survey survey = publishService.close(id, userId);
        return ApiResponse.ok(toDetail(survey));
    }

    private SurveyDetailResponse toDetail(Survey s) {
        String shareUrl = s.getShareCode() == null ? null : "/s/" + s.getShareCode();
        return new SurveyDetailResponse(
                s.getId(), s.getTitle(), s.getDescription(), s.getStatus(),
                s.getShareCode(), shareUrl, s.getVersion(), s.getPublishedVersion(),
                s.getPublishedAt(), s.getCreatedAt());
    }

    @PostMapping("/{id}/copy")
    public ApiResponse<SurveyDetailResponse> copy(@PathVariable Long id,
                                                  @RequestAttribute("userId") Long userId) {
        Survey draft = publishService.copyAsNewDraft(id, userId);
        return ApiResponse.ok(toDetail(draft));
    }


}