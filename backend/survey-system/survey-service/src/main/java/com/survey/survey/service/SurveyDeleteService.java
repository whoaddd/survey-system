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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SurveyDeleteService {

    private final SurveyMapper surveyMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper optionMapper;

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long surveyId, Long userId) {
        // 1. 归属校验
        Survey survey = surveyMapper.selectById(surveyId);
        if (survey == null || !survey.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 2. 状态校验：已发布不可直接删
        if ("PUBLISHED".equals(survey.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                    "已发布问卷不可直接删除，请先关闭");
        }

        // 3. 级联删除所有 version 的题目和选项
        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getSurveyId, surveyId));
        if (!questions.isEmpty()) {
            List<Long> questionIds = questions.stream().map(Question::getId).toList();
            optionMapper.delete(new LambdaQueryWrapper<QuestionOption>()
                    .in(QuestionOption::getQuestionId, questionIds));
            questionMapper.deleteBatchIds(questionIds);
        }

        // 4. 删问卷本身
        surveyMapper.deleteById(surveyId);
    }
}