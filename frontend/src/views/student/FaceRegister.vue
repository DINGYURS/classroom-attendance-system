<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, ref, shallowRef, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Picture, RefreshLeft, Upload, Warning } from '@element-plus/icons-vue'
import { ElLoading, ElMessage } from 'element-plus'
import { getStudentInfo, uploadFaceImage } from '@/api/student'
import { useAuthStore } from '@/stores/auth'

type ImageSource = 'camera' | 'album' | null

type PointerState = {
  x: number
  y: number
}

const router = useRouter()
const authStore = useAuthStore()

const videoRef = ref<HTMLVideoElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const albumInputRef = ref<HTMLInputElement | null>(null)
const editorFrameRef = ref<HTMLDivElement | null>(null)
const stream = shallowRef<MediaStream | null>(null)

const previewImage = ref<string | null>(null)
const selectedFile = ref<File | null>(null)
const selectedFileName = ref('')
const selectedSource = ref<ImageSource>(null)
const isCameraOpen = ref(false)
const uploading = ref(false)
const cameraError = ref<string | null>(null)

const editorSize = ref(288)
const imageWidth = ref(0)
const imageHeight = ref(0)
const fitScale = ref(1)
const zoom = ref(1)
const offsetX = ref(0)
const offsetY = ref(0)
const dragging = ref(false)
const activePointerId = ref<number | null>(null)
const dragStartX = ref(0)
const dragStartY = ref(0)
const dragOriginX = ref(0)
const dragOriginY = ref(0)

const pointerMap = new Map<number, PointerState>()
const pinchStartZoom = ref(1)
const pinchStartOffsetX = ref(0)
const pinchStartOffsetY = ref(0)
const pinchStartDistance = ref(0)
const pinchStartCenterX = ref(0)
const pinchStartCenterY = ref(0)

const minZoom = 1
const maxZoom = 3
const outputImageSize = 1024

const isAlbumMode = computed(() => selectedSource.value === 'album' && !!previewImage.value)
const renderWidth = computed(() => imageWidth.value * fitScale.value * zoom.value)
const renderHeight = computed(() => imageHeight.value * fitScale.value * zoom.value)
const imageTransformStyle = computed(() => {
  if (!isAlbumMode.value) {
    return {}
  }

  return {
    width: `${renderWidth.value}px`,
    height: `${renderHeight.value}px`,
    transform: `translate(calc(-50% + ${offsetX.value}px), calc(-50% + ${offsetY.value}px))`
  }
})

const clamp = (value: number, min: number, max: number) => Math.min(max, Math.max(min, value))

const syncEditorSize = () => {
  if (editorFrameRef.value) {
    editorSize.value = editorFrameRef.value.clientWidth || 288
  }
}

const revokePreview = (url?: string | null) => {
  if (url?.startsWith('blob:')) {
    URL.revokeObjectURL(url)
  }
}

const resetGestureState = () => {
  dragging.value = false
  activePointerId.value = null
  pointerMap.clear()
}

const resetEditorState = () => {
  imageWidth.value = 0
  imageHeight.value = 0
  fitScale.value = 1
  zoom.value = 1
  offsetX.value = 0
  offsetY.value = 0
  resetGestureState()
}

const resetSelectedImage = () => {
  revokePreview(previewImage.value)
  previewImage.value = null
  selectedFile.value = null
  selectedFileName.value = ''
  selectedSource.value = null
  resetEditorState()
}

const constrainOffsets = (nextX: number, nextY: number) => {
  if (!isAlbumMode.value) {
    return { x: 0, y: 0 }
  }

  const maxX = Math.max(0, (renderWidth.value - editorSize.value) / 2)
  const maxY = Math.max(0, (renderHeight.value - editorSize.value) / 2)

  return {
    x: clamp(nextX, -maxX, maxX),
    y: clamp(nextY, -maxY, maxY)
  }
}

