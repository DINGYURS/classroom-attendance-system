<script setup lang="ts">
import { ref } from 'vue'
import { Search, Upload, Download, View, User, DataLine, Ticket, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// --- Mock Data ---
const tableData = ref([
  {
    id: 1,
    courseName: '高级软件工程',
    semester: '2025-2026-1',
    studentId: '2022030101',
    realName: '张三',
    gender: '男',
    className: '计科225'
  },
  {
    id: 2,
    courseName: '高级软件工程',
    semester: '2025-2026-1',
    studentId: '2022030102',
    realName: '李四',
    gender: '女',
    className: '计科225'
  },
  {
    id: 3,
    courseName: '高级软件工程',
    semester: '2025-2026-1',
    studentId: '2022030215',
    realName: '王五',
    gender: '男',
    className: '软工221'
  }
])

// Pagination & Search
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(185)

const handleSearch = () => {
  ElMessage.success('搜索模拟成功: ' + searchQuery.value)
}

const handleReset = () => {
  searchQuery.value = ''
  ElMessage.success('重置搜索条件')
}

// --- Import/Export ---
const fileInputRef = ref<HTMLInputElement | null>(null)

const triggerImport = () => {
  fileInputRef.value?.click()
}

const handleFileChange = (e: Event) => {
  const target = e.target as HTMLInputElement
  if (target.files && target.files.length > 0) {
    const file = target.files[0]
    if (file) {
      ElMessage.success(`模拟成功导入文件: ${file.name}`)
    }
    // Reset
    target.value = ''
  }
}

// --- Student Detail Drawer ---
const detailDialogVisible = ref(false)
const currentStudent = ref<any>({})

// Mock Attendance Data for the detailed view
const attendanceRecords = ref([
  { date: '2025-09-22 10:00:00', status: 'present', statusText: '正常出勤', reason: '' },
  { date: '2025-09-15 10:00:00', status: 'absent', statusText: '缺勤', reason: '未在此课堂签到' },
  { date: '2025-09-08 10:00:00', status: 'leave', statusText: '请假', reason: '病假，已向辅导员报备' },
  { date: '2025-09-01 10:00:00', status: 'present', statusText: '正常出勤', reason: '' },
])

const viewDetails = (row: any) => {
  currentStudent.value = { ...row }
  detailDialogVisible.value = true
}

// Generate tag type based on status
const getStatusType = (status: string) => {
  switch (status) {
    case 'present': return 'success'
    case 'absent': return 'danger'
    case 'late': return 'warning'
    case 'leave': return 'info'
    default: return ''
  }
}
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="mb-5">
      <h1 class="text-2xl font-bold text-gray-900">学生管理</h1>
      <p class="text-gray-500 mt-1">查看和管理您所教授的课程中的学生名单及其考勤情况。</p>
    </div>

    <!-- Quick Stats Cards -->
    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div class="bg-white p-6 rounded-xl border border-gray-100 shadow-sm flex items-center">
        <div class="w-12 h-12 rounded-lg flex items-center justify-center mr-4 text-xl bg-blue-50 text-blue-600">
          <el-icon class="text-2xl"><User /></el-icon>
        </div>
        <div>
          <p class="text-sm text-gray-500 mb-1">总课程学生人数</p>
          <div class="flex items-baseline gap-1">
            <p class="text-2xl font-bold text-gray-900">185</p>
            <p class="text-sm text-gray-500">人</p>
          </div>
        </div>
      </div>
      
      <div class="bg-white p-6 rounded-xl border border-gray-100 shadow-sm flex items-center">
        <div class="w-12 h-12 rounded-lg flex items-center justify-center mr-4 text-xl bg-indigo-50 text-indigo-600">
          <el-icon class="text-2xl"><Ticket /></el-icon>
        </div>
        <div>
          <p class="text-sm text-gray-500 mb-1">涉及行政班级</p>
          <div class="flex items-baseline gap-1">
            <p class="text-2xl font-bold text-gray-900">6</p>
            <p class="text-sm text-gray-500">个</p>
          </div>
        </div>
      </div>

      <div class="bg-white p-6 rounded-xl border border-gray-100 shadow-sm flex items-center">
        <div class="w-12 h-12 rounded-lg flex items-center justify-center mr-4 text-xl bg-green-50 text-green-600">
          <el-icon class="text-2xl"><DataLine /></el-icon>
        </div>
        <div>
          <p class="text-sm text-gray-500 mb-1">整体平均出勤率</p>
          <p class="text-2xl font-bold text-gray-900">94.5%</p>
        </div>
      </div>
    </div>

    <!-- Toolbar / Search Panel -->
    <div class="bg-white p-4 md:p-5 rounded-xl border border-gray-100 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
      <el-form :inline="true" :model="{ keyword: searchQuery }" class="flex flex-col md:flex-row md:items-center gap-3 w-full md:w-auto" @submit.prevent>
        <el-form-item class="mb-0! mr-0! w-full md:w-80">
          <el-input
            v-model="searchQuery"
            placeholder="搜索课程名称 / 学号 / 姓名 / 班级"
            class="w-full"
            clearable
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item class="mb-0! mr-0! w-full md:w-auto flex">
          <el-button type="primary" @click="handleSearch" class="flex-1 md:flex-none">搜索</el-button>
          <el-button :icon="Refresh" @click="handleReset" class="flex-1 md:flex-none">重置</el-button>
        </el-form-item>
      </el-form>
      
      <div class="flex flex-col md:flex-row items-center gap-2 w-full md:w-auto shrink-0">
        <input 
          type="file" 
          ref="fileInputRef" 
          v-show="false" 
          accept=".xlsx, .xls" 
          @change="handleFileChange"
        />
        <el-button type="success" :icon="Upload" @click="triggerImport" class="w-full md:w-auto mx-0! md:mx-0!">导入名单</el-button>
        <el-button :icon="Download" plain class="w-full md:w-auto mx-0! md:mx-0!">导出名单</el-button>
      </div>
    </div>

    <!-- Desktop Data Table -->
    <div class="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden flex flex-col min-h-0">
      <el-table 
        :data="tableData" 
        style="width: 100%"
        :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: '600' }"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="courseName" label="课程名称" min-width="150" show-overflow-tooltip>
           <template #default="{ row }">
            <div class="font-medium text-gray-900">{{ row.courseName }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="semester" label="学期时间" min-width="120" align="center" />
        <el-table-column prop="studentId" label="学生学号" min-width="120" align="center" />
        <el-table-column prop="realName" label="真实姓名" min-width="100" align="center">
          <template #default="scope">
            <span class="font-bold text-gray-800">{{ scope.row.realName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="gender" label="性别" width="80" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.gender === '男' ? 'primary' : 'danger'" effect="light" size="small" round>
              {{ scope.row.gender }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="行政班级" min-width="120" align="center">
           <template #default="{ row }">
             <el-tag type="info" size="small">{{ row.className }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="scope">
            <el-button link type="primary" :icon="View" @click="viewDetails(scope.row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="p-4 border-t border-gray-100 flex flex-col md:flex-row justify-between items-center gap-4 text-sm text-gray-500">
        <span>此列表显示教师任课下所有登记报名的学生</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="sizes, prev, pager, next, jumper"
          :total="total"
        />
      </div>
    </div>

    <!-- Student Detail Drawer -->
    <el-drawer
      v-model="detailDialogVisible"
      title="学生个人及考勤详情"
      size="450px"
    >
      <div v-if="currentStudent.id" class="flex flex-col gap-6 h-full">
        <!-- Personal Info -->
        <div class="flex items-center gap-5 bg-linear-to-r from-blue-50 to-indigo-50 p-5 rounded-xl border border-blue-100">
          <el-avatar :size="72" class="bg-blue-600 font-bold text-xl text-white outline-4 outline-white shadow-md">
            {{ currentStudent.realName.charAt(0) }}
          </el-avatar>
          <div>
            <div class="text-xl font-bold text-gray-900 flex items-center gap-2">
              {{ currentStudent.realName }}
              <el-tag :type="currentStudent.gender === '男' ? 'primary' : 'danger'" size="small" round effect="dark">
                {{ currentStudent.gender }}
              </el-tag>
            </div>
            <div class="text-sm text-gray-600 mt-2 flex flex-col gap-1">
              <span class="flex items-center gap-1"><el-icon><Postcard /></el-icon> {{ currentStudent.studentId }}</span> 
              <span class="flex items-center gap-1"><el-icon><School /></el-icon> {{ currentStudent.className }}</span>
            </div>
          </div>
        </div>

        <!-- Course Info -->
        <div>
          <h3 class="text-md font-bold mb-3 border-l-4 border-blue-500 pl-2 text-gray-800">课程统计概览</h3>
          <el-descriptions :column="1" border size="default" class="bg-white">
            <el-descriptions-item label="所属课程">{{ currentStudent.courseName }}</el-descriptions-item>
            <el-descriptions-item label="开课学期">{{ currentStudent.semester }}</el-descriptions-item>
            <el-descriptions-item label="全勤率">
              <span class="text-green-600 font-bold">75%</span> (3 / 4 次)
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- Recent Attendance -->
        <div class="flex-1 flex flex-col min-h-0">
          <h3 class="text-md font-bold mb-3 border-l-4 border-blue-500 pl-2 text-gray-800 flex justify-between">
            <span>历史考勤轨迹</span>
            <span class="text-xs font-normal text-gray-400">仅显示最近记录</span>
          </h3>
          <div class="flex-1 overflow-auto px-2 pb-2">
            <el-timeline class="pt-2" style="padding-left: 2px;">
              <el-timeline-item
                v-for="(record, index) in attendanceRecords"
                :key="index"
                :type="getStatusType(record.status)"
                :hollow="record.status === 'present'"
                :timestamp="record.date"
                placement="top"
              >
                <el-card shadow="hover" class="my-0! border-gray-100" body-class="p-3">
                  <div class="flex justify-between items-start gap-4">
                    <div class="flex flex-col gap-1">
                      <span class="text-sm font-bold text-gray-700">第 {{ 4 - index }} 次点名</span>
                      <span v-if="record.reason" class="text-xs text-gray-500 line-clamp-2">
                        备注：{{ record.reason }}
                      </span>
                    </div>
                    <el-tag :type="getStatusType(record.status)" effect="light" disable-transitions class="shrink-0">
                      {{ record.statusText }}
                    </el-tag>
                  </div>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script lang="ts">
import { Postcard, School } from '@element-plus/icons-vue'
export default {
  components: {
    Postcard,
    School
  }
}
</script>

<style scoped>
:deep(.el-drawer__header) {
  margin-bottom: 0;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  font-weight: bold;
}
:deep(.el-timeline-item__node--normal) {
  width: 12px;
  height: 12px;
  left: -2px;
}
</style>
