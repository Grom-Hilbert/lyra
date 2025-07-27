<template>
  <div class="component-demo p-6 space-y-8">
    <h1 class="text-2xl font-bold mb-6">文件管理组件演示</h1>

    <!-- FileList 组件演示 -->
    <section class="space-y-4">
      <h2 class="text-xl font-semibold">FileList 组件</h2>
      <div class="bg-card border border-border rounded-lg p-4">
        <div class="mb-4 flex items-center space-x-4">
          <button
            @click="toggleViewMode"
            class="px-4 py-2 bg-primary text-primary-foreground rounded hover:bg-primary/90"
          >
            切换视图 ({{ viewMode === 'grid' ? '网格' : '列表' }})
          </button>
          <button
            @click="toggleLoading"
            class="px-4 py-2 bg-secondary text-secondary-foreground rounded hover:bg-secondary/90"
          >
            {{ loading ? '停止加载' : '开始加载' }}
          </button>
        </div>
        
        <FileList
          :files="mockFiles"
          :folders="mockFolders"
          :view-mode="viewMode"
          :loading="loading"
          :selectable="true"
          @file-select="handleFileSelect"
          @file-open="handleFileOpen"
          @file-download="handleFileDownload"
          @file-delete="handleFileDelete"
          @file-rename="handleFileRename"
          @folder-open="handleFolderOpen"
          @folder-delete="handleFolderDelete"
          @folder-rename="handleFolderRename"
          @selection-change="handleSelectionChange"
          @batch-download="handleBatchDownload"
          @batch-delete="handleBatchDelete"
          @view-mode-change="handleViewModeChange"
          @sort-change="handleSortChange"
        />
      </div>
    </section>

    <!-- UploadProgress 组件演示 -->
    <section class="space-y-4">
      <h2 class="text-xl font-semibold">UploadProgress 组件</h2>
      <div class="bg-card border border-border rounded-lg p-4">
        <div class="mb-4 flex items-center space-x-4">
          <button
            @click="toggleUploadProgress"
            class="px-4 py-2 bg-primary text-primary-foreground rounded hover:bg-primary/90"
          >
            {{ showUploadProgress ? '隐藏' : '显示' }}上传进度
          </button>
          <button
            @click="addMockUploadTask"
            class="px-4 py-2 bg-secondary text-secondary-foreground rounded hover:bg-secondary/90"
          >
            添加上传任务
          </button>
          <button
            @click="clearUploadTasks"
            class="px-4 py-2 bg-destructive text-destructive-foreground rounded hover:bg-destructive/90"
          >
            清空任务
          </button>
        </div>
        
        <UploadProgress
          :upload-tasks="uploadTasks"
          :visible="showUploadProgress"
          position="bottom-right"
          @task-pause="handleTaskPause"
          @task-resume="handleTaskResume"
          @task-cancel="handleTaskCancel"
          @task-retry="handleTaskRetry"
          @clear-completed="handleClearCompleted"
          @clear-all="handleClearAll"
          @close="handleUploadProgressClose"
        />
      </div>
    </section>

    <!-- FolderTree 组件演示 -->
    <section class="space-y-4">
      <h2 class="text-xl font-semibold">FolderTree 组件</h2>
      <div class="bg-card border border-border rounded-lg p-4">
        <div class="max-w-sm">
          <FolderTree
            :space-id="1"
            :current-folder-id="currentFolderId"
            @folder-select="handleFolderSelect"
            @folder-create="handleFolderCreate"
            @folder-rename="handleFolderRename"
            @folder-delete="handleFolderDelete"
            @folder-move="handleFolderMove"
          />
        </div>
      </div>
    </section>

    <!-- 事件日志 -->
    <section class="space-y-4">
      <h2 class="text-xl font-semibold">事件日志</h2>
      <div class="bg-card border border-border rounded-lg p-4">
        <div class="max-h-64 overflow-y-auto space-y-2">
          <div
            v-for="(log, index) in eventLogs"
            :key="index"
            class="text-sm p-2 bg-muted rounded"
          >
            <span class="font-mono text-xs text-muted-foreground">{{ log.timestamp }}</span>
            <span class="ml-2">{{ log.message }}</span>
          </div>
          <div v-if="eventLogs.length === 0" class="text-muted-foreground text-center py-4">
            暂无事件日志
          </div>
        </div>
        <button
          @click="clearEventLogs"
          class="mt-4 px-4 py-2 bg-secondary text-secondary-foreground rounded hover:bg-secondary/90"
        >
          清空日志
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import FileList from '@/components/FileList.vue'
import UploadProgress from '@/components/UploadProgress.vue'
import FolderTree from '@/components/FolderTree.vue'
import type { IFileInfo, IFolderInfo } from '@/types/index'
import type { UploadTask } from '@/components/UploadProgress.vue'

