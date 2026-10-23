<template>
  <div class="survey-edit">
    <el-card shadow="never" class="toolbar">
      <div class="toolbar-inner">
        <div class="left">
          <el-button :icon="ArrowLeft" link @click="goBack">返回列表</el-button>
          <!-- 改动：只在可编辑状态显示 AI 生成 -->
          <el-button
            v-if="!readonly"
            :icon="MagicStick"
            @click="openAiDialog"
          >
            AI 生成
          </el-button>
          <span class="divider">|</span>
          <el-input
            v-model="store.title"
            placeholder="问卷标题"
            class="title-input"
            :disabled="readonly"
            @input="store.dirty = true"
          />
          <el-tag v-if="store.dirty" type="warning" size="small">未保存</el-tag>
          <el-tag v-else type="success" size="small">已保存</el-tag>
        </div>
        <div class="right">
          <el-button :icon="View" :disabled="store.dirty" @click="goPreview">预览</el-button>
          <el-button
            v-if="!readonly"
            :icon="DocumentChecked"
            :loading="store.saving"
            @click="handleSave"
          >
            保存草稿
          </el-button>
          <el-button
            v-if="!readonly"
            type="primary"
            :icon="Upload"
            :disabled="store.questions.length === 0"
            @click="handlePublish"
          >
            发布
          </el-button>
          <el-button
            v-if="readonly"
            type="primary"
            :icon="CopyDocument"
            @click="handleCopyAsDraft"
          >
            复制为新草稿
          </el-button>
        </div>
      </div>
      <div v-if="readonly" class="readonly-banner">
        <el-alert
          type="warning"
          :closable="false"
          show-icon
          :title="`当前问卷状态为「${statusText(store.status)}」，不可直接编辑。如需修改请复制为新草稿。`"
        />
      </div>
      <div class="desc-row">
        <el-input
          v-model="store.description"
          type="textarea"
          :rows="1"
          placeholder="问卷描述（可选）"
          :disabled="readonly"
          @input="store.dirty = true"
        />
      </div>
    </el-card>

    <el-dialog v-model="aiDialogVisible" title="AI 生成题目" width="480px">
      <el-form label-width="80px">
        <el-form-item label="问卷主题">
          <el-input v-model="aiForm.topic" placeholder="例如：产品满意度调研" />
        </el-form-item>
        <el-form-item label="题目数量">
          <el-input-number v-model="aiForm.count" :min="1" :max="10" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="aiLoading" @click="handleAiGenerate">
          生成草案
        </el-button>
      </template>
    </el-dialog>

    <div class="body">
      <el-card shadow="never" class="question-list-card">
        <div class="list-header">
          <span>题目列表（{{ store.questions.length }}）</span>
        </div>

        <VueDraggable
          v-model="store.questions"
          :animation="150"
          handle=".drag-handle"
          :disabled="readonly"
          class="question-list"
          @end="store.dirty = true"
        >
          <div
            v-for="(q, idx) in store.questions"
            :key="q.uid"
            class="question-item"
            :class="{ active: q.uid === store.selectedUid }"
            @click="store.selectedUid = q.uid"
          >
            <span class="drag-handle" v-if="!readonly">⋮⋮</span>
            <span class="index">{{ idx + 1 }}</span>
            <span class="type-tag">{{ typeText(q.type) }}</span>
            <span class="qtitle">{{ q.title || '（未填写题干）' }}</span>
            <span v-if="q.required" class="required">*</span>
            <el-button
              v-if="!readonly"
              :icon="Delete"
              link
              size="small"
              class="del"
              @click.stop="handleRemove(idx, q.uid)"
            />
          </div>
        </VueDraggable>

        <div v-if="!readonly" class="add-actions">
          <el-button :icon="Plus" size="small" @click="store.addQuestion('TEXT')">文本题</el-button>
          <el-button :icon="Plus" size="small" @click="store.addQuestion('SINGLE')">单选题</el-button>
          <el-button :icon="Plus" size="small" @click="store.addQuestion('MULTI')">多选题</el-button>
          <el-button :icon="Plus" size="small" @click="store.addQuestion('RATING')">评分题</el-button>
          <el-button :icon="Plus" size="small" @click="store.addQuestion('IMAGE')">图片题</el-button>
        </div>
      </el-card>

      <el-card shadow="never" class="question-edit-card">
        <QuestionEditor
          v-if="selectedQuestion"
          :question="selectedQuestion"
          :readonly="readonly"
        />
        <el-empty v-else description="从左侧选择或添加一道题目" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, h } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  View,
  DocumentChecked,
  Upload,
  CopyDocument,
  Delete,
  Plus,
  MagicStick,
} from '@element-plus/icons-vue'
import { VueDraggable } from 'vue-draggable-plus'
import { useSurveyDraftStore, type QuestionDraft } from '@/stores/surveyDraft'
import {
  getSurvey,
  getDraft,
  saveDraft,
  publishSurvey,
  copySurvey,
} from '@/api/survey'
import QuestionEditor from '@/components/editor/QuestionEditor.vue'
import { generateSurvey } from '@/api/ai'
import type { QuestionType } from '@/types/survey'

