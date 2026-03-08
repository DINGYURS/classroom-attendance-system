<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Camera, Check, Picture, RefreshRight, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCourseDetail } from '@/api/course'
import {
  endAttendance,
  getAttendanceSessionDetail,
  getAttendanceSessionRecords,
  recognizeAttendance,
  startAttendance,
  updateAttendanceStatus,
  uploadAttendanceImage
} from '@/api/attendance'
import type {
  CourseVO,
  RecognitionResultVO,
  SessionRecordVO
} from '@/types/api'

type CaptureSlotKey = 'left' | 'center' | 'right'
type FileSource = 'camera' | 'album'

type CaptureSlot = {
  key: CaptureSlotKey
  label: string
  hint: string
  objectKey: string
  previewUrl: string
  fileName: string
  uploading: boolean
}

const route = useRoute()
const router = useRouter()

const courseId = computed(() => Number(route.params.id))
const courseInfo = ref<CourseVO | null>(null)
const pageLoading = ref(false)
const step = ref(1)
const sessionId = ref<number | null>(null)
const recognizing = ref(false)
const finishing = ref(false)
const filterMode = ref<'all' | 'abnormal'>('all')
const recognitionResults = ref<RecognitionResultVO[]>([])
const records = ref<SessionRecordVO[]>([])
const updatingRecordId = ref<number | null>(null)
const pendingSlotKey = ref<CaptureSlotKey | null>(null)
const cameraInputRef = ref<HTMLInputElement | null>(null)
const albumInputRef = ref<HTMLInputElement | null>(null)

const captureSlots = ref<CaptureSlot[]>([
  {
    key: 'left',
    label: '左侧视角',
    hint: '建议覆盖教室左半区学生。',
    objectKey: '',
    previewUrl: '',
    fileName: '',
    uploading: false
  },
  {
    key: 'center',
    label: '中间视角',
    hint: '建议覆盖教室中间区域学生。',
    objectKey: '',
    previewUrl: '',
    fileName: '',
    uploading: false
  },
  {
    key: 'right',
    label: '右侧视角',
    hint: '建议覆盖教室右半区学生。',
    objectKey: '',
    previewUrl: '',
    fileName: '',
    uploading: false
  }
])

const statusOptions = [
  { label: '缺勤', value: 0, tagType: 'danger' as const },
  { label: '已到', value: 1, tagType: 'success' as const },
  { label: '迟到', value: 2, tagType: 'warning' as const },
  { label: '请假', value: 3, tagType: 'info' as const }
]

const allUploaded = computed(() => captureSlots.value.every((slot) => !!slot.objectKey))
const matchedCount = computed(() => records.value.filter((item) => item.status === 1 || item.status === 2).length)
const absentCount = computed(() => records.value.filter((item) => item.status === 0).length)
const lateCount = computed(() => records.value.filter((item) => item.status === 2).length)
const leaveCount = computed(() => records.value.filter((item) => item.status === 3).length)
const unmatchedCount = computed(() => recognitionResults.value.filter((item) => !item.matched).length)
const displayRecords = computed(() => {
  if (filterMode.value === 'abnormal') {
    return records.value.filter((item) => item.status !== 1)
  }
  return records.value
})

const loadCourseDetail = async () => {
  if (!Number.isFinite(courseId.value) || courseId.value <= 0) {
    ElMessage.error('课程参数无效')
    router.replace('/teacher/dashboard')
    return
  }

  pageLoading.value = true
  try {
    const res = await getCourseDetail(courseId.value)
    courseInfo.value = res.data
  } catch (error: any) {
    ElMessage.error(error.message || '获取课程信息失败')
    router.replace('/teacher/dashboard')
  } finally {
    pageLoading.value = false
  }
}

const getSlot = (slotKey: CaptureSlotKey) => {
  return captureSlots.value.find((item) => item.key === slotKey) as CaptureSlot
}

const canOperateSlot = (index: number) => {
  if (index === 0) {
    return true
  }
  return captureSlots.value.slice(0, index).every((item) => !!item.objectKey)
}

