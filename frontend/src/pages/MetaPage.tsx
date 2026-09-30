import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import {
  listCodeTypeCandidates,
  listCodeTypes,
  listMetaTables,
  syncMeta,
} from '../api/endpoints'
import { ApiClientError } from '../api/client'
import { Alert, Button, Card, Input, Badge } from '../components/ui'

export function MetaPage() {
  const [mtdtId, setMtdtId] = useState('')
  const [activeMtdt, setActiveMtdt] = useState('')
  const [syncResult, setSyncResult] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const syncMut = useMutation({
    mutationFn: () => syncMeta(mtdtId),
    onSuccess: (res) => {
      setActiveMtdt(res.mtdtId)
      setSyncResult(
        `동기화 완료 — 테이블 ${res.tableCount}건 (추가 ${res.addedTableCount}, 누락 ${res.missingTableCount})`,
      )
      setError(null)
    },
    onError: (e: Error) =>
      setError(e instanceof ApiClientError ? e.message : e.message),
  })

  const { data: tables = [], isFetching: tablesLoading } = useQuery({
    queryKey: ['meta-tables', activeMtdt],
    queryFn: () => listMetaTables(activeMtdt),
    enabled: !!activeMtdt,
  })

  const { data: codeTypes = [] } = useQuery({
    queryKey: ['code-types', activeMtdt],
    queryFn: () => listCodeTypes(activeMtdt),
    enabled: !!activeMtdt,
  })

  const { data: candidates = [] } = useQuery({
    queryKey: ['code-candidates', activeMtdt],
    queryFn: () => listCodeTypeCandidates(activeMtdt),
    enabled: !!activeMtdt,
  })

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-white">메타데이터</h1>
        <p className="text-sm text-slate-400">
          sync · tables · code-types API
        </p>
      </header>

      {error && <Alert type="error">{error}</Alert>}
      {syncResult && <Alert type="success">{syncResult}</Alert>}

      <Card title="메타 동기화">
        <div className="flex flex-wrap items-end gap-4">
          <Input
            label="mtdtId (메타데이터세트 ID)"
            placeholder="mtdt-20260623-001"
            value={mtdtId}
            onChange={(e) => setMtdtId(e.target.value)}
            className="min-w-[280px]"
          />
          <Button
            loading={syncMut.isPending}
            disabled={!mtdtId.trim()}
            onClick={() => syncMut.mutate()}
          >
            POST /meta/sync
          </Button>
          <Button
            variant="secondary"
            disabled={!mtdtId.trim()}
            onClick={() => setActiveMtdt(mtdtId.trim())}
          >
            테이블 조회
          </Button>
        </div>
      </Card>

      {activeMtdt && (
        <>
          <Card title={`테이블 목록 — ${activeMtdt}`}>
            {tablesLoading ? (
              <p className="text-slate-400">로딩…</p>
            ) : (
              <div className="max-h-96 overflow-auto">
                <table className="w-full text-left text-sm">
                  <thead className="sticky top-0 bg-slate-900 text-slate-500">
                    <tr>
                      <th className="py-2">mtbl ID</th>
                      <th className="py-2">원천 테이블</th>
                      <th className="py-2">존재</th>
                      <th className="py-2">코드테이블</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {tables.map((t) => (
                      <tr key={t.mtdtTblId}>
                        <td className="py-2 font-mono text-xs text-slate-500">
                          {t.mtdtTblId}
                        </td>
                        <td className="py-2 text-slate-200">
                          {t.orgnlTblNm}
                        </td>
                        <td className="py-2">
                          {t.srcExstYn ? (
                            <Badge tone="success">Y</Badge>
                          ) : (
                            <Badge tone="error">N</Badge>
                          )}
                        </td>
                        <td className="py-2">
                          {t.cdTblYn ? '코드' : '-'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          <div className="grid gap-4 md:grid-cols-2">
            <Card title={`코드유형 (${codeTypes.length})`}>
              <ul className="space-y-1 text-sm text-slate-300">
                {codeTypes.map((c) => (
                  <li key={c.mtdtTblId} className="font-mono">
                    {c.orgnlTblNm}
                  </li>
                ))}
                {codeTypes.length === 0 && (
                  <li className="text-slate-500">없음</li>
                )}
              </ul>
            </Card>
            <Card title={`코드유형 후보 (${candidates.length})`}>
              <ul className="space-y-1 text-sm text-slate-300">
                {candidates.map((c) => (
                  <li key={c.mtdtTblId} className="font-mono">
                    {c.orgnlTblNm}
                  </li>
                ))}
                {candidates.length === 0 && (
                  <li className="text-slate-500">없음</li>
                )}
              </ul>
            </Card>
          </div>
        </>
      )}
    </div>
  )
}
