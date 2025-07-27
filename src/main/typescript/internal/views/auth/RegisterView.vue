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
            注册 Lyra
          </CardTitle>
          <CardDescription class="text-muted-foreground mt-2">
            加入企业级云原生文档管理系统
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

        <!-- 成功提示 -->
        <Alert v-if="successMessage" class="animate-in slide-in-from-top-2 border-success text-success">
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
          <AlertDescription>{{ successMessage }}</AlertDescription>
        </Alert>

        <!-- 注册表单 -->
        <Form :validation-schema="formSchema" @submit="handleRegister" class="space-y-6">
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
              <FormDescription class="text-xs text-muted-foreground mt-1">
                用户名将作为您的登录凭据
              </FormDescription>
              <FormMessage />
            </FormItem>
          </FormField>

          <FormField v-slot="{ componentField }" name="displayName">
            <FormItem>
              <FormLabel class="text-sm font-medium">显示名称</FormLabel>
              <FormControl>
                <Input
                  type="text"
                  placeholder="请输入显示名称"
                  :disabled="loading"
                  class="transition-all duration-200 focus:ring-2 focus:ring-tech-blue/20"
                  v-bind="componentField"
                />
              </FormControl>
              <FormDescription class="text-xs text-muted-foreground mt-1">
                这将是其他用户看到的名称
              </FormDescription>
              <FormMessage />
            </FormItem>
          </FormField>

          <FormField v-slot="{ componentField }" name="email">
            <FormItem>
              <FormLabel class="text-sm font-medium">邮箱地址</FormLabel>
              <FormControl>
                <Input
                  type="email"
                  placeholder="请输入邮箱地址"
                  :disabled="loading"
                  class="transition-all duration-200 focus:ring-2 focus:ring-tech-blue/20"
                  v-bind="componentField"
                />
              </FormControl>
              <FormDescription class="text-xs text-muted-foreground mt-1">
                用于接收通知和密码重置
              </FormDescription>
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
                    placeholder="请输入密码（至少6位）"
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

          <FormField v-slot="{ componentField }" name="confirmPassword">
            <FormItem>
              <FormLabel class="text-sm font-medium">确认密码</FormLabel>
              <FormControl>
                <div class="relative">
                  <Input
                    :type="showConfirmPassword ? 'text' : 'password'"
                    placeholder="请再次输入密码"
                    :disabled="loading"
                    class="pr-10 transition-all duration-200 focus:ring-2 focus:ring-tech-blue/20"
                    v-bind="componentField"
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    class="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                    @click="showConfirmPassword = !showConfirmPassword"
                  >
                    <svg v-if="!showConfirmPassword" class="h-4 w-4 text-muted-foreground" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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

          <FormField v-slot="{ value, handleChange }" name="agreeToTerms">
            <FormItem class="flex items-start space-x-3">
              <input
                id="agree"
                type="checkbox"
                :checked="value"
                @change="(e) => handleChange((e.target as HTMLInputElement)?.checked)"
                class="w-4 h-4 text-tech-blue bg-background border-border rounded focus:ring-tech-blue focus:ring-2 mt-0.5"
              />
              <div class="grid gap-1.5 leading-none">
                <FormLabel for="agree" class="text-sm text-muted-foreground cursor-pointer">
                  我已阅读并同意
                  <a href="#" class="text-tech-blue hover:text-tech-purple underline">服务条款</a>
                  和
                  <a href="#" class="text-tech-blue hover:text-tech-purple underline">隐私政策</a>
                </FormLabel>
                <FormMessage />
              </div>
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
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path>
            </svg>
            {{ loading ? '注册中...' : '注册账号' }}
          </Button>
        </Form>
      </CardContent>

      <CardFooter class="flex-col space-y-4">
        <Separator />
        <div class="text-center space-y-2">
          <RouterLink
            to="/login"
            class="text-sm text-muted-foreground hover:text-primary transition-colors"
          >
            已有账号？立即登录
          </RouterLink>
          
          <div class="text-xs text-muted-foreground">
            注册后需要管理员审核激活
          </div>
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
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Separator } from '@/components/ui/separator'
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'

const router = useRouter()
const userStore = useUserStore()

// 响应式状态
const loading = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

// 表单验证模式
const formSchema = toTypedSchema(z.object({
  username: z.string({ message: '请输入用户名' })
    .min(3, '用户名至少3个字符')
    .max(20, '用户名不能超过20个字符')
    .regex(/^[a-zA-Z0-9_-]+$/, '用户名只能包含字母、数字、下划线和连字符'),
  displayName: z.string({ message: '请输入显示名称' })
    .min(2, '显示名称至少2个字符')
    .max(50, '显示名称不能超过50个字符'),
  email: z.string({ message: '请输入邮箱地址' })
    .email('请输入有效的邮箱地址'),
  password: z.string({ message: '请输入密码' })
    .min(6, '密码至少6位')
    .regex(/^(?=.*[a-zA-Z])(?=.*\d).*$/, '密码必须包含字母和数字'),
  confirmPassword: z.string({ message: '请确认密码' }),
  agreeToTerms: z.boolean({ message: '请同意服务条款' }).refine(val => val === true, '请同意服务条款和隐私政策')
}).refine(data => data.password === data.confirmPassword, {
  message: '两次输入的密码不一致',
  path: ['confirmPassword']
}))

// 处理注册
const handleRegister = async (values: any) => {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    await userStore.register({
      username: values.username,
      email: values.email,
      password: values.password,
      confirmPassword: values.confirmPassword,
      displayName: values.displayName
    })

    successMessage.value = '注册成功！请等待管理员审核激活您的账号。'
    
    // 3秒后跳转到登录页面
    setTimeout(() => {
      router.push('/login')
    }, 3000)

  } catch (error: any) {
    console.error('注册失败:', error)

    // 处理不同类型的错误
    if (error.response?.status === 409) {
      const message = error.response?.data?.message || ''
      if (message.includes('username')) {
        errorMessage.value = '用户名已存在，请选择其他用户名'
      } else if (message.includes('email')) {
        errorMessage.value = '邮箱已被注册，请使用其他邮箱或直接登录'
      } else {
        errorMessage.value = '用户名或邮箱已存在'
      }
    } else if (error.response?.status === 400) {
      errorMessage.value = error.response?.data?.message || '输入信息有误，请检查后重试'
    } else if (error.response?.status === 429) {
      errorMessage.value = '注册请求过于频繁，请稍后再试'
    } else {
      errorMessage.value = error.response?.data?.message || '注册失败，请检查网络连接或稍后再试'
    }
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
</style> 