import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import {
  cancelMigrationJob,
  getMigrationJob,
  getMigrationJobTables,
  listMigrationJobs,
  loadTable,
  previewDdl,
  retryMigrationJob,
  startBatchLoad,
} from '../api/endpoints'
import { ConnectionPicker } from '../components/ConnectionPicker'
import { ApiClientError } from '../api/client'
import {
  Alert,
  Badge,
  Button,
  Card,
  Input,
  Textarea,
  statusTone,
} from '../components/ui'

type Tab = 'preview' | 'sync' | 'batch' | 'jobs'

export function MigrationPage() {
  const qc = useQueryClient()
  const [tab, setTab] = useState<Tab>('jobs')
  const [sourceId, setSourceId] = useState('')
  const [targetId, setTargetId] = useState('')
  const [sourceSchema, setSourceSchema] = useState('')
  const [targetSchema, setTargetSchema] = useState('etl_data')
  const [tableName, setTableName] = useState('')
  const [tableNames, setTableNames] = useState('')
  const [mtdtId, setMtdtId] = useState('')
  const [ddl, setDdl] = useState('')
  const [selectedJobId, setSelectedJobId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [msg, setMsg] = useState<string | null>(null)

  const { data: jobs = [], refetch: refetchJobs } = useQuery({
    queryKey: ['migration-jobs'],
    queryFn: () => listMigrationJobs(50),
    refetchInterval: tab === 'jobs' ? 5000 : false,
  })

  const { data: jobDetail } = useQuery({
    queryKey: ['migration-job', selectedJobId],
    queryFn: () => getMigrationJob(selectedJobId),
    enabled: !!selectedJobId,
    refetchInterval: 3000,
  })

  const { data: jobTables = [] } = useQuery({
    queryKey: ['migration-job-tables', selectedJobId],
    queryFn: () => getMigrationJobTables(selectedJobId),
    enabled: !!selectedJobId,
    refetchInterval: 3000,
  })

  const previewMut = useMutation({
    mutationFn: () =>
      previewDdl({
        sourceConnectionId: sourceId,
        targetConnectionId: targetId,
        sourceSchema: sourceSchema || undefined,
        targetSchema,
        tableName,
      }),
    onSuccess: (res) => {
      setDdl(res.createTableDdl)
      setMsg(`DDL 미리보기 — 컬럼 ${res.columnCount}개`)
      setError(null)
    },
    onError: (e: Error) =>
      setError(e instanceof ApiClientError ? e.message : e.message),
  })

  const loadMut = useMutation({
    mutationFn: () =>
      loadTable({
        sourceConnectionId: sourceId,
        targetConnectionId: targetId,
        sourceSchema: sourceSchema || undefined,
        targetSchema,
        tableName,
        batchSize: 500,
        dropExisting: true,
      }),
    onSuccess: (res) => {
      setMsg(`동기 적재 완료 — ${res.rowCount}행 (job: ${res.jobId})`)
      qc.invalidateQueries({ queryKey: ['migration-jobs'] })
    },
    onError: (e: Error) =>
      setError(e instanceof ApiClientError ? e.message : e.message),
  })

  const batchMut = useMutation({
    mutationFn: () =>
      startBatchLoad({
        sourceConnectionId: sourceId,
        targetConnectionId: targetId,
        mtdtId: mtdtId || undefined,
        sourceSchema: sourceSchema || undefined,
        targetSchema,
        tableNames: tableNames
          ? tableNames.split(/[\s,]+/).filter(Boolean)
          : undefined,
        batchSize: 500,
        dropExisting: true,
      }),
    onSuccess: (job) => {
      setSelectedJobId(job.jobId)
      setTab('jobs')
      setMsg(`배치 job 시작: ${job.jobId}`)
      qc.invalidateQueries({ queryKey: ['migration-jobs'] })
    },
    onError: (e: Error) =>
      setError(e instanceof ApiClientError ? e.message : e.message),
  })

  const tabs: { id: Tab; label: string }[] = [
    { id: 'jobs', label: 'Job 모니터' },
    { id: 'preview', label: 'DDL 미리보기' },
    { id: 'sync', label: '동기 적재' },
    { id: 'batch', label: '배치 적재' },
  ]

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-white">마이그레이션</h1>
        <p className="text-sm text-slate-400">
          DDL 변환 · 동기/비동기 적재 · job 운영
        </p>
      </header>

      {error && <Alert type="error">{error}</Alert>}
      {msg && <Alert type="success">{msg}</Alert>}

      <div className="flex flex-wrap gap-2">
        {tabs.map((t) => (
          <button
            key={t.id}
            type="button"
            onClick={() => setTab(t.id)}
            className={`rounded-lg px-4 py-2 text-sm font-medium transition ${
              tab === t.id
                ? 'bg-indigo-600 text-white'
                : 'bg-slate-800 text-slate-400 hover:text-white'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {(tab === 'preview' || tab === 'sync' || tab === 'batch') && (
        <Card title="공통 설정">
          <div className="grid gap-4 md:grid-cols-2">
            <ConnectionPicker
              label="원천 연결"
              value={sourceId}
              onChange={setSourceId}
            />
            <ConnectionPicker
              label="타깃 연결"
              value={targetId}
              onChange={setTargetId}
            />
            <Input
              label="원천 스키마"
              value={sourceSchema}
              onChange={(e) => setSourceSchema(e.target.value)}
            />
            <Input
              label="타깃 스키마 *"
              value={targetSchema}
              onChange={(e) => setTargetSchema(e.target.value)}
            />
          </div>
        </Card>
      )}

      {tab === 'preview' && (
        <Card title="DDL 미리보기">
          <div className="mb-4 flex flex-wrap gap-4">
            <Input
              label="테이블명"
              value={tableName}
              onChange={(e) => setTableName(e.target.value)}
            />
            <div className="flex items-end">
              <Button
                loading={previewMut.isPending}
                disabled={!sourceId || !targetId || !tableName}
                onClick={() => previewMut.mutate()}
              >
                미리보기
              </Button>
            </div>
          </div>
          {ddl && (
            <Textarea
              label="CREATE TABLE DDL"
              readOnly
              rows={12}
              value={ddl}
            />
          )}
        </Card>
      )}

      {tab === 'sync' && (
        <Card title="단일 테이블 동기 적재">
          <div className="flex flex-wrap items-end gap-4">
            <Input
              label="테이블명"
              value={tableName}
              onChange={(e) => setTableName(e.target.value)}
            />
            <Button
              loading={loadMut.isPending}
              disabled={!sourceId || !targetId || !tableName}
              onClick={() => loadMut.mutate()}
            >
              load (동기)
            </Button>
          </div>
        </Card>
      )}

      {tab === 'batch' && (
        <Card title="배치 비동기 적재">
          <div className="grid gap-4 md:grid-cols-2">
            <Input
              label="mtdtId (tableNames 대신)"
              value={mtdtId}
              onChange={(e) => setMtdtId(e.target.value)}
            />
            <Input
              label="tableNames (쉼표 구분)"
              placeholder="EMPLOYEES, DEPARTMENTS"
              value={tableNames}
              onChange={(e) => setTableNames(e.target.value)}
            />
          </div>
          <div className="mt-4">
            <Button
              loading={batchMut.isPending}
              disabled={!sourceId || !targetId}
              onClick={() => batchMut.mutate()}
            >
              load/batch
            </Button>
          </div>
        </Card>
      )}

      {tab === 'jobs' && (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card
            title="작업 목록"
            action={
              <Button variant="ghost" onClick={() => refetchJobs()}>
                새로고침
              </Button>
            }
          >
            <ul className="max-h-[480px] divide-y divide-slate-800 overflow-auto text-sm">
              {jobs.map((job) => (
                <li key={job.jobId}>
                  <button
                    type="button"
                    className={`flex w-full items-center justify-between px-2 py-3 text-left hover:bg-slate-800/50 ${
                      selectedJobId === job.jobId ? 'bg-indigo-950/40' : ''
                    }`}
                    onClick={() => setSelectedJobId(job.jobId)}
                  >
                    <span className="font-mono text-xs text-slate-400">
                      {job.jobId}
                    </span>
                    <Badge tone={statusTone(job.status)}>{job.status}</Badge>
                  </button>
                </li>
              ))}
            </ul>
          </Card>

          {selectedJobId && jobDetail && (
            <Card title="Job 상세">
              <dl className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <dt className="text-slate-500">상태</dt>
                  <dd>
                    <Badge tone={statusTone(jobDetail.status)}>
                      {jobDetail.status}
                    </Badge>
                  </dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-slate-500">테이블</dt>
                  <dd>
                    {jobDetail.successTableCount}/{jobDetail.totalTableCount}{' '}
                    성공
                  </dd>
                </div>
                <div className="flex justify-between">
                  <dt className="text-slate-500">총 행</dt>
                  <dd>{jobDetail.totalRowCount.toLocaleString()}</dd>
                </div>
              </dl>
              <div className="mt-4 flex flex-wrap gap-2">
                <Button
                  variant="secondary"
                  onClick={() =>
                    cancelMigrationJob(selectedJobId).then(() =>
                      qc.invalidateQueries({
                        queryKey: ['migration-job', selectedJobId],
                      }),
                    )
                  }
                >
                  취소
                </Button>
                <Button
                  variant="secondary"
                  onClick={() =>
                    retryMigrationJob(selectedJobId, true).then(() =>
                      setMsg('재시도 시작'),
                    )
                  }
                >
                  실패분 재시도
                </Button>
              </div>
              <div className="mt-6 max-h-64 overflow-auto">
                <table className="w-full text-left text-xs">
                  <thead className="text-slate-500">
                    <tr>
                      <th className="py-1">테이블</th>
                      <th className="py-1">상태</th>
                      <th className="py-1">행</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {jobTables.map((t) => (
                      <tr key={t.jobTableId}>
                        <td className="py-2 text-slate-300">{t.tableName}</td>
                        <td className="py-2">
                          <Badge tone={statusTone(t.status)}>
                            {t.status}
                          </Badge>
                        </td>
                        <td className="py-2 text-slate-400">
                          {t.rowCount ?? '-'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          )}
        </div>
      )}
    </div>
  )
}
