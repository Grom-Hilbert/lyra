import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user.ts'

// 路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomeView.vue'),
    meta: {
      requiresAuth: false,
      title: 'Lyra 文档管理系统'
    }
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
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('@/views/auth/ForgotPasswordView.vue'),
    meta: {
      requiresAuth: false,
      title: '忘记密码'
    }
  },
  {
    path: '/reset-password',
    name: 'ResetPassword',
    component: () => import('@/views/auth/ResetPasswordView.vue'),
    meta: {
      requiresAuth: false,
      title: '重置密码'
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
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/user/ProfileView.vue'),
    meta: {
      requiresAuth: true,
      title: '个人信息'
    }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/user/SettingsView.vue'),
    meta: {
      requiresAuth: true,
      title: '用户设置'
    }
  },
  {
    path: '/files',
    name: 'Files',
    component: () => import('@/views/FileManagerView.vue'),
    meta: {
      requiresAuth: true,
      title: '文件管理'
    }
  },
  {
    path: '/files/:spaceId',
    name: 'FilesSpace',
    component: () => import('@/views/FileManagerView.vue'),
    meta: {
      requiresAuth: true,
      title: '文件管理'
    }
  },
  {
    path: '/files/:spaceId/:folderId',
    name: 'FilesFolder',
    component: () => import('@/views/FileManagerView.vue'),
    meta: {
      requiresAuth: true,
      title: '文件管理'
    }
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/SearchView.vue'),
    meta: {
      requiresAuth: true,
      title: '搜索'
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
  },
  // 管理后台路由
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('@/views/admin/AdminDashboardView.vue'),
    meta: {
      requiresAuth: true,
      roles: ['ADMIN'],
      title: '管理仪表板'
    }
  },
  {
    path: '/admin/users',
    name: 'UserManagement',
    component: () => import('@/views/admin/UserManagementView.vue'),
    meta: {
      requiresAuth: true,
      roles: ['ADMIN'],
      title: '用户管理'
    }
  },
  {
    path: '/admin/config',
    name: 'SystemConfig',
    component: () => import('@/views/admin/SystemConfigView.vue'),
    meta: {
      requiresAuth: true,
      roles: ['ADMIN'],
      title: '系统配置'
    }
  },
  {
    path: '/admin/version-control',
    name: 'VersionControl',
    component: () => import('@/views/admin/VersionControlView.vue'),
    meta: {
      requiresAuth: true,
      roles: ['ADMIN'],
      title: '版本控制'
    }
  },
  // 404 页面
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: {
      requiresAuth: false,
      title: '页面未找到'
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
router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  
  // 设置页面标题
  if (to.meta?.title) {
    document.title = `${to.meta.title} - Lyra 文档管理系统`
  }
  
  // 初始化用户认证状态（仅在首次加载时）
  if (!userStore.isAuthenticated && userStore.token) {
    try {
      await userStore.initAuth()
    } catch (error) {
      console.error('Failed to initialize auth:', error)
      userStore.clearAuth()
    }
  }
  
  // 检查是否需要认证
  if (to.meta?.requiresAuth === false) {
    // 不需要认证的页面，如果已登录则重定向到仪表板（登录、注册页面除外，但保留首页访问权限）
    const authPageNames = ['Login', 'Register', 'ForgotPassword', 'ResetPassword']
    if (userStore.isAuthenticated && to.name && authPageNames.includes(String(to.name))) {
      next({ name: 'Dashboard' })
    } else {
      next()
    }
    return
  }
  
  // 需要认证的页面
  if (!userStore.isAuthenticated) {
    // 对于核心功能页面，先重定向到首页让用户了解系统
    if (['Dashboard', 'Profile', 'Settings'].includes(to.name as string)) {
      next({ name: 'Home' })
    } else {
      // 其他需要认证的页面直接跳转到登录页
      next({
        name: 'Login',
        query: { redirect: to.fullPath }
      })
    }
    return
  }
  
  // 检查角色权限
  if (to.meta?.roles && Array.isArray(to.meta.roles)) {
    const hasPermission = to.meta.roles.some((role: string) => userStore.user?.roles.includes(role))
    if (!hasPermission) {
      // 权限不足，重定向到仪表板
      next({ name: 'Dashboard' })
      return
    }
  }
  
  next()
})

// 全局后置钩子 - 加载状态处理
router.afterEach((to, from) => {
  // 路由切换完成后的处理
  // 可以在这里添加页面加载完成的逻辑
  console.log(`Route changed from ${String(from.name)} to ${String(to.name)}`)
})

// 路由错误处理
router.onError((error) => {
  console.error('Router error:', error)
})

export default router 