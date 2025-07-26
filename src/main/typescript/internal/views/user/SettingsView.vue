<template>
  <div class="min-h-screen bg-gradient-to-br from-background via-muted/30 to-accent/5">
    <!-- 顶部导航栏 -->
    <header class="bg-card/80 backdrop-blur-sm border-b border-border/50">
      <div class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
          <div class="flex items-center space-x-4">
            <Button variant="ghost" size="sm" @click="$router.back()" class="text-muted-foreground hover:text-foreground">
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
              </svg>
              返回
            </Button>
            <h1 class="text-xl font-semibold text-foreground">设置</h1>
          </div>
        </div>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div class="grid grid-cols-1 lg:grid-cols-4 gap-8">
        
        <!-- 侧边导航 -->
        <div class="lg:col-span-1">
          <Card class="sticky top-8">
            <CardContent class="p-4">
              <nav class="space-y-2">
                <button
                  v-for="tab in tabs"
                  :key="tab.id"
                  @click="activeTab = tab.id"
                  :class="[
                    'w-full flex items-center px-3 py-2 text-sm font-medium rounded-lg transition-colors',
                    activeTab === tab.id
                      ? 'bg-primary text-primary-foreground'
                      : 'text-muted-foreground hover:text-foreground hover:bg-muted'
                  ]"
                >
                  <component :is="tab.icon" class="w-4 h-4 mr-3" />
                  {{ tab.label }}
                </button>
              </nav>
            </CardContent>
          </Card>
        </div>

        <!-- 内容区域 -->
        <div class="lg:col-span-3">
          
          <!-- 账户安全 -->
          <div v-if="activeTab === 'security'" class="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle class="flex items-center">
                  <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
                  </svg>
                  修改密码
                </CardTitle>
                <CardDescription>
                  定期更新密码可以提高账户安全性
                </CardDescription>
              </CardHeader>
              <CardContent class="space-y-4">
                <form @submit.prevent="updatePassword" class="space-y-4">
                  <FormField v-slot="{ field }" name="currentPassword">
                    <FormItem>
                      <FormLabel>当前密码</FormLabel>
                      <FormControl>
                        <Input 
                          v-bind="field"
                          type="password" 
                          placeholder="输入当前密码"
                          class="transition-all"
                        />
                      </FormControl>
                    </FormItem>
                  </FormField>

                  <FormField v-slot="{ field }" name="newPassword">
                    <FormItem>
                      <FormLabel>新密码</FormLabel>
                      <FormControl>
                        <Input 
                          v-bind="field"
                          type="password" 
                          placeholder="输入新密码"
                          class="transition-all"
                        />
                      </FormControl>
                    </FormItem>
                  </FormField>

                  <FormField v-slot="{ field }" name="confirmPassword">
                    <FormItem>
                      <FormLabel>确认新密码</FormLabel>
                      <FormControl>
                        <Input 
                          v-bind="field"
                          type="password" 
                          placeholder="再次输入新密码"
                          class="transition-all"
                        />
                      </FormControl>
                    </FormItem>
                  </FormField>

                  <Button 
                    type="submit"
                    :disabled="passwordForm.isSubmitting"
                    class="bg-gradient-to-r from-tech-blue to-tech-purple hover:from-tech-blue/90 hover:to-tech-purple/90"
                  >
                    <svg v-if="passwordForm.isSubmitting" class="w-4 h-4 mr-2 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"></path>
                    </svg>
                    {{ passwordForm.isSubmitting ? '更新中...' : '更新密码' }}
                  </Button>
                </form>
              </CardContent>
            </Card>

            <!-- 两步验证 -->
            <Card>
              <CardHeader>
                <CardTitle class="flex items-center">
                  <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                  </svg>
                  两步验证
                </CardTitle>
                <CardDescription>
                  启用两步验证以增强账户安全性
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div class="flex items-center justify-between">
                  <div>
                    <p class="font-medium">{{ twoFactorEnabled ? '已启用' : '未启用' }}</p>
                    <p class="text-sm text-muted-foreground">
                      {{ twoFactorEnabled ? '账户已受到额外保护' : '建议启用以保护账户安全' }}
                    </p>
                  </div>
                  <Button 
                    variant="outline"
                    @click="toggleTwoFactor"
                    :disabled="twoFactorLoading"
                  >
                    {{ twoFactorEnabled ? '禁用' : '启用' }}
                  </Button>
                </div>
              </CardContent>
            </Card>
          </div>

          <!-- 个人偏好 -->
          <div v-if="activeTab === 'preferences'" class="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>界面设置</CardTitle>
                <CardDescription>
                  自定义您的使用体验
                </CardDescription>
              </CardHeader>
              <CardContent class="space-y-6">
                <!-- 主题设置 -->
                <div class="flex items-center justify-between">
                  <div>
                    <Label class="text-base font-medium">主题模式</Label>
                    <p class="text-sm text-muted-foreground">选择您喜欢的界面主题</p>
                  </div>
                  <select 
                    v-model="preferences.theme"
                    class="border border-input bg-background px-3 py-2 rounded-md"
                  >
                    <option value="light">浅色</option>
                    <option value="dark">深色</option>
                    <option value="system">跟随系统</option>
                  </select>
                </div>

                <!-- 语言设置 -->
                <div class="flex items-center justify-between">
                  <div>
                    <Label class="text-base font-medium">语言</Label>
                    <p class="text-sm text-muted-foreground">选择界面显示语言</p>
                  </div>
                  <select 
                    v-model="preferences.language"
                    class="border border-input bg-background px-3 py-2 rounded-md"
                  >
                    <option value="zh-CN">简体中文</option>
                    <option value="en-US">English</option>
                  </select>
                </div>

                <!-- 文件预览 -->
                <div class="flex items-center justify-between">
                  <div>
                    <Label class="text-base font-medium">自动预览</Label>
                    <p class="text-sm text-muted-foreground">点击文件时自动显示预览</p>
                  </div>
                  <input 
                    type="checkbox" 
                    v-model="preferences.autoPreview"
                    class="h-4 w-4 rounded border-gray-300"
                  >
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>通知设置</CardTitle>
                <CardDescription>
                  管理您接收的通知类型
                </CardDescription>
              </CardHeader>
              <CardContent class="space-y-4">
                <div class="flex items-center justify-between">
                  <div>
                    <Label class="text-base font-medium">邮件通知</Label>
                    <p class="text-sm text-muted-foreground">接收重要更新和消息</p>
                  </div>
                  <input 
                    type="checkbox" 
                    v-model="preferences.emailNotifications"
                    class="h-4 w-4 rounded border-gray-300"
                  >
                </div>

                <div class="flex items-center justify-between">
                  <div>
                    <Label class="text-base font-medium">桌面通知</Label>
                    <p class="text-sm text-muted-foreground">显示浏览器推送通知</p>
                  </div>
                  <input 
                    type="checkbox" 
                    v-model="preferences.desktopNotifications"
                    class="h-4 w-4 rounded border-gray-300"
                  >
                </div>
              </CardContent>
            </Card>
          </div>

          <!-- 存储设置 -->
          <div v-if="activeTab === 'storage'" class="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>存储使用情况</CardTitle>
                <CardDescription>
                  查看您的存储空间使用情况
                </CardDescription>
              </CardHeader>
              <CardContent>
                <div class="space-y-4">
                  <div class="flex justify-between items-center">
                    <span class="text-sm font-medium">已使用存储</span>
                    <span class="text-sm text-muted-foreground">{{ formatBytes(storageInfo.used) }} / {{ formatBytes(storageInfo.total) }}</span>
                  </div>
                  <div class="w-full bg-muted rounded-full h-2">
                    <div 
                      class="bg-gradient-to-r from-tech-blue to-tech-purple h-2 rounded-full transition-all duration-300"
                      :style="{ width: `${(storageInfo.used / storageInfo.total) * 100}%` }"
                    ></div>
                  </div>
                  <div class="text-xs text-muted-foreground">
                    剩余可用空间：{{ formatBytes(storageInfo.total - storageInfo.used) }}
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>清理工具</CardTitle>
                <CardDescription>
                  清理不需要的文件以释放空间
                </CardDescription>
              </CardHeader>
              <CardContent class="space-y-4">
                <div class="flex justify-between items-center">
                  <div>
                    <p class="font-medium">临时文件</p>
                    <p class="text-sm text-muted-foreground">清理缓存和临时文件</p>
                  </div>
                  <Button variant="outline" size="sm">
                    清理
                  </Button>
                </div>
                <div class="flex justify-between items-center">
                  <div>
                    <p class="font-medium">回收站</p>
                    <p class="text-sm text-muted-foreground">永久删除回收站中的文件</p>
                  </div>
                  <Button variant="outline" size="sm">
                    清空
                  </Button>
                </div>
              </CardContent>
            </Card>
          </div>

          <!-- 关于 -->
          <div v-if="activeTab === 'about'" class="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>关于 Lyra</CardTitle>
                <CardDescription>
                  企业级云原生文档管理系统
                </CardDescription>
              </CardHeader>
              <CardContent class="space-y-4">
                <div class="flex items-center space-x-4">
                  <div class="w-16 h-16 bg-gradient-to-br from-tech-blue to-tech-purple rounded-2xl flex items-center justify-center">
                    <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
                    </svg>
                  </div>
                  <div>
                    <h3 class="text-lg font-semibold">Lyra v1.0.0</h3>
                    <p class="text-muted-foreground">构建版本：{{ buildInfo.version }}</p>
                    <p class="text-muted-foreground">构建时间：{{ buildInfo.buildTime }}</p>
                  </div>
                </div>

                <div class="pt-4 border-t">
                  <h4 class="font-medium mb-2">技术栈</h4>
                  <div class="grid grid-cols-2 gap-2 text-sm text-muted-foreground">
                    <div>前端：Vue 3.5 + TypeScript</div>
                    <div>后端：Spring Boot 3.5</div>
                    <div>UI：TailwindCSS + shadcn/vue</div>
                    <div>数据库：SQLite/MySQL/PostgreSQL</div>
                  </div>
                </div>

                <div class="pt-4 border-t">
                  <h4 class="font-medium mb-2">支持与帮助</h4>
                  <div class="space-y-2">
                    <Button variant="outline" size="sm" class="w-full justify-start">
                      <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"></path>
                      </svg>
                      用户手册
                    </Button>
                    <Button variant="outline" size="sm" class="w-full justify-start">
                      <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                      </svg>
                      常见问题
                    </Button>
                    <Button variant="outline" size="sm" class="w-full justify-start">
                      <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path>
                      </svg>
                      联系支持
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h } from 'vue'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { FormField, FormItem, FormLabel, FormControl } from '@/components/ui/form'