const initializeAlbumEditor = () => {
  syncEditorSize()
  if (!imageWidth.value || !imageHeight.value || !editorSize.value) {
    return
  }

  fitScale.value = Math.max(editorSize.value / imageWidth.value, editorSize.value / imageHeight.value)
  zoom.value = 1
  offsetX.value = 0
  offsetY.value = 0
  resetGestureState()
}

const getImageSize = (url: string) => new Promise<{ width: number; height: number }>((resolve, reject) => {
  const image = new Image()
  image.onload = () => {
    resolve({
      width: image.naturalWidth,
      height: image.naturalHeight
    })
  }
  image.onerror = () => reject(new Error('图片加载失败'))
  image.src = url
})

const getPointerPosition = (event: PointerEvent): PointerState => {
  const rect = editorFrameRef.value?.getBoundingClientRect()
  if (!rect) {
    return { x: 0, y: 0 }
  }

  return {
    x: event.clientX - rect.left - rect.width / 2,
    y: event.clientY - rect.top - rect.height / 2
  }
}

const getPointerPair = () => Array.from(pointerMap.values()).slice(0, 2)

const getCenterPoint = (first: PointerState, second: PointerState) => ({
  x: (first.x + second.x) / 2,
  y: (first.y + second.y) / 2
})

const getDistance = (first: PointerState, second: PointerState) => {
  const deltaX = second.x - first.x
  const deltaY = second.y - first.y
  return Math.hypot(deltaX, deltaY)
}

const beginDrag = (pointerId: number, point: PointerState) => {
  dragging.value = true
  activePointerId.value = pointerId
  dragStartX.value = point.x
  dragStartY.value = point.y
  dragOriginX.value = offsetX.value
  dragOriginY.value = offsetY.value
}

const beginPinch = () => {
  const [first, second] = getPointerPair()
  if (!first || !second) {
    return
  }

  const center = getCenterPoint(first, second)
  pinchStartCenterX.value = center.x
  pinchStartCenterY.value = center.y
  pinchStartDistance.value = Math.max(getDistance(first, second), 1)
  pinchStartZoom.value = zoom.value
  pinchStartOffsetX.value = offsetX.value
  pinchStartOffsetY.value = offsetY.value
  dragging.value = false
  activePointerId.value = null
}

const startCamera = async () => {
  cameraError.value = null
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    cameraError.value = '当前浏览器或运行环境不支持摄像头调用。'
    ElMessage.error(cameraError.value)
    return
  }

  try {
    const constraints = {
      video: {
        facingMode: 'user',
        width: { ideal: 1280 },
        height: { ideal: 720 }
      },
      audio: false
    }

    stream.value = await navigator.mediaDevices.getUserMedia(constraints)
    await nextTick()

    if (videoRef.value && stream.value) {
      videoRef.value.srcObject = stream.value
      try {
        await videoRef.value.play()
        isCameraOpen.value = true
      } catch (playError) {
        console.error('Auto-play failed:', playError)
        ElMessage.warning('如果摄像头未自动启动，请点击预览区域后重试。')
      }
    }
  } catch (err: any) {
    console.error('Camera start error:', err)
    let msg = '打开摄像头失败。'
    if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
      msg = '摄像头权限被拒绝。'
    } else if (err.name === 'NotFoundError' || err.name === 'DevicesNotFoundError') {
      msg = '未检测到可用摄像头设备。'
    } else if (err.name === 'NotReadableError' || err.name === 'TrackStartError') {
      msg = '摄像头正在被占用，暂时无法读取。'
    }
    cameraError.value = msg
    ElMessage.error(msg)
  }
}

const stopCamera = () => {
  if (stream.value) {
    stream.value.getTracks().forEach((track) => track.stop())
    stream.value = null
  }
  isCameraOpen.value = false
}

const openAlbumPicker = () => {
  albumInputRef.value?.click()
}

