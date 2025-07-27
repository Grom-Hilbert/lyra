<template>
  <div class="file-manager h-screen bg-background flex">
    <!-- 侧边栏 -->
    <div class="w-64 bg-card border-r border-border flex flex-col">
      <!-- 侧边栏头部 -->
              <div class="p-4 border-b border-border">
        <h1 class="text-lg font-semibold text-foreground">文件管理</h1>
        <p class="text-sm text-muted-foreground mt-1">管理您的文件和文件夹</p>
      </div>

      <!-- 空间列表 -->
      <div class="flex-1 overflow-y-auto">
        <div class="p-4">
          <h2 class="text-sm font-medium text-muted-foreground mb-3">我的空间</h2>
          <div class="space-y-1">
            <div
              v-for="space in spaces"
              :key="space.id"
              class="flex items-center p-2 rounded-lg cursor-pointer transition-colors"
              :class="{
                'bg-primary/10 text-primary': currentSpaceId === space.id,
                'text-foreground hover:bg-muted/50': currentSpaceId !== space.id
              }"
              @click="selectSpace(space.id)"
            >
              <div class="flex-shrink-0 mr-3">
                <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"/>
                </svg>
              </div>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium truncate">{{ space.name }}</p>
                <p class="text-xs text-muted-foreground">{{ space.type }}</p>
              </div>
              <div v-if="(space as any).isDefault" class="flex-shrink-0">
                <span class="inline-flex items-center px-2 py-0.5 rounded text-xs font-medium bg-green-100 text-green-800">
                  默认
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- 文件夹树 -->
        <div v-if="currentSpaceId" class="px-4 pb-4">
          <h2 class="text-sm font-medium text-muted-foreground mb-3">文件夹</h2>
          <FolderTree
            :space-id="currentSpaceId"
            :current-folder-id="currentFolderId"
            @folder-select="selectFolder"
            @folder-create="() => showCreateFolderDialog = true"
            @folder-rename="handleFolderTreeRename"
            @folder-delete="handleFolderTreeDelete"
          />
        </div>
      </div>

      <!-- 存储配额 -->
      <div v-if="storageInfo" class="p-4 border-t border-border">
        <div class="text-sm text-foreground mb-2">存储空间</div>
        <div class="w-full bg-muted rounded-full h-2 mb-2">
          <div
            class="bg-primary h-2 rounded-full transition-all duration-300"
            :style="{ width: `${storageInfo.usagePercentage}%` }"
          ></div>
        </div>
        <div class="text-xs text-muted-foreground">
          {{ storageInfo.usedReadable }} / {{ storageInfo.totalReadable }}
        </div>
        <div class="text-xs text-muted-foreground mt-1">
          {{ storageInfo.fileCount }} 个文件，{{ storageInfo.folderCount }} 个文件夹
        </div>
      </div>
    </div>

    <!-- 主内容区域 -->
    <div class="flex-1 flex flex-col">
      <!-- 顶部工具栏 -->
      <div class="bg-card border-b border-border p-4">
        <!-- 面包屑导航 -->
        <div class="flex items-center mb-4">
          <nav class="flex" aria-label="Breadcrumb">
            <ol class="flex items-center space-x-2">
              <li>
                <button
                  class="text-sm text-muted-foreground hover:text-foreground"
                  @click="selectFolder(null)"
                >
                  {{ currentSpace?.name || '根目录' }}
                </button>
              </li>
              <li v-for="(crumb, index) in breadcrumbs" :key="crumb.id">
                <div class="flex items-center">
                  <svg class="w-4 h-4 text-muted-foreground mx-2" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 111.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
                  </svg>
                  <button
                    class="text-sm text-muted-foreground hover:text-foreground"
                    @click="selectFolder(crumb.id)"
                  >
                    {{ crumb.name }}
                  </button>
                </div>
              </li>
            </ol>
          </nav>
        </div>

        <!-- 操作按钮栏 -->
        <div class="flex items-center justify-between">
          <div class="flex space-x-3">
            <!-- 上传按钮 -->
            <button
              type="button"
              class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-primary-foreground bg-primary hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary"
              @click="showUploadDialog = true"
            >
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
              </svg>
              上传文件
            </button>

            <!-- 新建文件夹按钮 -->
            <button
              type="button"
              class="inline-flex items-center px-4 py-2 border border-border text-sm font-medium rounded-md text-foreground bg-background hover:bg-muted focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary"
              @click="showCreateFolderDialog = true"
            >
              <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/>
              </svg>
              新建文件夹
            </button>

            <!-- 批量操作按钮 -->
            <div v-if="selectedItems.length > 0" class="flex space-x-2">
              <button
                type="button"
                class="inline-flex items-center px-3 py-2 border border-border text-sm font-medium rounded-md text-foreground bg-background hover:bg-muted"
                @click="batchDownload"
              >
                <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                </svg>
                下载 ({{ selectedItems.length }})
              </button>
              <button
                type="button"
                class="inline-flex items-center px-3 py-2 border border-border text-sm font-medium rounded-md text-foreground bg-background hover:bg-muted"
                @click="handleBatchMove"
              >
                <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4"/>
                </svg>
                移动 ({{ selectedItems.length }})
              </button>
              <button
                type="button"
                class="inline-flex items-center px-3 py-2 border border-border text-sm font-medium rounded-md text-foreground bg-background hover:bg-muted"
                @click="handleBatchCopy"
              >
                <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"/>
                </svg>
                复制 ({{ selectedItems.length }})
              </button>
              <button
                type="button"
                class="inline-flex items-center px-3 py-2 border border-destructive text-sm font-medium rounded-md text-destructive bg-background hover:bg-destructive/5"
                @click="batchDelete"
              >
                <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                </svg>
                删除 ({{ selectedItems.length }})
              </button>
            </div>
          </div>

          <div class="flex items-center space-x-3">
            <!-- 搜索框 -->
            <SearchBox
              :space-id="currentSpaceId || undefined"
              @search="handleQuickSearch"
              @clear="clearSearch"
            />

            <!-- 视图切换 -->
            <div class="flex border border-border rounded-md">
              <button
                type="button"
                class="p-2 text-sm font-medium"
                :class="{
                  'bg-primary/10 text-primary': viewMode === 'grid',
                  'text-muted-foreground hover:text-foreground': viewMode !== 'grid'
                }"
                @click="viewMode = 'grid'"
              >
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M5 3a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2V5a2 2 0 00-2-2H5zM5 11a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2v-2a2 2 0 00-2-2H5zM11 5a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V5zM11 13a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/>
                </svg>
              </button>
              <button
                type="button"
                class="p-2 text-sm font-medium border-l border-border"
                :class="{
                  'bg-primary/10 text-primary': viewMode === 'list',
                  'text-muted-foreground hover:text-foreground': viewMode !== 'list'
                }"
                @click="viewMode = 'list'"
              >
                <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M3 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z" clip-rule="evenodd"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 文件列表内容区域 -->
      <div class="flex-1 overflow-auto p-4">
        <!-- 加载状态 -->
        <div v-if="loading" class="flex items-center justify-center h-64">
          <div class="text-center">
            <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary mx-auto mb-4"></div>
            <p class="text-muted-foreground">加载中...</p>
          </div>
        </div>

        <!-- 空状态 -->
                  <div v-else-if="!loading && files.length === 0 && folders.length === 0" class="text-center py-12">
          <svg class="mx-auto h-12 w-12 text-muted-foreground mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"/>
          </svg>
          <h3 class="text-lg font-medium text-foreground mb-2">此文件夹为空</h3>
          <p class="text-muted-foreground mb-6">开始上传文件或创建新文件夹</p>
          <div class="flex justify-center space-x-3">
            <button
              type="button"
              class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-primary-foreground bg-primary hover:bg-primary/90"
              @click="showUploadDialog = true"
            >
              上传文件
            </button>
            <button
              type="button"
              class="inline-flex items-center px-4 py-2 border border-border text-sm font-medium rounded-md text-foreground bg-background hover:bg-muted"
              @click="showCreateFolderDialog = true"
            >
              新建文件夹
            </button>
          </div>
        </div>

        <!-- 文件和文件夹列表 -->
        <div v-else>
          <FileList
            :files="files"
            :folders="folders"
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
      </div>
    </div>

    <!-- 上传对话框 -->
    <div v-if="showUploadDialog" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
      <div class="bg-card rounded-lg shadow-xl max-w-4xl w-full mx-4 max-h-[90vh] overflow-y-auto border border-border">
        <div class="p-6">
          <div class="flex items-center justify-between mb-6">
            <h2 class="text-lg font-semibold text-foreground">上传文件</h2>
            <button
              type="button"
              class="text-muted-foreground hover:text-foreground"
              @click="showUploadDialog = false"
            >
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>

          <FileUpload
            v-if="currentSpaceId"
            :space-id="currentSpaceId"
            :folder-id="currentFolderId"
            @upload-success="handleUploadSuccess"
            @upload-error="handleUploadError"
          />
        </div>
      </div>
    </div>

    <!-- 新建文件夹对话框 -->
    <CreateFolderDialog
      v-model:open="showCreateFolderDialog"
      :space-id="currentSpaceId || 0"
      :parent-folder-id="currentFolderId"
      @success="handleCreateFolderSuccess"
      @error="handleCreateFolderError"
    />

    <!-- 文件操作对话框 -->
    <FileOperationDialog
      v-model:open="showFileOperationDialog"
      :operation="fileOperation"
      :item="selectedFileItem"
      :item-type="selectedItemType"
      :current-space-id="currentSpaceId || 0"
      @success="handleFileOperationSuccess"
      @error="handleFileOperationError"
    />

    <!-- 右键菜单 -->
    <ContextMenu
      :visible="showContextMenu"
      :x="contextMenuX"
      :y="contextMenuY"
      :item="contextMenuItem"
      :item-type="contextMenuType"
      @action="handleContextMenuAction"
      @close="hideContextMenu"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fileApi, folderApi, spaceApi } from '@/apis'
