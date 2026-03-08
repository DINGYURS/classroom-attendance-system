import request from '@/utils/request'
import type { CourseDTO, Result, CourseVO } from '@/types/api'

// 教师创建新课程
export function createCourse(data: CourseDTO) {
  return request.post<any, Result<number>>('/course', data)
}

// 获取教师的课程列表
export function getMyCourses() {
  return request.get<any, Result<CourseVO[]>>('/course/list')
}

// 获取课程详情
export function getCourseDetail(courseId: number) {
  return request.get<any, Result<CourseVO>>(`/course/${courseId}`)
}

// 更新课程信息
export function updateCourse(data: CourseDTO) {
  return request.put<any, Result<any>>('/course', data)
}

// 删除课程
export function deleteCourse(courseId: number) {
  return request.delete<any, Result<any>>(`/course/${courseId}`)
}