// 页面元数据
defineOptions({
  name: 'SettingsView'
})

// 标签页配置
const tabs = [
  {
    id: 'security',
    label: '账户安全',
    icon: () => h('svg', {
      class: 'w-4 h-4',
      fill: 'none',
      stroke: 'currentColor',
      viewBox: '0 0 24 24'
    }, [
      h('path', {
        'stroke-linecap': 'round',
        'stroke-linejoin': 'round',
        'stroke-width': '2',
        d: 'M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z'
      })
    ])
  },
  {
    id: 'preferences',
    label: '个人偏好',
    icon: () => h('svg', {
      class: 'w-4 h-4',
      fill: 'none',
      stroke: 'currentColor',
      viewBox: '0 0 24 24'
    }, [
      h('path', {
        'stroke-linecap': 'round',
        'stroke-linejoin': 'round',
        'stroke-width': '2',
        d: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z'
      }),
      h('path', {
        'stroke-linecap': 'round',
        'stroke-linejoin': 'round',
        'stroke-width': '2',
        d: 'M15 12a3 3 0 11-6 0 3 3 0 016 0z'
      })
    ])
  },
  {
    id: 'storage',
    label: '存储管理',
    icon: () => h('svg', {
      class: 'w-4 h-4',
      fill: 'none',
      stroke: 'currentColor',
      viewBox: '0 0 24 24'
    }, [
      h('path', {
        'stroke-linecap': 'round',
        'stroke-linejoin': 'round',
        'stroke-width': '2',
        d: 'M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4'
      })
    ])
  },
  {
    id: 'about',
    label: '关于',
    icon: () => h('svg', {
      class: 'w-4 h-4',
      fill: 'none',
      stroke: 'currentColor',
      viewBox: '0 0 24 24'
    }, [
      h('path', {
        'stroke-linecap': 'round',
        'stroke-linejoin': 'round',
        'stroke-width': '2',
        d: 'M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z'
      })
    ])
  }
]