const route = useRoute()
const router = useRouter()
const store = useSurveyDraftStore()

// ===== AI 生成相关状态 =====
const aiDialogVisible = ref(false)
const aiLoading = ref(false)
const aiForm = reactive<{ topic: string; count: number }>({ topic: '', count: 5 })

const VALID_QUESTION_TYPES: QuestionType[] = ['TEXT', 'SINGLE', 'MULTI', 'RATING', 'IMAGE']

// ===== 计算属性 =====
const surveyId = computed(() => Number(route.params.id))
const readonly = computed(() => store.status !== 'DRAFT')
const selectedQuestion = computed(() =>
  store.questions.find((q) => q.uid === store.selectedUid) ?? null
)

// ===== 工具函数 =====
function statusText(s: string) {
  return { DRAFT: '草稿', PUBLISHED: '已发布', CLOSED: '已关闭' }[s] ?? s
}

function typeText(t: string) {
  return { TEXT: '文本', SINGLE: '单选', MULTI: '多选', RATING: '评分', IMAGE: '图片' }[t] ?? t
}

// ===== 生命周期 =====
async function load() {
  store.reset()
  const [detail, draft] = await Promise.all([
    getSurvey(surveyId.value),
    getDraft(surveyId.value),
  ])
  store.loadFromServer(surveyId.value, {
    title: detail.title,
    description: detail.description,
    status: detail.status,
  }, draft)
}

onMounted(load)

// ===== 导航 =====
function goBack() {
  router.push('/admin/surveys')
}

function goPreview() {
  if (store.dirty) {
    ElMessage.warning('请先保存草稿再预览')
    return
  }
  router.push(`/admin/surveys/${surveyId.value}/preview`)
}

// ===== 题目操作 =====
async function handleRemove(idx: number, uid: string) {
  try {
    await ElMessageBox.confirm(`确定删除第 ${idx + 1} 题？`, '提示', { type: 'warning' })
    store.removeQuestion(uid)
  } catch { /* 取消 */ }
}

// ===== 保存 / 发布 / 复制 =====
async function handleSave() {
  if (!store.title.trim()) {
    ElMessage.warning('问卷标题不能为空')
    return
  }
  store.saving = true
  try {
    await saveDraft(surveyId.value, store.toSaveRequest())
    store.markSaved()
    ElMessage.success('已保存')
  } finally {
    store.saving = false
  }
}

async function handlePublish() {
  for (let i = 0; i < store.questions.length; i++) {
    const q = store.questions[i]
    if (!q.title.trim()) {
      ElMessage.warning(`第 ${i + 1} 题题干不能为空`)
      return
    }
    if (['SINGLE', 'MULTI'].includes(q.type) && q.options.length < 2) {
      ElMessage.warning(`第 ${i + 1} 题至少需要 2 个选项`)
      return
    }
  }
  try {
    await ElMessageBox.confirm('发布后问卷将不可直接编辑，确定发布？', '发布确认', {
      type: 'warning',
    })
  } catch { return }

  if (store.dirty) {
    await saveDraft(surveyId.value, store.toSaveRequest())
    store.markSaved()
  }

  const result = await publishSurvey(surveyId.value)
  const fullUrl = `${window.location.origin}${result.shareUrl}`
  store.status = 'PUBLISHED'

  ElMessageBox({
    title: '发布成功',
    showCancelButton: false,
    confirmButtonText: '关闭',
    message: h('div', { style: 'line-height: 1.8' }, [
      h('p', null, '分享链接：'),
      h(
        'div',
        {
          style:
            'margin: 8px 0; padding: 8px; background: #f5f7fa; border-radius: 4px; ' +
            'word-break: break-all; font-size: 13px;',
        },
        fullUrl
      ),
      h(
        'button',
        {
          style:
            'margin-top: 8px; padding: 5px 14px; background: #409eff; color: #fff; ' +
            'border: none; border-radius: 4px; cursor: pointer; font-size: 13px;',
          onClick: () => copyText(fullUrl),
        },
        '复制链接'
      ),
    ]),
  })
}

async function handleCopyAsDraft() {
  try {
    await ElMessageBox.confirm('将基于已发布版本复制一份新草稿，确定？', '提示', {
      type: 'info',
    })
    const newSurvey = await copySurvey(surveyId.value)
    ElMessage.success('已复制为新草稿')
    router.push(`/admin/surveys/${newSurvey.id}/edit`)
  } catch { /* 取消 */ }
}