import FileUpload from '@/components/FileUpload.vue'
import FolderTree from '@/components/FolderTree.vue'
import FileList from '@/components/FileList.vue'
import CreateFolderDialog from '@/components/CreateFolderDialog.vue'
import FileOperationDialog from '@/components/FileOperationDialog.vue'
import ContextMenu from '@/components/ContextMenu.vue'
import SearchBox from '@/components/SearchBox.vue'
import type {
  IFileInfo,
  IFolderInfo,
  ISpace
} from '@/types/index'

// 路由
const route = useRoute()
const router = useRouter()

// 响应式数据
const loading = ref(false)
const spaces = ref<ISpace[]>([])
const files = ref<IFileInfo[]>([])
const folders = ref<IFolderInfo[]>([])
const breadcrumbs = ref<Array<{ id: number; name: string; path: string }>>([])
const selectedItems = ref<Array<{ id: number; type: 'file' | 'folder' }>>([])
const searchQuery = ref('')
const viewMode = ref<'grid' | 'list'>('grid')
const showUploadDialog = ref(false)
const showCreateFolderDialog = ref(false)
const newFolderName = ref('')
const isSearchMode = ref(false)
const searchResults = ref<IFileInfo[]>([])
const storageInfo = ref<any>(null)

// 文件操作相关
const showFileOperationDialog = ref(false)
const fileOperation = ref<'move' | 'copy' | 'share' | 'rename'>('move')
const selectedFileItem = ref<IFileInfo | IFolderInfo | null>(null)
const selectedItemType = ref<'file' | 'folder'>('file')

