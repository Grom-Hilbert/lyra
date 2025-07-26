<template>
  <div class="folder-tree">
    <!-- 树节点 -->
    <div
      v-for="node in treeNodes"
      :key="node.id"
      class="folder-tree-node"
    >
      <TreeNode
        :node="node"
        :current-folder-id="currentFolderId"
        :expanded-nodes="expandedNodes"
        @node-click="handleNodeClick"
        @node-expand="handleNodeExpand"
        @node-context="handleNodeContext"
      />
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
import type { FolderTreeNode } from '@/types/file'
import TreeNode from './TreeNode.vue'

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
const treeNodes = ref<FolderTreeNode[]>([])
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
    const tree = await folderApi.getFolderTree(props.spaceId, 3) // 最大深度3层
    treeNodes.value = buildTreeStructure(tree)
  } catch (error) {
    console.error('Failed to load folder tree:', error)
  } finally {
    loading.value = false
  }
}

const buildTreeStructure = (flatTree: FolderTreeNode[]): FolderTreeNode[] => {
  const nodeMap = new Map<number, FolderTreeNode>()
  const rootNodes: FolderTreeNode[] = []

  // 创建节点映射
  flatTree.forEach(node => {
    nodeMap.set(node.id, { ...node, children: [] })
  })

  // 构建树结构
  flatTree.forEach(node => {
    const treeNode = nodeMap.get(node.id)!
    if (node.parentId && nodeMap.has(node.parentId)) {
      const parent = nodeMap.get(node.parentId)!
      if (!parent.children) parent.children = []
      parent.children.push(treeNode)
    } else {
      rootNodes.push(treeNode)
    }
  })

  return rootNodes
}

const handleNodeClick = (node: FolderTreeNode) => {
  emit('folder-select', node.id)
}

const handleNodeExpand = async (node: FolderTreeNode) => {
  if (expandedNodes.value.has(node.id)) {
    expandedNodes.value.delete(node.id)
  } else {
    expandedNodes.value.add(node.id)
    
    // 如果节点有子文件夹但未加载，则加载子文件夹
    if (node.hasChildren && (!node.children || node.children.length === 0)) {
      await loadChildNodes(node)
    }
  }
}

const loadChildNodes = async (parentNode: FolderTreeNode) => {
  try {
    const children = await folderApi.getFolders({
      spaceId: props.spaceId,
      parentId: parentNode.id,
      size: 100
    })
    
    const childNodes: FolderTreeNode[] = children.content.map(folder => ({
      id: folder.id,
      name: folder.name,
      path: folder.path,
      parentId: folder.parentId || undefined,
      level: parentNode.level + 1,
      isRoot: false,
      hasChildren: folder.folderCount > 0,
      fileCount: folder.fileCount,
      folderCount: folder.folderCount,
      children: [],
      permissions: folder.permissions
    }))

    parentNode.children = childNodes
  } catch (error) {
    console.error('Failed to load child nodes:', error)
  }
}

const handleNodeContext = (event: MouseEvent, node: FolderTreeNode) => {
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