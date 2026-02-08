<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, VideoCamera, School } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const activeTab = ref('teacher')
const isLoading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入完整账号密码')
    return
  }

  isLoading.value = true
  
  // Mock login delay
  setTimeout(() => {
    isLoading.value = false
    ElMessage.success('登录成功')
    
    if (activeTab.value === 'teacher') {
      router.push('/teacher/dashboard')
    } else {
      router.push('/student/profile')
    }
  }, 1000)
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 relative overflow-hidden">
    <!-- Decorative Background Elements -->
    <div class="absolute inset-0 z-0 overflow-hidden">
      <div class="absolute -top-1/4 -right-1/4 w-[800px] h-[800px] rounded-full bg-blue-100/40 blur-3xl"></div>
      <div class="absolute -bottom-1/4 -left-1/4 w-[600px] h-[600px] rounded-full bg-indigo-100/40 blur-3xl"></div>
    </div>

    <!-- Login Card -->
    <div class="relative z-10 w-full max-w-4xl bg-white rounded-2xl shadow-xl overflow-hidden flex flex-col md:flex-row m-4">
      
      <!-- Left Side - Visual -->
      <div class="md:w-1/2 bg-gradient-to-br from-blue-600 to-indigo-700 p-12 text-white flex flex-col justify-between relative overflow-hidden">
        <div class="relative z-10">
          <div class="flex items-center gap-3 mb-6">
            <div class="w-10 h-10 rounded-lg bg-white/20 flex items-center justify-center backdrop-blur-sm">
              <el-icon :size="20"><VideoCamera /></el-icon>
            </div>
            <span class="text-xl font-bold tracking-wide">RollCall 系统</span>
          </div>
          
          <h1 class="text-3xl font-bold mb-4 leading-tight">
            智慧课堂<br>无感化考勤解决方案
          </h1>
          <p class="text-blue-100 text-sm leading-relaxed opacity-90">
            利用计算机视觉技术，只需一张合照即可完成全班点名。告别繁琐的传统点名，让课堂回归教学本质。
          </p>
        </div>

        <div class="relative z-10 mt-12 grid grid-cols-2 gap-4">
          <div class="bg-white/10 backdrop-blur-sm rounded-lg p-4">
            <p class="text-2xl font-bold mb-1">99%</p>
            <p class="text-xs text-blue-200">人脸识别准确率</p>
          </div>
          <div class="bg-white/10 backdrop-blur-sm rounded-lg p-4">
            <p class="text-2xl font-bold mb-1">2s</p>
            <p class="text-xs text-blue-200">平均点名耗时</p>
          </div>
        </div>

        <!-- Decorative circles -->
        <div class="absolute top-10 right-10 w-32 h-32 rounded-full border border-white/10"></div>
        <div class="absolute bottom-[-20px] left-[-20px] w-48 h-48 rounded-full bg-white/5 blur-xl"></div>
      </div>

      <!-- Right Side - Form -->
      <div class="md:w-1/2 p-8 md:p-12 flex flex-col justify-center">
        <div class="mb-8">
          <h2 class="text-2xl font-bold text-gray-900 mb-2">欢迎回来</h2>
          <p class="text-gray-500 text-sm">请选择您的身份并登录系统</p>
        </div>

        <el-tabs v-model="activeTab" class="login-tabs mb-6">
          <el-tab-pane name="teacher">
            <template #label>
              <span class="flex items-center gap-2 px-2 py-1">
                <el-icon><School /></el-icon> 我是教师
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="student">
             <template #label>
              <span class="flex items-center gap-2 px-2 py-1">
                <el-icon><User /></el-icon> 我是学生
              </span>
            </template>
          </el-tab-pane>
        </el-tabs>

        <form @submit.prevent="handleLogin" class="space-y-5">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1.5 ml-1">
              {{ activeTab === 'teacher' ? '工号' : '学号' }}
            </label>
            <el-input 
              v-model="loginForm.username"
              :prefix-icon="User"
              :placeholder="activeTab === 'teacher' ? '请输入教师工号' : '请输入学生学号'"
              size="large"
            />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1.5 ml-1">密码</label>
            <el-input 
              v-model="loginForm.password"
              type="password"
              :prefix-icon="Lock"
              placeholder="请输入密码"
              show-password
              size="large"
            />
          </div>

          <div class="flex items-center justify-between text-sm">
            <el-checkbox>记住我</el-checkbox>
            <a href="#" class="text-blue-600 hover:text-blue-700 font-medium">忘记密码?</a>
          </div>

          <el-button 
            type="primary" 
            size="large" 
            class="w-full !rounded-lg !text-base !font-medium !h-11 shadow-lg shadow-blue-500/20" 
            :loading="isLoading"
            @click="handleLogin"
          >
            立即登录
          </el-button>
        </form>

        <div class="mt-8 text-center text-xs text-gray-400">
          RollCall System v1.0 &copy; 2026
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: #f3f4f6;
}
.login-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  color: #6b7280;
}
.login-tabs :deep(.el-tabs__item.is-active) {
  color: var(--el-color-primary);
  font-weight: 600;
}
</style>