// 右键菜单相关
const showContextMenu = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const contextMenuItem = ref<IFileInfo | IFolderInfo | null>(null)
const contextMenuType = ref<'file' | 'folder' | 'empty'>('empty')

// 当前状态
const currentSpaceId = ref<number | null>(null)
const currentFolderId = ref<number | undefined>(undefined)

// 计算属性
const currentSpace = computed(() => {
  return spaces.value.find(space => space.id === currentSpaceId.value)
})

const isAllSelected = computed(() => {
  const totalItems = files.value.length + folders.value.length
  return totalItems > 0 && selectedItems.value.length === totalItems
})

// 生命周期
onMounted(async () => {
  await loadSpaces()
  await handleRouteChange()
})

// 方法
const handleRouteChange = async () => {
  const spaceId = route.params.spaceId ? Number(route.params.spaceId) : null
  const folderId = route.params.folderId ? Number(route.params.folderId) : undefined

  if (spaceId && spaceId !== currentSpaceId.value) {
    currentSpaceId.value = spaceId
  }

  if (folderId !== currentFolderId.value) {
    currentFolderId.value = folderId
    await loadFolderContent()
  }
}

// 监听路由变化
watch(() => route.params, handleRouteChange)

const loadSpaces = async () => {
  try {
    const spacesResponse = await spaceApi.getUserSpaces()
    if (spacesResponse.success && spacesResponse.data) {
      spaces.value = spacesResponse.data
    }
    if (spaces.value.length > 0 && !currentSpaceId.value) {
      // 选择默认空间或第一个空间
      const defaultSpace = spaces.value.find((s: any) => s.isDefault) || spaces.value[0]
      currentSpaceId.value = defaultSpace.id
      // 加载存储配额信息
      await loadStorageInfo()
    }
  } catch (error) {
    console.error('Failed to load spaces:', error)
  }
}

