<template>
  <div class="container mx-auto p-6">
    <!-- 页面标题和搜索框 -->
    <div class="mb-6">
      <div class="flex items-center justify-between mb-4">
        <h1 class="text-2xl font-bold">搜索结果</h1>
        <Button
          variant="outline"
          @click="showAdvancedSearch = !showAdvancedSearch"
        >
          <Filter class="mr-2 h-4 w-4" />
          高级搜索
        </Button>
      </div>
      
      <!-- 搜索框 -->
      <SearchBox
        :space-id="currentSpaceId"
        @search="handleSearch"
        @clear="handleClear"
      />
    </div>

    <!-- 高级搜索面板 -->
    <Card v-if="showAdvancedSearch" class="mb-6">
      <CardHeader>
        <CardTitle>高级搜索</CardTitle>
      </CardHeader>
      <CardContent>
        <form @submit.prevent="performAdvancedSearch" class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <!-- 文件类型 -->
            <div>
              <Label>文件类型</Label>
              <div class="flex flex-wrap gap-2 mt-2">
                <Badge
                  v-for="type in fileTypes"
                  :key="type.value"
                  :variant="advancedFilters.fileType.includes(type.value) ? 'default' : 'outline'"
                  class="cursor-pointer"
                  @click="toggleFileType(type.value)"
                >
                  {{ type.label }}
                </Badge>
              </div>
            </div>

            <!-- 文件大小 -->
            <div>
              <Label>文件大小</Label>
              <div class="flex items-center space-x-2 mt-2">
                <Input
                  v-model.number="advancedFilters.fileSize.min"
                  type="number"
                  placeholder="最小 (MB)"
                  class="w-24"
                />
                <span>-</span>
                <Input
                  v-model.number="advancedFilters.fileSize.max"
                  type="number"
                  placeholder="最大 (MB)"
                  class="w-24"
                />
              </div>
            </div>

            <!-- 创建日期 -->
            <div>
              <Label>创建日期</Label>
              <div class="flex items-center space-x-2 mt-2">
                <Input
                  v-model="advancedFilters.dateCreated.start"
                  type="date"
                  class="w-36"
                />
                <span>-</span>
                <Input
                  v-model="advancedFilters.dateCreated.end"
                  type="date"
                  class="w-36"
                />
              </div>
            </div>

            <!-- 修改日期 -->
            <div>
              <Label>修改日期</Label>
              <div class="flex items-center space-x-2 mt-2">
                <Input
                  v-model="advancedFilters.dateModified.start"
                  type="date"
                  class="w-36"
                />
                <span>-</span>
                <Input
                  v-model="advancedFilters.dateModified.end"
                  type="date"
                  class="w-36"
                />
              </div>
            </div>
          </div>

          <div class="flex items-center space-x-2">
            <Button type="submit" :disabled="isSearching">
              <Search class="mr-2 h-4 w-4" />
              搜索
            </Button>
            <Button type="button" variant="outline" @click="resetFilters">
              重置
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>

    <!-- 搜索统计 -->
    <div v-if="searchResults" class="mb-4 flex items-center justify-between">
      <div class="text-sm text-muted-foreground">
        找到 {{ resultStats.total }} 个结果
        <span v-if="searchResults.searchTime">({{ searchResults.searchTime }}ms)</span>
      </div>
      <div class="flex items-center space-x-2">
        <Badge variant="secondary">{{ resultStats.files }} 个文件</Badge>
        <Badge variant="secondary">{{ resultStats.folders }} 个文件夹</Badge>
      </div>
    </div>

    <!-- 搜索结果 -->
    <div v-if="isSearching" class="flex items-center justify-center py-12">
      <div class="text-center">
        <div class="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent mx-auto mb-2"></div>
        <p class="text-muted-foreground">搜索中...</p>
      </div>
    </div>

    <div v-else-if="searchError" class="text-center py-12">
      <AlertCircle class="h-12 w-12 text-destructive mx-auto mb-4" />
      <h3 class="text-lg font-semibold mb-2">搜索失败</h3>
      <p class="text-muted-foreground">{{ searchError }}</p>
    </div>

    <div v-else-if="!hasResults && currentQuery && !isSearching" class="text-center py-12">
      <Search class="h-12 w-12 text-muted-foreground mx-auto mb-4" />
      <h3 class="text-lg font-semibold mb-2">未找到结果</h3>
      <p class="text-muted-foreground">尝试使用不同的关键词或调整搜索条件</p>
    </div>

    <div v-else-if="searchResults && !isSearching" class="space-y-6">
      <!-- 文件夹结果 -->
      <div v-if="searchResults.folders.length > 0">
        <h3 class="text-lg font-semibold mb-3 flex items-center">
          <Folder class="mr-2 h-5 w-5" />
          文件夹 ({{ searchResults.folders.length }})
        </h3>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <Card
            v-for="folder in searchResults.folders"
            :key="folder.id"
            class="cursor-pointer hover:shadow-md transition-shadow"
            @click="openFolder(folder)"
          >
            <CardContent class="p-4">
              <div class="flex items-center space-x-3">
                <Folder class="h-8 w-8 text-blue-500" />
                <div class="flex-1 min-w-0">
                  <h4 class="font-medium truncate" v-html="highlightText(folder.name, currentQuery)"></h4>
                  <p class="text-sm text-muted-foreground">{{ folder.path }}</p>
                  <p class="text-xs text-muted-foreground">
                    {{ formatDate(folder.updatedAt) }}
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      <!-- 文件结果 -->
      <div v-if="searchResults.files.length > 0">
        <h3 class="text-lg font-semibold mb-3 flex items-center">
          <FileText class="mr-2 h-5 w-5" />
          文件 ({{ searchResults.files.length }})
        </h3>
        <div class="space-y-2">
          <Card
            v-for="file in searchResults.files"
            :key="file.id"
            class="cursor-pointer hover:shadow-md transition-shadow"
            @click="openFile(file)"
          >
            <CardContent class="p-4">
              <div class="flex items-center space-x-3">
                <div class="flex-shrink-0">
                  <FileIcon :file-name="file.name" class="h-8 w-8" />
                </div>
                <div class="flex-1 min-w-0">
                  <h4 class="font-medium truncate" v-html="highlightText(file.name, currentQuery)"></h4>
                  <p class="text-sm text-muted-foreground">{{ file.path }}</p>
                  <div class="flex items-center space-x-4 text-xs text-muted-foreground mt-1">
                    <span>{{ formatFileSize(file.size) }}</span>
                    <span>{{ formatDate(file.updatedAt) }}</span>
                    <span v-if="file.mimeType">{{ file.mimeType }}</span>
                  </div>
                </div>
                <div class="flex-shrink-0">
                  <Button variant="ghost" size="sm" @click.stop="downloadFile(file)">
                    <Download class="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!currentQuery && !isSearching" class="text-center py-12">
      <Search class="h-12 w-12 text-muted-foreground mx-auto mb-4" />
      <h3 class="text-lg font-semibold mb-2">开始搜索</h3>
      <p class="text-muted-foreground">输入关键词搜索文件和文件夹</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { 
  Search, 
  Filter, 
  Folder, 
  FileText, 
  Download, 
  AlertCircle 
} from 'lucide-vue-next'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import SearchBox from '@/components/SearchBox.vue'
import FileIcon from '@/components/FileIcon.vue'
import { useSearchStore } from '@/stores/search'
import type { IFileInfo, IFolderInfo } from '@/types/index'

