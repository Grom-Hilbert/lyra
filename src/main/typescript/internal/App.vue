<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user.ts'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 判断是否为认证页面（登录、注册等）
const isAuthPage = computed(() => {
  const authRoutes = ['/login', '/register', '/about']
  return authRoutes.includes(route.path)
})

// 处理用户下拉菜单命令
const handleUserCommand = async (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm(
          '确定要退出登录吗？',
          '确认退出',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        await userStore.logout()
        ElMessage.success('已成功退出登录')
        router.push('/login')
      } catch (error) {
        // 用户取消操作
      }
      break
  }
}
</script>

<template>
  <div id="app">
    <!-- 登录页面不显示布局 -->
    <template v-if="isAuthPage">
      <router-view />
    </template>
    
    <!-- 主应用布局 -->
    <template v-else>
      <el-container class="app-container">
        <!-- 顶部导航栏 -->
        <el-header class="app-header">
          <div class="header-left">
            <div class="logo-placeholder">L</div>
            <h1 class="app-title">Lyra 文档管理系统</h1>
          </div>
          
          <div class="header-right">
            <!-- 用户信息下拉菜单 -->
            <el-dropdown v-if="userStore.isAuthenticated" @command="handleUserCommand">
              <span class="user-info">
                <el-avatar :size="32" :src="userStore.user?.avatar">
                  {{ userStore.displayName.charAt(0) }}
                </el-avatar>
                <span class="username">{{ userStore.displayName }}</span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>
                    个人资料
                  </el-dropdown-item>
                  <el-dropdown-item command="settings">
                    <el-icon><Setting /></el-icon>
                    用户设置
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>
                    退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            
            <!-- 未登录状态 -->
            <div v-else class="auth-buttons">
              <el-button @click="$router.push('/login')">登录</el-button>
              <el-button type="primary" @click="$router.push('/register')">注册</el-button>
            </div>
          </div>
        </el-header>

        <el-container>
          <!-- 侧边导航栏 -->
          <el-aside v-if="userStore.isAuthenticated" width="240px" class="app-sidebar">
            <el-menu
              :default-active="$route.path"
              :router="true"
              class="sidebar-menu"
              background-color="#304156"
              text-color="#bfcbd9"
              active-text-color="#409EFF"
            >
              <el-menu-item index="/dashboard">
                <el-icon><Monitor /></el-icon>
                <span>仪表板</span>
              </el-menu-item>
              
              <el-menu-item index="/about">
                <el-icon><InfoFilled /></el-icon>
                <span>关于</span>
              </el-menu-item>
            </el-menu>
          </el-aside>

          <!-- 主内容区域 -->
          <el-main class="app-main">
            <router-view />
          </el-main>
        </el-container>
      </el-container>
    </template>
  </div>
</template>

<style scoped>
.app-container {
  height: 100vh;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background-color: #ffffff;
  border-bottom: 1px solid #e6e8eb;
}

.header-left {
  display: flex;
  align-items: center;
}

.logo-placeholder {
  width: 32px;
  height: 32px;
  margin-right: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  font-size: 18px;
}

.app-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #2c3e50;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 6px;
  transition: background-color 0.2s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.username {
  margin: 0 8px;
  font-size: 14px;
  color: #606266;
}

.auth-buttons {
  display: flex;
  gap: 12px;
}

.app-sidebar {
  background-color: #304156;
}

.sidebar-menu {
  border-right: none;
  height: 100%;
}

.app-main {
  background-color: #f0f2f5;
  padding: 20px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .app-header {
    padding: 0 16px;
  }
  
  .app-title {
    font-size: 18px;
  }
  
  .app-sidebar {
    width: 200px !important;
  }
  
  .app-main {
    padding: 16px;
  }
}
</style>
