import { apiDownload, apiFetch } from './client'
import type {
  CreateFileTableRequest,
  FileColumnDef,
  FileLoadProbeResult,
  FileLoadResult,
  FileTableSummary,
  SpreadsheetFormat,
} from '../types/api'

export const probeFileLoadConnection = (connectionId: string) =>
  apiFetch<FileLoadProbeResult>(
    `/api/v1/file-load/probe?connectionId=${encodeURIComponent(connectionId)}`,
    { method: 'POST' },
  )

export const listFileLoadTables = (connectionId: string, schemaName?: string) => {
  const params = new URLSearchParams({ connectionId })
  if (schemaName) {
    params.set('schemaName', schemaName)
  }
  return apiFetch<FileTableSummary[]>(`/api/v1/file-load/tables?${params}`)
}

export const listFileLoadColumns = (
  connectionId: string,
  tableName: string,
  schemaName?: string,
) => {
  const params = new URLSearchParams({ connectionId, tableName })
  if (schemaName) {
    params.set('schemaName', schemaName)
  }
  return apiFetch<FileColumnDef[]>(`/api/v1/file-load/columns?${params}`)
}

export const createFileLoadTable = (body: CreateFileTableRequest) =>
  apiFetch<void>('/api/v1/file-load/tables', {
    method: 'POST',
    body: JSON.stringify(body),
  })

export const downloadFileLoadTemplate = (
  connectionId: string,
  tableName: string,
  format: SpreadsheetFormat,
  schemaName?: string,
) => {
  const params = new URLSearchParams({
    connectionId,
    tableName,
    format,
  })
  if (schemaName) {
    params.set('schemaName', schemaName)
  }
  return apiDownload(`/api/v1/file-load/template?${params}`)
}

export const downloadFileLoadTemplateForColumns = (
  tableName: string,
  columns: FileColumnDef[],
  format: SpreadsheetFormat,
) =>
  apiDownload('/api/v1/file-load/template', {
    method: 'POST',
    body: JSON.stringify({ tableName, columns, format }),
  })

export const uploadFileLoad = (
  connectionId: string,
  tableName: string,
  format: SpreadsheetFormat,
  file: File,
  schemaName?: string,
) => {
  const params = new URLSearchParams({
    connectionId,
    tableName,
    format,
  })
  if (schemaName) {
    params.set('schemaName', schemaName)
  }
  const form = new FormData()
  form.append('file', file)
  return apiFetch<FileLoadResult>(`/api/v1/file-load/upload?${params}`, {
    method: 'POST',
    body: form,
  })
}
