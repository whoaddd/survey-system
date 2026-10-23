export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  userId: number
  username: string
  role: string
}

export interface CurrentUser {
  id: number
  username: string
  role: string
}
export interface RegisterRequest {
  username: string
  password: string
}