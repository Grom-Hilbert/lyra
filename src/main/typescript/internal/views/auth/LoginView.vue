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
          登录 Lyra
        </h1>
        <p class="text-muted-foreground mt-2">企业级云原生文档管理系统</p>
      </div>

      <!-- 登录表单 -->
      <form @submit.prevent="handleLogin" class="space-y-6">
        <div class="space-y-4">
          <!-- 用户名输入 -->
          <div>
            <label for="username" class="block text-sm font-medium text-foreground mb-2">
              用户名或邮箱
            </label>
            <input
              id="username"
              v-model="loginForm.username"
              type="text"
              required
              placeholder="请输入用户名或邮箱"
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
              v-model="loginForm.password"
              type="password"
              required
              placeholder="请输入密码"
              class="w-full px-4 py-3 bg-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-all"
            />
          </div>

          <!-- 记住我 -->
          <div class="flex items-center">
            <input
              id="remember"
              v-model="loginForm.rememberMe"
              type="checkbox"
              class="w-4 h-4 text-primary bg-background border-border rounded focus:ring-primary focus:ring-2"
            />
            <label for="remember" class="ml-2 text-sm text-muted-foreground">
              记住我（7天内免登录）
            </label>
          </div>
        </div>

        <!-- 登录按钮 -->
        <button
          type="submit"
          :disabled="loading"
          class="w-full py-3 px-4 bg-gradient-to-r from-tech-blue to-tech-purple text-white font-medium rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:opacity-50 transition-all"
        >
          <span v-if="loading">登录中...</span>
          <span v-else>登录</span>
        </button>

        <!-- 其他选项 -->
        <div class="text-center space-y-3">
          <router-link
            to="/register"
            class="text-sm text-muted-foreground hover:text-primary transition-colors"
          >
            还没有账号？立即注册
          </router-link>
          
          <div class="text-xs text-muted-foreground">
            忘记密码？请联系管理员重置
          </div>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false
})

// 处理登录
const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    alert('请填写用户名和密码')
    return
  }

  loading.value = true

  try {
    await userStore.login({
      username: loginForm.username,
      password: loginForm.password,
      rememberMe: loginForm.rememberMe
    })

    // 登录成功后跳转
    const redirect = (route.query.redirect as string) || '/dashboard'
    await router.push(redirect)
    
    console.log('登录成功')
  } catch (error) {
    console.error('登录失败:', error)
    alert('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.login-header {
  padding: 40px 40px 20px;
  text-align: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.login-header h2 {
  margin: 0 0 8px;
  font-size: 28px;
  font-weight: 600;
}

.login-header p {
  margin: 0;
  opacity: 0.9;
  font-size: 14px;
}

.login-form {
  padding: 40px;
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.login-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
}

.register-link {
  text-align: center;
  margin-top: 20px;
  color: #666;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-container {
    padding: 10px;
  }
  
  .login-header,
  .login-form {
    padding: 30px 20px;
  }
  
  .login-header h2 {
    font-size: 24px;
  }
}
</style> 