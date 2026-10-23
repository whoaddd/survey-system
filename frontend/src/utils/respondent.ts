const KEY_PREFIX = 'survey_respondent_'

function uuid(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  // 兼容旧浏览器
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

export function getRespondentKey(shareCode: string): string {
  const storageKey = `${KEY_PREFIX}${shareCode}`
  let key = localStorage.getItem(storageKey)
  if (!key) {
    key = uuid()
    localStorage.setItem(storageKey, key)
  }
  return key
}

export function hasSubmitted(shareCode: string): boolean {
  return localStorage.getItem(`${KEY_PREFIX}submitted_${shareCode}`) === '1'
}

export function markSubmitted(shareCode: string): void {
  localStorage.setItem(`${KEY_PREFIX}submitted_${shareCode}`, '1')
}