import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const instance = axios.create({
  baseURL: '/api', // Proxy target in vite.config.ts should map this to backend
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 添加请求拦截器
instance.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.userInfo.token) {
      // 后端规定的字段名为 token
      config.headers['token'] = authStore.userInfo.token
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 添加响应拦截器
instance.interceptors.response.use(
  (res) => {
    // If the custom code is not 0, it is judged as an error.
    if (res.data.code !== 1) {
      ElMessage.error(res.data.message || 'Error')

      // 401: Token expired or invalid
      // Adjust the condition based on your backend error codes for auth failure
      // if (res.code === 401) {
      //   const authStore = useAuthStore()
      //   authStore.logout()
      //   location.reload()
      // }
      return Promise.reject(new Error(res.data.message || 'Error'))
    } else {
      return res.data
    }
  },
  (error) => {
    console.log('err' + error)
    let message = error.message || 'Request Error'
    if (error.response && error.response.status === 401) {
      message = '登录已过期，请重新登录'
      const authStore = useAuthStore()
      authStore.logout()
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default instance
