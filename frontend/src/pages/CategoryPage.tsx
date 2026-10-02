import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import {
  createCategory,
  deleteCategory,
  listCategories,
  listMetaTables,
  mapCategoryTables,
} from '../api/endpoints'
import { toErrorMessage } from '../api/client'
import { Alert, Button, Card, Input } from '../components/ui'

export function CategoryPage() {
  const qc = useQueryClient()
  const [mtdtId, setMtdtId] = useState('')
  const [activeMtdt, setActiveMtdt] = useState('')
  const [name, setName] = useState('')
  const [mapCategoryId, setMapCategoryId] = useState('')
  const [selectedTables, setSelectedTables] = useState<string[]>([])
  const [error, setError] = useState<string | null>(null)
  const [msg, setMsg] = useState<string | null>(null)

  const {
    data: categories = [],
    isError: categoriesError,
    error: categoriesErrorValue,
  } = useQuery({
    queryKey: ['categories', activeMtdt],
    queryFn: () => listCategories(activeMtdt),
    enabled: !!activeMtdt,
  })

  const {
    data: tables = [],
    isError: tablesError,
    error: tablesErrorValue,
  } = useQuery({
    queryKey: ['meta-tables', activeMtdt],
    queryFn: () => listMetaTables(activeMtdt),
    enabled: !!activeMtdt,
  })

  const createMut = useMutation({
    mutationFn: () =>
      createCategory(activeMtdt, {
        name,
        exposed: true,
        allowChildCategories: true,
        sortNo: categories.length + 1,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['categories', activeMtdt] })
      setName('')
      setMsg('카테고리가 생성되었습니다.')
      setError(null)
    },
    onError: (e: Error) => setError(toErrorMessage(e)),
  })

  const deleteMut = useMutation({
    mutationFn: deleteCategory,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['categories', activeMtdt] })
      setMsg('카테고리가 삭제되었습니다.')
      setError(null)
    },
    onError: (e: Error) => setError(toErrorMessage(e)),
  })

  const mapMut = useMutation({
    mutationFn: () => mapCategoryTables(mapCategoryId, selectedTables),
    onSuccess: (rows) => {
      setMsg(`매핑 ${rows.length}건으로 교체되었습니다.`)
      setError(null)
    },
    onError: (e: Error) => setError(toErrorMessage(e)),
  })

  const toggleTable = (id: string) => {
    setSelectedTables((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    )
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-white">카테고리</h1>
        <p className="text-sm text-slate-400">
          트리 분류 · 테이블 매핑 전체 교체
        </p>
      </header>

      {(error || categoriesError || tablesError) && (
        <Alert type="error">
          {error ||
            (categoriesError
              ? toErrorMessage(categoriesErrorValue)
              : toErrorMessage(tablesErrorValue))}
        </Alert>
      )}
      {msg && <Alert type="success">{msg}</Alert>}

      <Card title="메타데이터세트 선택">
        <div className="flex flex-wrap gap-4">
          <Input
            label="mtdtId"
            value={mtdtId}
            onChange={(e) => setMtdtId(e.target.value)}
            className="min-w-[260px]"
          />
          <div className="flex items-end">
            <Button
              variant="secondary"
              disabled={!mtdtId.trim()}
              onClick={() => setActiveMtdt(mtdtId.trim())}
            >
              조회
            </Button>
          </div>
        </div>
      </Card>

      {activeMtdt && (
        <>
          <Card title="카테고리 생성">
            <div className="flex flex-wrap items-end gap-4">
              <Input
                label="카테고리명"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />
              <Button
                loading={createMut.isPending}
                disabled={!name.trim()}
                onClick={() => createMut.mutate()}
              >
                생성
              </Button>
            </div>
          </Card>

          <Card title={`카테고리 목록 (${categories.length})`}>
            <ul className="divide-y divide-slate-800 text-sm">
              {categories.map((c) => (
                <li
                  key={c.id}
                  className="flex items-center justify-between py-3"
                >
                  <div>
                    <span className="font-medium text-slate-200">
                      {c.name}
                    </span>
                    <span className="ml-3 font-mono text-xs text-slate-500">
                      {c.id}
                    </span>
                  </div>
                  <Button
                    variant="danger"
                    className="!px-2 !py-1 text-xs"
                    onClick={() => {
                      if (confirm('삭제하시겠습니까?')) deleteMut.mutate(c.id)
                    }}
                  >
                    삭제
                  </Button>
                </li>
              ))}
            </ul>
          </Card>

          <Card title="테이블 매핑 (전체 교체)">
            <div className="mb-4 grid gap-4 md:grid-cols-2">
              <Input
                label="categoryId"
                value={mapCategoryId}
                onChange={(e) => setMapCategoryId(e.target.value)}
                placeholder="ctgr-..."
              />
              <div className="flex items-end">
                <Button
                  loading={mapMut.isPending}
                  disabled={!mapCategoryId || selectedTables.length === 0}
                  onClick={() => mapMut.mutate()}
                >
                  map-table ({selectedTables.length}건)
                </Button>
              </div>
            </div>
            <div className="max-h-64 overflow-auto rounded border border-slate-800 p-2">
              {tables.map((t) => (
                <label
                  key={t.mtdtTblId}
                  className="flex cursor-pointer items-center gap-2 rounded px-2 py-1 hover:bg-slate-800"
                >
                  <input
                    type="checkbox"
                    checked={selectedTables.includes(t.mtdtTblId)}
                    onChange={() => toggleTable(t.mtdtTblId)}
                  />
                  <span className="font-mono text-sm text-slate-300">
                    {t.orgnlTblNm}
                  </span>
                  <span className="text-xs text-slate-600">{t.mtdtTblId}</span>
                </label>
              ))}
            </div>
          </Card>
        </>
      )}
    </div>
  )
}
