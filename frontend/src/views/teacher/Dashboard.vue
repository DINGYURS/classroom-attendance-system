<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, User, Clock, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createCourse, getMyCourses } from '@/api/course'
import type { CourseDTO } from '@/types/api'

const router = useRouter()

const stats = [
  { label: '本学期课程', value: '4', icon: 'Book', color: 'bg-blue-50 text-blue-600' },
  { label: '授课班级', value: '6', icon: 'School', color: 'bg-indigo-50 text-indigo-600' },
  { label: '覆盖学生', value: '248', icon: 'User', color: 'bg-emerald-50 text-emerald-600' },
]

const recentCourses = ref<any[]>([])
const loading = ref(true)

const coverColors = [
  'from-blue-500 to-blue-600',
  'from-indigo-500 to-indigo-600',
  'from-purple-500 to-purple-600',
  'from-emerald-500 to-emerald-600',
  'from-rose-500 to-rose-600',
  'from-amber-500 to-amber-600'
]

const loadCourses = async () => {
  loading.value = true
  try {
    const res = await getMyCourses()
    recentCourses.value = (res.data || []).map((course, index) => ({
      id: course.courseId,
      name: course.courseName,
      class: course.classes && course.classes.length > 0 ? course.classes.join(', ') : '暂无授课班级',
      studentCount: course.studentCount || 0,
      lastSession: course.semester || '暂无学期',
      attendanceRate: course.attendanceRate !== undefined && course.attendanceRate !== null ? course.attendanceRate : 100,
      coverColor: coverColors[index % coverColors.length]
    }))
  } catch (error: any) {
    ElMessage.error(error.message || '获取课程列表失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCourses()
})

const courseDialogVisible = ref(false)
const courseFormRef = ref<FormInstance>()
const submitting = ref(false)

const courseForm = ref<CourseDTO>({
  courseName: '',
  semester: '',
  description: ''
})

const rules = ref<FormRules<CourseDTO>>({
  courseName: [
    { required: true, message: '请输入课程名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  semester: [
    { required: true, message: '请输入学期，例如 2025-2026-1', trigger: 'blur' }
  ]
})

const handleCreateCourse = () => {
  // 重置表单状态
  if (courseFormRef.value) {
    courseFormRef.value.resetFields()
  }
  courseForm.value = {
    courseName: '',
    semester: '',
    description: ''
  }
  courseDialogVisible.value = true
}

const submitCourseForm = async () => {
  if (!courseFormRef.value) return
  
  await courseFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await createCourse(courseForm.value)
        ElMessage.success('课程创建成功')
        courseDialogVisible.value = false
        await loadCourses()
      } catch (error: any) {
        ElMessage.error(error.message || '创建课程失败')
      } finally {
        submitting.value = false
      }
    }
  })
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

      <el-skeleton :loading="loading" animated>
        <template #template>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <div v-for="i in 3" :key="i" class="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden">
              <!-- Header skeleton -->
              <div class="h-24 p-6 bg-gray-100 flex flex-col justify-center">
                <el-skeleton-item variant="h3" class="w-1/2 mb-2" />
                <el-skeleton-item variant="text" class="w-1/3" />
              </div>
              
              <!-- Body skeleton -->
              <div class="p-6 space-y-4">
                <div class="flex justify-between">
                  <el-skeleton-item variant="text" class="w-16" />
                  <el-skeleton-item variant="text" class="w-20" />
                </div>
                <div>
                  <div class="flex justify-between mb-1">
                    <el-skeleton-item variant="text" class="w-16" />
                    <el-skeleton-item variant="text" class="w-8" />
                  </div>
                  <el-skeleton-item variant="text" class="h-2 w-full rounded" />
                </div>
                <div class="flex gap-2">
                  <el-skeleton-item variant="button" class="flex-1" />
                  <el-skeleton-item variant="button" class="flex-1 h-8" style="background-color: var(--el-color-primary-light-7);" />
                </div>
              </div>
            </div>
          </div>
        </template>
        <template #default>
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <div 
              v-for="course in recentCourses" 
              :key="course.id" 
              class="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden hover:shadow-md transition-shadow group cursor-pointer"
            >
              <!-- Card Header / Cover -->
              <div :class="['h-24 bg-linear-to-r p-6 relative', course.coverColor]">
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
                    <el-icon><Clock /></el-icon> {{ course.lastSession }}
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
        </template>
      </el-skeleton>
    </div>

    <!-- 创建课程弹窗 -->
    <el-dialog
      v-model="courseDialogVisible"
      title="创建新课程"
      width="90%"
      style="max-width: 500px;"
      destroy-on-close
    >
      <el-form
        ref="courseFormRef"
        :model="courseForm"
        :rules="rules"
        label-width="90px"
        class="md:pr-5"
      >
        <el-form-item label="课程名称" prop="courseName">
          <el-input 
            v-model="courseForm.courseName" 
            placeholder="例如: Java程序设计" 
            clearable
          />
        </el-form-item>
        <el-form-item label="学期" prop="semester">
          <el-input 
            v-model="courseForm.semester" 
            placeholder="例如: 2025-2026-1" 
            clearable
          />
        </el-form-item>
        <el-form-item label="课程描述" prop="description">
          <el-input 
            v-model="courseForm.description" 
            type="textarea" 
            :rows="4"
            placeholder="请输入课程简要描述..." 
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="courseDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitCourseForm">
            确认创建
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
