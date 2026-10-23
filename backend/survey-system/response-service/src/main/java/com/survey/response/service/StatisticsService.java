package com.survey.response.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survey.common.BusinessException;
import com.survey.common.ErrorCode;
import com.survey.response.dto.SurveyStatistics;
import com.survey.response.entity.*;
import com.survey.response.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final SurveyRefMapper surveyRefMapper;
    private final QuestionRefMapper questionRefMapper;
    private final OptionRefMapper optionRefMapper;
    private final ResponseMapper responseMapper;
    private final ResponseAnswerMapper answerMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SurveyStatistics statistics(Long surveyId, Long userId) {
        SurveyRef survey = surveyRefMapper.selectById(surveyId);
        if (survey == null || !Objects.equals(survey.getCreatorId(), userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (survey.getPublishedVersion() == null) {
            return new SurveyStatistics(0, List.of());
        }

        long totalResponses = responseMapper.selectCount(
                new LambdaQueryWrapper<Response>().eq(Response::getSurveyId, surveyId));

        List<QuestionRef> questions = questionRefMapper.selectList(
                new LambdaQueryWrapper<QuestionRef>()
                        .eq(QuestionRef::getSurveyId, surveyId)
                        .eq(QuestionRef::getVersion, survey.getPublishedVersion())
                        .orderByAsc(QuestionRef::getSortOrder));

        List<Long> responseIds = responseMapper.selectList(
                        new LambdaQueryWrapper<Response>()
                                .select(Response::getId)
                                .eq(Response::getSurveyId, surveyId))
                .stream().map(Response::getId).toList();

        List<ResponseAnswer> allAnswers = responseIds.isEmpty()
                ? List.of()
                : answerMapper.selectList(
                new LambdaQueryWrapper<ResponseAnswer>()
                        .in(ResponseAnswer::getResponseId, responseIds));

        Map<Long, String> optionLabelMap = Map.of();
        if (!questions.isEmpty()) {
            List<Long> qIds = questions.stream().map(QuestionRef::getId).toList();
            optionLabelMap = optionRefMapper.selectList(
                            new LambdaQueryWrapper<OptionRef>().in(OptionRef::getQuestionId, qIds))
                    .stream().collect(Collectors.toMap(OptionRef::getId, OptionRef::getLabel));
        }

        Map<Long, List<ResponseAnswer>> byQuestion = allAnswers.stream()
                .collect(Collectors.groupingBy(ResponseAnswer::getQuestionId));

        Map<Long, String> finalOptionLabelMap = optionLabelMap;
        List<SurveyStatistics.QuestionStatistic> stats = questions.stream()
                .map(q -> aggregate(q, byQuestion.getOrDefault(q.getId(), List.of()),
                        finalOptionLabelMap))
                .toList();

        return new SurveyStatistics(totalResponses, stats);
    }

    private SurveyStatistics.QuestionStatistic aggregate(
            QuestionRef q, List<ResponseAnswer> answers,
            Map<Long, String> optionLabelMap) {

        Double avg = null;
        Map<String, Long> scoreDist = null;
        Map<String, Long> optionDist = null;
        List<String> textAnswers = null;

        switch (q.getType()) {
            case "RATING" -> {
                List<Integer> values = answers.stream()
                        .map(ResponseAnswer::getRatingValue)
                        .filter(Objects::nonNull)
                        .toList();
                if (!values.isEmpty()) {
                    avg = values.stream().mapToInt(Integer::intValue).average().orElse(0);
                    scoreDist = new TreeMap<>();
                    for (int v : values) {
                        scoreDist.merge(String.valueOf(v), 1L, Long::sum);
                    }
                }
            }
            case "SINGLE", "MULTI" -> {
                optionDist = new LinkedHashMap<>();
                for (ResponseAnswer a : answers) {
                    if (a.getSelectedOptionIds() == null) continue;
                    try {
                        List<Long> ids = objectMapper.readValue(
                                a.getSelectedOptionIds(),
                                new TypeReference<List<Long>>() {});
                        for (Long oid : ids) {
                            String label = optionLabelMap.getOrDefault(oid, "选项" + oid);
                            optionDist.merge(label, 1L, Long::sum);
                        }
                    } catch (Exception e) {
                        log.warn("[Statistics] 解析选项失败, answerId={}, raw={}",
                                a.getId(), a.getSelectedOptionIds());
                    }
                }
            }
            case "TEXT", "IMAGE" -> {
                textAnswers = answers.stream()
                        .map(ResponseAnswer::getAnswerText)
                        .filter(t -> t != null && !t.isBlank())
                        .toList();
            }
            default -> { }
        }

        return new SurveyStatistics.QuestionStatistic(
                q.getId(), q.getTitle(), q.getType(),
                avg, scoreDist, optionDist, textAnswers);
    }
}