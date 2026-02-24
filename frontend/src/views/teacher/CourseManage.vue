<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { Plus, Edit, Delete, Search, Refresh, User } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getMyCourses, createCourse, updateCourse, deleteCourse } from '@/api/course'
import type { CourseDTO, CourseVO } from '@/types/api'

// 数据状态
const loading = ref(false)
const courseList = ref<CourseVO[]>([])

// 搜索表单（前端过滤模拟查询）
const searchForm = reactive({
  keyword: ''
})

// 弹窗表单控制
const dialogVisible = ref(false)
const submitting = ref(false)
const dialogTitle = ref('创建课程')
const formRef = ref<FormInstance>()
const isEdit = ref(false)

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

// --- 方法 ---

// 获取课程列表
const fetchCourses = async () => {
  loading.value = true
  try {
    const res = await getMyCourses()
    // 假设后端返回全部列表，前端实现简易搜索过滤
    const data = res.data || []
    if (searchForm.keyword.trim()) {
      const keyword = searchForm.keyword.toLowerCase()
      courseList.value = data.filter(c => 
        c.courseName.toLowerCase().includes(keyword) || 
        c.semester.toLowerCase().includes(keyword) ||
        (c.classes && c.classes.join('').toLowerCase().includes(keyword))
      )
    } else {
      courseList.value = data
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取课程列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  fetchCourses()
}

const handleReset = () => {
  searchForm.keyword = ''
  fetchCourses()
}

// 打开新增弹窗
const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '创建课程'
  courseForm.value = {
    courseName: '',
    semester: '',
    description: ''
  }
  if (formRef.value) {
    formRef.value.resetFields()
  }
  dialogVisible.value = true
}

// 打开编辑弹窗
const handleEdit = (row: CourseVO) => {
  isEdit.value = true
  dialogTitle.value = '编辑课程'
  
  // 回填数据
  courseForm.value = {
    courseId: row.courseId,
    courseName: row.courseName,
    semester: row.semester,
    description: row.description || ''
  }
  
  if (formRef.value) {
    // 移除潜在校验状态
    formRef.value.clearValidate()
  }
  dialogVisible.value = true
}

// 提交表单（新增 / 编辑）
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (isEdit.value) {
          await updateCourse(courseForm.value)
          ElMessage.success('课程更新成功')
        } else {
          await createCourse(courseForm.value)
          ElMessage.success('课程创建成功')
        }
        dialogVisible.value = false
        fetchCourses()
      } catch (error: any) {
        ElMessage.error(error.message || (isEdit.value ? '更新失败' : '创建失败'))
      } finally {
        submitting.value = false
      }
    }
  })
}

