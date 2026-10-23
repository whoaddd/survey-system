import request from '@/utils/request'
import type { GeneratedSurvey, AiScoreResult, EvaluateRequest } from '@/types/ai'

/**
 * AI 生成题目：模型推理通常 10–30 秒，单独设置 60 秒超时
 */
export function generateSurvey(data: {
  topic: string
  count: number
}): Promise<GeneratedSurvey> {
  return request.post('/ai/surveys/generate', data, {
    timeout: 60000,
  })
}

/**
 * AI 辅助评分：单条文本评分通常 5–20 秒，60 秒兜底
 */
export function evaluateResponse(
  responseId: number,
  data: EvaluateRequest
): Promise<AiScoreResult> {
  return request.post(`/ai/responses/${responseId}/evaluate`, data, {
    timeout: 60000,
  })
}