// 响应式数据
const viewMode = ref<'grid' | 'list'>('grid')
const loading = ref(false)
const showUploadProgress = ref(true)
const currentFolderId = ref<number | null>(null)
const eventLogs = ref<Array<{ timestamp: string, message: string }>>([])

// Mock 数据
const mockFiles: IFileInfo[] = [
  {
    id: 1,
    filename: 'document.pdf',
    originalName: 'document.pdf',
    path: '/document.pdf',
    sizeBytes: 1024 * 1024,
    mimeType: 'application/pdf',
    fileHash: 'hash1',
    version: 1,
    isPublic: false,
    downloadCount: 5,
    spaceId: 1,
    uploaderId: 1,
    status: 'ACTIVE',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z'
  },
  {
    id: 2,
    filename: 'image.jpg',
    originalName: 'image.jpg',
    path: '/image.jpg',
    sizeBytes: 2 * 1024 * 1024,
    mimeType: 'image/jpeg',
    fileHash: 'hash2',
    version: 1,
    isPublic: false,
    downloadCount: 10,
    spaceId: 1,
    uploaderId: 1,
    status: 'ACTIVE',
    createdAt: '2024-01-02T00:00:00Z',
    updatedAt: '2024-01-02T00:00:00Z'
  },
  {
    id: 3,
    filename: 'spreadsheet.xlsx',
    originalName: 'spreadsheet.xlsx',
    path: '/spreadsheet.xlsx',
    sizeBytes: 512 * 1024,
    mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    fileHash: 'hash3',
    version: 1,
    isPublic: false,
    downloadCount: 2,
    spaceId: 1,
    uploaderId: 1,
    status: 'ACTIVE',
    createdAt: '2024-01-03T00:00:00Z',
    updatedAt: '2024-01-03T00:00:00Z'
  }
]

const mockFolders: IFolderInfo[] = [
  {
    id: 1,
    name: 'Documents',
    path: '/Documents',
    spaceId: 1,
    level: 1,
    isRoot: false,
    fileCount: 5,
    sizeBytes: 10 * 1024 * 1024,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z'
  },
  {
    id: 2,
    name: 'Images',
    path: '/Images',
    spaceId: 1,
    level: 1,
    isRoot: false,
    fileCount: 12,
    sizeBytes: 50 * 1024 * 1024,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z'
  }
]

const uploadTasks = ref<UploadTask[]>([
  {
    id: '1',
    filename: 'example.pdf',
    size: 1024 * 1024,
    progress: 75,
    status: 'uploading',
    speed: 1024 * 100
  },
  {
    id: '2',
    filename: 'photo.jpg',
    size: 2 * 1024 * 1024,
    progress: 100,
    status: 'success'
  },
  {
    id: '3',
    filename: 'video.mp4',
    size: 10 * 1024 * 1024,
    progress: 30,
    status: 'error',
    error: '网络连接失败'
  }
])

// 工具函数
const addEventLog = (message: string) => {
  eventLogs.value.unshift({
    timestamp: new Date().toLocaleTimeString(),
    message
  })
  
  // 限制日志数量
  if (eventLogs.value.length > 50) {
    eventLogs.value = eventLogs.value.slice(0, 50)
  }
}

// 事件处理
const toggleViewMode = () => {
  viewMode.value = viewMode.value === 'grid' ? 'list' : 'grid'
  addEventLog(`切换视图模式: ${viewMode.value}`)
}

const toggleLoading = () => {
  loading.value = !loading.value
  addEventLog(`${loading.value ? '开始' : '停止'}加载`)
}

