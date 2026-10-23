<template>
  <div class="question-renderer">
    <el-input
      v-if="question.type === 'TEXT'"
      :model-value="(modelValue as string) || ''"
      type="textarea"
      :rows="3"
      :disabled="readonly"
      maxlength="1000"
      show-word-limit
      placeholder="请输入你的回答"
      @update:model-value="(v: string) => emit('update:modelValue', v)"
    />

    <el-radio-group
      v-else-if="question.type === 'SINGLE'"
      :model-value="(modelValue as number) ?? null"
      class="options-vertical"
      :disabled="readonly"
      @update:model-value="(v: any) => emit('update:modelValue', v)"
    >
      <el-radio
        v-for="opt in question.options"
        :key="opt.id"
        :label="opt.id"
        size="large"
      >
        {{ opt.label }}
      </el-radio>
    </el-radio-group>

    <el-checkbox-group
      v-else-if="question.type === 'MULTI'"
      :model-value="(modelValue as number[]) || []"
      class="options-vertical"
      :disabled="readonly"
      @update:model-value="(v: any) => emit('update:modelValue', v)"
    >
      <el-checkbox
        v-for="opt in question.options"
        :key="opt.id"
        :label="opt.id"
        size="large"
      >
        {{ opt.label }}
      </el-checkbox>
    </el-checkbox-group>

    <div v-else-if="question.type === 'RATING'" class="rating-box">
      <el-rate
        :model-value="(modelValue as number) || 0"
        :max="question.scoreMax"
        :low-threshold="Math.ceil(question.scoreMax * 0.4)"
        :high-threshold="Math.ceil(question.scoreMax * 0.8)"
        :colors="['#F56C6C', '#E6A23C', '#67C23A']"
        size="large"
        show-score
        :score-template="`{value} / ${question.scoreMax}`"
        :disabled="readonly"
        @update:model-value="(v: number) => emit('update:modelValue', v)"
      />
      <div class="rating-hint">
        分值范围 {{ question.scoreMin }} – {{ question.scoreMax }}
      </div>
    </div>

    <div v-else-if="question.type === 'IMAGE'" class="image-question">
      <div v-if="question.imageKey" class="question-image">
        <img v-if="questionImageUrl" :src="questionImageUrl" alt="题目图片" />
        <div v-else class="image-loading">图片加载中...</div>
      </div>

      <div class="answer-area">
        <el-input
          :model-value="(modelValue as string) || ''"
          type="textarea"
          :rows="4"
          maxlength="1000"
          show-word-limit
          placeholder="请根据图片内容作答"
          @update:model-value="(v: string) => emit('update:modelValue', v)"
        />
      </div>
    </div>

    <el-alert
      v-else
      type="warning"
      :closable="false"
      :title="`未知题型：${question.type}`"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { getFileUrl } from '@/api/file'
import type { PublicQuestion } from '@/types/response'

const props = defineProps<{
  question: PublicQuestion
  modelValue: string | number | number[] | null | undefined
  readonly?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number | number[]): void
}>()

const questionImageUrl = ref('')

watch(
  () => props.question.imageKey,
  async (key) => {
    if (props.question.type === 'IMAGE' && key) {
      try {
        const { url } = await getFileUrl(key)
        questionImageUrl.value = url
      } catch (e) {
        console.error('[QuestionRenderer] 题目图片加载失败:', key, e)
        questionImageUrl.value = ''
      }
    } else {
      questionImageUrl.value = ''
    }
  },
  { immediate: true }
)



</script>

<style scoped>
.options-vertical {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.options-vertical :deep(.el-radio),
.options-vertical :deep(.el-checkbox) {
  margin-right: 0;
  height: auto;
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  transition: all 0.15s;
}
.options-vertical :deep(.el-radio:hover),
.options-vertical :deep(.el-checkbox:hover) {
  border-color: #c6e2ff;
  background: #f5f9ff;
}
.options-vertical :deep(.el-radio.is-checked),
.options-vertical :deep(.el-checkbox.is-checked) {
  border-color: #409eff;
  background: #ecf5ff;
}
.options-vertical :deep(.el-radio__label),
.options-vertical :deep(.el-checkbox__label) {
  font-size: 15px;
  white-space: normal;
  line-height: 1.5;
}
.rating-box {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.rating-hint {
  font-size: 12px;
  color: #909399;
}
.image-question {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.question-image img {
  max-width: 100%;
  max-height: 320px;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.answer-area {
  margin-top: 12px;
}

.answer-preview img {
  max-width: 200px;
  max-height: 200px;
  border-radius: 6px;
}
.image-loading {
  color: #909399;
  font-size: 13px;
  padding: 8px 0;
}
</style>