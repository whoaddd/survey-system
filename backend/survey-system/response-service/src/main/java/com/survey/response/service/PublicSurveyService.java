package com.survey.response.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.survey.common.BusinessException;
import com.survey.common.ErrorCode;
import com.survey.response.dto.PublicSurveyResponse;
import com.survey.response.entity.OptionRef;
import com.survey.response.entity.QuestionRef;
import com.survey.response.entity.SurveyRef;
import com.survey.response.mapper.OptionRefMapper;
import com.survey.response.mapper.QuestionRefMapper;
import com.survey.response.mapper.SurveyRefMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicSurveyService {

    private final SurveyRefMapper surveyRefMapper;
    private final QuestionRefMapper questionRefMapper;
    private final OptionRefMapper optionRefMapper;

    public PublicSurveyResponse getByShareCode(String shareCode) {
        // 1. 按 shareCode 查问卷
        SurveyRef survey = surveyRefMapper.selectOne(
                new LambdaQueryWrapper<SurveyRef>()
                        .eq(SurveyRef::getShareCode, shareCode));

        // 2. 仅 PUBLISHED 可被答卷者看到；DRAFT/CLOSED 一律 404（不泄露存在性）
        if (survey == null || !"PUBLISHED".equals(survey.getStatus())
                || survey.getPublishedVersion() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 3. 取 published_version 对应的题目
        List<QuestionRef> questions = questionRefMapper.selectList(
                new LambdaQueryWrapper<QuestionRef>()
                        .eq(QuestionRef::getSurveyId, survey.getId())
                        .eq(QuestionRef::getVersion, survey.getPublishedVersion())
                        .orderByAsc(QuestionRef::getSortOrder));

        // 4. 一次性取所有选项，按 questionId 分组
        Map<Long, List<OptionRef>> optionMap = Map.of();
        if (!questions.isEmpty()) {
            List<Long> qIds = questions.stream().map(QuestionRef::getId).toList();
            List<OptionRef> options = optionRefMapper.selectList(
                    new LambdaQueryWrapper<OptionRef>()
                            .in(OptionRef::getQuestionId, qIds)
                            .orderByAsc(OptionRef::getSortOrder));
            optionMap = options.stream().collect(Collectors.groupingBy(OptionRef::getQuestionId));
        }

        // 5. 组装响应，过滤内部字段
        List<PublicSurveyResponse.QuestionView> questionViews = new ArrayList<>(questions.size());
        for (QuestionRef q : questions) {
            List<PublicSurveyResponse.OptionView> optionViews =
                    optionMap.getOrDefault(q.getId(), List.of()).stream()
                            .map(o -> new PublicSurveyResponse.OptionView(
                                    o.getId(), o.getLabel(), o.getValue(), o.getSortOrder()))
                            .toList();
            questionViews.add(new PublicSurveyResponse.QuestionView(
                    q.getId(), q.getType(), q.getTitle(), q.getContentHtml(), q.getImageKey(),
                    q.getRequired() == 1, q.getScoreEnabled() == 1,
                    q.getScoreMin(), q.getScoreMax(), q.getSortOrder(), optionViews));
        }

        return new PublicSurveyResponse(
                survey.getShareCode(), survey.getTitle(), survey.getDescription(),
                survey.getPublishedVersion(), questionViews);
    }
}