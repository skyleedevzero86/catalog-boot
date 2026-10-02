import { useMutation, useQuery } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import {
  createFileLoadTable,
  downloadFileLoadTemplate,
  listFileLoadColumns,
  listFileLoadTables,
  probeFileLoadConnection,
  uploadFileLoad,
} from '../api/endpoints'
import { toErrorMessage } from '../api/client'
import { ConnectionPicker } from '../components/ConnectionPicker'
import { Alert, Button, Card, Input, Select } from '../components/ui'
import type { FileColumnDef, SpreadsheetFormat } from '../types/api'

type Step = 1 | 2 | 3 | 4

const SQL_TYPES = [
  'VARCHAR(255)',
  'INTEGER',
  'BIGINT',
  'NUMERIC(18,4)',
  'BOOLEAN',
  'DATE',
  'TIMESTAMP',
  'TEXT',
]

const emptyColumn = (): FileColumnDef => ({
  name: '',
  sqlType: 'VARCHAR(255)',
  nullable: true,
  comment: '',
})

function triggerDownload(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = fileName
  anchor.click()
  URL.revokeObjectURL(url)
}

export function FileLoadPage() {
  const [step, setStep] = useState<Step>(1)
  const [connectionId, setConnectionId] = useState('')
  const [schemaName, setSchemaName] = useState('')
  const [tableMode, setTableMode] = useState<'existing' | 'create'>('existing')
  const [tableName, setTableName] = useState('')
  const [tableComment, setTableComment] = useState('')
  const [columns, setColumns] = useState<FileColumnDef[]>([emptyColumn()])
  const [format, setFormat] = useState<SpreadsheetFormat>('xlsx')
  const [file, setFile] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [msg, setMsg] = useState<string | null>(null)
  const [probeOk, setProbeOk] = useState(false)

  const tablesQuery = useQuery({
    queryKey: ['file-load-tables', connectionId, schemaName],
    queryFn: () => listFileLoadTables(connectionId, schemaName || undefined),
    enabled: !!connectionId && probeOk && tableMode === 'existing',
  })

  const columnsQuery = useQuery({
    queryKey: ['file-load-columns', connectionId, schemaName, tableName],
    queryFn: () =>
      listFileLoadColumns(connectionId, tableName, schemaName || undefined),
    enabled:
      !!connectionId &&
      probeOk &&
      tableMode === 'existing' &&
      !!tableName &&
      step >= 2,
  })

  const resolvedColumns = useMemo(() => {
    if (tableMode === 'create') {
      return columns
    }
    return columnsQuery.data ?? []
  }, [tableMode, columns, columnsQuery.data])

  const probeMut = useMutation({
    mutationFn: () => probeFileLoadConnection(connectionId),
    onSuccess: (res) => {
      setProbeOk(res.connected)
      setMsg(res.message)
      setError(res.connected ? null : res.message)
      if (res.connected) {
        setStep(2)
      }
    },
    onError: (e: Error) => {
      setProbeOk(false)
      setError(toErrorMessage(e))
    },
  })

  const createMut = useMutation({
    mutationFn: () =>
      createFileLoadTable({
        connectionId,
        schemaName: schemaName || undefined,
        tableName,
        tableComment: tableComment || undefined,
        columns: columns.filter((c) => c.name.trim()),
      }),
    onSuccess: () => {
      setMsg(`테이블 생성 완료: ${tableName}`)
      setError(null)
      setTableMode('existing')
      tablesQuery.refetch()
      setStep(3)
    },
    onError: (e: Error) => setError(toErrorMessage(e)),
  })

  const templateMut = useMutation({
    mutationFn: () =>
      downloadFileLoadTemplate(
        connectionId,
        tableName,
        format,
        schemaName || undefined,
      ),
    onSuccess: ({ blob, fileName }) => {
      triggerDownload(blob, fileName)
      setMsg(`양식 다운로드: ${fileName}`)
      setError(null)
      setStep(4)
    },
    onError: (e: Error) => setError(toErrorMessage(e)),
  })

  const uploadMut = useMutation({
    mutationFn: () => {
      if (!file) {
        throw new Error('업로드할 파일을 선택하세요.')
      }
      return uploadFileLoad(
        connectionId,
        tableName,
        format,
        file,
        schemaName || undefined,
      )
    },
    onSuccess: (res) => {
      setMsg(res.message)
      setError(null)
    },
    onError: (e: Error) => setError(toErrorMessage(e)),
  })

  const canGoTemplate =
    !!connectionId &&
    probeOk &&
    !!tableName &&
    (tableMode === 'existing'
      ? (columnsQuery.data?.length ?? 0) > 0
      : columns.some((c) => c.name.trim()))

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-white">파일 적재</h1>
        <p className="text-sm text-slate-400">
          등록 연결 선택 → 테이블/양식 → 업로드 · PG/MySQL/MariaDB/Oracle는 sp_cdw_stg_* ·
          ClickHouse는 multi-VALUES 배치
        </p>
      </header>

      {error && <Alert type="error">{error}</Alert>}
      {msg && <Alert type="success">{msg}</Alert>}

      <div className="flex flex-wrap gap-2">
        {[
          { id: 1 as Step, label: '1. 연결 점검' },
          { id: 2 as Step, label: '2. 테이블' },
          { id: 3 as Step, label: '3. 양식' },
          { id: 4 as Step, label: '4. 업로드' },
        ].map((item) => (
          <button
            key={item.id}
            type="button"
            onClick={() => setStep(item.id)}
            className={`rounded-lg px-4 py-2 text-sm font-medium transition ${
              step === item.id
                ? 'bg-indigo-600 text-white'
                : 'bg-slate-800 text-slate-400 hover:text-white'
            }`}
          >
            {item.label}
          </button>
        ))}
      </div>

      {step === 1 && (
        <Card title="등록 연결 접속 점검">
          <div className="grid max-w-xl gap-4">
            <ConnectionPicker
              label="대상 DB 연결"
              value={connectionId}
              onChange={(id) => {
                setConnectionId(id)
                setProbeOk(false)
                setTableName('')
              }}
              allowEmpty
            />
            <Input
              label="스키마 (선택, 비우면 연결 기본값)"
              value={schemaName}
              onChange={(e) => setSchemaName(e.target.value)}
              placeholder="etl_data"
            />
            <Button
              loading={probeMut.isPending}
              disabled={!connectionId}
              onClick={() => probeMut.mutate()}
            >
              접속 점검
            </Button>
          </div>
        </Card>
      )}

      {step === 2 && (
        <Card title="테이블 선택 또는 생성">
          <div className="mb-4 flex gap-2">
            <Button
              variant={tableMode === 'existing' ? 'primary' : 'secondary'}
              onClick={() => setTableMode('existing')}
            >
              기존 테이블
            </Button>
            <Button
              variant={tableMode === 'create' ? 'primary' : 'secondary'}
              onClick={() => setTableMode('create')}
            >
              새 테이블
            </Button>
          </div>

          {tableMode === 'existing' ? (
            <div className="grid max-w-xl gap-4">
              <Select
                label="테이블"
                value={tableName}
                onChange={(e) => setTableName(e.target.value)}
                options={[
                  { value: '', label: '선택…' },
                  ...(tablesQuery.data ?? []).map((t) => ({
                    value: t.tableName,
                    label: t.remarks
                      ? `${t.tableName} — ${t.remarks}`
                      : t.tableName,
                  })),
                ]}
              />
              {tableName && (
                <div className="rounded-lg border border-slate-800 p-3 text-sm text-slate-300">
                  <p className="mb-2 font-medium text-slate-200">컬럼</p>
                  {columnsQuery.isLoading && <p>불러오는 중…</p>}
                  <ul className="space-y-1">
                    {resolvedColumns.map((c) => (
                      <li key={c.name} className="font-mono text-xs">
                        {c.name} {c.sqlType}
                        {c.nullable ? '' : ' NOT NULL'}
                        {c.comment ? ` — ${c.comment}` : ''}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
              <Button
                disabled={!canGoTemplate}
                onClick={() => setStep(3)}
              >
                다음: 양식 다운로드
              </Button>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="grid max-w-xl gap-4 md:grid-cols-2">
                <Input
                  label="테이블명"
                  value={tableName}
                  onChange={(e) => setTableName(e.target.value)}
                  placeholder="file_load_demo"
                />
                <Input
                  label="테이블 설명"
                  value={tableComment}
                  onChange={(e) => setTableComment(e.target.value)}
                />
              </div>
              <div className="space-y-3">
                {columns.map((col, index) => (
                  <div
                    key={index}
                    className="grid gap-2 rounded-lg border border-slate-800 p-3 md:grid-cols-4"
                  >
                    <Input
                      label="컬럼명"
                      value={col.name}
                      onChange={(e) => {
                        const next = [...columns]
                        next[index] = { ...col, name: e.target.value }
                        setColumns(next)
                      }}
                    />
                    <Select
                      label="타입"
                      value={col.sqlType}
                      onChange={(e) => {
                        const next = [...columns]
                        next[index] = { ...col, sqlType: e.target.value }
                        setColumns(next)
                      }}
                      options={SQL_TYPES.map((t) => ({ value: t, label: t }))}
                    />
                    <Select
                      label="NULL"
                      value={col.nullable ? 'Y' : 'N'}
                      onChange={(e) => {
                        const next = [...columns]
                        next[index] = {
                          ...col,
                          nullable: e.target.value === 'Y',
                        }
                        setColumns(next)
                      }}
                      options={[
                        { value: 'Y', label: '허용' },
                        { value: 'N', label: '불가' },
                      ]}
                    />
                    <Input
                      label="코멘트"
                      value={col.comment ?? ''}
                      onChange={(e) => {
                        const next = [...columns]
                        next[index] = { ...col, comment: e.target.value }
                        setColumns(next)
                      }}
                    />
                  </div>
                ))}
              </div>
              <div className="flex flex-wrap gap-2">
                <Button
                  variant="secondary"
                  onClick={() => setColumns([...columns, emptyColumn()])}
                >
                  컬럼 추가
                </Button>
                <Button
                  loading={createMut.isPending}
                  disabled={!tableName || !columns.some((c) => c.name.trim())}
                  onClick={() => createMut.mutate()}
                >
                  테이블 생성 후 양식으로
                </Button>
              </div>
            </div>
          )}
        </Card>
      )}

      {step === 3 && (
        <Card title="양식 다운로드">
          <div className="grid max-w-xl gap-4">
            <p className="text-sm text-slate-400">
              대상: {tableName || '(테이블 미선택)'} · 컬럼{' '}
              {resolvedColumns.length}개
            </p>
            <Select
              label="파일 형식"
              value={format}
              onChange={(e) => setFormat(e.target.value as SpreadsheetFormat)}
              options={[
                { value: 'xlsx', label: 'Excel (.xlsx)' },
                { value: 'xls', label: 'Excel 97-2003 (.xls)' },
                { value: 'csv', label: 'CSV (.csv)' },
              ]}
            />
            <Button
              loading={templateMut.isPending}
              disabled={!canGoTemplate}
              onClick={() => templateMut.mutate()}
            >
              양식 다운로드
            </Button>
          </div>
        </Card>
      )}

      {step === 4 && (
        <Card title="파일 업로드·적재">
          <div className="grid max-w-xl gap-4">
            <Select
              label="파일 형식"
              value={format}
              onChange={(e) => setFormat(e.target.value as SpreadsheetFormat)}
              options={[
                { value: 'xlsx', label: 'Excel (.xlsx)' },
                { value: 'xls', label: 'Excel 97-2003 (.xls)' },
                { value: 'csv', label: 'CSV (.csv)' },
              ]}
            />
            <label className="block text-sm">
              <span className="mb-1 block text-slate-400">파일</span>
              <input
                type="file"
                accept=".csv,.xlsx,.xls"
                className="block w-full text-sm text-slate-300"
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              />
            </label>
            <Button
              loading={uploadMut.isPending}
              disabled={!file || !tableName || !probeOk}
              onClick={() => uploadMut.mutate()}
            >
              업로드 적재
            </Button>
          </div>
        </Card>
      )}
    </div>
  )
}
