<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Camera, List, Edit, Key, User, Checked, Warning, ArrowRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { updateStudentInfo, getAttendanceRecords, getStudentInfo } from '@/api/student'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const loading = ref(false)

const studentInfo = computed(() => {
  const user = authStore.userInfo
  return {
    name: user.realName || user.username || '未知姓名',
    id: user.username || '',
    className: user.adminClass || '暂无班级',
    avatar: user.avatarUrl || '',
    faceRegistered: authStore.isFaceRegistered
  }
})

const activities = ref<any[]>([])

const dialogVisible = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  realName: '',
  password: '',
  confirmPassword: ''
})

const validatePass2 = (_rule: any, value: any, callback: any) => {
  if (value === '') {
    if (form.password !== '') {
      callback(new Error('请再次输入密码以确认'))
    } else {
      callback()
    }
  } else if (value !== form.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const rules = reactive<FormRules>({
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validatePass2, trigger: 'blur' }
  ]
})

const getStatusType = (status: number) => {
  switch (status) {
    case 1: return 'success'
    case 2: return 'warning'
    case 3: return 'warning'
    case 4: return 'danger'
    case 5: return 'info'
    default: return 'info'
  }
}

const getStatusText = (status: number) => {
  switch (status) {
    case 1: return '正常出勤'
    case 2: return '迟到'
    case 3: return '早退'
    case 4: return '缺勤'
    case 5: return '请假'
    default: return '未签到'
  }
}

const fetchData = async () => {
  try {
    const res = await getAttendanceRecords()
    if (res && res.code === 1) {
      activities.value = (res.data || []).map((item) => ({
        content: `《${item.courseName || '未知课程'}》考勤 - ${item.statusText || getStatusText(item.status)}`,
        timestamp: item.attendanceTime || '',
        type: getStatusType(item.status),
        icon: item.status === 1 ? Checked : (item.status === 4 ? Warning : List)
      }))
    }
  } catch (error) {
    console.error('获取考勤记录失败', error)
  }
}

const handleEdit = () => {
  form.realName = studentInfo.value.name
  form.password = ''
  form.confirmPassword = ''
  dialogVisible.value = true
}

