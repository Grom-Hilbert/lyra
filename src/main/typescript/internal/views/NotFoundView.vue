<template>
  <div class="min-h-screen flex items-center justify-center bg-gradient-to-br from-background via-muted/30 to-accent/5 p-4">
    <div class="text-center max-w-md">
      <!-- 404图标 -->
      <div class="mb-8">
        <div class="w-32 h-32 bg-gradient-to-br from-tech-blue to-tech-purple rounded-3xl flex items-center justify-center mx-auto mb-6 shadow-2xl animate-bounce">
          <svg class="w-16 h-16 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.268 16.5c-.77.833.192 2.5 1.732 2.5z"></path>
          </svg>
        </div>
        
        <!-- 404文字 -->
        <h1 class="text-8xl font-bold bg-gradient-to-r from-tech-blue via-tech-purple to-tech-cyan bg-clip-text text-transparent mb-4">
          404
        </h1>
        
        <h2 class="text-2xl font-semibold text-foreground mb-4">
          页面未找到
        </h2>
        
        <p class="text-muted-foreground mb-8 leading-relaxed">
          抱歉，您访问的页面不存在或已被移动。<br>
          请检查网址是否正确，或返回首页继续浏览。
        </p>
      </div>
      
      <!-- 操作按钮 -->
      <div class="space-y-4">
        <Button 
          @click="$router.push('/dashboard')" 
          class="w-full bg-gradient-to-r from-tech-blue to-tech-purple hover:from-tech-blue/90 hover:to-tech-purple/90 text-white font-medium py-3 px-6 rounded-xl shadow-lg hover:shadow-xl transition-all duration-300"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"></path>
          </svg>
          返回首页
        </Button>
        
        <Button 
          variant="outline" 
          @click="$router.back()" 
          class="w-full border-tech-blue/30 text-tech-blue hover:bg-tech-blue/10 hover:border-tech-blue/50 py-3 px-6 rounded-xl transition-all duration-300"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
          </svg>
          返回上页
        </Button>
      </div>
      
      <!-- 帮助链接 -->
      <div class="mt-8 text-sm text-muted-foreground">
        需要帮助？ 
        <router-link to="/help" class="text-tech-blue hover:text-tech-purple transition-colors duration-200 underline">
          联系支持
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Button } from '@/components/ui/button'

// 页面元数据
defineOptions({
  name: 'NotFoundView'
})
</script>

<style scoped>
/* 可以添加一些特殊的动画效果 */
.animate-bounce {
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 20%, 53%, 80%, 100% {
    animation-timing-function: cubic-bezier(0.215, 0.610, 0.355, 1.000);
    transform: translate3d(0,0,0);
  }
  40%, 43% {
    animation-timing-function: cubic-bezier(0.755, 0.050, 0.855, 0.060);
    transform: translate3d(0,-30px,0);
  }
  70% {
    animation-timing-function: cubic-bezier(0.755, 0.050, 0.855, 0.060);
    transform: translate3d(0,-15px,0);
  }
  90% {
    transform: translate3d(0,-4px,0);
  }
}
</style> 