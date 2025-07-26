<template>
  <div class="tree-node">
    <div
      class="flex items-center py-1 px-2 rounded cursor-pointer transition-colors"
      :class="{
        'bg-blue-100 text-blue-700': currentFolderId === node.id,
        'hover:bg-gray-100': currentFolderId !== node.id
      }"
      :style="{ paddingLeft: `${node.level * 16 + 8}px` }"
      @click="$emit('node-click', node)"
      @contextmenu="$emit('node-context', $event, node)"
    >
      <!-- 展开/折叠图标 -->
      <button
        v-if="node.hasChildren"
        type="button"
        class="flex-shrink-0 mr-1 p-0.5 rounded hover:bg-gray-200"
        @click.stop="$emit('node-expand', node)"
      >
        <svg
          class="w-3 h-3 text-gray-500 transition-transform"
          :class="{ 'rotate-90': expandedNodes.has(node.id) }"
          fill="currentColor"
          viewBox="0 0 20 20"
        >
          <path fill-rule="evenodd" d="M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 111.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z" clip-rule="evenodd"/>
        </svg>
      </button>
      <div v-else class="w-4 flex-shrink-0"></div>

      <!-- 文件夹图标 -->
      <svg class="w-4 h-4 text-blue-500 mr-2 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
        <path d="M2 6a2 2 0 012-2h5l2 2h5a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V6z"/>
      </svg>

      <!-- 文件夹名称 -->
      <span class="text-sm truncate flex-1">{{ node.name }}</span>

      <!-- 文件数量 -->
      <span v-if="node.fileCount > 0" class="text-xs text-gray-500 ml-1 flex-shrink-0">
        {{ node.fileCount }}
      </span>
    </div>

    <!-- 子节点 -->
    <div v-if="expandedNodes.has(node.id) && node.children && node.children.length > 0">
      <TreeNode
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :current-folder-id="currentFolderId"
        :expanded-nodes="expandedNodes"
        @node-click="$emit('node-click', $event)"
        @node-expand="$emit('node-expand', $event)"
        @node-context="$emit('node-context', $event, child)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { FolderTreeNode } from '@/types/file'

interface Props {
  node: FolderTreeNode
  currentFolderId?: number | null
  expandedNodes: Set<number>
}

defineProps<Props>()
defineEmits<{
  'node-click': [node: FolderTreeNode]
  'node-expand': [node: FolderTreeNode]
  'node-context': [event: MouseEvent, node: FolderTreeNode]
}>()
</script>

<style scoped>
.tree-node {
  user-select: none;
}
</style> 