export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId: string | null
  success: boolean
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  size: number
}