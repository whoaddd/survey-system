export type SurveyStatus = 'DRAFT' | 'PUBLISHED' | 'CLOSED'

export type QuestionType = 'TEXT' | 'IMAGE' | 'SINGLE' | 'MULTI' | 'RATING'

export interface SurveyDetail {
  id: number
  title: string
  description: string | null
  status: SurveyStatus
  shareCode: string | null
  shareUrl: string | null
  version: number
  publishedVersion: number | null
  publishedAt: string | null
  createdAt: string
}

// ===== 编辑器相关 =====

export interface OptionDto {
  label: string
  value: string
}

export interface QuestionDto {
  type: QuestionType
  title: string
  contentHtml: string | null
  imageKey: string | null
  required: boolean
  scoreEnabled: boolean
  scoreMin: number
  scoreMax: number
  options: OptionDto[]
}

export interface SaveSurveyRequest {
  title: string
  description: string | null
  questions: QuestionDto[]
}

// 后端回显的题目视图（含 id 和 sortOrder）
export interface OptionView {
  id: number
  label: string
  value: string
  sortOrder: number
}

export interface QuestionView {
  id: number
  type: QuestionType
  title: string
  contentHtml: string | null
  imageKey: string | null
  required: boolean
  scoreEnabled: boolean
  scoreMin: number
  scoreMax: number
  sortOrder: number
  options: OptionView[]
}