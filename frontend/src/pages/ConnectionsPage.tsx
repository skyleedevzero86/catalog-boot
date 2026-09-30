import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import {
  createConnection,
  deleteConnection,
  listConnections,
} from '../api/endpoints'
import type { CreateConnectionRequest, DatabaseVendor } from '../types/api'
import { ApiClientError } from '../api/client'
import {
  Alert,
  Badge,
  Button,
  Card,
  Input,
  Select,
  statusTone,
} from '../components/ui'

const vendors: DatabaseVendor[] = [
  'POSTGRESQL',
  'MYSQL',
  'MARIADB',
  'ORACLE',
  'CLICKHOUSE',
]

const emptyForm: CreateConnectionRequest = {
  name: '',
  vendor: 'POSTGRESQL',
  host: 'localhost',
  port: 5432,
  databaseName: '',
  schemaName: '',
  description: '',
  username: '',
  password: '',
  enabled: true,
}

export function ConnectionsPage() {
  const qc = useQueryClient()
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState(emptyForm)
  const [error, setError] = useState<string | null>(null)

  const { data: connections = [], isLoading } = useQuery({
    queryKey: ['connections'],
    queryFn: listConnections,
  })

  const createMut = useMutation({
    mutationFn: createConnection,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['connections'] })
      setShowForm(false)
      setForm(emptyForm)
      setError(null)
    },
    onError: (e: Error) =>
      setError(e instanceof ApiClientError ? e.message : e.message),
  })

  const deleteMut = useMutation({
    mutationFn: deleteConnection,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['connections'] }),
  })

  return (
    <div className="space-y-6">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">DB 연결</h1>
          <p className="text-sm text-slate-400">
            /api/v1/conn — JDBC 프로필 CRUD
          </p>
        </div>
        <Button onClick={() => setShowForm((v) => !v)}>
          {showForm ? '닫기' : '+ 연결 등록'}
        </Button>
      </header>

      {error && <Alert type="error">{error}</Alert>}

      {showForm && (
        <Card title="새 연결 등록">
          <form
            className="grid gap-4 md:grid-cols-2"
            onSubmit={(e) => {
              e.preventDefault()
              createMut.mutate(form)
            }}
          >
            <Input
              label="연결명 *"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              required
            />
            <Select
              label="벤더 *"
              value={form.vendor}
              onChange={(e) =>
                setForm({
                  ...form,
                  vendor: e.target.value as DatabaseVendor,
                })
              }
              options={vendors.map((v) => ({ value: v, label: v }))}
            />
            <Input
              label="호스트 *"
              value={form.host}
              onChange={(e) => setForm({ ...form, host: e.target.value })}
              required
            />
            <Input
              label="포트 *"
              type="number"
              value={form.port}
              onChange={(e) =>
                setForm({ ...form, port: Number(e.target.value) })
              }
              required
            />
            <Input
              label="데이터베이스명 *"
              value={form.databaseName}
              onChange={(e) =>
                setForm({ ...form, databaseName: e.target.value })
              }
              required
            />
            <Input
              label="스키마"
              value={form.schemaName ?? ''}
              onChange={(e) =>
                setForm({ ...form, schemaName: e.target.value })
              }
            />
            <Input
              label="계정 *"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              required
            />
            <Input
              label="비밀번호"
              type="password"
              value={form.password ?? ''}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
            />
            <Input
              label="설명"
              className="md:col-span-2"
              value={form.description ?? ''}
              onChange={(e) =>
                setForm({ ...form, description: e.target.value })
              }
            />
            <div className="md:col-span-2">
              <Button type="submit" loading={createMut.isPending}>
                등록 (JDBC 헬스체크)
              </Button>
            </div>
          </form>
        </Card>
      )}

      <Card title={`연결 목록 (${connections.length})`}>
        {isLoading ? (
          <p className="text-slate-400">로딩…</p>
        ) : connections.length === 0 ? (
          <p className="text-slate-500">등록된 연결이 없습니다.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-slate-800 text-slate-500">
                <tr>
                  <th className="py-2 pr-4">ID</th>
                  <th className="py-2 pr-4">이름</th>
                  <th className="py-2 pr-4">벤더</th>
                  <th className="py-2 pr-4">호스트</th>
                  <th className="py-2 pr-4">상태</th>
                  <th className="py-2">액션</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {connections.map((c) => (
                  <tr key={c.connectionId}>
                    <td className="py-3 pr-4 font-mono text-xs text-slate-400">
                      {c.connectionId}
                    </td>
                    <td className="py-3 pr-4 font-medium text-slate-200">
                      {c.name}
                    </td>
                    <td className="py-3 pr-4">{c.vendor}</td>
                    <td className="py-3 pr-4 text-slate-400">
                      {c.host}:{c.port}/{c.databaseName}
                    </td>
                    <td className="py-3 pr-4">
                      <Badge tone={statusTone(c.healthStatus)}>
                        {c.healthStatus}
                      </Badge>
                    </td>
                    <td className="py-3">
                      <Button
                        variant="danger"
                        className="!px-2 !py-1 text-xs"
                        loading={deleteMut.isPending}
                        onClick={() => {
                          if (
                            confirm(
                              `${c.name} 연결을 삭제(soft)하시겠습니까?`,
                            )
                          ) {
                            deleteMut.mutate(c.connectionId)
                          }
                        }}
                      >
                        삭제
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </div>
  )
}
