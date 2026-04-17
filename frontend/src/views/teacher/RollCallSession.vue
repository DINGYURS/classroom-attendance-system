<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Camera, Check, Picture, RefreshRight, UploadFilled, Refresh, Aim, MagicStick } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCourseDetail, getCourseStudents } from '@/api/course'
import {
  assignAttendanceDetection,
  endAttendance,
  getAttendanceSessionAnnotations,
  getAttendanceSessionDetail,
  getAttendanceSessionRecords,
  ignoreAttendanceDetection,
  recognizeAttendance,
  startAttendance,
  updateAttendanceStatus,
  uploadAttendanceImage
} from '@/api/attendance'
import type {
  AttendanceDetectionVO,
  AttendanceSessionImageVO,
  CourseStudentVO,
  CourseVO,
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

interface BoundingBox {
  id: string
  detectionId: number
  recordId?: number
  sourceKey: CaptureSlotKey
  x1: number
  y1: number
  x2: number
  y2: number
  studentNumber?: string
  name?: string
  similarityScore?: number | string
  status: 'matched' | 'low_conf' | 'unmatched' | 'corrected' | 'ignored'
  attendanceStatus?: number
  manualModified?: boolean
  ignoreReason?: string
}

type FilterMode = 'all' | 'unmatched' | 'low_conf' | 'corrected' | 'abnormal' | 'ignored'

type UnifiedItem = {
  type: 'record' | 'unmatched' | 'ignored'
  id: string
  record?: SessionRecordVO
  box?: BoundingBox
}

const route = useRoute()
const router = useRouter()

const courseId = computed(() => Number(route.params.id))
const courseInfo = ref<CourseVO | null>(null)
const pageLoading = ref(false)
const annotationLoading = ref(false)
const step = ref(1)
const sessionId = ref<number | null>(null)
const recognizing = ref(false)
const finishing = ref(false)

const records = ref<SessionRecordVO[]>([])
const annotationImages = ref<AttendanceSessionImageVO[]>([])
const courseStudents = ref<CourseStudentVO[]>([])
const updatingRecordId = ref<number | null>(null)
const ignoringDetectionId = ref<number | null>(null)
const assigningDetectionId = ref<number | null>(null)
const pendingSlotKey = ref<CaptureSlotKey | null>(null)
const cameraInputRef = ref<HTMLInputElement | null>(null)
const albumInputRef = ref<HTMLInputElement | null>(null)

const activePerspective = ref<CaptureSlotKey>('center')
const transform = ref({ scale: 1, x: 0, y: 0 })
const isDragging = ref(false)
const dragStart = ref({ x: 0, y: 0 })
const viewportRef = ref<HTMLDivElement | null>(null)
const viewportSize = ref({ width: 0, height: 0 })
const imageNaturalMap = ref<Record<CaptureSlotKey, { width: number; height: number }>>({
  left: { width: 0, height: 0 },
  center: { width: 0, height: 0 },
  right: { width: 0, height: 0 }
})

const boundingBoxes = ref<BoundingBox[]>([])
const selectedBoxId = ref<string | null>(null)
const showBoxTags = ref(true)
const filterAnnotationMode = ref<FilterMode>('all')
const assignDialogVisible = ref(false)
const currentAssignBox = ref<BoundingBox | null>(null)
const selectedAssignStudentId = ref<number | null>(null)

const captureSlots = ref<CaptureSlot[]>([
  { key: 'left', label: '左侧视角', hint: '建议覆盖教室左半区学生。', objectKey: '', previewUrl: '', fileName: '', uploading: false },
  { key: 'center', label: '中间视角', hint: '建议覆盖教室中间区域学生。', objectKey: '', previewUrl: '', fileName: '', uploading: false },
  { key: 'right', label: '右侧视角', hint: '建议覆盖教室右半区学生。', objectKey: '', previewUrl: '', fileName: '', uploading: false }
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
const unmatchedCount = computed(() => boundingBoxes.value.filter(b => b.status === 'unmatched').length)
const ignoredCount = computed(() => boundingBoxes.value.filter(b => b.status === 'ignored').length)

const activeImage = computed(() => {
  return annotationImages.value.find((item) => normalizeViewKey(item.viewKey, item.imageIndex) === activePerspective.value) || null
})

const activeImageSource = computed(() => {
  return activeImage.value?.imageUrl || getSlot(activePerspective.value).previewUrl || ''
})

const currentImageNatural = computed(() => imageNaturalMap.value[activePerspective.value])

const stageSize = computed(() => {
  const natural = currentImageNatural.value
  const viewport = viewportSize.value

  if (!natural.width || !natural.height || !viewport.width || !viewport.height) {
    return {
      width: Math.max(viewport.width - 32, 320),
      height: Math.max(viewport.height - 32, 240)
    }
  }

  const naturalRatio = natural.width / natural.height
  const viewportRatio = viewport.width / viewport.height

  if (naturalRatio > viewportRatio) {
    const width = Math.max(viewport.width - 32, 320)
    return {
      width,
      height: width / naturalRatio
    }
  }

  const height = Math.max(viewport.height - 32, 240)
  return {
    width: height * naturalRatio,
    height
  }
})

const unifiedList = computed<UnifiedItem[]>(() => {
  let list: UnifiedItem[] = []
  
  records.value.forEach(record => {
      const recordBoxes = boundingBoxes.value.filter(b => b.recordId === record.recordId)
      const box = recordBoxes.find(b => b.sourceKey === activePerspective.value)
        || [...recordBoxes].sort((a, b) => Number(b.similarityScore || 0) - Number(a.similarityScore || 0))[0]
      let pass = false
      if (filterAnnotationMode.value === 'all') pass = true
      else if (filterAnnotationMode.value === 'abnormal' && record.status !== 1) pass = true
      else if (filterAnnotationMode.value === 'low_conf' && recordBoxes.some(b => b.status === 'low_conf')) pass = true
      else if (filterAnnotationMode.value === 'corrected' && recordBoxes.some(b => b.status === 'corrected')) pass = true

      if (pass) {
          list.push({ type: 'record', id: `record_${record.recordId}`, record, box })
      }
  })
  
  if (['all', 'unmatched', 'ignored'].includes(filterAnnotationMode.value)) {
      const detectionBoxes = boundingBoxes.value.filter((box) => {
        if (filterAnnotationMode.value === 'unmatched') return box.status === 'unmatched'
        if (filterAnnotationMode.value === 'ignored') return box.status === 'ignored'
        return box.status === 'unmatched' || box.status === 'ignored'
      })
      detectionBoxes.forEach(box => {
          list.push({ type: box.status === 'ignored' ? 'ignored' : 'unmatched', id: box.id, box })
      })
  }
  
  return list
})

const currentViewBoxes = computed(() => {
  return boundingBoxes.value.filter(b => b.sourceKey === activePerspective.value)
})

const assignableStudents = computed(() => {
  const boundStudentIds = new Set(
    boundingBoxes.value
      .filter((box) => box.status !== 'ignored' && box.recordId)
      .map((box) => records.value.find((record) => record.recordId === box.recordId)?.studentId)
      .filter((studentId): studentId is number => typeof studentId === 'number')
  )

  return courseStudents.value.filter((student) => !boundStudentIds.has(student.userId))
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

const loadCourseStudents = async () => {
  try {
    const res = await getCourseStudents(courseId.value)
    courseStudents.value = res.data || []
  } catch (error: any) {
    ElMessage.error(error.message || '获取课程学生名单失败')
  }
}

const getSlot = (slotKey: CaptureSlotKey) => captureSlots.value.find((item) => item.key === slotKey) as CaptureSlot

const canOperateSlot = (index: number) => index === 0 ? true : captureSlots.value.slice(0, index).every((item) => !!item.objectKey)

const revokePreview = (previewUrl: string) => {
  if (previewUrl.startsWith('blob:')) URL.revokeObjectURL(previewUrl)
}

const openPicker = (slotKey: CaptureSlotKey, source: FileSource, index: number) => {
  if (!canOperateSlot(index)) {
    ElMessage.warning('请先上传上一个视角的照片')
    return
  }
  pendingSlotKey.value = slotKey
  if (source === 'camera') cameraInputRef.value?.click()
  else albumInputRef.value?.click()
}

const handleFileSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  const slotKey = pendingSlotKey.value

  input.value = ''
  pendingSlotKey.value = null

  if (!file || !slotKey) return

  const slot = getSlot(slotKey)
  slot.uploading = true

  if (slot.previewUrl) revokePreview(slot.previewUrl)
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
    if (slot.previewUrl) revokePreview(slot.previewUrl)
    slot.objectKey = ''
    slot.previewUrl = ''
    slot.fileName = ''
    slot.uploading = false
  })
  records.value = []
  annotationImages.value = []
  boundingBoxes.value = []
  filterAnnotationMode.value = 'all'
  selectedBoxId.value = null
  assignDialogVisible.value = false
  currentAssignBox.value = null
  selectedAssignStudentId.value = null
  activePerspective.value = 'center'
  resetView()
  step.value = 1
  sessionId.value = null
}

const normalizeViewKey = (viewKey?: string, imageIndex?: number): CaptureSlotKey => {
  if (viewKey === 'left' || viewKey === 'center' || viewKey === 'right') {
    return viewKey
  }
  if (imageIndex === 0) return 'left'
  if (imageIndex === 1) return 'center'
  return 'right'
}

const parseBbox = (bbox: string) => {
  try {
    const result = JSON.parse(bbox)
    if (!Array.isArray(result) || result.length < 4) {
      return null
    }
    const [x1, y1, x2, y2] = result.map((item) => Number(item))
    if ([x1, y1, x2, y2].some((item) => Number.isNaN(item))) {
      return null
    }
    return { x1, y1, x2, y2 }
  } catch {
    return null
  }
}

const resolveBoxStatus = (detection: AttendanceDetectionVO): BoundingBox['status'] => {
  if (detection.ignored) {
    return 'ignored'
  }
  if (!detection.matched) {
    return 'unmatched'
  }
  if (detection.manualModified) {
    return 'corrected'
  }
  const similarity = Number(detection.similarityScore)
  if (!Number.isNaN(similarity) && similarity < 0.7) {
    return 'low_conf'
  }
  return 'matched'
}

const mapDetections = (detections: AttendanceDetectionVO[]) => {
  return detections
    .map((detection) => {
      const bbox = parseBbox(detection.bbox)
      if (!bbox) {
        return null
      }

      return {
        id: `detection_${detection.detectionId}`,
        detectionId: detection.detectionId,
        recordId: detection.recordId,
        sourceKey: normalizeViewKey(detection.viewKey, detection.imageIndex),
        x1: bbox.x1,
        y1: bbox.y1,
        x2: bbox.x2,
        y2: bbox.y2,
        studentNumber: detection.studentNumber,
        name: detection.realName || (detection.matched ? '未知学生' : '未匹配'),
        similarityScore: detection.similarityScore,
        status: resolveBoxStatus(detection),
        attendanceStatus: detection.finalStatus,
        manualModified: detection.manualModified,
        ignoreReason: detection.ignoreReason
      } as BoundingBox
    })
    .filter((item): item is BoundingBox => !!item)
}

const syncViewportSize = () => {
  if (!viewportRef.value) {
    return
  }
  viewportSize.value = {
    width: viewportRef.value.clientWidth,
    height: viewportRef.value.clientHeight
  }
}

const loadResultData = async (currentSessionId: number, keepSelectedId?: string | null) => {
  annotationLoading.value = true
  try {
    const [detailRes, recordRes, annotationRes] = await Promise.all([
      getAttendanceSessionDetail(currentSessionId),
      getAttendanceSessionRecords(currentSessionId),
      getAttendanceSessionAnnotations(currentSessionId)
    ])

    records.value = [...(recordRes.data || [])].sort((a, b) => a.studentNumber.localeCompare(b.studentNumber))
    annotationImages.value = annotationRes.data?.images || []
    boundingBoxes.value = mapDetections(annotationRes.data?.detections || [])

    if (courseInfo.value) {
      courseInfo.value = {
        ...courseInfo.value,
        studentCount: detailRes.data.totalCount ?? courseInfo.value.studentCount
      }
    }

    const firstImage = annotationImages.value[0]
    if (!annotationImages.value.some((item) => normalizeViewKey(item.viewKey, item.imageIndex) === activePerspective.value)
      && firstImage) {
      activePerspective.value = normalizeViewKey(firstImage.viewKey, firstImage.imageIndex)
    }

    if (keepSelectedId) {
      selectedBoxId.value = keepSelectedId
    } else if (selectedBoxId.value && !boundingBoxes.value.some((item) => item.id === selectedBoxId.value)) {
      selectedBoxId.value = null
    }
  } finally {
    annotationLoading.value = false
    await nextTick()
    syncViewportSize()
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

    if (recognizeRes.code !== 1) {
      throw new Error(recognizeRes.message || '识别失败')
    }

    step.value = 3
    resetView()
    await loadResultData(sessionId.value)
    ElMessage.success('识别完成')
  } catch (error: any) {
    step.value = 1
    ElMessage.error(error.message || '识别失败')
  } finally {
    recognizing.value = false
  }
}

const getStatusLabel = (status: number) => statusOptions.find((item) => item.value === status)?.label || '未知'
const getStatusTagType = (status: number) => statusOptions.find((item) => item.value === status)?.tagType || 'info'

const formatSimilarity = (value?: number | string) => {
  if (value === undefined || value === null || value === '') return '--'
  const numericValue = Number(value)
  if (Number.isNaN(numericValue)) return '--'
  return `${(numericValue * 100).toFixed(2)}%`
}

const handleStatusChange = async (record: SessionRecordVO, status: number) => {
  if (record.status === status) return
  updatingRecordId.value = record.recordId

  try {
    await updateAttendanceStatus({ recordId: record.recordId, status })
    ElMessage.success('状态已人工修正')
    if (sessionId.value) {
      await loadResultData(sessionId.value, `record_${record.recordId}`)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '状态更新失败')
  } finally {
    updatingRecordId.value = null
  }
}

const handleStatusSelectChange = (record: SessionRecordVO, value: number | string) => {
  void handleStatusChange(record, Number(value))
}

const handleIgnoreBox = async (box: BoundingBox) => {
  try {
    const promptResult = await ElMessageBox.prompt('请输入忽略原因，便于后续回看本次识别结果。', '忽略未匹配人脸', {
      confirmButtonText: '确认忽略',
      cancelButtonText: '取消',
      inputValue: '非本课程学生或误检',
      inputPlaceholder: '例如：教师、路人、重复框、误检',
      inputPattern: /^.{0,128}$/,
      inputErrorMessage: '忽略原因不能超过 128 个字符',
      type: 'warning'
    }) as { value?: string }

    ignoringDetectionId.value = box.detectionId
    await ignoreAttendanceDetection(box.detectionId, { ignoreReason: promptResult.value })
    ElMessage.success('检测框已忽略')
    if (sessionId.value) {
      await loadResultData(sessionId.value, box.id)
    }
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '忽略检测框失败')
    }
  } finally {
    ignoringDetectionId.value = null
  }
}

