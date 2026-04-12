<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search,
  ArrowLeft,
  User,
  Ticket,
  View,
  Postcard,
  School
} from '@element-plus/icons-vue'
import { getCourseDetail, getCourseStudentPage } from '@/api/course'
import type { TeacherStudentTableVO } from '@/types/api'

const route = useRoute()
const router = useRouter()

const courseId = computed(() => Number(route.params.id))
const courseName = ref('')

const loading = ref(false)
const tableData = ref<TeacherStudentTableVO[]>([])
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const detailDialogVisible = ref(false)
const currentStudent = ref<Partial<TeacherStudentTableVO>>({})

const totalStudentCount = computed(() => total.value)
const currentClassCount = computed(() => new Set(tableData.value.map(item => item.className).filter(Boolean)).size)

const loadCourseDetail = async () => {
  const id = courseId.value
  if (!id || Number.isNaN(id)) {
    ElMessage.error('课程 ID 无效')
    router.replace('/teacher/course')
    return
  }

  const res = await getCourseDetail(id)
  courseName.value = res.data.courseName || ''
}

const fetchTableData = async () => {
  const id = courseId.value
  if (!id || Number.isNaN(id)) {
    ElMessage.error('课程 ID 无效')
    router.replace('/teacher/course')
    return
  }

  loading.value = true
  try {
    const res = await getCourseStudentPage(id, {
      keyword: searchQuery.value.trim() || undefined,
      currentPage: currentPage.value,
      pageSize: pageSize.value
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error: any) {
    console.error(error)
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = async () => {
  currentPage.value = 1
  await fetchTableData()
}

const handleReset = async () => {
  searchQuery.value = ''
  currentPage.value = 1
  await fetchTableData()
}

const handlePageChange = () => {
  fetchTableData()
}

const handleSizeChange = () => {
  currentPage.value = 1
  fetchTableData()
}

const viewDetails = (row: TeacherStudentTableVO) => {
  currentStudent.value = { ...row }
  detailDialogVisible.value = true
}

const goBack = () => {
  router.push('/teacher/dashboard')
}

onMounted(async () => {
  await loadCourseDetail()
  await fetchTableData()
})
</script>

<template>
  <div class="space-y-6">
    <!-- 头部区域 -->
    <div class="mb-5 flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div class="flex items-center gap-3">
        <el-button :icon="ArrowLeft" circle @click="goBack" />
        <div>
          <h1 class="text-2xl font-bold text-gray-900">班级管理 <span class="text-lg font-normal text-gray-500 ml-2">—— {{ courseName }}</span></h1>
          <p class="text-gray-500 mt-1">查看和管理当前课程名下的学生状态及信息</p>
        </div>
      </div>
    </div>

    <!-- 统计信息卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div class="bg-white p-6 rounded-xl border border-gray-100 shadow-sm flex items-center">
        <div class="w-12 h-12 rounded-lg flex items-center justify-center mr-4 text-xl bg-blue-50 text-blue-600">
          <el-icon class="text-2xl"><User /></el-icon>
        </div>
        <div>
          <p class="text-sm text-gray-500 mb-1">班级学生总数</p>
          <div class="flex items-baseline gap-1">
            <p class="text-2xl font-bold text-gray-900">{{ totalStudentCount }}</p>
            <p class="text-sm text-gray-500">人</p>
          </div>
        </div>
      </div>

      <div class="bg-white p-6 rounded-xl border border-gray-100 shadow-sm flex items-center">
        <div class="w-12 h-12 rounded-lg flex items-center justify-center mr-4 text-xl bg-indigo-50 text-indigo-600">
          <el-icon class="text-2xl"><Ticket /></el-icon>
        </div>
        <div>
          <p class="text-sm text-gray-500 mb-1">当前页涉及行政班级</p>
          <div class="flex items-baseline gap-1">
            <p class="text-2xl font-bold text-gray-900">{{ currentClassCount }}</p>
            <p class="text-sm text-gray-500">个</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 搜索筛选 -->
    <div class="bg-white p-4 md:p-5 rounded-xl border border-gray-100 shadow-sm flex flex-col gap-4">
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <el-form :inline="true" :model="{ keyword: searchQuery }" class="flex flex-col md:flex-row md:items-center gap-3 w-full" @submit.prevent>
          <el-form-item class="mb-0! mr-0! w-full md:w-80">
            <el-input
              v-model="searchQuery"
              placeholder="搜索学号 / 姓名 / 行政班级"
              class="w-full"
              clearable
              :prefix-icon="Search"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item class="mb-0! mr-0! w-full md:w-auto flex">
            <el-button type="primary" @click="handleSearch" class="flex-1 md:flex-none">搜索当前课程</el-button>
            <el-button @click="handleReset" class="flex-1 md:flex-none">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden flex flex-col min-h-0">
      <el-table
        v-loading="loading"
        :data="tableData"
        style="width: 100%"
        :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: '600' }"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        
        <el-table-column label="头像" width="80" align="center">
          <template #default="{ row }">
            <el-avatar :size="36" class="bg-blue-100 text-blue-600 font-bold overflow-hidden shadow-sm">
              <span v-if="!row.avatarUrl">{{ row.realName?.charAt(0) }}</span>
              <img v-else :src="row.avatarUrl" :alt="row.realName" class="w-full h-full object-cover" />
            </el-avatar>
          </template>
        </el-table-column>

        <el-table-column prop="studentId" label="学号" min-width="130" align="center">
          <template #default="{ row }">
            <span class="font-mono text-gray-600">{{ row.studentId }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="realName" label="姓名" min-width="120" align="center">
          <template #default="{ row }">
            <span class="font-bold text-gray-800">{{ row.realName }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="gender" label="性别" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.gender === '男' ? 'primary' : row.gender === '女' ? 'danger' : 'info'" effect="light" size="small" round>
              {{ row.gender }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="行政班级" min-width="150" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small" effect="plain" class="border-gray-200">{{ row.className || '未填写' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="viewDetails(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页栏 -->
      <div class="p-4 border-t border-gray-100 flex flex-col md:flex-row justify-between items-center gap-4 text-sm text-gray-500">
        <span>列表只展示课程《{{ courseName }}》中的学生</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-drawer
      v-model="detailDialogVisible"
      title="课内学生详情"
      size="400px"
    >
      <div v-if="currentStudent.userId" class="flex flex-col gap-6 h-full">
        <div class="flex items-center gap-5 bg-linear-to-r from-blue-50 to-indigo-50 p-5 rounded-xl border border-blue-100">
          <el-avatar :size="72" class="bg-blue-600 font-bold text-xl text-white outline-4 outline-white shadow-md overflow-hidden">
            <span v-if="!currentStudent.avatarUrl">{{ currentStudent.realName?.charAt(0) }}</span>
            <img v-else :src="currentStudent.avatarUrl" :alt="currentStudent.realName" class="w-full h-full object-cover" />
          </el-avatar>
          <div>
            <div class="text-xl font-bold text-gray-900 flex items-center gap-2">
              {{ currentStudent.realName }}
              <el-tag :type="currentStudent.gender === '男' ? 'primary' : currentStudent.gender === '女' ? 'danger' : 'info'" size="small" round effect="dark">
                {{ currentStudent.gender }}
              </el-tag>
            </div>
            <div class="text-sm text-gray-600 mt-2 flex flex-col gap-1">
              <span class="flex items-center gap-1"><el-icon><Postcard /></el-icon> {{ currentStudent.studentId }}</span>
              <span class="flex items-center gap-1"><el-icon><School /></el-icon> {{ currentStudent.className || '未填写行政班级' }}</span>
            </div>
          </div>
        </div>

        <div>
          <h3 class="text-md font-bold mb-3 border-l-4 border-blue-500 pl-2 text-gray-800">课程基本信息</h3>
          <el-descriptions :column="1" border size="default" class="bg-white">
            <el-descriptions-item label="所属课程">{{ currentStudent.courseName }}</el-descriptions-item>
            <el-descriptions-item label="课程ID">{{ courseId }}</el-descriptions-item>
            <el-descriptions-item label="系统备注">数据来源于课程信息、选课关系和学生档案</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
:deep(.el-drawer__header) {
  margin-bottom: 0;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  font-weight: bold;
}
</style>
