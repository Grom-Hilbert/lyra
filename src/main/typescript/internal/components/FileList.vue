<template>
  <div class="file-list">
    <!-- 工具栏 -->
    <div class="flex items-center justify-between mb-4 p-4 bg-background border-b">
      <!-- 左侧操作 -->
      <div class="flex items-center space-x-2">
        <!-- 全选复选框 -->
        <input
          v-if="selectable"
          type="checkbox"
          :checked="isAllSelected"
          :indeterminate="isPartiallySelected"
          @change="handleSelectAll"
          class="rounded border-border"
        />
        
        <!-- 选中项操作 -->
        <div v-if="selectedItems.length > 0" class="flex items-center space-x-2">
          <span class="text-sm text-muted-foreground">
            已选择 {{ selectedItems.length }} 项
          </span>
          <button
            @click="$emit('batch-download', selectedItems)"
            class="px-3 py-1 text-sm bg-primary text-primary-foreground rounded hover:bg-primary/90"
          >
            下载
          </button>
          <button
            @click="$emit('batch-delete', selectedItems)"
            class="px-3 py-1 text-sm bg-destructive text-destructive-foreground rounded hover:bg-destructive/90"
          >
            删除
          </button>
        </div>
      </div>

      <!-- 右侧操作 -->
      <div class="flex items-center space-x-2">
        <!-- 排序选择 -->
        <select
          v-model="currentSort"
          @change="handleSortChange"
          class="px-3 py-1 text-sm border border-border rounded"
        >
          <option value="name">按名称</option>
          <option value="size">按大小</option>
          <option value="updatedAt">按修改时间</option>
          <option value="type">按类型</option>
        </select>

        <!-- 排序方向 -->
        <button
          @click="toggleSortDirection"
          class="p-1 hover:bg-accent rounded"
          :title="sortDirection === 'asc' ? '升序' : '降序'"
        >
          <svg class="w-4 h-4" :class="{ 'rotate-180': sortDirection === 'desc' }" fill="currentColor" viewBox="0 0 20 20">
            <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd"/>
          </svg>
        </button>

        <!-- 视图切换 -->
        <div class="flex border border-border rounded">
          <button
            @click="$emit('view-mode-change', 'grid')"
            :class="[
              'p-2 hover:bg-accent',
              viewMode === 'grid' ? 'bg-accent' : ''
            ]"
            title="网格视图"
          >
            <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
              <path d="M5 3a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2V5a2 2 0 00-2-2H5zM5 11a2 2 0 00-2 2v2a2 2 0 002 2h2a2 2 0 002-2v-2a2 2 0 00-2-2H5zM11 5a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V5zM11 13a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/>
            </svg>
          </button>
          <button
            @click="$emit('view-mode-change', 'list')"
            :class="[
              'p-2 hover:bg-accent',
              viewMode === 'list' ? 'bg-accent' : ''
            ]"
            title="列表视图"
          >
            <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M3 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm0 4a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1z" clip-rule="evenodd"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- 文件列表内容 -->
    <div class="file-list-content">
      <!-- 网格视图 -->
      <div v-if="viewMode === 'grid'" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-4 p-4">
        <!-- 文件夹 -->
        <div
          v-for="folder in sortedFolders"
          :key="`folder-${folder.id}`"
          class="file-item folder-item"
          :class="{ 'selected': isSelected('folder', folder.id) }"
          @click="handleItemClick('folder', folder)"
          @dblclick="$emit('folder-open', folder)"
          @contextmenu="handleContextMenu($event, 'folder', folder)"
        >
          <div class="file-item-content">
            <input
              v-if="selectable"
              type="checkbox"
              :checked="isSelected('folder', folder.id)"
              @click.stop
              @change="handleItemSelect('folder', folder.id, $event)"
              class="file-item-checkbox"
            />
            <div class="file-icon">
              <svg class="w-12 h-12 text-primary" fill="currentColor" viewBox="0 0 20 20">
                <path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"/>
              </svg>
            </div>
            <div class="file-info">
              <div class="file-name" :title="folder.name">{{ folder.name }}</div>
              <div class="file-meta">{{ folder.fileCount }} 项</div>
            </div>
          </div>
        </div>

        <!-- 文件 -->
        <div
          v-for="file in sortedFiles"
          :key="`file-${file.id}`"
          class="file-item file-item-file"
          :class="{ 'selected': isSelected('file', file.id) }"
          @click="handleItemClick('file', file)"
          @dblclick="$emit('file-open', file)"
          @contextmenu="handleContextMenu($event, 'file', file)"
        >
          <div class="file-item-content">
            <input
              v-if="selectable"
              type="checkbox"
              :checked="isSelected('file', file.id)"
              @click.stop
              @change="handleItemSelect('file', file.id, $event)"
              class="file-item-checkbox"
            />
            <div class="file-icon">
              <img
                v-if="isImageFile(file)"
                :src="getFilePreviewUrl(file)"
                :alt="file.filename"
                class="w-12 h-12 object-cover rounded"
                @error="handleImageError"
              />
              <div v-else class="file-type-icon">
                <svg class="w-12 h-12 text-muted-foreground" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4zm2 6a1 1 0 011-1h6a1 1 0 110 2H7a1 1 0 01-1-1zm1 3a1 1 0 100 2h6a1 1 0 100-2H7z" clip-rule="evenodd"/>
                </svg>
                <span class="file-extension">{{ getFileExtension(file.filename) }}</span>
              </div>
            </div>
            <div class="file-info">
              <div class="file-name" :title="file.filename">{{ file.filename }}</div>
              <div class="file-meta">{{ formatFileSize(file.sizeBytes) }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 列表视图 -->
      <div v-else class="list-view">
        <table class="w-full">
          <thead class="border-b">
            <tr>
              <th v-if="selectable" class="w-8 p-2">
                <input
                  type="checkbox"
                  :checked="isAllSelected"
                  :indeterminate="isPartiallySelected"
                  @change="handleSelectAll"
                  class="rounded border-border"
                />
              </th>
              <th class="text-left p-2">名称</th>
              <th class="text-left p-2 w-24">大小</th>
              <th class="text-left p-2 w-32">修改时间</th>
              <th class="text-left p-2 w-20">操作</th>
            </tr>
          </thead>
          <tbody>
            <!-- 文件夹行 -->
            <tr
              v-for="folder in sortedFolders"
              :key="`folder-${folder.id}`"
              class="list-item folder-row"
              :class="{ 'selected': isSelected('folder', folder.id) }"
              @click="handleItemClick('folder', folder)"
              @dblclick="$emit('folder-open', folder)"
              @contextmenu="handleContextMenu($event, 'folder', folder)"
            >
              <td v-if="selectable" class="p-2">
                <input
                  type="checkbox"
                  :checked="isSelected('folder', folder.id)"
                  @click.stop
                  @change="handleItemSelect('folder', folder.id, $event)"
                  class="rounded border-border"
                />
              </td>
              <td class="p-2">
                <div class="flex items-center">
                  <svg class="w-5 h-5 mr-2 text-primary" fill="currentColor" viewBox="0 0 20 20">
                    <path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"/>
                  </svg>
                  <span>{{ folder.name }}</span>
                </div>
              </td>
              <td class="p-2 text-muted-foreground">{{ folder.fileCount }} 项</td>
              <td class="p-2 text-muted-foreground">{{ formatDate(folder.updatedAt) }}</td>
              <td class="p-2">
                <button
                  @click.stop="$emit('folder-open', folder)"
                  class="text-primary hover:text-primary/80"
                >
                  打开
                </button>
              </td>
            </tr>

            <!-- 文件行 -->
            <tr
              v-for="file in sortedFiles"
              :key="`file-${file.id}`"
              class="list-item file-row"
              :class="{ 'selected': isSelected('file', file.id) }"
              @click="handleItemClick('file', file)"
              @dblclick="$emit('file-open', file)"
              @contextmenu="handleContextMenu($event, 'file', file)"
            >
              <td v-if="selectable" class="p-2">
                <input
                  type="checkbox"
                  :checked="isSelected('file', file.id)"
                  @click.stop
                  @change="handleItemSelect('file', file.id, $event)"
                  class="rounded border-border"
                />
              </td>
              <td class="p-2">
                <div class="flex items-center">
                  <div class="w-5 h-5 mr-2 flex-shrink-0">
                    <img
                      v-if="isImageFile(file)"
                      :src="getFilePreviewUrl(file)"
                      :alt="file.filename"
                      class="w-5 h-5 object-cover rounded"
                      @error="handleImageError"
                    />
                    <svg v-else class="w-5 h-5 text-muted-foreground" fill="currentColor" viewBox="0 0 20 20">
                      <path fill-rule="evenodd" d="M4 4a2 2 0 012-2h4.586A2 2 0 0112 2.586L15.414 6A2 2 0 0116 7.414V16a2 2 0 01-2 2H6a2 2 0 01-2-2V4z" clip-rule="evenodd"/>
                    </svg>
                  </div>
                  <span>{{ file.filename }}</span>
                </div>
              </td>
              <td class="p-2 text-muted-foreground">{{ formatFileSize(file.sizeBytes) }}</td>
              <td class="p-2 text-muted-foreground">{{ formatDate(file.updatedAt) }}</td>
              <td class="p-2">
                <button
                  @click.stop="$emit('file-download', file)"
                  class="text-primary hover:text-primary/80 mr-2"
                >
                  下载
                </button>
                <button
                  @click.stop="$emit('file-open', file)"
                  class="text-primary hover:text-primary/80"
                >
                  预览
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- 空状态 -->
      <div v-if="!loading && totalItems === 0" class="empty-state">
        <div class="text-center py-12">
          <svg class="mx-auto h-12 w-12 text-muted-foreground" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z"/>
          </svg>
          <h3 class="mt-2 text-sm font-medium text-foreground">暂无文件</h3>
          <p class="mt-1 text-sm text-muted-foreground">开始上传文件或创建文件夹</p>
        </div>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <div class="flex items-center justify-center py-12">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
          <span class="ml-2 text-muted-foreground">加载中...</span>
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="contextMenu.visible"
      ref="contextMenuRef"
      class="context-menu"
      :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }"
      @click.stop
    >
      <div class="bg-background border border-border rounded-md shadow-lg py-1 min-w-32">
        <template v-if="contextMenu.type === 'file'">
          <button @click="handleContextAction('open')" class="context-menu-item">
            <svg class="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 20 20">
              <path d="M10 12a2 2 0 100-4 2 2 0 000 4z"/>
              <path fill-rule="evenodd" d="M.458 10C1.732 5.943 5.522 3 10 3s8.268 2.943 9.542 7c-1.274 4.057-5.064 7-9.542 7S1.732 14.057.458 10zM14 10a4 4 0 11-8 0 4 4 0 018 0z" clip-rule="evenodd"/>
            </svg>
            预览
          </button>
          <button @click="handleContextAction('download')" class="context-menu-item">
            <svg class="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clip-rule="evenodd"/>
            </svg>
            下载
          </button>
          <button @click="handleContextAction('rename')" class="context-menu-item">
            <svg class="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 20 20">
              <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z"/>
            </svg>
            重命名
          </button>
          <div class="border-t border-border my-1"></div>
          <button @click="handleContextAction('delete')" class="context-menu-item text-destructive">
            <svg class="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd"/>
            </svg>
            删除
          </button>
        </template>
        <template v-else-if="contextMenu.type === 'folder'">
          <button @click="handleContextAction('open')" class="context-menu-item">
            <svg class="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 20 20">
              <path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"/>
            </svg>
            打开
          </button>
          <button @click="handleContextAction('rename')" class="context-menu-item">
            <svg class="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 20 20">
              <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z"/>
            </svg>
            重命名
          </button>
          <div class="border-t border-border my-1"></div>
          <button @click="handleContextAction('delete')" class="context-menu-item text-destructive">
            <svg class="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9z" clip-rule="evenodd"/>
            </svg>
            删除
          </button>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import type { IFileInfo, IFolderInfo } from '@/types/index'