const submitForm = async (formEl: FormInstance | undefined) => {
  if (!formEl) return
  await formEl.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const payload: any = {
          realName: form.realName
        }
        if (form.password) {
          payload.password = form.password
        }

        const res = await updateStudentInfo(payload)
        if (res && res.code === 1) {
          if (form.password) {
            ElMessage.error('身份信息已过期，请重新登录')
            dialogVisible.value = false
            setTimeout(() => {
              authStore.logout()
            }, 1000)
          } else {
            ElMessage.success('个人信息修改成功')
            dialogVisible.value = false

            try {
              const infoRes = await getStudentInfo()
              if (infoRes && infoRes.code === 1) {
                authStore.updateUserInfo({
                  realName: infoRes.data.realName,
                  avatarUrl: infoRes.data.avatarUrl,
                  adminClass: infoRes.data.adminClass
                })
              }
            } catch (err) {
              console.error('获取最新学生信息失败', err)
            }
          }
        }
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="min-h-full bg-gray-50 pb-6">
    <!-- Top Banner & Profile Header -->
    <div class="bg-blue-600 pt-8 pb-16 px-4 relative overflow-hidden">
      <!-- Decorational background shapes -->
      <div class="absolute top-0 left-0 w-full h-full overflow-hidden opacity-20 pointer-events-none">
        <div class="absolute -top-10 -right-10 w-40 h-40 rounded-full bg-white mix-blend-overlay"></div>
        <div class="absolute top-20 -left-10 w-24 h-24 rounded-full bg-white mix-blend-overlay"></div>
      </div>

      <div class="relative z-10 flex flex-col items-center justify-center text-center">
        <el-avatar :size="84" class="border-4 border-white/30 shadow-sm bg-white text-3xl font-bold text-gray-400 mb-4 transition-transform hover:scale-105">
           <span v-if="!studentInfo.avatar" class="text-blue-600">{{ studentInfo.name.charAt(0) }}</span>
           <img v-else :src="studentInfo.avatar" class="w-full h-full object-cover" />
        </el-avatar>
        <h1 class="text-2xl font-bold text-white mb-1 flex items-center justify-center gap-2">
          {{ studentInfo.name }}
          <el-icon class="text-emerald-400 text-lg" v-if="studentInfo.faceRegistered"><Checked /></el-icon>
        </h1>
        <div class="flex items-center justify-center gap-3 text-blue-100 text-sm">
           <span class="bg-blue-700/50 px-2 py-0.5 rounded-full font-mono">{{ studentInfo.id }}</span>
           <span>{{ studentInfo.className }}</span>
        </div>
      </div>
    </div>

    <!-- Main Content Overlapping Header -->
    <div class="px-4 -mt-10 relative z-20 space-y-4 max-w-lg mx-auto">
      
      <!-- Quick Actions / Face DB Card -->
      <div class="bg-white rounded-2xl shadow-xs p-5 flex items-center justify-between border border-gray-100 hover:shadow-md transition-shadow cursor-pointer" @click="$router.push('/student/face-register')">
        <div class="flex items-center gap-4">
          <div :class="['w-12 h-12 rounded-full flex items-center justify-center text-xl transition-colors', studentInfo.faceRegistered ? 'bg-emerald-50 text-emerald-500' : 'bg-orange-50 text-orange-500']">
            <el-icon><Camera /></el-icon>
          </div>
          <div>
            <h3 class="font-bold text-gray-800 tracking-wide text-base">人脸底库</h3>
            <p class="text-xs mt-1" :class="studentInfo.faceRegistered ? 'text-emerald-600' : 'text-orange-500'">
              {{ studentInfo.faceRegistered ? '已录入，点击可更新' : '未录入，点击立即录入' }}
            </p>
          </div>
        </div>
        <div class="text-gray-300">
           <el-icon><ArrowRight /></el-icon>
        </div>
      </div>

      <!-- Edit Profile Action Card -->
      <div class="bg-white rounded-2xl shadow-xs p-5 flex items-center justify-between border border-gray-100 hover:shadow-md transition-shadow cursor-pointer" @click="handleEdit">
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 rounded-full bg-blue-50 text-blue-500 flex items-center justify-center text-xl">
            <el-icon><Edit /></el-icon>
          </div>
          <div>
            <h3 class="font-bold text-gray-800 tracking-wide text-base">编辑资料</h3>
            <p class="text-xs text-gray-500 mt-1">设置密码或修改基本信息</p>
          </div>
        </div>
        <div class="text-gray-300">
           <el-icon><ArrowRight /></el-icon>
        </div>
      </div>

      <!-- Attendance Records -->
      <div class="bg-white rounded-2xl shadow-xs border border-gray-100 overflow-hidden">
        <div class="px-5 py-4 border-b border-gray-50/80 bg-gray-50/50 flex items-center justify-between">
          <h3 class="font-bold text-gray-800 flex items-center gap-2 text-base tracking-wide">
            <el-icon class="text-blue-500"><List /></el-icon> 最近考勤
          </h3>
        </div>
        
        <div class="p-5">
          <el-skeleton v-if="loading && activities.length === 0" :rows="3" animated />
          
          <div v-else-if="activities.length === 0" class="py-8 text-center flex flex-col items-center justify-center text-gray-400">
            <el-icon class="text-4xl text-gray-200 mb-2"><List /></el-icon>
            <span class="text-sm">暂无考勤记录</span>
          </div>

          <el-timeline v-else class="pl-2 pr-1 mobile-timeline">
            <el-timeline-item
              v-for="(activity, index) in activities"
              :key="index"
              :type="activity.type"
              :timestamp="activity.timestamp"
              :icon="activity.icon"
              size="large"
            >
              <div class="text-sm text-gray-800 leading-relaxed font-medium">
                {{ activity.content }}
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </div>

    <!-- Edit Profile Dialog (Mobile Adapted) -->
    <el-dialog
      v-model="dialogVisible"
      title="修改个人信息"
      width="90%"
      max-width="400px"
      :close-on-click-modal="false"
      destroy-on-close
      class="mobile-friendly-dialog mt-20"
      :show-close="false"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" class="mt-2">
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" :prefix-icon="User" size="large" />
        </el-form-item>
        
        <div class="h-4"></div>
        <div class="text-xs text-gray-400 mb-4 flex items-center gap-2">
           <div class="h-px bg-gray-200 flex-1"></div>
           <span>修改密码（可选）</span>
           <div class="h-px bg-gray-200 flex-1"></div>
        </div>
        
        <el-form-item label="新密码" prop="password">
          <el-input 
            v-model="form.password" 
            type="password" 
            placeholder="不修改请留空" 
            show-password
            :prefix-icon="Key"
            size="large"
          />
        </el-form-item>
        
        <el-form-item 
          label="确认密码" 
          prop="confirmPassword"
          v-if="form.password"
        >
          <el-input 
            v-model="form.confirmPassword" 
            type="password" 
            placeholder="请再次输入新密码" 
            show-password
            :prefix-icon="Key"
            size="large"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex gap-3 pt-2">
          <el-button @click="dialogVisible = false" class="flex-1" size="large">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submitForm(formRef)" class="flex-1" size="large">
            保 存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* Optimize Timeline for Mobile: Reduce left padding since space is tight */
.mobile-timeline :deep(.el-timeline-item__wrapper) {
  padding-left: 24px;
}
.mobile-timeline :deep(.el-timeline-item__content) {
  margin-top: -2px;
}
.mobile-timeline :deep(.el-timeline-item__timestamp) {
  margin-top: 6px;
  font-size: 13px;
  color: #9ca3af;
}

/* Make Dialog more app-like */
:deep(.mobile-friendly-dialog) {
  border-radius: 16px;
  overflow: hidden;
}
:deep(.mobile-friendly-dialog .el-dialog__header) {
  padding: 20px 20px 10px;
  margin-right: 0;
  text-align: center;
}
:deep(.mobile-friendly-dialog .el-dialog__title) {
  font-weight: 700;
  color: #1f2937;
  font-size: 18px;
}
:deep(.mobile-friendly-dialog .el-dialog__body) {
  padding: 10px 24px 20px;
}
:deep(.mobile-friendly-dialog .el-dialog__footer) {
  padding: 15px 24px 20px;
  border-top: 1px solid #f3f4f6;
}
</style>
