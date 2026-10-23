package com.survey.response.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.survey.common.BusinessException;
import com.survey.common.ErrorCode;
import com.survey.response.dto.SubmitResponseRequest;
import com.survey.response.dto.SubmitResponseResult;
import com.survey.response.entity.*;
import com.survey.response.mapper.*;
import com.survey.response.util.RateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublicResponseService {

    private final SurveyRefMapper surveyRefMapper;
    private final QuestionRefMapper questionRefMapper;
    private final OptionRefMapper optionRefMapper;
    private final ResponseMapper responseMapper;
    private final ResponseWriteService responseWriteService;   // 独立事务 Bean
    private final RateLimiter rateLimiter;

    @Value("${rate-limit.submit-per-hour:5}")
    private int submitPerHour;

    /**
     * 注意：方法上绝对不能加 @Transactional。
     * 一旦加事务，连接会绑定到主数据源 response_db，
     * 后面 @DS("survey") 的 Mapper 调用就会失效，导致查 response_db.survey 报错。
     */
    public SubmitResponseResult submit(SubmitResponseRequest req, String clientIp, String userAgent) {
        // 1. 基本参数
        if (req.shareCode() == null || req.respondentKey() == null
                || req.respondentKey().isBlank() || req.answers() == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "缺少必要参数");
        }

        // 2. IP 限流（走 Redis）
        String ipKey = "rl:submit:ip:" + clientIp;
        if (!rateLimiter.tryAcquire(ipKey, submitPerHour, Duration.ofHours(1))) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS.getCode(), "提交过于频繁，请稍后再试");
        }

        // ========== 以下全部是"读"，无事务，@DS("survey") 生效 ==========

        // 3. 查问卷（走 survey_db）
        SurveyRef survey = surveyRefMapper.selectOne(
                new LambdaQueryWrapper<SurveyRef>()
                        .eq(SurveyRef::getShareCode, req.shareCode()));
        if (survey == null || !"PUBLISHED".equals(survey.getStatus())
                || survey.getPublishedVersion() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 4. 查重（走 response_db）
        Long exists = responseMapper.selectCount(
                new LambdaQueryWrapper<Response>()
                        .eq(Response::getSurveyId, survey.getId())
                        .eq(Response::getRespondentKey, req.respondentKey()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(), "你已提交过该问卷");
        }

        // 5. 取题目（走 survey_db）
        List<QuestionRef> questions = questionRefMapper.selectList(
                new LambdaQueryWrapper<QuestionRef>()
                        .eq(QuestionRef::getSurveyId, survey.getId())
                        .eq(QuestionRef::getVersion, survey.getPublishedVersion()));
        if (questions.isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(), "问卷题目为空");
        }

        // 6. 取选项（走 survey_db）
        List<Long> qIds = questions.stream().map(QuestionRef::getId).toList();
        List<OptionRef> allOptions = optionRefMapper.selectList(
                new LambdaQueryWrapper<OptionRef>().in(OptionRef::getQuestionId, qIds));
        Map<Long, Set<Long>> validOptionsByQuestion = allOptions.stream()
                .collect(Collectors.groupingBy(OptionRef::getQuestionId,
                        Collectors.mapping(OptionRef::getId, Collectors.toSet())));

        // 7. 校验答案
        Map<Long, QuestionRef> questionMap = questions.stream()
                .collect(Collectors.toMap(QuestionRef::getId, q -> q));
        Map<Long, SubmitResponseRequest.AnswerItem> answerByQuestion = new HashMap<>();

        for (SubmitResponseRequest.AnswerItem a : req.answers()) {
            if (a.questionId() == null) continue;
            if (!questionMap.containsKey(a.questionId())) {
                throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "题目不属于当前问卷版本");
            }
            answerByQuestion.put(a.questionId(), a);
        }

        for (QuestionRef q : questions) {
            SubmitResponseRequest.AnswerItem a = answerByQuestion.get(q.getId());
            boolean required = q.getRequired() != null && q.getRequired() == 1;

            if (required && isEmptyAnswer(a)) {
                throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "必填题未作答：" + q.getTitle());
            }
            if (a == null) continue;

            switch (q.getType()) {
                case "SINGLE" -> {
                    if (a.selectedOptionIds() == null || a.selectedOptionIds().size() != 1) {
                        throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                                "单选题必须且只能选 1 项：" + q.getTitle());
                    }
                    validateOption(a.selectedOptionIds().get(0),
                            validOptionsByQuestion.get(q.getId()), q.getTitle());
                }
                case "MULTI" -> {
                    if (a.selectedOptionIds() == null || a.selectedOptionIds().isEmpty()) {
                        if (required) {
                            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                                    "多选题至少选 1 项：" + q.getTitle());
                        }
                    } else {
                        for (Long oid : a.selectedOptionIds()) {
                            validateOption(oid, validOptionsByQuestion.get(q.getId()), q.getTitle());
                        }
                    }
                }
                case "RATING" -> {
                    if (a.ratingValue() == null) {
                        if (required) {
                            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                                    "评分题未打分：" + q.getTitle());
                        }
                    } else {
                        int min = q.getScoreMin() == null ? 1 : q.getScoreMin();
                        int max = q.getScoreMax() == null ? 5 : q.getScoreMax();
                        if (a.ratingValue() < min || a.ratingValue() > max) {
                            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                                    "评分超出范围（" + min + "~" + max + "）：" + q.getTitle());
                        }
                    }
                }
                case "TEXT", "IMAGE" -> { }
                default -> throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                        "未知题型：" + q.getType());
            }
        }

        // ========== 写：交给独立事务 Bean ==========
        return responseWriteService.write(survey, req, clientIp, userAgent, questions, answerByQuestion);
    }

    private void validateOption(Long optionId, Set<Long> validSet, String questionTitle) {
        if (validSet == null || !validSet.contains(optionId)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                    "选项不属于该题：" + questionTitle);
        }
    }

    private boolean isEmptyAnswer(SubmitResponseRequest.AnswerItem a) {
        if (a == null) return true;
        boolean noText = a.answerText() == null || a.answerText().isBlank();
        boolean noOptions = a.selectedOptionIds() == null || a.selectedOptionIds().isEmpty();
        boolean noRating = a.ratingValue() == null;
        boolean noAttachment = a.attachmentKey() == null || a.attachmentKey().isBlank();
        return noText && noOptions && noRating && noAttachment;
    }
}