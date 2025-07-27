<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-background via-muted/50 to-accent/10 p-4">
    <Card class="w-full max-w-md shadow-2xl border-border/50 backdrop-blur-sm bg-card/80">
      <CardHeader class="space-y-4">
        <!-- Logo和标题 -->
        <div class="text-center">
          <div class="w-16 h-16 bg-gradient-to-br from-tech-blue to-tech-purple rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg animate-pulse">
            <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z"></path>
            </svg>
          </div>
          <CardTitle class="text-2xl font-bold bg-gradient-to-r from-tech-blue to-tech-purple bg-clip-text text-transparent">
            重置密码
          </CardTitle>
          <CardDescription class="text-muted-foreground mt-2">
            输入您的邮箱地址，我们将发送重置密码链接
          </CardDescription>
        </div>
      </CardHeader>

      <CardContent class="space-y-6">
        <!-- 成功提示 -->
        <Alert v-if="successMessage" class="animate-in slide-in-from-top-2 border-success text-success">
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
          <AlertDescription>{{ successMessage }}</AlertDescription>
        </Alert>

        <!-- 错误提示 -->
        <Alert v-if="errorMessage" variant="destructive" class="animate-in slide-in-from-top-2">
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="15" y1="9" x2="9" y2="15"></line>
            <line x1="9" y1="9" x2="15" y2="15"></line>
          </svg>
          <AlertDescription>{{ errorMessage }}</AlertDescription>
        </Alert>

        <!-- 密码重置表单 -->
        <Form v-if="!isEmailSent" :validation-schema="formSchema" @submit="handlePasswordReset" class="space-y-5">
          <FormField v-slot="{ componentField }" name="email">
            <FormItem>
              <FormLabel class="text-sm font-medium">邮箱地址</FormLabel>
              <FormControl>
                <Input
                  type="email"
                  placeholder="请输入您的邮箱地址"
                  :disabled="loading"
                  class="transition-all duration-200 focus:ring-2 focus:ring-tech-blue/20"
                  v-bind="componentField"
                />
              </FormControl>
              <FormDescription class="text-xs text-muted-foreground mt-1">
                我们将向此邮箱发送密码重置链接
              </FormDescription>
              <FormMessage />
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
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path>
            </svg>
            {{ loading ? '发送中...' : '发送重置链接' }}
          </Button>
        </Form>

        <!-- 邮件发送成功后的提示 -->
        <div v-else class="text-center space-y-4">
          <div class="w-20 h-20 bg-gradient-to-br from-success to-info rounded-full flex items-center justify-center mx-auto">
            <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"></path>
            </svg>
          </div>
          <div class="space-y-2">
            <h3 class="text-lg font-semibold text-foreground">邮件已发送</h3>
            <p class="text-sm text-muted-foreground">
              我们已向您的邮箱发送了密码重置链接。<br>
              请检查您的邮箱（包括垃圾邮件文件夹）。
            </p>
          </div>
          
          <!-- 重新发送计时器 -->
          <div v-if="countdown > 0" class="text-sm text-muted-foreground">
            {{ countdown }} 秒后可重新发送
          </div>
          
          <Button
            v-else
            variant="outline"
            @click="resendEmail"
            :disabled="loading"
            class="w-full"
          >
            重新发送邮件
          </Button>
        </div>
      </CardContent>

      <CardFooter class="flex-col space-y-4">
        <Separator />
        <div class="text-center space-y-2">
          <RouterLink
            to="/login"
            class="text-sm text-muted-foreground hover:text-primary transition-colors"
          >
            ← 返回登录
          </RouterLink>
          
          <div class="text-xs text-muted-foreground">
            想起密码了？
            <RouterLink 
              to="/login" 
              class="text-tech-blue hover:text-tech-purple font-medium transition-colors duration-200"
            >
              直接登录
            </RouterLink>
          </div>
        </div>
      </CardFooter>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'
import { authApi } from '@/apis/auth'

import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Separator } from '@/components/ui/separator'
import { Form, FormControl, FormDescription, FormField, FormItem, FormLabel, FormMessage } from '@/components/ui/form'

const router = useRouter()

// 响应式状态
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const isEmailSent = ref(false)
const countdown = ref(0)
const countdownTimer = ref<number | null>(null)

// 表单验证模式
const formSchema = toTypedSchema(z.object({
  email: z.string({ message: '请输入邮箱地址' }).email('请输入有效的邮箱地址')
}))

// 开始倒计时
const startCountdown = () => {
  countdown.value = 60
  countdownTimer.value = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(countdownTimer.value!)
      countdownTimer.value = null
    }
  }, 1000)
}

// 处理密码重置请求
const handlePasswordReset = async (values: any) => {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    await authApi.requestPasswordReset(values.email)
    
    isEmailSent.value = true
    successMessage.value = '密码重置邮件已发送，请检查您的邮箱'
    startCountdown()
    
  } catch (error: any) {
    console.error('Password reset request failed:', error)

    // 处理不同类型的错误
    if (error.response?.status === 404) {
      errorMessage.value = '该邮箱地址未注册，请检查邮箱地址或先注册账号'
    } else if (error.response?.status === 429) {
      errorMessage.value = '重置请求过于频繁，请稍后再试'
    } else if (error.response?.status === 400) {
      errorMessage.value = '邮箱地址格式不正确，请检查后重试'
    } else {
      errorMessage.value = error.response?.data?.message || '发送失败，请检查网络连接或稍后再试'
    }
  } finally {
    loading.value = false
  }
}

// 重新发送邮件
const resendEmail = async () => {
  // 重置状态，允许用户重新输入邮箱
  isEmailSent.value = false
  successMessage.value = ''
  errorMessage.value = ''
}

// 清理定时器
onUnmounted(() => {
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value)
  }
})
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