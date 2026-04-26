import { defineStore } from 'pinia'
import type { StorageLike } from 'pinia-plugin-persistedstate'
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import type { UserLoginVO } from "@/types/api"

const authPersistStorage: StorageLike = {
  getItem: (key) => sessionStorage.getItem(key) || localStorage.getItem(key),
  setItem: (key, value) => {
    const state = JSON.parse(value)
    const targetStorage = state.rememberMe ? localStorage : sessionStorage
    const staleStorage = state.rememberMe ? sessionStorage : localStorage

    staleStorage.removeItem(key)
    targetStorage.setItem(key, value)
  }
}

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
  const rememberMe = ref(false)

  // 2. 权限计算属性 (参考 auth.ts)
  const isLoggedIn = computed(() => !!userInfo.value.token)
  const isTeacher = computed(() => userInfo.value.role === 1)
  const isFaceRegistered = computed(() => !!userInfo.value.avatarUrl)

  // 3. 操作方法
  const loginSuccess = (data: UserLoginVO, shouldRemember = false) => {
    rememberMe.value = shouldRemember
    userInfo.value = data
  }

  const updateUserInfo = (data: Partial<UserLoginVO>) => {
    userInfo.value = { ...userInfo.value, ...data }
  }

  const logout = () => {
    userInfo.value = {} as UserLoginVO
    rememberMe.value = false
    router.replace('/login')
  }

  return { userInfo, rememberMe, isLoggedIn, isTeacher, isFaceRegistered, loginSuccess, updateUserInfo, logout }
}, {
  persist: {
    storage: authPersistStorage
  }
})
