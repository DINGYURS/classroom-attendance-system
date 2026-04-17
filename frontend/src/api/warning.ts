import request from '@/utils/request'
import type {
  Result,
  WarningCenterPageVO,
  WarningDetailVO,
  WarningNoticeSendDTO,
  WarningNoticeVO,
  WarningOptionsVO,
  WarningQueryDTO
} from '@/types/api'

/**
 * 获取预警中心筛选项
 */
export function getWarningOptions(courseId?: number) {
  return request.get<any, Result<WarningOptionsVO>>('/warning/options', {
    params: { courseId }
  })
}

/**
 * 获取预警中心分页结果
 */
export function getWarningPage(params: WarningQueryDTO) {
  return request.get<any, Result<WarningCenterPageVO>>('/warning/page', {
    params
  })
}

/**
 * 获取预警中心学生详情
 */
export function getWarningDetail(courseId: number, studentId: number, params?: WarningQueryDTO) {
  return request.get<any, Result<WarningDetailVO>>('/warning/detail', {
    params: {
      courseId,
      studentId,
      startDate: params?.startDate,
      endDate: params?.endDate
    }
  })
}

/**
 * 获取通知记录列表
 */
export function getWarningHistory(params: WarningQueryDTO) {
  return request.get<any, Result<WarningNoticeVO[]>>('/warning/history', {
    params
  })
}

/**
 * 发送考勤提醒
 */
export function sendWarningNotice(data: WarningNoticeSendDTO) {
  return request.post<any, Result<void>>('/warning/notice', data)
}
