<template>
  <div class="folder-tree">
    <!-- 搜索框 -->
    <div class="folder-search mb-3">
      <div class="relative">
        <input
          v-model="searchQuery"
          type="text"
          placeholder="搜索文件夹..."
          class="w-full pl-8 pr-3 py-2 text-sm border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-primary/20"
        />
        <svg class="absolute left-2.5 top-2.5 w-4 h-4 text-muted-foreground" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd"/>
        </svg>
      </div>
    </div>

    <!-- 树节点 -->
    <div
      v-for="node in filteredNodes"
      :key="node.id"
      class="folder-tree-node"
    >
      <div
        class="folder-node cursor-pointer p-2 hover:bg-gray-100 flex items-center"
        :class="{ 'bg-blue-50 border-l-2 border-blue-500': currentFolderId === node.id }"
        @click="handleNodeClick(node)"
        @contextmenu="handleNodeContext($event, node)"
      >
        <!-- 展开/收起图标 -->
        <button
          v-if="node.children.length > 0 || !expandedNodes.has(node.id)"
          @click.stop="handleNodeExpand(node)"
          class="mr-1 p-1 hover:bg-gray-200 rounded"
        >
          <svg
            class="w-3 h-3 transition-transform"
            :class="{ 'rotate-90': expandedNodes.has(node.id) }"
            fill="currentColor"
            viewBox="0 0 20 20"
          >
            <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 111.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
          </svg>
        </button>
        <div v-else class="w-4 mr-1"></div>

        <!-- 文件夹图标 -->
        <svg class="w-4 h-4 mr-2 text-yellow-600" fill="currentColor" viewBox="0 0 20 20">
          <path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"/>
        </svg>

        <!-- 文件夹名称 -->
        <span class="text-sm truncate">{{ node.name }}</span>

        <!-- 加载指示器 -->
        <div v-if="node.isLoading" class="ml-auto">
          <div class="animate-spin rounded-full h-3 w-3 border-b-2 border-blue-600"></div>
        </div>
      </div>

      <!-- 子节点 -->
      <div
        v-if="expandedNodes.has(node.id) && node.children.length > 0"
        class="ml-4 border-l border-gray-200"
      >
        <div
          v-for="child in node.children"
          :key="child.id"
          class="folder-tree-node"
        >
          <div
            class="folder-node cursor-pointer p-2 hover:bg-gray-100 flex items-center"
            :class="{ 'bg-blue-50 border-l-2 border-blue-500': currentFolderId === child.id }"
            @click="handleNodeClick(child)"
            @contextmenu="handleNodeContext($event, child)"
          >
            <!-- 展开/收起图标 -->
            <button
              v-if="child.children.length > 0 || !expandedNodes.has(child.id)"
              @click.stop="handleNodeExpand(child)"
              class="mr-1 p-1 hover:bg-gray-200 rounded"
            >
              <svg
                class="w-3 h-3 transition-transform"
                :class="{ 'rotate-90': expandedNodes.has(child.id) }"
                fill="currentColor"
                viewBox="0 0 20 20"
              >
                <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 111.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
              </svg>
            </button>
            <div v-else class="w-4 mr-1"></div>

            <!-- 文件夹图标 -->
            <svg class="w-4 h-4 mr-2 text-yellow-600" fill="currentColor" viewBox="0 0 20 20">
              <path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"/>
            </svg>

            <!-- 文件夹名称 -->
            <span class="text-sm truncate">{{ child.name }}</span>

            <!-- 加载指示器 -->
            <div v-if="child.isLoading" class="ml-auto">
              <div class="animate-spin rounded-full h-3 w-3 border-b-2 border-blue-600"></div>
            </div>
          </div>

          <!-- 递归渲染子节点 -->
          <div
            v-if="expandedNodes.has(child.id) && child.children.length > 0"
            class="ml-4"
          >
            <div
              v-for="grandChild in child.children"
              :key="grandChild.id"
              class="folder-tree-node"
            >
              <!-- 这里可以继续递归，但为了简化先只显示两层 -->
              <div
                class="folder-node cursor-pointer p-2 hover:bg-gray-100 flex items-center"
                :class="{ 'bg-blue-50 border-l-2 border-blue-500': currentFolderId === grandChild.id }"
                @click="handleNodeClick(grandChild)"
                @contextmenu="handleNodeContext($event, grandChild)"
              >
                <div class="w-4 mr-1"></div>
                <svg class="w-4 h-4 mr-2 text-yellow-600" fill="currentColor" viewBox="0 0 20 20">
                  <path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"/>
                </svg>
                <span class="text-sm truncate">{{ grandChild.name }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="flex items-center justify-center py-4">
      <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
      <span class="ml-2 text-sm text-gray-500">加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && treeNodes.length === 0" class="py-4 text-center">
      <p class="text-sm text-gray-500">暂无文件夹</p>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="contextMenu.visible"
      class="context-menu fixed z-50 bg-background border border-border rounded-md shadow-lg py-1 min-w-32"
      :style="{ top: contextMenu.y + 'px', left: contextMenu.x + 'px' }"
      @click.stop
    >
      <button @click="handleContextAction('create')" class="context-menu-item">
        <svg class="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M10 3a1 1 0 011 1v5h5a1 1 0 110 2h-5v5a1 1 0 11-2 0v-5H4a1 1 0 110-2h5V4a1 1 0 011-1z" clip-rule="evenodd"/>
        </svg>
        新建文件夹
      </button>
      <button @click="handleContextAction('rename')" class="context-menu-item">
        <svg class="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 20 20">
          <path d="M13.586 3.586a2 2 0 112.828 2.828l-.793.793-2.828-2.828.793-.793zM11.379 5.793L3 14.172V17h2.828l8.38-8.379-2.83-2.828z"/>
        </svg>
        重命名
      </button>
      <button @click="handleContextAction('refresh')" class="context-menu-item">
        <svg class="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M4 2a1 1 0 011 1v2.101a7.002 7.002 0 0111.601 2.566 1 1 0 11-1.885.666A5.002 5.002 0 005.999 7H9a1 1 0 010 2H4a1 1 0 01-1-1V3a1 1 0 011-1z" clip-rule="evenodd"/>
        </svg>
        刷新
      </button>
      <div class="border-t border-border my-1"></div>
      <button @click="handleContextAction('delete')" class="context-menu-item text-destructive">
        <svg class="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 20 20">
          <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9z" clip-rule="evenodd"/>
        </svg>
        删除
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { folderApi } from '@/apis'
import type { FolderTreeNode as ApiFolderTreeNode } from '@/types/index'

interface LocalFolderTreeNode {
  id: number
  name: string
  path: string
  level: number
  parentId?: number
  children: LocalFolderTreeNode[]
  isExpanded?: boolean
  isLoading?: boolean
}

interface Props {
  spaceId: number
  currentFolderId?: number | null
}

interface Emits {
  (e: 'folder-select', folderId: number | null): void
  (e: 'folder-create', parentId?: number): void
  (e: 'folder-rename', folderId: number, name: string): void
  (e: 'folder-delete', folderId: number): void
  (e: 'folder-move', folderId: number, targetFolderId: number): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const loading = ref(false)
const treeNodes = ref<LocalFolderTreeNode[]>([])
const expandedNodes = ref<Set<number>>(new Set())

// 生命周期
onMounted(() => {
  loadFolderTree()
})

// 监听spaceId变化
watch(() => props.spaceId, () => {
  expandedNodes.value.clear()
  loadFolderTree()
})

// 方法
const loadFolderTree = async () => {
  if (!props.spaceId) return

  loading.value = true
  try {
    const treeResponse = await folderApi.getFolderTree({
      spaceId: props.spaceId,
      maxDepth: 3
    })
    if (treeResponse.success && treeResponse.data) {
      treeNodes.value = convertToLocalNodes(treeResponse.data)
    }
  } catch (error) {
    console.error('Failed to load folder tree:', error)
  } finally {
    loading.value = false
  }
}

const convertToLocalNodes = (apiNodes: ApiFolderTreeNode[]): LocalFolderTreeNode[] => {
  return apiNodes.map(apiNode => ({
    id: apiNode.folder.id,
    name: apiNode.folder.name,
    path: apiNode.folder.path,
    level: apiNode.folder.level,
    parentId: apiNode.folder.parentId,
    children: convertToLocalNodes(apiNode.children),
    isExpanded: false,
    isLoading: false
  }))
}

const handleNodeClick = (node: LocalFolderTreeNode) => {
  emit('folder-select', node.id)
}

const handleNodeExpand = async (node: LocalFolderTreeNode) => {
  if (expandedNodes.value.has(node.id)) {
    expandedNodes.value.delete(node.id)
  } else {
    expandedNodes.value.add(node.id)

    // 如果节点没有子节点，则加载
    if (!node.children || node.children.length === 0) {
      await loadChildNodes(node)
    }
  }
}

const loadChildNodes = async (parentNode: LocalFolderTreeNode) => {
  try {
    const childrenResponse = await folderApi.getFolders({
      spaceId: props.spaceId,
      parentId: parentNode.id,
      size: 100
    })

    if (!childrenResponse.success || !childrenResponse.data) return

    const childNodes: LocalFolderTreeNode[] = (childrenResponse.data || []).map((folder: any) => ({
      id: folder.id,
      name: folder.name,
      path: folder.path,
      parentId: folder.parentId || undefined,
      level: parentNode.level + 1,
      children: [],
      isExpanded: false,
      isLoading: false
    }))

    parentNode.children = childNodes
  } catch (error) {
    console.error('Failed to load child nodes:', error)
  }
}

const handleNodeContext = (event: MouseEvent, node: LocalFolderTreeNode) => {
  event.preventDefault()
  showContextMenu(event, node)
}

// 右键菜单相关
const contextMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  node: null as LocalFolderTreeNode | null
})