interface Props {
  files: IFileInfo[]
  folders: IFolderInfo[]
  viewMode: 'grid' | 'list'
  selectable?: boolean
  loading?: boolean
  sortBy?: string
  sortDirection?: 'asc' | 'desc'
}

interface Emits {
  (e: 'file-select', file: IFileInfo): void
  (e: 'file-open', file: IFileInfo): void
  (e: 'file-download', file: IFileInfo): void
  (e: 'file-delete', file: IFileInfo): void
  (e: 'file-rename', file: IFileInfo, newName: string): void
  (e: 'folder-open', folder: IFolderInfo): void
  (e: 'folder-delete', folder: IFolderInfo): void
  (e: 'folder-rename', folder: IFolderInfo, newName: string): void
  (e: 'selection-change', selectedItems: Array<{ type: 'file' | 'folder', id: number, item: IFileInfo | IFolderInfo }>): void
  (e: 'batch-download', selectedItems: Array<{ type: 'file' | 'folder', id: number }>): void
  (e: 'batch-delete', selectedItems: Array<{ type: 'file' | 'folder', id: number }>): void
  (e: 'view-mode-change', mode: 'grid' | 'list'): void
  (e: 'sort-change', sortBy: string, direction: 'asc' | 'desc'): void
}

const props = withDefaults(defineProps<Props>(), {
  selectable: true,
  loading: false,
  sortBy: 'name',
  sortDirection: 'asc'
})

