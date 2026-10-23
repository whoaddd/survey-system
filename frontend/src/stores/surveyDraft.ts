import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { QuestionDto, QuestionType, QuestionView, SaveSurveyRequest } from '@/types/survey'

export interface OptionDraft {
  uid: string
  label: string
  value: string
}

export interface QuestionDraft {
  uid: string
  type: QuestionType
  title: string
  contentHtml: string | null
  imageKey: string | null
  required: boolean
  scoreEnabled: boolean
  scoreMin: number
  scoreMax: number
  options: OptionDraft[]
}

let uidSeed = 0
function nextUid(): string {
  return `q_${Date.now()}_${++uidSeed}`
}

export const useSurveyDraftStore = defineStore('surveyDraft', () => {
  const surveyId = ref<number | null>(null)
  const title = ref('')
  const description = ref<string | null>(null)
  const status = ref<'DRAFT' | 'PUBLISHED' | 'CLOSED'>('DRAFT')
  const questions = ref<QuestionDraft[]>([])
  const selectedUid = ref<string | null>(null)
  const dirty = ref(false)
  const saving = ref(false)

  function reset() {
    surveyId.value = null
    title.value = ''
    description.value = null
    status.value = 'DRAFT'
    questions.value = []
    selectedUid.value = null
    dirty.value = false
  }

  function loadFromServer(
    id: number,
    meta: { title: string; description: string | null; status: 'DRAFT' | 'PUBLISHED' | 'CLOSED' },
    views: QuestionView[]
  ) {
    surveyId.value = id
    title.value = meta.title
    description.value = meta.description
    status.value = meta.status
    questions.value = views.map((v) => ({
      uid: nextUid(),
      type: v.type,
      title: v.title,
      contentHtml: v.contentHtml,
      imageKey: v.imageKey,
      required: v.required,
      scoreEnabled: v.scoreEnabled,
      scoreMin: v.scoreMin,
      scoreMax: v.scoreMax,
      options: v.options.map((o) => ({ uid: nextUid(), label: o.label, value: o.value })),
    }))
    selectedUid.value = questions.value[0]?.uid ?? null
    dirty.value = false
  }

  function addQuestion(type: QuestionType) {
    const q: QuestionDraft = {
      uid: nextUid(),
      type,
      title: '',
      contentHtml: null,
      imageKey: null,
      required: false,
      scoreEnabled: false,
      scoreMin: 1,
      scoreMax: 5,
      options: ['SINGLE', 'MULTI'].includes(type)
        ? [
            { uid: nextUid(), label: '选项 1', value: 'option1' },
            { uid: nextUid(), label: '选项 2', value: 'option2' },
          ]
        : [],
    }
    questions.value.push(q)
    selectedUid.value = q.uid
    dirty.value = true
  }

  function removeQuestion(uid: string) {
    const idx = questions.value.findIndex((q) => q.uid === uid)
    if (idx < 0) return
    questions.value.splice(idx, 1)
    if (selectedUid.value === uid) {
      selectedUid.value = questions.value[Math.min(idx, questions.value.length - 1)]?.uid ?? null
    }
    dirty.value = true
  }

  function moveQuestion(from: number, to: number) {
    if (from === to) return
    const list = questions.value
    const [item] = list.splice(from, 1)
    list.splice(to, 0, item)
    dirty.value = true
  }

  function updateQuestion(uid: string, patch: Partial<QuestionDraft>) {
    const q = questions.value.find((x) => x.uid === uid)
    if (!q) return
    Object.assign(q, patch)
    dirty.value = true
  }

  function addOption(questionUid: string) {
    const q = questions.value.find((x) => x.uid === questionUid)
    if (!q) return
    const n = q.options.length + 1
    q.options.push({ uid: nextUid(), label: `选项 ${n}`, value: `option${n}` })
    dirty.value = true
  }

  function removeOption(questionUid: string, optionUid: string) {
    const q = questions.value.find((x) => x.uid === questionUid)
    if (!q) return
    q.options = q.options.filter((o) => o.uid !== optionUid)
    dirty.value = true
  }

  function toSaveRequest(): SaveSurveyRequest {
    return {
      title: title.value,
      description: description.value,
      questions: questions.value.map<QuestionDto>((q) => ({
        type: q.type,
        title: q.title,
        contentHtml: q.contentHtml,
        imageKey: q.imageKey,
        required: q.required,
        scoreEnabled: q.scoreEnabled,
        scoreMin: q.scoreMin,
        scoreMax: q.scoreMax,
        options: q.options.map((o) => ({ label: o.label, value: o.value })),
      })),
    }
  }

  function markSaved() {
    dirty.value = false
  }

  return {
    surveyId, title, description, status, questions, selectedUid, dirty, saving,
    reset, loadFromServer, addQuestion, removeQuestion, moveQuestion, updateQuestion,
    addOption, removeOption, toSaveRequest, markSaved,
  }
})