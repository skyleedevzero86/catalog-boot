import { useState } from 'react'
import { useExtractWorkflow } from '../hooks/useExtractWorkflow'
import { ConnectionPicker } from '../components/ConnectionPicker'
import {
  Alert,
  Badge,
  Button,
  Card,
  Input,
  Select,
  statusTone,
} from '../components/ui'

export function ExtractPage() {
  const [sourceId, setSourceId] = useState('')
  const [stagingId, setStagingId] = useState('')
  const [sourceSchema, setSourceSchema] = useState('')
  const [tableName, setTableName] = useState('')
  const [columnKeys, setColumnKeys] = useState('id,name')
  const [outputFormat, setOutputFormat] = useState('csv')

  const {
    datasetId,
    setDatasetId,
    manifest,
    manifestError,
    refetchManifest,
    feedback,
    prepareMutation,
    exportMutation,
    cleanupMutation,
  } = useExtractWorkflow()

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-white">추출 (Extract)</h1>
        <p className="text-sm text-slate-400">
          prepare → export (CSV/Parquet) → cleanup
        </p>
      </header>

      {(feedback.error || manifestError) && (
        <Alert type="error">{feedback.error ?? manifestError}</Alert>
      )}
      {feedback.message && <Alert type="success">{feedback.message}</Alert>}

      <Card title="1. Prepare — 스테이징 적재">
        <div className="grid gap-4 md:grid-cols-2">
          <ConnectionPicker
            label="원천 연결 *"
            value={sourceId}
            onChange={setSourceId}
          />
          <ConnectionPicker
            label="스테이징 연결"
            value={stagingId}
            onChange={setStagingId}
            allowEmpty
            emptyLabel="(기본값 사용)"
          />
          <Input
            label="원천 스키마"
            value={sourceSchema}
            onChange={(e) => setSourceSchema(e.target.value)}
          />
          <Input
            label="테이블명 *"
            value={tableName}
            onChange={(e) => setTableName(e.target.value)}
          />
          <Input
            label="컬럼 keys (쉼표)"
            className="md:col-span-2"
            value={columnKeys}
            onChange={(e) => setColumnKeys(e.target.value)}
            placeholder="emp_id, emp_name"
          />
        </div>
        <div className="mt-4">
          <Button
            loading={prepareMutation.isPending}
            disabled={!sourceId || !tableName}
            onClick={() =>
              prepareMutation.mutate({
                sourceConnectionId: sourceId,
                stagingConnectionId: stagingId || undefined,
                sourceSchema: sourceSchema || undefined,
                tableName,
                columns: columnKeys.split(',').map((key) => ({
                  key: key.trim(),
                  label: key.trim(),
                })),
                deduplicate: true,
                replaceExisting: true,
              })
            }
          >
            Prepare
          </Button>
        </div>
      </Card>

      {datasetId && (
        <>
          <Card title="2. Export — 파일 추출">
            <div className="mb-4 flex flex-wrap items-end gap-4">
              <Input
                label="datasetId"
                value={datasetId}
                onChange={(e) => setDatasetId(e.target.value)}
                className="min-w-[320px] font-mono"
              />
              <Select
                label="출력 형식"
                value={outputFormat}
                onChange={(e) => setOutputFormat(e.target.value)}
                options={[
                  { value: 'csv', label: 'CSV' },
                  { value: 'parquet', label: 'Parquet' },
                ]}
              />
              <Button
                loading={exportMutation.isPending}
                onClick={() =>
                  exportMutation.mutate({
                    outputFormat,
                    includeHeader: true,
                    singleFile: false,
                  })
                }
              >
                Export
              </Button>
            </div>
          </Card>

          <Card title="3. Cleanup · Manifest">
            <div className="mb-4 flex flex-wrap gap-2">
              <Button
                variant="secondary"
                loading={cleanupMutation.isPending}
                onClick={() => cleanupMutation.mutate(false)}
              >
                cleanup (manifest 유지)
              </Button>
              <Button
                variant="danger"
                loading={cleanupMutation.isPending}
                onClick={() => cleanupMutation.mutate(true)}
              >
                cleanup + manifest 삭제
              </Button>
              <Button variant="ghost" onClick={() => refetchManifest()}>
                manifest 새로고침
              </Button>
            </div>

            {manifest && (
              <dl className="grid gap-3 text-sm md:grid-cols-2">
                <div>
                  <dt className="text-slate-500">상태</dt>
                  <dd className="mt-1">
                    <Badge tone={statusTone(manifest.status)}>
                      {manifest.status}
                    </Badge>
                  </dd>
                </div>
                <div>
                  <dt className="text-slate-500">행 수</dt>
                  <dd className="mt-1 text-slate-200">
                    {manifest.rowCount.toLocaleString()}
                  </dd>
                </div>
                <div>
                  <dt className="text-slate-500">스테이징 테이블</dt>
                  <dd className="mt-1 font-mono text-xs text-slate-400">
                    {manifest.stagingTableName}
                  </dd>
                </div>
                <div className="md:col-span-2">
                  <dt className="text-slate-500">export 파일</dt>
                  <dd className="mt-1">
                    {manifest.exportFilePaths?.length ? (
                      <ul className="font-mono text-xs text-emerald-400">
                        {manifest.exportFilePaths.map((p) => (
                          <li key={p}>{p}</li>
                        ))}
                      </ul>
                    ) : (
                      <span className="text-slate-500">없음</span>
                    )}
                  </dd>
                </div>
              </dl>
            )}
          </Card>
        </>
      )}
    </div>
  )
}
