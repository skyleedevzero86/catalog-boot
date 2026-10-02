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
  code?: string
  body?: ApiError
  path?: string

  constructor(status: number, message: string, body?: ApiError, path?: string) {
    super(message)
    this.name = 'ApiClientError'
    this.status = status
    this.code = body?.code
    this.body = body
    this.path = path
  }
}

export function toErrorMessage(error: unknown, fallback = '요청 처리 중 오류가 발생했습니다.'): string {
  if (error instanceof ApiClientError) {
    return error.message || fallback
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}

async function parseErrorBody(response: Response): Promise<ApiError | undefined> {
  const contentType = response.headers.get('content-type') ?? ''
  if (!contentType.includes('application/json')) {
    return undefined
  }
  try {
    return (await response.json()) as ApiError
  } catch (parseError) {
    if (import.meta.env.DEV) {
      console.warn('API 오류 응답 JSON 파싱 실패', {
        status: response.status,
        url: response.url,
        parseError,
      })
    }
    return undefined
  }
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type') && options.body && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  headers.set('userId', getUserId())

  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  } catch (networkError) {
    const message =
      networkError instanceof TypeError
        ? '서버에 연결할 수 없습니다. 네트워크 상태와 백엔드 기동 여부를 확인하세요.'
        : toErrorMessage(networkError, '네트워크 오류가 발생했습니다.')
    throw new ApiClientError(0, message, undefined, path)
  }

  if (!response.ok) {
    const body = await parseErrorBody(response)
    const message =
      body?.message?.trim() ||
      (response.status >= 500
        ? '서버 오류가 발생했습니다. 잠시 후 다시 시도하세요.'
        : `요청이 실패했습니다. (HTTP ${response.status})`)
    throw new ApiClientError(response.status, message, body, path)
  }

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  if (!text) {
    return undefined as T
  }

  try {
    return JSON.parse(text) as T
  } catch (parseError) {
    throw new ApiClientError(
      response.status,
      '서버 응답을 해석할 수 없습니다.',
      undefined,
      path,
    )
  }
}

export type BlobDownload = {
  blob: Blob
  fileName: string
}

export async function apiDownload(
  path: string,
  options: RequestInit = {},
): Promise<BlobDownload> {
  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type') && options.body && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json')
  }
  headers.set('userId', getUserId())

  let response: Response
  try {
    response = await fetch(`${API_BASE}${path}`, { ...options, headers })
  } catch (networkError) {
    const message =
      networkError instanceof TypeError
        ? '서버에 연결할 수 없습니다. 네트워크 상태와 백엔드 기동 여부를 확인하세요.'
        : toErrorMessage(networkError, '네트워크 오류가 발생했습니다.')
    throw new ApiClientError(0, message, undefined, path)
  }

  if (!response.ok) {
    const body = await parseErrorBody(response)
    const message =
      body?.message?.trim() ||
      (response.status >= 500
        ? '서버 오류가 발생했습니다. 잠시 후 다시 시도하세요.'
        : `요청이 실패했습니다. (HTTP ${response.status})`)
    throw new ApiClientError(response.status, message, body, path)
  }

  const disposition = response.headers.get('content-disposition') ?? ''
  const match = /filename\*=UTF-8''([^;]+)|filename="?([^";]+)"?/i.exec(disposition)
  const rawName = match?.[1] || match?.[2] || 'download.bin'
  const fileName = decodeURIComponent(rawName)
  const blob = await response.blob()
  return { blob, fileName }
}
