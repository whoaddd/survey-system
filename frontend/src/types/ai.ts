export interface GeneratedQuestion {
  type: string
  title: string
  options: string[] | null
}

export interface GeneratedSurvey {
  questions: GeneratedQuestion[]
}

// ===== Day 3：AI 评分 =====
export interface AiScoreResult {
  rating: number
  reason: string
  confidence: number
}

export interface EvaluateRequest {
  questionId: number
  questionTitle: string
  answerText: string
}
export interface AiScoreResult {
  rating: number
  reason: string
  confidence: number
}