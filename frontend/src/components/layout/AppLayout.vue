<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { 
  Menu as IconMenu, 
  User, 
  Setting, 
  Fold, 
  Expand,
  Monitor,
  SwitchButton
} from '@element-plus/icons-vue'

const isCollapse = ref(false)
const route = useRoute()
const router = useRouter()

// Mock user info - should come from Pinia store
const userRole = ref('teacher') // 'teacher' | 'student'
const userName = ref(' 张老师')

const toggleSidebar = () => {
  isCollapse.value = !isCollapse.value
}

const handleLogout = () => {
  // TODO: Implement logout logic
  router.push('/login')
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
          <span class="font-bold text-gray-800 text-lg whitespace-nowrap">云点名系统</span>
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
        <template v-if="userRole === 'teacher'">
          <el-menu-item index="/teacher/dashboard">
            <el-icon><Monitor /></el-icon>
            <template #title>工作台</template>
          </el-menu-item>
          <el-menu-item index="/teacher/course">
            <el-icon><IconMenu /></el-icon>
            <template #title>课程管理</template>
          </el-menu-item>
        </template>

        <!-- Student Menus -->
        <template v-if="userRole === 'student'">
          <el-menu-item index="/student/profile">
            <el-icon><User /></el-icon>
            <template #title>个人中心</template>
          </el-menu-item>
        </template>
      </el-menu>

      <div class="p-4 border-t border-gray-100">
        <div v-if="!isCollapse" class="flex items-center gap-3 mb-4 px-2">
          <el-avatar :size="36" class="bg-blue-100 text-blue-600">{{ userName.charAt(0) }}</el-avatar>
          <div class="flex-1 min-w-0">
            <p class="text-sm font-medium text-gray-900 truncate">{{ userName }}</p>
            <p class="text-xs text-gray-500 truncate">{{ userRole === 'teacher' ? '教师' : '学生' }}</p>
          </div>
        </div>
        <el-button 
          type="danger" 
          plain 
          class="w-full justify-start" 
          :class="{ 'px-2': isCollapse }"
          @click="handleLogout"
        >
          <el-icon><SwitchButton /></el-icon>
          <span v-if="!isCollapse" class="ml-2">退出登录</span>
        </el-button>
      </div>
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

        <div class="flex items-center gap-4">
          <el-button circle plain>
            <el-icon><Setting /></el-icon>
          </el-button>
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
