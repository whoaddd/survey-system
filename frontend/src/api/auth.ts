import request from '@/utils/request'
import type { LoginRequest, LoginResponse, RegisterRequest } from '@/types/auth'

export function login(data: LoginRequest): Promise<LoginResponse> {
  return request.post('/auth/login', data)
}

export function register(data: RegisterRequest): Promise<LoginResponse> {
  return request.post('/auth/register', data)
}