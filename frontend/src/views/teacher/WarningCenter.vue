<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Search,
  Refresh,
  Message,
  Warning,
  Bell,
  TrendCharts
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getWarningDetail,
  getWarningHistory,
  getWarningOptions,
  getWarningPage,
  sendWarningNotice
} from '@/api/warning'
import type {
  StatisticsOptionVO,
  WarningDetailVO,
  WarningNoticeVO,
  WarningQueryDTO,
  WarningRankingVO,
  WarningSummaryVO
} from '@/types/api'

type NotifyTarget = Pick<WarningRankingVO, 'courseId' | 'userId' | 'studentId' | 'studentName' | 'courseName' | 'absenceCount'>
  | Pick<WarningDetailVO, 'courseId' | 'userId' | 'studentId' | 'studentName' | 'courseName' | 'absenceCount'>

const activeTab = ref('ranking')
const pageLoading = ref(false)
const detailLoading = ref(false)
const historyLoading = ref(false)
const sending = ref(false)

const courseOptions = ref<StatisticsOptionVO[]>([])
const classOptions = ref<StatisticsOptionVO[]>([])
const rankingList = ref<WarningRankingVO[]>([])
const notificationHistory = ref<WarningNoticeVO[]>([])
const selectedStudent = ref<WarningDetailVO | null>(null)

const summary = reactive<WarningSummaryVO>({
  highAbsenceCount: 0,
  todayNotifyCount: 0,
  unreadNotifyCount: 0,
  maxAbsenceCount: 0
})

const searchQuery = reactive({
  courseId: undefined as number | undefined,
  adminClass: '',
  dateRange: [] as string[],
  keyword: ''
})

const currentPage = ref(1)
const pageSize = ref(10)
const totalRows = ref(0)

const dialogVisible = ref(false)
const currentNotifyTarget = ref<NotifyTarget | null>(null)
const notifyForm = reactive({
  title: '考勤异常提醒',
  content: ''
})

const queryParams = computed<WarningQueryDTO>(() => ({
  courseId: searchQuery.courseId,
  adminClass: searchQuery.adminClass || undefined,
  startDate: searchQuery.dateRange[0],
  endDate: searchQuery.dateRange[1],
  keyword: searchQuery.keyword.trim() || undefined,
  currentPage: currentPage.value,
  pageSize: pageSize.value
}))

const getStatusType = (status: number) => {
  switch (status) {
    case 0:
      return 'danger'
    case 1:
      return 'success'
    case 2:
      return 'warning'
    case 3:
      return 'info'
    default:
      return 'info'
  }
}

const formatDateTime = (value?: string | null) => {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ')
}

const loadWarningOptions = async () => {
  const res = await getWarningOptions(searchQuery.courseId)
  courseOptions.value = res.data.courseOptions || []
  classOptions.value = res.data.classOptions || []
}

const loadWarningDetail = async (row?: WarningRankingVO | null) => {
  if (!row) {
    selectedStudent.value = null
    return
  }

  detailLoading.value = true
  try {
    const res = await getWarningDetail(row.courseId, row.userId, queryParams.value)
    selectedStudent.value = res.data
  } catch (error) {
    console.error(error)
    selectedStudent.value = null
  } finally {
    detailLoading.value = false
  }
}

const loadWarningPage = async () => {
  pageLoading.value = true
  try {
    const res = await getWarningPage(queryParams.value)
    rankingList.value = res.data.pageData.records || []
    totalRows.value = res.data.pageData.total || 0

    summary.highAbsenceCount = res.data.summary.highAbsenceCount || 0
    summary.todayNotifyCount = res.data.summary.todayNotifyCount || 0
    summary.unreadNotifyCount = res.data.summary.unreadNotifyCount || 0
    summary.maxAbsenceCount = res.data.summary.maxAbsenceCount || 0

    await loadWarningDetail(rankingList.value[0] || null)
  } catch (error) {
    console.error(error)
    rankingList.value = []
    totalRows.value = 0
    selectedStudent.value = null
  } finally {
    pageLoading.value = false
  }
}

const loadNoticeHistory = async () => {
  historyLoading.value = true
  try {
    const res = await getWarningHistory(queryParams.value)
    notificationHistory.value = res.data || []
  } catch (error) {
    console.error(error)
    notificationHistory.value = []
  } finally {
    historyLoading.value = false
  }
}

const refreshData = async () => {
  await loadWarningOptions()
  await Promise.all([
    loadWarningPage(),
    loadNoticeHistory()
  ])
}

