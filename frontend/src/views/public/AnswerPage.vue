<template>
  <div class="answer-page">
    <div v-if="loading" class="loading">
      <el-skeleton :rows="8" animated />
    </div>

    <div v-else-if="error" class="error-box">
      <el-result
        :icon="errorIcon"
        :title="error.title"
        :sub-title="error.message"
      >
        <template #extra>
          <el-button v-if="error.retry" type="primary" @click="load">重试</el-button>
        </template>
      </el-result>
    </div>

    <div v-else-if="survey" class="content">
      <div class="header">
        <h1>{{ survey.title }}</h1>
        <p v-if="survey.description" class="desc">{{ survey.description }}</p>
      </div>

      <div class="questions">
  <div
    v-for="(q, idx) in survey.questions"
    :key="q.id"
    :ref="(el) => setQuestionRef(q.id, el)"
    class="q-item"
    :class="{ 'q-error': errorQuestionId === q.id }"
  >
    <div class="q-title">
      <span class="index">{{ idx + 1 }}.</span>
      <div class="q-title-content">
        <span v-if="q.contentHtml" v-html="q.contentHtml"></span>
        <span v-else>{{ q.title }}</span>
      </div>
      <span v-if="q.required" class="required">*</span>
    </div>

    <!-- ✅ 新增：作答区 -->
    <div class="q-body">
      <QuestionRenderer
        :question="q"
        :model-value="answers[q.id]"
        @update:model-value="(v) => updateAnswer(q.id, v)"
      />
    </div>
  </div>
</div>

      <div class="footer">
        <el-button
          type="primary"
          size="large"
          :loading="submitting"
          class="submit-btn"
          @click="handleSubmit"
        >
          提交答卷
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPublicSurvey, submitResponse } from '@/api/response'
import { getRespondentKey, hasSubmitted, markSubmitted } from '@/utils/respondent'
import type { PublicSurvey, AnswerItem } from '@/types/response'
import { getFileUrl } from '@/api/file'
import QuestionRenderer from '@/components/answer/QuestionRenderer.vue'


const route = useRoute()
const router = useRouter()
const shareCode = route.params.shareCode as string

const loading = ref(true)
const submitting = ref(false)
const survey = ref<PublicSurvey | null>(null)
const error = ref<{ icon: string; title: string; message: string; retry?: boolean } | null>(null)
const answers = reactive<Record<number, any>>({})
const errorQuestionId = ref<number | null>(null)
const questionRefs = new Map<number, HTMLElement>()

type ErrorIcon = 'success' | 'warning' | 'info' | 'error'
const errorIcon = ref<ErrorIcon>('error')

function setQuestionRef(id: number, el: any) {
  if (el) questionRefs.set(id, el as HTMLElement)
}

async function load() {
  loading.value = true
  error.value = null
  try {
    if (hasSubmitted(shareCode)) {
      router.replace(`/s/${shareCode}/done?repeat=1`)
      return
    }
    const data = await getPublicSurvey(shareCode)

    // 关键：刷新所有题干里的图片 URL
    await Promise.all(
      data.questions.map(async (q) => {
        if (q.contentHtml) {
          q.contentHtml = await refreshHtmlImages(q.contentHtml)
        }
      })
    )

    survey.value = data
  } catch (e: any) {
    const status = e?.response?.status
    if (status === 404) {
      errorIcon.value = 'warning'
      error.value = { icon: 'warning', title: '问卷不存在或已关闭', message: '请联系发布者确认链接' }
    } else if (status === 429) {
      errorIcon.value = 'warning'
      error.value = { icon: 'warning', title: '请求过于频繁', message: '请稍后再试' }
    } else {
      errorIcon.value = 'error'
      error.value = { icon: 'error', title: '加载失败', message: '网络异常，请重试', retry: true }
    }
  } finally {
    loading.value = false
  }
}

function updateAnswer(questionId: number, value: any) {
  answers[questionId] = value
  if (errorQuestionId.value === questionId) errorQuestionId.value = null
}

