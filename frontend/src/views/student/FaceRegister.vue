<script setup lang="ts">
import { ref, shallowRef, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Camera, RefreshLeft, Upload, Back } from '@element-plus/icons-vue'
import { ElMessage, ElLoading } from 'element-plus'
import { uploadFaceImage } from '@/api/student' // 确保你的 API 路径正确

const router = useRouter()
const videoRef = ref<HTMLVideoElement | null>(null)
const canvasRef = ref<HTMLCanvasElement | null>(null)

// 【关键保持】依然使用 shallowRef 避免 Vue 代理破坏视频流
const stream = shallowRef<MediaStream | null>(null)

const capturedImage = ref<string | null>(null)
const isCameraOpen = ref(false)
const uploading = ref(false)

// 启动摄像头
const startCamera = async () => {
  // 1. 基础环境检查
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    ElMessage.error('您的浏览器不支持摄像头访问，请使用 Chrome/Edge 并确保在 localhost 或 HTTPS 下运行。')
    return
  }

  try {
    // 2. PC 端宽松配置：不强制指定 facingMode，优先保证分辨率
    const constraints = {
      video: {
        width: { min: 640, ideal: 1280, max: 1920 },
        height: { min: 480, ideal: 720, max: 1080 },
        // PC端通常不需要 facingMode: 'user'，去掉它可以减少驱动兼容问题
      }
    }
    
    console.log('正在请求摄像头权限...')
    stream.value = await navigator.mediaDevices.getUserMedia(constraints)
    console.log('摄像头权限已获取，流ID:', stream.value.id)

    await nextTick() // 确保 DOM 更新

    if (videoRef.value && stream.value) {
      videoRef.value.srcObject = stream.value
      
      // 3. 显式播放，并捕获可能的自动播放策略错误
      try {
        await videoRef.value.play()
        isCameraOpen.value = true
      } catch (playError) {
        console.error('视频自动播放失败:', playError)
        ElMessage.warning('视频未自动播放，可能是浏览器策略限制')
      }
    }
  } catch (err: any) {
    console.error('摄像头启动详细错误:', err)
    let msg = '无法启动摄像头'
    // 细化错误提示
    if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
      msg = '访问被拒绝：请在浏览器地址栏点击“锁”图标允许摄像头权限'
    } else if (err.name === 'NotFoundError' || err.name === 'DevicesNotFoundError') {
      msg = '未检测到摄像头设备，请检查连接'
    } else if (err.name === 'NotReadableError' || err.name === 'TrackStartError') {
      msg = '摄像头可能正在被其他应用（如微信/会议软件）占用'
    }
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
    // 设置画布尺寸
    canvas.width = video.videoWidth
    canvas.height = video.videoHeight
    
    // 镜像绘制
    context.save()
    context.scale(-1, 1)
    context.drawImage(video, -canvas.width, 0, canvas.width, canvas.height)
    context.restore()
    
    capturedImage.value = canvas.toDataURL('image/jpeg', 0.95) // PC端可以使用更高质量
    // PC端通常不自动停止流，体验更好，如果想停掉可以解开下面注释
    // stopCamera() 
  }
}

const retakePhoto = () => {
  capturedImage.value = null
  // 如果流断了（比如之前调用了 stopCamera），重新开启
  if (!stream.value || !stream.value.active) {
    startCamera()
  }
}

const handleUpload = async () => {
  if (!capturedImage.value) return
  
  // Base64 转 File
  const arr = capturedImage.value.split(',')
  const mime = arr[0].match(/:(.*?);/)![1]
  const bstr = atob(arr[1])
  let n = bstr.length
  const u8arr = new Uint8Array(n)
  while (n--) {
    u8arr[n] = bstr.charCodeAt(n)
  }
  const file = new File([u8arr], 'pc_face_register.jpg', { type: mime })
  
  const formData = new FormData()
  formData.append('file', file)
  
  const loading = ElLoading.service({
    lock: true,
    text: '正在上传数据...',
    background: 'rgba(255, 255, 255, 0.7)',
  })
  
  try {
    uploading.value = true
    await uploadFaceImage(formData)
    ElMessage.success('人脸注册成功！即将返回...')
    setTimeout(() => router.push('/student/profile'), 1500)
  } catch (error) {
    console.error(error)
    // 错误处理交由 request 拦截器
  } finally {
    loading.close()
    uploading.value = false
  }
}

