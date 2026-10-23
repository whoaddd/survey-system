package com.survey.response.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survey.common.BusinessException;
import com.survey.common.ErrorCode;
import com.survey.response.dto.SubmitResponseRequest;
import com.survey.response.dto.SubmitResponseResult;
import com.survey.response.entity.QuestionRef;
import com.survey.response.entity.Response;
import com.survey.response.entity.ResponseAnswer;
import com.survey.response.entity.SurveyRef;
import com.survey.response.mapper.ResponseAnswerMapper;
import com.survey.response.mapper.ResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ResponseWriteService {

    private static final String RECEIPT_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int RECEIPT_LEN = 16;

    private final ResponseMapper responseMapper;
    private final ResponseAnswerMapper answerMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecureRandom random = new SecureRandom();

    /**
     * 只负责写 response_db。所有校验已经在调用方完成。
     * @param validOptions 每个题的合法选项 id 集合，用于决定是否落库
     */
    @Transactional(rollbackFor = Exception.class)
    public SubmitResponseResult write(
            SurveyRef survey,
            SubmitResponseRequest req,
            String clientIp,
            String userAgent,
            List<QuestionRef> questions,
            Map<Long, SubmitResponseRequest.AnswerItem> answerByQuestion) {

        // 1. 写 response
        Response response = new Response();
        response.setSurveyId(survey.getId());
        response.setSurveyVersion(survey.getPublishedVersion());
        response.setRespondentKey(req.respondentKey());
        response.setReceiptCode(generateReceiptCode());
        response.setIpHash(sha256(clientIp));
        response.setUserAgent(truncate(userAgent, 250));
        response.setSubmittedAt(LocalDateTime.now());

        try {
            responseMapper.insert(response);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.CONFLICT.getCode(), "你已提交过该问卷");
        }

        // 2. 写 response_answer
        for (QuestionRef q : questions) {
            SubmitResponseRequest.AnswerItem a = answerByQuestion.get(q.getId());
            if (isEmptyAnswer(a)) continue;

            ResponseAnswer answer = new ResponseAnswer();
            answer.setResponseId(response.getId());
            answer.setQuestionId(q.getId());
            switch (q.getType()) {
                case "SINGLE", "MULTI" -> answer.setSelectedOptionIds(toJson(a.selectedOptionIds()));
                case "RATING" -> answer.setRatingValue(a.ratingValue());
                case "TEXT", "IMAGE" -> answer.setAnswerText(a.answerText());
            }
            answer.setCreatedAt(LocalDateTime.now());
            answerMapper.insert(answer);
        }

        return new SubmitResponseResult(response.getReceiptCode(), response.getSubmittedAt());
    }

    // ===== 私有工具方法 =====
    private boolean isEmptyAnswer(SubmitResponseRequest.AnswerItem a) {
        if (a == null) return true;
        boolean noText = a.answerText() == null || a.answerText().isBlank();
        boolean noOptions = a.selectedOptionIds() == null || a.selectedOptionIds().isEmpty();
        boolean noRating = a.ratingValue() == null;
        boolean noAttachment = a.attachmentKey() == null || a.attachmentKey().isBlank();
        return noText && noOptions && noRating && noAttachment;
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }

    private String generateReceiptCode() {
        StringBuilder sb = new StringBuilder(RECEIPT_LEN);
        for (int i = 0; i < RECEIPT_LEN; i++) {
            sb.append(RECEIPT_CHARS.charAt(random.nextInt(RECEIPT_CHARS.length())));
        }
        return sb.toString();
    }

    private String sha256(String s) {
        if (s == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}