function buildAnswers(): AnswerItem[] | null {
  if (!survey.value) return null
  const list: AnswerItem[] = []

  for (let i = 0; i < survey.value.questions.length; i++) {
    const q = survey.value.questions[i]
    const v = answers[q.id]

    // 必填校验
    if (q.required && isEmpty(v)) {
      errorQuestionId.value = q.id
      ElMessage.warning(`第 ${i + 1} 题为必填`)
      questionRefs.get(q.id)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
      return null
    }

    if (isEmpty(v)) continue

    const item: AnswerItem = { questionId: q.id }
    if (q.type === 'TEXT') item.answerText = v
    else if (q.type === 'SINGLE') item.selectedOptionIds = [v]
    else if (q.type === 'MULTI') item.selectedOptionIds = v
    else if (q.type === 'RATING') item.ratingValue = v
    else if (q.type === 'IMAGE') item.answerText = v

    list.push(item)
  }
  return list
}

function isEmpty(v: any): boolean {
  if (v === undefined || v === null || v === '') return true
  if (Array.isArray(v) && v.length === 0) return true
  return false
}

async function refreshHtmlImages(html: string): Promise<string> {
  // 提取所有 alt="__objectKey__xxx" 标记
  const regex = /alt="__objectKey__([^"]+)"/g
  const matches = [...html.matchAll(regex)]
  const keys = matches.map((m) => m[1])
  if (keys.length === 0) return html

  // 批量换取新 URL
  const urlMap: Record<string, string> = {}
  await Promise.all(
    keys.map(async (key) => {
      try {
        const { url } = await getFileUrl(key)
        urlMap[key] = url
      } catch { /* 单个失败不影响其他 */ }
    })
  )

  // 替换 src（两个方向都覆盖）
  let result = html
  for (const [key, url] of Object.entries(urlMap)) {
    // src 在 alt 前面
    result = result.replace(
      new RegExp(`(<img[^>]*?)src="[^"]*"([^>]*?alt="__objectKey__${key}")`, 'g'),
      `$1src="${url}"$2`
    )
    // alt 在 src 前面
    result = result.replace(
      new RegExp(`(<img[^>]*?)alt="__objectKey__${key}"([^>]*?)src="[^"]*"`, 'g'),
      `$1alt="__objectKey__${key}"$2src="${url}"`
    )
  }
  return result
}

async function handleSubmit() {
  const list = buildAnswers()
  if (!list || !survey.value) return

  submitting.value = true
  try {
    const result = await submitResponse({
      shareCode,
      respondentKey: getRespondentKey(shareCode),
      answers: list,
    })
    markSubmitted(shareCode)
    router.push({
      path: `/s/${shareCode}/done`,
      query: { receipt: result.receiptCode },
    })
  } catch (e: any) {
    const code = e?.response?.data?.code
    if (code === 40901) {
      ElMessage.warning('你已提交过该问卷')
      markSubmitted(shareCode)
      router.push(`/s/${shareCode}/done?repeat=1`)
    } else if (code === 42901) {
      ElMessage.warning('提交过于频繁，请稍后再试')
    }
    // 其余错误拦截器已提示
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.answer-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 16px;
}

.loading, .error-box, .content {
  max-width: 720px;
  margin: 0 auto;
}

.content {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-top: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.header h1 {
  font-size: 20px;
  margin-bottom: 8px;
  color: #303133;
}

.header .desc {
  color: #909399;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 24px;
}

.q-item {
  padding: 20px 0;
  border-bottom: 1px solid #ebeef5;
  transition: background 0.3s;
  border-radius: 6px;
}

.q-item.q-error {
  background: #fef0f0;
  padding-left: 12px;
  padding-right: 12px;
}

.q-title {
  display: flex;
  gap: 6px;
  font-size: 15px;
  color: #303133;
  margin-bottom: 12px;
  line-height: 1.5;
}

.q-title .index {
  font-weight: 600;
  color: #409eff;
  flex-shrink: 0;
}

.q-title .required {
  color: #f56c6c;
}

.q-body {
  padding-left: 20px;
}

.footer {
  margin-top: 32px;
  text-align: center;
}

.submit-btn {
  min-width: 200px;
}

.q-title-content {
  flex: 1;
  line-height: 1.6;
}
.q-title-content :deep(img) {
  max-width: 100%;
  border-radius: 6px;
  margin: 8px 0;
}

/* 移动端优化 */
@media (max-width: 640px) {
  .answer-page {
    padding: 0;
  }
  .content {
    border-radius: 0;
    margin-top: 0;
    padding: 20px 16px 100px;
  }
  .footer {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 12px 16px;
    background: #fff;
    box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.06);
    margin-top: 0;
    z-index: 10;
  }
  .submit-btn {
    width: 100%;
  }
}
</style>