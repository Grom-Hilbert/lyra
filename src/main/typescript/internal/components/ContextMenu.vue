<template>
  <div
    v-if="visible"
    ref="menuRef"
    class="fixed z-50 bg-card border border-border rounded-md shadow-lg py-1 min-w-[160px]"
    :style="{ left: `${x}px`, top: `${y}px` }"
    @click.stop
  >
    <!-- 文件操作菜单 -->
    <template v-if="itemType === 'file'">
      <button
        class="w-full px-3 py-2 text-left text-sm text-foreground hover:bg-muted flex items-center"
        @click="handleAction('open')"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
        </svg>
        打开
      </button>
      
      <button
        class="w-full px-3 py-2 text-left text-sm text-foreground hover:bg-muted flex items-center"
        @click="handleAction('download')"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
        </svg>
        下载
      </button>
      
      <div class="border-t border-border my-1"></div>
      
      <button
        class="w-full px-3 py-2 text-left text-sm text-foreground hover:bg-muted flex items-center"
        @click="handleAction('rename')"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
        </svg>
        重命名
      </button>
      
      <button
        class="w-full px-3 py-2 text-left text-sm text-foreground hover:bg-muted flex items-center"
        @click="handleAction('move')"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4"/>
        </svg>
        移动
      </button>
      
      <button
        class="w-full px-3 py-2 text-left text-sm text-foreground hover:bg-muted flex items-center"
        @click="handleAction('copy')"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"/>
        </svg>
        复制
      </button>
      
      <button
        class="w-full px-3 py-2 text-left text-sm text-foreground hover:bg-muted flex items-center"
        @click="handleAction('share')"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z"/>
        </svg>
        分享
      </button>
      
      <div class="border-t border-border my-1"></div>
      
      <button
        class="w-full px-3 py-2 text-left text-sm text-destructive hover:bg-destructive/5 flex items-center"
        @click="handleAction('delete')"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
        </svg>
        删除
      </button>
    </template>

    <!-- 文件夹操作菜单 -->
    <template v-else-if="itemType === 'folder'">
      <button
        class="w-full px-3 py-2 text-left text-sm text-foreground hover:bg-muted flex items-center"
        @click="handleAction('open')"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"/>
        </svg>
        打开
      </button>
      
      <div class="border-t border-border my-1"></div>
      
      <button
        class="w-full px-3 py-2 text-left text-sm text-foreground hover:bg-muted flex items-center"
        @click="handleAction('rename')"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
        </svg>
        重命名
      </button>
      
      <button
        class="w-full px-3 py-2 text-left text-sm text-foreground hover:bg-muted flex items-center"
        @click="handleAction('move')"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7h12m0 0l-4-4m4 4l-4 4m0 6H4m0 0l4 4m-4-4l4-4"/>
        </svg>
        移动
      </button>
      
      <button
        class="w-full px-3 py-2 text-left text-sm text-foreground hover:bg-muted flex items-center"
        @click="handleAction('share')"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z"/>
        </svg>
        分享
      </button>
      
      <div class="border-t border-border my-1"></div>
      
      <button
        class="w-full px-3 py-2 text-left text-sm text-destructive hover:bg-destructive/5 flex items-center"
        @click="handleAction('delete')"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
        </svg>
        删除
      </button>
    </template>

    <!-- 空白区域菜单 -->
    <template v-else-if="itemType === 'empty'">
      <button
        class="w-full px-3 py-2 text-left text-sm text-foreground hover:bg-muted flex items-center"
        @click="handleAction('upload')"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        上传文件
      </button>
      
      <button
        class="w-full px-3 py-2 text-left text-sm text-foreground hover:bg-muted flex items-center"
        @click="handleAction('createFolder')"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6v6m0 0v6m0-6h6m-6 0H6"/>
        </svg>
        新建文件夹
      </button>
      
      <div class="border-t border-border my-1"></div>
      
      <button
        class="w-full px-3 py-2 text-left text-sm text-foreground hover:bg-muted flex items-center"
        @click="handleAction('refresh')"
      >
        <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
        </svg>
        刷新
      </button>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted, onUnmounted } from 'vue'
import type { IFileInfo, IFolderInfo } from '@/types'

interface Props {
  visible: boolean
  x: number
  y: number
  item: IFileInfo | IFolderInfo | null
  itemType: 'file' | 'folder' | 'empty'
}

interface Emits {
  (e: 'action', action: string, item?: IFileInfo | IFolderInfo | null): void
  (e: 'close'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const menuRef = ref<HTMLElement>()

const handleAction = (action: string) => {
  emit('action', action, props.item)
  emit('close')
}

const handleClickOutside = (event: MouseEvent) => {
  if (menuRef.value && !menuRef.value.contains(event.target as Node)) {
    emit('close')
  }
}

const handleEscape = (event: KeyboardEvent) => {
  if (event.key === 'Escape') {
    emit('close')
  }
}

onMounted(() => {
  nextTick(() => {
    document.addEventListener('click', handleClickOutside)
    document.addEventListener('keydown', handleEscape)
  })
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleEscape)
})
</script>

<style scoped>
/* 自定义样式 */
</style>
