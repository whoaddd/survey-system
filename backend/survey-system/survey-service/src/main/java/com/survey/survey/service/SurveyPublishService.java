package com.survey.survey.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.survey.common.BusinessException;
import com.survey.common.ErrorCode;
import com.survey.survey.entity.Question;
import com.survey.survey.entity.QuestionOption;
import com.survey.survey.entity.Survey;
import com.survey.survey.mapper.QuestionMapper;
import com.survey.survey.mapper.QuestionOptionMapper;
import com.survey.survey.mapper.SurveyMapper;
import com.survey.survey.util.ShareCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SurveyPublishService {

    private static final int MAX_SHARE_CODE_RETRY = 3;

    private final SurveyMapper surveyMapper;
    private final QuestionMapper questionMapper;
    private final ShareCodeGenerator shareCodeGenerator;
    private final QuestionOptionMapper optionMapper;

    @Transactional
    public Survey publish(Long surveyId, Long userId) {
        Survey survey = surveyMapper.selectById(surveyId);

        // 1. 归属校验
        if (survey == null || !survey.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 2. 状态机校验：仅 DRAFT 可发布
        if (!"DRAFT".equals(survey.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                    "仅草稿状态的问卷可发布，当前状态：" + survey.getStatus());
        }

        // 3. 题目完整性校验：当前 version 下至少一道题
        Long questionCount = questionMapper.selectCount(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getSurveyId, surveyId)
                        .eq(Question::getVersion, survey.getVersion()));
        if (questionCount == null || questionCount == 0) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                    "问卷至少需要一道题目才能发布");
        }

        // 4. 生成 shareCode，唯一索引冲突时重试
        String shareCode = generateUniqueShareCode();

        // 5. 冻结快照：publishedVersion = version
        survey.setShareCode(shareCode);
        survey.setPublishedVersion(survey.getVersion());
        survey.setStatus("PUBLISHED");
        survey.setPublishedAt(LocalDateTime.now());
        surveyMapper.updateById(survey);

        return survey;
    }

    private String generateUniqueShareCode() {
        for (int i = 0; i < MAX_SHARE_CODE_RETRY; i++) {
            String code = shareCodeGenerator.generate();
            Long exists = surveyMapper.selectCount(
                    new LambdaQueryWrapper<Survey>().eq(Survey::getShareCode, code));
            if (exists == null || exists == 0) {
                return code;
            }
        }
        // 理论上 8 位 base62 碰撞概率极低，重试 3 次仍冲突说明系统异常
        throw new BusinessException(ErrorCode.SYSTEM_ERROR.getCode(),
                "分享码生成失败，请重试");
    }

    @Transactional
    public Survey close(Long surveyId, Long userId) {
        Survey survey = surveyMapper.selectById(surveyId);
        if (survey == null || !survey.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!"PUBLISHED".equals(survey.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                    "仅已发布问卷可关闭，当前状态：" + survey.getStatus());
        }
        survey.setStatus("CLOSED");
        survey.setClosedAt(LocalDateTime.now());
        surveyMapper.updateById(survey);
        return survey;
    }

    @Transactional
    public Survey copyAsNewDraft(Long surveyId, Long userId) {
        Survey origin = surveyMapper.selectById(surveyId);
        if (origin == null || !origin.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!"PUBLISHED".equals(origin.getStatus()) && !"CLOSED".equals(origin.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                    "仅已发布或已关闭问卷可复制为新草稿");
        }

        // 1. 新 survey：version = 已发布版本 + 1，状态 DRAFT，不带 shareCode
        Survey draft = new Survey();
        draft.setCreatorId(userId);
        draft.setTitle(origin.getTitle() + "（副本）");
        draft.setDescription(origin.getDescription());
        draft.setStatus("DRAFT");
        draft.setVersion(origin.getPublishedVersion() + 1);
        draft.setPublishedVersion(null);
        surveyMapper.insert(draft);

        // 2. 复制已发布版本的题目到新 version
        List<Question> sourceQuestions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getSurveyId, origin.getId())
                        .eq(Question::getVersion, origin.getPublishedVersion())
                        .orderByAsc(Question::getSortOrder));

        for (Question src : sourceQuestions) {
            Question copy = new Question();
            copy.setSurveyId(draft.getId());
            copy.setVersion(draft.getVersion());
            copy.setType(src.getType());
            copy.setTitle(src.getTitle());
            copy.setContentHtml(src.getContentHtml());
            copy.setImageKey(src.getImageKey());
            copy.setRequired(src.getRequired());
            copy.setScoreEnabled(src.getScoreEnabled());
            copy.setScoreMin(src.getScoreMin());
            copy.setScoreMax(src.getScoreMax());
            copy.setSortOrder(src.getSortOrder());
            questionMapper.insert(copy);

            // 复制选项
            List<QuestionOption> srcOptions = optionMapper.selectList(
                    new LambdaQueryWrapper<QuestionOption>()
                            .eq(QuestionOption::getQuestionId, src.getId())
                            .orderByAsc(QuestionOption::getSortOrder));
            for (QuestionOption so : srcOptions) {
                QuestionOption oc = new QuestionOption();
                oc.setQuestionId(copy.getId());
                oc.setLabel(so.getLabel());
                oc.setValue(so.getValue());
                oc.setSortOrder(so.getSortOrder());
                optionMapper.insert(oc);
            }
        }
        return draft;
    }
}