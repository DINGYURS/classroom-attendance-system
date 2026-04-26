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
  rememberMe?: boolean
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
  faceLocation?: string
  manualModified?: boolean
}

export interface AttendanceSessionImageVO {
  imageIndex: number
  viewKey: string
  objectKey: string
  imageUrl: string
}

export interface AttendanceDetectionVO {
  detectionId: number
  imageIndex: number
  viewKey: string
  faceIndex: number
  bbox: string
  detectionScore?: string
  matched: boolean
  ignored?: boolean
  ignoreReason?: string
  studentId?: number
  recordId?: number
  studentNumber?: string
  realName?: string
  similarityScore?: string
  finalStatus?: number
  finalStatusText?: string
  manualModified?: boolean
}

export interface AttendanceSessionAnnotationVO {
  sessionId: number
  images: AttendanceSessionImageVO[]
  detections: AttendanceDetectionVO[]
}

export interface AttendanceDetectionIgnoreDTO {
  ignoreReason?: string
}

export interface AttendanceDetectionAssignDTO {
  studentId: number
}

export interface AttendanceArchiveQueryDTO {
  courseId?: number | string
  adminClass?: string
  startDate?: string
  endDate?: string
  status?: number
  type?: number
  keyword?: string
  currentPage?: number
  pageSize?: number
}

export interface AttendanceArchiveOptionsVO {
  courseOptions: StatisticsOptionVO[]
  classOptions: StatisticsOptionVO[]
}

export interface AttendanceArchiveSummaryVO {
  totalSessions: number
  expectedTotal: number
  actualTotal: number
  absentTotal: number
  lateTotal: number
  avgRate: string
}

export interface AttendanceArchiveSessionVO {
  id: number
  courseName: string
  className: string
  sessionTime: string
  expectedCount: number
  actualCount: number
  absentCount: number
  lateCount: number
  leaveCount: number
  attendanceRate: string
  type: string
}

export interface AttendanceArchivePageVO {
  summary: AttendanceArchiveSummaryVO
  pageData: PageResult<AttendanceArchiveSessionVO>
}

export interface AttendanceArchiveDetailVO {
  id: number
  studentId: string
  studentName: string
  className: string
  status: string
  type: string
  similarityScore?: string
}

export interface AttendanceArchiveSessionDetailVO {
  sessionId: number
  courseName: string
  className: string
  sessionTime: string
  expectedCount: number
  actualCount: number
  absentCount: number
  lateCount: number
  leaveCount: number
  attendanceRate: string
  type: string
  detailList: AttendanceArchiveDetailVO[]
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

export interface CourseStudentVO {
  userId: number
  studentNumber: string
  realName: string
  adminClass: string
  gender?: number
  hasFaceFeature?: boolean
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

export interface WarningQueryDTO {
  courseId?: number
  adminClass?: string
  startDate?: string
  endDate?: string
  keyword?: string
  currentPage?: number
  pageSize?: number
}

export interface WarningSummaryVO {
  highAbsenceCount: number
  todayNotifyCount: number
  unreadNotifyCount: number
  maxAbsenceCount: number
}

export interface WarningRankingVO {
  courseId: number
  userId: number
  studentId: string
  studentName: string
  className: string
  courseName: string
  absenceCount: number
  lastAbsenceTime?: string
  lastNotifyTime?: string
  notifyCount: number
  hasUnread: boolean
}

export interface WarningTimelineVO {
  id: number
  date: string
  course: string
  status: number
  statusText: string
  statusType: 'success' | 'warning' | 'danger' | 'info'
}

export interface WarningDetailVO {
  userId: number
  studentId: string
  studentName: string
  className: string
  courseId: number
  courseName: string
  absenceCount: number
  lastAbsenceTime?: string
  lastNotifyTime?: string
  notifyCount: number
  hasUnread: boolean
  timeline: WarningTimelineVO[]
}

export interface WarningNoticeVO {
  id: number
  studentName: string
  studentId: string
  courseId?: number
  courseName: string
  absenceSnapshot: number
  title: string
  sentTime: string
  isRead: boolean
}

export interface WarningOptionsVO {
  courseOptions: StatisticsOptionVO[]
  classOptions: StatisticsOptionVO[]
}

export interface WarningCenterPageVO {
  summary: WarningSummaryVO
  pageData: PageResult<WarningRankingVO>
}

export interface WarningNoticeSendDTO {
  studentId: number
  courseId: number
  absentCount: number
  title: string
  content: string
}
