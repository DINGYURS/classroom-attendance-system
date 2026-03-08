<script setup lang="ts">
import { ref, shallowRef, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { RefreshLeft, Upload, ArrowLeft, Warning } from '@element-plus/icons-vue'
import { ElMessage, ElLoading } from 'element-plus'
import { uploadFaceImage, getStudentInfo } from '@/api/student'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const videoRef = ref<HTMLVideoElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)
const stream = shallowRef<MediaStream | null>(null)

const capturedImage = ref<string | null>(null)
const isCameraOpen = ref(false)
const uploading = ref(false)
const cameraError = ref<string | null>(null)

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
      msg = '摄像头正被占用，暂时无法读取。'
    }
    cameraError.value = msg
    ElMessage.error(msg)
  }
}

const stopCamera = () => {
  if (stream.value) {
    stream.value.getTracks().forEach((track) => track.stop())
    stream.value = null
    isCameraOpen.value = false
  }
}

const capturePhoto = () => {
  if (!videoRef.value || !canvasRef.value) {
    return
  }

  const video = videoRef.value
  const canvas = canvasRef.value
  const context = canvas.getContext('2d')

  if (context) {
    canvas.width = video.videoWidth
    canvas.height = video.videoHeight

    context.save()
    context.scale(-1, 1)
    context.drawImage(video, -canvas.width, 0, canvas.width, canvas.height)
    context.restore()

    capturedImage.value = canvas.toDataURL('image/jpeg', 0.95)
  }
}

const retakePhoto = () => {
  capturedImage.value = null
  if (!stream.value || !stream.value.active) {
    void startCamera()
  }
}

const handleUpload = async () => {
  if (!capturedImage.value) {
    return
  }

  const arr = capturedImage.value.split(',')
  if (arr.length < 2) {
    ElMessage.error('照片数据无效')
    return
  }

  const header = arr[0] || ''
  const payload = arr[1]
  if (!payload) {
    ElMessage.error('照片数据无效')
    return
  }

  const matchResult = header.match(/:(.*?);/)
  const mime = matchResult?.[1] || 'image/jpeg'
  const binary = atob(payload)
  let n = binary.length
  const u8arr = new Uint8Array(n)

  while (n--) {
    u8arr[n] = binary.charCodeAt(n)
  }

  const file = new File([u8arr], 'mobile_face_register.jpg', { type: mime })
  const formData = new FormData()
  formData.append('file', file)

  const loading = ElLoading.service({
    lock: true,
    text: '正在上传人脸数据...',
    background: 'rgba(255, 255, 255, 0.8)',
    customClass: 'modern-loading'
  })

  try {
    uploading.value = true
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
  } catch (error) {
    console.error(error)
  } finally {
    loading.close()
    uploading.value = false
  }
}

onMounted(() => {
  setTimeout(() => {
    void startCamera()
  }, 300)
})

onUnmounted(() => {
  stopCamera()
})
</script>

<template>
  <div class="absolute inset-0 z-50 mx-auto flex h-screen w-full flex-col items-center justify-between overflow-hidden bg-gray-50">
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
          {{ capturedImage ? '确认照片' : '请正对摄像头' }}
        </h2>
        <p class="mx-auto max-w-[280px] text-sm leading-relaxed text-gray-500">
          {{ capturedImage ? '确认当前照片无误后上传，作为你的人脸底库。' : '请保持面部居中、光线充足，并确保五官清晰可见。' }}
        </p>
      </div>

      <div class="relative flex-none transform transition-transform duration-300">
        <div
          v-if="cameraError && !capturedImage"
          class="mx-auto flex h-72 w-72 flex-col items-center justify-center rounded-full border-4 border-dashed border-gray-300 bg-gray-100 p-8 text-center text-gray-400 shadow-inner"
        >
          <el-icon :size="48" class="mb-3 text-gray-300"><Warning /></el-icon>
          <span class="text-sm">{{ cameraError }}</span>
          <el-button type="primary" link class="mt-4" @click="startCamera">重试</el-button>
        </div>

        <div
          v-else
          class="camera-lens relative mx-auto flex h-72 w-72 items-center justify-center overflow-hidden rounded-full bg-gray-200 shadow-lg ring-4 ring-white md:h-80 md:w-80"
        >
          <div
            v-if="!capturedImage && isCameraOpen"
            class="pointer-events-none absolute inset-0 z-20 animate-pulse rounded-full border-2 border-blue-400 opacity-50"
          ></div>

          <video
            v-show="!capturedImage"
            ref="videoRef"
            autoplay
            playsinline
            muted
            class="h-full w-full scale-x-[-1] object-cover"
          ></video>

          <img
            v-if="capturedImage"
            :src="capturedImage"
            class="z-10 h-full w-full object-cover"
          />

          <div
            v-if="!capturedImage && isCameraOpen"
            class="pointer-events-none absolute inset-0 z-10 flex items-center justify-center mix-blend-overlay opacity-30"
          >
            <svg viewBox="0 0 200 200" width="80%" height="80%" fill="none">
              <ellipse cx="100" cy="90" rx="60" ry="75" stroke="#ffffff" stroke-width="2" stroke-dasharray="6 6" />
              <path d="M40 180 Q100 130 160 180" stroke="#ffffff" stroke-width="2" stroke-dasharray="6 6" />
            </svg>
          </div>
        </div>

        <div class="absolute left-1/2 -bottom-6 h-4 w-48 -translate-x-1/2 rounded-full bg-black/5 blur-xl"></div>
      </div>
    </main>

    <footer class="relative z-20 mx-auto flex h-40 w-full max-w-lg shrink-0 flex-col items-center justify-center rounded-t-3xl bg-white px-6 pt-6 pb-10 shadow-[0_-4px_20px_rgba(0,0,0,0.04)]">
      <template v-if="!capturedImage">
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
      </template>

      <template v-else>
        <div class="translate-y-2 flex w-full items-center justify-around">
          <button class="group flex flex-col items-center justify-center outline-none" @click="retakePhoto">
            <div class="mb-2 flex h-14 w-14 items-center justify-center rounded-full bg-gray-100 text-gray-600 transition-all group-active:scale-90 group-hover:bg-gray-200">
              <el-icon :size="22"><RefreshLeft /></el-icon>
            </div>
            <span class="text-sm font-medium text-gray-600">重新拍摄</span>
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