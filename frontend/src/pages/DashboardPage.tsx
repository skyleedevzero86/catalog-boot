import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { listConnections, listMigrationJobs } from '../api/endpoints'
import { Card, Badge, statusTone } from '../components/ui'

const links = [
  {
    to: '/connections',
    title: 'DB 연결',
    desc: 'JDBC 프로필 등록·헬스체크',
  },
  {
    to: '/meta',
    title: '메타데이터',
    desc: 'introspection 동기화·테이블 조회',
  },
  {
    to: '/category',
    title: '카테고리',
    desc: '테이블 분류·매핑',
  },
  {
    to: '/migration',
    title: '마이그레이션',
    desc: 'DDL 미리보기·적재·job 모니터링',
  },
  {
    to: '/extract',
    title: '추출',
    desc: 'prepare → CSV/Parquet → cleanup',
  },
]

export function DashboardPage() {
  const { data: connections = [] } = useQuery({
    queryKey: ['connections'],
    queryFn: listConnections,
  })
  const { data: jobs = [] } = useQuery({
    queryKey: ['migration-jobs'],
    queryFn: () => listMigrationJobs(10),
  })

  const running = jobs.filter((j) => j.status === 'RUNNING').length

  return (
    <div className="space-y-8">
      <header>
        <h1 className="text-2xl font-bold text-white">대시보드</h1>
        <p className="mt-1 text-slate-400">
          CDW Catalog Control Plane — Spring Boot API (:8081) 프론트
        </p>
      </header>

      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <p className="text-sm text-slate-400">등록 연결</p>
          <p className="mt-2 text-3xl font-bold text-white">
            {connections.length}
          </p>
        </Card>
        <Card>
          <p className="text-sm text-slate-400">최근 마이그레이션 job</p>
          <p className="mt-2 text-3xl font-bold text-white">{jobs.length}</p>
        </Card>
        <Card>
          <p className="text-sm text-slate-400">실행 중 job</p>
          <p className="mt-2 text-3xl font-bold text-indigo-400">{running}</p>
        </Card>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {links.map((item) => (
          <Link
            key={item.to}
            to={item.to}
            className="rounded-xl border border-slate-800 bg-slate-900/40 p-5 transition hover:border-indigo-700 hover:bg-slate-900"
          >
            <h3 className="font-semibold text-indigo-300">{item.title}</h3>
            <p className="mt-2 text-sm text-slate-400">{item.desc}</p>
          </Link>
        ))}
      </div>

      {jobs.length > 0 && (
        <Card title="최근 마이그레이션">
          <ul className="divide-y divide-slate-800">
            {jobs.slice(0, 5).map((job) => (
              <li
                key={job.jobId}
                className="flex items-center justify-between py-3 text-sm"
              >
                <span className="font-mono text-slate-300">{job.jobId}</span>
                <Badge tone={statusTone(job.status)}>{job.status}</Badge>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  )
}
