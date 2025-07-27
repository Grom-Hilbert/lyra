<template>
  <div class="folder-tree">
    <!-- 树节点 -->
    <div
      v-for="node in treeNodes"
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { folderApi } from '@/apis/fileApi'
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
  // TODO: 显示上下文菜单
  console.log('Context menu for folder:', node)
}
</script>

<style scoped>
.folder-tree {
  font-size: 0.875rem;
  line-height: 1.25rem;
}

.tree-node {
  user-select: none;
}
</style> 