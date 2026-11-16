import { Stamp } from 'lucide-react'
import { PhotoWatermarkPage } from './pages/PhotoWatermarkPage'

export const PHOTO_WATERMARK_MODULE = 'PHOTO_WATERMARK'

export const photoWatermarkNav = {
  name: '照片加水印',
  href: '/photo-watermark',
  icon: Stamp,
  adminOnly: false,
  regularOnly: true,
}

export function photoWatermarkRoute() {
  return <PhotoWatermarkPage />
}
