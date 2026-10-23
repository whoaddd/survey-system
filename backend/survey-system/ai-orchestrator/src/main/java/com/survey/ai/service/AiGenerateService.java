package com.survey.ai.service;

import com.survey.ai.dto.GeneratedSurvey;
import com.survey.common.BusinessException;
import com.survey.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiGenerateService {

    private final ChatClient chatClient;

    @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public GeneratedSurvey generate(String topic, int count) {
        if (topic == null || topic.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "主题不能为空");
        }
        if (count < 1 || count > 10) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "题目数量须在 1~10 之间");
        }

        String userPrompt = String.format(
                "请围绕「%s」设计 %d 道问卷题目，题型需多样，包含至少一道评分题。",
                topic, count
        );

        try {
            return chatClient.prompt()
                    .system(sys -> sys.text(new ClassPathResource("prompts/generate-survey.st")))
                    .user(userPrompt)
                    .call()
                    .entity(GeneratedSurvey.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(),
                    "AI 生成题目失败，请稍后重试");
        }
    }
}