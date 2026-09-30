import { useQuery } from '@tanstack/react-query'
import { listConnections } from '../api/endpoints'
import { Select } from './ui'

export function ConnectionPicker({
  label,
  value,
  onChange,
  allowEmpty,
  emptyLabel = '선택…',
}: {
  label?: string
  value: string
  onChange: (id: string) => void
  allowEmpty?: boolean
  emptyLabel?: string
}) {
  const { data: connections = [], isLoading } = useQuery({
    queryKey: ['connections'],
    queryFn: listConnections,
  })

  const options = [
    ...(allowEmpty ? [{ value: '', label: emptyLabel }] : []),
    ...connections.map((c) => ({
      value: c.connectionId,
      label: `${c.name} (${c.vendor})`,
    })),
  ]

  return (
    <Select
      label={label}
      value={value}
      disabled={isLoading}
      onChange={(e) => onChange(e.target.value)}
      options={
        options.length > (allowEmpty ? 1 : 0)
          ? options
          : [{ value: '', label: '연결 없음 — 먼저 등록하세요' }]
      }
    />
  )
}
