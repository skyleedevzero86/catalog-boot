import { apiFetch } from './client'
import type {
  Category,
  CategoryMapping,
  CodeTypeSummary,
  ConnectionDetail,
  ConnectionSummary,
  CreateConnectionRequest,
  LoadTableResult,
  MetaSyncResponse,
  MetaTableRow,
  MigrationJob,
  MigrationJobTable,
  TargetDdlPreview,
} from '../types/api'

export const listConnections = () =>
  apiFetch<ConnectionSummary[]>('/api/v1/conn/list')

export const getConnection = (id: string) =>
  apiFetch<ConnectionDetail>(`/api/v1/conn/detail/${id}`)

export const createConnection = (body: CreateConnectionRequest) =>
  apiFetch<ConnectionDetail>('/api/v1/conn/create', {
    method: 'POST',
    body: JSON.stringify(body),
  })

export const updateConnection = (
  id: string,
  body: Partial<CreateConnectionRequest>,
) =>
  apiFetch<ConnectionDetail>(`/api/v1/conn/update/${id}`, {
    method: 'POST',
    body: JSON.stringify(body),
  })

export const deleteConnection = (id: string) =>
  apiFetch<ConnectionDetail>(`/api/v1/conn/delete/${id}`, { method: 'POST' })

export const syncMeta = (mtdtId: string) =>
  apiFetch<MetaSyncResponse>('/api/v1/meta/sync', {
    method: 'POST',
    body: JSON.stringify({ mtdtId }),
  })

export const listMetaTables = (mtdtId: string) =>
  apiFetch<MetaTableRow[]>(`/api/v1/meta/tables/${mtdtId}`)

export const listCodeTypes = (mtdtId: string) =>
  apiFetch<CodeTypeSummary[]>(`/api/v1/meta/code-types/${mtdtId}`)

export const listCodeTypeCandidates = (mtdtId: string) =>
  apiFetch<CodeTypeSummary[]>(`/api/v1/meta/code-types/${mtdtId}/candidates`)

export const listCategories = (mtdtId: string) =>
  apiFetch<Category[]>(`/api/v1/category/list/${mtdtId}`)

export const createCategory = (
  mtdtId: string,
  body: {
    parentId?: string
    name: string
    description?: string
    sortNo?: number
    exposed?: boolean
    allowChildCategories?: boolean
  },
) =>
  apiFetch<Category>(`/api/v1/category/create/${mtdtId}`, {
    method: 'POST',
    body: JSON.stringify(body),
  })

export const updateCategory = (
  categoryId: string,
  body: {
    parentId?: string
    name: string
    description?: string
    sortNo?: number
    exposed?: boolean
    allowChildCategories?: boolean
  },
) =>
  apiFetch<Category>(`/api/v1/category/update/${categoryId}`, {
    method: 'POST',
    body: JSON.stringify(body),
  })

export const deleteCategory = (categoryId: string) =>
  apiFetch<void>(`/api/v1/category/delete/${categoryId}`, { method: 'POST' })

export const mapCategoryTables = (categoryId: string, tableIds: string[]) =>
  apiFetch<CategoryMapping[]>(`/api/v1/category/map-table/${categoryId}`, {
    method: 'POST',
    body: JSON.stringify({ tableIds }),
  })

export const previewDdl = (body: {
  sourceConnectionId: string
  targetConnectionId: string
  sourceSchema?: string
  targetSchema: string
  tableName: string
}) =>
  apiFetch<TargetDdlPreview>('/api/v1/migration/ddl/preview', {
    method: 'POST',
    body: JSON.stringify(body),
  })

export const loadTable = (body: {
  sourceConnectionId: string
  targetConnectionId: string
  sourceSchema?: string
  targetSchema: string
  tableName: string
  batchSize?: number
  dropExisting?: boolean
}) =>
  apiFetch<LoadTableResult>('/api/v1/migration/load', {
    method: 'POST',
    body: JSON.stringify(body),
  })

export const startBatchLoad = (body: {
  sourceConnectionId: string
  targetConnectionId: string
  mtdtId?: string
  sourceSchema?: string
  targetSchema: string
  tableNames?: string[]
  batchSize?: number
  dropExisting?: boolean
}) =>
  apiFetch<MigrationJob>('/api/v1/migration/load/batch', {
    method: 'POST',
    body: JSON.stringify(body),
  })

export const listMigrationJobs = (limit = 50) =>
  apiFetch<MigrationJob[]>(`/api/v1/migration/jobs?limit=${limit}`)

export const getMigrationJob = (jobId: string) =>
  apiFetch<MigrationJob>(`/api/v1/migration/jobs/${jobId}`)

export const getMigrationJobTables = (jobId: string) =>
  apiFetch<MigrationJobTable[]>(`/api/v1/migration/jobs/${jobId}/tables`)

export const cancelMigrationJob = (jobId: string) =>
  apiFetch<MigrationJob>(`/api/v1/migration/jobs/${jobId}/cancel`, {
    method: 'POST',
  })

export const retryMigrationJob = (jobId: string, failedOnly = true) =>
  apiFetch<MigrationJob>(
    `/api/v1/migration/jobs/${jobId}/retry?failedOnly=${failedOnly}`,
    { method: 'POST' },
  )

export {
  prepareExtract,
  exportDataset,
  cleanupDataset,
  getDataset,
} from './extract'
export type { PrepareExtractRequest, ExportDatasetRequest } from './extract'

export {
  probeFileLoadConnection,
  listFileLoadTables,
  listFileLoadColumns,
  createFileLoadTable,
  downloadFileLoadTemplate,
  downloadFileLoadTemplateForColumns,
  uploadFileLoad,
} from './fileLoad'
