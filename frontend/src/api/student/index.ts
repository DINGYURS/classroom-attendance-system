import request from '@/utils/request'

/**
 * 学生上传/更新人脸照片
 */
export function uploadFaceImage(formData: FormData) {
  return request.post('/file/upload/face', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
