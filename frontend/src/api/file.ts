import request from '@/utils/request'
import type {
  PresignRequest, PresignResponse, ConfirmRequest, ConfirmResponse,
} from '@/types/file'

export function presign(data: PresignRequest): Promise<PresignResponse> {
  return request.post('/file/presign', data)
}

export function confirm(data: ConfirmRequest): Promise<ConfirmResponse> {
  return request.post('/file/confirm', data)
}

// 这个接口不需要 token，但用同一个 axios 实例也行
export function getFileUrl(objectKey: string): Promise<{ url: string; expiresInSeconds: number }> {
  return request.get('/file/url', { params: { objectKey } })
}