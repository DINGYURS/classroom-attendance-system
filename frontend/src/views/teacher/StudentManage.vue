<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  Search,
  Upload,
  Download,
  View,
  User,
  DataLine,
  Ticket,
  Refresh,
  Postcard,
  School
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { downloadTeacherStudentList, getTeacherStudentPage, importTeacherStudentList } from '@/api/teacher'
import type { TeacherStudentTableVO } from '@/types/api'

const loading = ref(false)
const importing = ref(false)
const exporting = ref(false)
const tableData = ref<TeacherStudentTableVO[]>([])

const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fileInputRef = ref<HTMLInputElement | null>(null)

const detailDialogVisible = ref(false)
const currentStudent = ref<Partial<TeacherStudentTableVO>>({})

const attendanceRecords = ref([
  { date: '2025-09-22 10:00:00', status: 'present', statusText: '正常出勤', reason: '' },
  { date: '2025-09-15 10:00:00', status: 'absent', statusText: '缺勤', reason: '未在课堂点名时间内签到' },
  { date: '2025-09-08 10:00:00', status: 'leave', statusText: '请假', reason: '病假，已提前报备' },
  { date: '2025-09-01 10:00:00', status: 'present', statusText: '正常出勤', reason: '' }
])

const totalStudentCount = computed(() => total.value)
const currentClassCount = computed(() => new Set(tableData.value.map(item => item.className).filter(Boolean)).size)
const currentCourseCount = computed(() => new Set(tableData.value.map(item => `${item.courseName}-${item.semester}`)).size)