const route = useRoute()
const router = useRouter()
const searchStore = useSearchStore()

// 响应式数据
const showAdvancedSearch = ref(false)
const currentSpaceId = ref<number | null>(null)

// 计算属性
const searchResults = computed(() => searchStore.searchResults)
const isSearching = computed(() => searchStore.isSearching)
const searchError = computed(() => searchStore.searchError)
const hasResults = computed(() => searchStore.hasResults)
const resultStats = computed(() => searchStore.resultStats)
const currentQuery = computed(() => searchStore.currentKeyword)
const advancedFilters = computed(() => searchStore.advancedFilters)

// 文件类型选项
const fileTypes = [
  { label: '文档', value: 'document' },
  { label: '图片', value: 'image' },
  { label: '视频', value: 'video' },
  { label: '音频', value: 'audio' },
  { label: '压缩包', value: 'archive' },
  { label: '代码', value: 'code' }
]

// 方法
const handleSearch = async (query: string) => {
  await searchStore.quickSearch(query, currentSpaceId.value)
}

const handleClear = () => {
  searchStore.clearResults()
  router.push({ name: 'Search' })
}

const performAdvancedSearch = async () => {
  if (!currentQuery.value) return

  await searchStore.advancedSearch({
    query: currentQuery.value,
    spaceId: currentSpaceId.value || undefined,
    filters: advancedFilters.value
  })
}

const toggleFileType = (type: string) => {
  const types = [...advancedFilters.value.fileType]
  const index = types.indexOf(type)
  
  if (index > -1) {
    types.splice(index, 1)
  } else {
    types.push(type)
  }
  
  searchStore.setAdvancedFilters({ fileType: types })
}

const resetFilters = () => {
  searchStore.resetAdvancedFilters()
}

const highlightText = (text: string, query: string): string => {
  if (!query) return text
  
  const regex = new RegExp(`(${query})`, 'gi')
  return text.replace(regex, '<mark class="bg-yellow-200 dark:bg-yellow-800">$1</mark>')
}

const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatFileSize = (bytes: number): string => {
  const sizes = ['B', 'KB', 'MB', 'GB']
  if (bytes === 0) return '0 B'
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return Math.round(bytes / Math.pow(1024, i) * 100) / 100 + ' ' + sizes[i]
}

const openFolder = (folder: IFolderInfo) => {
  router.push({
    name: 'FilesFolder',
    params: {
      spaceId: folder.spaceId?.toString() || '1',
      folderId: folder.id.toString()
    }
  })
}

const openFile = (file: IFileInfo) => {
  // 这里可以打开文件预览或下载
  console.log('Open file:', file)
}

const downloadFile = (file: IFileInfo) => {
  // 实现文件下载
  console.log('Download file:', file)
}

// 生命周期
onMounted(() => {
  // 从URL参数获取搜索查询
  const query = route.query.q as string
  const spaceId = route.query.spaceId as string
  
  if (spaceId) {
    currentSpaceId.value = parseInt(spaceId)
  }
  
  if (query) {
    handleSearch(query)
  }
})

// 监听路由变化
watch(() => route.query, (newQuery) => {
  const query = newQuery.q as string
  const spaceId = newQuery.spaceId as string
  
  if (spaceId) {
    currentSpaceId.value = parseInt(spaceId)
  }
  
  if (query && query !== currentQuery.value) {
    handleSearch(query)
  }
})
</script>
