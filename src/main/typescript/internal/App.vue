<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { 
  DropdownMenu, 
  DropdownMenuContent, 
  DropdownMenuItem, 
  DropdownMenuSeparator, 
  DropdownMenuTrigger 
} from '@/components/ui/dropdown-menu'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { 
  User, 
  Settings, 
  LogOut, 
  Monitor, 
  FolderOpen, 
  Search, 
  Bell, 
  Menu,
  Zap,
  Sparkles
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 判断是否为认证页面
const isAuthPage = computed(() => {
  const authRoutes = ['/login', '/register', '/about']
  return authRoutes.includes(route.path)
})

// 处理用户下拉菜单
const handleUserMenuAction = async (action: string) => {
  switch (action) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      try {
        await userStore.logout()
        router.push('/login')
      } catch (error) {
        console.error('退出登录失败:', error)
      }
      break
  }
}

// 侧边栏菜单项
const menuItems = [
  { path: '/dashboard', icon: Monitor, label: '仪表板', badge: null },
  { path: '/files', icon: FolderOpen, label: '文件管理', badge: 'New' },
  { path: '/search', icon: Search, label: '搜索', badge: null },
  { path: '/about', icon: Sparkles, label: '关于', badge: null }
]

onMounted(() => {
  // 初始化主题
  document.documentElement.classList.add('dark')
})

// 获取页面标题
const pageTitle = computed(() => {
  return (route.meta?.title as string) || '仪表板'
})

// 获取用户邮箱
const userEmail = computed(() => {
  return userStore.user?.email || ''
})
</script>

