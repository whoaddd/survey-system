import type { QuestionType } from './survey'

// 公开问卷视图：不含 creatorId 等内部字段
export interface PublicOption {
  id: number
  label: string
  value: string
  sortOrder: number
}

export interface PublicQuestion {
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
  options: PublicOption[]
}

export interface PublicSurvey {
  shareCode: string
  title: string
  description: string | null
  version: number
  questions: PublicQuestion[]
}

// 提交答卷
export interface AnswerItem {
  questionId: number
  answerText?: string | null
  selectedOptionIds?: number[] | null
  ratingValue?: number | null
  attachmentKey?: string | null
}

export interface SubmitResponseRequest {
  shareCode: string
  respondentKey: string
  answers: AnswerItem[]
}

export interface SubmitResponseResult {
  receiptCode: string
  submittedAt: string
}

// 结果页
export interface ResponseSummary {
  id: number
  respondentKey: string
  receiptCode: string
  submittedAt: string
}

export interface QuestionStatistic {
  questionId: number
  title: string
  type: QuestionType
  // 评分题：均分 + 分布
  averageScore: number | null
  scoreDistribution: Record<string, number> | null
  // 单选/多选：选项分布
  optionDistribution: Record<string, number> | null
  // 文本题：回答列表
  textAnswers: string[] | null
}

export interface SurveyStatistics {
  totalResponses: number
  questions: QuestionStatistic[]
}