import request from '@/utils/request'
import type { TeacherUpdateDTO } from '@/types/api'

/**
 * 修改教师个人信息
 */
export function updateTeacherProfile(data: TeacherUpdateDTO) {
  return request.put('/user/teacher/profile', data)
}
