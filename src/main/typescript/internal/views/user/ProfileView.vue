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
            <h1 class="text-xl font-semibold text-foreground">个人信息</h1>
          </div>
          <div class="flex items-center space-x-2">
            <Button variant="outline" size="sm" @click="$router.push('/settings')">
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path>
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
              </svg>
              设置
            </Button>
          </div>
        </div>
      </div>
    </header>

    <!-- 主要内容 -->
    <main class="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <!-- 侧边栏 - 用户头像和基本信息 -->
        <div class="lg:col-span-1">
          <Card class="sticky top-8">
            <CardContent class="p-6">
              <div class="text-center space-y-4">
                <!-- 用户头像 -->
                <div class="relative">
                  <div class="w-24 h-24 bg-gradient-to-br from-primary to-primary/80 rounded-full flex items-center justify-center mx-auto overflow-hidden">
                    <img
                      v-if="userStore.user?.avatar"
                      :src="userStore.user.avatar"
                      :alt="userStore.user.displayName"
                      class="w-full h-full object-cover"
                    />
                    <span
                      v-else
                      class="text-2xl font-bold text-primary-foreground"
                    >
                      {{ userStore.user?.displayName?.charAt(0)?.toUpperCase() || 'U' }}
                    </span>
                  </div>
                  <Button
                    size="sm"
                    variant="outline"
                    class="absolute -bottom-2 left-1/2 transform -translate-x-1/2 h-8 w-8 rounded-full p-0"
                    @click="handleAvatarClick"
                    :disabled="avatarUploading"
                  >
                    <svg v-if="avatarUploading" class="w-4 h-4 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                    </svg>
                    <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"></path>
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z"></path>
                    </svg>
                  </Button>
                  <!-- 隐藏的文件输入 -->
                  <input
                    ref="avatarInput"
                    type="file"
                    accept="image/*"
                    class="hidden"
                    @change="handleAvatarChange"
                  />
                </div>

                <!-- 用户基本信息 -->
                <div class="space-y-2">
                  <h2 class="text-xl font-semibold text-foreground">{{ userStore.user?.displayName }}</h2>
                  <p class="text-sm text-muted-foreground">@{{ userStore.user?.username }}</p>
                  <p class="text-sm text-muted-foreground">{{ userStore.user?.email }}</p>
                </div>

                <!-- 用户角色标签 -->
                <div class="flex flex-wrap justify-center gap-2">
                  <Badge 
                    v-for="role in userStore.user?.roles" 
                    :key="role" 
                    :variant="role === 'admin' ? 'default' : 'secondary'"
                  >
                    {{ getRoleDisplayName(role) }}
                  </Badge>
                </div>

                <!-- 加入时间 -->
                <div class="text-xs text-muted-foreground">
                  加入时间：{{ formatDate(userStore.user?.createdAt) }}
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        <!-- 主要内容区域 -->
        <div class="lg:col-span-2 space-y-6">
          <!-- 错误提示 -->
          <Alert v-if="errorMessage" variant="destructive" class="animate-in slide-in-from-top-2">
            <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="15" y1="9" x2="9" y2="15"></line>
              <line x1="9" y1="9" x2="15" y2="15"></line>
            </svg>
            <AlertDescription>{{ errorMessage }}</AlertDescription>
          </Alert>

          <!-- 成功提示 -->
          <Alert v-if="successMessage" class="animate-in slide-in-from-top-2 border-success text-success">
            <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
            <AlertDescription>{{ successMessage }}</AlertDescription>
          </Alert>

          <!-- 编辑个人信息 -->
          <Card>
            <CardHeader>
              <CardTitle class="flex items-center">
                <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path>
                </svg>
                编辑个人信息
              </CardTitle>
              <CardDescription>
                更新您的个人资料信息
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Form :validation-schema="profileSchema" @submit="handleUpdateProfile">
                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <FormField v-slot="{ componentField }" name="displayName">
                    <FormItem>
                      <FormLabel>显示名称</FormLabel>
                      <FormControl>
                        <Input
                          placeholder="请输入显示名称"
                          :disabled="loading"
                          v-bind="componentField"
                        />
                      </FormControl>
                      <FormDescription>这是其他用户看到的名称</FormDescription>
                      <FormMessage />
                    </FormItem>
                  </FormField>

                  <FormField v-slot="{ componentField }" name="email">
                    <FormItem>
                      <FormLabel>邮箱地址</FormLabel>
                      <FormControl>
                        <Input
                          type="email"
                          placeholder="请输入邮箱地址"
                          :disabled="loading"
                          v-bind="componentField"
                        />
                      </FormControl>
                      <FormDescription>用于接收通知和密码重置</FormDescription>
                      <FormMessage />
                    </FormItem>
                  </FormField>
                </div>

                <div class="flex justify-end pt-6">
                  <Button 
                    type="submit" 
                    :disabled="loading"
                    class="bg-gradient-to-r from-tech-blue to-tech-purple hover:from-tech-blue/90 hover:to-tech-purple/90"
                  >
                    <svg v-if="loading" class="w-4 h-4 mr-2 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                    </svg>
                    {{ loading ? '保存中...' : '保存更改' }}
                  </Button>
                </div>
              </Form>
            </CardContent>
          </Card>

          <!-- 账户统计信息 -->
          <Card>
            <CardHeader>
              <CardTitle class="flex items-center">
                <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v4a2 2 0 01-2 2h-2a2 2 0 00-2-2z"></path>
                </svg>
                账户统计
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div class="text-center p-4 rounded-lg bg-muted/50 hover:bg-muted/70 transition-colors">
                  <div class="text-2xl font-bold text-primary">{{ accountStats.fileCount }}</div>
                  <div class="text-sm text-muted-foreground">文件数量</div>
                </div>
                <div class="text-center p-4 rounded-lg bg-muted/50 hover:bg-muted/70 transition-colors">
                  <div class="text-2xl font-bold text-primary">{{ formatFileSize(accountStats.usedSpace) }}</div>
                  <div class="text-sm text-muted-foreground">已用空间</div>
                </div>
                <div class="text-center p-4 rounded-lg bg-muted/50 hover:bg-muted/70 transition-colors">
                  <div class="text-2xl font-bold text-primary">{{ accountStats.sharedFiles }}</div>
                  <div class="text-sm text-muted-foreground">共享文件</div>
                </div>
                <div class="text-center p-4 rounded-lg bg-muted/50 hover:bg-muted/70 transition-colors">
                  <div class="text-2xl font-bold text-primary">{{ accountStats.recentActivity }}</div>
                  <div class="text-sm text-muted-foreground">最近活动</div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import dayjs from 'dayjs'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'