// ===== AI 生成 =====
function openAiDialog() {
  aiForm.topic = ''
  aiForm.count = 5
  aiDialogVisible.value = true
}

async function handleAiGenerate() {
  if (!aiForm.topic.trim()) {
    ElMessage.warning('请输入问卷主题')
    return
  }
  const count = aiForm.count ?? 5

  aiLoading.value = true
  try {
    const draft = await generateSurvey({ topic: aiForm.topic.trim(), count })
    if (!draft?.questions?.length) {
      ElMessage.warning('AI 未返回题目，请调整主题后重试')
      return
    }

    let added = 0
    for (const q of draft.questions) {
      // 校验题型，非法时降级为 TEXT
      const type: QuestionType = VALID_QUESTION_TYPES.includes(q.type as QuestionType)
        ? (q.type as QuestionType)
        : 'TEXT'

      store.addQuestion(type)
      const created = store.questions[store.questions.length - 1]

      const patch: Partial<QuestionDraft> = {
        title: q.title || '（AI 未生成题干）',
        contentHtml: `<p>${escapeHtml(q.title || '')}</p>`,
      }

      // 仅当 AI 确实返回选项时覆盖，避免清空单选/多选的默认选项
      if (q.options && q.options.length > 0) {
        patch.options = q.options.map((label: string, i: number) => ({
          uid: `ai_${Date.now()}_${i}_${Math.random().toString(36).slice(2, 8)}`,
          label,
          value: `option${i + 1}`,
        }))
      }

      store.updateQuestion(created.uid, patch)
      added++
    }

    ElMessage.success(`已生成 ${added} 道题目草案，请确认后保存`)
    aiDialogVisible.value = false
  } catch {
    // request 拦截器已提示具体错误
  } finally {
    aiLoading.value = false
  }
}

// 简单的 HTML 转义，避免 AI 返回的内容破坏富文本结构
function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

// ===== 复制工具 =====
function copyText(text: string) {
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text).then(
      () => ElMessage.success('链接已复制'),
      () => fallbackCopy(text)
    )
  } else {
    fallbackCopy(text)
  }
}

function fallbackCopy(text: string) {
  const ta = document.createElement('textarea')
  ta.value = text
  ta.style.position = 'fixed'
  ta.style.opacity = '0'
  document.body.appendChild(ta)
  ta.select()
  try {
    document.execCommand('copy')
    ElMessage.success('链接已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择文本复制')
  } finally {
    document.body.removeChild(ta)
  }
}

// ===== 离开提醒 =====
onBeforeRouteLeave(async () => {
  if (!store.dirty) return true
  try {
    await ElMessageBox.confirm('有未保存的修改，确定离开？', '提示', {
      type: 'warning',
      confirmButtonText: '离开',
      cancelButtonText: '留下',
    })
    return true
  } catch {
    return false
  }
})
</script>

<style scoped>
.survey-edit { display: flex; flex-direction: column; gap: 16px; }
.toolbar-inner { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.toolbar-inner .left { display: flex; align-items: center; gap: 8px; flex: 1; }
.toolbar-inner .right { display: flex; gap: 8px; }
.divider { color: #dcdfe6; }
.title-input { width: 320px; }
.desc-row { margin-top: 12px; }
.readonly-banner { margin-top: 12px; }
.body { display: grid; grid-template-columns: 320px 1fr; gap: 16px; }
.question-list-card { height: calc(100vh - 280px); overflow-y: auto; }
.list-header { font-size: 14px; color: #606266; margin-bottom: 12px; }
.question-list { display: flex; flex-direction: column; gap: 6px; }
.question-item {
  display: flex; align-items: center; gap: 6px; padding: 8px;
  border: 1px solid #ebeef5; border-radius: 6px; cursor: pointer;
  background: #fff; transition: all 0.15s;
}
.question-item:hover { border-color: #c6e2ff; }
.question-item.active { border-color: #409eff; background: #ecf5ff; }
.drag-handle { cursor: grab; color: #909399; }
.index { font-weight: 600; color: #409eff; min-width: 20px; }
.type-tag {
  font-size: 11px; padding: 1px 6px; background: #f0f2f5;
  border-radius: 3px; color: #606266;
}
.qtitle {
  flex: 1; font-size: 13px; white-space: nowrap;
  overflow: hidden; text-overflow: ellipsis;
}
.required { color: #f56c6c; }
.del { color: #f56c6c; }
.add-actions { margin-top: 12px; display: flex; flex-wrap: wrap; gap: 6px; }
.question-edit-card { height: calc(100vh - 280px); overflow-y: auto; }
</style>