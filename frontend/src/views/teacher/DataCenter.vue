<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import {
  DataLine,
  User,
  List,
  TrendCharts,
  Warning,
  Camera
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getStatisticsDashboard } from '@/api/statistics'
import type {
  StatisticsClassStatusVO,
  StatisticsCorrectionVO,
  StatisticsCourseRateVO,
  StatisticsDashboardVO,
  StatisticsOptionVO,
  StatisticsStatusItemVO,
  StatisticsStudentAnomalyVO,
  StatisticsSummaryVO,
  StatisticsTrendItemVO
} from '@/types/api'

const filterForm = ref({
  semester: '',
  courseId: '',
  adminClass: '',
  timeRange: [] as string[],
  isAnomalyIncludeLeave: true
})

const emptySummaryData: StatisticsSummaryVO = {
  totalCourses: 0,
  coveredStudents: 0,
  totalSessions: 0,
  avgAttendanceRate: 0,
  totalAnomalies: 0,
  faceEntryRate: 0
}

const semesterOptions = ref<StatisticsOptionVO[]>([])
const courseOptions = ref<StatisticsOptionVO[]>([])
const classOptions = ref<StatisticsOptionVO[]>([])
const summaryData = ref<StatisticsSummaryVO>({ ...emptySummaryData })
const statusDistribution = ref<StatisticsStatusItemVO[]>([])
const attendanceTrend = ref<StatisticsTrendItemVO[]>([])
const courseAttendanceComparison = ref<StatisticsCourseRateVO[]>([])
const classStatusComposition = ref<StatisticsClassStatusVO[]>([])
const studentAnomalyRanking = ref<StatisticsStudentAnomalyVO[]>([])
const correctionAnalysis = ref<StatisticsCorrectionVO[]>([])

const pieChartRef = ref<HTMLElement | null>(null)
const lineChartRef = ref<HTMLElement | null>(null)
const courseChartRef = ref<HTMLElement | null>(null)
const classChartRef = ref<HTMLElement | null>(null)
const studentChartRef = ref<HTMLElement | null>(null)
const correctionChartRef = ref<HTMLElement | null>(null)

let chartInstances: echarts.EChartsType[] = []

const formatPercent = (value?: number | string) => {
  const numericValue = Number(value ?? 0)
  if (!Number.isFinite(numericValue)) {
    return '0.0%'
  }
  return `${numericValue.toFixed(1)}%`
}

const buildAllOptionList = (options: StatisticsOptionVO[], label: string) => {
  return [{ label, value: '' }, ...(options || [])]
}

const syncSelectedValue = (currentValue: string, options: StatisticsOptionVO[]) => {
  return options.some(option => option.value === currentValue) ? currentValue : ''
}

const destroyCharts = () => {
  chartInstances.forEach(chart => chart.dispose())
  chartInstances = []
}

