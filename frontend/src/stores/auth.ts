import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import type { UserLoginVO } from "@/types/api"

export const useAuthStore = defineStore('user', () => {
  const router = useRouter()

  // 1. 状态定义
  const userInfo = ref<UserLoginVO>({
    userId: 0,
    username: "",
    realName: "",
    role: 0,
    avatarUrl: "",
    token: "",
    adminClass: "",
  })

  // 2. 权限计算属性 (参考 auth.ts)
  const isLoggedIn = computed(() => !!userInfo.value.token)
  const isTeacher = computed(() => userInfo.value.role === 1)
  const isFaceRegistered = computed(() => !!userInfo.value.avatarUrl)

  // 3. 操作方法
  const loginSuccess = (data: UserLoginVO) => {
    userInfo.value = data
  }

  const updateUserInfo = (data: Partial<UserLoginVO>) => {
    userInfo.value = { ...userInfo.value, ...data }
  }

  const logout = () => {
    userInfo.value = {} as UserLoginVO
    router.replace('/login')
  }

  return { userInfo, isLoggedIn, isTeacher, isFaceRegistered, loginSuccess, updateUserInfo, logout }
}, {
  persist: true
})