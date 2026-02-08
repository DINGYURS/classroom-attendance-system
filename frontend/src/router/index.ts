import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '../components/layout/AppLayout.vue'
import Login from '../views/auth/Login.vue'

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
          component: () => import('../views/teacher/Dashboard.vue'), // Reuse for demo
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
      component: AppLayout,
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
    }
  ]
})

router.beforeEach((to, _from, next) => {
  // Update document title
  document.title = `${to.meta.title} - 云点名系统` || '云点名系统'
  next()
})

export default router