const loadStorageInfo = async () => {
  if (!currentSpaceId.value) return

  try {
    const quotaResponse = await spaceApi.getSpaceQuota(currentSpaceId.value)
    if (quotaResponse.success && quotaResponse.data) {
      storageInfo.value = quotaResponse.data
    }
  } catch (error) {
    console.error('Failed to load storage info:', error)
    // 如果API不存在，使用默认值
    storageInfo.value = {
      used: 0,
      total: 1024 * 1024 * 1024, // 1GB
      usedReadable: '0 B',
      totalReadable: '1 GB',
      usagePercentage: 0,
      fileCount: 0,
      folderCount: 0
    }
  }
}

const selectSpace = async (spaceId: number) => {
  currentSpaceId.value = spaceId
  currentFolderId.value = undefined
  router.push(`/files/${spaceId}`)
  await loadStorageInfo()
  await loadFolderContent()
}

const selectFolder = async (folderId: number | null) => {
  currentFolderId.value = folderId || undefined
  const path = folderId ? `/files/${currentSpaceId.value}/${folderId}` : `/files/${currentSpaceId.value}`
  router.push(path)
  await loadFolderContent()
}

const buildBreadcrumbs = async () => {
  breadcrumbs.value = []

  if (!currentFolderId.value) {
    return
  }

  try {
    // 获取当前文件夹信息
    const folderResponse = await folderApi.getFolderDetail(currentFolderId.value)
    if (folderResponse.success && folderResponse.data) {
      const folder = folderResponse.data

      // 构建面包屑路径
      const pathParts = folder.path.split('/').filter(part => part.length > 0)
      const crumbs: Array<{ id: number; name: string; path: string }> = []

      // 获取路径中每个文件夹的信息
      let currentPath = ''
      for (const part of pathParts) {
        currentPath += '/' + part

        // 查找对应的文件夹
        const pathFolders = await folderApi.getFolders({
          spaceId: currentSpaceId.value!,
          parentId: undefined
        })

        if (pathFolders.success && pathFolders.data) {
          const pathFolder = pathFolders.data.find(f => f.path === currentPath)
          if (pathFolder) {
            crumbs.push({
              id: pathFolder.id,
              name: pathFolder.name,
              path: currentPath
            })
          }
        }
      }

      breadcrumbs.value = crumbs
    }
  } catch (error) {
    console.error('Failed to build breadcrumbs:', error)
  }
}

const loadFolderContent = async () => {
  if (!currentSpaceId.value) return

  loading.value = true
  selectedItems.value = []

  try {
    // 加载文件夹列表
    const foldersResponse = await folderApi.getFolders({
      spaceId: currentSpaceId.value,
      parentId: currentFolderId.value
    })
    if (foldersResponse.success && foldersResponse.data) {
      folders.value = foldersResponse.data || []
    }

    // 加载文件列表
    const filesResponse = await fileApi.getFilesBySpace({
      spaceId: currentSpaceId.value,
      folderId: currentFolderId.value,
      page: 0,
      size: 100
    })
    if (filesResponse.success && filesResponse.data) {
      files.value = filesResponse.data.content || []
    }

    // 设置面包屑导航
    await buildBreadcrumbs()
  } catch (error) {
    console.error('Failed to load folder content:', error)
  } finally {
    loading.value = false
  }
}