// 删除课程
const handleDelete = (row: CourseVO) => {
  ElMessageBox.confirm(
    `确定要删除课程 "${row.courseName}" 吗？此操作不可恢复。`,
    '删除警告',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
      confirmButtonClass: 'el-button--danger'
    }
  ).then(async () => {
    try {
      await deleteCourse(row.courseId)
      ElMessage.success('删除成功')
      fetchCourses()
    } catch (error: any) {
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {
    // cancelled
  })
}

onMounted(() => {
  fetchCourses()
})
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="mb-5">
      <h1 class="text-2xl font-bold text-gray-900">课程管理</h1>
      <p class="text-gray-500 mt-1">在这里管理您的所有教学课程，支持创建、信息更新及删除等操作。</p>
    </div>

    <!-- Toolbar / Search Panel -->
    <div class="bg-white p-4 md:p-5 rounded-xl border border-gray-100 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
      <el-form :inline="true" :model="searchForm" class="flex flex-col md:flex-row md:items-center gap-3 w-full md:w-auto" @submit.prevent>
        <el-form-item class="mb-0! mr-0! w-full md:w-80">
          <el-input 
            v-model="searchForm.keyword" 
            placeholder="搜索课程名称 / 学期 / 班级" 
            clearable
            class="w-full"
            @keyup.enter="handleSearch"
            :prefix-icon="Search"
          />
        </el-form-item>
        <el-form-item class="mb-0! mr-0! w-full md:w-auto flex">
          <el-button type="primary" @click="handleSearch" class="flex-1 md:flex-none">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset" class="flex-1 md:flex-none">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" :icon="Plus" @click="handleAdd" class="w-full md:w-auto shrink-0">创建新课程</el-button>
    </div>

    <!-- Desktop Data Table -->
    <div class="hidden md:block bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden">
      <el-table 
        v-loading="loading" 
        :data="courseList" 
        style="width: 100%"
        :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: '600' }"
      >
        <el-table-column prop="courseName" label="课程名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="font-medium text-gray-900">{{ row.courseName }}</div>
          </template>
        </el-table-column>
        
        <el-table-column prop="semester" label="学期" min-width="140" align="center" />
        
        <el-table-column label="授课班级" min-width="240" align="center">
          <template #default="{ row }">
            <div class="flex flex-wrap justify-center gap-1">
              <el-tag 
                v-if="row.classes && row.classes.length > 0" 
                type="info" 
                size="small" 
                v-for="cls in row.classes" 
                :key="cls"
              >
                {{ cls }}
              </el-tag>
              <span v-else class="text-gray-400 text-sm">暂无</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="学生人数" min-width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.studentCount > 0 ? 'success' : 'info'" effect="plain" round>
              {{ row.studentCount }} 人
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="出勤率" min-width="120" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-2" v-if="row.attendanceRate !== undefined && row.attendanceRate !== null">
              <span class="text-sm font-medium" :class="row.attendanceRate >= 90 ? 'text-green-600' : 'text-amber-600'">
                {{ row.attendanceRate }}%
              </span>
            </div>
            <span v-else class="text-gray-400 text-sm">无记录</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" :icon="Edit" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
        
        <template #empty>
          <el-empty description="暂无课程数据" />
        </template>
      </el-table>
    </div>

    <!-- Mobile Card List -->
    <div class="md:hidden space-y-4">
      <div v-if="loading" class="text-center py-8 text-gray-400">加载中...</div>
      <template v-else-if="courseList.length > 0">
        <div 
          v-for="course in courseList" 
          :key="course.courseId"
          class="bg-white p-4 rounded-xl border border-gray-100 shadow-sm relative transition-all active:scale-[0.98]"
        >
          <div class="flex justify-between items-start mb-3">
            <h3 class="text-lg font-bold text-gray-900 leading-tight pr-2">{{ course.courseName }}</h3>
            <div class="flex gap-2 shrink-0">
              <el-button circle type="primary" plain :icon="Edit" size="small" @click="handleEdit(course)" />
              <el-button circle type="danger" plain :icon="Delete" size="small" @click="handleDelete(course)" />
            </div>
          </div>
          
          <div class="text-sm text-gray-500 mb-3 flex items-center gap-3">
            <el-tag size="small" type="info" effect="plain" round class="font-medium">{{ course.semester }}</el-tag>
            <span class="flex items-center gap-1 font-medium">
              <el-icon><User /></el-icon>{{ course.studentCount }} 人
            </span>
          </div>

          <div class="mb-3">
            <div class="flex flex-wrap gap-1.5">
              <el-tag 
                v-if="course.classes && course.classes.length > 0" 
                type="info" 
                size="small" 
                v-for="cls in course.classes" 
                :key="cls"
              >
                {{ cls }}
              </el-tag>
              <span v-else class="text-gray-400 text-sm">暂无授课班级</span>
            </div>
          </div>

          <div class="flex items-center justify-between border-t border-gray-50 pt-3 mt-1">
            <span class="text-xs text-gray-400">出勤率</span>
            <span v-if="course.attendanceRate !== undefined && course.attendanceRate !== null" 
                  class="font-bold text-sm" 
                  :class="course.attendanceRate >= 90 ? 'text-green-600' : 'text-amber-600'">
              {{ course.attendanceRate }}%
            </span>
            <span v-else class="text-gray-400 text-xs">无记录</span>
          </div>
        </div>
      </template>
      <div v-else class="bg-white p-8 rounded-xl border border-gray-100 shadow-sm flex justify-center">
        <el-empty description="暂无课程数据" :image-size="80" />
      </div>
    </div>

    <!-- Create / Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="90%"
      style="max-width: 500px;"
      destroy-on-close
    >
      <el-form
        ref="formRef"
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
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? '保存更改' : '确认创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
