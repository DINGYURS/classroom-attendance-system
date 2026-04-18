<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, ArrowRight, Document, Check, WarningFilled, Loading } from '@element-plus/icons-vue'
import { getNotices, markNoticeRead } from '@/api/notice'
import type { NoticeVO } from '@/api/notice'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const notices = ref<NoticeVO[]>([])
const filterStatus = ref<string | number>('') // '' = All, 0 = Unread, 1 = Read

// For mobile drawer
const isMobile = ref(window.innerWidth < 768)
const handleResize = () => {
  isMobile.value = window.innerWidth < 768
}
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})
const drawerDirection = computed(() => (isMobile.value ? 'btt' : 'rtl'))
const drawerSize = computed(() => (isMobile.value ? '85%' : '400px'))

const drawerVisible = ref(false)
const currentNotice = ref<NoticeVO | null>(null)
const actionLoading = ref(false)

const stats = computed(() => {
  const total = notices.value.length
  const unread = notices.value.filter(n => n.readStatus === 0).length
  return { total, unread }
})

const filteredNotices = computed(() => {
  if (filterStatus.value === '') return notices.value
  return notices.value.filter(n => n.readStatus === Number(filterStatus.value))
})

const fetchNotices = async () => {
  loading.value = true
  try {
    const res = await getNotices()
    if (res && res.code === 1) {
      notices.value = res.data || []
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('获取通知失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  window.addEventListener('resize', handleResize)
  fetchNotices()
})

const openDetail = (notice: NoticeVO) => {
  currentNotice.value = notice
  drawerVisible.value = true
}

const handleAcknowledge = async () => {
  if (!currentNotice.value) return
  const currentNoticeId = currentNotice.value.id
  actionLoading.value = true
  try {
    const res = await markNoticeRead(currentNoticeId)
    if (res && res.code === 1) {
      ElMessage.success('已确认收到提醒')
      currentNotice.value.readStatus = 1
      // Update the list reference too
      const idx = notices.value.findIndex(n => n.id === currentNoticeId)
      const targetNotice = idx !== -1 ? notices.value[idx] : undefined
      if (targetNotice) {
        targetNotice.readStatus = 1
      }
    }
  } catch (error) {
    console.error(error)
    ElMessage.error('操作失败')
  } finally {
    actionLoading.value = false
  }
}

const goToAttendance = () => {
  if (!currentNotice.value) return
  router.push({
    path: '/student/attendance',
    query: {
      courseId: currentNotice.value.courseId || undefined,
      status: 0,
      fromNotice: currentNotice.value.id
    }
  })
}

const formatTime = (value?: string) => {
  if (!value) return '--'
  return value.replace('T', ' ')
}
</script>

<template>
  <div class="min-h-full bg-gray-50 pb-8">
    <!-- Header -->
    <div class="bg-blue-600 pt-8 pb-20 px-4 relative overflow-hidden">
      <!-- Decorational background shapes -->
      <div class="absolute top-0 left-0 w-full h-full overflow-hidden opacity-20 pointer-events-none">
        <div class="absolute -top-10 -right-10 w-40 h-40 rounded-full bg-white mix-blend-overlay"></div>
        <div class="absolute top-20 -left-10 w-24 h-24 rounded-full bg-white mix-blend-overlay"></div>
      </div>
      <div class="relative z-10 flex items-center justify-between text-center max-w-2xl mx-auto">
        <h1 class="text-2xl font-bold text-white tracking-wide mb-1 flex items-center gap-2">
          <el-icon><Bell /></el-icon> 缺勤提醒
        </h1>
      </div>
    </div>

    <!-- Main Content -->
    <div class="px-4 -mt-10 relative z-20 space-y-4 max-w-2xl mx-auto">
      
      <!-- Statistics Cards -->
      <div class="grid grid-cols-2 gap-3">
        <div class="bg-white rounded-2xl p-5 shadow-xs border border-gray-100 flex flex-col justify-center transition-shadow hover:shadow-md relative overflow-hidden">
          <div class="flex items-center gap-1.5 text-gray-500 text-xs font-medium mb-1">
             未读提醒
          </div>
          <div class="text-3xl font-bold text-red-500">
            {{ stats.unread }}<span class="text-lg font-medium text-red-300 ml-1">条</span>
          </div>
          <div class="absolute -bottom-4 -right-4 text-red-50 opacity-50">
            <el-icon :size="80"><Bell /></el-icon>
          </div>
        </div>

        <div class="bg-white rounded-2xl p-5 shadow-xs border border-gray-100 flex flex-col justify-center transition-shadow hover:shadow-md relative overflow-hidden">
          <div class="flex items-center gap-1.5 text-gray-500 text-xs font-medium mb-1">
             累计收到
          </div>
          <div class="text-3xl font-bold text-blue-600">
            {{ stats.total }}<span class="text-lg font-medium text-blue-300 ml-1">条</span>
          </div>
          <div class="absolute -bottom-4 -right-4 text-blue-50 opacity-50">
            <el-icon :size="80"><Document /></el-icon>
          </div>
        </div>
      </div>

      <!-- Filter Tabs -->
      <div class="bg-white rounded-xl shadow-xs border border-gray-100 p-1 flex">
        <div class="flex-1 text-center py-2 text-sm font-medium rounded-lg cursor-pointer transition-colors"
             :class="filterStatus === '' ? 'bg-blue-50 text-blue-600' : 'text-gray-500 hover:bg-gray-50'"
             @click="filterStatus = ''">
          全部
        </div>
        <div class="flex-1 text-center py-2 text-sm font-medium rounded-lg cursor-pointer transition-colors"
             :class="filterStatus === 0 ? 'bg-red-50 text-red-600' : 'text-gray-500 hover:bg-gray-50'"
             @click="filterStatus = 0">
          未读
        </div>
        <div class="flex-1 text-center py-2 text-sm font-medium rounded-lg cursor-pointer transition-colors"
             :class="filterStatus === 1 ? 'bg-gray-100 text-gray-700' : 'text-gray-500 hover:bg-gray-50'"
             @click="filterStatus = 1">
          已知晓
        </div>
      </div>

      <!-- Notices List -->
      <div class="space-y-3">
        <div v-if="loading" class="bg-white rounded-xl py-10 text-center text-gray-400">
          <el-icon class="is-loading text-2xl mb-2"><Loading /></el-icon>
          <p class="text-sm">加载中...</p>
        </div>
        <div v-else-if="filteredNotices.length === 0" class="bg-white rounded-xl py-10 text-center text-gray-400">
          <el-icon class="text-4xl text-gray-200 mb-2"><Document /></el-icon>
          <p class="text-sm">暂无相关提醒</p>
        </div>
        
        <div v-for="notice in filteredNotices" :key="notice.id"
             @click="openDetail(notice)"
             class="bg-white rounded-2xl shadow-xs border border-gray-100 overflow-hidden hover:shadow-md transition-all cursor-pointer transform relative"
             :class="(notice.readStatus === 1) ? 'opacity-80' : ''">
          
          <!-- Unread Dot -->
          <div v-if="notice.readStatus === 0" class="absolute top-4 right-4 w-2.5 h-2.5 bg-red-500 rounded-full shadow-sm ring-2 ring-white z-10"></div>
          
          <div class="p-5 relative z-0">
            <div class="flex justify-between items-start mb-1 pr-4">
              <h3 class="font-bold text-gray-800 text-base flex-1 line-clamp-1" :class="notice.readStatus === 0 ? 'text-gray-900' : 'text-gray-600'">
                {{ notice.title }}
              </h3>
            </div>
            
            <div class="text-xs text-gray-500 space-y-1.5 mt-3">
              <p class="flex items-center gap-1.5"><el-icon class="text-blue-500"><Document /></el-icon> 课程：{{ notice.courseName }}</p>
              <p class="flex items-center gap-1.5"><el-icon class="text-orange-500"><WarningFilled /></el-icon> 累计缺勤：<span class="font-bold text-orange-500">{{ notice.absenceCountCount }} 次</span></p>
            </div>
            
            <div class="flex justify-between items-end mt-4 text-xs">
              <span class="text-gray-400">{{ formatTime(notice.sendTime) }}</span>
              <el-tag :type="notice.readStatus === 0 ? 'danger' : 'info'" size="small" round effect="light" class="border-none font-medium">
                {{ notice.readStatus === 0 ? '未读' : '已知晓' }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
    
      <div class="h-6"></div>
    </div>

    <!-- Details Drawer -->
    <el-drawer
      v-model="drawerVisible"
      :direction="drawerDirection"
      :size="drawerSize"
      title="提醒详情"
      class="mobile-friendly-drawer md:rounded-l-2xl md:rounded-tr-none rounded-t-2xl"
      :show-close="true"
      destroy-on-close
    >
      <div v-if="currentNotice" class="flex flex-col h-full bg-gray-50/50 -m-5">
        <div class="flex-1 overflow-y-auto p-5">
          <!-- Notification Status Banner -->
          <div class="rounded-xl flex flex-col p-4 mb-5 border relative overflow-hidden"
               :class="currentNotice.readStatus === 0 ? 'bg-red-50 border-red-100' : 'bg-gray-100 border-gray-200'">
            <div class="absolute -right-4 -bottom-4 opacity-10">
              <el-icon :size="100"><Bell /></el-icon>
            </div>
            <div class="relative z-10">
              <div class="flex items-center gap-2 font-bold text-base mb-1" :class="currentNotice.readStatus === 0 ? 'text-red-600' : 'text-gray-600'">
                <el-icon><WarningFilled /></el-icon> {{ currentNotice.title }}
              </div>
              <div class="text-xs" :class="currentNotice.readStatus === 0 ? 'text-red-400' : 'text-gray-400'">
                当前状态：{{ currentNotice.readStatus === 0 ? '未读/待确认' : '已知晓' }}
              </div>
            </div>
          </div>

          <!-- Message Body -->
          <div class="bg-white p-4 rounded-xl shadow-xs border border-gray-100 mb-5 relative">
            <h4 class="text-sm font-bold text-gray-800 mb-2 flex items-center gap-1.5"><el-icon><Document /></el-icon>内容明细</h4>
            <div class="text-sm text-gray-600 leading-relaxed indent-7 whitespace-pre-wrap">
              {{ currentNotice.content }}
            </div>
          </div>

          <!-- Attributes -->
          <el-descriptions :column="1" border size="small" class="custom-desc bg-white rounded-xl overflow-hidden shadow-xs border border-gray-100">
            <el-descriptions-item label="关联课程">{{ currentNotice.courseName }}</el-descriptions-item>
            <el-descriptions-item label="缺勤快照">
              <span class="text-orange-500 font-bold">累计缺勤 {{ currentNotice.absenceCountCount }} 次</span>
            </el-descriptions-item>
            <el-descriptions-item label="发送时间">{{ formatTime(currentNotice.sendTime) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- Action Buttons Pinned to Bottom -->
        <div class="p-4 bg-white border-t border-gray-100 shadow-[0_-4px_6px_-1px_rgba(0,0,0,0.05)]">
          <div class="flex flex-col gap-3">
             <el-button 
               v-if="currentNotice.readStatus === 0" 
               type="primary" 
               size="large" 
               class="w-full rounded-xl! text-base font-bold shadow-sm"
               :loading="actionLoading"
               @click="handleAcknowledge"
             >
               我已知晓
             </el-button>
             
             <el-button 
               v-else 
               disabled 
               size="large" 
               class="w-full rounded-xl! bg-gray-100! text-gray-400! border-gray-200!"
             >
               <el-icon class="mr-1"><Check /></el-icon> 已知晓
             </el-button>
             
             <el-button 
               plain
               size="large" 
               class="w-full rounded-xl! border-gray-300 text-gray-700 justify-center! ml-0!"
               @click="goToAttendance"
             >
               查看该课程考勤记录 <el-icon class="ml-1"><ArrowRight /></el-icon>
             </el-button>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
/* Mobile form adaptations */
.custom-desc :deep(.el-descriptions__label) {
    width: 90px;
    color: #6b7280;
    background-color: #f9fafb;
    font-size: 13px;
}
.custom-desc :deep(.el-descriptions__content) {
    font-size: 13px;
    color: #374151;
}

/* Make Drawer more app-like */
:deep(.mobile-friendly-drawer .el-drawer__header) {
    margin-bottom: 0;
    padding: 16px 20px;
    border-bottom: 1px solid #f3f4f6;
    color: #1f2937;
    font-weight: 700;
}
:deep(.mobile-friendly-drawer .el-drawer__body) {
    padding: 20px;
}
</style>
