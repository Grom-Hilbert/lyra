<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
} from '@/components/ui/sidebar'
import { Badge } from '@/components/ui/badge'
import { 
  Monitor, 
  FolderOpen, 
  Search, 
  Sparkles,
  Shield,
  Users,
  Settings,
  GitBranch,
  FileText,
  Edit,
  Share,
  Globe
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 主要功能菜单项
const mainMenuItems = computed(() => [
  { 
    path: '/dashboard', 
    icon: Monitor, 
    label: '仪表板', 
    badge: null,
    description: '系统概览和快速操作'
  },
  { 
    path: '/files', 
    icon: FolderOpen, 
    label: '文件管理', 
    badge: 'New',
    description: '文件和文件夹管理'
  },
  { 
    path: '/search', 
    icon: Search, 
    label: '搜索', 
    badge: null,
    description: '全文搜索和高级筛选'
  },
])

// 工具菜单项
const toolMenuItems = computed(() => [
  { 
    path: '/preview', 
    icon: FileText, 
    label: '文件预览', 
    badge: null,
    description: '多格式文件预览'
  },
  { 
    path: '/editor', 
    icon: Edit, 
    label: '在线编辑', 
    badge: null,
    description: '在线文本编辑器'
  },
  { 
    path: '/share', 
    icon: Share, 
    label: '分享管理', 
    badge: null,
    description: '文件分享和权限管理'
  },
  { 
    path: '/webdav-config', 
    icon: Globe, 
    label: 'WebDAV配置', 
    badge: null,
    description: 'WebDAV客户端配置'
  },
])

// 管理员菜单项
const adminMenuItems = computed(() => {
  if (!userStore.isAdmin) return []
  
  return [
    { 
      path: '/admin', 
      icon: Shield, 
      label: '管理仪表板', 
      badge: 'Admin',
      description: '系统管理概览'
    },
    { 
      path: '/admin/users', 
      icon: Users, 
      label: '用户管理', 
      badge: null,
      description: '用户和权限管理'
    },
    { 
      path: '/admin/config', 
      icon: Settings, 
      label: '系统配置', 
      badge: null,
      description: '系统参数配置'
    },
    { 
      path: '/admin/version-control', 
      icon: GitBranch, 
      label: '版本控制', 
      badge: null,
      description: '版本控制管理'
    }
  ]
})

// 其他菜单项
const otherMenuItems = computed(() => [
  { 
    path: '/about', 
    icon: Sparkles, 
    label: '关于', 
    badge: null,
    description: '系统信息和帮助'
  }
])

// 检查菜单项是否激活
const isActive = (path: string) => {
  if (path === '/files') {
    // 文件管理相关路由都算激活状态
    return route.path.startsWith('/files')
  }
  return route.path === path
}

// 处理菜单点击
const handleMenuClick = (path: string) => {
  router.push(path)
}
</script>

<template>
  <!-- 主要功能 -->
  <SidebarGroup>
    <SidebarGroupLabel>主要功能</SidebarGroupLabel>
    <SidebarGroupContent>
      <SidebarMenu>
        <SidebarMenuItem v-for="item in mainMenuItems" :key="item.path">
          <SidebarMenuButton 
            :is-active="isActive(item.path)"
            @click="handleMenuClick(item.path)"
            class="cursor-pointer"
          >
            <component :is="item.icon" class="w-4 h-4" />
            <span>{{ item.label }}</span>
            <Badge v-if="item.badge" variant="secondary" class="ml-auto text-xs">
              {{ item.badge }}
            </Badge>
          </SidebarMenuButton>
        </SidebarMenuItem>
      </SidebarMenu>
    </SidebarGroupContent>
  </SidebarGroup>

  <!-- 工具 -->
  <SidebarGroup>
    <SidebarGroupLabel>工具</SidebarGroupLabel>
    <SidebarGroupContent>
      <SidebarMenu>
        <SidebarMenuItem v-for="item in toolMenuItems" :key="item.path">
          <SidebarMenuButton 
            :is-active="isActive(item.path)"
            @click="handleMenuClick(item.path)"
            class="cursor-pointer"
          >
            <component :is="item.icon" class="w-4 h-4" />
            <span>{{ item.label }}</span>
            <Badge v-if="item.badge" variant="secondary" class="ml-auto text-xs">
              {{ item.badge }}
            </Badge>
          </SidebarMenuButton>
        </SidebarMenuItem>
      </SidebarMenu>
    </SidebarGroupContent>
  </SidebarGroup>

  <!-- 管理员功能 -->
  <SidebarGroup v-if="adminMenuItems.length > 0">
    <SidebarGroupLabel>管理员</SidebarGroupLabel>
    <SidebarGroupContent>
      <SidebarMenu>
        <SidebarMenuItem v-for="item in adminMenuItems" :key="item.path">
          <SidebarMenuButton 
            :is-active="isActive(item.path)"
            @click="handleMenuClick(item.path)"
            class="cursor-pointer"
          >
            <component :is="item.icon" class="w-4 h-4" />
            <span>{{ item.label }}</span>
            <Badge v-if="item.badge" variant="secondary" class="ml-auto text-xs">
              {{ item.badge }}
            </Badge>
          </SidebarMenuButton>
        </SidebarMenuItem>
      </SidebarMenu>
    </SidebarGroupContent>
  </SidebarGroup>

  <!-- 其他 -->
  <SidebarGroup>
    <SidebarGroupLabel>其他</SidebarGroupLabel>
    <SidebarGroupContent>
      <SidebarMenu>
        <SidebarMenuItem v-for="item in otherMenuItems" :key="item.path">
          <SidebarMenuButton 
            :is-active="isActive(item.path)"
            @click="handleMenuClick(item.path)"
            class="cursor-pointer"
          >
            <component :is="item.icon" class="w-4 h-4" />
            <span>{{ item.label }}</span>
            <Badge v-if="item.badge" variant="secondary" class="ml-auto text-xs">
              {{ item.badge }}
            </Badge>
          </SidebarMenuButton>
        </SidebarMenuItem>
      </SidebarMenu>
    </SidebarGroupContent>
  </SidebarGroup>
</template>