const showContextMenu = (event: MouseEvent, node: LocalFolderTreeNode) => {
  contextMenu.value = {
    visible: true,
    x: event.clientX,
    y: event.clientY,
    node
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
  const node = contextMenu.value.node
  if (!node) return

  switch (action) {
    case 'create':
      handleCreateFolder(node.id)
      break
    case 'rename':
      handleRenameFolder(node)
      break
    case 'delete':
      handleDeleteFolder(node)
      break
    case 'refresh':
      refreshNode(node)
      break
  }

  hideContextMenu()
}

const handleCreateFolder = (parentId?: number) => {
  const name = prompt('请输入文件夹名称:')
  if (name && name.trim()) {
    emit('folder-create', parentId)
  }
}

const handleRenameFolder = (node: LocalFolderTreeNode) => {
  const newName = prompt('重命名文件夹:', node.name)
  if (newName && newName.trim() && newName !== node.name) {
    emit('folder-rename', node.id, newName.trim())
  }
}

const handleDeleteFolder = (node: LocalFolderTreeNode) => {
  if (confirm(`确定要删除文件夹 "${node.name}" 吗？此操作不可撤销。`)) {
    emit('folder-delete', node.id)
  }
}

const refreshNode = async (node: LocalFolderTreeNode) => {
  node.children = []
  expandedNodes.value.delete(node.id)
  await loadChildNodes(node)
  expandedNodes.value.add(node.id)
}

// 搜索功能
const searchQuery = ref('')
const filteredNodes = computed(() => {
  if (!searchQuery.value.trim()) {
    return treeNodes.value
  }

  return filterNodes(treeNodes.value, searchQuery.value.toLowerCase())
})

const filterNodes = (nodes: LocalFolderTreeNode[], query: string): LocalFolderTreeNode[] => {
  const filtered: LocalFolderTreeNode[] = []

  for (const node of nodes) {
    const matchesQuery = node.name.toLowerCase().includes(query)
    const filteredChildren = filterNodes(node.children, query)

    if (matchesQuery || filteredChildren.length > 0) {
      filtered.push({
        ...node,
        children: filteredChildren
      })
    }
  }

  return filtered
}

// 拖拽功能
const draggedNode = ref<LocalFolderTreeNode | null>(null)

const handleDragStart = (event: DragEvent, node: LocalFolderTreeNode) => {
  draggedNode.value = node
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', node.id.toString())
  }
}

