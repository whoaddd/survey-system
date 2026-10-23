import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'
import type { ApiResponse } from '@/types/api'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 请求拦截：注入 JWT
request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

// 响应拦截：统一处理 code 与 401
request.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>

    if (body.code === 0) {
      return body.data as never
    }

    // 业务层 401（HTTP 200 + code=40101）
    if (body.code === 40101) {
      handleUnauthorized()
      return Promise.reject(new Error(body.message))
    }

    // 其他业务错误
    ElMessage.error(body.message || '请求失败')
    return Promise.reject(new Error(body.message))
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    // Gateway 返回真 HTTP 401
    if (error.response?.status === 401) {
      handleUnauthorized()
      return Promise.reject(error)
    }

    const message = error.response?.data?.message || error.message || '网络异常'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

function handleUnauthorized() {
  const auth = useAuthStore()
  auth.logout()
  const current = router.currentRoute.value
  if (current.path !== '/login') {
    router.push({ path: '/login', query: { redirect: current.fullPath } })
  }
}

export default request