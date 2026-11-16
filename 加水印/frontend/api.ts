import { api } from '@/services/api'
import type { WatermarkEnterprise, WatermarkEnterpriseImportResult } from './types'

export const watermarkEnterpriseApi = {
  list: () => api.get<WatermarkEnterprise[]>('/watermark-enterprises'),
  importWorkbook: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return api.upload<WatermarkEnterpriseImportResult>('/watermark-enterprises/import', form)
  },
}