const renderCharts = () => {
  if (pieChartRef.value) {
    const pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item', formatter: '{a} <br/>{b} : {c} ({d}%)' },
      legend: { bottom: '0%', icon: 'circle' },
      color: ['#10B981', '#F59E0B', '#EF4444', '#6366F1'],
      series: [
        {
          name: '考勤状态',
          type: 'pie',
          radius: ['45%', '70%'],
          itemStyle: {
            borderRadius: 6,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: { show: false },
          data: statusDistribution.value
        }
      ]
    })
    chartInstances.push(pieChart)
  }

  if (lineChartRef.value) {
    const lineChart = echarts.init(lineChartRef.value)
    lineChart.setOption({
      tooltip: { trigger: 'axis', formatter: '{b} <br/>出勤率: {c}%' },
      grid: { left: '3%', right: '4%', bottom: '5%', top: '10%', containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: attendanceTrend.value.map(item => item.label),
        axisLine: { lineStyle: { color: '#E5E7EB' } },
        axisLabel: { color: '#6B7280' }
      },
      yAxis: {
        type: 'value',
        max: 100,
        min: 0,
        splitLine: { lineStyle: { type: 'dashed', color: '#F3F4F6' } },
        axisLabel: { color: '#6B7280', formatter: '{value}%' }
      },
      series: [
        {
          data: attendanceTrend.value.map(item => item.attendanceRate),
          type: 'line',
          smooth: true,
          symbolSize: 8,
          itemStyle: { color: '#3B82F6' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(59, 130, 246, 0.4)' },
              { offset: 1, color: 'rgba(59, 130, 246, 0.0)' }
            ])
          }
        }
      ]
    })
    chartInstances.push(lineChart)
  }

  if (courseChartRef.value) {
    const courseChart = echarts.init(courseChartRef.value)
    courseChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: '{b} <br/>出勤率: {c}%' },
      grid: { left: '3%', right: '8%', bottom: '5%', top: '5%', containLabel: true },
      xAxis: {
        type: 'value',
        max: 100,
        min: 0,
        splitLine: { lineStyle: { type: 'dashed', color: '#F3F4F6' } },
        axisLabel: { color: '#6B7280', formatter: '{value}%' }
      },
      yAxis: {
        type: 'category',
        data: courseAttendanceComparison.value.map(item => item.courseName).reverse(),
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: '#374151' }
      },
      series: [
        {
          type: 'bar',
          data: courseAttendanceComparison.value.map(item => item.attendanceRate).reverse(),
          barWidth: 16,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
              { offset: 0, color: '#8B5CF6' },
              { offset: 1, color: '#C4B5FD' }
            ]),
            borderRadius: [0, 8, 8, 0]
          },
          label: {
            show: true,
            position: 'right',
            formatter: '{c}%',
            color: '#6B7280'
          }
        }
      ]
    })
    chartInstances.push(courseChart)
  }

  if (classChartRef.value) {
    const classChart = echarts.init(classChartRef.value)
    classChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { bottom: '0%', icon: 'circle' },
      grid: { left: '3%', right: '4%', bottom: '15%', top: '5%', containLabel: true },
      xAxis: {
        type: 'category',
        data: classStatusComposition.value.map(item => item.adminClass),
        axisLine: { lineStyle: { color: '#E5E7EB' } },
        axisTick: { show: false },
        axisLabel: { color: '#374151' }
      },
      yAxis: {
        type: 'value',
        name: '人次',
        nameTextStyle: { color: '#6B7280' },
        splitLine: { lineStyle: { type: 'dashed', color: '#F3F4F6' } },
        axisLabel: { color: '#6B7280' }
      },
      color: ['#10B981', '#F59E0B', '#EF4444', '#6366F1'],
      series: [
        { name: '已到', type: 'bar', stack: 'total', barWidth: 24, data: classStatusComposition.value.map(item => item.presentCount) },
        { name: '迟到', type: 'bar', stack: 'total', data: classStatusComposition.value.map(item => item.lateCount) },
        { name: '缺勤', type: 'bar', stack: 'total', data: classStatusComposition.value.map(item => item.absentCount) },
        { name: '请假', type: 'bar', stack: 'total', itemStyle: { borderRadius: [4, 4, 0, 0] }, data: classStatusComposition.value.map(item => item.leaveCount) }
      ]
    })
    chartInstances.push(classChart)
  }

  if (studentChartRef.value) {
    const studentChart = echarts.init(studentChartRef.value)
    studentChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '8%', bottom: '5%', top: '5%', containLabel: true },
      xAxis: {
        type: 'value',
        splitLine: { lineStyle: { type: 'dashed', color: '#F3F4F6' } },
        axisLabel: { color: '#6B7280' }
      },
      yAxis: {
        type: 'category',
        data: studentAnomalyRanking.value.map(item => `${item.studentName}(${item.adminClass})`).reverse(),
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: '#374151' }
      },
      series: [
        {
          type: 'bar',
          data: studentAnomalyRanking.value.map(item => item.anomalyCount).reverse(),
          barWidth: 16,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
              { offset: 0, color: '#F43F5E' },
              { offset: 1, color: '#FDA4AF' }
            ]),
            borderRadius: [0, 8, 8, 0]
          },
          label: { show: true, position: 'right', color: '#6B7280' }
        }
      ]
    })
    chartInstances.push(studentChart)
  }

  if (correctionChartRef.value) {
    const correctionChart = echarts.init(correctionChartRef.value)
    correctionChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      legend: { bottom: '0%', icon: 'circle' },
      grid: { left: '3%', right: '4%', bottom: '15%', top: '5%', containLabel: true },
      xAxis: {
        type: 'category',
        data: correctionAnalysis.value.map(item => item.courseName),
        axisLine: { lineStyle: { color: '#E5E7EB' } },
        axisTick: { show: false },
        axisLabel: { color: '#374151', interval: 0, width: 60, overflow: 'break' }
      },
      yAxis: {
        type: 'value',
        name: '更新记录数',
        nameTextStyle: { color: '#6B7280' },
        splitLine: { lineStyle: { type: 'dashed', color: '#F3F4F6' } },
        axisLabel: { color: '#6B7280' }
      },
      color: ['#14B8A6', '#F59E0B'],
      series: [
        {
          name: '算法自动识别',
          type: 'bar',
          stack: 'total',
          barWidth: 24,
          data: correctionAnalysis.value.map(item => item.autoCount)
        },
        {
          name: '人工手动修正',
          type: 'bar',
          stack: 'total',
          itemStyle: { borderRadius: [4, 4, 0, 0] },
          data: correctionAnalysis.value.map(item => item.manualCount)
        }
      ]
    })
    chartInstances.push(correctionChart)
  }
}