const handleFileClick = async (file: IFileInfo) => {
  // 处理文件点击事件，可以预览或下载
  try {
    // 检查文件类型是否支持预览
    const supportedPreviewTypes = ['image/', 'text/', 'application/pdf']
    const canPreview = supportedPreviewTypes.some(type => file.mimeType.startsWith(type))

    if (canPreview) {
      // 获取预览URL并打开
      const previewUrl = fileApi.getPreviewUrl(file.id)
      window.open(previewUrl, '_blank')
    } else {
      // 直接下载文件
      const downloadUrl = fileApi.getDownloadUrl(file.id)
      window.open(downloadUrl, '_blank')
    }
  } catch (error) {
    console.error('Failed to open file:', error)
    // 直接使用API端点下载
    window.open(`/api/files/${file.id}/download`, '_blank')
  }
}

const isSelected = (id: number, type: 'file' | 'folder'): boolean => {
  return selectedItems.value.some(item => item.id === id && item.type === type)
}

const toggleSelection = (id: number, type: 'file' | 'folder') => {
  const index = selectedItems.value.findIndex(item => item.id === id && item.type === type)
  if (index > -1) {
    selectedItems.value.splice(index, 1)
  } else {
    selectedItems.value.push({ id, type })
  }
}

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedItems.value = []
  } else {
    selectedItems.value = [
      ...files.value.map(f => ({ id: f.id, type: 'file' as const })),
      ...folders.value.map(f => ({ id: f.id, type: 'folder' as const }))
    ]
  }
}

const handleCreateFolder = () => {
  const name = prompt('请输入文件夹名称：')
  if (name && name.trim()) {
    createFolder(name.trim())
  }
}

const createFolder = async (name: string) => {
  if (!currentSpaceId.value) return

  try {
    await folderApi.createFolder({
      name,
      spaceId: currentSpaceId.value,
      parentFolderId: currentFolderId.value
    })
    await loadFolderContent()
  } catch (error) {
    console.error('Failed to create folder:', error)
    alert('创建文件夹失败')
  }
}

const handleUploadSuccess = (_uploadedFiles: IFileInfo[]) => {
  showUploadDialog.value = false
  loadFolderContent()
}

const handleUploadError = (error: string) => {
  alert(`上传失败：${error}`)
}

const handleCreateFolderSuccess = (folder: any) => {
  console.log('文件夹创建成功:', folder)
  loadFolderContent()
}

const handleCreateFolderError = (error: string) => {
  alert(`创建文件夹失败：${error}`)
}

// 文件操作处理函数
const openFileOperation = (operation: 'move' | 'copy' | 'share' | 'rename', item: IFileInfo | IFolderInfo, type: 'file' | 'folder') => {
  fileOperation.value = operation
  selectedFileItem.value = item
  selectedItemType.value = type
  showFileOperationDialog.value = true
}

const handleFileOperationSuccess = (result: any) => {
  console.log('文件操作成功:', result)
  if (fileOperation.value === 'share') {
    // 显示分享链接
    alert(`分享链接已创建：${result.shareUrl}`)
  }
  loadFolderContent()
}

const handleFileOperationError = (error: string) => {
  alert(`操作失败：${error}`)
}

const batchDownload = () => {
  const fileItems = selectedItems.value.filter(item => item.type === 'file')
  if (fileItems.length === 0) {
    alert('请选择要下载的文件')
    return
  }

  fileItems.forEach(item => {
    const file = files.value.find(f => f.id === item.id)
    if (file) {
      const downloadUrl = fileApi.getDownloadUrl(file.id)
      // 使用延迟下载避免浏览器阻止多个下载
      setTimeout(() => {
        window.open(downloadUrl, '_blank')
      }, 100 * fileItems.indexOf(item))
    }
  })

  // 清除选择
  selectedItems.value = []
}

