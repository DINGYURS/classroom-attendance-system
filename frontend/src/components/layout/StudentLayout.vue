<script setup lang="ts">
import { useRouter } from 'vue-router'
import { SwitchButton } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()


const handleCommand = (command: string) => {
  if (command === 'logout') {
    authStore.logout()
  } else if (command === 'profile') {
    router.push('/student/profile')
  }
}
</script>

<template>
  <div class="common-layout h-screen flex flex-col overflow-hidden bg-gray-50">
    <!-- Header (Mobile Friendly) -->
    <header class="h-14 md:h-16 bg-white border-b border-gray-200 flex items-center justify-between px-4 md:px-6 shadow-xs sticky top-0 z-10 shrink-0">
      <!-- Logo & Title -->
      <div class="flex items-center gap-2" @click="router.push('/student/profile')" style="cursor: pointer;">
        <div class="w-8 h-8 rounded bg-blue-600 flex items-center justify-center text-white font-bold text-sm md:text-base">RC</div>
        <span class="font-bold text-gray-800 text-base md:text-lg whitespace-nowrap">云点名系统</span>
      </div>

      <!-- Right Side Logout Button -->
      <div class="flex items-center">
        <el-button type="danger" plain @click="handleCommand('logout')" class="font-medium">
          <el-icon class="mr-1"><SwitchButton /></el-icon>
          退出登录
        </el-button>
      </div>
    </header>

    <!-- Main Content Area -->
    <main class="flex-1 overflow-y-auto overflow-x-hidden relative scroll-smooth">
      <div class="max-w-3xl mx-auto w-full pb-8">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>
  </div>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.fade-enter-from {
  opacity: 0;
  transform: translateY(10px);
}
.fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>

