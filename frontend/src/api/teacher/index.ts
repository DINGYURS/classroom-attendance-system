import axios from 'axios'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import type { TeacherUpdateDTO, Result, PageResult, TeacherStudentPageQuery, TeacherStudentTableVO } from '@/types/api'

/**
 * 修改教师个人信息
 */
export function updateTeacherProfile(data: TeacherUpdateDTO) {
  return request.put('/user/teacher/profile', data)
}

/**
 * 教师端分页查询学生管理表格数据
 */
export function getTeacherStudentPage(params: TeacherStudentPageQuery) {
  return request.get<any, Result<PageResult<TeacherStudentTableVO>>>('/teacher/student/page', {
    params
  })
}

/**
 * 按模板导入教师端学生名单
 */
export function importTeacherStudentList(formData: FormData) {
  return request.post<any, Result<string>>('/excel/import/students', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 30000
  })
}

/**
 * 导出教师端学生名单
 */
export function downloadTeacherStudentList(params: TeacherStudentPageQuery) {
  const authStore = useAuthStore()
  return axios.get('/api/excel/export/students', {
    params: {
      keyword: params.keyword
    },
    responseType: 'blob',
    timeout: 30000,
    headers: {
      token: authStore.userInfo.token
    }
  })
}