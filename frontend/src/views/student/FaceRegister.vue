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

// Use shallowRef to avoid Vue proxy issues with MediaStream
const stream = shallowRef<MediaStream | null>(null)

const capturedImage = ref<string | null>(null)
const isCameraOpen = ref(false)
const uploading = ref(false)
const cameraError = ref<string | null>(null)

// Start Camera
const startCamera = async () => {
  cameraError.value = null
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    cameraError.value = '您的浏览器不支持摄像头访问，请使用 Chrome/Edge/Safari 并确保在 HTTPS 下运行。'
    ElMessage.error(cameraError.value)
    return
  }

  try {
    // Mobile optimization: prefer front camera ('user')
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
        ElMessage.warning('视频未自动播放，请点按屏幕或检查设置')
      }
    }
  } catch (err: any) {
    console.error('Camera start error:', err)
    let msg = '无法启动摄像头'
    if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
      msg = '访问被拒绝：请允许浏览器访问摄像头'
    } else if (err.name === 'NotFoundError' || err.name === 'DevicesNotFoundError') {
      msg = '未检测到摄像头设备'
    } else if (err.name === 'NotReadableError' || err.name === 'TrackStartError') {
      msg = '摄像头可能正在被其他应用占用'
    }
    cameraError.value = msg
    ElMessage.error(msg)
  }
}

const stopCamera = () => {
  if (stream.value) {
    stream.value.getTracks().forEach(track => track.stop())
    stream.value = null
    isCameraOpen.value = false
  }
}

const capturePhoto = () => {
  if (!videoRef.value || !canvasRef.value) return
  
  const video = videoRef.value
  const canvas = canvasRef.value
  const context = canvas.getContext('2d')
  
  if (context) {
    // Make target canvas be a square to match the circular mask perfectly if desired,
    // or keep full frame. We keep full frame but center-cropped later when displayed.
    // However, it's best to save the full 16:9 or 4:3 shot so recognition backend gets whole face.
    canvas.width = video.videoWidth
    canvas.height = video.videoHeight
    
    context.save()
    // Mirror the image horizontally to match user experience (like a mirror)
    context.scale(-1, 1)
    context.drawImage(video, -canvas.width, 0, canvas.width, canvas.height)
    context.restore()
    
    // High quality JPEG
    capturedImage.value = canvas.toDataURL('image/jpeg', 0.95) 
  }
}

const retakePhoto = () => {
  capturedImage.value = null
  if (!stream.value || !stream.value.active) {
    startCamera()
  }
}

const handleUpload = async () => {
  if (!capturedImage.value) return
  
  // Create non-null assertion or cast for TS
  const base64Data = capturedImage.value as string
  const arr = base64Data.split(',')
  if (arr.length < 2) {
      ElMessage.error('照片数据无效')
      return;
  }
  
  const matchResult = arr[0].match(/:(.*?);/)
  const mime = matchResult ? matchResult[1] : 'image/jpeg'
  const bstr = atob(arr[1])
  let n = bstr.length
  
  const u8arr = new Uint8Array(n)
  while (n--) {
    u8arr[n] = bstr.charCodeAt(n)
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
      ElMessage.success('人脸录入成功！')
      
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
    startCamera()
  }, 300)
})

onUnmounted(() => {
  stopCamera()
})
</script>