const openAssignDialog = async (box: BoundingBox) => {
  currentAssignBox.value = box
  selectedAssignStudentId.value = null
  assignDialogVisible.value = true
  if (courseStudents.value.length === 0) {
    await loadCourseStudents()
  }
}

const handleConfirmAssign = async () => {
  const box = currentAssignBox.value
  if (!box) return
  if (!selectedAssignStudentId.value) {
    ElMessage.warning('请选择要指派的学生')
    return
  }

  assigningDetectionId.value = box.detectionId
  try {
    await assignAttendanceDetection(box.detectionId, { studentId: selectedAssignStudentId.value })
    ElMessage.success('检测框已指派学生')
    assignDialogVisible.value = false
    currentAssignBox.value = null
    selectedAssignStudentId.value = null
    if (sessionId.value) {
      await loadResultData(sessionId.value)
    }
  } catch (error: any) {
    ElMessage.error(error.message || '指派检测框失败')
  } finally {
    assigningDetectionId.value = null
  }
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

// ============== Pan & Zoom & Box Handlers ==============
const adjustScale = (factor: number) => {
  const nextScale = transform.value.scale * factor
  transform.value.scale = Math.max(1, Math.min(nextScale, 4))
}

const handleWheel = (e: WheelEvent) => {
  if (step.value !== 3 || !activeImageSource.value) return
  adjustScale(e.deltaY > 0 ? 0.9 : 1.1)
}

const handleMouseDown = (e: MouseEvent) => {
  if (step.value !== 3 || !activeImageSource.value) return
  isDragging.value = true
  dragStart.value = { x: e.clientX - transform.value.x, y: e.clientY - transform.value.y }
}

const handleMouseMove = (e: MouseEvent) => {
  if (!isDragging.value) return
  transform.value.x = e.clientX - dragStart.value.x
  transform.value.y = e.clientY - dragStart.value.y
}

const handleMouseUp = () => isDragging.value = false

const resetView = () => {
  transform.value = { scale: 1, x: 0, y: 0 }
}

const switchPerspective = (key: CaptureSlotKey) => {
  activePerspective.value = key
  resetView()
}

const getBoxStyle = (box: BoundingBox) => {
  const natural = currentImageNatural.value
  if (!natural.width || !natural.height) {
    return {}
  }

  return {
    left: `${(box.x1 / natural.width) * 100}%`,
    top: `${(box.y1 / natural.height) * 100}%`,
    width: `${((box.x2 - box.x1) / natural.width) * 100}%`,
    height: `${((box.y2 - box.y1) / natural.height) * 100}%`
  }
}

const getBoxListId = (box: BoundingBox) => box.recordId ? `record_${box.recordId}` : box.id

const getBoxClass = (box: BoundingBox) => {
  let base = 'transition-all '
  if (box.id === selectedBoxId.value || getBoxListId(box) === selectedBoxId.value) {
      base += 'ring-[3px] ring-white z-[50] scale-105 shadow-sm '
  } else {
      base += 'opacity-80 hover:opacity-100 hover:z-[40] '
  }
  
  if (box.status === 'corrected') return base + 'border-blue-500 bg-blue-500/10'
  if (box.status === 'ignored') return base + 'border-slate-400 bg-slate-400/10'
  if (box.status === 'unmatched') return base + 'border-yellow-400 bg-yellow-400/10'
  if (box.status === 'low_conf') return base + 'border-red-500 bg-red-500/10'
  return base + 'border-emerald-400 bg-emerald-400/10'
}

const handleBoxClick = (box: BoundingBox) => {
  selectedBoxId.value = getBoxListId(box)
  nextTick(() => {
    const el = document.getElementById(getBoxListId(box))
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  })
}

const handleCardClick = (item: UnifiedItem) => {
  selectedBoxId.value = item.id
  if (item.box && activePerspective.value !== item.box.sourceKey) {
     switchPerspective(item.box.sourceKey)
  }
}

const handleImageLoad = (event: Event) => {
  const target = event.target as HTMLImageElement
  imageNaturalMap.value[activePerspective.value] = {
    width: target.naturalWidth,
    height: target.naturalHeight
  }
  syncViewportSize()
}

const handleWindowResize = () => {
  syncViewportSize()
}

onMounted(async () => {
  await loadCourseDetail()
  window.addEventListener('resize', handleWindowResize)
  nextTick(syncViewportSize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleWindowResize)
})

onBeforeUnmount(() => {
  captureSlots.value.forEach((slot) => { if (slot.previewUrl) revokePreview(slot.previewUrl) })
})
</script>

<template>
  <div class="mx-auto max-w-[1400px] space-y-6">
    <input ref="cameraInputRef" type="file" accept="image/*" capture="environment" class="hidden" @change="handleFileSelected" />
    <input ref="albumInputRef" type="file" accept="image/*" class="hidden" @change="handleFileSelected" />

    <el-dialog v-model="assignDialogVisible" title="指派未匹配人脸" width="520px" destroy-on-close>
      <div class="space-y-4">
        <div v-if="currentAssignBox" class="rounded-2xl border border-slate-100 bg-slate-50 p-4 text-sm text-slate-600">
          <div>检测框编号: {{ currentAssignBox.detectionId }}</div>
          <div class="mt-1">来源视角: {{ getSlot(currentAssignBox.sourceKey).label }}</div>
          <div class="mt-1">说明: 该框尚未匹配到课程学生，需要教师手动确认身份。</div>
        </div>

        <el-select
          v-model="selectedAssignStudentId"
          class="w-full"
          filterable
          placeholder="请选择要绑定的学生"
          :loading="courseStudents.length === 0"
        >
          <el-option
            v-for="student in assignableStudents"
            :key="student.userId"
            :label="`${student.realName}（${student.studentNumber}）${student.adminClass ? ' · ' + student.adminClass : ''}`"
            :value="student.userId"
          />
        </el-select>

        <p class="text-xs leading-6 text-slate-500">
          指派后会把该检测框绑定到所选学生，并将该学生本次考勤状态更新为“已到”，所有结果都会写入数据库。
        </p>
      </div>

      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="!!assigningDetectionId" @click="handleConfirmAssign">确认指派</el-button>
      </template>
    </el-dialog>

    <div class="flex flex-col gap-4 rounded-3xl bg-linear-to-r from-slate-900 via-slate-800 to-slate-900 p-6 text-white shadow-sm md:flex-row md:items-end md:justify-between">
      <div class="space-y-3">
        <el-button plain class="border-white/20! bg-white/10! text-white!" :icon="ArrowLeft" @click="router.back()">返回</el-button>
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
      <!-- STEP 1 -->
      <div v-if="step === 1" class="space-y-6">
        <div class="grid grid-cols-1 gap-5 lg:grid-cols-3">
          <div v-for="(slot, index) in captureSlots" :key="slot.key" class="rounded-3xl border border-gray-100 bg-white p-5 shadow-sm transition-all" :class="canOperateSlot(index) ? 'opacity-100' : 'opacity-60'">
            <div class="flex items-start justify-between gap-4">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">视角 {{ index + 1 }}</p>
                <h3 class="mt-2 text-lg font-bold text-gray-900">{{ slot.label }}</h3>
                <p class="mt-1 text-sm text-gray-500">{{ slot.hint }}</p>
              </div>
              <el-tag :type="slot.objectKey ? 'success' : 'info'" effect="light" round>{{ slot.objectKey ? '已上传' : '待上传' }}</el-tag>
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
            <div class="mt-4 grid grid-cols-2 gap-3">
              <el-button type="primary" plain :icon="Camera" :disabled="slot.uploading || !canOperateSlot(index)" @click="openPicker(slot.key, 'camera', index)">拍照上传</el-button>
              <el-button :icon="Picture" :disabled="slot.uploading || !canOperateSlot(index)" @click="openPicker(slot.key, 'album', index)">相册选择</el-button>
            </div>
          </div>
        </div>
        <div class="flex flex-col gap-3 rounded-3xl border border-amber-100 bg-amber-50 p-5 text-sm text-amber-800 shadow-sm md:flex-row md:items-center md:justify-between">
          <div>建议教师站在教室前方，按左、中、右顺序拍摄，尽量保证学生正脸清晰且无遮挡。</div>
          <div class="flex gap-3">
            <el-button :icon="RefreshRight" @click="resetUploads">清空重传</el-button>
            <el-button type="primary" :loading="recognizing" :disabled="!allUploaded" @click="handleRecognize">开始识别</el-button>
          </div>
        </div>
      </div>

      <!-- STEP 2 -->
      <div v-else-if="step === 2" class="rounded-3xl border border-gray-100 bg-white px-6 py-16 text-center shadow-sm">
        <div class="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-slate-100">
          <div class="h-10 w-10 animate-spin rounded-full border-4 border-slate-200 border-t-slate-700"></div>
        </div>
        <h3 class="mt-6 text-2xl font-bold text-gray-900">正在识别人脸</h3>
        <p class="mt-3 text-sm text-gray-500">照片正在通过 YOLO 检测与 InsightFace 比对中...</p>
      </div>

      <!-- STEP 3 -->
      <div v-else class="space-y-6">
        <!-- 汇总区域 -->
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
            <div class="text-xs text-slate-600">请假 / 未匹配 / 已忽略</div>
            <div class="mt-2 text-2xl font-bold text-slate-800">{{ leaveCount }} / <span class="text-red-500">{{ unmatchedCount }}</span> 个</div>
            <div class="mt-1 text-xs text-slate-400">已忽略 {{ ignoredCount }} 个</div>
          </div>
        </div>

        <!-- 核心重构区：双栏模式 -->
        <div class="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
           
           <!-- 左边栏：原图标注与查看器 -->
           <div class="lg:col-span-7 flex flex-col h-[750px] bg-white border border-gray-100 rounded-3xl shadow-sm overflow-hidden select-none">
              <!-- 查看器工具条 -->
              <div class="flex items-center justify-between p-4 bg-slate-50 border-b border-gray-100">
                  <el-radio-group v-model="activePerspective" @change="switchPerspective" size="default">
                    <el-radio-button label="left">左视角</el-radio-button>
                    <el-radio-button label="center">中视角</el-radio-button>
                    <el-radio-button label="right">右视角</el-radio-button>
                  </el-radio-group>
                  <div class="flex gap-2">
                    <el-switch v-model="showBoxTags" active-text="显示标签" class="mr-4" />
                    <el-button @click="adjustScale(1.1)" circle :icon="Aim" title="放大" />
                    <el-button @click="resetView" :icon="Refresh" circle title="还原视图" />
                  </div>
              </div>
              
              <!-- 画布区 -->
              <div 
                ref="viewportRef"
                class="flex-1 relative bg-slate-900 overflow-hidden cursor-grab active:cursor-grabbing"
                @wheel.prevent="handleWheel"
                @mousedown.prevent="handleMouseDown"
                @mousemove.prevent="handleMouseMove"
                @mouseup.prevent="handleMouseUp"
                @mouseleave.prevent="handleMouseUp"
              >
                  <div v-if="annotationLoading" class="absolute inset-0 z-[80] flex items-center justify-center bg-slate-900/50 backdrop-blur-sm text-white">
                     正在加载标注结果...
                  </div>
                  <div class="absolute right-4 top-4 z-[60] bg-black/60 text-white/90 text-xs px-3 py-1.5 rounded-full pointer-events-none backdrop-blur-md">
                     支持鼠标拖拽平移与滚轮缩放
                  </div>

                  <div v-if="activeImageSource" class="absolute inset-0 flex items-center justify-center">
                    <div
                      class="origin-center transition-transform duration-75"
                      :style="{ transform: `translate(${transform.x}px, ${transform.y}px) scale(${transform.scale})` }"
                    >
                      <div class="relative" :style="{ width: `${stageSize.width}px`, height: `${stageSize.height}px` }">
                        <img :src="activeImageSource" class="h-full w-full object-contain pointer-events-none" @load="handleImageLoad" />

                        <template v-for="box in currentViewBoxes" :key="box.id">
                          <div
                            class="absolute border-[2px] transition-all cursor-pointer group box-border"
                            :class="getBoxClass(box)"
                            :style="getBoxStyle(box)"
                            @click.stop="handleBoxClick(box)"
                          >
                            <div
                              v-show="showBoxTags"
                              class="absolute bottom-[calc(100%+4px)] left-1/2 -translate-x-1/2 bg-black/75 backdrop-blur-sm text-white text-[11px] px-2 py-1 rounded shadow-lg whitespace-nowrap transition-opacity pointer-events-none"
                              :class="box.id === selectedBoxId || getBoxListId(box) === selectedBoxId ? 'opacity-100 z-50 scale-110' : 'opacity-0 group-hover:opacity-100'"
                            >
                              <div class="font-bold mb-0.5">{{ box.name }}</div>
                              <div v-if="box.studentNumber" class="text-white/70">{{ box.studentNumber }}</div>
                              <div class="text-[10px] mt-0.5" :class="box.status === 'low_conf' ? 'text-amber-300' : box.status === 'ignored' || box.status === 'unmatched' ? 'text-slate-300' : 'text-emerald-300'">
                                <span v-if="box.status === 'unmatched'">未匹配课程学生</span>
                                <span v-else-if="box.status === 'ignored'">已忽略</span>
                                <span v-else>识别相似度: {{ formatSimilarity(box.similarityScore) }}</span>
                              </div>
                            </div>
                          </div>
                        </template>
                      </div>
                    </div>
                  </div>

                  <div v-else class="absolute inset-0 flex flex-col items-center justify-center text-slate-400">
                    <el-icon :size="32"><Picture /></el-icon>
                    <p class="mt-3 text-sm">当前视角暂无可展示的标注图片</p>
                  </div>
              </div>
              
              <!-- 底部图例 -->
              <div class="flex items-center gap-6 p-4 bg-white border-t border-gray-100 text-xs text-gray-600 overflow-x-auto whitespace-nowrap">
                  <div class="flex items-center gap-2"><span class="w-3 h-3 rounded bg-emerald-400"></span>普通识别匹配</div>
                  <div class="flex items-center gap-2"><span class="w-3 h-3 rounded bg-yellow-400"></span>低相似度(需确认)</div>
                  <div class="flex items-center gap-2"><span class="w-3 h-3 rounded bg-blue-500"></span>已人工修正</div>
                  <div class="flex items-center gap-2"><span class="w-3 h-3 rounded bg-red-500"></span>未匹配对象</div>
                  <div class="flex items-center gap-2"><span class="w-3 h-3 rounded bg-slate-400"></span>已忽略对象</div>
              </div>
           </div>

           <!-- 右边栏：数据卡片与纠正区 -->
           <div class="lg:col-span-5 flex flex-col h-[750px] bg-white border border-gray-100 rounded-3xl shadow-sm">
             <div class="p-5 border-b border-gray-100 bg-slate-50/50 rounded-t-3xl space-y-4">
                 <div class="flex items-center justify-between">
                    <h3 class="text-lg font-bold text-gray-900">数据结果与人工确认</h3>
                    <el-button type="primary" :icon="Check" :loading="finishing" @click="handleFinishAttendance">提交并结束</el-button>
                 </div>
                 <div class="grid grid-cols-2 lg:grid-cols-3 gap-2">
                    <div class="col-span-2 lg:col-span-3">
                       <el-select v-model="filterAnnotationMode" style="width: 100%" size="large" fit-input-width>
                          <template #prefix><el-icon><MagicStick /></el-icon></template>
                          <el-option label="查看所有检测结果" value="all" />
                          <el-option label="仅看未匹配 (需处理)" value="unmatched" />
                          <el-option label="仅看低相似度 (需确认)" value="low_conf" />
                          <el-option label="仅看异常状态 (缺/迟/假)" value="abnormal" />
                          <el-option label="已手工修正" value="corrected" />
                          <el-option label="已忽略检测框" value="ignored" />
                       </el-select>
                    </div>
                 </div>
             </div>

             <!-- 滚动卡片列表 -->
             <div class="flex-1 overflow-y-auto p-4 space-y-3 bg-slate-50/30">
                 <div v-if="unifiedList.length === 0" class="h-full flex flex-col items-center justify-center text-gray-400 space-y-4">
                    <el-empty description="当前过滤模式下暂无对应结果" />
                 </div>

                 <div 
                   v-for="item in unifiedList" 
                   :key="item.id" 
                   :id="item.id"
                   class="p-4 bg-white rounded-2xl border transition-all cursor-pointer hover:border-slate-300"
                   :class="selectedBoxId === item.id ? 'border-primary shadow-[0_0_0_2px_rgba(59,130,246,0.3)]' : 'border-gray-100 shadow-sm'"
                   @click="handleCardClick(item)"
                 >
                    <!-- 如果是普通绑定学生记录 -->
                    <template v-if="item.type === 'record' && item.record">
                       <div class="flex justify-between items-start mb-4">
                          <div class="flex gap-3 items-center">
                             <div class="w-10 h-10 bg-slate-900 rounded-xl flex items-center justify-center text-white font-bold text-base">
                                {{ item.record.realName?.slice(0,1) || '?' }}
                             </div>
                             <div>
                               <div class="font-bold text-gray-900">{{ item.record.realName }}</div>
                               <div class="text-xs text-gray-500 mt-0.5">学号: {{ item.record.studentNumber }}</div>
                             </div>
                          </div>
                          <div class="flex gap-2">
                             <el-tag v-if="item.box?.status === 'corrected'" size="small" type="info" effect="plain" class="border-blue-200 text-blue-600 bg-blue-50">人工修正</el-tag>
                             <el-tag :type="getStatusTagType(item.record.status)" round>{{ getStatusLabel(item.record.status) }}</el-tag>
                          </div>
                       </div>
                       
                       <div class="flex items-center justify-between text-xs text-slate-500 mb-3 bg-slate-50 p-2 rounded-lg">
                          <div>
                            识别相似度:
                            <span class="font-semibold" :class="item.box?.status === 'low_conf' ? 'text-amber-500' : 'text-emerald-500'">
                              {{ formatSimilarity(item.box?.similarityScore ?? item.record.similarityScore) }}
                            </span>
                          </div>
                          <div>来源视图: {{ getSlot(item.box?.sourceKey || 'center').label }}</div>
                       </div>

                       <!-- 修正区 -->
                       <div class="flex gap-2">
                          <el-select
                            :model-value="item.record.status"
                            class="flex-1"
                            size="small"
                            :loading="updatingRecordId === item.record.recordId"
                            @change="handleStatusSelectChange(item.record!, $event)"
                            @click.stop
                          >
                            <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                          </el-select>
                       </div>
                    </template>

                    <!-- 如果是未匹配或已忽略检测框 -->
                    <template v-if="(item.type === 'unmatched' || item.type === 'ignored') && item.box">
                       <div class="flex justify-between items-start mb-3">
                          <div class="flex gap-3 items-center">
                             <div
                               class="w-10 h-10 rounded-xl flex items-center justify-center text-xl"
                               :class="item.box.status === 'ignored' ? 'bg-slate-100 text-slate-500' : 'bg-red-100 text-red-500'"
                             >
                                ?
                             </div>
                             <div>
                               <div class="font-bold" :class="item.box.status === 'ignored' ? 'text-slate-600' : 'text-red-600'">
                                 {{ item.box.status === 'ignored' ? '已忽略人脸' : '未匹配人脸' }}
                               </div>
                               <div class="text-xs text-slate-500 mt-0.5">
                                 {{ item.box.status === 'ignored' ? '该检测框已落库标记为忽略' : '当前展示数据库中的未匹配检测结果，供教师人工复核' }}
                               </div>
                             </div>
                          </div>
                       </div>
                       
                       <div class="flex items-center justify-between text-xs text-slate-500 p-2 rounded-lg mb-3" :class="item.box.status === 'ignored' ? 'bg-slate-50' : 'bg-red-50/50'">
                          <div>匹配状态: {{ item.box.status === 'ignored' ? '已忽略' : '未匹配到课程学生' }}</div>
                          <div>出现于: {{ getSlot(item.box.sourceKey).label }}</div>
                       </div>

                       <div class="rounded-xl border border-dashed bg-white p-3 text-xs leading-6 text-slate-500" :class="item.box.status === 'ignored' ? 'border-slate-200' : 'border-red-200'">
                          <div>检测框编号: {{ item.box.detectionId }}</div>
                          <div>坐标信息: [{{ item.box.x1 }}, {{ item.box.y1 }}, {{ item.box.x2 }}, {{ item.box.y2 }}]</div>
                          <div v-if="item.box.status === 'ignored'">忽略原因: {{ item.box.ignoreReason || '教师确认忽略' }}</div>
                       </div>

                       <div v-if="item.box.status === 'unmatched'" class="mt-3 grid grid-cols-2 gap-2">
                          <el-button
                            size="small"
                            type="danger"
                            plain
                            :loading="ignoringDetectionId === item.box.detectionId"
                            @click.stop="handleIgnoreBox(item.box)"
                          >
                            忽略该框
                          </el-button>
                          <el-button
                            size="small"
                            type="primary"
                            plain
                            :loading="assigningDetectionId === item.box.detectionId"
                            @click.stop="openAssignDialog(item.box)"
                          >
                            指派学生
                          </el-button>
                       </div>
                    </template>
                 </div>
             </div>
           </div>
        </div>

      </div>
    </template>
  </div>
</template>

<style scoped>
/* Remove tailwind styling that prevents scrolling since it's constrained in the setup */
</style>