const userStore = useUserStore()

// 响应式状态
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

// 表单验证模式
const profileSchema = toTypedSchema(z.object({
  displayName: z.string()
    .min(2, '显示名称至少2个字符')
    .max(50, '显示名称不能超过50个字符'),
  email: z.string().email('请输入有效的邮箱地址')
}))

// 格式化日期
const formatDate = (dateString?: string) => {
  if (!dateString) return '未知'
  return dayjs(dateString).format('YYYY年MM月DD日')
}

// 获取角色显示名称
const getRoleDisplayName = (role: string) => {
  const roleMap: Record<string, string> = {
    'admin': '管理员',
    'user': '普通用户',
    'moderator': '版主'
  }
  return roleMap[role] || role
}

// 头像上传相关
const avatarInput = ref<HTMLInputElement>()
const avatarUploading = ref(false)

// 处理头像点击
const handleAvatarClick = () => {
  avatarInput.value?.click()
}

// 处理头像文件选择
const handleAvatarChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]

  if (!file) return

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    errorMessage.value = '请选择图片文件'
    return
  }

  // 验证文件大小 (5MB)
  if (file.size > 5 * 1024 * 1024) {
    errorMessage.value = '图片文件不能超过5MB'
    return
  }

  avatarUploading.value = true
  errorMessage.value = ''

  try {
    const formData = new FormData()
    formData.append('avatar', file)

    await userStore.uploadAvatar(formData)
    successMessage.value = '头像更新成功！'

    // 3秒后清除成功消息
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)

  } catch (error: any) {
    console.error('Avatar upload failed:', error)
    errorMessage.value = error.response?.data?.message || '头像上传失败，请稍后再试'
  } finally {
    avatarUploading.value = false
    // 清空文件输入
    if (target) target.value = ''
  }
}

// 处理更新个人信息
const handleUpdateProfile = async (values: any) => {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    await userStore.updateProfile({
      displayName: values.displayName,
      email: values.email
    })
    
    successMessage.value = '个人信息更新成功！'
    
    // 3秒后清除成功消息
    setTimeout(() => {
      successMessage.value = ''
    }, 3000)
    
  } catch (error: any) {
    console.error('Profile update failed:', error)
    errorMessage.value = error.response?.data?.message || '更新失败，请稍后再试'
  } finally {
    loading.value = false
  }
}

// 账户统计信息
const accountStats = ref({
  fileCount: 0,
  usedSpace: 0,
  sharedFiles: 0,
  recentActivity: 0
})

// 获取账户统计信息
const fetchAccountStats = async () => {
  try {
    // 这里可以调用API获取统计信息
    // const response = await userApi.getAccountStats()
    // accountStats.value = response.data

    // 暂时使用模拟数据
    accountStats.value = {
      fileCount: 42,
      usedSpace: 1024 * 1024 * 256, // 256MB
      sharedFiles: 8,
      recentActivity: 15
    }
  } catch (error) {
    console.error('Failed to fetch account stats:', error)
  }
}

// 格式化文件大小
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 组件挂载时设置表单初始值和获取统计信息
onMounted(async () => {
  // 获取账户统计信息
  await fetchAccountStats()

  // 设置表单初始值
  // 注意：这里需要等待用户信息加载完成
  if (userStore.user) {
    // 可以使用vee-validate的setValues方法设置初始值
    // 或者在表单组件中直接绑定userStore.user的值
  }
})
</script>

<style scoped>
/* 自定义样式 */
.animate-in {
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style> 