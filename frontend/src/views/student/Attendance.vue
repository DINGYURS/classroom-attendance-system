<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import {
  Checked,
  CircleCloseFilled,
  WarningFilled,
  Clock,
  ArrowRight,
  Search,
  DataLine,
  Warning
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getAttendanceRecords } from '@/api/student'
import type { AttendanceRecordVO } from '@/types/api'

const route = useRoute()

// -- 屏幕宽度监听，适配 Drawer 弹出方向 --
const isMobile = ref(window.innerWidth < 768)
const handleResize = () => {
  isMobile.value = window.innerWidth < 768
}
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
const drawerDirection = computed(() => (isMobile.value ? 'btt' : 'rtl'))
const drawerSize = computed(() => (isMobile.value ? '85%' : '400px'))

type StatusType = '' | number

type StatusConfig = {
  text: string
  type: 'success' | 'warning' | 'danger' | 'info'
  color: string
  icon: any
}

type CourseOption = {
  value: number
  label: string
}

const statusConfig: Record<number, StatusConfig> = {
  1: { text: '已到', type: 'success', color: 'text-emerald-500', icon: Checked },
  2: { text: '迟到', type: 'warning', color: 'text-orange-500', icon: Clock },
  0: { text: '缺勤', type: 'danger', color: 'text-red-500', icon: CircleCloseFilled },
  3: { text: '请假', type: 'info', color: 'text-blue-500', icon: WarningFilled }
}

const loading = ref(false)
const records = ref<AttendanceRecordVO[]>([])

const filterForm = reactive({
  term: '',
  course: '' as '' | number,
  status: '' as StatusType,
})

const appliedFilter = reactive({
  term: '',
  course: '' as '' | number,
  status: '' as StatusType,
})

const syncFilterFromRoute = () => {
  const courseId = route.query.courseId
  const status = route.query.status

  filterForm.course = courseId !== undefined && courseId !== '' && !Number.isNaN(Number(courseId))
    ? Number(courseId)
    : ''
  filterForm.status = status !== undefined && status !== '' && !Number.isNaN(Number(status))
    ? Number(status)
    : ''

  appliedFilter.term = filterForm.term
  appliedFilter.course = filterForm.course
  appliedFilter.status = filterForm.status
}

const termOptions = computed(() => {
  const semesterSet = new Set<string>()
  records.value.forEach((record) => {
    if (record.semester) {
      semesterSet.add(record.semester)
    }
  })
  return Array.from(semesterSet)
})

const courseOptions = computed<CourseOption[]>(() => {
  const courseMap = new Map<number, string>()
  records.value
    .filter((record) => !filterForm.term || record.semester === filterForm.term)
    .forEach((record) => {
      if (record.courseId && !courseMap.has(record.courseId)) {
        courseMap.set(record.courseId, record.courseName)
      }
    })

  return Array.from(courseMap.entries()).map(([value, label]) => ({ value, label }))
})

const filteredRecords = computed(() => {
  return records.value.filter((record) => {
    const semesterMatch = !appliedFilter.term || record.semester === appliedFilter.term
    const courseMatch = !appliedFilter.course || record.courseId === appliedFilter.course
    const statusMatch = appliedFilter.status === '' || record.status === appliedFilter.status
    return semesterMatch && courseMatch && statusMatch
  })
})

const stats = computed(() => {
  const total = filteredRecords.value.length
  const normal = filteredRecords.value.filter((record) => record.status === 1).length
  const absent = filteredRecords.value.filter((record) => record.status === 0).length
  const late = filteredRecords.value.filter((record) => record.status === 2).length
  const leave = filteredRecords.value.filter((record) => record.status === 3).length
  const rate = total > 0 ? Math.round(((normal + late) / total) * 100) : 0

  return {
    rate,
    total,
    normal,
    absent,
    late,
    leave
  }
})

// -- Drawer 相关 --
const drawerVisible = ref(false)
const currRecord = ref<AttendanceRecordVO | null>(null)

