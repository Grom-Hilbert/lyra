<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-background via-muted/50 to-accent/10 p-4">
    <Card class="w-full max-w-md shadow-2xl border-border/50 backdrop-blur-sm bg-card/80">
      <CardHeader class="space-y-4">
        <!-- Logo和标题 -->
        <div class="text-center">
          <div class="w-16 h-16 bg-gradient-to-br from-tech-blue to-tech-purple rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg animate-pulse">
            <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
            </svg>
          </div>
          <CardTitle class="text-2xl font-bold bg-gradient-to-r from-tech-blue to-tech-purple bg-clip-text text-transparent">
            设置新密码
          </CardTitle>
          <CardDescription class="text-muted-foreground mt-2">
            请设置您的新密码
          </CardDescription>
        </div>
      </CardHeader>

      <CardContent class="space-y-6">
        <!-- 令牌无效或过期提示 -->
        <Alert v-if="tokenError" variant="destructive" class="animate-in slide-in-from-top-2">
          <svg class="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="15" y1="9" x2="9" y2="15"></line>
            <line x1="9" y1="9" x2="15" y2="15"></line>
          </svg>
          <AlertDescription>{{ tokenError }}</AlertDescription>
        </Alert>

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

        <!-- 密码重置成功后的提示 -->
        <div v-if="isPasswordReset" class="text-center space-y-4">
          <div class="w-20 h-20 bg-gradient-to-br from-success to-tech-blue rounded-full flex items-center justify-center mx-auto">
            <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
            </svg>
          </div>
          <div class="space-y-2">
            <h3 class="text-lg font-semibold text-foreground">密码重置成功</h3>
            <p class="text-sm text-muted-foreground">
              您的密码已成功重置。<br>
              您现在可以使用新密码登录。
            </p>
          </div>
          
          <!-- 跳转倒计时 -->
          <div v-if="redirectCountdown > 0" class="text-sm text-muted-foreground">
            {{ redirectCountdown }} 秒后自动跳转到登录页面
          </div>
          
          <Button
            @click="goToLogin"
            class="w-full bg-gradient-to-r from-tech-blue to-tech-purple hover:from-tech-blue/90 hover:to-tech-purple/90 text-white"
          >
            立即登录
          </Button>
        </div>

        <!-- 密码重置表单 -->
        <Form v-else-if="!tokenError" :validation-schema="formSchema" @submit="handlePasswordReset" class="space-y-5">
          <FormField v-slot="{ componentField }" name="newPassword">
            <FormItem>
              <FormLabel class="text-sm font-medium">新密码</FormLabel>
              <FormControl>
                <div class="relative">
                  <Input
                    :type="showNewPassword ? 'text' : 'password'"
                    placeholder="请输入新密码（至少6位）"
                    :disabled="loading"
                    class="pr-10 transition-all duration-200 focus:ring-2 focus:ring-tech-blue/20"
                    v-bind="componentField"
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    class="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                    @click="showNewPassword = !showNewPassword"
                  >
                    <svg v-if="!showNewPassword" class="h-4 w-4 text-muted-foreground" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                    </svg>
                    <svg v-else class="h-4 w-4 text-muted-foreground" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21"></path>
                    </svg>
                  </Button>
                </div>
              </FormControl>
              <FormDescription class="text-xs text-muted-foreground mt-1">
                密码必须包含字母和数字，至少6位
              </FormDescription>
              <FormMessage />
            </FormItem>
          </FormField>

          <FormField v-slot="{ componentField }" name="confirmPassword">
            <FormItem>
              <FormLabel class="text-sm font-medium">确认新密码</FormLabel>
              <FormControl>
                <div class="relative">
                  <Input
                    :type="showConfirmPassword ? 'text' : 'password'"
                    placeholder="请再次输入新密码"
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

          <Button
            type="submit"
            :disabled="loading"
            class="w-full bg-gradient-to-r from-tech-blue to-tech-purple hover:from-tech-blue/90 hover:to-tech-purple/90 text-white font-medium py-2.5 transition-all duration-300 transform hover:scale-[1.02] active:scale-[0.98]"
          >
            <svg v-if="loading" class="w-4 h-4 mr-2 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
            </svg>
            <svg v-else class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"></path>
            </svg>
            {{ loading ? '设置中...' : '重置密码' }}
          </Button>
        </Form>
      </CardContent>

      <CardFooter class="flex-col space-y-4" v-if="!isPasswordReset">
        <Separator />
        <div class="text-center space-y-2">
          <RouterLink
            to="/login"
            class="text-sm text-muted-foreground hover:text-primary transition-colors"
          >
            ← 返回登录
          </RouterLink>
          
          <div class="text-xs text-muted-foreground">
            没有收到邮件？
            <RouterLink 
              to="/forgot-password" 
              class="text-tech-blue hover:text-tech-purple font-medium transition-colors duration-200"
            >
              重新发送
            </RouterLink>
          </div>
        </div>
      </CardFooter>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
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
const route = useRoute()

// 响应式状态
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const tokenError = ref('')
const isPasswordReset = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)
const redirectCountdown = ref(5)
const redirectTimer = ref<number | null>(null)

// 重置令牌（从URL参数获取）
const resetToken = ref(route.query.token as string || '')

// 表单验证模式
const formSchema = toTypedSchema(z.object({
  newPassword: z.string({ message: '请输入新密码' })
    .min(6, '密码至少6位')
    .regex(/^(?=.*[a-zA-Z])(?=.*\d).*$/, '密码必须包含字母和数字'),
  confirmPassword: z.string({ message: '请确认新密码' })
}).refine(data => data.newPassword === data.confirmPassword, {
  message: '两次输入的密码不一致',
  path: ['confirmPassword']
}))

// 开始跳转倒计时
const startRedirectCountdown = () => {
  redirectTimer.value = setInterval(() => {
    redirectCountdown.value--
    if (redirectCountdown.value <= 0) {
      clearInterval(redirectTimer.value!)
      redirectTimer.value = null
      goToLogin()
    }
  }, 1000)
}

// 跳转到登录页面
const goToLogin = () => {
  router.push('/login')
}

// 验证重置令牌
const validateResetToken = async () => {
  if (!resetToken.value) {
    tokenError.value = '重置链接无效，请重新申请密码重置'
    return
  }

  try {
    // 调用API验证令牌是否有效
    await authApi.validateResetToken(resetToken.value)
  } catch (error: any) {
    console.error('Token validation failed:', error)
    if (error.response?.status === 404 || error.response?.status === 400) {
      tokenError.value = '重置链接无效或已过期，请重新申请密码重置'
    } else {
      tokenError.value = '验证重置链接时出错，请稍后再试'
    }
  }
}

// 处理密码重置
const handlePasswordReset = async (values: any) => {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    await authApi.resetPassword({
      token: resetToken.value,
      newPassword: values.newPassword,
      confirmPassword: values.confirmPassword
    })
    
    isPasswordReset.value = true
    successMessage.value = '密码重置成功！'
    startRedirectCountdown()
    
  } catch (error: any) {
    console.error('Password reset failed:', error)
    const message = error.response?.data?.message
    
    if (message && (message.includes('token') || message.includes('expired') || message.includes('invalid'))) {
      tokenError.value = '重置链接已过期或无效，请重新申请密码重置'
    } else {
      errorMessage.value = message || '密码重置失败，请稍后再试'
    }
  } finally {
    loading.value = false
  }
}

// 组件挂载时验证令牌
onMounted(() => {
  validateResetToken()
})

// 清理定时器
onUnmounted(() => {
  if (redirectTimer.value) {
    clearInterval(redirectTimer.value)
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