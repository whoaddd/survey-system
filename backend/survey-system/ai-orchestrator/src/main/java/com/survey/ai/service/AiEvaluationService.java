package com.survey.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survey.ai.dto.AiScoreResult;
import com.survey.ai.dto.EvaluateRequest;
import com.survey.ai.entity.AiEvaluation;
import com.survey.ai.mapper.AiEvaluationMapper;
import com.survey.common.BusinessException;
import com.survey.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiEvaluationService {

    private final ChatClient chatClient;
    private final AiEvaluationMapper aiEvaluationMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
            你是一个客观的问卷回答质量评估员。
            你必须严格输出一个 JSON 对象，包含以下三个字段：
            - rating：整数，1 到 5 之间
            - reason：字符串，不超过 100 字的评分理由
            - confidence：小数，0.0 到 1.0 之间

            输出要求（非常重要）：
            1. 只输出 JSON，不要任何解释性文字，不要 markdown 代码块。
            2. 字段值中禁止使用英文双引号（"），如需强调请使用中文引号「」或单引号（'）。
            3. 只输出一个 JSON 对象，不要输出数组。
            """;

    public AiScoreResult evaluate(Long responseId, EvaluateRequest req) {
        if (req.answerText() == null || req.answerText().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(), "答案内容不能为空");
        }

        String userPrompt = String.format(
                "请对以下问卷回答进行质量评分。\n题目：%s\n回答：%s",
                req.questionTitle(), req.answerText()
        );

        String rawText;
        try {
            rawText = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("[AiEvaluation] 模型调用失败, responseId={}, questionId={}",
                    responseId, req.questionId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(),
                    "AI 评分服务暂不可用，请稍后重试");
        }

        AiScoreResult result = parseModelOutput(rawText);

        // 业务后校验
        int rating = Math.max(1, Math.min(5, result.rating()));
        double confidence = Math.max(0.0, Math.min(1.0, result.confidence()));

        final String MODEL_NAME = "gpt-4o-mini";
        final String PROMPT_VERSION = "v1.0";

        // 落库
        AiEvaluation existing = aiEvaluationMapper.selectOne(
                new LambdaQueryWrapper<AiEvaluation>()
                        .eq(AiEvaluation::getResponseId, responseId)
                        .eq(AiEvaluation::getQuestionId, req.questionId())
                        .eq(AiEvaluation::getModelName, MODEL_NAME));

        AiEvaluation entity = existing != null ? existing : new AiEvaluation();
        entity.setResponseId(responseId);
        entity.setQuestionId(req.questionId());
        entity.setRating(rating);
        entity.setReason(result.reason());
        entity.setConfidence(BigDecimal.valueOf(confidence));
        entity.setModelName(MODEL_NAME);
        entity.setPromptVersion(PROMPT_VERSION);
        entity.setStatus("SUCCESS");
        entity.setCreatedAt(LocalDateTime.now());  // 语义：最近一次评分时间

        if (existing != null) {
            aiEvaluationMapper.updateById(entity);
        } else {
            try {
                aiEvaluationMapper.insert(entity);
            } catch (DuplicateKeyException e) {
                // 并发兜底：另一个请求刚好抢先插入，改为按唯一键更新
                AiEvaluation conflict = aiEvaluationMapper.selectOne(
                        new LambdaQueryWrapper<AiEvaluation>()
                                .eq(AiEvaluation::getResponseId, responseId)
                                .eq(AiEvaluation::getQuestionId, req.questionId())
                                .eq(AiEvaluation::getModelName, MODEL_NAME));
                if (conflict != null) {
                    entity.setId(conflict.getId());
                    aiEvaluationMapper.updateById(entity);
                } else {
                    // 唯一索引冲突但查不到，说明有脏数据，抛出明确错误
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(),
                            "评分记录写入冲突，请稍后重试");
                }
            }
        }

        return new AiScoreResult(rating, result.reason(), confidence);
    }

    // ===== 解析核心 =====

    private AiScoreResult parseModelOutput(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(),
                    "AI 返回内容为空，请稍后重试");
        }

        String cleaned = sanitize(rawText);

        try {
            JsonNode node = objectMapper.readTree(cleaned);
            int rating = node.has("rating") ? node.get("rating").asInt() : 3;
            double confidence = node.has("confidence") ? node.get("confidence").asDouble() : 0.5;
            String reason = node.has("reason") ? node.get("reason").asText() : "";
            return new AiScoreResult(rating, reason, confidence);
        } catch (Exception e) {
            log.error("[AiEvaluation] 无法解析模型输出, cleaned={}", cleaned, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(),
                    "AI 返回格式异常，请稍后重试");
        }
    }

    /**
     * 清洗模型输出：
     * 1. 去掉 markdown 代码块标记
     * 2. 提取第一个 { 到最后一个 } 的内容
     * 3. 状态机修复字符串内的裸双引号
     */
    private String sanitize(String raw) {
        String s = raw.trim();

        // 去掉 ```json ... ``` 或 ``` ... ```
        s = s.replaceAll("^```(?:json)?\\s*", "");
        s = s.replaceAll("\\s*```$", "");

        // 提取最外层 JSON 对象
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(),
                    "AI 返回内容不包含 JSON 对象");
        }
        s = s.substring(start, end + 1);

        return fixUnescapedQuotes(s);
    }

    /**
     * 状态机修复字符串值内部的裸双引号。
     * 判断规则：在字符串内部遇到 "，看其后第一个非空白字符：
     *   - 是 , } ] : → 这是字符串结束的双引号
     *   - 否则       → 这是字符串内的裸双引号，转义为 \"
     */
    private String fixUnescapedQuotes(String json) {
        StringBuilder sb = new StringBuilder(json.length() + 16);
        boolean inString = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '\\' && inString && i + 1 < json.length()) {
                // 已转义的字符，整体拷贝
                sb.append(c).append(json.charAt(i + 1));
                i++;
                continue;
            }

            if (c == '"') {
                if (!inString) {
                    inString = true;
                    sb.append(c);
                } else {
                    // 在字符串内，判断是否为合法的字符串结束
                    int j = i + 1;
                    while (j < json.length() && Character.isWhitespace(json.charAt(j))) j++;
                    boolean isEnd = j >= json.length()
                            || json.charAt(j) == ','
                            || json.charAt(j) == '}'
                            || json.charAt(j) == ']'
                            || json.charAt(j) == ':';

                    if (isEnd) {
                        inString = false;
                        sb.append(c);
                    } else {
                        // 字符串内的裸双引号，转义
                        sb.append("\\\"");
                    }
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}