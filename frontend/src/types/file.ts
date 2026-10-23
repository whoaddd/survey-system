export interface PresignRequest {
  bizType: 'QUESTION' | 'RESPONSE' | 'SURVEY'
  bizId: number | null
  filename: string
  mimeType: string
  size: number
}

export interface PresignResponse {
  objectKey: string
  uploadUrl: string
  expiresInSeconds: number
}

export interface ConfirmRequest {
  objectKey: string
  bizType: 'QUESTION' | 'RESPONSE' | 'SURVEY'
  bizId: number | null
  mimeType: string
  size: number
}

export interface ConfirmResponse {
  attachmentId: number
  objectKey: string
}