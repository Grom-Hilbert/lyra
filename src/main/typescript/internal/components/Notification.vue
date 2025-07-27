<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { 
  DropdownMenu, 
  DropdownMenuContent, 
  DropdownMenuItem, 
  DropdownMenuSeparator,
  DropdownMenuTrigger 
} from '@/components/ui/dropdown-menu'
import { ScrollArea } from '@/components/ui/scroll-area'
import { 
  Bell, 
  Check, 
  X, 
  Info, 
  AlertTriangle, 
  CheckCircle, 
  XCircle,
  Settings
} from 'lucide-vue-next'

// 通知类型
interface Notification {
  id: string
  type: 'info' | 'success' | 'warning' | 'error'
  title: string
  message: string
  timestamp: Date
  read: boolean
  actions?: Array<{
    label: string
    action: () => void
  }>
}

// 通知数据
const notifications = ref<Notification[]>([
  {
    id: '1',
    type: 'info',
    title: '系统更新',
    message: 'Lyra 系统已更新到最新版本，新增了文件预览功能。',
    timestamp: new Date(Date.now() - 1000 * 60 * 30), // 30分钟前
    read: false
  },
  {
    id: '2',
    type: 'success',
    title: '文件上传成功',
    message: '您的文件 "项目文档.pdf" 已成功上传到企业空间。',
    timestamp: new Date(Date.now() - 1000 * 60 * 60 * 2), // 2小时前
    read: false
  },
  {
    id: '3',
    type: 'warning',
    title: '存储空间警告',
    message: '您的个人空间使用率已达到 85%，建议清理不必要的文件。',
    timestamp: new Date(Date.now() - 1000 * 60 * 60 * 24), // 1天前
    read: true
  }
])

// 未读通知数量
const unreadCount = computed(() => {
  return notifications.value.filter(n => !n.read).length
})

// 获取通知图标
const getNotificationIcon = (type: string) => {
  switch (type) {
    case 'success':
      return CheckCircle
    case 'warning':
      return AlertTriangle
    case 'error':
      return XCircle
    default:
      return Info
  }
}

// 获取通知图标颜色
const getNotificationIconColor = (type: string) => {
  switch (type) {
    case 'success':
      return 'text-green-500'
    case 'warning':
      return 'text-yellow-500'
    case 'error':
      return 'text-red-500'
    default:
      return 'text-blue-500'
  }
}

// 格式化时间
const formatTime = (date: Date) => {
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (minutes < 60) {
    return `${minutes}分钟前`
  } else if (hours < 24) {
    return `${hours}小时前`
  } else {
    return `${days}天前`
  }
}

// 标记为已读
const markAsRead = (id: string) => {
  const notification = notifications.value.find(n => n.id === id)
  if (notification) {
    notification.read = true
  }
}

// 删除通知
const deleteNotification = (id: string) => {
  const index = notifications.value.findIndex(n => n.id === id)
  if (index > -1) {
    notifications.value.splice(index, 1)
  }
}

// 标记所有为已读
const markAllAsRead = () => {
  notifications.value.forEach(n => {
    n.read = true
  })
}

// 清空所有通知
const clearAllNotifications = () => {
  notifications.value = []
}

// 处理通知点击
const handleNotificationClick = (notification: Notification) => {
  if (!notification.read) {
    markAsRead(notification.id)
  }
  // 这里可以添加跳转逻辑
}

onMounted(() => {
  // 这里可以添加从API获取通知的逻辑
})
</script>

<template>
  <DropdownMenu>
    <DropdownMenuTrigger as-child>
      <Button variant="ghost" size="sm" class="relative">
        <Bell class="w-5 h-5" />
        <Badge 
          v-if="unreadCount > 0" 
          variant="destructive" 
          class="absolute -top-1 -right-1 h-5 w-5 flex items-center justify-center p-0 text-xs"
        >
          {{ unreadCount > 99 ? '99+' : unreadCount }}
        </Badge>
        <span class="sr-only">通知</span>
      </Button>
    </DropdownMenuTrigger>
    
    <DropdownMenuContent align="end" class="w-80">
      <!-- 通知头部 -->
      <div class="flex items-center justify-between p-4 border-b">
        <h3 class="font-semibold">通知</h3>
        <div class="flex items-center space-x-2">
          <Button 
            v-if="unreadCount > 0"
            variant="ghost" 
            size="sm" 
            @click="markAllAsRead"
            class="text-xs"
          >
            全部已读
          </Button>
          <Button 
            variant="ghost" 
            size="sm" 
            @click="clearAllNotifications"
            class="text-xs"
          >
            清空
          </Button>
        </div>
      </div>
      
      <!-- 通知列表 -->
      <ScrollArea class="h-96">
        <div v-if="notifications.length === 0" class="p-4 text-center text-muted-foreground">
          暂无通知
        </div>
        
        <div v-else class="space-y-1">
          <div
            v-for="notification in notifications"
            :key="notification.id"
            @click="handleNotificationClick(notification)"
            :class="[
              'flex items-start space-x-3 p-3 hover:bg-accent cursor-pointer transition-colors',
              !notification.read && 'bg-muted/50'
            ]"
          >
            <!-- 通知图标 -->
            <component 
              :is="getNotificationIcon(notification.type)" 
              :class="['w-5 h-5 mt-0.5 flex-shrink-0', getNotificationIconColor(notification.type)]"
            />
            
            <!-- 通知内容 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-start justify-between">
                <h4 :class="['text-sm font-medium', !notification.read && 'font-semibold']">
                  {{ notification.title }}
                </h4>
                <div class="flex items-center space-x-1 ml-2">
                  <!-- 未读标识 -->
                  <div 
                    v-if="!notification.read" 
                    class="w-2 h-2 bg-primary rounded-full flex-shrink-0"
                  ></div>
                  <!-- 删除按钮 -->
                  <Button 
                    variant="ghost" 
                    size="sm" 
                    @click.stop="deleteNotification(notification.id)"
                    class="h-6 w-6 p-0 opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <X class="w-3 h-3" />
                  </Button>
                </div>
              </div>
              <p class="text-sm text-muted-foreground mt-1">
                {{ notification.message }}
              </p>
              <p class="text-xs text-muted-foreground mt-2">
                {{ formatTime(notification.timestamp) }}
              </p>
              
              <!-- 操作按钮 -->
              <div v-if="notification.actions" class="flex space-x-2 mt-2">
                <Button
                  v-for="action in notification.actions"
                  :key="action.label"
                  variant="outline"
                  size="sm"
                  @click.stop="action.action"
                  class="text-xs"
                >
                  {{ action.label }}
                </Button>
              </div>
            </div>
          </div>
        </div>
      </ScrollArea>
      
      <!-- 通知设置 -->
      <DropdownMenuSeparator />
      <DropdownMenuItem class="cursor-pointer">
        <Settings class="w-4 h-4 mr-2" />
        通知设置
      </DropdownMenuItem>
    </DropdownMenuContent>
  </DropdownMenu>
</template>
