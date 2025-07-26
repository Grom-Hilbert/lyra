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
              <div v-if="space.isDefault" class="flex-shrink-0">
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
            @folder-create="showCreateFolderDialog"
            @folder-rename="handleFolderRename"
            @folder-delete="handleFolderDelete"
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
              @click="showCreateFolderDialog"
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
            <div class="relative">
              <input
                v-model="searchQuery"
                type="text"
                placeholder="搜索文件和文件夹..."
                class="w-64 pl-10 pr-4 py-2 border border-border rounded-md bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
                @keyup.enter="handleSearch"
              />
              <svg class="absolute left-3 top-2.5 w-4 h-4 text-muted-foreground" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
              </svg>
            </div>

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
              @click="showCreateFolderDialog"
            >
              新建文件夹
            </button>
          </div>
        </div>

        <!-- 文件和文件夹列表 -->
        <div v-else>
          <!-- 网格视图 -->
          <div v-if="viewMode === 'grid'" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4">
            <!-- 文件夹 -->
            <div
              v-for="folder in folders"
              :key="`folder-${folder.id}`"
              class="relative group cursor-pointer"
              @click="selectFolder(folder.id)"
              @contextmenu="showContextMenu($event, folder, 'folder')"
            >
              <div class="flex flex-col items-center p-4 rounded-lg border-2 border-transparent hover:border-primary/20 hover:bg-primary/5 transition-all">
                <div class="flex-shrink-0 mb-3">
                  <svg class="w-12 h-12 text-primary" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"/>
                  </svg>
                </div>
                <div class="text-center w-full">
                  <p class="text-sm font-medium text-foreground truncate">{{ folder.name }}</p>
                  <p class="text-xs text-muted-foreground mt-1">{{ folder.fileCount }} 个文件</p>
                </div>
                <!-- 选择框 -->
                <div class="absolute top-2 left-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <input
                    type="checkbox"
                    class="rounded border-border text-primary focus:ring-primary"
                    :checked="isSelected(folder.id, 'folder')"
                    @click.stop="toggleSelection(folder.id, 'folder')"
                  />
                </div>
              </div>
            </div>

            <!-- 文件 -->
            <div
              v-for="file in files"
              :key="`file-${file.id}`"
              class="relative group cursor-pointer"
              @click="handleFileClick(file)"
              @contextmenu="showContextMenu($event, file, 'file')"
            >
              <div class="flex flex-col items-center p-4 rounded-lg border-2 border-transparent hover:border-primary/20 hover:bg-primary/5 transition-all">
                <div class="flex-shrink-0 mb-3">
                  <!-- 文件缩略图或图标 -->
                  <img
                    v-if="file.thumbnailUrl"
                    :src="file.thumbnailUrl"
                    :alt="file.name"
                    class="w-12 h-12 object-cover rounded-lg"
                  />
                  <div v-else class="w-12 h-12 flex items-center justify-center rounded-lg bg-muted">
                    <svg class="w-8 h-8 text-muted-foreground" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 6a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7z" clip-rule="evenodd"/>
                    </svg>
                  </div>
                </div>
                <div class="text-center w-full">
                  <p class="text-sm font-medium text-foreground truncate" :title="file.name">{{ file.name }}</p>
                  <p class="text-xs text-muted-foreground mt-1">{{ file.sizeReadable }}</p>
                </div>
                <!-- 选择框 -->
                <div class="absolute top-2 left-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <input
                    type="checkbox"
                    class="rounded border-border text-primary focus:ring-primary"
                    :checked="isSelected(file.id, 'file')"
                    @click.stop="toggleSelection(file.id, 'file')"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 列表视图 -->
          <div v-else class="bg-card rounded-lg border border-border">
            <table class="min-w-full divide-y divide-border">
              <thead class="bg-muted/50">
                <tr>
                  <th class="px-6 py-3 text-left">
                    <input
                      type="checkbox"
                      class="rounded border-border text-primary focus:ring-primary"
                      :checked="isAllSelected"
                      @change="toggleSelectAll"
                    />
                  </th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">名称</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">大小</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">修改时间</th>
                  <th class="px-6 py-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wider">操作</th>
                </tr>
              </thead>
              <tbody class="bg-card divide-y divide-border">
                <!-- 文件夹行 -->
                <tr
                  v-for="folder in folders"
                  :key="`folder-${folder.id}`"
                  class="hover:bg-muted/50 cursor-pointer"
                  @click="selectFolder(folder.id)"
                >
                  <td class="px-6 py-4 whitespace-nowrap">
                    <input
                      type="checkbox"
                      class="rounded border-border text-primary focus:ring-primary"
                      :checked="isSelected(folder.id, 'folder')"
                      @click.stop="toggleSelection(folder.id, 'folder')"
                    />
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="flex items-center">
                      <svg class="w-5 h-5 text-primary mr-3" fill="currentColor" viewBox="0 0 20 20">
                        <path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"/>
                      </svg>
                      <div>
                        <div class="text-sm font-medium text-foreground">{{ folder.name }}</div>
                        <div class="text-sm text-muted-foreground">{{ folder.fileCount }} 个文件</div>
                      </div>
                    </div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-muted-foreground">-</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-muted-foreground">{{ formatDate(folder.updatedAt) }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-muted-foreground">
                    <button
                      type="button"
                      class="text-primary hover:text-primary/80"
                      @click.stop="showContextMenu($event, folder, 'folder')"
                    >
                      更多
                    </button>
                  </td>
                </tr>

                <!-- 文件行 -->
                <tr
                  v-for="file in files"
                  :key="`file-${file.id}`"
                  class="hover:bg-muted/50 cursor-pointer"
                  @click="handleFileClick(file)"
                >
                  <td class="px-6 py-4 whitespace-nowrap">
                    <input
                      type="checkbox"
                      class="rounded border-border text-primary focus:ring-primary"
                      :checked="isSelected(file.id, 'file')"
                      @click.stop="toggleSelection(file.id, 'file')"
                    />
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="flex items-center">
                      <img
                        v-if="file.thumbnailUrl"
                        :src="file.thumbnailUrl"
                        :alt="file.name"
                        class="w-8 h-8 object-cover rounded mr-3"
                      />
                      <div v-else class="w-8 h-8 flex items-center justify-center rounded bg-muted mr-3">
                        <svg class="w-4 h-4 text-muted-foreground" fill="currentColor" viewBox="0 0 20 20">
                          <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"/>
                        </svg>
                      </div>
                      <div>
                        <div class="text-sm font-medium text-foreground" :title="file.name">{{ file.name }}</div>
                        <div class="text-sm text-muted-foreground">{{ file.extension?.toUpperCase() || 'FILE' }}</div>
                      </div>
                    </div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-muted-foreground">{{ file.sizeReadable }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-muted-foreground">{{ formatDate(file.updatedAt) }}</td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-muted-foreground">
                    <button
                      type="button"
                      class="text-primary hover:text-primary/80"
                      @click.stop="showContextMenu($event, file, 'file')"
                    >
                      更多
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
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
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fileApi, folderApi, spaceApi } from '@/apis/fileApi'
import FileUpload from '@/components/FileUpload.vue'
import FolderTree from '@/components/FolderTree.vue'
import type { FileInfo, FolderInfo, SpaceInfo } from '@/types/file'

