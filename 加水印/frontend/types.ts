export interface WatermarkEnterprise {
  id: string
  name: string
  city: string
  county: string
  visit1At?: string | null
  visit2At?: string | null
  visit3At?: string | null
}

export interface WatermarkEnterpriseImportResult {
  inserted: number
  updated: number
  unchanged: number
  skipped: number
}
