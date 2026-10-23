<template>
  <div class="question-editor">
    <div class="row">
      <span class="label">题型</span>
      <el-tag>{{ typeText(question.type) }}</el-tag>
    </div>

    <!-- 题干：富文本（评分题保持纯文本） -->
    <div class="row">
      <span class="label">题干</span>
      <div class="editor-wrap">
        <RichEditor
          v-if="question.type !== 'RATING'"
          :model-value="question.contentHtml || question.title"
          :biz-id="null"
          @update:model-value="handleRichChange"
        />
        <el-input
          v-else
          :model-value="question.title"
          type="textarea"
          :rows="2"
          placeholder="请输入题干"
          :disabled="readonly"
          @update:model-value="(v: string) => update({ title: v, contentHtml: null })"
        />
      </div>
    </div>

    <!-- 图片题：题目图片上传 -->
    <div v-if="question.type === 'IMAGE'" class="row">
      <span class="label">题目图片</span>
      <div class="image-upload">
        <div v-if="question.imageKey" class="preview">
          <img v-if="previewUrl" :src="previewUrl" alt="题目图片" />
          <div v-else class="preview-loading">加载中...</div>
          <el-button
            v-if="!readonly"
            size="small"
            type="danger"
            link
            @click="removeImage"
          >删除</el-button>
        </div>
        <el-upload
          v-else
          :show-file-list="false"
          :before-upload="handleImageUpload"
          :disabled="readonly"
          accept="image/png,image/jpeg,image/gif,image/webp"
        >
          <el-button :icon="Upload" :disabled="readonly">上传题目图片</el-button>
        </el-upload>
      </div>
    </div>

    <!-- 单选 / 多选：选项编辑 -->
    <template v-if="['SINGLE', 'MULTI'].includes(question.type)">
      <div class="row">
        <span class="label">选项</span>
        <div class="options">
          <div v-for="(opt, idx) in question.options" :key="opt.uid" class="option-row">
            <el-radio v-if="question.type === 'SINGLE'" disabled :label="idx + 1" />
            <el-checkbox v-else disabled />
            <el-input
              :model-value="opt.label"
              placeholder="选项文案"
              :disabled="readonly"
              @update:model-value="(v: string) => updateOption(opt.uid, { label: v })"
            />
            <el-input
              :model-value="opt.value"
              placeholder="值"
              class="value-input"
              :disabled="readonly"
              @update:model-value="(v: string) => updateOption(opt.uid, { value: v })"
            />
 
            <el-button
              v-if="!readonly"
              :icon="Delete"
              link
              size="small"
              @click="store.removeOption(question.uid, opt.uid)"
            />
          </div>
          <el-button v-if="!readonly" size="small" :icon="Plus" @click="store.addOption(question.uid)">
            添加选项
          </el-button>
        </div>
      </div>
    </template>

    <!-- 评分开关（图片题也参与评分，比如"这张图质量如何"） -->
    <div class="row">
      <span class="label">参与评分</span>
      <el-switch
        :model-value="question.scoreEnabled"
        :disabled="readonly"
        @update:model-value="(v: boolean) => update({ scoreEnabled: v })"
      />
      <template v-if="question.scoreEnabled">
        <span style="margin-left:12px">分值范围</span>
        <el-input-number
          :model-value="question.scoreMin"
          :min="0" :max="100"
          :disabled="readonly"
          size="small"
          @update:model-value="(v: number | undefined) => update({ scoreMin: v ?? 1 })"
        />
        <span style="margin:0 6px">~</span>
        <el-input-number
          :model-value="question.scoreMax"
          :min="1" :max="100"
          :disabled="readonly"
          size="small"
          @update:model-value="(v: number | undefined) => update({ scoreMax: v ?? 5 })"
        />
      </template>
    </div>

    <div class="row">
      <span class="label">必填</span>
      <el-switch
        :model-value="question.required"
        :disabled="readonly"
        @update:model-value="(v: boolean) => update({ required: v })"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Plus, Upload } from '@element-plus/icons-vue'
import { useSurveyDraftStore, type QuestionDraft } from '@/stores/surveyDraft'
import { uploadImage } from '@/utils/upload'
import { getFileUrl } from '@/api/file'
import RichEditor from './RichEditor.vue'

const props = defineProps<{ question: QuestionDraft; readonly: boolean }>()
const store = useSurveyDraftStore()

const previewUrl = ref('')

function typeText(t: string) {
  return { TEXT: '文本题', SINGLE: '单选题', MULTI: '多选题', RATING: '评分题', IMAGE: '图片题' }[t] ?? t
}

function update(patch: Partial<QuestionDraft>) {
  store.updateQuestion(props.question.uid, patch)
}

function handleRichChange(html: string) {
  // 同时更新 title（纯文本摘要，列表用）和 contentHtml（富文本）
  const plainText = html.replace(/<[^>]+>/g, '').trim()
  update({ contentHtml: html, title: plainText || '（未填写题干）' })
}

function updateOption(optionUid: string, patch: { label?: string; value?: string }) {
  const opt = props.question.options.find((o) => o.uid === optionUid)
  if (!opt) return
  Object.assign(opt, patch)
  store.dirty = true
}

async function handleImageUpload(file: File) {
  try {
    // 注意：QuestionDraft 没有 id（前端临时结构），bizId 传 null
    const r = await uploadImage(file, { bizType: 'QUESTION', bizId: null })
    update({ imageKey: r.objectKey })
    // 换取预览 URL
    const { url } = await getFileUrl(r.objectKey)
    previewUrl.value = url
    ElMessage.success('上传成功')
  } catch (e: any) {
    ElMessage.error(e.message || '上传失败')
  }
  return false  // 阻止 el-upload 默认行为
}

function removeImage() {
  update({ imageKey: null })
  previewUrl.value = ''
}

// 回显：切换题目或初始化时，如果有 imageKey 就换 URL 展示
watch(
  () => props.question.imageKey,
  async (key) => {
    if (key) {
      try {
        const { url } = await getFileUrl(key)
        previewUrl.value = url
      } catch {
        previewUrl.value = ''
      }
    } else {
      previewUrl.value = ''
    }
  },
  { immediate: true }
)

</script>

<style scoped>
.question-editor { display: flex; flex-direction: column; gap: 20px; }

.row { display: flex; align-items: flex-start; gap: 12px; }

.row .label {
  width: 72px; flex-shrink: 0; color: #606266;
  font-size: 14px; padding-top: 6px;
}

.editor-wrap { flex: 1; }

.options { flex: 1; display: flex; flex-direction: column; gap: 8px; }

.option-row { display: flex; align-items: center; gap: 6px; }

.value-input { width: 120px; flex-shrink: 0; }

.image-block {
  flex: 1; display: flex; flex-direction: column; gap: 8px;
}

.preview {
  display: flex; align-items: center; gap: 12px;
  padding: 8px; background: #f5f7fa; border-radius: 6px;
}

.preview-img {
  max-width: 200px; max-height: 120px;
  border-radius: 4px; object-fit: contain;
  background: #fff;
}

.preview-actions { display: flex; flex-direction: column; gap: 4px; }

.uploading {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; color: #909399;
}

.editor-wrap {
  flex: 1;
}
.image-upload {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.preview {
  display: flex;
  align-items: center;
  gap: 12px;
}
.preview img {
  max-width: 240px;
  max-height: 160px;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}
.preview-loading {
  color: #909399;
  font-size: 13px;
}
</style>