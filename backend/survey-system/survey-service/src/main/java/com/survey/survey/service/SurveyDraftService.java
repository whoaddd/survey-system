package com.survey.survey.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.survey.common.BusinessException;
import com.survey.common.ErrorCode;
import com.survey.survey.dto.QuestionView;
import com.survey.survey.dto.SaveSurveyRequest;
import com.survey.survey.entity.Question;
import com.survey.survey.entity.QuestionOption;
import com.survey.survey.entity.Survey;
import com.survey.survey.mapper.QuestionMapper;
import com.survey.survey.mapper.QuestionOptionMapper;
import com.survey.survey.mapper.SurveyMapper;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SurveyDraftService {

    private static final Set<String> VALID_TYPES =
            Set.of("TEXT", "IMAGE", "SINGLE", "MULTI", "RATING");
    private static final Set<String> TYPES_NEED_OPTIONS =
            Set.of("SINGLE", "MULTI");

    private final SurveyMapper surveyMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper optionMapper;

    @Transactional(rollbackFor = Exception.class)
    public List<QuestionView> saveDraft(Long surveyId, SaveSurveyRequest request, Long userId) {
        // 1. 归属与状态校验
        Survey survey = surveyMapper.selectById(surveyId);
        if (survey == null || !survey.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (!"DRAFT".equals(survey.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                    "已发布或已关闭的问卷不可编辑，请使用复制为新草稿");
        }

        // 2. 更新问卷标题与描述
        if (request.title() != null && !request.title().isBlank()) {
            survey.setTitle(request.title());
        }
        survey.setDescription(request.description());
        surveyMapper.updateById(survey);

        // 3. 归一化 + 校验：返回处理后的新列表（sanitize、title 补齐都在这一步完成）
        List<SaveSurveyRequest.QuestionDto> normalized =
                normalizeQuestions(request.questions() == null ? List.of() : request.questions());

        // 4. 删除当前 version 下的旧数据（先选项后题目）
        List<Question> oldQuestions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getSurveyId, surveyId)
                        .eq(Question::getVersion, survey.getVersion()));
        if (!oldQuestions.isEmpty()) {
            List<Long> oldQuestionIds = oldQuestions.stream().map(Question::getId).toList();
            optionMapper.delete(new LambdaQueryWrapper<QuestionOption>()
                    .in(QuestionOption::getQuestionId, oldQuestionIds));
            questionMapper.deleteBatchIds(oldQuestionIds);
        }

        // 5. 按归一化后的顺序重建题目与选项
        List<QuestionView> result = new ArrayList<>(normalized.size());
        int order = 1;
        for (SaveSurveyRequest.QuestionDto dto : normalized) {
            Question q = new Question();
            q.setSurveyId(surveyId);
            q.setVersion(survey.getVersion());
            q.setType(dto.type());
            q.setTitle(dto.title());
            q.setContentHtml(dto.contentHtml());      // 已经是 sanitize 后的
            q.setImageKey(dto.imageKey());
            q.setRequired(Boolean.TRUE.equals(dto.required()) ? 1 : 0);
            q.setScoreEnabled(Boolean.TRUE.equals(dto.scoreEnabled()) ? 1 : 0);
            q.setScoreMin(dto.scoreMin() == null ? 1 : dto.scoreMin());
            q.setScoreMax(dto.scoreMax() == null ? 5 : dto.scoreMax());
            q.setSortOrder(order++);
            questionMapper.insert(q);

            List<QuestionView.OptionView> optionViews = new ArrayList<>();
            if (dto.options() != null) {
                int optOrder = 1;
                for (SaveSurveyRequest.OptionDto od : dto.options()) {
                    QuestionOption o = new QuestionOption();
                    o.setQuestionId(q.getId());
                    o.setLabel(od.label());
                    o.setValue(od.value());
                    o.setSortOrder(optOrder++);
                    optionMapper.insert(o);
                    // ✅ 修复 1：真正把选项加入返回列表
                    optionViews.add(new QuestionView.OptionView(
                            o.getId(), o.getLabel(), o.getValue(), o.getSortOrder()));
                }
            }

            result.add(new QuestionView(
                    q.getId(), q.getType(), q.getTitle(), q.getContentHtml(), q.getImageKey(),
                    q.getRequired() == 1, q.getScoreEnabled() == 1,
                    q.getScoreMin(), q.getScoreMax(), q.getSortOrder(), optionViews));
        }
        return result;
    }

    /**
     * 归一化：sanitize + title 补齐 + 全量校验，返回处理后的新列表。
     * 关键：不能修改原列表中的元素（record 不可变），必须构造新列表。
     */
    private List<SaveSurveyRequest.QuestionDto> normalizeQuestions(
            List<SaveSurveyRequest.QuestionDto> raw) {
        List<SaveSurveyRequest.QuestionDto> normalized = new ArrayList<>(raw.size());

        for (int i = 0; i < raw.size(); i++) {
            SaveSurveyRequest.QuestionDto q = raw.get(i);
            String prefix = "第 " + (i + 1) + " 题";

            // 1. 题型校验
            if (q.type() == null || !VALID_TYPES.contains(q.type())) {
                throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                        prefix + "的题型非法：" + q.type());
            }

            // 2. sanitize contentHtml（结果会进入新 DTO）
            String safeHtml = (q.contentHtml() == null || q.contentHtml().isBlank())
                    ? null : sanitizeHtml(q.contentHtml());

            // 3. title 补齐（空 title 从 contentHtml 提取纯文本）
            String title = q.title();
            if (title == null || title.isBlank()) {
                String extracted = extractPlainText(safeHtml);
                if (extracted.isEmpty()) {
                    if (safeHtml != null && !safeHtml.isBlank()) {
                        title = "（图片题）";
                    } else {
                        throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                                prefix + "的题干不能为空");
                    }
                } else {
                    title = extracted;
                }
            }

            // 4. 选项校验（含分值校验）
            if (TYPES_NEED_OPTIONS.contains(q.type())) {
                if (q.options() == null || q.options().size() < 2) {
                    throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                            prefix + "（单选/多选）至少需要 2 个选项");
                }
                boolean needScore = Boolean.TRUE.equals(q.scoreEnabled());
                for (int j = 0; j < q.options().size(); j++) {
                    SaveSurveyRequest.OptionDto o = q.options().get(j);
                    if (o.label() == null || o.label().isBlank()) {
                        throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                                prefix + "的第 " + (j + 1) + " 个选项文案不能为空");
                    }
                    if (o.value() == null || o.value().isBlank()) {
                        throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                                prefix + "的第 " + (j + 1) + " 个选项值不能为空");
                    }
                }
            }

            // 5. 评分范围校验（RATING）
            if (Boolean.TRUE.equals(q.scoreEnabled())) {
                int min = q.scoreMin() == null ? 1 : q.scoreMin();
                int max = q.scoreMax() == null ? 5 : q.scoreMax();
                if (min >= max) {
                    throw new BusinessException(ErrorCode.PARAM_INVALID.getCode(),
                            prefix + "的评分范围非法：min 必须小于 max");
                }
            }

            // 6. 构造归一化后的新 DTO（title 和 contentHtml 都已是修正后的值）
            normalized.add(new SaveSurveyRequest.QuestionDto(
                    q.type(), title, safeHtml, q.imageKey(),
                    q.required(), q.scoreEnabled(),
                    q.scoreMin(), q.scoreMax(), q.options()));
        }
        return normalized;
    }

    private String extractPlainText(String html) {
        if (html == null || html.isBlank()) return "";
        return html.replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String sanitizeHtml(String html) {
        return Jsoup.clean(html, Safelist.basicWithImages()
                .addTags("p", "br", "span", "h1", "h2", "h3")
                .addAttributes("img", "src", "alt", "width", "height")
                .addAttributes("span", "style"));
    }

    public List<QuestionView> listCurrentDraft(Long surveyId, Long userId) {
        Survey survey = surveyMapper.selectById(surveyId);
        if (survey == null || !survey.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        List<Question> questions = questionMapper.selectList(
                new LambdaQueryWrapper<Question>()
                        .eq(Question::getSurveyId, surveyId)
                        .eq(Question::getVersion, survey.getVersion())
                        .orderByAsc(Question::getSortOrder));

        List<QuestionView> views = new ArrayList<>(questions.size());
        for (Question q : questions) {
            List<QuestionOption> options = optionMapper.selectList(
                    new LambdaQueryWrapper<QuestionOption>()
                            .eq(QuestionOption::getQuestionId, q.getId())
                            .orderByAsc(QuestionOption::getSortOrder));

            // ✅ 修复 4：补齐括号
            views.add(new QuestionView(
                    q.getId(), q.getType(), q.getTitle(), q.getContentHtml(), q.getImageKey(),
                    q.getRequired() == 1, q.getScoreEnabled() == 1,
                    q.getScoreMin(), q.getScoreMax(), q.getSortOrder(),
                    options.stream()
                            .map(o -> new QuestionView.OptionView(
                                    o.getId(), o.getLabel(), o.getValue(), o.getSortOrder()))
                            .toList()));
        }
        return views;
    }
}