const batchDelete = async () => {
  if (selectedItems.value.length === 0) {
    alert('请选择要删除的项目')
    return
  }

  const fileCount = selectedItems.value.filter(item => item.type === 'file').length
  const folderCount = selectedItems.value.filter(item => item.type === 'folder').length
  const message = `确定要删除选中的 ${fileCount} 个文件和 ${folderCount} 个文件夹吗？此操作不可撤销。`

  if (!confirm(message)) return

  const fileIds = selectedItems.value.filter(item => item.type === 'file').map(item => item.id)
  const folderIds = selectedItems.value.filter(item => item.type === 'folder').map(item => item.id)

  try {
    let successCount = 0
    let errorCount = 0

    // 批量删除文件
    if (fileIds.length > 0) {
      try {
        await fileApi.batchDeleteFiles({ fileIds })
        successCount += fileIds.length
      } catch (error) {
        console.error('批量删除文件失败:', error)
        errorCount += fileIds.length
      }
    }

    // 逐个删除文件夹
    if (folderIds.length > 0) {
      for (const folderId of folderIds) {
        try {
          await folderApi.deleteFolder(folderId)
          successCount++
        } catch (error) {
          console.error(`删除文件夹 ${folderId} 失败:`, error)
          errorCount++
        }
      }
    }

    // 显示结果
    if (errorCount === 0) {
      alert(`成功删除 ${successCount} 个项目`)
    } else {
      alert(`删除完成：成功 ${successCount} 个，失败 ${errorCount} 个`)
    }

    selectedItems.value = []
    await loadFolderContent()
  } catch (error) {
    console.error('批量删除失败:', error)
    alert('删除操作失败')
  }
}

const openContextMenu = (event: MouseEvent, item: any, type: 'file' | 'folder') => {
  event.preventDefault()
  contextMenuX.value = event.clientX
  contextMenuY.value = event.clientY
  contextMenuItem.value = item
  contextMenuType.value = type
  showContextMenu.value = true
}

const hideContextMenu = () => {
  showContextMenu.value = false
  contextMenuItem.value = null
}

const handleContextMenuAction = (action: string, item?: IFileInfo | IFolderInfo | null) => {
  switch (action) {
    case 'open':
      if (item) {
        if (contextMenuType.value === 'file') {
          handleFileOpen(item as IFileInfo)
        } else {
          handleFolderOpen(item as IFolderInfo)
        }
      }
      break
    case 'download':
      if (item && contextMenuType.value === 'file') {
        handleFileDownload(item as IFileInfo)
      }
      break
    case 'rename':
      if (item && contextMenuType.value !== 'empty') {
        openFileOperation('rename', item, contextMenuType.value as 'file' | 'folder')
      }
      break
    case 'move':
      if (item && contextMenuType.value !== 'empty') {
        openFileOperation('move', item, contextMenuType.value as 'file' | 'folder')
      }
      break
    case 'copy':
      if (item && contextMenuType.value === 'file') {
        openFileOperation('copy', item, 'file')
      }
      break
    case 'share':
      if (item && contextMenuType.value !== 'empty') {
        openFileOperation('share', item, contextMenuType.value as 'file' | 'folder')
      }
      break
    case 'delete':
      if (item) {
        if (contextMenuType.value === 'file') {
          handleFileDelete(item as IFileInfo)
        } else {
          handleFolderDelete(item as IFolderInfo)
        }
      }
      break
    case 'upload':
      showUploadDialog.value = true
      break
    case 'createFolder':
      showCreateFolderDialog.value = true
      break
    case 'refresh':
      loadFolderContent()
      break
  }
}

// 快速搜索处理（由SearchBox组件触发）
const handleQuickSearch = (query: string) => {
  // 导航到搜索页面
  router.push({
    name: 'Search',
    query: {
      q: query,
      spaceId: currentSpaceId.value?.toString()
    }
  })
}

// 原有的搜索方法（保留用于兼容性）
const handleSearch = async () => {
  if (!searchQuery.value.trim() || !currentSpaceId.value) return

  loading.value = true
  try {
    const searchResults = await fileApi.searchFiles({
      query: searchQuery.value,
      spaceId: currentSpaceId.value,
      page: 0,
      size: 100
    })
    if (searchResults.success && searchResults.data) {
      files.value = searchResults.data.content || []
    }
    folders.value = [] // 搜索结果只显示文件
  } catch (error) {
    console.error('Search failed:', error)
  } finally {
    loading.value = false
  }
}

