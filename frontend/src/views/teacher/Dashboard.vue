<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, User, Clock, ArrowRight } from '@element-plus/icons-vue'

const router = useRouter()

const stats = [
  { label: '本学期课程', value: '4', icon: 'Book', color: 'bg-blue-50 text-blue-600' },
  { label: '授课班级', value: '6', icon: 'School', color: 'bg-indigo-50 text-indigo-600' },
  { label: '覆盖学生', value: '248', icon: 'User', color: 'bg-emerald-50 text-emerald-600' },
]

const recentCourses = ref([
  {
    id: 1,
    name: 'Java 程序设计基础',
    class: '计科 2301 班',
    studentCount: 42,
    lastSession: '2026-02-07 14:00',
    attendanceRate: 98,
    coverColor: 'from-blue-500 to-blue-600'
  },
  {
    id: 2,
    name: '计算机网络原理',
    class: '计科 2302 班',
    studentCount: 45,
    lastSession: '2026-02-06 10:00',
    attendanceRate: 92,
    coverColor: 'from-indigo-500 to-indigo-600'
  },
  {
    id: 3,
    name: 'Web 前端开发实战',
    class: '软件 2301 班',
    studentCount: 38,
    lastSession: '2026-02-05 08:00',
    attendanceRate: 100,
    coverColor: 'from-purple-500 to-purple-600'
  }
])

const handleCreateCourse = () => {
    // TODO: Create course modal
}

const handleStartRollCall = (courseId: number) => {
    router.push(`/teacher/rollcall/${courseId}`)
}
</script>

<template>
  <div class="space-y-6">
    <!-- Welcome Section -->
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-gray-900">工作台</h1>
        <p class="text-gray-500 mt-1">欢迎回来，今天也要元气满满哦！</p>
      </div>
      <el-button type="primary" :icon="Plus" size="large" @click="handleCreateCourse">创建新课程</el-button>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div v-for="(stat, index) in stats" :key="index" class="bg-white p-6 rounded-xl border border-gray-100 shadow-sm flex items-center">
        <div :class="['w-12 h-12 rounded-lg flex items-center justify-center mr-4 text-xl', stat.color]">
           <!-- Simple icon placeholder based on index -->
           <el-icon v-if="index === 0" class="text-2xl"><reading /></el-icon>
           <el-icon v-else-if="index === 1" class="text-2xl"><office-building /></el-icon>
           <el-icon v-else class="text-2xl"><user /></el-icon>
        </div>
        <div>
          <p class="text-sm text-gray-500 mb-1">{{ stat.label }}</p>
          <p class="text-2xl font-bold text-gray-900">{{ stat.value }}</p>
        </div>
      </div>
    </div>

    <!-- Course List -->
    <div>
      <div class="flex items-center justify-between mb-4">
        <h2 class="text-lg font-bold text-gray-900">我的课程</h2>
        <el-button link type="primary">查看全部 <el-icon class="ml-1"><ArrowRight /></el-icon></el-button>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div 
          v-for="course in recentCourses" 
          :key="course.id" 
          class="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden hover:shadow-md transition-shadow group cursor-pointer"
        >
          <!-- Card Header / Cover -->
          <div :class="['h-24 bg-gradient-to-r p-6 relative', course.coverColor]">
            <h3 class="text-white font-bold text-lg relative z-10">{{ course.name }}</h3>
            <p class="text-white/80 text-sm relative z-10">{{ course.class }}</p>
            
            <!-- Decorative circle -->
            <div class="absolute -bottom-6 -right-6 w-24 h-24 rounded-full bg-white/10"></div>
          </div>

          <!-- Card Body -->
          <div class="p-6">
            <div class="flex items-center justify-between text-sm text-gray-500 mb-4">
              <span class="flex items-center gap-1">
                <el-icon><User /></el-icon> {{ course.studentCount }} 人
              </span>
              <span class="flex items-center gap-1">
                <el-icon><Clock /></el-icon> {{ course.lastSession.split(' ')[0] }}
              </span>
            </div>

            <div class="space-y-4">
              <div>
                <div class="flex justify-between text-xs mb-1">
                  <span class="text-gray-500">最近出勤率</span>
                  <span class="font-bold text-gray-700">{{ course.attendanceRate }}%</span>
                </div>
                <el-progress 
                  :percentage="course.attendanceRate" 
                  :status="course.attendanceRate >= 90 ? 'success' : 'warning'"
                  :stroke-width="8"
                  :show-text="false"
                />
              </div>

              <div class="flex gap-2">
                <el-button class="flex-1" @click.stop="router.push(`/teacher/course/${course.id}`)">管理</el-button>
                <el-button type="primary" class="flex-1" @click.stop="handleStartRollCall(course.id)">发起点名</el-button>
              </div>
            </div>
          </div>
        </div>
        
        <!-- Add New Placeholder -->
        <div 
          class="border-2 border-dashed border-gray-200 rounded-xl flex flex-col items-center justify-center text-gray-400 hover:border-blue-300 hover:text-blue-500 hover:bg-blue-50/50 transition-all cursor-pointer min-h-[220px]"
          @click="handleCreateCourse"
        >
          <el-icon size="32" class="mb-2"><Plus /></el-icon>
          <span class="font-medium">创建新课程</span>
        </div>
      </div>
    </div>
  </div>
</template>