const revokePreview = (previewUrl: string) => {
  if (previewUrl.startsWith('blob:')) {
    URL.revokeObjectURL(previewUrl)
  }
}

const openPicker = (slotKey: CaptureSlotKey, source: FileSource, index: number) => {
  if (!canOperateSlot(index)) {
    ElMessage.warning('请先上传上一个视角的照片')
    return
  }

  pendingSlotKey.value = slotKey
  if (source === 'camera') {
    cameraInputRef.value?.click()
    return
  }
  albumInputRef.value?.click()
}

const handleFileSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  const slotKey = pendingSlotKey.value

  input.value = ''
  pendingSlotKey.value = null

  if (!file || !slotKey) {
    return
  }

  const slot = getSlot(slotKey)
  slot.uploading = true

  if (slot.previewUrl) {
    revokePreview(slot.previewUrl)
  }
  slot.previewUrl = URL.createObjectURL(file)
  slot.fileName = file.name

  try {
    const formData = new FormData()
    formData.append('file', file)

    const res = await uploadAttendanceImage(formData)
    slot.objectKey = res.data
    ElMessage.success(`${slot.label}上传成功`)
  } catch (error: any) {
    slot.objectKey = ''
    ElMessage.error(error.message || `${slot.label}上传失败`)
  } finally {
    slot.uploading = false
  }
}

const resetUploads = () => {
  captureSlots.value.forEach((slot) => {
    if (slot.previewUrl) {
      revokePreview(slot.previewUrl)
    }
    slot.objectKey = ''
    slot.previewUrl = ''
    slot.fileName = ''
    slot.uploading = false
  })
  recognitionResults.value = []
  records.value = []
  filterMode.value = 'all'
  step.value = 1
  sessionId.value = null
}

const loadSessionRecords = async (currentSessionId: number) => {
  const [detailRes, recordRes] = await Promise.all([
    getAttendanceSessionDetail(currentSessionId),
    getAttendanceSessionRecords(currentSessionId)
  ])

  records.value = [...(recordRes.data || [])].sort((a, b) => a.studentNumber.localeCompare(b.studentNumber))

  if (courseInfo.value) {
    courseInfo.value = {
      ...courseInfo.value,
      studentCount: detailRes.data.totalCount ?? courseInfo.value.studentCount
    }
  }
}

const handleRecognize = async () => {
  if (!allUploaded.value) {
    ElMessage.warning('请先上传左、中、右三张照片')
    return
  }

  recognizing.value = true
  step.value = 2

  try {
    if (!sessionId.value) {
      const startRes = await startAttendance({ courseId: courseId.value })
      sessionId.value = startRes.data
    }

    const recognizeRes = await recognizeAttendance({
      sessionId: sessionId.value,
      imageKeys: captureSlots.value.map((item) => item.objectKey)
    })

    recognitionResults.value = recognizeRes.data || []
    await loadSessionRecords(sessionId.value)
    step.value = 3
    ElMessage.success('识别完成')
  } catch (error: any) {
    step.value = 1
    ElMessage.error(error.message || '识别失败')
  } finally {
    recognizing.value = false
  }
}

const getStatusLabel = (status: number) => {
  return statusOptions.find((item) => item.value === status)?.label || '未知'
}

const getStatusTagType = (status: number) => {
  return statusOptions.find((item) => item.value === status)?.tagType || 'info'
}

const formatSimilarity = (value?: number | string) => {
  if (value === undefined || value === null || value === '') {
    return '--'
  }

  const numericValue = Number(value)
  if (Number.isNaN(numericValue)) {
    return '--'
  }

  return `${(numericValue * 100).toFixed(2)}%`
}

