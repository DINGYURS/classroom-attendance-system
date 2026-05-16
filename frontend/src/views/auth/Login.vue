<script setup lang="ts">
import { onMounted, ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, School, Male, Female, Postcard, Key, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { getCaptcha, login, register } from '@/api/auth'
import logoUrl from '@/assets/logo.webp'
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
  password: '',
  captchaKey: '',
  captchaCode: '',
  rememberMe: false
})

// Register Form
const registerForm = reactive<UserRegisterDTO & { confirmPassword?: string }>({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  role: 2,
  adminClass: '',
  gender: 1
})

// Toggle Mode
const toggleMode = () => {
  isRegister.value = !isRegister.value
  if (!isRegister.value) {
    refreshCaptcha()
  }
}

// 刷新验证码
const refreshCaptcha = async () => {
  try {
    const res = await getCaptcha()
    loginForm.captchaKey = res.data.captchaKey
    loginForm.captchaCode = ''
    captchaImage.value = res.data.captchaImage
  } catch (error: any) {
    console.error(error)
  }
}

// 处理登录
const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入完整账号密码')
    return
  }
  if (!loginForm.captchaCode) {
    ElMessage.warning('请输入验证码')
    return
  }

  isLoading.value = true
  try {
    const res = await login(loginForm)
    authStore.loginSuccess(res.data, !!loginForm.rememberMe)
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
    refreshCaptcha()
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
    await register(dto)
    
    ElMessage.success('注册成功，请登录')
    toggleMode()
  } catch (error: any) {
    console.error(error)
  } finally {
    isLoading.value = false
  }
}

const captchaImage = ref('')

onMounted(() => {
  refreshCaptcha()
})
</script>

<template>
  <div class="auth-page">
    <main class="auth-card">
      <div class="form-header">
        <div class="brand-chip">
          <span class="brand-mark">
            <img :src="logoUrl" alt="智能考勤系统 Logo" />
          </span>
          <span>智能考勤系统</span>
        </div>
        <h1>{{ isRegister ? '注册账号' : '登录系统' }}</h1>
        <p>{{ isRegister ? '选择身份后填写账号信息' : '请输入账号、密码和验证码' }}</p>
      </div>

      <div v-if="isRegister" class="role-tabs">
        <el-tabs v-model="activeTab" class="login-tabs" stretch>
          <el-tab-pane name="student">
            <template #label>
              <span class="tab-label">
                <el-icon><User /></el-icon>
                我是学生
              </span>
            </template>
          </el-tab-pane>
          <el-tab-pane name="teacher">
            <template #label>
              <span class="tab-label">
                <el-icon><School /></el-icon>
                我是教师
              </span>
            </template>
          </el-tab-pane>
        </el-tabs>
      </div>

      <div class="form-stack">
        <div class="field-group">
          <label>
            {{ isRegister ? (activeTab === 'teacher' ? '工号' : '学号') : '账号' }}
          </label>
          <el-input
            v-if="isRegister"
            v-model="registerForm.username"
            :prefix-icon="activeTab === 'teacher' ?  Postcard : User"
            :placeholder="activeTab === 'teacher' ? '请输入教师工号' : '请输入学生学号'"
            size="large"
            @keyup.enter="handleRegister"
          />
          <el-input
            v-else
            v-model="loginForm.username"
            :prefix-icon="User"
            placeholder="请输入账号"
            size="large"
            @keyup.enter="handleLogin"
          />
        </div>

        <div v-if="isRegister" class="field-group">
          <label>真实姓名</label>
          <el-input
            v-model="registerForm.realName"
            :prefix-icon="User"
            placeholder="请输入真实姓名"
            size="large"
            @keyup.enter="handleRegister"
          />
        </div>

        <div v-if="isRegister && activeTab === 'student'" class="form-grid">
          <div class="field-group">
            <label>行政班级</label>
            <el-input
              v-model="registerForm.adminClass"
              :prefix-icon="School"
              placeholder="例: 计科221"
              size="large"
              @keyup.enter="handleRegister"
            />
          </div>
          <div class="field-group">
            <label>性别</label>
            <el-select v-model="registerForm.gender" placeholder="请选择" size="large">
              <el-option label="男" :value="1">
                <div class="option-row"><el-icon><Male /></el-icon> 男</div>
              </el-option>
              <el-option label="女" :value="2">
                <div class="option-row"><el-icon><Female /></el-icon> 女</div>
              </el-option>
            </el-select>
          </div>
        </div>

        <div class="field-group">
          <label>密码</label>
          <el-input
            v-if="isRegister"
            v-model="registerForm.password"
            type="password"
            :prefix-icon="Lock"
            placeholder="请输入密码"
            show-password
            size="large"
            @keyup.enter="handleRegister"
          />
          <el-input
            v-else
            v-model="loginForm.password"
            type="password"
            :prefix-icon="Lock"
            placeholder="请输入密码"
            show-password
            size="large"
            @keyup.enter="handleLogin"
          />
        </div>

        <div v-if="!isRegister" class="field-group">
          <label>验证码</label>
          <div class="captcha-row">
            <el-input
              v-model="loginForm.captchaCode"
              :prefix-icon="Key"
              placeholder="请输入验证码"
              maxlength="4"
              size="large"
              @keyup.enter="handleLogin"
            />
            <button
              type="button"
              class="captcha-button"
              aria-label="刷新验证码"
              title="刷新验证码"
              @click="refreshCaptcha"
            >
              <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
              <el-icon v-else><Refresh /></el-icon>
            </button>
          </div>
        </div>

        <div v-if="isRegister" class="field-group">
          <label>确认密码</label>
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            :prefix-icon="Key"
            placeholder="请再次输入密码"
            show-password
            size="large"
            @keyup.enter="handleRegister"
          />
        </div>

        <div v-if="!isRegister" class="form-options">
          <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
        </div>

        <div class="action-stack">
          <el-button
            type="primary"
            size="large"
            class="submit-button"
            :loading="isLoading"
            @click="isRegister ? handleRegister() : handleLogin()"
          >
            {{ isRegister ? '完成注册' : '登录' }}
          </el-button>

          <button
            type="button"
            class="switch-button"
            @click="toggleMode"
          >
            {{ isRegister ? '已有账号，返回登录' : '没有账号，注册新账号' }}
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  padding: 28px 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #eef5ff 0%, #f8fbff 48%, #eef2f7 100%);
}

