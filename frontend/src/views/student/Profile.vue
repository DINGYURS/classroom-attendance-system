<script setup lang="ts">
import { ref } from 'vue'
import { Camera, Checked, List } from '@element-plus/icons-vue'

const studentInfo = ref({
  name: '张同学',
  id: '2023001056',
  class: '计算机科学与技术 2301',
  avatar: '',
  faceRegistered: true
})

const activities = [
  { content: '《Java程序设计》考勤 - 正常出席', timestamp: '2026-02-07 14:05', type: 'primary', icon: Checked },
  { content: '《计算机网络》考勤 - 正常出席', timestamp: '2026-02-06 10:02', type: 'primary', icon: Checked },
  { content: '《高等数学》考勤 - 迟到', timestamp: '2026-02-05 08:15', type: 'warning', icon: List },
]
</script>

<template>
  <div class="max-w-4xl mx-auto space-y-6">
    <!-- Profile Card -->
    <div class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
      <div class="h-32 bg-gradient-to-r from-blue-500 to-indigo-600"></div>
      <div class="px-8 pb-8 flex flex-col md:flex-row items-start gap-6 -mt-12">
        <el-avatar :size="100" class="border-4 border-white shadow-md bg-white text-3xl font-bold text-gray-400">
          {{ studentInfo.name.charAt(0) }}
        </el-avatar>
        
        <div class="mt-14 md:mt-12 flex-1 space-y-1">
          <h1 class="text-2xl font-bold text-gray-900">{{ studentInfo.name }}</h1>
          <p class="text-gray-500 flex items-center gap-2">
             <span class="bg-gray-100 px-2 py-0.5 rounded text-xs font-mono">{{ studentInfo.id }}</span>
             <span>{{ studentInfo.class }}</span>
          </p>
        </div>

        <div class="mt-14 md:mt-12">
           <el-button type="primary" plain>编辑资料</el-button>
        </div>
      </div>
    </div>

    <!-- Layout Grid -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <!-- Left: Face Registration -->
      <div class="md:col-span-1 space-y-6">
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
          <h3 class="font-bold text-gray-900 mb-4 flex items-center gap-2">
            <el-icon class="text-blue-600"><Camera /></el-icon> 人脸底库
          </h3>
          
          <div class="aspect-square bg-gray-50 rounded-lg border flow-root relative overflow-hidden group">
             <div v-if="studentInfo.faceRegistered" class="absolute inset-0 flex items-center justify-center bg-emerald-50 text-emerald-600 flex-col">
                <el-icon :size="48"><Checked /></el-icon>
                <span class="mt-2 font-medium">已录入人脸</span>
             </div>
             <div v-else class="absolute inset-0 flex items-center justify-center text-gray-400 flex-col">
                <span class="text-sm">未录入</span>
             </div>
             
             <!-- Hover Action -->
             <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
               <el-button type="primary" size="small" @click="$router.push('/student/face-register')">更新照片</el-button>
             </div>
          </div>
          
          <p class="text-xs text-gray-400 mt-3 leading-relaxed">
            * 请上传清晰的本人正面免冠照片，请勿使用美颜或佩戴墨镜。照片将用于课堂考勤比对。
          </p>
        </div>
      </div>

      <!-- Right: Attendance History -->
      <div class="md:col-span-2">
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-6 min-h-[400px]">
          <h3 class="font-bold text-gray-900 mb-6 flex items-center gap-2">
            <el-icon class="text-blue-600"><List /></el-icon> 近期考勤记录
          </h3>

          <el-timeline>
            <el-timeline-item
              v-for="(activity, index) in activities"
              :key="index"
              :type="activity.type"
              :timestamp="activity.timestamp"
              :icon="activity.icon"
              size="large"
            >
              {{ activity.content }}
            </el-timeline-item>
             <el-timeline-item timestamp="..." type="info" center>
               更多历史记录
             </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </div>
  </div>
</template>