const handleStatusChange = async (record: SessionRecordVO, status: number) => {
  if (record.status === status) {
    return
  }

  const previousStatus = record.status
  updatingRecordId.value = record.recordId

  try {
    await updateAttendanceStatus({
      recordId: record.recordId,
      status
    })

    record.status = status
    record.statusText = getStatusLabel(status)
    ElMessage.success('状态已更新')
  } catch (error: any) {
    record.status = previousStatus
    ElMessage.error(error.message || '状态更新失败')
  } finally {
    updatingRecordId.value = null
  }
}

const handleStatusSelectChange = (record: SessionRecordVO, value: number | string) => {
  void handleStatusChange(record, Number(value))
}

const handleFinishAttendance = async () => {
  if (!sessionId.value) {
    ElMessage.warning('当前没有可提交的点名会话')
    return
  }

  try {
    await ElMessageBox.confirm('提交后将结束本次点名，是否继续？', '结束点名', {
      confirmButtonText: '确认结束',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }

  finishing.value = true
  try {
    await endAttendance(sessionId.value)
    ElMessage.success('点名已提交')
    router.replace('/teacher/dashboard')
  } catch (error: any) {
    ElMessage.error(error.message || '提交失败')
  } finally {
    finishing.value = false
  }
}

onMounted(() => {
  loadCourseDetail()
})

onBeforeUnmount(() => {
  captureSlots.value.forEach((slot) => {
    if (slot.previewUrl) {
      revokePreview(slot.previewUrl)
    }
  })
})
</script>

<template>
  <div class="mx-auto max-w-6xl space-y-6">
    <input
      ref="cameraInputRef"
      type="file"
      accept="image/*"
      capture="environment"
      class="hidden"
      @change="handleFileSelected"
    />
    <input
      ref="albumInputRef"
      type="file"
      accept="image/*"
      class="hidden"
      @change="handleFileSelected"
    />

    <div class="flex flex-col gap-4 rounded-3xl bg-linear-to-r from-slate-900 via-slate-800 to-slate-900 p-6 text-white shadow-sm md:flex-row md:items-end md:justify-between">
      <div class="space-y-3">
        <el-button plain class="!border-white/20 !bg-white/10 !text-white" :icon="ArrowLeft" @click="router.back()">
          返回
        </el-button>
        <div>
          <h1 class="text-2xl font-bold tracking-wide">发起点名</h1>
          <p class="mt-2 text-sm text-white/70">
            {{ courseInfo?.courseName || '正在加载课程...' }}
            <span v-if="courseInfo?.classes?.length"> · {{ courseInfo.classes.join(' / ') }}</span>
          </p>
        </div>
      </div>
      <div class="grid grid-cols-2 gap-3 md:grid-cols-3">
        <div class="rounded-2xl bg-white/8 px-4 py-3 backdrop-blur-sm">
          <div class="text-xs text-white/60">应到人数</div>
          <div class="mt-1 text-xl font-bold">{{ courseInfo?.studentCount ?? '--' }}</div>
        </div>
        <div class="rounded-2xl bg-white/8 px-4 py-3 backdrop-blur-sm">
          <div class="text-xs text-white/60">已上传视角</div>
          <div class="mt-1 text-xl font-bold">{{ captureSlots.filter((item) => item.objectKey).length }}/3</div>
        </div>
        <div class="rounded-2xl bg-white/8 px-4 py-3 backdrop-blur-sm col-span-2 md:col-span-1">
          <div class="text-xs text-white/60">当前步骤</div>
          <div class="mt-1 text-xl font-bold">{{ step === 1 ? '上传照片' : step === 2 ? '识别中' : '结果确认' }}</div>
        </div>
      </div>
    </div>

    <el-steps :active="step - 1" finish-status="success" align-center>
      <el-step title="上传视角" description="依次上传左、中、右三张课堂照片" />
      <el-step title="AI识别" description="调用 YOLO 和 InsightFace 完成人脸识别" />
      <el-step title="确认提交" description="核对考勤结果并结束本次点名" />
    </el-steps>

    <div v-if="pageLoading" class="rounded-3xl border border-gray-100 bg-white p-10 text-center text-gray-500 shadow-sm">
      正在加载课程信息...
    </div>

    <template v-else>
      <div v-if="step === 1" class="space-y-6">
        <div class="grid grid-cols-1 gap-5 lg:grid-cols-3">
          <div
            v-for="(slot, index) in captureSlots"
            :key="slot.key"
            class="rounded-3xl border border-gray-100 bg-white p-5 shadow-sm transition-all"
            :class="canOperateSlot(index) ? 'opacity-100' : 'opacity-60'"
          >
            <div class="flex items-start justify-between gap-4">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">视角 {{ index + 1 }}</p>
                <h3 class="mt-2 text-lg font-bold text-gray-900">{{ slot.label }}</h3>
                <p class="mt-1 text-sm text-gray-500">{{ slot.hint }}</p>
              </div>
              <el-tag :type="slot.objectKey ? 'success' : 'info'" effect="light" round>
                {{ slot.objectKey ? '已上传' : '待上传' }}
              </el-tag>
            </div>

            <div class="mt-5 overflow-hidden rounded-2xl border border-dashed border-gray-200 bg-slate-50">
              <div v-if="slot.previewUrl" class="aspect-video overflow-hidden bg-black">
                <img :src="slot.previewUrl" :alt="slot.label" class="h-full w-full object-cover" />
              </div>
              <div v-else class="flex aspect-video flex-col items-center justify-center text-gray-400">
                <el-icon :size="28"><UploadFilled /></el-icon>
                <span class="mt-3 text-sm">请上传{{ slot.label }}</span>
              </div>
            </div>

            <div class="mt-4 min-h-[52px] rounded-2xl bg-slate-50 px-4 py-3 text-sm text-slate-500">
              {{ slot.fileName || '可直接调用手机摄像头拍照，或从本地相册选择图片。' }}
            </div>

            <div class="mt-4 grid grid-cols-2 gap-3">
              <el-button
                type="primary"
                plain
                :icon="Camera"
                :disabled="slot.uploading || !canOperateSlot(index)"
                @click="openPicker(slot.key, 'camera', index)"
              >
                {{ slot.uploading ? '上传中...' : '拍照上传' }}
              </el-button>
              <el-button
                :icon="Picture"
                :disabled="slot.uploading || !canOperateSlot(index)"
                @click="openPicker(slot.key, 'album', index)"
              >
                相册选择
              </el-button>
            </div>
          </div>
        </div>

        <div class="flex flex-col gap-3 rounded-3xl border border-amber-100 bg-amber-50 p-5 text-sm text-amber-800 shadow-sm md:flex-row md:items-center md:justify-between">
          <div>
            建议教师站在教室前方，按左、中、右顺序拍摄，尽量保证学生正脸清晰且无遮挡。
          </div>
          <div class="flex gap-3">
            <el-button :icon="RefreshRight" @click="resetUploads">清空重传</el-button>
            <el-button type="primary" :loading="recognizing" :disabled="!allUploaded" @click="handleRecognize">
              开始识别
            </el-button>
          </div>
        </div>
      </div>

      <div v-else-if="step === 2" class="rounded-3xl border border-gray-100 bg-white px-6 py-16 text-center shadow-sm">
        <div class="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-slate-100">
          <div class="h-10 w-10 animate-spin rounded-full border-4 border-slate-200 border-t-slate-700"></div>
        </div>
        <h3 class="mt-6 text-2xl font-bold text-gray-900">正在识别人脸</h3>
        <p class="mt-3 text-sm text-gray-500">
          已上传 {{ captureSlots.filter((item) => item.objectKey).length }} 张照片，系统正在检测人脸并进行特征比对。
        </p>
      </div>

      <div v-else class="space-y-6">
        <div class="grid grid-cols-2 gap-4 lg:grid-cols-5">
          <div class="rounded-3xl border border-gray-100 bg-white p-5 shadow-sm">
            <div class="text-xs text-gray-500">应到人数</div>
            <div class="mt-2 text-2xl font-bold text-gray-900">{{ courseInfo?.studentCount ?? records.length }}</div>
          </div>
          <div class="rounded-3xl border border-emerald-100 bg-emerald-50 p-5 shadow-sm">
            <div class="text-xs text-emerald-700">实到人数</div>
            <div class="mt-2 text-2xl font-bold text-emerald-700">{{ matchedCount }}</div>
          </div>
          <div class="rounded-3xl border border-rose-100 bg-rose-50 p-5 shadow-sm">
            <div class="text-xs text-rose-700">缺勤人数</div>
            <div class="mt-2 text-2xl font-bold text-rose-700">{{ absentCount }}</div>
          </div>
          <div class="rounded-3xl border border-amber-100 bg-amber-50 p-5 shadow-sm">
            <div class="text-xs text-amber-700">迟到人数</div>
            <div class="mt-2 text-2xl font-bold text-amber-700">{{ lateCount }}</div>
          </div>
          <div class="rounded-3xl border border-slate-200 bg-slate-50 p-5 shadow-sm">
            <div class="text-xs text-slate-600">请假 / 未匹配</div>
            <div class="mt-2 text-2xl font-bold text-slate-800">{{ leaveCount }} / {{ unmatchedCount }}</div>
          </div>
        </div>

        <div class="flex flex-col gap-4 rounded-3xl border border-gray-100 bg-white p-5 shadow-sm md:flex-row md:items-center md:justify-between">
          <div class="space-y-1">
            <h3 class="text-lg font-bold text-gray-900">识别结果确认</h3>
            <p class="text-sm text-gray-500">
              未匹配人脸：{{ unmatchedCount }} 个。如识别结果有误，可先修正状态再提交。
            </p>
          </div>
          <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
            <el-radio-group v-model="filterMode" size="small">
              <el-radio-button label="all">全部学生</el-radio-button>
              <el-radio-button label="abnormal">仅看异常</el-radio-button>
            </el-radio-group>
            <el-button type="primary" :icon="Check" :loading="finishing" @click="handleFinishAttendance">
              提交点名
            </el-button>
          </div>
        </div>

        <div v-if="displayRecords.length" class="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <div
            v-for="record in displayRecords"
            :key="record.recordId"
            class="rounded-3xl border border-gray-100 bg-white p-5 shadow-sm"
          >
            <div class="flex items-start justify-between gap-4">
              <div class="min-w-0">
                <div class="flex items-center gap-3">
                  <div class="flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-900 text-lg font-bold text-white">
                    {{ record.realName?.slice(0, 1) || '?' }}
                  </div>
                  <div class="min-w-0">
                    <div class="truncate text-lg font-bold text-gray-900">{{ record.realName }}</div>
                    <div class="truncate text-sm text-gray-500">学号：{{ record.studentNumber }}</div>
                  </div>
                </div>
              </div>
              <el-tag :type="getStatusTagType(record.status)" effect="light" round>
                {{ record.statusText || getStatusLabel(record.status) }}
              </el-tag>
            </div>

            <div class="mt-5 grid grid-cols-2 gap-3 rounded-2xl bg-slate-50 p-4 text-sm">
              <div>
                <div class="text-slate-400">相似度</div>
                <div class="mt-1 font-semibold text-slate-800">{{ formatSimilarity(record.similarityScore) }}</div>
              </div>
              <div>
                <div class="text-slate-400">当前状态</div>
                <div class="mt-1 font-semibold text-slate-800">{{ getStatusLabel(record.status) }}</div>
              </div>
            </div>

            <div class="mt-4">
              <div class="mb-2 text-sm font-medium text-gray-600">人工修正</div>
              <el-select
                :model-value="record.status"
                class="w-full"
                :loading="updatingRecordId === record.recordId"
                @change="handleStatusSelectChange(record, $event)"
              >
                <el-option
                  v-for="item in statusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </div>
          </div>
        </div>

        <div v-else class="rounded-3xl border border-gray-100 bg-white p-10 shadow-sm">
          <el-empty description="暂无考勤记录" />
        </div>
      </div>
    </template>
  </div>
</template>