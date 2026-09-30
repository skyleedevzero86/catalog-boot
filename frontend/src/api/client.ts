import type { ApiError } from '../types/api'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

export function getUserId(): string {
  return localStorage.getItem('cdw.userId') ?? 'admin'
}

export function setUserId(userId: string) {
  localStorage.setItem('cdw.userId', userId)
}

export class ApiClientError extends Error {
  status: number
  body?: ApiError

  constructor(status: number, message: string, body?: ApiError) {
    super(message)
    this.status = status
    this.body = body
  }
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json')
  }
  headers.set('userId', getUserId())

  const response = await fetch(`${API_BASE}${path}`, { ...options, headers })

  if (!response.ok) {
    let body: ApiError | undefined
    try {
      body = (await response.json()) as ApiError
    } catch {
      /* empty */
    }
    throw new ApiClientError(
      response.status,
      body?.message ?? `HTTP ${response.status}`,
      body,
    )
  }

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  if (!text) {
    return undefined as T
  }
  return JSON.parse(text) as T
}
