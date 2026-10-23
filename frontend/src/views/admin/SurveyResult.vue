<template>
  <div class="survey-result">
    <el-card shadow="never" class="toolbar">
      <div class="toolbar-inner">
        <div>
          <el-button :icon="ArrowLeft" link @click="$router.push('/admin/surveys')">
            返回列表
          </el-button>
          <span class="title">{{ surveyTitle }}</span>
        </div>
        <el-button :icon="Download" @click="handleExport">导出 CSV</el-button>
      </div>
    </el-card>

    <el-card v-if="stats" shadow="never" class="stats-overview">
      <el-row :gutter="16">
        <el-col :xs="24" :sm="8">
          <div class="metric">
            <div class="num">{{ stats.totalResponses }}</div>
            <div class="label">答卷总数</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8">
          <div class="metric">
            <div class="num">{{ ratedQuestions.length }}</div>
            <div class="label">评分题数量</div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="8">
          <div class="metric">
            <div class="num">{{ avgOfAverages }}</div>
            <div class="label">全部评分题均分</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card
      v-for="(q, idx) in stats?.questions ?? []"
      :key="q.questionId"
      shadow="never"
      class="q-card"
    >
      <div class="q-header">
        <span class="index">{{ idx + 1 }}.</span>
        <span class="q-title">{{ q.title }}</span>
        <el-tag size="small" type="info">{{ typeText(q.type) }}</el-tag>
      </div>

      <!-- ===== 评分题：均分 + 柱状图 ===== -->
      <div v-if="q.type === 'RATING' && q.averageScore !== null" class="q-chart">
        <div class="avg">均分：<strong>{{ q.averageScore.toFixed(2) }}</strong></div>
        <div :ref="(el) => setChartRef(q.questionId, el)" class="chart"></div>
      </div>

      <!-- ===== 单选/多选：饼图 + 平均得分 + AI 辅助评分 ===== -->
      <div
        v-else-if="['SINGLE', 'MULTI'].includes(q.type) && q.optionDistribution"
        class="q-chart"
      >
        <div v-if="q.averageScore !== null" class="avg">
          平均得分：<strong>{{ q.averageScore.toFixed(2) }}</strong>
          <span class="hint">（基于选项分值计算）</span>
        </div>
        <div :ref="(el) => setChartRef(q.questionId, el)" class="chart"></div>

        <div class="ai-eval-section">
          <el-button
            size="small"
            :icon="MagicStick"
            :loading="evaluatingMap[q.questionId]"
            @click="handleAiEvaluate(q)"
          >
            AI 辅助评分
          </el-button>

          <div v-if="aiScores[q.questionId]" class="ai-score-card">
            <el-tag type="info" size="small">AI 生成，仅供参考</el-tag>
            <span class="score">
              评分：<strong>{{ aiScores[q.questionId].rating }}</strong> / 5
            </span>
            <span class="confidence">
              置信度：{{ ((aiScores[q.questionId].confidence ?? 0) * 100).toFixed(0) }}%
            </span>
            <p class="reason">{{ aiScores[q.questionId].reason }}</p>
          </div>
        </div>
      </div>

      <!-- ===== 文本题/图片题：答案列表 + AI 辅助评分 ===== -->
      <div v-else-if="['TEXT', 'IMAGE'].includes(q.type)" class="text-answers-wrap">
        <div class="text-answers">
          <el-empty v-if="!q.textAnswers?.length" description="暂无回答" />
          <div v-for="(t, i) in q.textAnswers" :key="i" class="text-item">
            <span class="text-index">{{ i + 1 }}.</span>
            <span>{{ t }}</span>
          </div>
        </div>

        <div v-if="q.textAnswers?.length" class="ai-eval-section">
          <el-button
            size="small"
            :icon="MagicStick"
            :loading="evaluatingMap[q.questionId]"
            @click="handleAiEvaluate(q)"
          >
            AI 辅助评分
          </el-button>

          <div v-if="aiScores[q.questionId]" class="ai-score-card">
            <el-tag type="info" size="small">AI 生成，仅供参考</el-tag>
            <span class="score">
              评分：<strong>{{ aiScores[q.questionId].rating }}</strong> / 5
            </span>
            <span class="confidence">
              置信度：{{ ((aiScores[q.questionId].confidence ?? 0) * 100).toFixed(0) }}%
            </span>
            <p class="reason">{{ aiScores[q.questionId].reason }}</p>
          </div>
        </div>
      </div>

      <!-- ===== 兜底：未知题型 ===== -->
      <div v-else class="no-data">暂无数据</div>
    </el-card>

    <el-empty
      v-if="!loading && stats && stats.totalResponses === 0"
      description="还没有人提交答卷"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Download, MagicStick } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getSurvey } from '@/api/survey'
