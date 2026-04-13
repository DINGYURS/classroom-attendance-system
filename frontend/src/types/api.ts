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
  role: number
  avatarUrl: string
  token: string
  adminClass?: string
}

// 用户注册
export interface UserRegisterDTO {
  username?: string
  password?: string
  realName?: string
  role?: number
  adminClass?: string
  gender?: number
}

// 教师信息修改
export interface TeacherUpdateDTO {
  jobNumber?: string
  password?: string
  realName?: string
}

// 学生信息修改
export interface StudentUpdateDTO {
  username?: string
  password?: string
  realName?: string
}

export interface StudentVO {
  username: string
  realName: string
  adminClass: string
  avatarUrl: string
}

export interface AttendanceRecordVO {
  recordId: number
  sessionId?: number
  courseId?: number
  courseName: string
  semester?: string
  teachingClass?: string
  status: number
  statusText: string
  attendanceTime?: string
  similarityScore?: string
  updateType?: number
  manualModified?: boolean
}

export interface AttendanceStartDTO {
  courseId: number
  lateThreshold?: number
}

export interface FaceRecognitionDTO {
  sessionId: number
  imageKeys: string[]
}

export interface AttendanceUpdateDTO {
  recordId: number
  status: number
}

export interface AttendanceSessionVO {
  sessionId: number
  courseId: number
  courseName: string
  startTime?: string
  endTime?: string
  status?: number
  presentCount: number
  totalCount: number
}

export interface RecognitionResultVO {
  studentId?: number
  studentNumber?: string
  realName?: string
  similarity: number
  matched: boolean
  status?: number
}

export interface SessionRecordVO {
  recordId: number
  studentId: number
  studentNumber: string
  realName: string
  status: number
  statusText: string
  similarityScore?: number | string
}

// 课程 DTO
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
  avatarUrl: string
}

export interface StatisticsDashboardQuery {
  semester?: string
  courseId?: number
  adminClass?: string
  startDate?: string
  endDate?: string
  anomalyIncludeLeave?: boolean
}

export interface StatisticsOptionVO {
  label: string
  value: string
}

export interface StatisticsSummaryVO {
  totalCourses: number
  coveredStudents: number
  totalSessions: number
  avgAttendanceRate: number
  totalAnomalies: number
  faceEntryRate: number
}

export interface StatisticsStatusItemVO {
  name: string
  value: number
}

export interface StatisticsTrendItemVO {
  label: string
  attendanceRate: number
}

export interface StatisticsCourseRateVO {
  courseName: string
  attendanceRate: number
}

export interface StatisticsClassStatusVO {
  adminClass: string
  presentCount: number
  lateCount: number
  absentCount: number
  leaveCount: number
}

export interface StatisticsStudentAnomalyVO {
  studentName: string
  adminClass: string
  anomalyCount: number
}

export interface StatisticsCorrectionVO {
  courseName: string
  autoCount: number
  manualCount: number
}

export interface StatisticsDashboardVO {
  semesterOptions: StatisticsOptionVO[]
  courseOptions: StatisticsOptionVO[]
  classOptions: StatisticsOptionVO[]
  summaryData: StatisticsSummaryVO
  statusDistribution: StatisticsStatusItemVO[]
  attendanceTrend: StatisticsTrendItemVO[]
  courseAttendanceComparison: StatisticsCourseRateVO[]
  classStatusComposition: StatisticsClassStatusVO[]
  studentAnomalyRanking: StatisticsStudentAnomalyVO[]
  correctionAnalysis: StatisticsCorrectionVO[]
}
