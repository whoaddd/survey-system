import request from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  PublicSurvey,
  ResponseSummary,
  SubmitResponseRequest,
  SubmitResponseResult,
  SurveyStatistics,
} from '@/types/response'

// ===== 公开接口（答卷者）=====
export function getPublicSurvey(shareCode: string): Promise<PublicSurvey> {
  return request.get(`/public/surveys/${shareCode}`)
}

export function submitResponse(data: SubmitResponseRequest): Promise<SubmitResponseResult> {
  return request.post('/public/responses', data)
}

// ===== 发布端接口（结果页）=====
export function pageResponses(
  surveyId: number,
  page = 1,
  size = 10
): Promise<PageResult<ResponseSummary>> {
  return request.get(`/surveys/${surveyId}/responses`, { params: { page, size } })
}

export function getStatistics(surveyId: number): Promise<SurveyStatistics> {
  return request.get(`/surveys/${surveyId}/statistics`)
}