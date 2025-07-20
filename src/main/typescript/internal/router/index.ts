import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user.ts'

// 路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: {
      requiresAuth: false,
      title: '登录'
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: {
      requiresAuth: false,
      title: '注册'
    }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: {
      requiresAuth: true,
      title: '仪表板'
    }
  },
  {
    path: '/about',
    name: 'About',
    component: () => import('@/views/AboutView.vue'),
    meta: {
      requiresAuth: false,
      title: '关于'
    }
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_, __, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// 全局前置守卫 - 认证检查
router.beforeEach(async (to, _, next) => {
  const userStore = useUserStore()
  
  // 设置页面标题
  if (to.meta?.title) {
    document.title = `${to.meta.title} - Lyra 文档管理系统`
  }
  
  // 检查是否需要认证
  if (to.meta?.requiresAuth === false) {
    // 不需要认证的页面，如果已登录则重定向到仪表板
    if (userStore.isAuthenticated && (to.name === 'Login' || to.name === 'Register')) {
      next('/dashboard')
    } else {
      next()
    }
    return
  }
  
  // 需要认证的页面
  if (!userStore.isAuthenticated) {
    // 未登录，重定向到登录页面
    next({
      name: 'Login',
      query: { redirect: to.fullPath }
    })
    return
  }
  
  // 检查角色权限
  if (to.meta?.roles && Array.isArray(to.meta.roles)) {
    const hasPermission = to.meta.roles.some((role: string) => userStore.user?.roles.includes(role))
    if (!hasPermission) {
      // 权限不足，重定向到仪表板
      next('/dashboard')
      return
    }
  }
  
  next()
})

// 全局后置钩子 - 加载状态处理
router.afterEach(() => {
  // 路由切换完成后的处理
  // 可以在这里添加页面加载完成的逻辑
})

export default router 