.auth-card {
  width: min(100%, 500px);
  padding: 38px 40px 34px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 28px 80px rgba(37, 99, 235, 0.13), 0 4px 18px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(12px);
}

.form-header {
  margin-bottom: 26px;
  text-align: center;
}

.brand-chip {
  width: fit-content;
  min-height: 42px;
  margin: 0 auto 18px;
  padding: 6px 14px 6px 8px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #f8fbff;
  color: #1d4ed8;
  font-size: 14px;
  font-weight: 700;
}

.brand-mark {
  width: 30px;
  height: 30px;
  display: block;
  overflow: hidden;
  border-radius: 50%;
  background: #ffffff;
}

.brand-mark img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.form-header h1 {
  margin: 0;
  color: #111827;
  font-size: 30px;
  line-height: 1.25;
  font-weight: 750;
  letter-spacing: 0;
}

.form-header p {
  margin: 0;
  margin-top: 8px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.5;
}

.role-tabs {
  margin-bottom: 22px;
}

.tab-label,
.option-row {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.form-stack {
  display: grid;
  gap: 18px;
}

.field-group {
  display: grid;
  gap: 8px;
}

.field-group label {
  color: #334155;
  font-size: 14px;
  line-height: 1.4;
  font-weight: 650;
}

.form-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(120px, 0.72fr);
  gap: 14px;
}

.captcha-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 118px;
  gap: 12px;
}

.captcha-button {
  height: 48px;
  width: 126px;
  padding: 3px 8px;
  display: grid;
  place-items: center;
  border: 1px solid #cbd5e1;
  border-radius: 16px;
  background: #f8fafc;
  color: #64748b;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.captcha-button:hover {
  border-color: #2563eb;
  background: #ffffff;
}

.captcha-button:focus-visible,
.switch-button:focus-visible {
  outline: 3px solid rgba(37, 99, 235, 0.22);
  outline-offset: 2px;
}

.captcha-button img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.form-options {
  min-height: 24px;
  display: flex;
  align-items: center;
}

.action-stack {
  padding-top: 4px;
  display: grid;
  gap: 12px;
}

.submit-button {
  width: 100%;
  min-height: 48px;
  border-radius: 16px !important;
  font-size: 15px !important;
  font-weight: 700 !important;
  box-shadow: 0 14px 28px rgba(37, 99, 235, 0.2);
}

.switch-button {
  min-height: 46px;
  border-radius: 14px;
  border: 0;
  background: transparent;
  color: #475569;
  font-size: 14px;
  font-weight: 650;
  cursor: pointer;
  transition: color 0.18s ease, background 0.18s ease;
}

.switch-button:hover {
  color: #2563eb;
  background: #f8fafc;
}

.login-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: #e2e8f0;
}

.login-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  color: #64748b;
}

.login-tabs :deep(.el-tabs__item.is-active) {
  color: var(--el-color-primary);
  font-weight: 700;
}

:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  min-height: 48px;
  border-radius: 16px;
  box-shadow: 0 0 0 1px #cbd5e1 inset;
}

:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px #94a3b8 inset;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px #2563eb inset, 0 0 0 4px rgba(37, 99, 235, 0.12);
}

:deep(.el-select) {
  width: 100%;
}

@media (max-width: 560px) {
  .auth-page {
    min-height: 100dvh;
    padding: 18px 14px;
    align-items: flex-start;
  }

  .auth-card {
    margin-top: 8px;
    padding: 28px 20px 24px;
    border-radius: 24px;
  }

  .form-header {
    margin-bottom: 22px;
  }

  .form-header h1 {
    font-size: 26px;
  }

  .brand-chip {
    margin-bottom: 14px;
  }

  .form-stack {
    gap: 16px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 360px) {
  .auth-page {
    padding: 0;
  }

  .auth-card {
    min-height: 100dvh;
    border-radius: 0;
    border-width: 0;
    box-shadow: none;
  }

  .captcha-row {
    grid-template-columns: minmax(0, 1fr);
  }
  
  .captcha-button {
    width: 100%;
  }
}
</style>
