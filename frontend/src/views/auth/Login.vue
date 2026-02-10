<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, VideoCamera, School, Male, Female, Postcard, Key } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { login, register } from '@/api/auth'
import type { UserLoginDTO, UserRegisterDTO } from '@/types/api'

const router = useRouter()
const authStore = useAuthStore()
const isLoading = ref(false)

// UI State
const isRegister = ref(false)
const activeTab = ref('student') // 'student' or 'teacher' (used for Registration role selection)

// Login Form
const loginForm = reactive<UserLoginDTO>({
  username: '',
  password: ''
})

// Register Form
const registerForm = reactive<UserRegisterDTO & { confirmPassword?: string }>({
  username: '', // StudentID or WorkID
  password: '',
  confirmPassword: '',
  realName: '',
  role: 2, // Default to Student (2)
  adminClass: '', // Only for Student
  gender: 1 // 1-Male, 2-Female
})

// Toggle Mode
const toggleMode = () => {
  isRegister.value = !isRegister.value
  // Reset forms/errors if needed
}

// ... (existing imports)

// 处理登录
const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入完整账号密码')
    return
  }

  isLoading.value = true
  try {
    const res = await login(loginForm)
    console.log(res)
    authStore.loginSuccess(res.data)
    ElMessage.success('登录成功')
    
    // Redirect based on role
    if (authStore.isTeacher) {
      router.push('/teacher/dashboard')
    } else {
      router.push('/student/profile')
    }
  } catch (error: any) {
    // Error handled in interceptor or here if re-thrown
    console.error(error)
  } finally {
    isLoading.value = false
  }
}