const emit = defineEmits<Emits>()

// 响应式数据
const selectedItems = ref<Array<{ type: 'file' | 'folder', id: number, item: IFileInfo | IFolderInfo }>>([])
const currentSort = ref(props.sortBy)
const sortDirection = ref(props.sortDirection)
const contextMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  type: '' as 'file' | 'folder',
  item: null as IFileInfo | IFolderInfo | null
})
const contextMenuRef = ref<HTMLElement>()

// 计算属性
const totalItems = computed(() => props.files.length + props.folders.length)

const isAllSelected = computed(() => {
  return totalItems.value > 0 && selectedItems.value.length === totalItems.value
})

const isPartiallySelected = computed(() => {
  return selectedItems.value.length > 0 && selectedItems.value.length < totalItems.value
})

const sortedFolders = computed(() => {
  const folders = [...props.folders]
  return folders.sort((a, b) => {
    let aValue: any, bValue: any

    switch (currentSort.value) {
      case 'name':
        aValue = a.name.toLowerCase()
        bValue = b.name.toLowerCase()
        break
      case 'size':
        aValue = a.sizeBytes || 0
        bValue = b.sizeBytes || 0
        break
      case 'updatedAt':
        aValue = new Date(a.updatedAt).getTime()
        bValue = new Date(b.updatedAt).getTime()
        break
      case 'type':
        aValue = 'folder'
        bValue = 'folder'
        break
      default:
        aValue = a.name.toLowerCase()
        bValue = b.name.toLowerCase()
    }

    if (aValue < bValue) return sortDirection.value === 'asc' ? -1 : 1
    if (aValue > bValue) return sortDirection.value === 'asc' ? 1 : -1
    return 0
  })
})

