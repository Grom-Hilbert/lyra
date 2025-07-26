<template>
  <div class="min-h-screen bg-gradient-to-br from-background via-muted/30 to-accent/5">
    <!-- 导航栏 -->
    <header class="border-b border-border/50 bg-card/80 backdrop-blur-sm">
      <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex justify-between items-center h-16">
          <!-- Logo -->
          <div class="flex items-center space-x-3">
            <div class="w-10 h-10 bg-gradient-to-br from-tech-blue to-tech-purple rounded-xl flex items-center justify-center shadow-lg">
              <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
              </svg>
            </div>
            <div>
              <h1 class="text-xl font-bold bg-gradient-to-r from-tech-blue to-tech-purple bg-clip-text text-transparent">
                Lyra
              </h1>
              <p class="text-xs text-muted-foreground">文档管理系统</p>
            </div>
          </div>
          
          <!-- 导航按钮 -->
          <div class="flex items-center space-x-3">
            <Button variant="ghost" @click="$router.push('/login')">
              登录
            </Button>
            <Button @click="$router.push('/register')" class="bg-gradient-to-r from-tech-blue to-tech-purple hover:opacity-90">
              注册
            </Button>
          </div>
        </div>
      </div>
    </header>

    <!-- 主要内容 -->
    <main class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      
      <!-- 英雄区域 -->
      <section class="text-center mb-20">
        <div class="space-y-6">
          <!-- Logo图标 -->
          <div class="flex justify-center">
            <div class="w-32 h-32 bg-gradient-to-br from-tech-blue to-tech-purple rounded-3xl flex items-center justify-center shadow-2xl animate-pulse">
              <svg class="w-16 h-16 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"></path>
              </svg>
            </div>
          </div>
          
          <!-- 主标题 -->
          <div class="space-y-4">
            <h1 class="text-5xl md:text-6xl font-bold bg-gradient-to-r from-tech-blue via-tech-purple to-tech-cyan bg-clip-text text-transparent">
              企业级云原生
            </h1>
            <h2 class="text-4xl md:text-5xl font-bold text-foreground">
              文档管理系统
            </h2>
            <p class="text-xl text-muted-foreground max-w-2xl mx-auto leading-relaxed">
              为中小型企业打造的现代化文档管理平台，支持版本控制、团队协作和WebDAV协议，让文档管理变得简单高效。
            </p>
          </div>
          
          <!-- CTA按钮 -->
          <div class="flex flex-col sm:flex-row gap-4 justify-center items-center pt-8">
            <Button 
              @click="$router.push('/register')" 
              size="lg"
              class="w-full sm:w-auto bg-gradient-to-r from-tech-blue to-tech-purple hover:from-tech-blue/90 hover:to-tech-purple/90 text-white font-medium px-8 py-3 text-lg shadow-lg hover:shadow-xl transition-all duration-300 transform hover:scale-[1.02]"
            >
              <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"></path>
              </svg>
              免费开始使用
            </Button>
            
            <Button 
              @click="$router.push('/login')" 
              variant="outline" 
              size="lg"
              class="w-full sm:w-auto border-tech-blue/30 text-tech-blue hover:bg-tech-blue/10 hover:border-tech-blue/50 px-8 py-3 text-lg"
            >
              <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1"></path>
              </svg>
              已有账号登录
            </Button>
          </div>
        </div>
      </section>

      <!-- 功能特色 -->
      <section class="mb-20">
        <div class="text-center mb-12">
          <h3 class="text-3xl font-bold text-foreground mb-4">为什么选择 Lyra？</h3>
          <p class="text-lg text-muted-foreground">专为现代企业设计的文档管理解决方案</p>
        </div>
        
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          <!-- 版本控制 -->
          <Card class="p-6 hover:shadow-lg transition-shadow duration-300">
            <CardContent class="space-y-4">
              <div class="w-12 h-12 bg-gradient-to-br from-tech-blue to-tech-purple rounded-xl flex items-center justify-center">
                <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4V2a1 1 0 011-1h8a1 1 0 011 1v2m-9 4v10a2 2 0 002 2h6a2 2 0 002-2V8M7 8V6a2 2 0 012-2h6a2 2 0 012 2v2M7 8h10M12 12v4"></path>
                </svg>
              </div>
              <h4 class="text-xl font-semibold text-foreground">版本控制</h4>
              <p class="text-muted-foreground">
                内置Git版本控制系统，自动追踪文档变更历史，支持分支管理和协作开发。
              </p>
            </CardContent>
          </Card>

          <!-- WebDAV支持 -->
          <Card class="p-6 hover:shadow-lg transition-shadow duration-300">
            <CardContent class="space-y-4">
              <div class="w-12 h-12 bg-gradient-to-br from-tech-cyan to-tech-blue rounded-xl flex items-center justify-center">
                <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 01-9 9m9-9a9 9 0 00-9-9m9 9H3m9 9v-9m0-9v9m0 9c-5 0-9-4-9-9s4-9 9-9"></path>
                </svg>
              </div>
              <h4 class="text-xl font-semibold text-foreground">WebDAV协议</h4>
              <p class="text-muted-foreground">
                标准WebDAV协议支持，兼容各种办公软件和操作系统，无缝集成现有工作流。
              </p>
            </CardContent>
          </Card>

          <!-- 云原生架构 -->
          <Card class="p-6 hover:shadow-lg transition-shadow duration-300">
            <CardContent class="space-y-4">
              <div class="w-12 h-12 bg-gradient-to-br from-tech-purple to-tech-cyan rounded-xl flex items-center justify-center">
                <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z"></path>
                </svg>
              </div>
              <h4 class="text-xl font-semibold text-foreground">云原生架构</h4>
              <p class="text-muted-foreground">
                基于Docker和Kubernetes的现代云原生架构，支持弹性扩展和高可用部署。
              </p>
            </CardContent>
          </Card>

          <!-- 安全可靠 -->
          <Card class="p-6 hover:shadow-lg transition-shadow duration-300">
            <CardContent class="space-y-4">
              <div class="w-12 h-12 bg-gradient-to-br from-success to-tech-blue rounded-xl flex items-center justify-center">
                <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.031 9-11.622 0-1.042-.133-2.052-.382-3.016z"></path>
                </svg>
              </div>
              <h4 class="text-xl font-semibold text-foreground">安全可靠</h4>
              <p class="text-muted-foreground">
                企业级安全标准，支持多层认证、权限管理和数据加密，保障文档安全。
              </p>
            </CardContent>
          </Card>

          <!-- 团队协作 -->
          <Card class="p-6 hover:shadow-lg transition-shadow duration-300">
            <CardContent class="space-y-4">
              <div class="w-12 h-12 bg-gradient-to-br from-tech-cyan to-tech-purple rounded-xl flex items-center justify-center">
                <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
                </svg>
              </div>
              <h4 class="text-xl font-semibold text-foreground">团队协作</h4>
              <p class="text-muted-foreground">
                支持多人实时协作，文档共享和权限管理，提升团队工作效率。
              </p>
            </CardContent>
          </Card>

          <!-- 易于部署 -->
          <Card class="p-6 hover:shadow-lg transition-shadow duration-300">
            <CardContent class="space-y-4">
              <div class="w-12 h-12 bg-gradient-to-br from-tech-blue to-success rounded-xl flex items-center justify-center">
                <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"></path>
                </svg>
              </div>
              <h4 class="text-xl font-semibold text-foreground">易于部署</h4>
              <p class="text-muted-foreground">
                一键Docker部署，支持本地私有化部署，也可快速接入云端服务。
              </p>
            </CardContent>
          </Card>
        </div>
      </section>

      <!-- 技术栈 -->
      <section class="mb-20">
        <div class="text-center mb-12">
          <h3 class="text-3xl font-bold text-foreground mb-4">现代化技术栈</h3>
          <p class="text-lg text-muted-foreground">基于最新的技术构建，保证系统的先进性和可维护性</p>
        </div>
        
        <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
          <div class="text-center space-y-2">
            <div class="w-16 h-16 bg-card rounded-xl flex items-center justify-center mx-auto shadow-lg">
              <span class="text-2xl font-bold text-tech-blue">Vue</span>
            </div>
            <p class="text-sm text-muted-foreground">Vue 3.5</p>
          </div>
          <div class="text-center space-y-2">
            <div class="w-16 h-16 bg-card rounded-xl flex items-center justify-center mx-auto shadow-lg">
              <span class="text-2xl font-bold text-tech-green">Spring</span>
            </div>
            <p class="text-sm text-muted-foreground">Spring Boot</p>
          </div>
          <div class="text-center space-y-2">
            <div class="w-16 h-16 bg-card rounded-xl flex items-center justify-center mx-auto shadow-lg">
              <span class="text-2xl font-bold text-tech-cyan">Docker</span>
            </div>
            <p class="text-sm text-muted-foreground">容器化</p>
          </div>
          <div class="text-center space-y-2">
            <div class="w-16 h-16 bg-card rounded-xl flex items-center justify-center mx-auto shadow-lg">
              <span class="text-2xl font-bold text-tech-purple">K8s</span>
            </div>
            <p class="text-sm text-muted-foreground">Kubernetes</p>
          </div>
        </div>
      </section>

      <!-- 底部CTA -->
      <section class="text-center">
        <Card class="p-8 bg-gradient-to-r from-tech-blue/10 to-tech-purple/10 border-tech-blue/20">
          <CardContent class="space-y-6">
            <h3 class="text-2xl font-bold text-foreground">准备开始了吗？</h3>
            <p class="text-muted-foreground">
              立即注册Lyra账号，体验现代化的企业文档管理服务
            </p>
            <div class="flex flex-col sm:flex-row gap-4 justify-center">
              <Button 
                @click="$router.push('/register')" 
                size="lg"
                class="bg-gradient-to-r from-tech-blue to-tech-purple hover:opacity-90 text-white px-8 py-3"
              >
                立即注册
              </Button>
              <Button 
                @click="$router.push('/login')" 
                variant="outline" 
                size="lg"
                class="border-tech-blue/30 text-tech-blue hover:bg-tech-blue/10 px-8 py-3"
              >
                登录账号
              </Button>
            </div>
          </CardContent>
        </Card>
      </section>
    </main>

    <!-- 页脚 -->
    <footer class="border-t border-border/50 bg-card/50 backdrop-blur-sm mt-20">
      <div class="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div class="text-center text-sm text-muted-foreground">
          <p>&copy; 2024 Lyra 文档管理系统. 保留所有权利.</p>
          <p class="mt-2">企业级云原生文档管理解决方案</p>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'

// 页面元数据
defineOptions({
  name: 'HomeView'
})
</script>

<style scoped>
/* 自定义动画 */
@keyframes pulse {
  0%, 100% { 
    opacity: 1; 
    transform: scale(1);
  }
  50% { 
    opacity: 0.9; 
    transform: scale(1.02);
  }
}

.animate-pulse {
  animation: pulse 3s ease-in-out infinite;
}
</style> 