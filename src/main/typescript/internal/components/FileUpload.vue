<template>
  <div class="file-upload">
    <!-- 拖拽上传区域 -->
    <div
      ref="dropZone"
      class="border-2 border-dashed transition-colors duration-200 rounded-lg p-8"
      :class="{
        'border-primary bg-primary/5': isDragOver,
        'border-border hover:border-primary/50': !isDragOver && !disabled,
        'border-muted bg-muted/50': disabled
      }"
      @drop="handleDrop"
      @dragover="handleDragOver"
      @dragenter="handleDragEnter"
      @dragleave="handleDragLeave"
      @click="openFileDialog"
    >
      <div class="text-center">
        <!-- 上传图标 -->
        <div class="mx-auto flex items-center justify-center w-12 h-12 rounded-full bg-primary/10 mb-4">
          <svg class="w-6 h-6 text-primary" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/>
          </svg>
        </div>
        
        <!-- 上传文本 -->
        <div class="text-lg font-medium text-foreground mb-2">
          拖拽文件到此处或点击上传
        </div>
        <div class="text-sm text-muted-foreground mb-4">
          支持 {{ allowedTypes.join(', ') }} 格式文件
          <br>
          单文件最大 {{ formatFileSize(maxFileSize) }}
        </div>
        
        <!-- 上传按钮 -->
        <button
          type="button"
          class="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md text-primary-foreground bg-primary hover:bg-primary/90 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-primary disabled:opacity-50 disabled:cursor-not-allowed"
          :disabled="disabled"
          @click.stop="openFileDialog"
        >
          <svg class="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
          选择文件
        </button>
      </div>
    </div>

    <!-- 隐藏的文件输入 -->
    <input
      ref="fileInput"
      type="file"
      class="hidden"
      :multiple="multiple"
      :accept="acceptedFileTypes"
      @change="handleFileInput"
    />

    <!-- 上传文件列表 -->
    <div v-if="uploadFiles.length > 0" class="mt-6">
      <h3 class="text-lg font-medium text-gray-900 mb-4">上传文件 ({{ uploadFiles.length }})</h3>
      
      <div class="space-y-3">
        <div
          v-for="file in uploadFiles"
          :key="file.id"
          class="flex items-center p-4 border border-gray-200 rounded-lg bg-white"
        >
          <!-- 文件图标 -->
          <div class="flex-shrink-0 mr-4">
            <div class="w-10 h-10 rounded-lg bg-gray-100 flex items-center justify-center">
              <svg class="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
            </div>
          </div>

          <!-- 文件信息 -->
          <div class="flex-1 min-w-0">
            <div class="flex items-center justify-between">
              <div class="truncate">
                <p class="text-sm font-medium text-gray-900 truncate">{{ file.name }}</p>
                                 <p class="text-sm text-gray-500">{{ formatFileSize(file.size || 0) }}</p>
              </div>
              <div class="flex items-center space-x-2 ml-4">
                <!-- 状态指示器 -->
                <div v-if="file.status === 'uploading'" class="flex items-center">
                  <div class="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-600"></div>
                  <span class="ml-2 text-sm text-blue-600">{{ file.progress }}%</span>
                </div>
                <div v-else-if="file.status === 'completed'" class="flex items-center text-green-600">
                  <svg class="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
                  </svg>
                  <span class="text-sm">完成</span>
                </div>
                <div v-else-if="file.status === 'error'" class="flex items-center text-red-600">
                  <svg class="w-4 h-4 mr-1" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                  </svg>
                  <span class="text-sm">失败</span>
                </div>
                <div v-else class="text-sm text-gray-500">等待中</div>

                <!-- 删除按钮 -->
                <button
                  type="button"
                  class="p-1 text-gray-400 hover:text-red-500 transition-colors"
                  @click="removeFile(file.id)"
                  :disabled="file.status === 'uploading'"
                >
                  <svg class="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                    <path fill-rule="evenodd" d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" clip-rule="evenodd"/>
                  </svg>
                </button>
              </div>
            </div>

            <!-- 进度条 -->
            <div v-if="file.status === 'uploading'" class="mt-2">
              <div class="w-full bg-gray-200 rounded-full h-2">
                <div
                  class="bg-blue-600 h-2 rounded-full transition-all duration-300"
                  :style="{ width: `${file.progress}%` }"
                ></div>
              </div>
            </div>

            <!-- 错误信息 -->
            <div v-if="file.status === 'error' && file.error" class="mt-2">
              <p class="text-sm text-red-600">{{ file.error }}</p>
            </div>
          </div>
        </div>
      </div>

      <!-- 批量操作按钮 -->
      <div class="flex items-center justify-between mt-6">
        <div class="text-sm text-gray-500">
          总计 {{ uploadFiles.length }} 个文件，
          {{ formatFileSize(totalSize) }}
        </div>
        <div class="flex space-x-3">
          <button
            type="button"
            class="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            @click="clearAll"
            :disabled="isUploading"
          >
            清空列表
          </button>
          <button
            type="button"
            class="px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
            @click="startUpload"
            :disabled="isUploading || uploadFiles.length === 0"
          >
            <svg v-if="isUploading" class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            {{ isUploading ? '上传中...' : '开始上传' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, defineEmits, defineProps } from 'vue'
import { fileApi, fileUtils } from '@/apis/fileApi'
import type { IFileInfo, FileUploadRequest } from '@/types/index'

interface Props {
  spaceId: number
  folderId?: number
  multiple?: boolean
  maxFileSize?: number
  allowedTypes?: string[]
  disabled?: boolean
}

interface LocalUploadProgress {
  id: string
  filename: string
  name: string
  progress: number
  status: 'pending' | 'uploading' | 'success' | 'error' | 'completed'
  file: File
  size: number
  error?: string
}

interface Emits {
  (e: 'upload-success', files: IFileInfo[]): void
  (e: 'upload-error', error: string): void
  (e: 'upload-progress', progress: LocalUploadProgress[]): void
}

const props = withDefaults(defineProps<Props>(), {
  multiple: true,
  maxFileSize: 100 * 1024 * 1024, // 100MB
  allowedTypes: () => ['jpg', 'jpeg', 'png', 'gif', 'pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt', 'zip', 'rar'],
  disabled: false
})

const emit = defineEmits<Emits>()

// 响应式数据
const dropZone = ref<HTMLElement>()
const fileInput = ref<HTMLInputElement>()
const isDragOver = ref(false)
const uploadFiles = ref<LocalUploadProgress[]>([])
const isUploading = ref(false)

// 计算属性
const acceptedFileTypes = computed(() => {
  return props.allowedTypes.map(type => `.${type}`).join(',')
})

const totalSize = computed(() => {
  return uploadFiles.value.reduce((sum: number, file: LocalUploadProgress) => sum + (file.size || 0), 0)
})

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  return fileUtils.formatFileSize(bytes)
}