const sortedFiles = computed(() => {
  const files = [...props.files]
  return files.sort((a, b) => {
    let aValue: any, bValue: any

    switch (currentSort.value) {
      case 'name':
        aValue = a.filename.toLowerCase()
        bValue = b.filename.toLowerCase()
        break
      case 'size':
        aValue = a.sizeBytes
        bValue = b.sizeBytes
        break
      case 'updatedAt':
        aValue = new Date(a.updatedAt).getTime()
        bValue = new Date(b.updatedAt).getTime()
        break
      case 'type':
        aValue = getFileExtension(a.filename).toLowerCase()
        bValue = getFileExtension(b.filename).toLowerCase()
        break
      default:
        aValue = a.filename.toLowerCase()
        bValue = b.filename.toLowerCase()
    }

    if (aValue < bValue) return sortDirection.value === 'asc' ? -1 : 1
    if (aValue > bValue) return sortDirection.value === 'asc' ? 1 : -1
    return 0
  })
})

// 方法
const isSelected = (type: 'file' | 'folder', id: number): boolean => {
  return selectedItems.value.some(item => item.type === type && item.id === id)
}

const handleItemSelect = (type: 'file' | 'folder', id: number, event: Event) => {
  const target = event.target as HTMLInputElement
  const item = type === 'file'
    ? props.files.find(f => f.id === id)
    : props.folders.find(f => f.id === id)

  if (!item) return

  if (target.checked) {
    selectedItems.value.push({ type, id, item })
  } else {
    const index = selectedItems.value.findIndex(item => item.type === type && item.id === id)
    if (index > -1) {
      selectedItems.value.splice(index, 1)
    }
  }

  emit('selection-change', selectedItems.value)
}

const handleSelectAll = (event: Event) => {
  const target = event.target as HTMLInputElement

  if (target.checked) {
    selectedItems.value = [
      ...props.folders.map(folder => ({ type: 'folder' as const, id: folder.id, item: folder })),
      ...props.files.map(file => ({ type: 'file' as const, id: file.id, item: file }))
    ]
  } else {
    selectedItems.value = []
  }

  emit('selection-change', selectedItems.value)
}

const handleItemClick = (type: 'file' | 'folder', item: IFileInfo | IFolderInfo) => {
  if (type === 'file') {
    emit('file-select', item as IFileInfo)
  }
}

const handleSortChange = () => {
  emit('sort-change', currentSort.value, sortDirection.value)
}

const toggleSortDirection = () => {
  sortDirection.value = sortDirection.value === 'asc' ? 'desc' : 'asc'
  emit('sort-change', currentSort.value, sortDirection.value)
}

