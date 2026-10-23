package com.survey.response.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.survey.common.BusinessException;
import com.survey.common.ErrorCode;
import com.survey.common.PageResult;
import com.survey.response.dto.ResponseSummary;
import com.survey.response.entity.Response;
import com.survey.response.entity.SurveyRef;
import com.survey.response.mapper.ResponseMapper;
import com.survey.response.mapper.SurveyRefMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResponseListService {

    private final SurveyRefMapper surveyRefMapper;
    private final ResponseMapper responseMapper;

    public PageResult<ResponseSummary> list(Long surveyId, Long userId, int page, int size) {
        // 1. 归属校验：问卷必须存在且属于当前用户
        SurveyRef survey = surveyRefMapper.selectById(surveyId);
        if (survey == null || !survey.getCreatorId().equals(userId)) {
            // 不区分"不存在"和"不属于你"，统一 404，避免枚举
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }

        // 2. 分页参数兜底
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;   // 单页上限，防止恶意拉全量

        // 3. 分页查询
        Page<Response> p = new Page<>(page, size);
        responseMapper.selectPage(p,
                new LambdaQueryWrapper<Response>()
                        .eq(Response::getSurveyId, surveyId)
                        .orderByDesc(Response::getSubmittedAt));

        // 4. 转 DTO
        List<ResponseSummary> list = p.getRecords().stream()
                .map(r -> new ResponseSummary(
                        r.getId(), r.getReceiptCode(),
                        r.getRespondentKey(), r.getSubmittedAt()))
                .toList();

        return new PageResult<>(list, p.getTotal(), page, size);
    }
}