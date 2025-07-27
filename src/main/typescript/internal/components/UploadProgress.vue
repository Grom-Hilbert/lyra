<template>
  <div
    v-if="visible && uploadTasks.length > 0"
    class="upload-progress-panel"
    :class="positionClasses"
  >
    <!-- 头部 -->
    <div class="upload-header">
      <div class="flex items-center justify-between">
        <div class="flex items-center space-x-2">
          <svg class="w-5 h-5 text-primary" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zM6.293 6.707a1 1 0 010-1.414l3-3a1 1 0 011.414 0l3 3a1 1 0 01-1.414 1.414L11 5.414V13a1 1 0 11-2 0V5.414L7.707 6.707a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
          </svg>
          <h3 class="text-sm font-medium">
            上传进度 ({{ activeTasksCount }}/{{ uploadTasks.length }})
          </h3>
        </div>
        
        <div class="flex items-center space-x-1">
          <!-- 折叠/展开按钮 -->
          <button
            @click="toggleCollapsed"
            class="p-1 hover:bg-accent rounded"
            :title="isCollapsed ? '展开' : '折叠'"
          >
            <svg
              class="w-4 h-4 transition-transform"
              :class="{ 'rotate-180': isCollapsed }"
              fill="currentColor"
              viewBox="0 0 20 20"
            >
              <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd"/>
            </svg>
          </button>
          
          <!-- 关闭按钮 -->
          <button
            @click="$emit('close')"
            class="p-1 hover:bg-accent rounded"
            title="关闭"
          >
            <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
            </svg>
          </button>
        </div>
      </div>
      
      <!-- 总体进度条 -->
      <div v-if="!isCollapsed" class="mt-3">
        <div class="flex items-center justify-between text-xs text-muted-foreground mb-1">
          <span>总进度: {{ Math.round(overallProgress) }}%</span>
          <span v-if="uploadSpeed > 0">{{ formatSpeed(uploadSpeed) }}</span>
        </div>
        <div class="w-full bg-secondary rounded-full h-2">
          <div
            class="bg-primary h-2 rounded-full transition-all duration-300"
            :style="{ width: overallProgress + '%' }"
          ></div>
        </div>
      </div>
    </div>

    <!-- 任务列表 -->
    <div v-if="!isCollapsed" class="upload-tasks">
      <div class="max-h-64 overflow-y-auto">
        <div
          v-for="task in uploadTasks"
          :key="task.id"
          class="upload-task"
          :class="getTaskStatusClass(task.status)"
        >
          <div class="flex items-center space-x-3">
            <!-- 文件图标 -->
            <div class="flex-shrink-0">
              <div v-if="task.status === 'uploading'" class="w-8 h-8 flex items-center justify-center">
                <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-primary"></div>
              </div>
              <div v-else-if="task.status === 'success'" class="w-8 h-8 flex items-center justify-center">
                <svg class="w-5 h-5 text-green-500" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                </svg>
              </div>
              <div v-else-if="task.status === 'error'" class="w-8 h-8 flex items-center justify-center">
                <svg class="w-5 h-5 text-red-500" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                </svg>
              </div>
              <div v-else-if="task.status === 'paused'" class="w-8 h-8 flex items-center justify-center">
                <svg class="w-5 h-5 text-yellow-500" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zM7 8a1 1 0 012 0v4a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v4a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd"/>
                </svg>
              </div>
              <div v-else class="w-8 h-8 flex items-center justify-center">
                <svg class="w-5 h-5 text-muted-foreground" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"/>
                </svg>
              </div>
            </div>

            <!-- 文件信息 -->
            <div class="flex-1 min-w-0">
              <div class="flex items-center justify-between">
                <p class="text-sm font-medium truncate">{{ task.filename }}</p>
                <span class="text-xs text-muted-foreground ml-2">
                  {{ formatFileSize(task.size) }}
                </span>
              </div>
              
              <!-- 进度条 -->
              <div v-if="task.status === 'uploading' || task.status === 'paused'" class="mt-1">
                <div class="flex items-center justify-between text-xs text-muted-foreground mb-1">
                  <span>{{ Math.round(task.progress) }}%</span>
                  <span v-if="task.speed && task.speed > 0">{{ formatSpeed(task.speed) }}</span>
                </div>
                <div class="w-full bg-secondary rounded-full h-1">
                  <div
                    class="h-1 rounded-full transition-all duration-300"
                    :class="{
                      'bg-primary': task.status === 'uploading',
                      'bg-yellow-500': task.status === 'paused'
                    }"
                    :style="{ width: task.progress + '%' }"
                  ></div>
                </div>
              </div>
              
              <!-- 错误信息 -->
              <div v-if="task.status === 'error' && task.error" class="mt-1">
                <p class="text-xs text-red-500">{{ task.error }}</p>
              </div>
              
              <!-- 成功信息 -->
              <div v-if="task.status === 'success'" class="mt-1">
                <p class="text-xs text-green-600">上传完成</p>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="flex-shrink-0 flex items-center space-x-1">
              <!-- 暂停/恢复按钮 -->
              <button
                v-if="task.status === 'uploading'"
                @click="$emit('task-pause', task.id)"
                class="p-1 hover:bg-accent rounded"
                title="暂停"
              >
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zM7 8a1 1 0 012 0v4a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v4a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd"/>
                </svg>
              </button>
              
              <button
                v-else-if="task.status === 'paused'"
                @click="$emit('task-resume', task.id)"
                class="p-1 hover:bg-accent rounded"
                title="恢复"
              >
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z" clip-rule="evenodd"/>
                </svg>
              </button>
              
              <!-- 重试按钮 -->
              <button
                v-if="task.status === 'error'"
                @click="$emit('task-retry', task.id)"
                class="p-1 hover:bg-accent rounded"
                title="重试"
              >
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1zm.008 9.057a1 1 0 011.276.61A5.002 5.002 0 0014.001 13H11a1 1 0 110-2h5a1 1 0 011 1v5a1 1 0 11-2 0v-2.101a7.002 7.002 0 01-11.601-2.566 1 1 0 01.61-1.276z" clip-rule="evenodd"/>
                </svg>
              </button>
              
              <!-- 取消/删除按钮 -->
              <button
                @click="$emit('task-cancel', task.id)"
                class="p-1 hover:bg-accent rounded"
                :title="task.status === 'success' ? '移除' : '取消'"
              >
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 底部操作 -->
      <div v-if="uploadTasks.length > 0" class="upload-footer">
        <div class="flex items-center justify-between">
          <div class="text-xs text-muted-foreground">
            {{ completedTasksCount }} 已完成, {{ errorTasksCount }} 失败
          </div>
          <div class="flex items-center space-x-2">
            <button
              v-if="completedTasksCount > 0"
              @click="$emit('clear-completed')"
              data-testid="clear-completed"
              class="text-xs text-muted-foreground hover:text-foreground"
            >
              清除已完成
            </button>
            <button
              @click="$emit('clear-all')"
              data-testid="clear-all"
              class="text-xs text-muted-foreground hover:text-foreground"
            >
              清除全部
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'

