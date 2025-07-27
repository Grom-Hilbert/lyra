<template>
  <Dialog :open="open" @update:open="$emit('update:open', $event)">
    <DialogContent class="sm:max-w-md">
      <DialogHeader>
        <DialogTitle>新建文件夹</DialogTitle>
        <DialogDescription>
          在当前位置创建一个新的文件夹
        </DialogDescription>
      </DialogHeader>
      
      <form @submit.prevent="handleSubmit" class="space-y-4">
        <div class="space-y-2">
          <label for="folderName" class="text-sm font-medium text-foreground">
            文件夹名称
          </label>
          <input
            id="folderName"
            v-model="folderName"
            type="text"
            placeholder="请输入文件夹名称"
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
            :class="{
              'border-destructive focus:ring-destructive': error
            }"
            required
            maxlength="255"
            @input="clearError"
          />
          <p v-if="error" class="text-sm text-destructive">{{ error }}</p>
          <p class="text-xs text-muted-foreground">
            文件夹名称不能包含以下字符：/ \ : * ? " &lt; &gt; |
          </p>
        </div>

        <div class="space-y-2">
          <label for="folderDescription" class="text-sm font-medium text-foreground">
            描述（可选）
          </label>
          <textarea
            id="folderDescription"
            v-model="folderDescription"
            placeholder="请输入文件夹描述"
            rows="3"
            class="w-full px-3 py-2 border border-border rounded-md bg-background text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent resize-none"
            maxlength="500"
          ></textarea>
          <p class="text-xs text-muted-foreground text-right">
            {{ folderDescription.length }}/500
          </p>
        </div>
      </form>

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
          :disabled="!isValid || loading"
          @click="handleSubmit"
        >
          <div v-if="loading" class="flex items-center">
            <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-primary-foreground mr-2"></div>
            创建中...
          </div>
          <span v-else>创建文件夹</span>
        </button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { folderApi } from '@/apis'
import type { CreateFolderRequest } from '@/types'
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
  spaceId: number
  parentFolderId?: number
}

interface Emits {
  (e: 'update:open', value: boolean): void
  (e: 'success', folder: any): void
  (e: 'error', error: string): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式数据
const folderName = ref('')
const folderDescription = ref('')
const loading = ref(false)
const error = ref('')

// 计算属性
const isValid = computed(() => {
  return folderName.value.trim().length > 0 && !hasInvalidChars(folderName.value)
})

// 方法
const hasInvalidChars = (name: string): boolean => {
  const invalidChars = /[\/\\:*?"<>|]/
  return invalidChars.test(name)
}

const clearError = () => {
  error.value = ''
}

const validateFolderName = (): boolean => {
  const name = folderName.value.trim()
  
  if (!name) {
    error.value = '文件夹名称不能为空'
    return false
  }
  
  if (name.length > 255) {
    error.value = '文件夹名称不能超过255个字符'
    return false
  }
  
  if (hasInvalidChars(name)) {
    error.value = '文件夹名称包含非法字符'
    return false
  }
  
  // 检查是否为保留名称
  const reservedNames = ['CON', 'PRN', 'AUX', 'NUL', 'COM1', 'COM2', 'COM3', 'COM4', 'COM5', 'COM6', 'COM7', 'COM8', 'COM9', 'LPT1', 'LPT2', 'LPT3', 'LPT4', 'LPT5', 'LPT6', 'LPT7', 'LPT8', 'LPT9']
  if (reservedNames.includes(name.toUpperCase())) {
    error.value = '文件夹名称不能使用系统保留名称'
    return false
  }
  
  return true
}

const handleSubmit = async () => {
  if (!validateFolderName()) {
    return
  }
  
  loading.value = true
  error.value = ''
  
  try {
    const request: CreateFolderRequest = {
      name: folderName.value.trim(),
      spaceId: props.spaceId,
      parentFolderId: props.parentFolderId
    }
    
    if (folderDescription.value.trim()) {
      (request as any).description = folderDescription.value.trim()
    }
    
    const response = await folderApi.createFolder(request)
    
    if (response.success && response.data) {
      emit('success', response.data)
      handleCancel() // 关闭对话框并重置表单
    } else {
      error.value = response.message || '创建文件夹失败'
    }
  } catch (err: any) {
    console.error('创建文件夹失败:', err)
    error.value = err.response?.data?.message || err.message || '创建文件夹失败'
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  emit('update:open', false)
  resetForm()
}

const resetForm = () => {
  folderName.value = ''
  folderDescription.value = ''
  error.value = ''
  loading.value = false
}

// 监听对话框打开状态，重置表单
watch(() => props.open, (newOpen) => {
  if (newOpen) {
    resetForm()
  }
})
</script>

<style scoped>
/* 自定义样式 */
</style>