const applyAlbumFile = async (file: File) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请选择图片文件')
    return
  }

  const previewUrl = URL.createObjectURL(file)

  try {
    const size = await getImageSize(previewUrl)
    resetSelectedImage()
    stopCamera()

    previewImage.value = previewUrl
    selectedFile.value = file
    selectedFileName.value = file.name
    selectedSource.value = 'album'
    imageWidth.value = size.width
    imageHeight.value = size.height

    await nextTick()
    initializeAlbumEditor()
  } catch (error) {
    revokePreview(previewUrl)
    console.error(error)
    ElMessage.error('图片加载失败，请重新选择')
  }
}

const handleAlbumSelected = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]

  input.value = ''

  if (!file) {
    return
  }

  await applyAlbumFile(file)
}

const capturePhoto = async () => {
  if (!videoRef.value || !canvasRef.value) {
    return
  }

  const video = videoRef.value
  const canvas = canvasRef.value
  const context = canvas.getContext('2d')

  if (!context || !video.videoWidth || !video.videoHeight) {
    ElMessage.warning('摄像头画面尚未准备完成，请稍后重试')
    return
  }

  canvas.width = video.videoWidth
  canvas.height = video.videoHeight

  context.save()
  context.scale(-1, 1)
  context.drawImage(video, -canvas.width, 0, canvas.width, canvas.height)
  context.restore()

  const blob = await new Promise<Blob | null>((resolve) => {
    canvas.toBlob(resolve, 'image/jpeg', 0.95)
  })

  if (!blob) {
    ElMessage.error('照片生成失败，请重试')
    return
  }

  const file = new File([blob], 'mobile_face_register.jpg', { type: 'image/jpeg' })
  const previewUrl = URL.createObjectURL(file)

  resetSelectedImage()
  stopCamera()

  previewImage.value = previewUrl
  selectedFile.value = file
  selectedFileName.value = file.name
  selectedSource.value = 'camera'
}

const handleEditorPointerDown = (event: PointerEvent) => {
  if (!isAlbumMode.value) {
    return
  }

  const point = getPointerPosition(event)
  pointerMap.set(event.pointerId, point)
  ;(event.currentTarget as HTMLElement)?.setPointerCapture?.(event.pointerId)

  if (pointerMap.size === 1) {
    beginDrag(event.pointerId, point)
    return
  }

  if (pointerMap.size >= 2) {
    beginPinch()
  }
}

const handleEditorPointerMove = (event: PointerEvent) => {
  if (!isAlbumMode.value || !pointerMap.has(event.pointerId)) {
    return
  }

  const point = getPointerPosition(event)
  pointerMap.set(event.pointerId, point)

  if (pointerMap.size >= 2) {
    event.preventDefault()
    const [first, second] = getPointerPair()
    if (!first || !second) {
      return
    }

    const currentCenter = getCenterPoint(first, second)
    const currentDistance = Math.max(getDistance(first, second), 1)
    const nextZoom = clamp(pinchStartZoom.value * (currentDistance / pinchStartDistance.value), minZoom, maxZoom)
    const startScale = fitScale.value * pinchStartZoom.value
    const nextScale = fitScale.value * nextZoom
    const imageAnchorX = (pinchStartCenterX.value - pinchStartOffsetX.value) / startScale
    const imageAnchorY = (pinchStartCenterY.value - pinchStartOffsetY.value) / startScale
    const nextOffset = constrainOffsets(
      currentCenter.x - imageAnchorX * nextScale,
      currentCenter.y - imageAnchorY * nextScale
    )

    zoom.value = nextZoom
    offsetX.value = nextOffset.x
    offsetY.value = nextOffset.y
    return
  }

  if (dragging.value && activePointerId.value === event.pointerId) {
    event.preventDefault()
    const moved = constrainOffsets(
      dragOriginX.value + (point.x - dragStartX.value),
      dragOriginY.value + (point.y - dragStartY.value)
    )
    offsetX.value = moved.x
    offsetY.value = moved.y
  }
}