// 拖拽事件处理
const handleDragOver = (e: DragEvent) => {
  e.preventDefault()
  e.stopPropagation()
}

const handleDragEnter = (e: DragEvent) => {
  e.preventDefault()
  e.stopPropagation()
  isDragOver.value = true
}

const handleDragLeave = (e: DragEvent) => {
  e.preventDefault()
  e.stopPropagation()
  
  // 只有在离开拖拽区域时才取消高亮
  if (!dropZone.value?.contains(e.relatedTarget as Node)) {
    isDragOver.value = false
  }
}

const handleDrop = (e: DragEvent) => {
  e.preventDefault()
  e.stopPropagation()
  isDragOver.value = false

  if (props.disabled) return

  const files = Array.from(e.dataTransfer?.files || [])
  addFiles(files)
}

// 文件选择处理
const openFileDialog = () => {
  if (props.disabled) return
  fileInput.value?.click()
}

const handleFileInput = (e: Event) => {
  const input = e.target as HTMLInputElement
  const files = Array.from(input.files || [])
  addFiles(files)
  
  // 清空输入框以允许重复选择同一文件
  input.value = ''
}

// 添加文件到上传列表
const addFiles = (files: File[]) => {
  const validFiles = files.filter(file => validateFile(file))
  
  const newUploadFiles = validFiles.map(file => ({
    id: `${Date.now()}-${Math.random()}`,
    filename: file.name,
    name: file.name,
    progress: 0,
    status: 'pending' as const,
    file,
    size: file.size
  }))

  uploadFiles.value.push(...newUploadFiles)
}

// 验证文件
const validateFile = (file: File): boolean => {
  // 检查文件大小
  if (file.size > props.maxFileSize) {
    alert(`文件 "${file.name}" 大小超过限制 (${formatFileSize(props.maxFileSize)})`)
    return false
  }

  // 检查文件类型
  const extension = fileUtils.getFileExtension(file.name)
  if (!props.allowedTypes.includes(extension)) {
    alert(`文件 "${file.name}" 类型不支持`)
    return false
  }

  return true
}

// 移除文件
const removeFile = (fileId: string) => {
  const index = uploadFiles.value.findIndex((f: LocalUploadProgress) => f.id === fileId)
  if (index > -1) {
    uploadFiles.value.splice(index, 1)
  }
}

// 清空所有文件
const clearAll = () => {
  uploadFiles.value = []
}

// 开始上传
const startUpload = async () => {
  if (uploadFiles.value.length === 0 || isUploading.value) return

  isUploading.value = true
  const uploadPromises: Promise<void>[] = []
  const successFiles: IFileInfo[] = []

  for (const uploadFile of uploadFiles.value) {
    if (uploadFile.status === 'completed') continue

    uploadFile.status = 'uploading'
    uploadFile.progress = 0

    const uploadPromise = uploadSingleFile(uploadFile)
      .then((fileInfo) => {
        uploadFile.status = 'completed'
        uploadFile.progress = 100
        if (fileInfo) {
          successFiles.push(fileInfo)
        }
      })
      .catch((error) => {
        uploadFile.status = 'error'
        uploadFile.error = error.message || '上传失败'
      })

    uploadPromises.push(uploadPromise)
  }

  try {
    await Promise.all(uploadPromises)
    
    if (successFiles.length > 0) {
      emit('upload-success', successFiles)
    }
    
    // 移除成功上传的文件
    uploadFiles.value = uploadFiles.value.filter(f => f.status !== 'completed')
    
  } catch (error) {
    emit('upload-error', '部分文件上传失败')
  } finally {
    isUploading.value = false
  }
}

// 上传单个文件
const uploadSingleFile = async (uploadFile: LocalUploadProgress): Promise<IFileInfo | null> => {
  if (!uploadFile.file) return null

  const formData = new FormData()
  formData.append('file', uploadFile.file)
  formData.append('spaceId', props.spaceId.toString())
  
  if (props.folderId) {
    formData.append('folderId', props.folderId.toString())
  }

  try {
    const uploadRequest: FileUploadRequest = {
      file: uploadFile.file,
      spaceId: props.spaceId,
      folderId: props.folderId
    }

    const response = await fileApi.upload(uploadRequest, (progress) => {
      uploadFile.progress = progress
      emit('upload-progress', uploadFiles.value)
    })

    if (response.success && response.data) {
      return response.data
    }
    throw new Error('上传失败')
  } catch (error) {
    throw error
  }
}
</script>

<style scoped>
.file-upload {
  width: 100%;
}
</style> 