import { getStatistics, pageResponses } from '@/api/response'
import { evaluateResponse } from '@/api/ai'
import type { SurveyStatistics, QuestionStatistic } from '@/types/response'
import type { AiScoreResult } from '@/types/ai'

const route = useRoute()
const surveyId = Number(route.params.id)

const surveyTitle = ref('')
const stats = ref<SurveyStatistics | null>(null)
const loading = ref(false)
const chartRefs = new Map<number, HTMLElement>()

// AI 评分状态
const evaluatingMap = reactive<Record<number, boolean>>({})
const aiScores = reactive<Record<number, AiScoreResult>>({})

// AI 评分锚点：该问卷第一条答卷的 id
const currentResponseId = ref<number | null>(null)

// 评分题 = RATING + 已配置分值的 SINGLE/MULTI（averageScore 非 null）
const ratedQuestions = computed(
  () =>
    stats.value?.questions.filter(
      (q) =>
        (q.type === 'RATING' || q.type === 'SINGLE' || q.type === 'MULTI') &&
        q.averageScore !== null
    ) ?? []
)

const avgOfAverages = computed(() => {
  if (!ratedQuestions.value.length) return '-'
  const sum = ratedQuestions.value.reduce((s, q) => s + (q.averageScore ?? 0), 0)
  return (sum / ratedQuestions.value.length).toFixed(2)
})

function typeText(t: string) {
  return { TEXT: '文本', SINGLE: '单选', MULTI: '多选', RATING: '评分', IMAGE: '图片' }[t] ?? t
}

function setChartRef(id: number, el: any) {
  if (el) chartRefs.set(id, el as HTMLElement)
}

async function load() {
  loading.value = true
  try {
    const [detail, s] = await Promise.all([getSurvey(surveyId), getStatistics(surveyId)])
    surveyTitle.value = detail.title
    stats.value = s

    // 拿第一条答卷 id 作为 AI 评分锚点
    if (s.totalResponses > 0) {
      try {
        const page = await pageResponses(surveyId, 1, 1)
        currentResponseId.value = page.list[0]?.id ?? null
      } catch {
        currentResponseId.value = null
      }
    }

    await nextTick()
    renderCharts()
  } catch {
    // request 拦截器已提示
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  if (!stats.value) return
  stats.value.questions.forEach((q) => {
    const el = chartRefs.get(q.questionId)
    if (!el) return
    if (q.type === 'RATING' && q.scoreDistribution) {
      renderScoreChart(el, q.scoreDistribution)
    } else if (['SINGLE', 'MULTI'].includes(q.type) && q.optionDistribution) {
      renderOptionChart(el, q.optionDistribution)
    }
  })
}

function renderScoreChart(el: HTMLElement, dist: Record<string, number>) {
  const chart = echarts.init(el)
  const entries = Object.entries(dist).sort((a, b) => Number(a[0]) - Number(b[0]))
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: entries.map(([k]) => `${k} 分`) },
    yAxis: { type: 'value', name: '人数' },
    series: [
      {
        type: 'bar',
        data: entries.map(([, v]) => v),
        itemStyle: { color: '#409eff', borderRadius: [4, 4, 0, 0] },
        label: { show: true, position: 'top' },
      },
    ],
    grid: { left: 48, right: 24, top: 32, bottom: 32 },
  })
}