const handleResize = () => {
  chartInstances.forEach(chart => chart.resize())
}

const applyDashboardData = (dashboard: StatisticsDashboardVO) => {
  const nextSemesterOptions = buildAllOptionList(dashboard.semesterOptions || [], '全部学期')
  const nextCourseOptions = buildAllOptionList(dashboard.courseOptions || [], '全部课程')
  const nextClassOptions = buildAllOptionList(dashboard.classOptions || [], '全部班级')

  semesterOptions.value = nextSemesterOptions
  courseOptions.value = nextCourseOptions
  classOptions.value = nextClassOptions

  filterForm.value.semester = syncSelectedValue(filterForm.value.semester, nextSemesterOptions)
  filterForm.value.courseId = syncSelectedValue(filterForm.value.courseId, nextCourseOptions)
  filterForm.value.adminClass = syncSelectedValue(filterForm.value.adminClass, nextClassOptions)

  summaryData.value = dashboard.summaryData || { ...emptySummaryData }
  statusDistribution.value = dashboard.statusDistribution || []
  attendanceTrend.value = dashboard.attendanceTrend || []
  courseAttendanceComparison.value = dashboard.courseAttendanceComparison || []
  classStatusComposition.value = dashboard.classStatusComposition || []
  studentAnomalyRanking.value = dashboard.studentAnomalyRanking || []
  correctionAnalysis.value = dashboard.correctionAnalysis || []
}

const fetchDashboard = async () => {
  const [startDate, endDate] = filterForm.value.timeRange || []
  chartInstances.forEach(chart => chart.showLoading())

  try {
    const res = await getStatisticsDashboard({
      semester: filterForm.value.semester || undefined,
      courseId: filterForm.value.courseId ? Number(filterForm.value.courseId) : undefined,
      adminClass: filterForm.value.adminClass || undefined,
      startDate: startDate || undefined,
      endDate: endDate || undefined,
      anomalyIncludeLeave: filterForm.value.isAnomalyIncludeLeave
    })

    applyDashboardData(res.data)
    await nextTick()
    destroyCharts()
    renderCharts()
  } finally {
    chartInstances.forEach(chart => chart.hideLoading())
  }
}

const handleFilterSearch = async () => {
  await fetchDashboard()
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await nextTick()
  await fetchDashboard()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  destroyCharts()
})
</script>