const handleSearch = async () => {
  currentPage.value = 1
  await refreshData()
}

const handleReset = async () => {
  searchQuery.courseId = undefined
  searchQuery.adminClass = ''
  searchQuery.dateRange = []
  searchQuery.keyword = ''
  currentPage.value = 1
  await refreshData()
}

const handleRowClick = async (row: WarningRankingVO) => {
  await loadWarningDetail(row)
}

const handleViewDetails = async (row: WarningRankingVO) => {
  await loadWarningDetail(row)
}

const handleSizeChange = async (val: number) => {
  pageSize.value = val
  currentPage.value = 1
  await loadWarningPage()
}

const handleCurrentChange = async (val: number) => {
  currentPage.value = val
  await loadWarningPage()
}

const openNotifyDialog = (student: NotifyTarget) => {
  currentNotifyTarget.value = student
  notifyForm.title = '考勤异常提醒'
  notifyForm.content = `同学你好，你在《${student.courseName}》课程中已累计缺勤 ${student.absenceCount} 次，请及时关注后续考勤情况。`
  dialogVisible.value = true
}

const handleSendNotify = async () => {
  if (!currentNotifyTarget.value) {
    ElMessage.warning('请选择需要提醒的学生')
    return
  }
  if (!notifyForm.title.trim() || !notifyForm.content.trim()) {
    ElMessage.warning('标题和内容均不能为空')
    return
  }

  sending.value = true
  try {
    await sendWarningNotice({
      studentId: currentNotifyTarget.value.userId,
      courseId: currentNotifyTarget.value.courseId,
      absentCount: currentNotifyTarget.value.absenceCount,
      title: notifyForm.title.trim(),
      content: notifyForm.content.trim()
    })
    ElMessage.success('提醒发送成功')
    dialogVisible.value = false
    await Promise.all([
      loadWarningPage(),
      loadNoticeHistory()
    ])
  } catch (error) {
    console.error(error)
  } finally {
    sending.value = false
  }
}

onMounted(async () => {
  await refreshData()
})
</script>