const fetchTableData = async () => {
  loading.value = true
  try {
    const res = await getTeacherStudentPage({
      keyword: searchQuery.value.trim() || undefined,
      currentPage: currentPage.value,
      pageSize: pageSize.value
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
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

const triggerImport = () => {
  fileInputRef.value?.click()
}

const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) {
    return
  }

  if (!/\.(xlsx|xls)$/i.test(file.name)) {
    ElMessage.error('请上传 .xlsx 或 .xls 格式的名单文件')
    target.value = ''
    return
  }

  const formData = new FormData()
  formData.append('file', file)

  importing.value = true
  try {
    const res = await importTeacherStudentList(formData)
    ElMessage.success(res.data || '名单导入成功')
    currentPage.value = 1
    await fetchTableData()
  } finally {
    importing.value = false
    target.value = ''
  }
}

const handleExportClick = async () => {
  exporting.value = true
  try {
    const response = await downloadTeacherStudentList({
      keyword: searchQuery.value.trim() || undefined
    })

    const blob = new Blob([response.data], {
      type: response.headers['content-type'] || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const fileName = extractFileName(response.headers['content-disposition']) || '学生名单.xlsx'
    downloadBlob(blob, fileName)
    ElMessage.success('名单导出成功')
  } catch (error) {
    console.error(error)
    ElMessage.error('名单导出失败')
  } finally {
    exporting.value = false
  }
}

const extractFileName = (contentDisposition?: string) => {
  if (!contentDisposition) {
    return ''
  }

  const utf8Match = contentDisposition.match(/filename\*=utf-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    return decodeURIComponent(utf8Match[1])
  }

  const plainMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  return plainMatch?.[1] || ''
}

const downloadBlob = (blob: Blob, fileName: string) => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

const viewDetails = (row: TeacherStudentTableVO) => {
  currentStudent.value = { ...row }
  detailDialogVisible.value = true
}

const getStatusType = (status: string) => {
  switch (status) {
    case 'present':
      return 'success'
    case 'absent':
      return 'danger'
    case 'late':
      return 'warning'
    case 'leave':
      return 'info'
    default:
      return ''
  }
}

onMounted(() => {
  fetchTableData()
})
</script>

<template>
  <div class="space-y-6">
    <div class="mb-5">
      <h1 class="text-2xl font-bold text-gray-900">学生管理</h1>
      <p class="text-gray-500 mt-1">查看和管理您所教授课程中的学生名单，并支持按 Excel 模板批量导入导出</p>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div class="bg-white p-6 rounded-xl border border-gray-100 shadow-sm flex items-center">
        <div class="w-12 h-12 rounded-lg flex items-center justify-center mr-4 text-xl bg-blue-50 text-blue-600">
          <el-icon class="text-2xl"><User /></el-icon>
        </div>
        <div>
          <p class="text-sm text-gray-500 mb-1">学生总条目</p>
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

      <div class="bg-white p-6 rounded-xl border border-gray-100 shadow-sm flex items-center">
        <div class="w-12 h-12 rounded-lg flex items-center justify-center mr-4 text-xl bg-green-50 text-green-600">
          <el-icon class="text-2xl"><DataLine /></el-icon>
        </div>
        <div>
          <p class="text-sm text-gray-500 mb-1">当前页涉及课程</p>
          <p class="text-2xl font-bold text-gray-900">{{ currentCourseCount }}</p>
        </div>
      </div>
    </div>

    <div class="bg-white p-4 md:p-5 rounded-xl border border-gray-100 shadow-sm flex flex-col gap-4">
      <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
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
            ref="fileInputRef"
            type="file"
            v-show="false"
            accept=".xlsx,.xls"
            @change="handleFileChange"
          />
          <el-button
            type="success"
            :icon="Upload"
            :loading="importing"
            @click="triggerImport"
            class="w-full md:w-auto mx-0! md:mx-0!"
          >
            导入名单
          </el-button>
          <el-button
            :icon="Download"
            plain
            :loading="exporting"
            class="w-full md:w-auto mx-0! md:mx-0!"
            @click="handleExportClick"
          >
            导出名单
          </el-button>
        </div>
      </div>

      <div class="text-sm text-gray-500 bg-emerald-50 border border-emerald-100 rounded-lg px-4 py-3 leading-6">
        导入模板列顺序：课程名称、学期时间、学生学号、真实姓名、性别、行政班级。导出时会按当前搜索条件导出全部匹配名单
      </div>
    </div>

    <div class="bg-white rounded-xl border border-gray-100 shadow-sm overflow-hidden flex flex-col min-h-0">
      <el-table
        v-loading="loading || importing || exporting"
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
        <el-table-column label="行政班级" min-width="120" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.className || '未填写' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :icon="View" @click="viewDetails(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="p-4 border-t border-gray-100 flex flex-col md:flex-row justify-between items-center gap-4 text-sm text-gray-500">
        <span>列表展示当前教师名下课程与学生的关联关系，导入或导出都会基于当前筛选条件</span>
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          background
          layout="sizes, prev, pager, next, jumper"
          :total="total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <el-drawer
      v-model="detailDialogVisible"
      title="学生个人及考勤详情"
      size="450px"
    >
      <div v-if="currentStudent.userId" class="flex flex-col gap-6 h-full">
        <div class="flex items-center gap-5 bg-linear-to-r from-blue-50 to-indigo-50 p-5 rounded-xl border border-blue-100">
          <el-avatar :size="72" class="bg-blue-600 font-bold text-xl text-white outline-4 outline-white shadow-md">
            {{ currentStudent.realName?.charAt(0) }}
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
          <h3 class="text-md font-bold mb-3 border-l-4 border-blue-500 pl-2 text-gray-800">课程统计概览</h3>
          <el-descriptions :column="1" border size="default" class="bg-white">
            <el-descriptions-item label="所属课程">{{ currentStudent.courseName }}</el-descriptions-item>
            <el-descriptions-item label="开课学期">{{ currentStudent.semester }}</el-descriptions-item>
            <el-descriptions-item label="全勤率">
              <span class="text-green-600 font-bold">75%</span> (3 / 4 次)
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="flex-1 flex flex-col min-h-0">
          <h3 class="text-md font-bold mb-3 border-l-4 border-blue-500 pl-2 text-gray-800 flex justify-between">
            <span>历史考勤轨迹</span>
            <span class="text-xs font-normal text-gray-400">当前为静态示例数据</span>
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
                      <span class="text-sm font-bold text-gray-700">第 {{ attendanceRecords.length - index }} 次点名</span>
                      <span v-if="record.reason" class="text-xs text-gray-500 line-clamp-2">
                        备注：{{ record.reason }}
                      </span>
                    </div>
                    <el-tag :type="getStatusType(record.status)">
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