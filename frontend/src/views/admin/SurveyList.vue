<template>
  <div class="survey-list">
    <!-- 顶部操作栏 -->
    <el-card shadow="never" class="toolbar">
      <div class="toolbar-inner">
        <div>
          <h3>我的问卷</h3>
          <span class="sub">共 {{ total }} 份</span>
        </div>
        <el-button type="primary" :icon="Plus" @click="handleCreate">
          创建问卷
        </el-button>
      </div>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="never" class="table-card" v-loading="loading">
      <el-empty
        v-if="!loading && list.length === 0"
        description="还没有问卷，点击「创建问卷」开始"
      >
        <el-button type="primary" @click="handleCreate">创建第一份问卷</el-button>
      </el-empty>

      <template v-else>
        <el-table :data="list" style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" effect="light">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="版本" width="100">
            <template #default="{ row }">
              v{{ row.version }}
              <span v-if="row.publishedVersion" style="color: #909399">
                （发布 v{{ row.publishedVersion }}）
              </span>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="560" fixed="right">
  <template #default="{ row }">
    <!-- DRAFT：编辑 -->
    <el-button
      v-if="row.status === 'DRAFT'"
      type="primary" link size="small"
      @click="handleEdit(row)"
    >编辑</el-button>

    <!-- PUBLISHED：复制链接 -->
    <el-button
      v-if="row.status === 'PUBLISHED'"
      type="success" link size="small"
      @click="handleCopyLink(row)"
    >复制链接</el-button>

    <!-- 非 DRAFT：查看结果 -->
    <el-button
      v-if="row.status !== 'DRAFT'"
      type="primary" link size="small"
      @click="handleViewResult(row)"
    >查看结果</el-button>

    <!-- PUBLISHED：截止 -->
    <el-button
      v-if="row.status === 'PUBLISHED'"
      type="warning" link size="small"
      @click="handleClose(row)"
    >截止</el-button>

    <!-- 非 DRAFT：复制为新草稿 -->
    <el-button
      v-if="row.status !== 'DRAFT'"
      type="primary" link size="small"
      @click="handleCopy(row)"
    >复制为新草稿</el-button>

    <!-- 所有状态：删除 -->
    <el-button
      type="danger" link size="small"
      @click="handleDelete(row)"
    >删除</el-button>
  </template>
</el-table-column>
        </el-table>

        <el-pagination
          :current-page="page"
          :page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          style="margin-top: 16px; justify-content: flex-end"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </template>
    </el-card>
  </div>
</template>
 
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { pageSurveys, createSurvey, copySurvey, deleteSurvey,closeSurvey } from '../../api/survey'
import type { SurveyDetail, SurveyStatus } from '../../types/survey'
import { useRouter } from 'vue-router'

const router = useRouter()
const list = ref<SurveyDetail[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)

function statusText(s: SurveyStatus) {
  return { DRAFT: '草稿', PUBLISHED: '已发布', CLOSED: '已关闭' }[s] ?? s
}

function statusType(s: SurveyStatus): 'info' | 'success' | 'warning' {
  const map: Record<SurveyStatus, 'info' | 'success' | 'warning'> = {
    DRAFT: 'info',
    PUBLISHED: 'success',
    CLOSED: 'warning',
  }
  return map[s]
}

function formatTime(t: string | null) {
  if (!t) return '-'
  return t.replace('T', ' ').slice(0, 19)
}

async function load() {
  loading.value = true
  try {
    const res = await pageSurveys(page.value, size.value)
    list.value = res.list
    total.value = res.total
  } catch {
    // request 拦截器已提示
  } finally {
    loading.value = false
  }
}

function handlePageChange(currentPage: number) {
  page.value = currentPage
  load()
}

function handleSizeChange(newSize: number) {
  size.value = newSize
  page.value = 1
  load()
}