export interface UploadTask {
  id: string
  filename: string
  size: number
  progress: number
  status: 'pending' | 'uploading' | 'paused' | 'success' | 'error' | 'cancelled'
  error?: string
  speed?: number
  startTime?: number
  endTime?: number
}

interface Props {
  uploadTasks: UploadTask[]
  visible?: boolean
  position?: 'bottom-right' | 'bottom-left' | 'top-right' | 'top-left'
}

interface Emits {
  (e: 'task-pause', taskId: string): void
  (e: 'task-resume', taskId: string): void
  (e: 'task-cancel', taskId: string): void
  (e: 'task-retry', taskId: string): void
  (e: 'clear-completed'): void
  (e: 'clear-all'): void
  (e: 'close'): void
}

const props = withDefaults(defineProps<Props>(), {
  visible: true,
  position: 'bottom-right'
})

const emit = defineEmits<Emits>()

// 响应式数据
const isCollapsed = ref(false)

// 计算属性
const positionClasses = computed(() => {
  const baseClasses = 'fixed z-50'

  switch (props.position) {
    case 'bottom-right':
      return `${baseClasses} bottom-4 right-4`
    case 'bottom-left':
      return `${baseClasses} bottom-4 left-4`
    case 'top-right':
      return `${baseClasses} top-4 right-4`
    case 'top-left':
      return `${baseClasses} top-4 left-4`
    default:
      return `${baseClasses} bottom-4 right-4`
  }
})

const activeTasksCount = computed(() => {
  return props.uploadTasks.filter(task =>
    task.status === 'uploading' || task.status === 'pending' || task.status === 'paused'
  ).length
})

const completedTasksCount = computed(() => {
  return props.uploadTasks.filter(task => task.status === 'success').length
})

const errorTasksCount = computed(() => {
  return props.uploadTasks.filter(task => task.status === 'error').length
})

const overallProgress = computed(() => {
  if (props.uploadTasks.length === 0) return 0

  const totalProgress = props.uploadTasks.reduce((sum, task) => {
    if (task.status === 'success') return sum + 100
    if (task.status === 'error' || task.status === 'cancelled') return sum + 0
    return sum + task.progress
  }, 0)

  return totalProgress / props.uploadTasks.length
})

const uploadSpeed = computed(() => {
  const uploadingTasks = props.uploadTasks.filter(task =>
    task.status === 'uploading' && task.speed
  )

  if (uploadingTasks.length === 0) return 0

  return uploadingTasks.reduce((sum, task) => sum + (task.speed || 0), 0)
})

// 方法
const toggleCollapsed = () => {
  isCollapsed.value = !isCollapsed.value
}

const getTaskStatusClass = (status: UploadTask['status']) => {
  switch (status) {
    case 'success':
      return 'task-success'
    case 'error':
      return 'task-error'
    case 'uploading':
      return 'task-uploading'
    case 'paused':
      return 'task-paused'
    default:
      return 'task-pending'
  }
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'

  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))

  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatSpeed = (bytesPerSecond: number): string => {
  return formatFileSize(bytesPerSecond) + '/s'
}
</script>

<style scoped>
.upload-progress-panel {
  background-color: var(--background);
  border: 1px solid var(--border);
  border-radius: 0.5rem;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  max-width: 24rem;
  width: 100%;
}

.upload-header {
  padding: 1rem;
  border-bottom: 1px solid var(--border);
}

.upload-tasks {
  max-height: 20rem;
  overflow: hidden;
}

.upload-task {
  padding: 0.75rem;
  border-bottom: 1px solid var(--border);
}

.upload-task:last-child {
  border-bottom: none;
}

.upload-task.task-success {
  background-color: #f0fdf4;
  border-color: #bbf7d0;
}

.upload-task.task-error {
  background-color: #fef2f2;
  border-color: #fecaca;
}

.upload-task.task-uploading {
  background-color: #eff6ff;
  border-color: #bfdbfe;
}

.upload-task.task-paused {
  background-color: #fffbeb;
  border-color: #fed7aa;
}

.upload-footer {
  padding: 0.75rem;
  background-color: rgba(var(--muted), 0.5);
  border-top: 1px solid var(--border);
}
</style>
