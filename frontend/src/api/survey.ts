import request from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  SaveSurveyRequest,
  SurveyDetail,
  QuestionView,
} from '@/types/survey'

export function pageSurveys(page = 1, size = 10): Promise<PageResult<SurveyDetail>> {
  return request.get('/surveys', { params: { page, size } })
}

export function getSurvey(id: number): Promise<SurveyDetail> {
  return request.get(`/surveys/${id}`)
}

export function createSurvey(data: { title: string; description?: string }): Promise<SurveyDetail> {
  return request.post('/surveys', data)
}

export function saveDraft(id: number, data: SaveSurveyRequest): Promise<QuestionView[]> {
  return request.put(`/surveys/${id}`, data)
}

export function getDraft(id: number): Promise<QuestionView[]> {
  return request.get(`/surveys/${id}/draft`)
}

export function publishSurvey(id: number): Promise<SurveyDetail> {
  return request.post(`/surveys/${id}/publish`)
}

export function closeSurvey(id: number): Promise<SurveyDetail> {
  return request.post(`/surveys/${id}/close`)
}

export function copySurvey(id: number): Promise<SurveyDetail> {
  return request.post(`/surveys/${id}/copy`)
}

export function deleteSurvey(id: number): Promise<void> {
  return request.delete(`/surveys/${id}`)
}