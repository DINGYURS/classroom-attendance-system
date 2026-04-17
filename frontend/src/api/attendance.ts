import axios from 'axios'
import request from '@/utils/request'
import { useAuthStore } from '@/stores/auth'
import type {
  AttendanceDetectionAssignDTO,
  AttendanceDetectionIgnoreDTO,
  AttendanceSessionAnnotationVO,
  AttendanceArchiveOptionsVO,
  AttendanceArchivePageVO,
  AttendanceArchiveQueryDTO,
  AttendanceArchiveSessionDetailVO,
  AttendanceSessionVO,
  AttendanceStartDTO,
  AttendanceUpdateDTO,
  FaceRecognitionDTO,
  RecognitionResultVO,
  Result,
  SessionRecordVO
} from '@/types/api'

export function uploadAttendanceImage(formData: FormData) {
  return request.post<any, Result<string>>('/file/upload/attendance', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 30000
  })
}

export function startAttendance(data: AttendanceStartDTO) {
  return request.post<any, Result<number>>('/attendance/start', data)
}

export function recognizeAttendance(data: FaceRecognitionDTO) {
  return request.post<any, Result<RecognitionResultVO[]>>('/attendance/recognize', data, {
    timeout: 60000
  })
}

export function getAttendanceSessionDetail(sessionId: number) {
  return request.get<any, Result<AttendanceSessionVO>>(`/attendance/session/${sessionId}`)
}

export function getAttendanceSessionAnnotations(sessionId: number) {
  return request.get<any, Result<AttendanceSessionAnnotationVO>>(`/attendance/session/${sessionId}/annotations`)
}

export function ignoreAttendanceDetection(detectionId: number, data: AttendanceDetectionIgnoreDTO) {
  return request.put<any, Result<void>>(`/attendance/detection/${detectionId}/ignore`, data)
}

export function assignAttendanceDetection(detectionId: number, data: AttendanceDetectionAssignDTO) {
  return request.put<any, Result<void>>(`/attendance/detection/${detectionId}/assign`, data)
}

export function getAttendanceSessionRecords(sessionId: number) {
  return request.get<any, Result<SessionRecordVO[]>>(`/attendance/session/${sessionId}/records`)
}

export function updateAttendanceStatus(data: AttendanceUpdateDTO) {
  return request.put<any, Result<void>>('/attendance/status', data)
}

export function endAttendance(sessionId: number) {
  return request.post<any, Result<void>>(`/attendance/end/${sessionId}`)
}

export function getAttendanceArchiveOptions(courseId?: number | string) {
  return request.get<any, Result<AttendanceArchiveOptionsVO>>('/attendance/archive/options', {
    params: {
      courseId
    },
    timeout: 30000
  })
}

export function getAttendanceArchivePage(params: AttendanceArchiveQueryDTO) {
  return request.get<any, Result<AttendanceArchivePageVO>>('/attendance/archive/page', {
    params,
    timeout: 30000
  })
}

export function getAttendanceArchiveSessionDetail(sessionId: number) {
  return request.get<any, Result<AttendanceArchiveSessionDetailVO>>(`/attendance/archive/session/${sessionId}`, {
    timeout: 30000
  })
}

function downloadExcel(url: string, params?: object) {
  const authStore = useAuthStore()
  return axios.get(url, {
    baseURL: '/api',
    params,
    responseType: 'blob',
    timeout: 30000,
    headers: {
      token: authStore.userInfo.token
    }
  })
}

export function exportAttendanceArchive(params: AttendanceArchiveQueryDTO) {
  return downloadExcel('/excel/export/attendance/archive', params)
}

export function exportAttendanceArchiveSummary(params: AttendanceArchiveQueryDTO) {
  return downloadExcel('/excel/export/attendance/archive/summary', params)
}

export function exportAttendanceSession(sessionId: number) {
  return downloadExcel(`/excel/export/attendance/session/${sessionId}`)
}
