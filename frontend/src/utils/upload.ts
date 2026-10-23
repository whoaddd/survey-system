import axios from 'axios'

import { presign, confirm } from '@/api/file'

export interface UploadOptions {
  bizType: 'QUESTION' | 'RESPONSE' | 'SURVEY'
  bizId: number | null
}

export interface UploadResult {
  objectKey: string
  attachmentId: number
}

const ALLOWED_MIME = ['image/png', 'image/jpeg', 'image/gif', 'image/webp']
const MAX_SIZE = 10 * 1024 * 1024

export async function uploadImage(file: File, opts: UploadOptions): Promise<UploadResult> {
  // 1. 前端预校验（后端也会再校验一次）
  if (!ALLOWED_MIME.includes(file.type)) {
    throw new Error('仅支持 PNG / JPEG / GIF / WebP 图片')
  }
  if (file.size > MAX_SIZE) {
    throw new Error('图片不能超过 10MB')
  }

  // 2. 拿预签名
  const presignResp = await presign({
    bizType: opts.bizType,
    bizId: opts.bizId,
    filename: file.name,
    mimeType: file.type,
    size: file.size,
  })

  // 3. 直传 MinIO（关键：不走业务服务的 axios 实例，直接用 axios 裸调）
  await axios.put(presignResp.uploadUrl, file, {
    headers: { 'Content-Type': file.type },
    timeout: 60000,
  })

  // 4. confirm 登记
  const confirmResp = await confirm({
    objectKey: presignResp.objectKey,
    bizType: opts.bizType,
    bizId: opts.bizId,
    mimeType: file.type,
    size: file.size,
  })

  return { objectKey: confirmResp.objectKey, attachmentId: confirmResp.attachmentId }
}