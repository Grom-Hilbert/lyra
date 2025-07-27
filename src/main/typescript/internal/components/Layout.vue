<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarInset,
  SidebarProvider,
  SidebarTrigger,
} from '@/components/ui/sidebar'
import Navigation from './Navigation.vue'
import Breadcrumb from './Breadcrumb.vue'
import Notification from './Notification.vue'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { 
  DropdownMenu, 
  DropdownMenuContent, 
  DropdownMenuItem, 
  DropdownMenuSeparator, 
  DropdownMenuTrigger 
} from '@/components/ui/dropdown-menu'
import { 
  User, 
  Settings, 
  LogOut, 
  Bell, 
  Menu,
  Zap
} from 'lucide-vue-next'

const route = useRoute()
const userStore = useUserStore()

// 侧边栏状态
const sidebarOpen = ref(true)

// 判断是否为认证页面
const isAuthPage = computed(() => {
  const authRoutes = ['/', '/login', '/register', '/forgot-password', '/reset-password', '/about']
  return authRoutes.includes(route.path)
})

// 获取页面标题
const pageTitle = computed(() => {
  return (route.meta?.title as string) || '仪表板'
})

// 获取用户邮箱
const userEmail = computed(() => {
  return userStore.user?.email || ''
})

// 处理用户下拉菜单
const handleUserMenuAction = async (action: string) => {
  switch (action) {
    case 'profile':
      // 路由跳转逻辑
      break
    case 'settings':
      // 路由跳转逻辑
      break
    case 'logout':
      try {
        await userStore.logout()
        // 路由跳转逻辑
      } catch (error) {
        console.error('退出登录失败:', error)
      }
      break
  }
}
</script>

<template>
  <div class="min-h-screen bg-background text-foreground">
    <!-- 认证页面布局 -->
    <template v-if="isAuthPage">
      <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-background via-muted/50 to-accent/10">
        <slot />
      </div>
    </template>
    
    <!-- 主应用布局 -->
    <template v-else-if="userStore.isAuthenticated">
      <SidebarProvider :default-open="sidebarOpen">
        <Sidebar variant="inset">
          <!-- 侧边栏头部 -->
          <SidebarHeader>
            <div class="flex items-center space-x-3 p-4">
              <div class="relative">
                <div class="w-10 h-10 bg-gradient-to-br from-primary to-primary/80 rounded-xl flex items-center justify-center shadow-lg">
                  <Zap class="w-6 h-6 text-primary-foreground" />
                </div>
                <div class="absolute -top-1 -right-1 w-3 h-3 bg-green-500 rounded-full animate-pulse"></div>
              </div>
              <div>
                <h1 class="text-xl font-bold bg-gradient-to-r from-primary to-primary/80 bg-clip-text text-transparent">
                  Lyra
                </h1>
                <p class="text-xs text-muted-foreground">文档管理系统</p>
              </div>
            </div>
          </SidebarHeader>
          
          <!-- 侧边栏内容 -->
          <SidebarContent>
            <Navigation />
          </SidebarContent>
          
          <!-- 侧边栏底部 -->
          <SidebarFooter>
            <div class="p-4">
              <DropdownMenu>
                <DropdownMenuTrigger as-child>
                  <Button 
                    variant="ghost" 
                    class="w-full justify-start space-x-3 h-auto p-3 hover:bg-accent"
                  >
                    <Avatar class="w-8 h-8">
                      <AvatarImage :src="userStore.user?.avatar || ''" />
                      <AvatarFallback class="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground font-semibold">
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
          </SidebarFooter>
        </Sidebar>
        
        <SidebarInset>
          <!-- 顶部导航栏 -->
          <header class="flex h-16 shrink-0 items-center gap-2 border-b px-4">
            <SidebarTrigger class="-ml-1" />
            <div class="flex items-center justify-between w-full">
              <div class="flex items-center space-x-4">
                <!-- 面包屑导航 -->
                <Breadcrumb />
              </div>
              
              <!-- 右侧操作 -->
              <div class="flex items-center space-x-3">
                <!-- 通知组件 -->
                <Notification />
                
                <!-- 状态指示器 -->
                <div class="flex items-center space-x-2">
                  <div class="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                  <span class="text-sm text-muted-foreground">在线</span>
                </div>
              </div>
            </div>
          </header>
          
          <!-- 主内容区域 -->
          <div class="flex flex-1 flex-col gap-4 p-4">
            <slot />
          </div>
        </SidebarInset>
      </SidebarProvider>
    </template>
    
    <!-- 未登录状态 -->
    <template v-else>
      <div class="min-h-screen flex items-center justify-center">
        <div class="text-center">
          <p class="text-muted-foreground">正在重定向到首页...</p>
        </div>
      </div>
    </template>
  </div>
</template>
