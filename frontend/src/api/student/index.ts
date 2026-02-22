import request from '@/utils/request'
import type { StudentUpdateDTO, Result, StudentVO, AttendanceRecordVO } from '@/types/api'

/**
 * 获取学生个人信息
 */
export function getStudentInfo() {
  return request.get<any, Result<StudentVO>>('/student/info')
}

/**
 * 修改学生个人信息
 */
export function updateStudentInfo(data: StudentUpdateDTO) {
  return request.put<any, Result<void>>('/user/student/profile', data)
}

/**
 * 获取考勤记录
 */
export function getAttendanceRecords() {
  return request.get<any, Result<AttendanceRecordVO[]>>('/student/attendance')
}

/**
 * 学生上传/更新人脸照片
 */
export function uploadFaceImage(formData: FormData) {
  return request.post('/student/face/register', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