async function handleCreate() {
  try {
    const { value: title } = await ElMessageBox.prompt('请输入问卷标题', '创建问卷', {
      confirmButtonText: '创建',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '标题不能为空',
    })
    const survey = await createSurvey({ title })
    ElMessage.success('创建成功')
    router.push(`/admin/surveys/${survey.id}/edit`)
  } catch {
    // 用户取消或接口失败（拦截器已提示）
  }
}

async function handleEdit(row: SurveyDetail) {
  if (row.status !== 'DRAFT') {
    ElMessage.warning('只有草稿状态的问卷可编辑')
    return
  }
  router.push(`/admin/surveys/${row.id}/edit`)
}

function handleViewResult(row: SurveyDetail) {
  if (row.status === 'DRAFT') {
    ElMessage.warning('草稿尚未发布，暂无结果')
    return
  }
  router.push(`/admin/surveys/${row.id}/result`)
}

async function handleCopy(row: SurveyDetail) {
  if (!['PUBLISHED', 'CLOSED'].includes(row.status)) {
    ElMessage.warning('只有已发布或已关闭的问卷可复制')
    return
  }
  try {
    await ElMessageBox.confirm(
      `将基于「${row.title}」的已发布版本复制一份新草稿，确定？`,
      '复制确认',
      { type: 'info', confirmButtonText: '复制', cancelButtonText: '取消' }
    )
  } catch {
    return  // 用户点取消
  }
  try {
    const newSurvey = await copySurvey(row.id)
    ElMessage.success('已复制为新草稿')
    router.push(`/admin/surveys/${newSurvey.id}/edit`)
  } catch (e) {
    // request 拦截器已弹 ElMessage，这里补一行便于定位
    console.error('[copySurvey] 失败：', e)
  }
}

// 新增：截止
async function handleClose(row: SurveyDetail) {
  if (row.status !== 'PUBLISHED') {
    ElMessage.warning('仅已发布问卷可截止')
    return
  }
  try {
    await ElMessageBox.confirm(
      `截止后答卷者将无法再提交，且分享链接将失效。确定截止「${row.title}」？`,
      '截止确认',
      { type: 'warning', confirmButtonText: '截止', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await closeSurvey(row.id)
    ElMessage.success('已截止')
    await load()
  } catch {
    // 拦截器已提示
  }
}

// 改写：删除（区分状态）
async function handleDelete(row: SurveyDetail) {
  if (row.status === 'PUBLISHED') {
    // 两段式：先截止，再删除
    try {
      await ElMessageBox.confirm(
        `「${row.title}」已发布，删除前需先截止。\n` +
        `截止后答卷者将无法提交，删除后问卷和已收集的答卷数据均不可恢复。\n` +
        `确定截止并删除？`,
        '删除确认',
        {
          type: 'warning',
          confirmButtonText: '截止并删除',
          cancelButtonText: '取消',
          dangerouslyUseHTMLString: false,
        }
      )
    } catch {
      return
    }
    try {
      await closeSurvey(row.id)
    } catch {
      // 截止失败，不继续删
      return
    }
  } else {
    try {
      await ElMessageBox.confirm(
        `确定删除问卷「${row.title}」？删除后不可恢复。`,
        '删除确认',
        { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
      )
    } catch {
      return
    }
  }

  try {
    await deleteSurvey(row.id)
    ElMessage.success('已删除')
    // 删的是当前页最后一条时回上一页
    if (list.value.length === 1 && page.value > 1) {
      page.value -= 1
    }
    await load()
  } catch {
    // 拦截器已提示
  }
}

function handleCopyLink(row: SurveyDetail) {
  if (!row.shareUrl) return
  const fullUrl = `${window.location.origin}${row.shareUrl}`
  navigator.clipboard.writeText(fullUrl).then(
    () => ElMessage.success('链接已复制'),
    () => ElMessage.warning('复制失败，请手动复制：' + fullUrl)
  )
}

onMounted(load)
</script>

<style scoped>
.survey-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-inner h3 {
  font-size: 18px;
  color: #303133;
  margin-bottom: 4px;
}

.toolbar-inner .sub {
  font-size: 13px;
  color: #909399;
}
</style>