const endPointerInteraction = (event: PointerEvent) => {
  pointerMap.delete(event.pointerId)
  ;(event.currentTarget as HTMLElement)?.releasePointerCapture?.(event.pointerId)

  if (pointerMap.size >= 2) {
    beginPinch()
    return
  }

  if (pointerMap.size === 1) {
    const remainingPointer = Array.from(pointerMap.entries())[0]
    if (remainingPointer) {
      const [remainingId, point] = remainingPointer
      beginDrag(remainingId, point)
    }
    return
  }

  resetGestureState()
}

const renderAdjustedAlbumFile = async () => {
  if (!previewImage.value || !isAlbumMode.value) {
    return selectedFile.value
  }

  const image = await new Promise<HTMLImageElement>((resolve, reject) => {
    const element = new Image()
    element.onload = () => resolve(element)
    element.onerror = () => reject(new Error('调整后的图片生成失败'))
    element.src = previewImage.value || ''
  })

  const exportCanvas = document.createElement('canvas')
  exportCanvas.width = outputImageSize
  exportCanvas.height = outputImageSize

  const context = exportCanvas.getContext('2d')
  if (!context) {
    throw new Error('图片处理上下文创建失败')
  }

  const drawWidth = (renderWidth.value / editorSize.value) * outputImageSize
  const drawHeight = (renderHeight.value / editorSize.value) * outputImageSize
  const centerX = outputImageSize / 2 + (offsetX.value / editorSize.value) * outputImageSize
  const centerY = outputImageSize / 2 + (offsetY.value / editorSize.value) * outputImageSize
  const drawX = centerX - drawWidth / 2
  const drawY = centerY - drawHeight / 2

  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, outputImageSize, outputImageSize)
  context.drawImage(image, drawX, drawY, drawWidth, drawHeight)

  const blob = await new Promise<Blob | null>((resolve) => {
    exportCanvas.toBlob(resolve, 'image/jpeg', 0.95)
  })

  if (!blob) {
    throw new Error('调整后的图片导出失败')
  }

  const baseName = selectedFileName.value.replace(/\.[^.]+$/, '') || 'face-register'
  return new File([blob], `${baseName}-adjusted.jpg`, { type: 'image/jpeg' })
}

const retakePhoto = () => {
  resetSelectedImage()
  void startCamera()
}

const handleUpload = async () => {
  if (!selectedFile.value) {
    return
  }

  const loading = ElLoading.service({
    lock: true,
    text: '正在上传人脸数据...',
    background: 'rgba(255, 255, 255, 0.8)',
    customClass: 'modern-loading'
  })

  try {
    uploading.value = true

    const uploadFile = selectedSource.value === 'album'
      ? await renderAdjustedAlbumFile()
      : selectedFile.value

    if (!uploadFile) {
      throw new Error('待上传图片不存在')
    }

    const formData = new FormData()
    formData.append('file', uploadFile)

    const res: any = await uploadFaceImage(formData)

    if (res && res.code === 1) {
      ElMessage.success('人脸录入完成')

      const infoRes: any = await getStudentInfo()
      if (infoRes && infoRes.code === 1 && infoRes.data) {
        authStore.updateUserInfo({
          avatarUrl: infoRes.data.avatarUrl
        })
      }

      setTimeout(() => router.replace('/student/profile'), 1000)
    } else {
      ElMessage.error(res?.message || '人脸录入失败')
    }
  } catch (error: any) {
    console.error(error)
    ElMessage.error(error?.message || '人脸录入失败')
  } finally {
    loading.close()
    uploading.value = false
  }
}

watch([zoom, renderWidth, renderHeight], () => {
  if (!isAlbumMode.value) {
    return
  }

  const constrained = constrainOffsets(offsetX.value, offsetY.value)
  offsetX.value = constrained.x
  offsetY.value = constrained.y
})

