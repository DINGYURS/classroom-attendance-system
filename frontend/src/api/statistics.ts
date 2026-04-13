import request from '@/utils/request'
import type { Result, StatisticsDashboardQuery, StatisticsDashboardVO } from '@/types/api'

/**
 * 获取教师端数据中心聚合统计
 */
export function getStatisticsDashboard(params: StatisticsDashboardQuery) {
  return request.get<any, Result<StatisticsDashboardVO>>('/statistics/dashboard', {
    params
  })
}
