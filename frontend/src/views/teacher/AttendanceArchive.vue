<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import {
  Search,
  Download,
  Document,
  Histogram,
  User,
  Warning,
  CircleCheck,
  CircleClose,
  DataLine,
  View
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  exportAttendanceArchive,
  exportAttendanceArchiveSummary,
  exportAttendanceSession,
  getAttendanceArchiveOptions,
  getAttendanceArchivePage,
  getAttendanceArchiveSessionDetail
} from '@/api/attendance'
import type {
  AttendanceArchiveDetailVO,
  AttendanceArchiveQueryDTO,
  AttendanceArchiveSessionDetailVO,
  AttendanceArchiveSessionVO,
  StatisticsOptionVO
} from '@/types/api'

const statusOptions = [
  { label: '全部', value: undefined },
  { label: '正常', value: 1 },
  { label: '迟到', value: 2 },
  { label: '请假', value: 3 },
  { label: '缺勤', value: 0 }
]

const typeOptions = [
  { label: '全部类型', value: undefined },
  { label: '课堂识别', value: 1 },
  { label: '人工修正', value: 2 }
]

const filterForm = reactive({
  courseId: undefined as number | string | undefined,
  adminClass: '',
  dateRange: [] as string[],
  status: undefined as number | undefined,
  type: undefined as number | undefined,
  keyword: ''
})

