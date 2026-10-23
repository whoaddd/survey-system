<template>
  <div class="done-page">
    <el-result
      :icon="isRepeat ? 'warning' : 'success'"
      :title="isRepeat ? '你已提交过该问卷' : '提交成功'"
      :sub-title="isRepeat ? '同一浏览器无法重复提交' : '感谢你的参与'"
    >
      <template #extra>
        <div v-if="receiptCode && !isRepeat" class="receipt">
          <p class="label">回执码（请留存）</p>
          <div class="code-row">
            <span class="code">{{ receiptCode }}</span>
            <el-button size="small" @click="copyReceipt">复制</el-button>
          </div>
        </div>
        <el-button v-if="isRepeat" type="primary" @click="goHome">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const receiptCode = computed(() => route.query.receipt as string | undefined)
const isRepeat = computed(() => route.query.repeat === '1')

function copyReceipt() {
  if (!receiptCode.value) return
  navigator.clipboard.writeText(receiptCode.value).then(
    () => ElMessage.success('回执码已复制'),
    () => ElMessage.warning('复制失败，请手动记录')
  )
}

function goHome() {
  router.push('/login')
}
</script>

<style scoped>
.done-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  padding: 16px;
}

.receipt {
  margin: 16px 0;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
  text-align: center;
}

.receipt .label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.code-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.code {
  font-family: 'Menlo', 'Consolas', monospace;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  letter-spacing: 1px;
}
</style>