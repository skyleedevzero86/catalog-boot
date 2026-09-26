import { useState } from 'react'
import { Link, Route, Routes } from 'react-router-dom'
import reactLogo from './assets/react.svg'
import viteLogo from './assets/vite.svg'

function Home() {
  const [count, setCount] = useState(0)

  return (
    <main className="mx-auto flex min-h-svh max-w-3xl flex-col items-center justify-center gap-6 px-4 text-center">
      <div className="flex items-center gap-6">
        <a href="https://vite.dev" target="_blank" rel="noreferrer">
          <img src={viteLogo} className="h-16 w-16" alt="Vite logo" />
        </a>
        <a href="https://react.dev" target="_blank" rel="noreferrer">
          <img src={reactLogo} className="h-16 w-16" alt="React logo" />
        </a>
      </div>
      <h1 className="text-4xl font-semibold tracking-tight">DataBridge Catalog</h1>
      <p className="text-neutral-600">
        Edit <code className="rounded bg-neutral-100 px-1.5 py-0.5">src/App.tsx</code> and save to test HMR
      </p>
      <button
        type="button"
        className="rounded-lg border border-neutral-300 px-4 py-2 hover:bg-neutral-50"
        onClick={() => setCount((c) => c + 1)}
      >
        Count is {count}
      </button>
      <Link className="text-sm text-neutral-500 underline" to="/about">
        About
      </Link>
    </main>
  )
}

function About() {
  return (
    <main className="mx-auto flex min-h-svh max-w-3xl flex-col items-center justify-center gap-4 px-4 text-center">
      <h1 className="text-3xl font-semibold">About</h1>
      <p className="text-neutral-600">Frontend scaffold with React Query, Router, and Tailwind.</p>
      <Link className="text-sm text-neutral-500 underline" to="/">
        Home
      </Link>
    </main>
  )
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/about" element={<About />} />
    </Routes>
  )
}