function renderOptionChart(el: HTMLElement, dist: Record<string, number>) {
  const chart = echarts.init(el)
  const entries = Object.entries(dist)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '65%'],
        data: entries.map(([name, value]) => ({ name, value })),
        label: { formatter: '{b}: {c} ({d}%)' },
      },
    ],
  })
}

/**
 * 触发 AI 辅助评分：
 * - TEXT / IMAGE：取第一条文字答案
 * - SINGLE / MULTI：把选项分布拼成文字描述
 */
async function handleAiEvaluate(q: QuestionStatistic) {
  if (!currentResponseId.value) {
    ElMessage.warning('还没有任何答卷可供 AI 评分')
    return
  }

  let answerText = ''

  if (q.type === 'TEXT' || q.type === 'IMAGE') {
    answerText = q.textAnswers?.[0] ?? ''
    if (!answerText) {
      ElMessage.warning('该题暂无文字答案可评分')
      return
    }
  } else if (q.type === 'SINGLE' || q.type === 'MULTI') {
    const dist = q.optionDistribution ?? {}
    const entries = Object.entries(dist)
    if (entries.length === 0) {
      ElMessage.warning('该题暂无答案分布数据')
      return
    }
    const total = entries.reduce((s, [, v]) => s + v, 0)
    const lines = entries.map(([label, count]) => {
      const pct = total > 0 ? ((count / total) * 100).toFixed(1) : '0'
      return `「${label}」${count} 人选择（${pct}%）`
    })
    answerText =
      `题型：${q.type === 'SINGLE' ? '单选题' : '多选题'}\n` +
      `总答卷数：${total}\n` +
      `选项分布：\n${lines.join('\n')}\n\n` +
      `请基于以上选择分布，分析答卷者的倾向性、意见集中度，并给出 1-5 的合理性评分。`
  } else {
    ElMessage.warning('该题型暂不支持 AI 辅助评分')
    return
  }

  evaluatingMap[q.questionId] = true
  try {
    const result = await evaluateResponse(currentResponseId.value, {
      questionId: q.questionId,
      questionTitle: q.title,
      answerText,
    })
    aiScores[q.questionId] = result
    ElMessage.success('AI 评分完成')
  } catch {
    // request 拦截器已提示
  } finally {
    evaluatingMap[q.questionId] = false
  }
}

function handleExport() {
  ElMessage.info('导出功能将在 S6 后端导出接口就绪后接入')
}

onMounted(load)
</script>

<style scoped>
.survey-result {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-inner .title {
  margin-left: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.stats-overview .metric {
  text-align: center;
  padding: 12px 0;
}

.stats-overview .num {
  font-size: 28px;
  font-weight: 700;
  color: #409eff;
}

.stats-overview .label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.q-card .q-header {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 16px;
  font-size: 15px;
  color: #303133;
}

.q-card .index {
  font-weight: 600;
  color: #409eff;
}

.q-card .q-title {
  flex: 1;
}

.q-chart .avg {
  font-size: 14px;
  color: #606266;
  margin-bottom: 12px;
}

.q-chart .avg strong {
  font-size: 20px;
  color: #409eff;
}

.q-chart .hint {
  color: #909399;
  font-size: 12px;
  margin-left: 6px;
}

.q-chart .chart {
  width: 100%;
  height: 260px;
}

.text-answers-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.text-answers {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.text-item {
  display: flex;
  gap: 8px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.6;
}

.text-index {
  color: #909399;
  flex-shrink: 0;
}

.no-data {
  color: #909399;
  font-size: 13px;
  text-align: center;
  padding: 24px;
}

.ai-eval-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
}

.ai-score-card {
  margin-top: 8px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 13px;
}

.ai-score-card .score {
  margin: 0 12px;
}

.ai-score-card .confidence {
  color: #909399;
}

.ai-score-card .reason {
  margin-top: 6px;
  color: #606266;
  line-height: 1.6;
}
</style>