const handleDragOver = (event: DragEvent) => {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'move'
  }
}

const handleDrop = (event: DragEvent, targetNode: LocalFolderTreeNode) => {
  event.preventDefault()

  if (!draggedNode.value || draggedNode.value.id === targetNode.id) {
    return
  }

  // 检查是否试图将文件夹移动到自己的子文件夹中
  if (isDescendant(targetNode, draggedNode.value)) {
    alert('不能将文件夹移动到自己的子文件夹中')
    return
  }

  // 触发移动事件
  emit('folder-move', draggedNode.value.id, targetNode.id)
  draggedNode.value = null
}

const isDescendant = (ancestor: LocalFolderTreeNode, node: LocalFolderTreeNode): boolean => {
  for (const child of ancestor.children) {
    if (child.id === node.id || isDescendant(child, node)) {
      return true
    }
  }
  return false
}

// 生命周期
onUnmounted(() => {
  document.removeEventListener('click', hideContextMenu)
})
</script>

<style scoped>
.folder-tree {
  font-size: 0.875rem;
  line-height: 1.25rem;
}

.tree-node {
  user-select: none;
}

.folder-search input:focus {
  outline: none;
  ring: 2px solid var(--primary);
  ring-opacity: 0.2;
  border-color: var(--primary);
}

.context-menu {
  background-color: var(--background);
  border: 1px solid var(--border);
  border-radius: 0.375rem;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  padding: 0.25rem 0;
  min-width: 8rem;
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

.folder-node {
  transition: background-color 150ms;
}

.folder-node:hover {
  background-color: var(--accent);
}

.folder-node.selected {
  background-color: rgba(var(--primary), 0.1);
  border-left: 2px solid var(--primary);
}
</style>