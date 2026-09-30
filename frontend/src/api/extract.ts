import { apiFetch } from './client'
import type {
  ExportExtractResponse,
  ExtractDataset,
  PrepareExtractResponse,
} from '../types/api'
import type { ExtractCodeMappingSpec, ExtractColumnSpec } from '../types/api'

export type PrepareExtractRequest = {
  datasetId?: string
  sourceConnectionId: string
  stagingConnectionId?: string
  sourceSchema?: string
  tableName?: string
  generatedSql?: string
  columns: ExtractColumnSpec[]
  codeMappings?: ExtractCodeMappingSpec[]
  deduplicate?: boolean
  replaceExisting?: boolean
  fetchSize?: number
}

export type ExportDatasetRequest = {
  outputPath?: string
  includeHeader?: boolean
  outputFormat?: string
  singleFile?: boolean
  selectedColumnKeys?: string[]
  maxRowsPerFile?: number
}

export const prepareExtract = (body: PrepareExtractRequest) =>
  apiFetch<PrepareExtractResponse>('/api/v1/extract/prepare', {
    method: 'POST',
    body: JSON.stringify(body),
  })

export const exportDataset = (
  datasetId: string,
  body?: ExportDatasetRequest,
) =>
  apiFetch<ExportExtractResponse>(
    `/api/v1/extract/datasets/${datasetId}/export`,
    { method: 'POST', body: JSON.stringify(body ?? {}) },
  )

export const cleanupDataset = (
  datasetId: string,
  dropManifest = false,
) =>
  apiFetch<ExtractDataset>(
    `/api/v1/extract/datasets/${datasetId}/cleanup`,
    {
      method: 'POST',
      body: JSON.stringify({ dropManifest }),
    },
  )

export const getDataset = (datasetId: string) =>
  apiFetch<ExtractDataset>(`/api/v1/extract/datasets/${datasetId}`)