<template>
  <div class="warning-center flex flex-col gap-5 h-full min-h-[700px]">
    <div class="flex items-center justify-between shrink-0">
      <div class="-mb-1">
        <h1 class="text-2xl font-bold text-gray-900">预警中心</h1>
        <p class="text-gray-500 mt-1">在这里查看缺勤较多学生并发送考勤异常提醒。</p>
      </div>
    </div>

    <div class="grid grid-cols-2 lg:grid-cols-4 gap-4 shrink-0">
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 transition-all hover:shadow-md hover:border-red-200">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-8 h-8 rounded-full bg-red-50 text-red-500 flex items-center justify-center">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="text-sm font-medium text-gray-500">高缺勤学生数 <span class="text-xs text-gray-400 font-normal">(&ge;3次)</span></div>
        </div>
        <div class="text-2xl font-bold text-gray-800 ml-1">{{ summary.highAbsenceCount }}</div>
      </div>

      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 transition-all hover:shadow-md hover:border-blue-200">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-8 h-8 rounded-full bg-blue-50 text-blue-500 flex items-center justify-center">
            <el-icon><Bell /></el-icon>
          </div>
          <div class="text-sm font-medium text-gray-500">今日已通知</div>
        </div>
        <div class="text-2xl font-bold text-gray-800 ml-1">{{ summary.todayNotifyCount }}</div>
      </div>

      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 transition-all hover:shadow-md hover:border-orange-200">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-8 h-8 rounded-full bg-orange-50 text-orange-500 flex items-center justify-center">
            <el-icon><Message /></el-icon>
          </div>
          <div class="text-sm font-medium text-gray-500">未读提醒数</div>
        </div>
        <div class="text-2xl font-bold text-orange-500 ml-1">{{ summary.unreadNotifyCount }}</div>
      </div>

      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 transition-all hover:shadow-md hover:border-indigo-200">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-8 h-8 rounded-full bg-indigo-50 text-indigo-500 flex items-center justify-center">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="text-sm font-medium text-gray-500">最高缺勤次数</div>
        </div>
        <div class="text-2xl font-bold text-gray-800 ml-1">{{ summary.maxAbsenceCount }}</div>
      </div>
    </div>

    <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-wrap gap-4 items-end shrink-0">
      <el-form :model="searchQuery" :inline="true" class="flex-1 filter-form -mb-4">
        <el-form-item label="课程">
          <el-select v-model="searchQuery.courseId" placeholder="全部课程" style="width: 220px;" clearable>
            <el-option v-for="item in courseOptions" :key="item.value" :label="item.label" :value="Number(item.value)" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="searchQuery.adminClass" placeholder="全部班级" style="width: 180px;" clearable>
            <el-option v-for="item in classOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="searchQuery.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px;"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="searchQuery.keyword"
            placeholder="搜姓名/学号..."
            clearable
            style="width: 180px;"
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="bg-white rounded-xl shadow-sm border border-gray-100 shrink-0 px-5 pt-3">
      <el-tabs v-model="activeTab" class="warning-tabs">
        <el-tab-pane label="缺勤排行" name="ranking" />
        <el-tab-pane label="通知记录" name="history" />
      </el-tabs>
    </div>

    <div v-show="activeTab === 'ranking'" class="flex gap-5 h-[620px] w-full">
      <div class="w-[70%] bg-white rounded-xl shadow-sm border border-gray-100 flex flex-col overflow-hidden">
        <el-table
          v-loading="pageLoading"
          :data="rankingList"
          height="100%"
          highlight-current-row
          @row-click="handleRowClick"
          class="flex-1 w-full"
        >
          <el-table-column type="index" label="排名" width="60" align="center" />
          <el-table-column prop="studentName" label="姓名" width="100" />
          <el-table-column prop="studentId" label="学号" width="110" />
          <el-table-column prop="className" label="行政班级" min-width="120" show-overflow-tooltip />
          <el-table-column prop="courseName" label="课程名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="absenceCount" label="缺勤次数" width="90" align="center">
            <template #default="{ row }">
              <span class="font-bold text-red-500">{{ row.absenceCount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="lastAbsenceTime" label="最近缺勤" width="160" align="center">
            <template #default="{ row }">
              <span class="text-gray-500">{{ formatDateTime(row.lastAbsenceTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="lastNotifyTime" label="最近通知" width="160" align="center">
            <template #default="{ row }">
              <span class="text-gray-500">{{ formatDateTime(row.lastNotifyTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="notifyCount" label="通知次数" width="90" align="center" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click.stop="handleViewDetails(row)">查看详情</el-button>
              <el-button link type="primary" size="small" @click.stop="openNotifyDialog(row)">发送提醒</el-button>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无缺勤排行数据" />
          </template>
        </el-table>

        <div class="h-14 border-t border-gray-100 flex items-center justify-end px-4 shrink-0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 30]"
            :background="true"
            size="small"
            layout="total, sizes, prev, pager, next"
            :total="totalRows"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>

      <div class="w-[30%] bg-white rounded-xl shadow-sm border border-gray-100 flex flex-col overflow-hidden">
        <div v-if="detailLoading" class="flex-1 flex items-center justify-center text-gray-400">
          正在加载详情...
        </div>

        <div v-else-if="selectedStudent" class="p-5 flex flex-col gap-5 h-full overflow-y-auto">
          <div class="rounded-2xl border border-gray-100 bg-gray-50 p-4">
            <div class="flex items-start justify-between gap-3">
              <div>
                <h3 class="text-lg font-bold text-gray-900">{{ selectedStudent.studentName }}</h3>
                <p class="mt-1 text-sm text-gray-500">学号：{{ selectedStudent.studentId }}</p>
                <p class="mt-1 text-sm text-gray-500">班级：{{ selectedStudent.className || '未分班' }}</p>
                <p class="mt-1 text-sm text-gray-500">课程：{{ selectedStudent.courseName }}</p>
              </div>
              <el-tag :type="selectedStudent.hasUnread ? 'warning' : 'info'" effect="light" round>
                {{ selectedStudent.hasUnread ? '存在未读提醒' : '暂无未读提醒' }}
              </el-tag>
            </div>

            <div class="mt-4 grid grid-cols-2 gap-3">
              <div class="rounded-xl bg-white p-3 border border-gray-100">
                <div class="text-xs text-gray-400">累计缺勤次数</div>
                <div class="mt-1 text-xl font-bold text-red-500">{{ selectedStudent.absenceCount }}</div>
              </div>
              <div class="rounded-xl bg-white p-3 border border-gray-100">
                <div class="text-xs text-gray-400">通知次数</div>
                <div class="mt-1 text-xl font-bold text-blue-500">{{ selectedStudent.notifyCount }}</div>
              </div>
              <div class="rounded-xl bg-white p-3 border border-gray-100 col-span-2">
                <div class="text-xs text-gray-400">最近缺勤</div>
                <div class="mt-1 text-sm font-medium text-gray-700">{{ formatDateTime(selectedStudent.lastAbsenceTime) }}</div>
              </div>
              <div class="rounded-xl bg-white p-3 border border-gray-100 col-span-2">
                <div class="text-xs text-gray-400">最近通知</div>
                <div class="mt-1 text-sm font-medium text-gray-700">{{ formatDateTime(selectedStudent.lastNotifyTime) }}</div>
              </div>
            </div>
          </div>

          <div class="flex-1 flex flex-col min-h-[250px]">
            <div class="flex items-center gap-2 mb-3">
              <div class="w-1 h-4 bg-orange-400 rounded-full"></div>
              <h3 class="font-bold text-gray-800">{{ selectedStudent.studentName }} 的考勤轨迹</h3>
            </div>
            <div class="bg-white p-4 rounded-lg border border-gray-100 flex-1 shadow-sm overflow-hidden flex flex-col">
              <el-scrollbar class="flex-1 pr-2">
                <el-timeline v-if="selectedStudent.timeline && selectedStudent.timeline.length > 0">
                  <el-timeline-item
                    v-for="log in selectedStudent.timeline"
                    :key="log.id"
                    :type="log.statusType"
                    :timestamp="log.date"
                    size="large"
                  >
                    <div class="text-sm flex flex-col gap-1">
                      <div class="flex items-center gap-2">
                        <span class="font-medium text-gray-800">状态：</span>
                        <el-tag :type="getStatusType(log.status)" size="small" effect="plain">{{ log.statusText }}</el-tag>
                      </div>
                      <div class="text-gray-500">{{ log.course }}</div>
                    </div>
                  </el-timeline-item>
                </el-timeline>
                <el-empty v-else description="暂无考勤轨迹" :image-size="60" />
              </el-scrollbar>
            </div>
          </div>

          <div class="pt-2 text-center pb-2">
            <el-button
              type="primary"
              size="large"
              class="w-full"
              :icon="Message"
              @click="openNotifyDialog(selectedStudent)"
            >
              发送提醒通知
            </el-button>
          </div>
        </div>

        <div v-else class="flex-1 flex flex-col items-center justify-center text-gray-400 h-full">
          <el-empty description="在左侧点击表格行查看详情" :image-size="100" />
        </div>
      </div>
    </div>

    <div v-show="activeTab === 'history'" class="bg-white rounded-xl shadow-sm border border-gray-100 h-[620px] w-full flex flex-col overflow-hidden">
      <div class="flex-1 overflow-hidden p-0 flex flex-col">
        <el-table v-loading="historyLoading" :data="notificationHistory" height="100%" class="w-full text-sm">
          <el-table-column type="index" label="序号" width="60" align="center" />
          <el-table-column prop="studentName" label="学生姓名" width="120" />
          <el-table-column prop="studentId" label="学号" width="120" />
          <el-table-column prop="courseName" label="关联课程" min-width="160" show-overflow-tooltip />
          <el-table-column prop="absenceSnapshot" label="缺勤次数快照" width="120" align="center">
            <template #default="{ row }">
              <span class="text-red-500 font-bold">{{ row.absenceSnapshot }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="通知标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="sentTime" label="发送时间" width="180" align="center" />
          <el-table-column prop="isRead" label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.isRead ? 'success' : 'info'" effect="light">
                {{ row.isRead ? '已读' : '未读' }}
              </el-tag>
            </template>
          </el-table-column>

          <template #empty>
            <el-empty description="暂无通知记录" />
          </template>
        </el-table>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="发送考勤提醒" width="500px" append-to-body>
      <div class="bg-blue-50/70 border border-blue-100 text-blue-800 px-4 py-3 rounded text-sm mb-4">
        正在发送提醒给：<span class="font-bold">{{ currentNotifyTarget?.studentName }}</span>
        (学号：{{ currentNotifyTarget?.studentId }})
      </div>
      <el-form label-position="top">
        <el-form-item label="通知标题">
          <el-input v-model="notifyForm.title" placeholder="请输入标题..." />
        </el-form-item>
        <el-form-item label="通知正文内容">
          <el-input
            v-model="notifyForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入对学生的考勤提醒详情..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer flex justify-end gap-2">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="sending" @click="handleSendNotify">确认发送</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.filter-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.warning-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--el-border-color-light);
}

.warning-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  color: var(--el-text-color-regular);
}

.warning-tabs :deep(.el-tabs__item.is-active) {
  color: var(--el-color-primary);
  font-weight: 600;
}

.warning-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 3px;
}
</style>
