import request from '@/utils/request'
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
