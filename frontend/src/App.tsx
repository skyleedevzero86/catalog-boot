import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Layout } from './components/Layout'
import { DashboardPage } from './pages/DashboardPage'
import { ConnectionsPage } from './pages/ConnectionsPage'
import { MetaPage } from './pages/MetaPage'
import { CategoryPage } from './pages/CategoryPage'
import { MigrationPage } from './pages/MigrationPage'
import { ExtractPage } from './pages/ExtractPage'
import { FileLoadPage } from './pages/FileLoadPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 0,
    },
  },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<DashboardPage />} />
            <Route path="connections" element={<ConnectionsPage />} />
            <Route path="meta" element={<MetaPage />} />
            <Route path="category" element={<CategoryPage />} />
            <Route path="migration" element={<MigrationPage />} />
            <Route path="extract" element={<ExtractPage />} />
            <Route path="file-load" element={<FileLoadPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
