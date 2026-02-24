<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { 
  Menu as IconMenu, 
  User, 
  Fold, 
  Expand,
  Monitor,
  SwitchButton,
  Postcard,
  Lock,
  Key
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { updateTeacherProfile } from '@/api/teacher'

const isCollapse = ref(true)
const route = useRoute()
const authStore = useAuthStore()

// User Info
const userName = computed(() => authStore.userInfo.realName || authStore.userInfo.username || '用户')
const userInitial = computed(() => userName.value ? userName.value.charAt(0) : 'U')

const toggleSidebar = () => {
  isCollapse.value = !isCollapse.value
}

const handleCommand = (command: string) => {
  if (command === 'logout') {
    authStore.logout()
  } else if (command === 'profile') {
    openProfileDialog()
  }
}

// 教师个人中心对话框
const showProfileDialog = ref(false)
const profileLoading = ref(false)
const profileForm = reactive({
  realName: '',
  jobNumber: '',
  password: '',
  confirmPassword: ''
})

const openProfileDialog = () => {
  profileForm.realName = authStore.userInfo.realName
  profileForm.jobNumber = authStore.userInfo.username
  profileForm.password = ''
  profileForm.confirmPassword = ''
  showProfileDialog.value = true
}

const handleUpdateProfile = async () => {
  if (!profileForm.realName || !profileForm.jobNumber) {
    ElMessage.warning('姓名和工号不能为空')
    return
  }

  if (profileForm.password && profileForm.password !== profileForm.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  
  profileLoading.value = true
  try {
    await updateTeacherProfile({
      realName: profileForm.realName,
      jobNumber: profileForm.jobNumber,
      password: profileForm.password || undefined
    })
    
    // 如果修改了密码，要求重新登录
    if (profileForm.password) {
      ElMessage.success('密码修改成功，请使用新密码重新登录')
      showProfileDialog.value = false
      // 延迟一秒退出，让用户看清提示
      setTimeout(() => {
        authStore.logout()
      }, 1000)
    } else {
      // 仅更新了基本信息
      authStore.updateUserInfo({
        realName: profileForm.realName,
        username: profileForm.jobNumber
      })
      ElMessage.success('个人信息更新成功')
      showProfileDialog.value = false
    }
  } catch (error) {
    console.error(error)
  } finally {
    profileLoading.value = false
  }
}

const activeMenu = computed(() => route.path)
</script>

<template>
  <div class="common-layout h-screen flex overflow-hidden bg-gray-50">
    <!-- Sidebar -->
    <aside 
      class="bg-white border-r border-gray-200 transition-all duration-300 flex flex-col"
      :class="isCollapse ? 'w-16' : 'w-64'"
    >
      <div class="h-16 flex items-center justify-center border-b border-gray-100">
        <div v-if="!isCollapse" class="flex items-center gap-2 px-4 transition-opacity duration-300">
          <div class="w-8 h-8 rounded bg-blue-600 flex items-center justify-center text-white font-bold">RC</div>
          <span class="font-bold text-gray-800 text-lg whitespace-nowrap">智能考勤系统</span>
        </div>
        <div v-else class="w-8 h-8 rounded bg-blue-600 flex items-center justify-center text-white font-bold text-sm">RC</div>
      </div>

      <el-menu
        :default-active="activeMenu"
        class="border-none flex-1 router-menu"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
      >
        <!-- Teacher Menus -->
        <el-menu-item index="/teacher/dashboard">
          <el-icon><Monitor /></el-icon>
          <template #title>工作台</template>
        </el-menu-item>
        <el-menu-item index="/teacher/course">
          <el-icon><IconMenu /></el-icon>
          <template #title>课程管理</template>
        </el-menu-item>
        <el-menu-item index="/teacher/student">
          <el-icon><User /></el-icon>
          <template #title>学生管理</template>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- Main Content -->
    <div class="flex-1 flex flex-col min-w-0">
      <!-- Header -->
      <header class="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6">
        <div class="flex items-center gap-4">
          <el-button link @click="toggleSidebar">
            <el-icon :size="20">
              <Expand v-if="isCollapse" />
              <Fold v-else />
            </el-icon>
          </el-button>
          
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title || '当前页面' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <!-- Right Side User Dropdown -->
        <div class="flex items-center gap-4">
          <el-dropdown trigger="hover" @command="handleCommand" popper-class="user-dropdown-popper">
            <span class="flex items-center gap-2 cursor-pointer outline-none transition-opacity hover:opacity-80">
              <el-avatar :size="36" class="bg-blue-100 text-blue-600 font-medium select-none">
                {{ userInitial }}
              </el-avatar>
            </span>
            <template #dropdown>
              <el-dropdown-menu class="min-w-[180px]">
                <!-- User Info Header -->
                <div class="px-4 py-3 border-b border-gray-100 mb-1">
                  <p class="text-sm font-bold text-gray-900 truncate">{{ userName }}</p>
                  <p class="text-xs text-gray-500 mt-1">教师</p>
                </div>
                
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" class="danger-item">
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- Page Content -->
      <main class="flex-1 overflow-auto p-6 relative">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>

    <!-- 教师个人中心对话框 -->
    <el-dialog
      v-model="showProfileDialog"
      title="个人中心"
      width="400px"
      append-to-body
      destroy-on-close
      class="profile-dialog"
    >
      <div class="py-2">
        <el-form :model="profileForm" label-position="top">
          <el-form-item label="真实姓名" required>
            <el-input v-model="profileForm.realName" :prefix-icon="User" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="工号" required>
            <el-input v-model="profileForm.jobNumber" :prefix-icon="Postcard" placeholder="请输入工号" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input 
              v-model="profileForm.password" 
              type="password" 
              :prefix-icon="Lock" 
              placeholder="不修改请留空" 
              show-password 
            />
          </el-form-item>
          <el-form-item label="确认新密码" v-if="profileForm.password">
            <el-input 
              v-model="profileForm.confirmPassword" 
              type="password" 
              :prefix-icon="Key" 
              placeholder="请再次输入新密码" 
              show-password 
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="showProfileDialog = false">取消</el-button>
          <el-button type="primary" :loading="profileLoading" @click="handleUpdateProfile">
            保存修改
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.router-menu :deep(.el-menu-item.is-active) {
  background-color: var(--color-primary-50);
  border-right: 3px solid var(--color-primary-600);
  color: var(--color-primary-700);
  font-weight: 600;
}

.router-menu :deep(.el-menu-item:hover) {
  background-color: var(--color-primary-50);
  color: var(--color-primary-600);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

<style>
.user-dropdown-popper .el-dropdown-menu__item.danger-item {
  color: var(--el-color-danger) !important;
}

.user-dropdown-popper .el-dropdown-menu__item.danger-item:hover {
  color: var(--el-color-danger) !important;
  background-color: var(--el-color-danger-light-9) !important;
}
</style>
