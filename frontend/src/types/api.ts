export type DatabaseVendor =
  | 'POSTGRESQL'
  | 'MYSQL'
  | 'MARIADB'
  | 'ORACLE'
  | 'CLICKHOUSE'

export type ConnectionLifecycleStatus = 'ACTIVE' | 'DELETED' | string
export type ConnectionHealthStatus = 'HEALTHY' | 'UNHEALTHY' | 'VALIDATING' | string

export interface ConnectionSummary {
  connectionId: string
  name: string
  vendor: DatabaseVendor
  host: string
  port: number
  databaseName: string
  enabled: boolean
  lifecycleStatus: ConnectionLifecycleStatus
  healthStatus: ConnectionHealthStatus
  lastTestAt?: string
}

export interface ConnectionDetail extends ConnectionSummary {
  schemaName?: string
  description?: string
  username: string
  creatorId?: string
  modifierId?: string
}

export interface CreateConnectionRequest {
  name: string
  vendor: DatabaseVendor
  host: string
  port: number
  databaseName: string
  schemaName?: string
  description?: string
  username: string
  password?: string
  enabled?: boolean
}

export interface MetaTableRow {
  mtdtTblId: string
  mtdtId: string
  orgnlTblNm: string
  tblNm?: string
  tblExpln?: string
  srcExstYn?: boolean
  cdTblYn?: boolean
  tblTypeCd?: string
  wholNocs?: number
}

export interface CodeTypeSummary {
  mtdtTblId: string
  orgnlTblNm: string
  tblNm?: string
}

export interface MetaSyncResponse {
  mtdtId: string
  lastSyncStatus: string
  lastSyncAt?: string
  lastSyncMessage?: string
  tableCount: number
  addedTableCount: number
  missingTableCount: number
  restoredTableCount: number
}

export interface Category {
  id: string
  mtdtId: string
  parentId?: string
  name: string
  description?: string
  sortNo: number
  exposed: boolean
  allowChildCategories: boolean
}

export interface CategoryMapping {
  mtdtTblCtgrMpngId: string
  mtdtTblCtgrId: string
  mtdtTblId: string
  sortNo: number
}

export interface TargetDdlPreview {
  tableName: string
  sourceVendor: DatabaseVendor
  targetVendor: DatabaseVendor
  createTableDdl: string
  columnCount: number
}

export interface LoadTableResult {
  jobId: string
  tableName: string
  status: string
  rowCount: number
  batchCount: number
  errorMessage?: string
}

export type MigrationJobStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'SUCCESS'
  | 'PARTIAL_SUCCESS'
  | 'FAILED'
  | 'CANCELLED'

export interface MigrationJob {
  jobId: string
  status: MigrationJobStatus
  sourceConnectionId: string
  targetConnectionId: string
  mtdtId?: string
  sourceSchema?: string
  targetSchema: string
  totalTableCount: number
  successTableCount: number
  failedTableCount: number
  totalRowCount: number
  errorMessage?: string
  startedAt?: string
  endedAt?: string
  createdAt?: string
}

export interface MigrationJobTable {
  jobTableId: string
  tableName: string
  status: string
  rowCount?: number
  batchCount?: number
  errorMessage?: string
  startedAt?: string
  endedAt?: string
  sortOrder: number
}

export type ExtractDatasetStatus =
  | 'PREPARING'
  | 'PREPARED'
  | 'EXPORTING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CLEANED'

export interface ExtractColumnSpec {
  key: string
  label?: string
}

export interface ExtractCodeMappingSpec {
  sourceColumnKey: string
  codeNameColumnKey: string
  codeNameLabel?: string
  schemaName: string
  codeTableName: string
  codeColumnName: string
  codeNameColumnName: string
}

export interface PrepareExtractResponse {
  datasetId: string
  status: ExtractDatasetStatus
  rowCount: number
  duplicateCount: number
  stagingTableName: string
  mappingTableNames: string[]
}

export interface ExtractDataset {
  datasetId: string
  status: ExtractDatasetStatus
  rowCount: number
  duplicateCount: number
  stagingTableName: string
  mappingTableNames: string[]
  exportFilePaths: string[]
}

export interface ExportExtractResponse {
  datasetId: string
  status: ExtractDatasetStatus
  rowCount: number
  filePaths: string[]
}

export interface ApiError {
  timestamp?: string
  status?: number
  error?: string
  message?: string
  path?: string
  code?: string
}

export interface FileLoadProbeResult {
  connectionId: string
  connected: boolean
  message: string
}

export interface FileTableSummary {
  tableName: string
  remarks?: string
}

export interface FileColumnDef {
  name: string
  sqlType: string
  nullable: boolean
  comment?: string
}

export interface CreateFileTableRequest {
  connectionId: string
  schemaName?: string
  tableName: string
  tableComment?: string
  columns: FileColumnDef[]
}

export interface FileLoadResult {
  connectionId: string
  schemaName: string
  tableName: string
  insertedRows: number
  message: string
}

export type SpreadsheetFormat = 'csv' | 'xlsx' | 'xls'
