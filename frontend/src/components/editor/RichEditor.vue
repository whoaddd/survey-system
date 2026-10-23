<template>
  <div class="rich-editor">
    <Toolbar :editor="editorRef" :defaultConfig="toolbarConfig" :mode="mode" class="toolbar" />
    <Editor
      v-model="valueHtml"
      :defaultConfig="editorConfig"
      :mode="mode"
      class="editor"
      @onCreated="handleCreated"
      @onChange="handleChange"
    />
  </div>
</template>

<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css'
import { ref, shallowRef, watch, onBeforeUnmount } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import type { IDomEditor, IEditorConfig } from '@wangeditor/editor'
import { ElMessage } from 'element-plus'
import { uploadImage } from '@/utils/upload'

const props = defineProps<{
  modelValue: string | null
  bizId: number | null
}>()
const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const editorRef = shallowRef<IDomEditor>()
const valueHtml = ref(props.modelValue ?? '')
const mode = 'default'

// 关键：工具栏和编辑器配置
const toolbarConfig = {
  excludeKeys: [
    'group-video',     // 不支持视频
    'insertLink',      // 不支持外链，防 XSS
    'uploadVideo',
  ],
}

const editorConfig: Partial<IEditorConfig> = {
  placeholder: '请输入题干...',
  MENU_CONF: {
    uploadImage: {
      // 自定义上传：走预签名直传
      async customUpload(file: File, insertFn: (url: string, alt: string, href: string) => void) {
        try {
          const result = await uploadImage(file, { bizType: 'QUESTION', bizId: props.bizId })
          // 换取展示 URL（相对短期有效，5 分钟内展示没问题）
          const { url } = await import('@/api/file').then((m) => m.getFileUrl(result.objectKey))
          // 关键：把 objectKey 存到 alt 属性里，后续能从 HTML 中提取
          insertFn(url, `__objectKey__${result.objectKey}`, '')
        } catch (e: any) {
          ElMessage.error(e.message || '上传失败')
        }
      },
    },
  },
}

function handleCreated(editor: IDomEditor) {
  editorRef.value = editor
}

function handleChange(editor: IDomEditor) {
  emit('update:modelValue', editor.getHtml())
}

watch(
  () => props.modelValue,
  (v) => {
    if (v !== valueHtml.value) valueHtml.value = v ?? ''
  }
)

onBeforeUnmount(() => {
  editorRef.value?.destroy()
})
</script>

<style scoped>
.rich-editor {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}
.toolbar {
  border-bottom: 1px solid #dcdfe6;
}
.editor {
  height: 200px;
  overflow-y: auto;
}
</style>