// 路由
const route = useRoute()
const router = useRouter()

// 响应式数据
const loading = ref(false)
const spaces = ref<SpaceInfo[]>([])
const files = ref<FileInfo[]>([])
const folders = ref<FolderInfo[]>([])
const breadcrumbs = ref<Array<{ id: number; name: string; path: string }>>([])
const selectedItems = ref<Array<{ id: number; type: 'file' | 'folder' }>>([])
const searchQuery = ref('')
const viewMode = ref<'grid' | 'list'>('grid')
const showUploadDialog = ref(false)

// 当前状态
const currentSpaceId = ref<number | null>(null)
const currentFolderId = ref<number | undefined>(undefined)
const storageInfo = ref<any>(null)

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
    spaces.value = await spaceApi.getUserSpaces()
    if (spaces.value.length > 0 && !currentSpaceId.value) {
      // 选择默认空间或第一个空间
      const defaultSpace = spaces.value.find(s => s.isDefault) || spaces.value[0]
      currentSpaceId.value = defaultSpace.id
    }
  } catch (error) {
    console.error('Failed to load spaces:', error)
  }
}

const selectSpace = async (spaceId: number) => {
  currentSpaceId.value = spaceId
  currentFolderId.value = undefined
  router.push(`/files/${spaceId}`)
  await loadFolderContent()
}

