import { NavLink, Outlet } from 'react-router-dom'
import { getUserId, setUserId } from '../api/client'
import { useState } from 'react'

const nav = [
  { to: '/', label: '대시보드', end: true },
  { to: '/connections', label: 'DB 연결' },
  { to: '/meta', label: '메타데이터' },
  { to: '/category', label: '카테고리' },
  { to: '/migration', label: '마이그레이션' },
  { to: '/extract', label: '추출' },
  { to: '/file-load', label: '파일 적재' },
]

export function Layout() {
  const [userId, setLocalUserId] = useState(getUserId())

  return (
    <div className="flex min-h-screen">
      <aside className="flex w-60 shrink-0 flex-col border-r border-slate-800 bg-slate-900">
        <div className="border-b border-slate-800 px-5 py-6">
          <p className="text-xs font-medium uppercase tracking-wider text-indigo-400">
            CDW Catalog
          </p>
          <h1 className="mt-1 text-lg font-bold text-white">Control UI</h1>
          <p className="mt-1 text-xs text-slate-500">:8081 API 연동</p>
        </div>
        <nav className="flex-1 space-y-1 p-3">
          {nav.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) =>
                `block rounded-lg px-3 py-2 text-sm font-medium transition ${
                  isActive
                    ? 'bg-indigo-600/20 text-indigo-300'
                    : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-slate-800 p-4">
          <label className="block text-xs text-slate-500">userId 헤더</label>
          <input
            className="mt-1 w-full rounded border border-slate-700 bg-slate-950 px-2 py-1 text-xs text-slate-200"
            value={userId}
            onChange={(e) => {
              setLocalUserId(e.target.value)
              setUserId(e.target.value)
            }}
          />
        </div>
      </aside>
      <main className="flex-1 overflow-auto p-8">
        <Outlet />
      </main>
    </div>
  )
}
