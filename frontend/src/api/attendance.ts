import request from '@/utils/request'
import type {
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

export function getAttendanceSessionRecords(sessionId: number) {
  return request.get<any, Result<SessionRecordVO[]>>(`/attendance/session/${sessionId}/records`)
}

export function updateAttendanceStatus(data: AttendanceUpdateDTO) {
  return request.put<any, Result<void>>('/attendance/status', data)
}

export function endAttendance(sessionId: number) {
  return request.post<any, Result<void>>(`/attendance/end/${sessionId}`)
}
