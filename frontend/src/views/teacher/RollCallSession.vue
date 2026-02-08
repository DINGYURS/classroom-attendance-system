<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Camera, Check, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const step = ref(1) // 1: Upload, 2: Processing, 3: Result
const courseInfo = {
  name: 'Java 程序设计基础',
  class: '计科 2301 班',
  total: 42
}

// Mock Upload State
const fileList = ref([])
const isProcessing = ref(false)

// Mock Result Data
const attendanceData = ref([
  { id: 1, name: '王小明', studentId: '2023001', status: 'present', similarity: 0.92, avatar: '' },
  { id: 2, name: '李华', studentId: '2023002', status: 'absent', similarity: 0, avatar: '' },
  { id: 3, name: '张伟', studentId: '2023003', status: 'present', similarity: 0.88, avatar: '' },
  { id: 4, name: '刘洋', studentId: '2023004', status: 'late', similarity: 0.75, avatar: '' },
  // ... more mock data
])

const stats = reactive({
  present: 38,
  absent: 3,
  late: 1
})

const handleUpload = () => {
  if (fileList.value.length === 0) return
  
  step.value = 2
  isProcessing.value = true
  
  // Simulate processing time
  setTimeout(() => {
    isProcessing.value = false
    step.value = 3
    ElMessage.success('识别完成')
  }, 2000)
}

const toggleStatus = (student: any) => {
  // Cycle status: present -> absent -> late -> present
  const statusMap: Record<string, string> = {
    'present': 'absent',
    'absent': 'late',
    'late': 'present'
  }
  student.status = statusMap[student.status]
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'present': return 'success'
    case 'absent': return 'danger'
    case 'late': return 'warning'
    default: return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'present': return '已到'
    case 'absent': return '缺勤'
    case 'late': return '迟到'
    default: return '未知'
  }
}
</script>