// 响应式数据
const activeTab = ref('security')
const twoFactorEnabled = ref(false)
const twoFactorLoading = ref(false)

// 表单状态
const passwordForm = reactive({
  isSubmitting: false
})

// 用户偏好设置
const preferences = reactive({
  theme: 'system',
  language: 'zh-CN',
  autoPreview: true,
  emailNotifications: true,
  desktopNotifications: false
})

// 存储信息
const storageInfo = reactive({
  used: 2.5 * 1024 * 1024 * 1024, // 2.5GB in bytes
  total: 10 * 1024 * 1024 * 1024   // 10GB in bytes
})

// 构建信息
const buildInfo = reactive({
  version: '1.0.0-beta.1',
  buildTime: new Date().toLocaleDateString()
})

// 方法
const updatePassword = async () => {
  passwordForm.isSubmitting = true
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000))
    console.log('Password updated successfully')
  } catch (error) {
    console.error('Failed to update password:', error)
  } finally {
    passwordForm.isSubmitting = false
  }
}

const toggleTwoFactor = async () => {
  twoFactorLoading.value = true
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000))
    twoFactorEnabled.value = !twoFactorEnabled.value
  } catch (error) {
    console.error('Failed to toggle two-factor authentication:', error)
  } finally {
    twoFactorLoading.value = false
  }
}

const formatBytes = (bytes: number): string => {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}
</script>

<style scoped>
/* 自定义样式可以在这里添加 */
</style> 