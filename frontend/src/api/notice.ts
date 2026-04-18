import request from '@/utils/request'
import type { Result } from '@/types/api'

type StudentNoticeResponseVO = {
  noticeId: number
  title: string
  content: string
  courseId?: number
  courseName: string
  sendTime: string
  isRead: boolean
  absentCount: number
}

export interface NoticeVO {
  id: number
  title: string
  content: string
  courseId?: number
  courseName: string
  sendTime: string
  readStatus: number
  absenceCountCount: number
}

const mapNotice = (item: StudentNoticeResponseVO): NoticeVO => ({
  id: item.noticeId,
  title: item.title,
  content: item.content,
  courseId: item.courseId,
  courseName: item.courseName,
  sendTime: item.sendTime,
  readStatus: item.isRead ? 1 : 0,
  absenceCountCount: item.absentCount ?? 0
})

/**
 * 获取学生通知列表
 */
export async function getNotices(params?: { status?: string | number }) {
  const res = await request.get<any, Result<StudentNoticeResponseVO[]>>('/student/notices', {
    params: {
      status: params?.status === undefined || params.status === ''
        ? undefined
        : Number(params.status)
    }
  })

  return {
    ...res,
    data: (res.data || []).map(mapNotice)
  }
}

/**
 * 获取未读通知数量
 */
export function getUnreadCount() {
  return request.get<any, Result<number>>('/student/notices/unread-count')
}

/**
 * 标记通知为已知晓
 */
export function markNoticeRead(id: number) {
  return request.put<any, Result<void>>(`/student/notice/${id}/read`)
}