<template>
  <div class="h-screen w-full bg-gray-50 flex flex-col items-center justify-between mx-auto overflow-hidden absolute inset-0 z-50">
    <!-- Header Navigation -->
    <header class="w-full h-14 md:h-16 bg-white flex items-center justify-between px-4 shadow-xs shrink-0 relative z-20">
      <el-button link class="text-gray-600 hover:text-gray-900 px-2" @click="router.back()">
        <el-icon :size="24"><ArrowLeft /></el-icon>
      </el-button>
      <span class="text-gray-800 font-bold text-lg leading-none absolute left-1/2 -translate-x-1/2">
        人脸信息采集
      </span>
      <div class="w-10"></div> <!-- Placeholder for centering -->
    </header>

    <!-- Main Content / Camera Area -->
    <main class="flex-1 w-full flex flex-col items-center justify-center relative px-6 md:px-0 mt-4 max-w-lg mb-4">
      
      <!-- Instructions Title -->
      <div class="text-center mb-8 shrink-0">
        <h2 class="text-xl font-bold text-gray-800 mb-2">
          {{ capturedImage ? '确认人脸信息' : '请正对摄像头' }}
        </h2>
        <p class="text-sm text-gray-500 max-w-[280px] mx-auto leading-relaxed">
           {{ capturedImage ? '请确认照片清晰、面部完整' : '请将面部置于圆形取景框中，并保持光线充足' }}
        </p>
      </div>

      <!-- Circular Camera Viewport -->
      <div class="relative flex-none transform transition-transform duration-300">
        <!-- Error State Fallback -->
        <div v-if="cameraError && !capturedImage" class="w-72 h-72 rounded-full border-4 border-dashed border-gray-300 bg-gray-100 flex flex-col items-center justify-center text-gray-400 p-8 text-center mx-auto shadow-inner">
           <el-icon :size="48" class="text-gray-300 mb-3"><Warning /></el-icon>
           <span class="text-sm">{{ cameraError }}</span>
           <el-button type="primary" link class="mt-4" @click="startCamera">重试</el-button>
        </div>

        <!-- Video & Masking -->
        <div v-else class="camera-lens mx-auto w-72 h-72 md:w-80 md:h-80 rounded-full overflow-hidden bg-gray-200 flex items-center justify-center relative shadow-lg ring-4 ring-white">
          
          <!-- Outer border glow effect (scanning) -->
          <div v-if="!capturedImage && isCameraOpen" class="absolute inset-0 rounded-full border-2 border-blue-400 opacity-50 animate-pulse z-20 pointer-events-none"></div>

          <!-- The Video Feed -->
          <video
            v-show="!capturedImage"
            ref="videoRef"
            autoplay
            playsinline
            muted
            class="w-full h-full object-cover scale-x-[-1]"
          ></video>

          <!-- Captured Image Preview -->
          <img
            v-if="capturedImage"
            :src="capturedImage"
            class="w-full h-full object-cover z-10"
          />
          
          <!-- Silhouette Guide Pattern overlay -->
          <div v-if="!capturedImage && isCameraOpen" class="absolute z-10 inset-0 pointer-events-none flex items-center justify-center mix-blend-overlay opacity-30">
            <svg viewBox="0 0 200 200" width="80%" height="80%" fill="none">
               <ellipse cx="100" cy="90" rx="60" ry="75" stroke="#ffffff" stroke-width="2" stroke-dasharray="6 6" />
               <path d="M40 180 Q100 130 160 180" stroke="#ffffff" stroke-width="2" stroke-dasharray="6 6" />
            </svg>
          </div>
        </div>
        
        <!-- Subtle shadow under the lens -->
        <div class="w-48 h-4 bg-black/5 blur-xl rounded-full absolute -bottom-6 left-1/2 -translate-x-1/2"></div>
      </div>

    </main>

    <!-- Bottom Controls Area -->
    <footer class="w-full bg-white rounded-t-3xl shadow-[0_-4px_20px_rgba(0,0,0,0.04)] px-6 pt-6 pb-10 flex flex-col items-center justify-center shrink-0 max-w-lg mx-auto h-40 relative z-20">
      
      <!-- Capture Mode -->
      <template v-if="!capturedImage">
        <button 
          class="capture-btn relative flex items-center justify-center group" 
          @click="capturePhoto" 
          :disabled="!isCameraOpen"
          :class="{'opacity-50 cursor-not-allowed': !isCameraOpen}"
        >
          <!-- Outer Ring -->
          <div class="w-20 h-20 rounded-full border-[5px] border-blue-100 flex items-center justify-center transition-transform duration-200 group-active:scale-95">
             <!-- Inner Button -->
             <div class="w-[60px] h-[60px] rounded-full bg-blue-500 shadow-md flex items-center justify-center transition-all duration-200 group-hover:bg-blue-600"></div>
          </div>
        </button>
        <span class="text-xs text-gray-400 mt-4 font-medium tracking-wide text-center">点击按钮拍照</span>
      </template>

      <!-- Review Mode -->
      <template v-else>
        <div class="w-full flex items-center justify-around translate-y-2">
          
          <button class="flex flex-col items-center justify-center group outline-none" @click="retakePhoto">
            <div class="w-14 h-14 rounded-full bg-gray-100 text-gray-600 flex items-center justify-center mb-2 transition-all group-active:scale-90 group-hover:bg-gray-200">
               <el-icon :size="22"><RefreshLeft /></el-icon>
            </div>
            <span class="text-sm font-medium text-gray-600">重拍照片</span>
          </button>
          
          <button 
            class="flex flex-col items-center justify-center group outline-none relative" 
            @click="handleUpload" 
            :disabled="uploading"
          >
            <div class="w-[72px] h-[72px] rounded-full bg-blue-500 text-white flex items-center justify-center mb-2 transition-all shadow-lg shadow-blue-500/30 group-active:scale-90 group-hover:bg-blue-600" :class="{'opacity-80': uploading}">
               <el-icon :size="28" v-if="!uploading"><Upload /></el-icon>
               <span v-else class="upload-spinner border-t-2 border-white border-solid rounded-full w-6 h-6 animate-spin"></span>
            </div>
            <span class="text-sm font-bold text-blue-600 absolute -bottom-6 whitespace-nowrap">
              {{ uploading ? '正在上传...' : '确认使用本照片' }}
            </span>
          </button>
          
        </div>
      </template>

    </footer>

    <!-- Hidden Canvas -->
    <canvas ref="canvasRef" class="hidden"></canvas>
  </div>
</template>

<style>
/* Global fix applied specifically to the loading overlay inside this component context */
.modern-loading .el-loading-spinner .path {
  stroke: #3b82f6 !important; /* Blue-500 */
}
.modern-loading .el-loading-text {
  color: #1f2937 !important; /* Gray-800 */
  font-weight: 600;
  margin-top: 10px;
}
</style>