<template>
  <component :is="iconComponent" :class="iconClass" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  FileText,
  FileImage,
  FileVideo,
  FileAudio,
  FileArchive,
  FileCode,
  File,
  FileSpreadsheet,
  FilePresentation,
  FilePdf
} from 'lucide-vue-next'

interface Props {
  fileName: string
  mimeType?: string
  size?: 'sm' | 'md' | 'lg'
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md'
})

// 根据文件扩展名或MIME类型确定图标
const iconComponent = computed(() => {
  const extension = props.fileName.split('.').pop()?.toLowerCase() || ''
  const mimeType = props.mimeType?.toLowerCase() || ''

  // 根据MIME类型判断
  if (mimeType.startsWith('image/')) return FileImage
  if (mimeType.startsWith('video/')) return FileVideo
  if (mimeType.startsWith('audio/')) return FileAudio
  if (mimeType === 'application/pdf') return FilePdf

  // 根据文件扩展名判断
  switch (extension) {
    // 图片
    case 'jpg':
    case 'jpeg':
    case 'png':
    case 'gif':
    case 'bmp':
    case 'svg':
    case 'webp':
      return FileImage

    // 视频
    case 'mp4':
    case 'avi':
    case 'mov':
    case 'wmv':
    case 'flv':
    case 'webm':
    case 'mkv':
      return FileVideo

    // 音频
    case 'mp3':
    case 'wav':
    case 'flac':
    case 'aac':
    case 'ogg':
    case 'wma':
      return FileAudio

    // 压缩包
    case 'zip':
    case 'rar':
    case '7z':
    case 'tar':
    case 'gz':
    case 'bz2':
      return FileArchive

    // 代码文件
    case 'js':
    case 'ts':
    case 'jsx':
    case 'tsx':
    case 'vue':
    case 'html':
    case 'css':
    case 'scss':
    case 'sass':
    case 'less':
    case 'json':
    case 'xml':
    case 'yaml':
    case 'yml':
    case 'java':
    case 'py':
    case 'cpp':
    case 'c':
    case 'h':
    case 'cs':
    case 'php':
    case 'rb':
    case 'go':
    case 'rs':
    case 'swift':
    case 'kt':
    case 'scala':
    case 'sh':
    case 'bat':
    case 'ps1':
      return FileCode

    // 表格文件
    case 'xlsx':
    case 'xls':
    case 'csv':
    case 'ods':
      return FileSpreadsheet

    // 演示文稿
    case 'pptx':
    case 'ppt':
    case 'odp':
      return FilePresentation

    // PDF
    case 'pdf':
      return FilePdf

    // 文档
    case 'docx':
    case 'doc':
    case 'odt':
    case 'rtf':
    case 'txt':
    case 'md':
      return FileText

    // 默认
    default:
      return File
  }
})

// 图标样式类
const iconClass = computed(() => {
  const baseClass = getIconColor()
  const sizeClass = {
    sm: 'h-4 w-4',
    md: 'h-5 w-5',
    lg: 'h-6 w-6'
  }[props.size]

  return `${baseClass} ${sizeClass}`
})

// 根据文件类型获取颜色
const getIconColor = (): string => {
  const extension = props.fileName.split('.').pop()?.toLowerCase() || ''
  const mimeType = props.mimeType?.toLowerCase() || ''

  // 根据MIME类型或扩展名设置颜色
  if (mimeType.startsWith('image/') || ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp'].includes(extension)) {
    return 'text-green-500'
  }
  
  if (mimeType.startsWith('video/') || ['mp4', 'avi', 'mov', 'wmv', 'flv', 'webm', 'mkv'].includes(extension)) {
    return 'text-purple-500'
  }
  
  if (mimeType.startsWith('audio/') || ['mp3', 'wav', 'flac', 'aac', 'ogg', 'wma'].includes(extension)) {
    return 'text-pink-500'
  }
  
  if (['zip', 'rar', '7z', 'tar', 'gz', 'bz2'].includes(extension)) {
    return 'text-orange-500'
  }
  
  if (['js', 'ts', 'jsx', 'tsx', 'vue', 'html', 'css', 'scss', 'sass', 'less', 'json', 'xml', 'yaml', 'yml', 'java', 'py', 'cpp', 'c', 'h', 'cs', 'php', 'rb', 'go', 'rs', 'swift', 'kt', 'scala', 'sh', 'bat', 'ps1'].includes(extension)) {
    return 'text-blue-500'
  }
  
  if (['xlsx', 'xls', 'csv', 'ods'].includes(extension)) {
    return 'text-emerald-500'
  }
  
  if (['pptx', 'ppt', 'odp'].includes(extension)) {
    return 'text-red-500'
  }
  
  if (extension === 'pdf' || mimeType === 'application/pdf') {
    return 'text-red-600'
  }
  
  if (['docx', 'doc', 'odt', 'rtf', 'txt', 'md'].includes(extension)) {
    return 'text-blue-600'
  }
  
  return 'text-gray-500'
}
</script>
