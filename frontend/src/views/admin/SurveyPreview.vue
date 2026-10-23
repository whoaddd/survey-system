<template>
  <div class="survey-preview">
    <!-- 顶部提示条 -->
    <div class="preview-banner">
      <el-tag type="warning" effect="dark">预览模式</el-tag>
      <span>这是答卷者看到的真实效果，所有输入控件为只读</span>
      <el-button :icon="ArrowLeft" link @click="goBack">返回编辑器</el-button>
    </div>

    <div v-if="loading" class="loading">
      <el-skeleton :rows="8" animated />
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
          class="q-item"
        >
          <div class="q-title">
            <span class="index">{{ idx + 1 }}.</span>
            <div class="q-title-content">
              <span v-if="q.contentHtml" v-html="q.contentHtml"></span>
              <span v-else>{{ q.title }}</span>
            </div>
            <span v-if="q.required" class="required">*</span>
          </div>

          <!-- 复用答卷页的渲染组件，只读模式 -->
          <div class="q-body">
            <QuestionRenderer
              :question="q"
              :model-value="null"
              readonly
            />
          </div>
        </div>
      </div>

      <div class="footer">
        <el-button type="primary" size="large" disabled class="submit-btn">
          提交答卷（预览模式不可提交）
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getPublicSurvey } from '@/api/response'
import { getSurvey } from '@/api/survey'
import type { PublicSurvey } from '@/types/response'
import QuestionRenderer from '@/components/answer/QuestionRenderer.vue'
import { getDraft } from '@/api/survey'

const route = useRoute()
const router = useRouter()
const surveyId = Number(route.params.id)

const loading = ref(true)
const survey = ref<PublicSurvey | null>(null)

async function load() {
  loading.value = true
  try {
    // 预览页要看到"发布时冻结的版本"，所以走发布端详情拿 shareCode
    const detail = await getSurvey(surveyId)
    if (!detail.shareCode) {
      // 还没发布过的草稿无法预览，展示草稿数据
      const draft = await getDraft(surveyId)
      // 构造临时 survey 结构，按草稿渲染
      survey.value = {
        shareCode: '',
        title: detail.title,
        description: detail.description,
        version: detail.version,
        questions: draft.map((q, i) => ({
          id: q.id,
          type: q.type,
          title: q.title,
          contentHtml: q.contentHtml,
          imageKey: q.imageKey,
          required: q.required,
          scoreEnabled: q.scoreEnabled,
          scoreMin: q.scoreMin,
          scoreMax: q.scoreMax,
          sortOrder: i + 1,
          options: q.options,
        })),
      }
    } else {
      // 已发布：走公开接口，与答卷者看到的一致
      survey.value = await getPublicSurvey(detail.shareCode)
    }
  } catch {
    // request 拦截器已提示
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push(`/admin/surveys/${surveyId}/edit`)
}

onMounted(load)
</script>

<style scoped>
/* ⚠️ 完全复用 AnswerPage 的样式，确保视觉一致 */
.survey-preview {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 16px;
}

.preview-banner {
  max-width: 720px;
  margin: 0 auto 16px;
  padding: 12px 16px;
  background: #fdf6ec;
  border: 1px solid #faecd8;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: #e6a23c;
}

.preview-banner .el-button {
  margin-left: auto;
}

.loading, .content {
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

.q-title-content {
  flex: 1;
  line-height: 1.6;
}

.q-title-content :deep(img) {
  max-width: 100%;
  border-radius: 6px;
  margin: 8px 0;
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

@media (max-width: 640px) {
  .survey-preview {
    padding: 0;
  }
  .content {
    border-radius: 0;
    margin-top: 0;
    padding: 20px 16px 24px;
  }
  .preview-banner {
    margin: 0;
    border-radius: 0;
    padding: 10px 16px;
  }
}
</style>