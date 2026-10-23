package com.survey.survey.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.survey.common.BusinessException;
import com.survey.common.ErrorCode;
import com.survey.common.PageResult;
import com.survey.survey.dto.CreateSurveyRequest;
import com.survey.survey.dto.SurveyDetailResponse;
import com.survey.survey.entity.Survey;
import com.survey.survey.mapper.SurveyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SurveyService {

    private final SurveyMapper surveyMapper;

    /** 创建空问卷，返回新建的实体 */
    @Transactional(rollbackFor = Exception.class)
    public Survey create(CreateSurveyRequest request, Long creatorId) {
        if (request.title() == null || request.title().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "问卷标题不能为空");
        }
        Survey survey = new Survey();
        survey.setCreatorId(creatorId);
        survey.setTitle(request.title());
        survey.setDescription(request.description());
        survey.setStatus("DRAFT");
        survey.setVersion(1);
        surveyMapper.insert(survey);
        return survey;
    }

    /** 当前用户的问卷列表，分页 */
    public PageResult<SurveyDetailResponse> list(Long creatorId, int page, int size) {
        Page<Survey> p = new Page<>(page, size);
        surveyMapper.selectPage(p,
                new LambdaQueryWrapper<Survey>()
                        .eq(Survey::getCreatorId, creatorId)
                        .orderByAsc(Survey::getId));
        var list = p.getRecords().stream().map(this::toDetail).toList();
        return new PageResult<>(list, p.getTotal(), page, size);
    }

    /** 问卷详情，校验归属 */
    public SurveyDetailResponse detail(Long surveyId, Long creatorId) {
        return toDetail(requireOwned(surveyId, creatorId));
    }

    /** 校验归属并返回实体，供其他 Service 复用 */
    public Survey requireOwned(Long surveyId, Long creatorId) {
        Survey survey = surveyMapper.selectById(surveyId);
        if (survey == null || !survey.getCreatorId().equals(creatorId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return survey;
    }

    /** 实体转详情响应，供其他 Controller 复用 */
    public SurveyDetailResponse toDetail(Survey s) {
        String shareUrl = s.getShareCode() == null ? null : "/s/" + s.getShareCode();
        return new SurveyDetailResponse(
                s.getId(), s.getTitle(), s.getDescription(), s.getStatus(),
                s.getShareCode(), shareUrl, s.getVersion(), s.getPublishedVersion(),
                s.getPublishedAt(), s.getCreatedAt());
    }
}