// 处理注册
const handleRegister = async () => {
  // Basic Validation
  if (!registerForm.username || !registerForm.password || !registerForm.realName) {
    ElMessage.warning('请填写所有必填字段')
    return
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  
  // Set role based on active tab
  registerForm.role = activeTab.value === 'teacher' ? 1 : 2
  
  // Student specific validation
  if (registerForm.role === 2 && !registerForm.adminClass) {
    ElMessage.warning('请填写行政班级')
    return
  }

  isLoading.value = true
  try {
    // Prepare DTO (remove confirmPassword)
    const { confirmPassword, ...dto } = registerForm
    console.log(dto)
    await register(dto)
    
    ElMessage.success('注册成功，请登录')
    toggleMode()
  } catch (error: any) {
    console.error(error)
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 relative overflow-hidden">
    <!-- Decorative Background Elements -->
    <div class="absolute inset-0 z-0 overflow-hidden">
      <div class="absolute -top-1/4 -right-1/4 w-[800px] h-[800px] rounded-full bg-blue-100/40 blur-3xl"></div>
      <div class="absolute -bottom-1/4 -left-1/4 w-[600px] h-[600px] rounded-full bg-indigo-100/40 blur-3xl"></div>
    </div>

    <!-- Login/Register Card -->
    <div class="relative z-10 w-full max-w-4xl bg-white rounded-2xl shadow-xl overflow-hidden flex flex-col md:flex-row m-4 transition-all duration-500"
         :class="{'h-auto': true}">
      
      <!-- Left Side - Visual -->
      <div class="md:w-1/2 bg-gradient-to-br from-blue-600 to-indigo-700 p-12 text-white flex flex-col justify-between relative overflow-hidden">
        <div class="relative z-10">
          <div class="flex items-center gap-3 mb-6">
            <div class="w-10 h-10 rounded-lg bg-white/20 flex items-center justify-center backdrop-blur-sm">
              <el-icon :size="20"><VideoCamera /></el-icon>
            </div>
            <span class="text-xl font-bold tracking-wide">课堂考勤系统</span>
          </div>
          
          <h1 class="text-3xl font-bold mb-4 leading-tight">
            {{ isRegister ? '加入智慧课堂' : '智慧课堂' }}<br>
            {{ isRegister ? '开启无感考勤' : '无感化考勤解决方案' }}
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
      <div class="md:w-1/2 p-8 md:p-12 flex flex-col justify-center bg-white transition-all duration-300">
        
        <!-- Header -->
        <div class="mb-8">
          <h2 class="text-2xl font-bold text-gray-900 mb-2">{{ isRegister ? '注册账号' : '欢迎回来' }}</h2>
          <p class="text-gray-500 text-sm">
            {{ isRegister ? '请填写信息完成注册' : '请输入账号密码登录系统' }}
          </p>
        </div>

        <!-- Role Selection Tabs (Only visible in Register mode) -->
        <div v-if="isRegister" class="mb-6">
           <el-tabs v-model="activeTab" class="login-tabs">
            <el-tab-pane name="student">
               <template #label>
                <span class="flex items-center gap-2 px-2 py-1">
                  <el-icon><User /></el-icon> 我是学生
                </span>
              </template>
            </el-tab-pane>
            <el-tab-pane name="teacher">
              <template #label>
                <span class="flex items-center gap-2 px-2 py-1">
                  <el-icon><School /></el-icon> 我是教师
                </span>
              </template>
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- Forms -->
        <div class="space-y-5">
          
          <!-- Shared: Username -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1.5 ml-1">
              {{ isRegister ? (activeTab === 'teacher' ? '工号' : '学号') : '账号' }}
            </label>
            <el-input 
              v-if="isRegister"
              v-model="registerForm.username"
              :prefix-icon="activeTab === 'teacher' ?  Postcard : User"
              :placeholder="activeTab === 'teacher' ? '请输入教师工号' : '请输入学生学号'"
              size="large"
            />
            <el-input 
              v-else
              v-model="loginForm.username"
              :prefix-icon="User"
              placeholder="请输入账号"
              size="large"
            />
          </div>

          <!-- Register Only: Real Name -->
          <div v-if="isRegister">
            <label class="block text-sm font-medium text-gray-700 mb-1.5 ml-1">真实姓名</label>
            <el-input 
              v-model="registerForm.realName"
              :prefix-icon="User"
              placeholder="请输入真实姓名"
              size="large"
            />
          </div>

          <!-- Register Only (Student): Admin Class & Gender -->
          <div v-if="isRegister && activeTab === 'student'" class="grid grid-cols-2 gap-4">
             <div>
              <label class="block text-sm font-medium text-gray-700 mb-1.5 ml-1">行政班级</label>
              <el-input 
                v-model="registerForm.adminClass"
                :prefix-icon="School"
                placeholder="例: 计科221"
                size="large"
              />
            </div>
             <div>
              <label class="block text-sm font-medium text-gray-700 mb-1.5 ml-1">性别</label>
              <el-select v-model="registerForm.gender" placeholder="请选择" size="large">
                <el-option label="男" :value="1">
                   <div class="flex items-center gap-2"><el-icon><Male /></el-icon> 男</div>
                </el-option>
                <el-option label="女" :value="2">
                   <div class="flex items-center gap-2"><el-icon><Female /></el-icon> 女</div>
                </el-option>
              </el-select>
            </div>
          </div>

          <!-- Shared: Password -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1.5 ml-1">密码</label>
            <el-input 
              v-if="isRegister"
              v-model="registerForm.password"
              type="password"
              :prefix-icon="Lock"
              placeholder="请输入密码"
              show-password
              size="large"
            />
            <el-input 
              v-else
              v-model="loginForm.password"
              type="password"
              :prefix-icon="Lock"
              placeholder="请输入密码"
              show-password
              size="large"
            />
          </div>

           <!-- Register Only: Confirm Password -->
          <div v-if="isRegister">
            <label class="block text-sm font-medium text-gray-700 mb-1.5 ml-1">确认密码</label>
            <el-input 
              v-model="registerForm.confirmPassword"
              type="password"
              :prefix-icon="Key"
              placeholder="请再次输入密码"
              show-password
              size="large"
            />
          </div>

          <!-- Login Only: Remember & Forgot -->
          <div v-if="!isRegister" class="flex items-center justify-between text-sm">
            <el-checkbox>记住我</el-checkbox>
            <a href="#" class="text-blue-600 hover:text-blue-700 font-medium">忘记密码?</a>
          </div>

          <!-- Actions -->
          <div class="pt-2 space-y-3">
            <el-button 
              type="primary" 
              size="large" 
              class="w-full !rounded-lg !text-base !font-medium !h-11 shadow-lg shadow-blue-500/20" 
              :loading="isLoading"
              @click="isRegister ? handleRegister() : handleLogin()"
            >
              {{ isRegister ? '立即注册' : '立即登录' }}
            </el-button>
            
            <div class="text-center">
              <button 
                @click="toggleMode"
                class="text-sm text-gray-500 hover:text-blue-600 font-medium transition-colors"
              >
                {{ isRegister ? '已有账号？返回登录' : '没有账号？立即注册' }}
              </button>
            </div>
          </div>

        </div>

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