const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}



// 新增功能方法
const refreshContent = async () => {
  await loadFolderContent()
}

const handleSpaceChange = async () => {
  currentFolderId.value = undefined
  await loadFolderContent()
}

const navigateToFolder = async (folderId: number | null) => {
  currentFolderId.value = folderId || undefined
  await loadFolderContent()
}

const handleSearchInput = () => {
  // 实时搜索建议可以在这里实现
}

const clearSearch = () => {
  searchQuery.value = ''
  isSearchMode.value = false
  // 如果当前在搜索页面，返回文件管理页面
  if (route.name === 'Search') {
    router.push({
      name: 'Files',
      params: currentSpaceId.value ? { spaceId: currentSpaceId.value.toString() } : {}
    })
  } else {
    loadFolderContent()
  }
}

const selectAll = () => {
  if (isAllSelected.value) {
    selectedItems.value = []
  } else {
    selectedItems.value = [
      ...files.value.map(f => ({ id: f.id, type: 'file' as const })),
      ...folders.value.map(f => ({ id: f.id, type: 'folder' as const }))
    ]
  }
}

const clearSelection = () => {
  selectedItems.value = []
}

const handleBatchMove = () => {
  if (selectedItems.value.length === 0) {
    alert('请选择要移动的项目')
    return
  }

  // 打开批量移动对话框
  openBatchOperation('move')
}

const handleBatchCopy = () => {
  if (selectedItems.value.length === 0) {
    alert('请选择要复制的项目')
    return
  }

  // 打开批量复制对话框
  openBatchOperation('copy')
}

const openBatchOperation = (operation: 'move' | 'copy') => {
  // 这里可以创建一个专门的批量操作对话框
  // 暂时使用简单的实现
  const targetSpaceId = prompt('请输入目标空间ID:')
  const targetFolderId = prompt('请输入目标文件夹ID (留空表示根目录):')

  if (targetSpaceId) {
    if (operation === 'move') {
      batchMoveItems(parseInt(targetSpaceId), targetFolderId ? parseInt(targetFolderId) : undefined)
    } else {
      batchCopyItems(parseInt(targetSpaceId), targetFolderId ? parseInt(targetFolderId) : undefined)
    }
  }
}

const batchMoveItems = async (targetSpaceId: number, targetFolderId?: number) => {
  const fileIds = selectedItems.value.filter(item => item.type === 'file').map(item => item.id)
  const folderIds = selectedItems.value.filter(item => item.type === 'folder').map(item => item.id)

  try {
    let successCount = 0
    let errorCount = 0

    // 批量移动文件
    if (fileIds.length > 0) {
      try {
        await fileApi.batchMoveFiles({
          fileIds,
          targetSpaceId,
          targetFolderId
        })
        successCount += fileIds.length
      } catch (error) {
        console.error('批量移动文件失败:', error)
        errorCount += fileIds.length
      }
    }

    // 逐个移动文件夹
    if (folderIds.length > 0) {
      for (const folderId of folderIds) {
        try {
          await folderApi.moveFolder(folderId, {
            targetSpaceId,
            targetParentFolderId: targetFolderId
          })
          successCount++
        } catch (error) {
          console.error(`移动文件夹 ${folderId} 失败:`, error)
          errorCount++
        }
      }
    }

    // 显示结果
    if (errorCount === 0) {
      alert(`成功移动 ${successCount} 个项目`)
    } else {
      alert(`移动完成：成功 ${successCount} 个，失败 ${errorCount} 个`)
    }

    selectedItems.value = []
    await loadFolderContent()
  } catch (error) {
    console.error('批量移动失败:', error)
    alert('移动操作失败')
  }
}

const batchCopyItems = async (targetSpaceId: number, targetFolderId?: number) => {
  const fileIds = selectedItems.value.filter(item => item.type === 'file').map(item => item.id)

  if (fileIds.length === 0) {
    alert('当前只支持批量复制文件')
    return
  }

  try {
    await fileApi.batchCopyFiles({
      fileIds,
      targetSpaceId,
      targetFolderId
    })

    alert(`成功复制 ${fileIds.length} 个文件`)
    selectedItems.value = []
    await loadFolderContent()
  } catch (error) {
    console.error('批量复制失败:', error)
    alert('复制操作失败')
  }
}