const toggleUploadProgress = () => {
  showUploadProgress.value = !showUploadProgress.value
  addEventLog(`${showUploadProgress.value ? '显示' : '隐藏'}上传进度`)
}

const addMockUploadTask = () => {
  const newTask: UploadTask = {
    id: Date.now().toString(),
    filename: `file-${Date.now()}.txt`,
    size: Math.random() * 1024 * 1024,
    progress: Math.random() * 100,
    status: Math.random() > 0.5 ? 'uploading' : 'pending',
    speed: Math.random() * 1024 * 100
  }
  uploadTasks.value.push(newTask)
  addEventLog(`添加上传任务: ${newTask.filename}`)
}

const clearUploadTasks = () => {
  uploadTasks.value = []
  addEventLog('清空所有上传任务')
}

const clearEventLogs = () => {
  eventLogs.value = []
}

// FileList 事件处理
const handleFileSelect = (file: IFileInfo) => {
  addEventLog(`选择文件: ${file.filename}`)
}

const handleFileOpen = (file: IFileInfo) => {
  addEventLog(`打开文件: ${file.filename}`)
}

const handleFileDownload = (file: IFileInfo) => {
  addEventLog(`下载文件: ${file.filename}`)
}

const handleFileDelete = (file: IFileInfo) => {
  addEventLog(`删除文件: ${file.filename}`)
}

const handleFileRename = (file: IFileInfo, newName: string) => {
  addEventLog(`重命名文件: ${file.filename} -> ${newName}`)
}

const handleFolderOpen = (folder: IFolderInfo) => {
  addEventLog(`打开文件夹: ${folder.name}`)
}

const handleFolderDelete = (folderId: number) => {
  addEventLog(`删除文件夹: ${folderId}`)
}

const handleFolderRename = (folderId: number, newName: string) => {
  addEventLog(`重命名文件夹: ${folderId} -> ${newName}`)
}

const handleSelectionChange = (selectedItems: any[]) => {
  addEventLog(`选择变化: ${selectedItems.length} 项`)
}

const handleBatchDownload = (selectedItems: any[]) => {
  addEventLog(`批量下载: ${selectedItems.length} 项`)
}

const handleBatchDelete = (selectedItems: any[]) => {
  addEventLog(`批量删除: ${selectedItems.length} 项`)
}

const handleViewModeChange = (mode: 'grid' | 'list') => {
  viewMode.value = mode
  addEventLog(`视图模式变化: ${mode}`)
}

const handleSortChange = (sortBy: string, direction: 'asc' | 'desc') => {
  addEventLog(`排序变化: ${sortBy} ${direction}`)
}

// UploadProgress 事件处理
const handleTaskPause = (taskId: string) => {
  addEventLog(`暂停任务: ${taskId}`)
}

const handleTaskResume = (taskId: string) => {
  addEventLog(`恢复任务: ${taskId}`)
}

const handleTaskCancel = (taskId: string) => {
  addEventLog(`取消任务: ${taskId}`)
  uploadTasks.value = uploadTasks.value.filter(task => task.id !== taskId)
}

const handleTaskRetry = (taskId: string) => {
  addEventLog(`重试任务: ${taskId}`)
}

const handleClearCompleted = () => {
  addEventLog('清除已完成任务')
  uploadTasks.value = uploadTasks.value.filter(task => task.status !== 'success')
}

const handleClearAll = () => {
  addEventLog('清除所有任务')
  uploadTasks.value = []
}

const handleUploadProgressClose = () => {
  showUploadProgress.value = false
  addEventLog('关闭上传进度面板')
}

// FolderTree 事件处理
const handleFolderSelect = (folderId: number | null) => {
  currentFolderId.value = folderId
  addEventLog(`选择文件夹: ${folderId}`)
}

const handleFolderCreate = (parentId?: number) => {
  addEventLog(`创建文件夹，父级: ${parentId}`)
}

const handleFolderMove = (folderId: number, targetFolderId: number) => {
  addEventLog(`移动文件夹: ${folderId} -> ${targetFolderId}`)
}
</script>

<style scoped>
.component-demo {
  max-width: 1200px;
  margin: 0 auto;
}
</style>
