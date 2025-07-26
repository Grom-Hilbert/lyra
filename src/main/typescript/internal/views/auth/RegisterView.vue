<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-background via-muted/50 to-accent/10">
    <div class="w-full max-w-md p-8 bg-card rounded-2xl shadow-2xl border border-border">
      <!-- Logo和标题 -->
      <div class="text-center mb-8">
        <div class="w-16 h-16 bg-gradient-to-br from-tech-blue to-tech-purple rounded-2xl flex items-center justify-center mx-auto mb-4 shadow-lg">
          <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
          </svg>
        </div>
        <h1 class="text-2xl font-bold bg-gradient-to-r from-tech-blue to-tech-purple bg-clip-text text-transparent">
          注册 Lyra
        </h1>
        <p class="text-muted-foreground mt-2">加入企业级云原生文档管理系统</p>
      </div>

      <!-- 注册表单 -->
      <form @submit.prevent="handleRegister" class="space-y-6">
        <div class="space-y-4">
          <!-- 用户名输入 -->
          <div>
            <label for="username" class="block text-sm font-medium text-foreground mb-2">
              用户名
            </label>
            <input
              id="username"
              v-model="registerForm.username"
              type="text"
              required
              placeholder="请输入用户名"
              class="w-full px-4 py-3 bg-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all"
            />
          </div>

          <!-- 邮箱输入 -->
          <div>
            <label for="email" class="block text-sm font-medium text-foreground mb-2">
              邮箱
            </label>
            <input
              id="email"
              v-model="registerForm.email"
              type="email"
              required
              placeholder="请输入邮箱地址"
              class="w-full px-4 py-3 bg-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all"
            />
          </div>

          <!-- 密码输入 -->
          <div>
            <label for="password" class="block text-sm font-medium text-foreground mb-2">
              密码
            </label>
            <input
              id="password"
              v-model="registerForm.password"
              type="password"
              required
              placeholder="请输入密码（至少6位）"
              class="w-full px-4 py-3 bg-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all"
            />
          </div>

          <!-- 确认密码输入 -->
          <div>
            <label for="confirmPassword" class="block text-sm font-medium text-foreground mb-2">
              确认密码
            </label>
            <input
              id="confirmPassword"
              v-model="registerForm.confirmPassword"
              type="password"
              required
              placeholder="请再次输入密码"
              class="w-full px-4 py-3 bg-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all"
            />
          </div>

          <!-- 同意条款 -->
          <div class="flex items-start">
            <input
              id="agree"
              v-model="registerForm.agreeToTerms"
              type="checkbox"
              required
              class="w-4 h-4 mt-1 text-primary bg-background border-border rounded focus:ring-primary focus:ring-2"
            />
            <label for="agree" class="ml-2 text-sm text-muted-foreground">
              我已阅读并同意 
              <a href="#" class="text-primary hover:underline">服务条款</a> 
              和 
              <a href="#" class="text-primary hover:underline">隐私政策</a>
            </label>
          </div>
        </div>

        <!-- 注册按钮 -->
        <button
          type="submit"
          :disabled="loading"
          class="w-full py-3 px-4 bg-gradient-to-r from-tech-blue to-tech-purple text-white font-medium rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:opacity-50 transition-all"
        >
          <span v-if="loading">注册中...</span>
          <span v-else>注册账号</span>
        </button>

        <!-- 其他选项 -->
        <div class="text-center space-y-3">
          <router-link
            to="/login"
            class="text-sm text-muted-foreground hover:text-primary transition-colors"
          >
            已有账号？立即登录
          </router-link>
          
          <div class="text-xs text-muted-foreground">
            注册后需要管理员审核激活
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)

// 注册表单数据
const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  agreeToTerms: false
})

// 处理注册
const handleRegister = async () => {
  // 基本验证
  if (!registerForm.username || !registerForm.email || !registerForm.password || !registerForm.confirmPassword) {
    alert('请填写所有必填字段')
    return
  }

  if (registerForm.password !== registerForm.confirmPassword) {
    alert('两次输入的密码不一致')
    return
  }

  if (registerForm.password.length < 6) {
    alert('密码长度至少6位')
    return
  }

  if (!registerForm.agreeToTerms) {
    alert('请同意服务条款和隐私政策')
    return
  }

  loading.value = true

  try {
    await userStore.register({
      username: registerForm.username,
      email: registerForm.email,
      password: registerForm.password,
      confirmPassword: registerForm.confirmPassword,
      displayName: registerForm.username
    } as any)

    alert('注册成功！请等待管理员审核激活您的账号。')
    await router.push('/login')
  } catch (error) {
    console.error('注册失败:', error)
    alert('注册失败，请检查输入信息或稍后再试')
  } finally {
    loading.value = false
  }
}
</script> 