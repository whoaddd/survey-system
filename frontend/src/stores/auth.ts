import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { CurrentUser, LoginRequest, LoginResponse } from '@/types/auth'
import { login as loginApi } from '@/api/auth'

const TOKEN_KEY = 'survey_token'
const USER_KEY = 'survey_user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref<CurrentUser | null>(
    localStorage.getItem(USER_KEY)
      ? JSON.parse(localStorage.getItem(USER_KEY) as string)
      : null
  )

  const isLoggedIn = computed(() => !!token.value)

  async function login(username: string, password: string) {
    const payload: LoginRequest = { username, password }
    const data: LoginResponse = await loginApi(payload)
    token.value = data.token
    user.value = { id: data.userId, username: data.username, role: data.role }
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  function setAuth(data: LoginResponse) {
  token.value = data.token
  user.value = { id: data.userId, username: data.username, role: data.role }
  localStorage.setItem(TOKEN_KEY, data.token)
  localStorage.setItem(USER_KEY, JSON.stringify(user.value))
}

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return { token, user, isLoggedIn, login, logout, setAuth }
})