const selectFolder = async (folderId: number | null) => {
  currentFolderId.value = folderId || undefined
  const path = folderId ? `/files/${currentSpaceId.value}/${folderId}` : `/files/${currentSpaceId.value}`
  router.push(path)
  await loadFolderContent()
}

const loadFolderContent = async () => {
  if (!currentSpaceId.value) return

  loading.value = true
  selectedItems.value = []

  try {
    if (currentFolderId.value) {
      // 加载文件夹内容
      const content = await folderApi.getFolderContent(currentFolderId.value)
      files.value = content.files
      folders.value = content.subfolders
      breadcrumbs.value = content.breadcrumb
    } else {
      // 加载空间根目录
      const rootContent = await folderApi.getFolders({
        spaceId: currentSpaceId.value,
        parentId: undefined
      })
      folders.value = rootContent.content
      
      // 加载根目录文件（需要额外API调用）
      const fileContent = await fileApi.searchFiles({
        spaceId: currentSpaceId.value,
        folderId: undefined,
        page: 0,
        size: 100
      })
      files.value = fileContent.content
      breadcrumbs.value = []
    }
  } catch (error) {
    console.error('Failed to load folder content:', error)
  } finally {
    loading.value = false
  }
}

const handleFileClick = (file: FileInfo) => {
  // 处理文件点击事件，可以预览或下载
  if (file.previewUrl) {
    // 打开预览
    window.open(file.previewUrl, '_blank')
  } else {
    // 下载文件
    window.open(file.downloadUrl, '_blank')
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

const showCreateFolderDialog = () => {
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
      parentId: currentFolderId.value
    })
    await loadFolderContent()
  } catch (error) {
    console.error('Failed to create folder:', error)
    alert('创建文件夹失败')
  }
}

const handleUploadSuccess = (uploadedFiles: FileInfo[]) => {
  showUploadDialog.value = false
  loadFolderContent()
}

const handleUploadError = (error: string) => {
  alert(`上传失败：${error}`)
}

const batchDownload = () => {
  const fileItems = selectedItems.value.filter(item => item.type === 'file')
  fileItems.forEach(item => {
    const file = files.value.find(f => f.id === item.id)
    if (file) {
      window.open(file.downloadUrl, '_blank')
    }
  })
}

const batchDelete = async () => {
  if (!confirm('确定要删除选中的项目吗？此操作不可撤销。')) return

  const fileIds = selectedItems.value.filter(item => item.type === 'file').map(item => item.id)
  const folderIds = selectedItems.value.filter(item => item.type === 'folder').map(item => item.id)

  try {
    if (fileIds.length > 0) {
      await fileApi.batchDeleteFiles(fileIds)
    }
    if (folderIds.length > 0) {
      await folderApi.batchDeleteFolders(folderIds)
    }
    selectedItems.value = []
    await loadFolderContent()
  } catch (error) {
    console.error('Failed to delete items:', error)
    alert('删除失败')
  }
}

const showContextMenu = (event: MouseEvent, item: any, type: 'file' | 'folder') => {
  event.preventDefault()
  // TODO: 实现右键菜单
  console.log('Context menu for', type, item)
}

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
    files.value = searchResults.content
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

const handleFolderRename = (folderId: number, name: string) => {
  // TODO: 实现文件夹重命名功能
  console.log('Rename folder:', folderId, name)
}

const handleFolderDelete = (folderId: number) => {
  // TODO: 实现文件夹删除功能
  console.log('Delete folder:', folderId)
}
</script>

<style scoped>
.file-manager {
  font-family: 'Inter', system-ui, -apple-system, sans-serif;
}
</style> 