onMounted(() => {
  // 稍微延迟一下，确保页面完全渲染
  setTimeout(() => {
    startCamera()
  }, 300)
})

onUnmounted(() => {
  stopCamera()
})
</script>

<template>
  <div class="pc-container">
    <el-card class="box-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <div class="flex items-center gap-2">
            <el-button circle :icon="Back" @click="router.back()" />
            <span class="text-xl font-bold ml-2">人脸信息采集 (PC版)</span>
          </div>
          <el-tag type="info" effect="plain">请正对摄像头</el-tag>
        </div>
      </template>

      <div class="content-wrapper flex gap-8">
        <div class="camera-section relative bg-black rounded-lg overflow-hidden shadow-lg">
          <video
            v-show="!capturedImage"
            ref="videoRef"
            autoplay
            playsinline
            muted
            class="video-feed"
          ></video>

          <img
            v-if="capturedImage"
            :src="capturedImage"
            class="image-preview"
          />

          <div v-if="!capturedImage" class="mask-overlay">
            <div class="scan-frame">
              <div class="scan-line"></div>
            </div>
            <div class="tips-text">请将面部置于框内</div>
          </div>
        </div>

        <div class="control-section flex flex-col justify-center gap-6">
          <div class="instructions text-gray-500">
            <h3 class="text-lg font-bold text-gray-700 mb-2">操作指南</h3>
            <ul class="list-disc pl-5 space-y-2">
              <li>保持光线充足，不要背光。</li>
              <li>摘下帽子、口罩或深色墨镜。</li>
              <li>正视前方，确保面部完整显示。</li>
            </ul>
          </div>

          <div class="actions flex flex-col gap-4 mt-4">
            <template v-if="!capturedImage">
              <el-button 
                type="primary" 
                size="large" 
                :icon="Camera" 
                @click="capturePhoto"
                :disabled="!isCameraOpen"
                class="action-btn"
              >
                立即拍照
              </el-button>
            </template>

            <template v-else>
              <div class="flex gap-4">
                <el-button 
                  size="large" 
                  :icon="RefreshLeft" 
                  @click="retakePhoto"
                  class="flex-1"
                >
                  重新拍摄
                </el-button>
                <el-button 
                  type="success" 
                  size="large" 
                  :icon="Upload" 
                  @click="handleUpload"
                  :loading="uploading"
                  class="flex-1"
                >
                  确认上传
                </el-button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </el-card>

    <canvas ref="canvasRef" style="display: none;"></canvas>
  </div>
</template>

<style scoped>
.pc-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5; /* 浅灰背景，适合 PC */
  padding: 20px;
}

.box-card {
  width: 900px; /* 宽卡片 */
  max-width: 95vw;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.camera-section {
  width: 480px; /* 4:3 比例 */
  height: 360px;
  position: relative;
  background-color: #000;
  border-radius: 8px;
}

.video-feed, .image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 视频镜像翻转 */
.video-feed {
  transform: scaleX(-1);
}

/* 简单的 CSS 遮罩 */
.mask-overlay {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  pointer-events: none; /* 穿透点击 */
}

.scan-frame {
  width: 200px;
  height: 240px;
  border: 2px solid rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.5); /* 超大阴影形成遮罩 */
  position: relative;
  overflow: hidden;
}

/* 扫描线动画 */
.scan-line {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 2px;
  background: #409eff;
  box-shadow: 0 0 4px #409eff;
  animation: scan 2s linear infinite;
}

@keyframes scan {
  0% { top: 0; opacity: 0; }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% { top: 100%; opacity: 0; }
}

.tips-text {
  margin-top: 16px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  background: rgba(0, 0, 0, 0.6);
  padding: 4px 12px;
  border-radius: 4px;
}

.control-section {
  flex: 1;
}

.action-btn {
  width: 100%;
  height: 50px;
  font-size: 16px;
}
</style>