<template>
  <div class="data-center h-full flex flex-col gap-5 overflow-x-hidden pb-8">
    <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-wrap gap-4 items-end">
      <el-form :model="filterForm" :inline="true" class="flex-1 filter-form -mb-4">
        <el-form-item label="学期">
          <el-select
            v-model="filterForm.semester"
            style="width: 180px;"
            @change="() => { filterForm.courseId = ''; filterForm.adminClass = '' }"
          >
            <el-option v-for="opt in semesterOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程">
          <el-select
            v-model="filterForm.courseId"
            filterable
            style="width: 160px;"
            placeholder="全部课程"
            @change="() => { filterForm.adminClass = '' }"
          >
            <el-option v-for="opt in courseOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="行政班级">
          <el-select v-model="filterForm.adminClass" filterable style="width: 160px;" placeholder="全部班级">
            <el-option v-for="opt in classOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filterForm.timeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px;"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilterSearch">筛选统计</el-button>
        </el-form-item>
      </el-form>

      <div class="flex gap-4 items-center bg-gray-50 px-4 py-2 rounded-lg border border-gray-100 h-9 shrink-0 shadow-inner">
        <div class="text-sm text-gray-500 font-medium">统计口径：</div>
        <el-checkbox v-model="filterForm.isAnomalyIncludeLeave" @change="handleFilterSearch" class="m-0! font-normal">
          异常包含请假
        </el-checkbox>
      </div>
    </div>

    <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 transition-all hover:shadow-md hover:border-blue-200">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-8 h-8 rounded-full bg-blue-50 text-blue-500 flex items-center justify-center">
            <el-icon><List /></el-icon>
          </div>
          <div class="text-sm font-medium text-gray-500">课程总数</div>
        </div>
        <div class="text-2xl font-bold text-gray-800 ml-1">{{ summaryData.totalCourses }}</div>
      </div>

      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 transition-all hover:shadow-md hover:border-indigo-200">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-8 h-8 rounded-full bg-indigo-50 text-indigo-500 flex items-center justify-center">
            <el-icon><User /></el-icon>
          </div>
          <div class="text-sm font-medium text-gray-500">覆盖学生数</div>
        </div>
        <div class="text-2xl font-bold text-gray-800 ml-1">{{ summaryData.coveredStudents }}</div>
      </div>

      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 transition-all hover:shadow-md hover:border-emerald-200">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-8 h-8 rounded-full bg-emerald-50 text-emerald-500 flex items-center justify-center">
            <el-icon><DataLine /></el-icon>
          </div>
          <div class="text-sm font-medium text-gray-500">累计点名次数</div>
        </div>
        <div class="text-2xl font-bold text-gray-800 ml-1">{{ summaryData.totalSessions }}</div>
      </div>

      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 transition-all hover:shadow-md hover:border-sky-200">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-8 h-8 rounded-full bg-sky-50 text-sky-500 flex items-center justify-center">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="text-sm font-medium text-gray-500">平均出勤率</div>
        </div>
        <div class="text-2xl font-bold text-gray-800 ml-1">{{ formatPercent(summaryData.avgAttendanceRate) }}</div>
      </div>

      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 transition-all hover:shadow-md hover:border-rose-200">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-8 h-8 rounded-full bg-rose-50 text-rose-500 flex items-center justify-center">
            <el-icon><Warning /></el-icon>
          </div>
          <div class="text-sm font-medium text-gray-500">异常总人次</div>
        </div>
        <div class="text-2xl font-bold text-rose-600 ml-1">{{ summaryData.totalAnomalies }}</div>
      </div>

      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 transition-all hover:shadow-md hover:border-purple-200">
        <div class="flex items-center gap-3 mb-2">
          <div class="w-8 h-8 rounded-full bg-purple-50 text-purple-500 flex items-center justify-center">
            <el-icon><Camera /></el-icon>
          </div>
          <div class="text-sm font-medium text-gray-500">人脸录入率</div>
        </div>
        <div class="text-2xl font-bold text-gray-800 ml-1">{{ formatPercent(summaryData.faceEntryRate) }}</div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 px-5 pt-5 pb-3">
        <div class="flex items-center gap-2 mb-4">
          <div class="w-1 h-4 bg-blue-500 rounded-full"></div>
          <h3 class="font-bold text-gray-800">总体考勤状态占比</h3>
        </div>
        <div ref="pieChartRef" class="w-full h-[280px]"></div>
      </div>

      <div class="bg-white rounded-xl shadow-sm border border-gray-100 px-5 pt-5 pb-3">
        <div class="flex items-center gap-2 mb-4">
          <div class="w-1 h-4 bg-sky-500 rounded-full"></div>
          <h3 class="font-bold text-gray-800">考勤趋势图</h3>
        </div>
        <div ref="lineChartRef" class="w-full h-[280px]"></div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 px-5 pt-5 pb-3">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-2">
            <div class="w-1 h-4 bg-purple-500 rounded-full"></div>
            <h3 class="font-bold text-gray-800">课程出勤率对比</h3>
          </div>
          <span class="text-xs text-gray-400 bg-gray-50 px-2 py-1 rounded">平均出勤率</span>
        </div>
        <div ref="courseChartRef" class="w-full h-[280px]"></div>
      </div>

      <div class="bg-white rounded-xl shadow-sm border border-gray-100 px-5 pt-5 pb-3">
        <div class="flex items-center gap-2 mb-4">
          <div class="w-1 h-4 bg-emerald-500 rounded-full"></div>
          <h3 class="font-bold text-gray-800">班级考勤状态构成</h3>
        </div>
        <div ref="classChartRef" class="w-full h-[280px]"></div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 px-5 pt-5 pb-3">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-2">
            <div class="w-1 h-4 bg-rose-500 rounded-full"></div>
            <h3 class="font-bold text-gray-800">学生异常排行预警</h3>
          </div>
          <span class="text-xs text-gray-400 bg-gray-50 px-2 py-1 rounded">Top 10 异常记录</span>
        </div>
        <div ref="studentChartRef" class="w-full h-[280px]"></div>
      </div>

      <div class="bg-white rounded-xl shadow-sm border border-gray-100 px-5 pt-5 pb-3">
        <div class="flex items-center gap-2 mb-4">
          <div class="w-1 h-4 bg-teal-500 rounded-full"></div>
          <h3 class="font-bold text-gray-800">打卡方式识别准确率推算 (算法 vs 人工)</h3>
        </div>
        <div ref="correctionChartRef" class="w-full h-[280px]"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.filter-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

:deep(.el-checkbox__label) {
  font-size: 13px !important;
  color: #4b5563 !important;
}
</style>