const handleContextMenu = (event: MouseEvent, type: 'file' | 'folder', item: IFileInfo | IFolderInfo) => {
  event.preventDefault()

  contextMenu.value = {
    visible: true,
    x: event.clientX,
    y: event.clientY,
    type,
    item
  }

  nextTick(() => {
    document.addEventListener('click', hideContextMenu)
  })
}

const hideContextMenu = () => {
  contextMenu.value.visible = false
  document.removeEventListener('click', hideContextMenu)
}

const handleContextAction = (action: string) => {
  const { type, item } = contextMenu.value

  if (!item) return

  switch (action) {
    case 'open':
      if (type === 'file') {
        emit('file-open', item as IFileInfo)
      } else {
        emit('folder-open', item as IFolderInfo)
      }
      break
    case 'download':
      if (type === 'file') {
        emit('file-download', item as IFileInfo)
      }
      break
    case 'rename':
      const newName = prompt(`重命名${type === 'file' ? '文件' : '文件夹'}:`,
        type === 'file' ? (item as IFileInfo).filename : (item as IFolderInfo).name)
      if (newName && newName.trim()) {
        if (type === 'file') {
          emit('file-rename', item as IFileInfo, newName.trim())
        } else {
          emit('folder-rename', item as IFolderInfo, newName.trim())
        }
      }
      break
    case 'delete':
      if (confirm(`确定要删除这个${type === 'file' ? '文件' : '文件夹'}吗？`)) {
        if (type === 'file') {
          emit('file-delete', item as IFileInfo)
        } else {
          emit('folder-delete', item as IFolderInfo)
        }
      }
      break
  }

  hideContextMenu()
}

// 工具函数
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'

  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))

  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDate = (dateString: string): string => {
  const date = new Date(dateString)
  const now = new Date()
  const diffTime = Math.abs(now.getTime() - date.getTime())
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  if (diffDays === 1) {
    return '今天'
  } else if (diffDays === 2) {
    return '昨天'
  } else if (diffDays <= 7) {
    return `${diffDays} 天前`
  } else {
    return date.toLocaleDateString('zh-CN')
  }
}

const getFileExtension = (filename: string): string => {
  const lastDot = filename.lastIndexOf('.')
  return lastDot > 0 ? filename.substring(lastDot + 1).toUpperCase() : ''
}

const isImageFile = (file: IFileInfo): boolean => {
  const imageTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml']
  return imageTypes.includes(file.mimeType)
}

const getFilePreviewUrl = (file: IFileInfo): string => {
  return `/api/files/${file.id}/preview`
}

const handleImageError = (event: Event) => {
  const target = event.target as HTMLImageElement
  target.style.display = 'none'
}

// 生命周期
onMounted(() => {
  // 初始化
})

onUnmounted(() => {
  document.removeEventListener('click', hideContextMenu)
})
</script>

<style scoped>
.file-list {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.file-list-content {
  flex: 1;
  overflow: auto;
}

.file-item {
  position: relative;
  cursor: pointer;
  border-radius: 0.5rem;
  border: 2px solid transparent;
  transition: all 0.2s;
}

.file-item:hover {
  border-color: rgba(var(--primary), 0.2);
  background-color: rgba(var(--primary), 0.05);
}

.file-item.selected {
  border-color: rgba(var(--primary), 0.5);
  background-color: rgba(var(--primary), 0.1);
}

.file-item-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 1rem;
  position: relative;
}

.file-item-checkbox {
  position: absolute;
  top: 0.5rem;
  left: 0.5rem;
  border-radius: 0.25rem;
}

.file-icon {
  flex-shrink: 0;
  margin-bottom: 0.75rem;
  position: relative;
}

.file-type-icon {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.file-extension {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  font-weight: bold;
  color: white;
  background-color: rgba(0, 0, 0, 0.5);
  border-radius: 0.25rem;
}

.file-info {
  text-align: center;
  width: 100%;
}

.file-name {
  font-size: 0.875rem;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 0.25rem;
}

.file-meta {
  font-size: 0.75rem;
  color: var(--muted-foreground);
}

.list-view table {
  table-layout: fixed;
}

.list-item {
  cursor: pointer;
}

.list-item:hover {
  background-color: var(--accent);
}

.list-item.selected {
  background-color: rgba(var(--primary), 0.1);
}

.context-menu {
  position: fixed;
  z-index: 50;
}

.context-menu-item {
  width: 100%;
  text-align: left;
  padding: 0.5rem 0.75rem;
  font-size: 0.875rem;
  display: flex;
  align-items: center;
}

.context-menu-item:hover {
  background-color: var(--accent);
}

.empty-state,
.loading-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