// 权限检查
const canUpload = computed(() => {
  // TODO: 根据当前空间权限检查
  return currentSpaceId.value !== null
})

const canCreateFolder = computed(() => {
  // TODO: 根据当前空间权限检查
  return currentSpaceId.value !== null
})

// 文件属性计算函数
const getFileName = (file: IFileInfo): string => {
  return file.filename || file.originalName
}

const getFileExtension = (file: IFileInfo): string => {
  const name = getFileName(file)
  return name.split('.').pop()?.toLowerCase() || ''
}

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const getFileSizeReadable = (file: IFileInfo): string => {
  return formatFileSize(file.sizeBytes)
}

const getThumbnailUrl = (file: IFileInfo): string | null => {
  // 检查是否是图片文件
  if (file.mimeType.startsWith('image/')) {
    return `/api/files/${file.id}/thumbnail`
  }
  return null
}

// FileList组件事件处理
const handleFileSelect = (file: IFileInfo) => {
  console.log('File selected:', file)
}

const handleFileOpen = (file: IFileInfo) => {
  // TODO: 实现文件预览功能
  console.log('Open file:', file)
}

const handleFileDownload = (file: IFileInfo) => {
  const downloadUrl = fileApi.getDownloadUrl(file.id)
  window.open(downloadUrl, '_blank')
}

const handleFileDelete = async (file: IFileInfo) => {
  try {
    const response = await fileApi.deleteFile(file.id)
    if (response.success) {
      await loadFolderContent()
    }
  } catch (error) {
    console.error('Failed to delete file:', error)
  }
}

const handleFileRename = async (file: IFileInfo, newName: string) => {
  try {
    const response = await fileApi.updateFile(file.id, { filename: newName })
    if (response.success) {
      await loadFolderContent()
    }
  } catch (error) {
    console.error('Failed to rename file:', error)
  }
}

const handleFolderOpen = (folder: IFolderInfo) => {
  selectFolder(folder.id)
}

const handleFolderDelete = async (folder: IFolderInfo) => {
  try {
    const response = await folderApi.deleteFolder(folder.id)
    if (response.success) {
      await loadFolderContent()
    }
  } catch (error) {
    console.error('Failed to delete folder:', error)
  }
}

const handleFolderRename = async (folder: IFolderInfo, newName: string) => {
  try {
    const response = await folderApi.updateFolder(folder.id, { name: newName })
    if (response.success) {
      await loadFolderContent()
    }
  } catch (error) {
    console.error('Failed to rename folder:', error)
  }
}

const handleSelectionChange = (selectedItems: Array<{ type: 'file' | 'folder', id: number, item: IFileInfo | IFolderInfo }>) => {
  // 更新选择状态
  console.log('Selection changed:', selectedItems)
}

const handleBatchDownload = (selectedItems: Array<{ type: 'file' | 'folder', id: number }>) => {
  // TODO: 实现批量下载
  console.log('Batch download:', selectedItems)
}

const handleBatchDelete = async (selectedItems: Array<{ type: 'file' | 'folder', id: number }>) => {
  // TODO: 实现批量删除
  console.log('Batch delete:', selectedItems)
}

const handleViewModeChange = (mode: 'grid' | 'list') => {
  viewMode.value = mode
}

const handleSortChange = (sortBy: string, direction: 'asc' | 'desc') => {
  // TODO: 实现排序功能
  console.log('Sort change:', sortBy, direction)
}

// FolderTree组件的适配器函数
const handleFolderTreeRename = async (folderId: number, newName: string) => {
  try {
    const response = await folderApi.updateFolder(folderId, { name: newName })
    if (response.success) {
      await loadFolderContent()
    }
  } catch (error) {
    console.error('Failed to rename folder:', error)
  }
}

const handleFolderTreeDelete = async (folderId: number) => {
  try {
    const response = await folderApi.deleteFolder(folderId)
    if (response.success) {
      await loadFolderContent()
    }
  } catch (error) {
    console.error('Failed to delete folder:', error)
  }
}

</script>

<style scoped>
.file-manager {
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
}
</style> 