const handleResize = () => {
  syncEditorSize()
  if (isAlbumMode.value) {
    initializeAlbumEditor()
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  setTimeout(() => {
    void startCamera()
  }, 300)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  resetSelectedImage()
  stopCamera()
})
</script>

<template>
  <div class="absolute inset-0 z-50 mx-auto flex h-screen w-full flex-col items-center justify-between overflow-hidden bg-gray-50">
    <input
      ref="albumInputRef"
      type="file"
      accept="image/*"
      class="hidden"
      @change="handleAlbumSelected"
    />

    <header class="relative z-20 flex h-14 w-full shrink-0 items-center justify-between bg-white px-4 shadow-xs md:h-16">
      <el-button link class="px-2 text-gray-600 hover:text-gray-900" @click="router.back()">
        <el-icon :size="24"><ArrowLeft /></el-icon>
      </el-button>
      <span class="absolute left-1/2 -translate-x-1/2 text-lg font-bold leading-none text-gray-800">
        人脸录入
      </span>
      <div class="w-10"></div>
    </header>

    <main class="relative mb-4 mt-4 flex w-full max-w-lg flex-1 flex-col items-center justify-center px-6 md:px-0">
      <div class="mb-8 shrink-0 text-center">
        <h2 class="mb-2 text-xl font-bold text-gray-800">
          {{ previewImage ? '确认照片' : '请正对摄像头' }}
        </h2>
        <p class="mx-auto max-w-[320px] text-sm leading-relaxed text-gray-500">
          {{ previewImage ? '确认当前照片无误后上传，作为你的人脸底库。' : '你可以直接拍照，也可以从手机相册导入照片。请保证面部居中、光线充足，五官清晰可见。' }}
        </p>
      </div>

      <div class="relative flex-none transform transition-transform duration-300">
        <div
          v-if="cameraError && !previewImage"
          class="mx-auto flex h-72 w-72 flex-col items-center justify-center rounded-full border-4 border-dashed border-gray-300 bg-gray-100 p-8 text-center text-gray-400 shadow-inner"
        >
          <el-icon :size="48" class="mb-3 text-gray-300"><Warning /></el-icon>
          <span class="text-sm">{{ cameraError }}</span>
          <el-button type="primary" link class="mt-4" @click="startCamera">重试</el-button>
        </div>

        <div
          v-else
          ref="editorFrameRef"
          class="camera-lens relative mx-auto flex h-72 w-72 items-center justify-center overflow-hidden rounded-full bg-gray-200 shadow-lg ring-4 ring-white md:h-80 md:w-80"
          :class="isAlbumMode ? 'touch-none cursor-grab active:cursor-grabbing' : ''"
          @pointerdown="handleEditorPointerDown"
          @pointermove="handleEditorPointerMove"
          @pointerup="endPointerInteraction"
          @pointercancel="endPointerInteraction"
        >
          <video
            v-show="!previewImage"
            ref="videoRef"
            autoplay
            playsinline
            muted
            class="h-full w-full scale-x-[-1] object-cover"
          ></video>

          <template v-if="previewImage && isAlbumMode">
            <img
              :src="previewImage"
              :style="imageTransformStyle"
              class="absolute left-1/2 top-1/2 max-w-none select-none object-cover"
              draggable="false"
              alt="人脸预览"
            />
          </template>

          <img
            v-else-if="previewImage"
            :src="previewImage"
            class="z-10 h-full w-full object-cover"
            alt="人脸预览"
          />

          <div
            v-if="!previewImage && isCameraOpen"
            class="pointer-events-none absolute inset-0 z-20 animate-pulse rounded-full border-2 border-blue-400 opacity-50"
          ></div>

          <div class="pointer-events-none absolute inset-0 z-30 flex items-center justify-center mix-blend-overlay opacity-40">
            <svg viewBox="0 0 200 200" width="80%" height="80%" fill="none">
              <ellipse cx="100" cy="90" rx="60" ry="75" stroke="#ffffff" stroke-width="2" stroke-dasharray="6 6" />
              <path d="M40 180 Q100 130 160 180" stroke="#ffffff" stroke-width="2" stroke-dasharray="6 6" />
            </svg>
          </div>
        </div>

        <div class="absolute left-1/2 -bottom-6 h-4 w-48 -translate-x-1/2 rounded-full bg-black/5 blur-xl"></div>
      </div>

      <div v-if="previewImage" class="mt-8 w-full max-w-sm space-y-3 text-center">
        <p class="text-xs text-gray-400">{{ selectedFileName }}</p>

        <div v-if="isAlbumMode" class="rounded-2xl bg-white/90 px-4 py-4 shadow-sm ring-1 ring-gray-100">
          <p class="text-xs leading-5 text-gray-500">
            单指拖动位置，双指缩放大小，让人脸尽量贴合虚线区域后再上传。
          </p>
        </div>
      </div>
    </main>

    <footer class="relative z-20 mx-auto flex min-h-40 w-full max-w-lg shrink-0 flex-col items-center justify-center rounded-t-3xl bg-white px-6 pb-10 pt-6 shadow-[0_-4px_20px_rgba(0,0,0,0.04)]">
      <template v-if="!previewImage">
        <button
          class="capture-btn group relative flex items-center justify-center"
          :disabled="!isCameraOpen"
          :class="{ 'cursor-not-allowed opacity-50': !isCameraOpen }"
          @click="capturePhoto"
        >
          <div class="flex h-20 w-20 items-center justify-center rounded-full border-[5px] border-blue-100 transition-transform duration-200 group-active:scale-95">
            <div class="h-[60px] w-[60px] rounded-full bg-blue-500 shadow-md transition-all duration-200 group-hover:bg-blue-600"></div>
          </div>
        </button>
        <span class="mt-4 text-center text-xs font-medium tracking-wide text-gray-400">点击拍照</span>
        <el-button class="mt-5" :icon="Picture" @click="openAlbumPicker">
          从相册导入
        </el-button>
      </template>

      <template v-else>
        <div class="translate-y-2 flex w-full items-center justify-around">
          <button class="group flex flex-col items-center justify-center outline-none" @click="retakePhoto">
            <div class="mb-2 flex h-14 w-14 items-center justify-center rounded-full bg-gray-100 text-gray-600 transition-all group-active:scale-90 group-hover:bg-gray-200">
              <el-icon :size="22"><RefreshLeft /></el-icon>
            </div>
            <span class="text-sm font-medium text-gray-600">重新选择</span>
          </button>

          <button
            class="group relative flex flex-col items-center justify-center outline-none"
            :disabled="uploading"
            @click="handleUpload"
          >
            <div class="mb-2 flex h-[72px] w-[72px] items-center justify-center rounded-full bg-blue-500 text-white shadow-lg shadow-blue-500/30 transition-all group-active:scale-90 group-hover:bg-blue-600" :class="{ 'opacity-80': uploading }">
              <el-icon v-if="!uploading" :size="28"><Upload /></el-icon>
              <span v-else class="upload-spinner h-6 w-6 animate-spin rounded-full border-t-2 border-white border-solid"></span>
            </div>
            <span class="absolute -bottom-6 whitespace-nowrap text-sm font-bold text-blue-600">
              {{ uploading ? '上传中...' : '确认上传' }}
            </span>
          </button>
        </div>
      </template>
    </footer>

    <canvas ref="canvasRef" class="hidden"></canvas>
  </div>
</template>

<style>
.modern-loading .el-loading-spinner .path {
  stroke: #3b82f6 !important;
}

.modern-loading .el-loading-text {
  color: #1f2937 !important;
  font-weight: 600;
  margin-top: 10px;
}
</style>
