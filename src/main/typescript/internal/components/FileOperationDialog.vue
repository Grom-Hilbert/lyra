<template>
  <Dialog :open="open" @update:open="$emit('update:open', $event)">
    <DialogContent class="sm:max-w-lg">
      <DialogHeader>
        <DialogTitle>{{ dialogTitle }}</DialogTitle>
        <DialogDescription>
          {{ dialogDescription }}
        </DialogDescription>
      </DialogHeader>
      
      <!-- 移动/复制操作 -->
      <div v-if="operation === 'move' || operation === 'copy'" class="space-y-4">
        <div class="space-y-2">
          <label class="text-sm font-medium text-foreground">目标空间</label>
          <select
            v-model="targetSpaceId"
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
          >
            <option v-for="space in spaces" :key="space.id" :value="space.id">
              {{ space.name }}
            </option>
          </select>
        </div>

        <div class="space-y-2">
          <label class="text-sm font-medium text-foreground">目标文件夹</label>
          <select
            v-model="targetFolderId"
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
          >
            <option :value="undefined">根目录</option>
            <option v-for="folder in targetFolders" :key="folder.id" :value="folder.id">
              {{ folder.name }}
            </option>
          </select>
        </div>

        <div v-if="operation === 'copy'" class="space-y-2">
          <label class="text-sm font-medium text-foreground">新名称（可选）</label>
          <input
            v-model="newName"
            type="text"
            placeholder="留空则使用原名称"
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
          />
        </div>
      </div>

      <!-- 分享操作 -->
      <div v-else-if="operation === 'share'" class="space-y-4">
        <div class="space-y-2">
          <label class="text-sm font-medium text-foreground">访问权限</label>
          <select
            v-model="shareData.accessType"
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
          >
            <option value="read">只读</option>
            <option value="write">读写</option>
          </select>
        </div>

        <div class="space-y-2">
          <label class="text-sm font-medium text-foreground">访问密码（可选）</label>
          <input
            v-model="shareData.password"
            type="password"
            placeholder="留空则无需密码"
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
          />
        </div>

        <div class="space-y-2">
          <label class="text-sm font-medium text-foreground">过期时间（可选）</label>
          <input
            v-model="shareData.expiresAt"
            type="datetime-local"
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
          />
        </div>

        <div class="space-y-2">
          <label class="text-sm font-medium text-foreground">下载限制（可选）</label>
          <input
            v-model.number="shareData.downloadLimit"
            type="number"
            min="1"
            placeholder="留空则无限制"
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
          />
        </div>
      </div>

      <!-- 重命名操作 -->
      <div v-else-if="operation === 'rename'" class="space-y-4">
        <div class="space-y-2">
          <label class="text-sm font-medium text-foreground">新名称</label>
          <input
            v-model="newName"
            type="text"
            :placeholder="getItemName(item) || '请输入新名称'"
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
            required
          />
        </div>
      </div>

      <p v-if="error" class="text-sm text-destructive">{{ error }}</p>

      <DialogFooter>
        <button
          type="button"
          class="px-4 py-2 text-sm font-medium text-muted-foreground bg-background border border-border rounded-md hover:bg-muted focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2"
          @click="handleCancel"
        >
          取消
        </button>
        <button
          type="submit"
          class="px-4 py-2 text-sm font-medium text-primary-foreground bg-primary rounded-md hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="loading"
          @click="handleSubmit"
        >
          <div v-if="loading" class="flex items-center">
            <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-primary-foreground mr-2"></div>
            {{ loadingText }}
          </div>
          <span v-else>{{ submitText }}</span>
        </button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { fileApi, folderApi, spaceApi } from '@/apis'
import type { IFileInfo, IFolderInfo, ISpace } from '@/types'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'

