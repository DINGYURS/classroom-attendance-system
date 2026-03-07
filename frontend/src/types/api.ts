// Common Result
export interface Result<T> {
  code: number
  message: string
  data: T
}

// 用户登录
export interface UserLoginDTO {
  username?: string
  password?: string
}

export interface UserLoginVO {
  userId: number
  username: string
  realName: string
  role: number // 1-Teacher, 2-Student
  avatarUrl: string
  token: string
  adminClass?: string
}

// 用户注册
export interface UserRegisterDTO {
  username?: string
  password?: string
  realName?: string
  role?: number // 1-Teacher, 2-Student
  adminClass?: string
  gender?: number // 1-Male, 2-Female
}

// 教师信息修改
export interface TeacherUpdateDTO {
  jobNumber?: string
  password?: string
  realName?: string
}

// 学生信息修改
export interface StudentUpdateDTO {
  username?: string // 学号
  password?: string // 新密码
  realName?: string // 姓名
}

export interface StudentVO {
  username: string
  realName: string
  adminClass: string
  avatarUrl: string
}

export interface AttendanceRecordVO {
  id: number
  courseName: string
  status: number
  createTime: string
}

// 课程对应 DTO
export interface CourseDTO {
  courseId?: number
  courseName: string
  semester: string
  description?: string
}

export interface CourseVO {
  courseId: number
  courseName: string
  semester: string
  description: string
  studentCount: number
  classes?: string[]
  attendanceRate?: number
}

export interface PageResult<T> {
  total: number
  records: T[]
}

export interface TeacherStudentPageQuery {
  keyword?: string
  currentPage?: number
  pageSize?: number
}

export interface TeacherStudentTableVO {
  id: number
  courseId: number
  userId: number
  courseName: string
  semester: string
  studentId: string
  realName: string
  gender: string
  className: string
}
