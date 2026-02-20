import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '../components/layout/AppLayout.vue'
import StudentLayout from '../components/layout/StudentLayout.vue'
import Login from '../views/auth/Login.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: Login,
      meta: { title: '登录' }
    },
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/teacher',
      component: AppLayout,
      redirect: '/teacher/dashboard',
      meta: { title: '教师端' },
      children: [
        {
          path: 'dashboard',
          name: 'teacher-dashboard',
          component: () => import('../views/teacher/Dashboard.vue'),
          meta: { title: '工作台' }
        },
        {
          path: 'course',
          name: 'teacher-course',
          component: () => import('../views/teacher/Dashboard.vue'), // 复用演示
          meta: { title: '课程管理' }
        },
        {
          path: 'rollcall/:id',
          name: 'teacher-rollcall',
          component: () => import('../views/teacher/RollCallSession.vue'),
          meta: { title: '正在点名' }
        }
      ]
    },
    {
      path: '/student',
      component: StudentLayout,
      redirect: '/student/profile',
      meta: { title: '学生端' },
      children: [
        {
          path: 'profile',
          name: 'student-profile',
          component: () => import('../views/student/Profile.vue'),
          meta: { title: '个人中心' }
        }
      ]
    },
    {
      path: '/student/face-register',
      name: 'student-face-register',
      component: () => import('../views/student/FaceRegister.vue'),
      meta: { title: '人脸注册' }
    }
  ]
})

router.beforeEach((to, _from, next) => {
  // 更新文档标题
  document.title = (to.meta.title as string) ? `${to.meta.title} - 云点名系统` : '云点名系统'

  const authStore = useAuthStore()

  // 1. 检查用户是否已登录
  if (!authStore.isLoggedIn && to.path !== '/login') {
    next('/login')
    return
  }

  // 2. 如果已登录并访问登录页，重定向到控制台
  if (authStore.isLoggedIn && to.path === '/login') {
    if (authStore.isTeacher) {
      next('/teacher/dashboard')
    } else {
      next('/student/profile')
    }
    return
  }

  // 3. 基于角色的访问控制
  if (to.path.startsWith('/teacher') && !authStore.isTeacher) {
    next('/student/profile')
    return
  }
  
  if (to.path.startsWith('/student') && authStore.isTeacher) {
    next('/teacher/dashboard')
    return
  }

  next()
})

export default router