interface Props {
  open: boolean
  operation: 'move' | 'copy' | 'share' | 'rename'
  item: IFileInfo | IFolderInfo | null
  itemType: 'file' | 'folder'
  currentSpaceId: number
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'success', result: any): void
  (e: 'error', error: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const loading = ref(false)
const error = ref('')
const spaces = ref<ISpace[]>([])
const targetFolders = ref<IFolderInfo[]>([])
const targetSpaceId = ref<number>()
const targetFolderId = ref<number>()
const newName = ref('')
const shareData = ref({
  accessType: 'read' as 'read' | 'write',
  password: '',
  expiresAt: '',
  downloadLimit: undefined as number | undefined
})

// 计算属性
const getItemName = (item: IFileInfo | IFolderInfo | null): string => {
  if (!item) return '项目'
  if ('filename' in item) return item.filename
  if ('name' in item) return item.name
  return '项目'
}

const dialogTitle = computed(() => {
  const itemName = getItemName(props.item)
  switch (props.operation) {
    case 'move': return `移动 ${itemName}`
    case 'copy': return `复制 ${itemName}`
    case 'share': return `分享 ${itemName}`
    case 'rename': return `重命名 ${itemName}`
    default: return '操作'
  }
})

const dialogDescription = computed(() => {
  switch (props.operation) {
    case 'move': return '选择要移动到的位置'
    case 'copy': return '选择要复制到的位置'
    case 'share': return '设置分享参数'
    case 'rename': return '输入新的名称'
    default: return ''
  }
})

const submitText = computed(() => {
  switch (props.operation) {
    case 'move': return '移动'
    case 'copy': return '复制'
    case 'share': return '创建分享'
    case 'rename': return '重命名'
    default: return '确定'
  }
})

const loadingText = computed(() => {
  switch (props.operation) {
    case 'move': return '移动中...'
    case 'copy': return '复制中...'
    case 'share': return '创建中...'
    case 'rename': return '重命名中...'
    default: return '处理中...'
  }
})

// 方法
const loadSpaces = async () => {
  try {
    const response = await spaceApi.getUserSpaces()
    if (response.success && response.data) {
      spaces.value = response.data
    }
  } catch (err) {
    console.error('加载空间列表失败:', err)
  }
}

const loadTargetFolders = async () => {
  if (!targetSpaceId.value) return
  
  try {
    const response = await folderApi.getFolders({
      spaceId: targetSpaceId.value
    })
    if (response.success && response.data) {
      targetFolders.value = response.data || []
    }
  } catch (err) {
    console.error('加载文件夹列表失败:', err)
  }
}

const handleSubmit = async () => {
  if (!props.item) return
  
  loading.value = true
  error.value = ''
  
  try {
    let result
    
    switch (props.operation) {
      case 'move':
        result = await handleMove()
        break
      case 'copy':
        result = await handleCopy()
        break
      case 'share':
        result = await handleShare()
        break
      case 'rename':
        result = await handleRename()
        break
    }
    
    if (result) {
      emit('success', result)
      handleCancel()
    }
  } catch (err: any) {
    console.error(`${props.operation} 操作失败:`, err)
    error.value = err.response?.data?.message || err.message || '操作失败'
  } finally {
    loading.value = false
  }
}

const handleMove = async () => {
  if (!props.item || !targetSpaceId.value) return null
  
  if (props.itemType === 'file') {
    const response = await fileApi.moveFile(props.item.id, {
      targetSpaceId: targetSpaceId.value,
      targetFolderId: targetFolderId.value
    })
    return response.data
  } else {
    const response = await folderApi.moveFolder(props.item.id, {
      targetSpaceId: targetSpaceId.value,
      targetParentFolderId: targetFolderId.value
    })
    return response.data
  }
}

const handleCopy = async () => {
  if (!props.item || !targetSpaceId.value) return null
  
  if (props.itemType === 'file') {
    const response = await fileApi.copyFile(props.item.id, {
      targetSpaceId: targetSpaceId.value,
      targetFolderId: targetFolderId.value,
      newName: newName.value || undefined
    })
    return response.data
  } else {
    // 文件夹复制需要后端支持
    throw new Error('文件夹复制功能暂未实现')
  }
}

const handleShare = async () => {
  if (!props.item) return null
  
  const shareRequest = {
    accessType: shareData.value.accessType,
    password: shareData.value.password || undefined,
    expiresAt: shareData.value.expiresAt || undefined,
    downloadLimit: shareData.value.downloadLimit
  }
  
  if (props.itemType === 'file') {
    const response = await fileApi.createFileShare(props.item.id, shareRequest)
    return response.data
  } else {
    const response = await folderApi.createFolderShare(props.item.id, shareRequest)
    return response.data
  }
}

const handleRename = async () => {
  if (!props.item || !newName.value.trim()) return null
  
  if (props.itemType === 'file') {
    const response = await fileApi.renameFile(props.item.id, { newName: newName.value.trim() })
    return response.data
  } else {
    const response = await folderApi.updateFolder(props.item.id, { name: newName.value.trim() })
    return response.data
  }
}

const handleCancel = () => {
  emit('update:open', false)
  resetForm()
}

const resetForm = () => {
  targetSpaceId.value = props.currentSpaceId
  targetFolderId.value = undefined
  newName.value = ''
  shareData.value = {
    accessType: 'read',
    password: '',
    expiresAt: '',
    downloadLimit: undefined
  }
  error.value = ''
  loading.value = false
}

// 监听
watch(() => props.open, async (newOpen) => {
  if (newOpen) {
    resetForm()
    await loadSpaces()
    if (props.operation === 'move' || props.operation === 'copy') {
      await loadTargetFolders()
    }
    if (props.operation === 'rename' && props.item) {
      newName.value = getItemName(props.item)
    }
  }
})

watch(targetSpaceId, loadTargetFolders)
</script>

<style scoped>
/* 自定义样式 */
</style>