<template>
  <div id="app" class="min-h-screen bg-background text-foreground">
    <!-- 认证页面布局 -->
    <template v-if="isAuthPage">
      <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-background via-muted/50 to-accent/10">
        <router-view />
      </div>
    </template>
    
    <!-- 主应用布局 -->
    <template v-else>
      <div class="flex h-screen bg-background">
        <!-- 侧边栏 -->
        <aside v-if="userStore.isAuthenticated" class="w-64 bg-card border-r border-border flex flex-col">
          <!-- Logo区域 -->
          <div class="p-6 border-b border-border">
            <div class="flex items-center space-x-3">
              <div class="relative">
                <div class="w-10 h-10 bg-gradient-to-br from-tech-blue to-tech-purple rounded-xl flex items-center justify-center shadow-lg">
                  <Zap class="w-6 h-6 text-white" />
                </div>
                <div class="absolute -top-1 -right-1 w-3 h-3 bg-neon-green rounded-full animate-pulse"></div>
              </div>
              <div>
                <h1 class="text-xl font-bold bg-gradient-to-r from-tech-blue to-tech-purple bg-clip-text text-transparent">
                  Lyra
                </h1>
                <p class="text-xs text-muted-foreground">文档管理系统</p>
              </div>
            </div>
          </div>
          
          <!-- 导航菜单 -->
          <nav class="flex-1 p-4 space-y-2">
            <template v-for="item in menuItems" :key="item.path">
              <router-link
                :to="item.path"
                :class="[
                  'flex items-center space-x-3 w-full px-4 py-3 rounded-lg transition-all duration-200',
                  'hover:bg-accent hover:shadow-sm group',
                  route.path === item.path 
                    ? 'bg-primary text-primary-foreground shadow-md' 
                    : 'text-foreground hover:text-accent-foreground'
                ]"
              >
                <component 
                  :is="item.icon" 
                  :class="[
                    'w-5 h-5 transition-colors',
                    route.path === item.path ? 'text-primary-foreground' : 'text-muted-foreground group-hover:text-accent-foreground'
                  ]" 
                />
                <span class="font-medium">{{ item.label }}</span>
                <Badge v-if="item.badge" variant="secondary" class="ml-auto text-xs">
                  {{ item.badge }}
                </Badge>
              </router-link>
            </template>
          </nav>
          
          <!-- 用户信息区域 -->
          <div class="p-4 border-t border-border">
            <DropdownMenu>
              <DropdownMenuTrigger as-child>
                <Button 
                  variant="ghost" 
                  class="w-full justify-start space-x-3 h-auto p-3 hover:bg-accent"
                >
                  <Avatar class="w-8 h-8">
                    <AvatarImage :src="userStore.user?.avatar || ''" />
                    <AvatarFallback class="bg-gradient-to-br from-tech-blue to-tech-purple text-white font-semibold">
                      {{ userStore.displayName.charAt(0) }}
                    </AvatarFallback>
                  </Avatar>
                  <div class="flex-1 text-left">
                    <p class="text-sm font-medium">{{ userStore.displayName }}</p>
                    <p class="text-xs text-muted-foreground">{{ userEmail }}</p>
                  </div>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" class="w-56">
                <DropdownMenuItem @click="handleUserMenuAction('profile')" class="cursor-pointer">
                  <User class="w-4 h-4 mr-2" />
                  个人资料
                </DropdownMenuItem>
                <DropdownMenuItem @click="handleUserMenuAction('settings')" class="cursor-pointer">
                  <Settings class="w-4 h-4 mr-2" />
                  用户设置
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem @click="handleUserMenuAction('logout')" class="cursor-pointer text-destructive">
                  <LogOut class="w-4 h-4 mr-2" />
                  退出登录
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </aside>

        <!-- 主内容区域 -->
        <main class="flex-1 flex flex-col overflow-hidden">
          <!-- 顶部导航栏 -->
          <header v-if="userStore.isAuthenticated" class="bg-card border-b border-border px-6 py-4">
            <div class="flex items-center justify-between">
              <div class="flex items-center space-x-4">
                <!-- 移动端菜单按钮 -->
                <Button variant="ghost" size="sm" class="md:hidden">
                  <Menu class="w-5 h-5" />
                </Button>
                
                <!-- 页面标题 -->
                <h2 class="text-xl font-semibold">
                  {{ pageTitle }}
                </h2>
              </div>
              
              <!-- 右侧操作 -->
              <div class="flex items-center space-x-3">
                <!-- 通知按钮 -->
                <Button variant="ghost" size="sm" class="relative">
                  <Bell class="w-5 h-5" />
                  <span class="absolute -top-1 -right-1 w-2 h-2 bg-destructive rounded-full"></span>
                </Button>
                
                <!-- 状态指示器 -->
                <div class="flex items-center space-x-2">
                  <div class="w-2 h-2 bg-success rounded-full animate-pulse"></div>
                  <span class="text-sm text-muted-foreground">在线</span>
                </div>
              </div>
            </div>
          </header>
          
          <!-- 页面内容 -->
          <div class="flex-1 overflow-auto bg-muted/30">
            <div class="h-full p-6">
              <router-view />
            </div>
          </div>
        </main>
      </div>
      
      <!-- 未登录状态 -->
      <div v-if="!userStore.isAuthenticated" class="min-h-screen flex items-center justify-center bg-gradient-to-br from-background via-muted/50 to-accent/10">
        <div class="text-center space-y-6">
          <div class="w-20 h-20 bg-gradient-to-br from-tech-blue to-tech-purple rounded-2xl flex items-center justify-center mx-auto shadow-2xl">
            <Zap class="w-10 h-10 text-white" />
          </div>
          <div>
            <h1 class="text-3xl font-bold bg-gradient-to-r from-tech-blue to-tech-purple bg-clip-text text-transparent mb-2">
              欢迎使用 Lyra
            </h1>
            <p class="text-muted-foreground">企业级云原生文档管理系统</p>
          </div>
          <div class="flex space-x-4 justify-center">
            <Button @click="$router.push('/login')" class="bg-gradient-to-r from-tech-blue to-tech-purple hover:opacity-90 transition-opacity">
              登录
            </Button>
            <Button @click="$router.push('/register')" variant="outline">
              注册
            </Button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
/* 自定义样式补充 */
/* 移除了有问题的@apply样式，改为在模板中直接使用类名 */

/* 渐变动画 */
@keyframes gradient-shift {
  0%, 100% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
}

.animate-gradient {
  background-size: 200% 200%;
  animation: gradient-shift 3s ease infinite;
}

/* 响应式设计 */
@media (max-width: 768px) {
  aside {
    width: 100%;
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    z-index: 50;
    transform: translateX(-100%);
    transition: transform 0.3s ease;
  }
  
  aside.open {
    transform: translateX(0);
  }
}
</style>
