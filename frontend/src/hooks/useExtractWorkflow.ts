import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useState } from 'react'
import {
  cleanupDataset,
  exportDataset,
  getDataset,
  prepareExtract,
  type ExportDatasetRequest,
  type PrepareExtractRequest,
} from '../api/extract'
import { toErrorMessage } from '../api/client'

export function useExtractWorkflow() {
  const queryClient = useQueryClient()
  const [datasetId, setDatasetId] = useState('')
  const [feedback, setFeedback] = useState<{ error: string | null; message: string | null }>({
    error: null,
    message: null,
  })

  const manifestQuery = useQuery({
    queryKey: ['extract-dataset', datasetId],
    queryFn: () => getDataset(datasetId),
    enabled: !!datasetId,
  })

  const handleError = useCallback((error: Error) => {
    setFeedback({
      error: toErrorMessage(error),
      message: null,
    })
  }, [])

  const prepareMutation = useMutation({
    mutationFn: (body: PrepareExtractRequest) => prepareExtract(body),
    onSuccess: (result) => {
      setDatasetId(result.datasetId)
      setFeedback({
        error: null,
        message: `Prepare 완료 — ${result.rowCount}행 (중복 제거 ${result.duplicateCount})`,
      })
      queryClient.invalidateQueries({ queryKey: ['extract-dataset', result.datasetId] })
    },
    onError: handleError,
  })

  const exportMutation = useMutation({
    mutationFn: (body: ExportDatasetRequest) => exportDataset(datasetId, body),
    onSuccess: (result) => {
      setFeedback({
        error: null,
        message: `Export 완료 — 파일 ${result.filePaths.length}개`,
      })
      queryClient.invalidateQueries({ queryKey: ['extract-dataset', datasetId] })
    },
    onError: handleError,
  })

  const cleanupMutation = useMutation({
    mutationFn: (dropManifest: boolean) => cleanupDataset(datasetId, dropManifest),
    onSuccess: () => {
      setFeedback({ error: null, message: 'Cleanup 완료' })
      queryClient.invalidateQueries({ queryKey: ['extract-dataset', datasetId] })
    },
    onError: handleError,
  })

  return {
    datasetId,
    setDatasetId,
    manifest: manifestQuery.data,
    manifestError: manifestQuery.isError
      ? toErrorMessage(manifestQuery.error)
      : null,
    refetchManifest: manifestQuery.refetch,
    feedback,
    prepareMutation,
    exportMutation,
    cleanupMutation,
  }
}