const fetchAttendanceRecords = async () => {
  loading.value = true
  try {
    const res = await getAttendanceRecords()
    records.value = res.data || []
  } catch (error: any) {
    console.error(error)
    ElMessage.error(error?.message || '获取考勤记录失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  syncFilterFromRoute()
  void fetchAttendanceRecords()
})

watch(() => route.query, () => {
  syncFilterFromRoute()
})

const openDetail = (record: AttendanceRecordVO) => {
  currRecord.value = record
  drawerVisible.value = true
}

const getStatusConf = (status: number) => {
  return statusConfig[status] || { text: '未知', type: 'info', color: 'text-gray-500', icon: WarningFilled }
}

const getSimilarityValue = (record: AttendanceRecordVO) => {
  const value = Number(record.similarityScore ?? 0)
  return Number.isFinite(value) ? value : 0
}

const formatAttendanceTime = (value?: string) => value ? value.replace('T', ' ') : '--'

const getSessionCode = (record: AttendanceRecordVO) => {
  if (!record.sessionId) {
    return '--'
  }
  const datePart = record.attendanceTime?.slice(0, 10).replace(/-/g, '') || 'SESSION'
  return `RC_${datePart}_${String(record.sessionId).padStart(3, '0')}`
}

const getAbnormalHint = (record: AttendanceRecordVO) => {
  const similarity = getSimilarityValue(record)

  if (record.manualModified) {
    return '该记录包含教师人工调整，当前以最终确认结果为准。'
  }

  if (record.status === 0 && similarity > 0 && similarity < 0.6) {
    return `系统未能稳定识别到本人，相似度为 ${Math.round(similarity * 100)}%。`
  }

  if (record.status === 2) {
    return '本次记录被判定为迟到。'
  }

  if (record.status === 3) {
    return '本次记录为请假状态。'
  }

  return '非正常出勤记录。'
}

const applyFilter = () => {
  appliedFilter.term = filterForm.term
  appliedFilter.course = filterForm.course
  appliedFilter.status = filterForm.status
}

const resetFilter = () => {
  filterForm.term = ''
  filterForm.course = ''
  filterForm.status = ''
  applyFilter()
}

const isFilterOpen = ref(false)
const toggleFilter = () => {
    isFilterOpen.value = !isFilterOpen.value
}
</script>

<template>
  <div class="min-h-full bg-gray-50 pb-8">
    <!-- 头部装饰蓝底区域 -->
    <div class="bg-blue-600 pt-8 pb-20 px-4 relative overflow-hidden">
      <!-- Decorational background shapes 同步自 Profile.vue -->
      <div class="absolute top-0 left-0 w-full h-full overflow-hidden opacity-20 pointer-events-none">
        <div class="absolute -top-10 -right-10 w-40 h-40 rounded-full bg-white mix-blend-overlay"></div>
        <div class="absolute top-20 -left-10 w-24 h-24 rounded-full bg-white mix-blend-overlay"></div>
      </div>
      <div class="relative z-10 flex items-center justify-between text-center max-w-2xl mx-auto">
        <h1 class="text-2xl font-bold text-white tracking-wide mb-1">我的考勤</h1>
      </div>
    </div>

    <!-- Main Content Overlapping Header -->
    <div class="px-4 -mt-10 relative z-20 space-y-4 max-w-2xl mx-auto">
      
      <!-- 1. 统计概览区 -->
      <div class="grid grid-cols-2 gap-3">
        <!-- 重点：出勤率 -->
        <div class="bg-white rounded-2xl p-5 shadow-xs border border-gray-100 flex flex-col justify-center transition-shadow hover:shadow-md">
            <div class="flex items-center gap-1.5 text-gray-500 text-xs font-medium mb-1">
                <el-icon><DataLine /></el-icon> 总体出勤率
            </div>
            <div class="text-3xl font-bold text-blue-600">
                {{ stats.rate }}<span class="text-lg font-medium text-blue-400">%</span>
            </div>
            <div class="mt-2 w-full bg-gray-100 rounded-full h-1.5">
                <div class="bg-blue-500 h-1.5 rounded-full" :style="{ width: `${stats.rate}%` }"></div>
            </div>
        </div>

        <!-- 详细：各项数据统计 -->
        <div class="bg-white rounded-2xl p-5 shadow-xs border border-gray-100 grid grid-cols-2 gap-y-3 gap-x-3 transition-shadow hover:shadow-md">
            <div>
                <div class="text-gray-400 text-[10px]">考勤总数</div>
                <div class="text-gray-800 font-bold text-lg leading-tight">{{ stats.total }}</div>
            </div>
            <div>
                <div class="text-gray-400 text-[10px]">已到</div>
                <div class="text-emerald-500 font-bold text-lg leading-tight">{{ stats.normal }}</div>
            </div>
            <div>
                <div class="text-gray-400 text-[10px]">迟到</div>
                <div class="text-orange-500 font-bold text-lg leading-tight">{{ stats.late }}</div>
            </div>
            <div>
                <div class="text-gray-400 text-[10px]">缺勤/请假</div>
                <div class="text-red-500 font-bold text-lg leading-tight">{{ stats.absent + stats.leave }}</div>
            </div>
        </div>
      </div>

      <!-- 2. 筛选过滤区 -->
      <div class="bg-white rounded-2xl shadow-xs border border-gray-100 overflow-hidden hover:shadow-md transition-shadow">
        <div class="px-5 py-4 border-b border-gray-50/80 bg-gray-50/50 flex items-center justify-between cursor-pointer" @click="toggleFilter">
            <div class="flex items-center gap-2 text-gray-700 font-medium text-sm">
                <el-icon class="text-blue-500"><Search /></el-icon> 筛选记录
            </div>
            <el-icon class="text-gray-400 transition-transform duration-300" :class="{ 'rotate-90': isFilterOpen }">
                <ArrowRight />
            </el-icon>
        </div>
        
        <div v-show="isFilterOpen" class="p-4 pt-0 bg-gray-50/50">
            <el-form label-position="right" label-width="70px" class="mobile-filter-form">
                <el-form-item label="选择学期">
                    <el-select v-model="filterForm.term" placeholder="选择学期" class="w-full">
                        <el-option label="全部学期" value="" />
                        <el-option v-for="item in termOptions" :key="item" :label="item" :value="item" />
                    </el-select>
                </el-form-item>
                <div class="grid grid-cols-2 gap-3">
                    <el-form-item label="选择课程">
                        <el-select v-model="filterForm.course" placeholder="全部课程" class="w-full">
                            <el-option label="全部课程" value="" />
                            <el-option v-for="item in courseOptions" :key="item.value" :label="item.label" :value="item.value" />
                        </el-select>
                    </el-form-item>
                    <el-form-item label="考勤状态">
                        <el-select v-model="filterForm.status" placeholder="全部" class="w-full">
                            <el-option label="全部状态" value="" />
                            <el-option label="已到" :value="1" />
                            <el-option label="迟到" :value="2" />
                            <el-option label="缺勤" :value="0" />
                            <el-option label="请假" :value="3" />
                        </el-select>
                    </el-form-item>
                </div>
                <div class="grid grid-cols-2 gap-3">
                  <el-button class="w-full" @click="resetFilter">重置</el-button>
                  <el-button type="primary" class="w-full" plain :loading="loading" @click="applyFilter">应用筛选</el-button>
                </div>
            </el-form>
        </div>
      </div>

      <!-- 3. 考勤列表区 -->
      <div class="space-y-4">
          <div v-if="loading" class="bg-white rounded-2xl shadow-xs border border-gray-100 p-8 text-center text-gray-400">
            加载中...
          </div>
          <div v-else-if="filteredRecords.length === 0" class="bg-white rounded-2xl shadow-xs border border-gray-100 p-8 text-center text-gray-400">
            暂无符合条件的考勤记录
          </div>
          <div v-for="record in filteredRecords" :key="record.recordId" 
                class="bg-white rounded-2xl shadow-xs border border-gray-100 overflow-hidden hover:shadow-md transition-shadow cursor-pointer"
                @click="openDetail(record)">
              
              <div class="p-5">
                  <!-- 头部：课程名和状态标签 -->
                  <div class="flex justify-between items-start mb-2">
                      <h3 class="font-bold text-gray-800 text-base leading-snug line-clamp-1 pr-2">
                          {{ record.courseName }}
                      </h3>
                      <el-tag :type="getStatusConf(record.status).type" effect="light" round size="small" class="shrink-0 border-none font-medium">
                          {{ getStatusConf(record.status).text }}
                      </el-tag>
                  </div>
                  
                  <!-- 副文本信息 -->
                  <div class="text-xs text-gray-500 mb-3 space-y-1">
                      <p>学期：{{ record.semester || '未设置' }}</p>
                      <p>班级：{{ record.teachingClass || '未分班' }}</p>
                      <p>时间：{{ formatAttendanceTime(record.attendanceTime) }}</p>
                  </div>

                  <!-- 异常信息显著提醒 -->
                  <div v-if="(record.status !== 1 && record.status !== 0) || record.manualModified" 
                       class="mt-3 bg-red-50/50 p-2.5 rounded-xl border border-red-100 flex items-start gap-2"
                       :class="{ 'bg-orange-50/50 border-orange-100': record.status === 2 || record.manualModified,
                                 'bg-blue-50/50 border-blue-100': record.status === 3 && !record.manualModified }">
                       <el-icon class="mt-0.5" :class="getStatusConf(record.status).color">
                           <Warning />
                       </el-icon>
                       <div class="text-xs text-gray-600 leading-relaxed">
                           {{ getAbnormalHint(record) }}
                       </div>
                  </div>
              </div>
              <div class="bg-gray-50/70 border-t border-gray-50 px-4 py-2 flex justify-between items-center text-xs text-blue-500">
                  <span>查看详情</span>
                  <el-icon><ArrowRight /></el-icon>
              </div>
          </div>
      </div>
      
      <!-- 底部留白以防遮挡 -->
      <div class="h-6"></div>
    </div>

    <!-- 4. 详情展示 Draw -->
    <el-drawer
      v-model="drawerVisible"
      :direction="drawerDirection"
      :size="drawerSize"
      title="记录详情"
      class="mobile-friendly-drawer rounded-t-2xl md:rounded-l-2xl md:rounded-tr-none"
      :show-close="true"
      destroy-on-close
    >
        <div v-if="currRecord" class="px-1 text-sm text-gray-700">
            <!-- 采用突出效果的状态横幅 -->
            <div class="rounded-xl flex flex-col items-center justify-center p-5 mb-5 border"
                 :class="{
                     'bg-emerald-50 border-emerald-100': currRecord.status === 1,
                     'bg-orange-50 border-orange-100': currRecord.status === 2,
                     'bg-red-50 border-red-100': currRecord.status === 0,
                     'bg-blue-50 border-blue-100': currRecord.status === 3
                 }">
                <el-icon class="text-3xl mb-1" :class="getStatusConf(currRecord.status).color">
                    <component :is="getStatusConf(currRecord.status).icon" />
                </el-icon>
                <div class="font-bold text-lg tracking-wide" :class="getStatusConf(currRecord.status).color">
                    最终状态：{{ getStatusConf(currRecord.status).text }}
                </div>
            </div>

            <!-- 详细属性 -->
                <el-descriptions :column="1" border size="small" class="custom-desc">
                    <el-descriptions-item label="课程名称">{{ currRecord.courseName }}</el-descriptions-item>
                <el-descriptions-item label="所属学期">{{ currRecord.semester || '未设置' }}</el-descriptions-item>
                <el-descriptions-item label="教学班级">{{ currRecord.teachingClass || '未分班' }}</el-descriptions-item>
                <el-descriptions-item label="签到时间">{{ formatAttendanceTime(currRecord.attendanceTime) }}</el-descriptions-item>
                
                <!-- 若人工有修正，说明修正路径 -->
                <el-descriptions-item label="考勤判决">
                    <div v-if="currRecord.manualModified">
                        <el-tag size="small" :type="getStatusConf(currRecord.status).type" effect="dark">{{ getStatusConf(currRecord.status).text }}</el-tag>
                        <span class="ml-2 text-xs text-blue-500 font-medium">包含人工调整</span>
                    </div>
                    <div v-else>
                        <el-tag size="small" :type="getStatusConf(currRecord.status).type">{{ getStatusConf(currRecord.status).text }}</el-tag>
                        <span class="ml-2 text-xs text-gray-400">系统自动判定</span>
                    </div>
                </el-descriptions-item>

                <el-descriptions-item label="异常说明">
                    <span class="text-gray-600">{{ getAbnormalHint(currRecord) }}</span>
                </el-descriptions-item>

                <!-- 人脸识别结果如果存在且相关 -->
                <el-descriptions-item label="识别相似度">
                    <span v-if="getSimilarityValue(currRecord) > 0" 
                          :class="getSimilarityValue(currRecord) >= 0.75 ? 'text-emerald-500' : 'text-red-500'">
                        {{ Math.round(getSimilarityValue(currRecord) * 100) }}%
                    </span>
                    <span v-else class="text-gray-400">无识别数据 / 特殊情况</span>
                </el-descriptions-item>
                
                <el-descriptions-item label="会话流水号">
                    <span class="font-mono text-xs text-gray-400">{{ getSessionCode(currRecord) }}</span>
                </el-descriptions-item>
            </el-descriptions>
        </div>
    </el-drawer>
  </div>
</template>

<style scoped>
/* Mobile form adaptations */
.mobile-filter-form :deep(.el-form-item) {
    margin-bottom: 12px;
}
.mobile-filter-form :deep(.el-form-item__label) {
    font-size: 13px;
    line-height: 32px;
}

/* Custom Descriptions spacing */
.custom-desc :deep(.el-descriptions__label) {
    width: 90px;
    color: #6b7280;
    background-color: #f9fafb;
    font-size: 13px;
}
.custom-desc :deep(.el-descriptions__content) {
    font-size: 13px;
    color: #374151;
}

/* 适配移动端Drawer */
:deep(.mobile-friendly-drawer .el-drawer__header) {
    margin-bottom: 0;
    padding: 16px 20px;
    border-bottom: 1px solid #f3f4f6;
    color: #1f2937;
    font-weight: 700;
}
:deep(.mobile-friendly-drawer .el-drawer__body) {
    padding: 20px;
}
</style>
