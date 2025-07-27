<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '@/components/ui/breadcrumb'
import { Button } from '@/components/ui/button'
import { 
  DropdownMenu, 
  DropdownMenuContent, 
  DropdownMenuItem, 
  DropdownMenuTrigger 
} from '@/components/ui/dropdown-menu'
import { ChevronDown, Copy, ExternalLink } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()

// 路由名称映射
const routeNameMap: Record<string, string> = {
  '/': '首页',
  '/dashboard': '仪表板',
  '/files': '文件管理',
  '/search': '搜索',
  '/preview': '文件预览',
  '/editor': '在线编辑',
  '/share': '分享管理',
  '/webdav-config': 'WebDAV配置',
  '/profile': '个人资料',
  '/settings': '用户设置',
  '/admin': '管理仪表板',
  '/admin/users': '用户管理',
  '/admin/config': '系统配置',
  '/admin/version-control': '版本控制',
  '/about': '关于',
}

// 生成面包屑路径
const breadcrumbItems = computed(() => {
  const pathSegments = route.path.split('/').filter(Boolean)
  const items: Array<{
    path: string
    name: string
    isLast: boolean
  }> = []

  // 添加首页
  if (route.path !== '/') {
    items.push({
      path: '/',
      name: '首页',
      isLast: false
    })
  }

  // 构建路径
  let currentPath = ''
  pathSegments.forEach((segment, index) => {
    currentPath += `/${segment}`
    const isLast = index === pathSegments.length - 1
    
    // 获取路由名称
    let name = routeNameMap[currentPath] || segment
    
    // 处理动态路由参数
    if (route.params.spaceId && segment === route.params.spaceId) {
      name = `空间 ${segment}`
    } else if (route.params.folderId && segment === route.params.folderId) {
      name = `文件夹 ${segment}`
    } else if (route.params.fileId && segment === route.params.fileId) {
      name = `文件 ${segment}`
    }

    items.push({
      path: currentPath,
      name,
      isLast
    })
  })

  return items
})

// 处理面包屑点击
const handleBreadcrumbClick = (path: string) => {
  if (path !== route.path) {
    router.push(path)
  }
}

// 复制当前路径
const copyCurrentPath = async () => {
  try {
    await navigator.clipboard.writeText(window.location.href)
    // 这里可以添加成功提示
    console.log('路径已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
  }
}

// 在新窗口打开
const openInNewWindow = () => {
  window.open(window.location.href, '_blank')
}

// 获取父级路径选项（用于下拉菜单）
const getParentPaths = (currentPath: string) => {
  const segments = currentPath.split('/').filter(Boolean)
  const paths: Array<{ path: string; name: string }> = []
  
  let buildPath = ''
  segments.slice(0, -1).forEach(segment => {
    buildPath += `/${segment}`
    paths.push({
      path: buildPath,
      name: routeNameMap[buildPath] || segment
    })
  })
  
  return paths
}
</script>

<template>
  <div class="flex items-center space-x-2">
    <Breadcrumb>
      <BreadcrumbList>
        <template v-for="(item, index) in breadcrumbItems" :key="item.path">
          <BreadcrumbItem>
            <!-- 最后一项显示为当前页面 -->
            <BreadcrumbPage v-if="item.isLast" class="font-medium">
              {{ item.name }}
            </BreadcrumbPage>
            
            <!-- 其他项显示为链接 -->
            <BreadcrumbLink 
              v-else
              @click="handleBreadcrumbClick(item.path)"
              class="cursor-pointer hover:text-foreground transition-colors"
            >
              {{ item.name }}
            </BreadcrumbLink>
          </BreadcrumbItem>
          
          <!-- 分隔符 -->
          <BreadcrumbSeparator v-if="!item.isLast" />
        </template>
      </BreadcrumbList>
    </Breadcrumb>

    <!-- 路径操作下拉菜单 -->
    <DropdownMenu>
      <DropdownMenuTrigger as-child>
        <Button variant="ghost" size="sm" class="h-8 w-8 p-0">
          <ChevronDown class="h-4 w-4" />
          <span class="sr-only">路径操作</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" class="w-48">
        <DropdownMenuItem @click="copyCurrentPath" class="cursor-pointer">
          <Copy class="w-4 h-4 mr-2" />
          复制当前路径
        </DropdownMenuItem>
        <DropdownMenuItem @click="openInNewWindow" class="cursor-pointer">
          <ExternalLink class="w-4 h-4 mr-2" />
          在新窗口打开
        </DropdownMenuItem>
        
        <!-- 父级路径快速导航 -->
        <template v-if="breadcrumbItems.length > 1">
          <div class="border-t my-1"></div>
          <div class="px-2 py-1 text-xs text-muted-foreground font-medium">
            快速导航
          </div>
          <DropdownMenuItem 
            v-for="parent in getParentPaths(route.path)" 
            :key="parent.path"
            @click="handleBreadcrumbClick(parent.path)"
            class="cursor-pointer"
          >
            {{ parent.name }}
          </DropdownMenuItem>
        </template>
      </DropdownMenuContent>
    </DropdownMenu>
  </div>
</template>
