<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-background via-muted/50 to-accent/10 p-4">
    <Card class="w-full max-w-md shadow-2xl border-border/50 backdrop-blur-sm bg-card/80">
      <CardHeader class="space-y-4">
        <!-- Logo和标题 -->
        <div class="text-center">
          <div class="w-16 h-16 bg-gradient-to-br from-tech-blue to-tech-purple rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg animate-pulse">
            <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
            </svg>
          </div>
          <CardTitle class="text-2xl font-bold bg-gradient-to-r from-tech-blue to-tech-purple bg-clip-text text-transparent">
            登录 Lyra
          </CardTitle>
          <CardDescription class="text-muted-foreground mt-2">
            欢迎回到您的云端文档空间
          </CardDescription>
        </div>
      </CardHeader>

      <CardContent class="space-y-6">
        <!-- 错误提示 -->
        <Alert v-if="errorMessage" variant="destructive" class="animate-in slide-in-from-top-2">
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="15" y1="9" x2="9" y2="15"></line>
            <line x1="9" y1="9" x2="15" y2="15"></line>
          </svg>
          <AlertDescription>{{ errorMessage }}</AlertDescription>
        </Alert>

        <!-- 登录表单 -->
        <Form :validation-schema="formSchema" @submit="handleLogin" class="space-y-5">
          <FormField v-slot="{ componentField }" name="username">
            <FormItem>
              <FormLabel class="text-sm font-medium">用户名</FormLabel>
              <FormControl>
                <Input
                  type="text"
                  placeholder="请输入用户名"
                  :disabled="loading"
                  class="transition-all duration-200 focus:ring-2 focus:ring-tech-blue/20"
                  v-bind="componentField"
                />
              </FormControl>
              <FormMessage />
            </FormItem>
          </FormField>

          <FormField v-slot="{ componentField }" name="password">
            <FormItem>
              <FormLabel class="text-sm font-medium">密码</FormLabel>
              <FormControl>
                <div class="relative">
                  <Input
                    :type="showPassword ? 'text' : 'password'"
                    placeholder="请输入密码"
                    :disabled="loading"
                    class="pr-10 transition-all duration-200 focus:ring-2 focus:ring-tech-blue/20"
                    v-bind="componentField"
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    class="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                    @click="showPassword = !showPassword"
                  >
                    <svg v-if="!showPassword" class="h-4 w-4 text-muted-foreground" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                    </svg>
                    <svg v-else class="h-4 w-4 text-muted-foreground" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21"></path>
                    </svg>
                  </Button>
                </div>
              </FormControl>
              <FormMessage />
            </FormItem>
          </FormField>

          <FormField v-slot="{ value, handleChange }" name="rememberMe">
            <FormItem class="flex items-center justify-between">
              <div class="flex items-center space-x-2">
                <input
                  id="remember"
                  type="checkbox"
                  :checked="value"
                  @change="handleChange"
                  class="w-4 h-4 text-tech-blue bg-background border-border rounded focus:ring-tech-blue focus:ring-2"
                />
                <FormLabel for="remember" class="text-sm text-muted-foreground cursor-pointer">
                  记住我
                </FormLabel>
              </div>
              <Button variant="link" size="sm" class="text-sm text-tech-blue hover:text-tech-purple" @click="$router.push('/forgot-password')">
                忘记密码？
              </Button>
            </FormItem>
          </FormField>

          <Button
            type="submit"
            :disabled="loading"
            class="w-full bg-gradient-to-r from-tech-blue to-tech-purple hover:from-tech-blue/90 hover:to-tech-purple/90 text-white font-medium py-2.5 transition-all duration-300 transform hover:scale-[1.02] active:scale-[0.98]"
          >
            <svg v-if="loading" class="w-4 h-4 mr-2 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
            </svg>
            <svg v-else class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1"></path>
            </svg>
            {{ loading ? '登录中...' : '登录' }}
          </Button>
        </Form>
      </CardContent>

      <CardFooter class="flex-col space-y-4">
        <Separator />
        <div class="text-center text-sm text-muted-foreground">
          还没有账号？
          <RouterLink 
            to="/register" 
            class="text-tech-blue hover:text-tech-purple font-medium transition-colors duration-200"
          >
            立即注册
          </RouterLink>
        </div>
      </CardFooter>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'

import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Separator } from '@/components/ui/separator'
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'

const router = useRouter()
const userStore = useUserStore()

// 响应式状态
const loading = ref(false)
const showPassword = ref(false)
const errorMessage = ref('')

// 表单验证模式
const formSchema = toTypedSchema(z.object({
  username: z.string({ message: '请输入用户名' }).min(1, '请输入用户名').max(50, '用户名不能超过50个字符'),
  password: z.string({ message: '请输入密码' }).min(1, '请输入密码'),
  rememberMe: z.boolean().optional()
}))

// 登录处理
const handleLogin = async (values: any) => {
  loading.value = true
  errorMessage.value = ''

  try {
    await userStore.login({
      usernameOrEmail: values.username,
      password: values.password
    })

    // 登录成功，跳转到仪表板
    router.push('/dashboard')
  } catch (error: any) {
    console.error('Login error:', error)
    errorMessage.value = error.response?.data?.message || '登录失败，请检查用户名和密码'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 自定义动画 */
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.8; }
}

.animate-pulse {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

/* 卡片悬浮效果 */
.card-hover {
  transition: all 0.3s ease;
}

.card-hover:hover {
  transform: translateY(-2px);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}
</style> 