<template>
  <div class="max-w-5xl mx-auto">
    <!-- Header -->
    <div class="flex items-center justify-between mb-8">
      <div>
        <h1 class="text-2xl font-bold text-gray-900 mb-1">发起点名</h1>
        <p class="text-gray-500">{{ courseInfo.name }} - {{ courseInfo.class }} (应到 {{ courseInfo.total }} 人)</p>
      </div>
      <div class="flex items-center gap-2">
         <el-tag effect="dark" round>第 12 周</el-tag>
         <el-tag type="info" effect="plain" round>{{ new Date().toLocaleDateString() }}</el-tag>
      </div>
    </div>

    <!-- Steps -->
    <el-steps :active="step" finish-status="success" class="mb-12" align-center>
      <el-step title="上传合照" description="支持多视角拍摄" />
      <el-step title="智能识别" description="AI 人脸检测与比对" />
      <el-step title="确认结果" description="人工修正与提交" />
    </el-steps>

    <!-- Step 1: Upload -->
    <div v-if="step === 1" class="bg-white p-8 rounded-xl border border-gray-200 shadow-sm text-center">
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <!-- Multi-angle placeholders -->
        <div v-for="i in 3" :key="i" class="aspect-video bg-gray-50 rounded-lg border-2 border-dashed border-gray-300 flex flex-col items-center justify-center hover:border-blue-400 hover:bg-blue-50/50 transition-colors cursor-pointer group relative overflow-hidden">
           <input type="file" class="absolute inset-0 opacity-0 cursor-pointer z-10" accept="image/*" />
           <div class="flex flex-col items-center text-gray-400 group-hover:text-blue-500 transition-colors">
              <el-icon :size="32" class="mb-2"><Camera /></el-icon>
              <span class="font-medium">{{ ['左侧视角', '中间视角', '右侧视角'][i-1] }}</span>
           </div>
        </div>
      </div>
      
      <div class="flex justify-center gap-4">
        <el-upload
          class="hidden"
          action="#"
          multiple
          :auto-upload="false"
          v-model:file-list="fileList"
        >
           <el-button type="primary" size="large">选择照片</el-button>
        </el-upload>
        <el-button type="primary" size="large" class="px-12" @click="handleUpload" :disabled="false">开始识别</el-button>
      </div>
      <p class="mt-4 text-xs text-gray-400">建议分别从教室左、中、右三个角度拍摄，以覆盖所有学生</p>
    </div>

    <!-- Step 2: Processing -->
    <div v-if="step === 2" class="py-20 text-center">
      <div class="relative w-24 h-24 mx-auto mb-6">
         <!-- Spinner Pulse -->
         <div class="absolute inset-0 rounded-full border-4 border-blue-100 animate-ping"></div>
         <div class="absolute inset-0 rounded-full border-4 border-t-blue-600 border-r-blue-600 border-b-transparent border-l-transparent animate-spin"></div>
      </div>
      <h3 class="text-xl font-bold text-gray-900 mb-2">正在识别中...</h3>
      <p class="text-gray-500">已检测到 45 张人脸，正在进行特征比对</p>
    </div>

    <!-- Step 3: Result -->
    <div v-if="step === 3" class="space-y-6">
      
      <!-- Summary Cards -->
      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div class="bg-white p-4 rounded-lg border border-gray-100 shadow-sm">
          <p class="text-gray-500 text-xs mb-1">应到人数</p>
          <p class="text-2xl font-bold text-gray-900">{{ courseInfo.total }}</p>
        </div>
        <div class="bg-emerald-50 p-4 rounded-lg border border-emerald-100 shadow-sm">
          <p class="text-emerald-600 text-xs mb-1">实到人数</p>
          <p class="text-2xl font-bold text-emerald-700">{{ stats.present }}</p>
        </div>
         <div class="bg-rose-50 p-4 rounded-lg border border-rose-100 shadow-sm">
          <p class="text-rose-600 text-xs mb-1">缺勤人数</p>
          <p class="text-2xl font-bold text-rose-700">{{ stats.absent }}</p>
        </div>
        <div class="bg-amber-50 p-4 rounded-lg border border-amber-100 shadow-sm">
          <p class="text-amber-600 text-xs mb-1">迟到人数</p>
          <p class="text-2xl font-bold text-amber-700">{{ stats.late }}</p>
        </div>
      </div>

      <!-- Toolbar -->
      <div class="flex justify-between items-center bg-white p-4 rounded-lg border border-gray-200">
        <div class="flex gap-2">
          <el-radio-group v-model="stats" size="small">
            <el-radio-button label="全部" />
            <el-radio-button label="只看异常" />
          </el-radio-group>
        </div>
        <div class="space-x-3">
           <el-button :icon="Refresh">重新识别</el-button>
           <el-button type="primary" :icon="Check">提交考勤</el-button>
        </div>
      </div>

      <!-- Student Grid -->
      <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        <div 
          v-for="student in attendanceData" 
          :key="student.id"
          class="bg-white rounded-lg border border-gray-200 overflow-hidden hover:shadow-md transition-shadow group relative"
          :class="{'border-rose-300 bg-rose-50/30': student.status === 'absent'}"
        >
          <div class="p-4 flex items-center gap-3">
             <el-avatar :size="48" :src="student.avatar" class="flex-shrink-0 bg-gray-200">
               {{ student.name.charAt(0) }}
             </el-avatar>
             <div class="min-w-0 flex-1">
               <p class="font-bold text-gray-900 truncate">{{ student.name }}</p>
               <p class="text-xs text-gray-500 truncate">{{ student.studentId }}</p>
             
               <div class="flex items-center gap-2 mt-1">
                 <el-tag size="small" :type="getStatusType(student.status)" effect="light" round>
                   {{ getStatusText(student.status) }}
                 </el-tag>
                 <span v-if="student.similarity > 0" class="text-xs text-gray-400 scale-90 origin-left">
                   相似度 {{ (student.similarity * 100).toFixed(0) }}%
                 </span>
               </div>
             </div>
          </div>

          <!-- Hover Overlay Action -->
          <div class="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-2 backdrop-blur-[1px]">
             <el-button size="small" circle :icon="Refresh" @click="toggleStatus(student)" title="切换状态" />
             <!-- More actions -->
          </div>
        </div>
        
        <!-- Generate more fake cards for visual fulness -->
        <div 
          v-for="i in 8" 
          :key="'mock-'+i"
          class="bg-white rounded-lg border border-gray-200 p-4 flex items-center gap-3 opacity-60 grayscale"
        >
           <div class="w-12 h-12 rounded-full bg-gray-100"></div>
           <div class="space-y-2 flex-1">
             <div class="h-4 bg-gray-100 rounded w-1/2"></div>
             <div class="h-3 bg-gray-100 rounded w-2/3"></div>
           </div>
        </div>

      </div>
    </div>
  </div>
</template>