const loading = ref(false)
const optionLoading = ref(false)
const sessionList = ref<AttendanceArchiveSessionVO[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const courseOptions = ref<StatisticsOptionVO[]>([])
const classOptions = ref<StatisticsOptionVO[]>([])

const summary = reactive({
  totalSessions: 0,
  expectedTotal: 0,
  actualTotal: 0,
  absentTotal: 0,
  lateTotal: 0,
  avgRate: '0%'
})

const drawerVisible = ref(false)
const drawerLoading = ref(false)
const currentSession = ref<AttendanceArchiveSessionDetailVO | null>(null)
const exportingAll = ref(false)
const exportingSummary = ref(false)
const exportingSessionId = ref<number | null>(null)

const detailList = computed<AttendanceArchiveDetailVO[]>(() => currentSession.value?.detailList || [])

const buildQueryParams = (): AttendanceArchiveQueryDTO => {
  const [startDate, endDate] = filterForm.dateRange || []
  return {
    courseId: filterForm.courseId,
    adminClass: filterForm.adminClass || undefined,
    startDate: startDate || undefined,
    endDate: endDate || undefined,
    status: filterForm.status,
    type: filterForm.type,
    keyword: filterForm.keyword.trim() || undefined,
    currentPage: currentPage.value,
    pageSize: pageSize.value
  }
}

const buildExportParams = (): AttendanceArchiveQueryDTO => {
  const params = buildQueryParams()
  delete params.currentPage
  delete params.pageSize
  return params
}

const normalizeCourseOptions = (options: StatisticsOptionVO[]) => [
  { label: '全部', value: '' },
  ...options
]

const normalizeClassOptions = (options: StatisticsOptionVO[]) => [
  { label: '全部', value: '' },
  ...options
]

const fetchArchiveOptions = async (courseId?: number | string) => {
  optionLoading.value = true
  try {
    const res = await getAttendanceArchiveOptions(courseId || undefined)
    courseOptions.value = normalizeCourseOptions(res.data.courseOptions || [])
    classOptions.value = normalizeClassOptions(res.data.classOptions || [])
  } catch (error) {
    console.error(error)
  } finally {
    optionLoading.value = false
  }
}

const fetchArchivePage = async () => {
  loading.value = true
  try {
    const res = await getAttendanceArchivePage(buildQueryParams())
    summary.totalSessions = res.data.summary?.totalSessions || 0
    summary.expectedTotal = res.data.summary?.expectedTotal || 0
    summary.actualTotal = res.data.summary?.actualTotal || 0
    summary.absentTotal = res.data.summary?.absentTotal || 0
    summary.lateTotal = res.data.summary?.lateTotal || 0
    summary.avgRate = res.data.summary?.avgRate || '0%'
    sessionList.value = res.data.pageData?.records || []
    total.value = res.data.pageData?.total || 0
  } catch (error) {
    console.error(error)
    sessionList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  currentPage.value = 1
  await fetchArchivePage()
}

const handleReset = async () => {
  filterForm.courseId = undefined
  filterForm.adminClass = ''
  filterForm.dateRange = []
  filterForm.status = undefined
  filterForm.type = undefined
  filterForm.keyword = ''
  currentPage.value = 1
  pageSize.value = 10
  await fetchArchiveOptions()
  await fetchArchivePage()
}

const extractFileName = (contentDisposition?: string) => {
  if (!contentDisposition) {
    return ''
  }

  const utf8Match = contentDisposition.match(/filename\*=utf-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }

  const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  return plainMatch?.[1] || ''
}

const downloadBlob = (blob: Blob, fileName: string) => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

const handleExportAll = async () => {
  exportingAll.value = true
  try {
    const response = await exportAttendanceArchive(buildExportParams())
    const blob = new Blob([response.data], {
      type: response.headers['content-type'] || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const fileName = extractFileName(response.headers['content-disposition']) || '考勤档案明细.xlsx'
    downloadBlob(blob, fileName)
    ElMessage.success('筛选结果导出成功')
  } catch (error) {
    console.error(error)
    ElMessage.error('筛选结果导出失败')
  } finally {
    exportingAll.value = false
  }
}

const handleExportSummary = async () => {
  exportingSummary.value = true
  try {
    const response = await exportAttendanceArchiveSummary(buildExportParams())
    const blob = new Blob([response.data], {
      type: response.headers['content-type'] || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const fileName = extractFileName(response.headers['content-disposition']) || '考勤档案课程汇总.xlsx'
    downloadBlob(blob, fileName)
    ElMessage.success('课程汇总导出成功')
  } catch (error) {
    console.error(error)
    ElMessage.error('课程汇总导出失败')
  } finally {
    exportingSummary.value = false
  }
}

const handleExportSession = async (sessionId: number) => {
  exportingSessionId.value = sessionId
  try {
    const response = await exportAttendanceSession(sessionId)
    const blob = new Blob([response.data], {
      type: response.headers['content-type'] || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const fileName = extractFileName(response.headers['content-disposition']) || `单次点名详情_${sessionId}.xlsx`
    downloadBlob(blob, fileName)
    ElMessage.success('单次会话导出成功')
  } catch (error) {
    console.error(error)
    ElMessage.error('单次会话导出失败')
  } finally {
    exportingSessionId.value = null
  }
}

const openSessionDetail = async (row: AttendanceArchiveSessionVO) => {
  drawerVisible.value = true
  drawerLoading.value = true
  currentSession.value = null

  try {
    const res = await getAttendanceArchiveSessionDetail(row.id)
    currentSession.value = res.data
  } catch (error) {
    console.error(error)
    drawerVisible.value = false
  } finally {
    drawerLoading.value = false
  }
}

watch(
  () => filterForm.courseId,
  async (courseId) => {
    filterForm.adminClass = ''
    await fetchArchiveOptions(courseId)
  }
)

onMounted(async () => {
  await fetchArchiveOptions()
  await fetchArchivePage()
})
</script>

<template>
  <div class="space-y-6">
    <div class="mb-5 flex flex-col justify-between gap-4 md:flex-row md:items-center">
      <div class="flex items-center gap-3">
        <div>
          <h1 class="text-2xl font-bold text-gray-900">考勤档案</h1>
          <p class="mt-1 text-gray-500">用于查看历史点名、筛选记录、导出报表及单次点名复盘</p>
        </div>
      </div>
      <div class="flex flex-wrap gap-3">
        <el-button :icon="Download" :loading="exportingSummary" @click="handleExportSummary">
          导出课程汇总
        </el-button>
        <el-button type="primary" :icon="Download" :loading="exportingAll" @click="handleExportAll">导出考勤结果</el-button>
      </div>
    </div>

    <div class="rounded-xl border border-gray-100 bg-white p-5 shadow-sm">
      <el-form :model="filterForm" class="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4" @submit.prevent>
        <el-form-item class="mb-0!">
          <el-select v-model="filterForm.courseId" placeholder="选择课程" clearable class="w-full" :loading="optionLoading">
            <el-option v-for="item in courseOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item class="mb-0!">
          <el-select v-model="filterForm.adminClass" placeholder="选择班级" clearable class="w-full" :loading="optionLoading">
            <el-option v-for="item in classOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item class="mb-0! lg:col-span-2">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            class="w-full"
          />
        </el-form-item>
        <el-form-item class="mb-0!">
          <el-select v-model="filterForm.status" placeholder="考勤状态" clearable class="w-full">
            <el-option v-for="item in statusOptions" :key="String(item.value)" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item class="mb-0!">
          <el-select v-model="filterForm.type" placeholder="点名类型" clearable class="w-full">
            <el-option v-for="item in typeOptions" :key="String(item.value)" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item class="mb-0!">
          <el-input
            v-model="filterForm.keyword"
            placeholder="搜学号/姓名"
            clearable
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <div class="flex items-center gap-3">
          <el-button type="primary" @click="handleSearch" class="flex-1">查询</el-button>
          <el-button @click="handleReset" class="flex-1">重置</el-button>
        </div>
      </el-form>
    </div>

    <div class="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-6">
      <div class="flex flex-col items-center justify-center rounded-xl border border-gray-100 bg-white p-4 shadow-sm">
        <div class="mb-2 flex h-10 w-10 items-center justify-center rounded-full bg-blue-50 text-blue-500">
          <el-icon class="text-xl"><Histogram /></el-icon>
        </div>
        <div class="text-2xl font-bold text-gray-800">{{ summary.totalSessions }}</div>
        <div class="mt-1 text-xs text-gray-500">点名次数</div>
      </div>
      <div class="flex flex-col items-center justify-center rounded-xl border border-gray-100 bg-white p-4 shadow-sm">
        <div class="mb-2 flex h-10 w-10 items-center justify-center rounded-full bg-indigo-50 text-indigo-500">
          <el-icon class="text-xl"><User /></el-icon>
        </div>
        <div class="text-2xl font-bold text-gray-800">{{ summary.expectedTotal }}</div>
        <div class="mt-1 text-xs text-gray-500">应到总人次</div>
      </div>
      <div class="flex flex-col items-center justify-center rounded-xl border border-gray-100 bg-white p-4 shadow-sm">
        <div class="mb-2 flex h-10 w-10 items-center justify-center rounded-full bg-emerald-50 text-emerald-500">
          <el-icon class="text-xl"><CircleCheck /></el-icon>
        </div>
        <div class="text-2xl font-bold text-gray-800">{{ summary.actualTotal }}</div>
        <div class="mt-1 text-xs text-gray-500">实到总人次</div>
      </div>
      <div class="flex flex-col items-center justify-center rounded-xl border border-gray-100 bg-white p-4 shadow-sm">
        <div class="mb-2 flex h-10 w-10 items-center justify-center rounded-full bg-red-50 text-red-500">
          <el-icon class="text-xl"><Warning /></el-icon>
        </div>
        <div class="text-2xl font-bold text-gray-800">{{ summary.absentTotal }}</div>
        <div class="mt-1 text-xs text-gray-500">缺勤总人次</div>
      </div>
      <div class="flex flex-col items-center justify-center rounded-xl border border-gray-100 bg-white p-4 shadow-sm">
        <div class="mb-2 flex h-10 w-10 items-center justify-center rounded-full bg-amber-50 text-amber-500">
          <el-icon class="text-xl"><CircleClose /></el-icon>
        </div>
        <div class="text-2xl font-bold text-gray-800">{{ summary.lateTotal }}</div>
        <div class="mt-1 text-xs text-gray-500">迟到总人次</div>
      </div>
      <div class="flex flex-col items-center justify-center rounded-xl border border-gray-100 bg-white p-4 shadow-sm">
        <div class="mb-2 flex h-10 w-10 items-center justify-center rounded-full bg-purple-50 text-purple-500">
          <el-icon class="text-xl"><DataLine /></el-icon>
        </div>
        <div class="text-2xl font-bold text-gray-800">{{ summary.avgRate }}</div>
        <div class="mt-1 text-xs text-gray-500">平均出勤率</div>
      </div>
    </div>

    <div class="flex min-h-0 flex-col overflow-hidden rounded-xl border border-gray-100 bg-white shadow-sm">
      <div class="flex items-center justify-between border-b border-gray-100 bg-gray-50/50 p-4">
        <h3 class="flex items-center gap-2 font-bold text-gray-800">
          <el-icon class="text-blue-500"><Document /></el-icon>
          历史点名会话
          <span class="rounded border border-gray-200 bg-white px-2 py-0.5 text-xs font-normal text-gray-500">
            共 {{ total }} 条记录
          </span>
        </h3>
      </div>
      <el-table
        v-loading="loading"
        :data="sessionList"
        style="width: 100%"
        :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: '600' }"
      >
        <el-table-column prop="sessionTime" label="时间" width="180" align="center" />
        <el-table-column prop="courseName" label="课程" min-width="140" align="center" />
        <el-table-column prop="className" label="班级" min-width="140" align="center" show-overflow-tooltip />

        <el-table-column label="出勤概况" min-width="180" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2 text-xs">
              <span class="text-gray-500" title="应到">{{ row.expectedCount }}人</span>
              <span class="font-medium text-emerald-500" title="实到">{{ row.actualCount }}到</span>
              <span v-if="row.lateCount > 0" class="font-medium text-amber-500" title="迟到">{{ row.lateCount }}迟</span>
              <span v-if="row.absentCount > 0" class="font-medium text-red-500" title="缺勤">{{ row.absentCount }}缺</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="attendanceRate" label="出勤率" width="100" align="center">
          <template #default="{ row }">
            <span class="font-mono font-bold" :class="row.attendanceRate === '100%' ? 'text-emerald-500' : 'text-blue-500'">
              {{ row.attendanceRate }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="openSessionDetail(row)">详情</el-button>
            <el-button
              link
              type="primary"
              :icon="Download"
              :loading="exportingSessionId === row.id"
              @click="handleExportSession(row.id)"
            >
              导出
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex justify-end border-t border-gray-100 p-4">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next"
          :total="total"
          @current-change="fetchArchivePage"
          @size-change="handleSearch"
        />
      </div>
    </div>

    <el-drawer v-model="drawerVisible" title="会话详情" size="600px">
      <div v-if="currentSession" class="flex h-full flex-col gap-5">
        <div class="space-y-4 rounded-xl border border-gray-100 bg-gray-50 p-5">
          <div class="flex items-start justify-between">
            <div>
              <h3 class="text-lg font-bold text-gray-900">{{ currentSession.courseName }} - {{ currentSession.className }}</h3>
              <p class="mt-1 text-sm text-gray-500">{{ currentSession.sessionTime }}</p>
            </div>
            <div class="rounded-full border border-blue-100 bg-blue-50 px-3 py-1 text-sm font-medium text-blue-600">
              {{ currentSession.type }}
            </div>
          </div>
          <div class="grid grid-cols-4 gap-2 border-t border-gray-200/60 pt-4 text-center">
            <div>
              <div class="mb-1 text-xs text-gray-400">应到</div>
              <div class="font-mono text-xl font-bold text-gray-800">{{ currentSession.expectedCount }}</div>
            </div>
            <div>
              <div class="mb-1 text-xs text-gray-400">实到</div>
              <div class="font-mono text-xl font-bold text-emerald-600">{{ currentSession.actualCount }}</div>
            </div>
            <div>
              <div class="mb-1 text-xs text-gray-400">迟到</div>
              <div class="font-mono text-xl font-bold text-amber-600">{{ currentSession.lateCount }}</div>
            </div>
            <div>
              <div class="mb-1 text-xs text-gray-400">缺勤</div>
              <div class="font-mono text-xl font-bold text-red-600">{{ currentSession.absentCount }}</div>
            </div>
          </div>
        </div>

        <div class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-xl border border-gray-100 shadow-sm">
          <div class="flex items-center justify-between border-b border-gray-100 bg-white p-3">
            <span class="border-l-3 border-blue-500 pl-2 text-sm font-bold text-gray-700">学生考勤明细</span>
            <el-button
              size="small"
              type="primary"
              plain
              :icon="Download"
              :loading="exportingSessionId === currentSession.sessionId"
              @click="handleExportSession(currentSession.sessionId)"
            >
              导出该表
            </el-button>
          </div>
          <el-table
            v-loading="drawerLoading"
            :data="detailList"
            height="100%"
            style="width: 100%"
            :header-cell-style="{ background: '#f8fafc', color: '#475569', fontSize: '13px', fontWeight: 'bold' }"
          >
            <el-table-column prop="studentId" label="学号" min-width="120" />
            <el-table-column prop="studentName" label="姓名" min-width="100" />
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.status === '已到'" type="success" size="small" effect="plain">正常</el-tag>
                <el-tag v-else-if="row.status === '迟到'" type="warning" size="small" effect="plain">{{ row.status }}</el-tag>
                <el-tag v-else-if="row.status === '缺勤'" type="danger" size="small" effect="plain">{{ row.status }}</el-tag>
                <el-tag v-else-if="row.status === '请假'" type="info" size="small" effect="plain">{{ row.status }}</el-tag>
                <span v-else class="text-xs text-gray-400">{{ row.status }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="记录方式" width="100" align="center">
              <template #default="{ row }">
                <span class="text-xs text-gray-500">{{ row.type }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
:deep(.el-drawer__header) {
  margin-bottom: 0;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  font-weight: bold;
}

:deep(.el-date-editor) {
  --el-date-editor-width: 100%;
}
</style>
