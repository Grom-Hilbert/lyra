<template>
  <div class="min-h-screen bg-gradient-to-br from-background via-muted/30 to-accent/5">
    <div class="container mx-auto p-6 space-y-6">
      <!-- 页面标题 -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-3xl font-bold bg-gradient-to-r from-tech-blue to-tech-purple bg-clip-text text-transparent">
            版本控制管理
          </h1>
          <p class="text-muted-foreground mt-1">管理文件版本控制设置和历史记录</p>
        </div>
        <Button @click="saveSettings" :disabled="saving">
          <Save class="w-4 h-4 mr-2" v-if="!saving" />
          <div class="w-4 h-4 mr-2 animate-spin rounded-full border-2 border-current border-t-transparent" v-else />
          {{ saving ? '保存中...' : '保存设置' }}
        </Button>
      </div>

      <!-- 版本控制设置 -->
      <Card class="border-border/50 shadow-lg">
        <CardHeader>
          <CardTitle class="flex items-center">
            <Settings class="w-5 h-5 mr-2 text-tech-blue" />
            版本控制设置
          </CardTitle>
          <CardDescription>
            配置系统的版本控制行为和策略
          </CardDescription>
        </CardHeader>
        <CardContent class="space-y-6">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div class="flex items-center space-x-2">
              <input
                type="checkbox"
                id="versionEnabled"
                v-model="settings.enabled"
                class="rounded border-border"
              />
              <Label for="versionEnabled" class="font-medium">启用版本控制</Label>
            </div>
            
            <div class="space-y-2">
              <Label for="maxVersions">最大版本数</Label>
              <Input
                id="maxVersions"
                v-model.number="settings.maxVersions"
                type="number"
                min="1"
                max="100"
                :disabled="!settings.enabled"
              />
              <p class="text-xs text-muted-foreground">
                每个文件保留的最大版本数
              </p>
            </div>
            
            <div class="space-y-2">
              <Label for="retentionDays">保留天数</Label>
              <Input
                id="retentionDays"
                v-model.number="settings.retentionDays"
                type="number"
                min="1"
                max="3650"
                :disabled="!settings.enabled"
              />
              <p class="text-xs text-muted-foreground">
                版本文件的保留天数
              </p>
            </div>
          </div>
          
          <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div class="flex items-center space-x-2">
              <input
                type="checkbox"
                id="compressionEnabled"
                v-model="settings.compressionEnabled"
                :disabled="!settings.enabled"
                class="rounded border-border"
              />
              <Label for="compressionEnabled">启用压缩存储</Label>
            </div>
            
            <div class="flex items-center space-x-2">
              <input
                type="checkbox"
                id="autoCleanup"
                v-model="settings.autoCleanup"
                :disabled="!settings.enabled"
                class="rounded border-border"
              />
              <Label for="autoCleanup">自动清理过期版本</Label>
            </div>
          </div>

          <!-- 版本控制统计 -->
          <div class="grid grid-cols-1 md:grid-cols-4 gap-4 pt-4 border-t">
            <div class="p-4 bg-muted/50 rounded-lg text-center">
              <div class="text-2xl font-bold text-tech-blue">{{ versionStats.totalVersions || 0 }}</div>
              <div class="text-sm text-muted-foreground">总版本数</div>
            </div>
            <div class="p-4 bg-muted/50 rounded-lg text-center">
              <div class="text-2xl font-bold text-tech-purple">{{ versionStats.totalFiles || 0 }}</div>
              <div class="text-sm text-muted-foreground">有版本的文件</div>
            </div>
            <div class="p-4 bg-muted/50 rounded-lg text-center">
              <div class="text-2xl font-bold text-neon-green">{{ formatBytes(versionStats.totalSize || 0) }}</div>
              <div class="text-sm text-muted-foreground">版本存储大小</div>
            </div>
            <div class="p-4 bg-muted/50 rounded-lg text-center">
              <div class="text-2xl font-bold text-orange-500">{{ versionStats.avgVersionsPerFile || 0 }}</div>
              <div class="text-sm text-muted-foreground">平均版本数/文件</div>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- 文件搜索和版本管理 -->
      <Card class="border-border/50 shadow-lg">
        <CardHeader>
          <CardTitle class="flex items-center">
            <Search class="w-5 h-5 mr-2 text-tech-purple" />
            文件版本管理
          </CardTitle>
          <CardDescription>
            查看和管理具体文件的版本历史
          </CardDescription>
        </CardHeader>
        <CardContent class="space-y-4">
          <!-- 搜索文件 -->
          <div class="flex gap-2">
            <Input
              v-model="searchKeyword"
              placeholder="搜索文件名或路径..."
              @input="searchFiles"
              class="flex-1"
            />
            <Button @click="searchFiles" :disabled="searching">
              <Search class="w-4 h-4 mr-2" v-if="!searching" />
              <div class="w-4 h-4 mr-2 animate-spin rounded-full border-2 border-current border-t-transparent" v-else />
              搜索
            </Button>
          </div>

          <!-- 文件列表 -->
          <div v-if="fileList.length" class="space-y-2">
            <div 
              v-for="file in fileList" 
              :key="file.id"
              class="flex items-center justify-between p-3 border rounded-lg hover:bg-muted/50 transition-colors"
            >
              <div class="flex items-center gap-3">
                <FileText class="w-8 h-8 text-tech-blue" />
                <div>
                  <div class="font-medium">{{ file.name }}</div>
                  <div class="text-sm text-muted-foreground">{{ file.path }}</div>
                  <div class="text-xs text-muted-foreground">
                    当前版本: v{{ file.currentVersion }} • {{ file.totalVersions }} 个版本
                  </div>
                </div>
              </div>
              <div class="flex gap-2">
                <Button @click="viewVersionHistory(file)" size="sm" variant="outline">
                  <History class="w-4 h-4 mr-1" />
                  查看历史
                </Button>
                <Button @click="cleanupVersions(file)" size="sm" variant="outline">
                  <Trash2 class="w-4 h-4 mr-1" />
                  清理版本
                </Button>
              </div>
            </div>
          </div>
          
          <div v-else-if="searchKeyword && !searching" class="text-center py-8 text-muted-foreground">
            <FileX class="w-12 h-12 mx-auto mb-2 opacity-50" />
            <p>未找到匹配的文件</p>
          </div>
          
          <div v-else-if="!searchKeyword" class="text-center py-8 text-muted-foreground">
            <Search class="w-12 h-12 mx-auto mb-2 opacity-50" />
            <p>输入文件名开始搜索</p>
          </div>
        </CardContent>
      </Card>

      <!-- 批量版本清理 -->
      <Card class="border-border/50 shadow-lg">
        <CardHeader>
          <CardTitle class="flex items-center">
            <Trash2 class="w-5 h-5 mr-2 text-orange-500" />
            批量版本清理
          </CardTitle>
          <CardDescription>
            批量清理系统中的过期版本文件
          </CardDescription>
        </CardHeader>
        <CardContent class="space-y-4">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div class="space-y-2">
              <Label for="cleanupType">清理类型</Label>
              <select
                id="cleanupType"
                v-model="cleanupOptions.type"
                class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus:ring-2 focus:ring-ring focus:ring-offset-2"
              >
                <option value="old">清理旧版本</option>
                <option value="excess">清理超出限制的版本</option>
                <option value="size">按大小清理</option>
              </select>
            </div>
            
            <div class="space-y-2" v-if="cleanupOptions.type === 'old'">
              <Label for="olderThan">清理早于</Label>
              <Input
                id="olderThan"
                v-model.number="cleanupOptions.olderThanDays"
                type="number"
                min="1"
                placeholder="30"
              />
              <p class="text-xs text-muted-foreground">天</p>
            </div>
            
            <div class="space-y-2" v-if="cleanupOptions.type === 'excess'">
              <Label for="keepVersions">保留版本数</Label>
              <Input
                id="keepVersions"
                v-model.number="cleanupOptions.keepVersions"
                type="number"
                min="1"
                placeholder="5"
              />
            </div>
            
            <div class="space-y-2" v-if="cleanupOptions.type === 'size'">
              <Label for="maxSize">最大总大小</Label>
              <Input
                id="maxSize"
                v-model="cleanupOptions.maxSizeGB"
                placeholder="10"
              />
              <p class="text-xs text-muted-foreground">GB</p>
            </div>
          </div>
          
          <div class="flex items-center gap-4">
            <Button @click="previewCleanup" variant="outline" :disabled="cleanupRunning">
              <Eye class="w-4 h-4 mr-2" />
              预览清理结果
            </Button>
            <Button @click="runCleanup" variant="destructive" :disabled="cleanupRunning">
              <Trash2 class="w-4 h-4 mr-2" v-if="!cleanupRunning" />
              <div class="w-4 h-4 mr-2 animate-spin rounded-full border-2 border-current border-t-transparent" v-else />
              {{ cleanupRunning ? '清理中...' : '开始清理' }}
            </Button>
          </div>
          
          <!-- 清理预览结果 -->
          <div v-if="cleanupPreview" class="p-4 bg-muted/50 rounded-lg">
            <h4 class="font-medium mb-2">清理预览</h4>
            <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div>
                <div class="font-medium">将清理文件数</div>
                <div class="text-orange-500">{{ cleanupPreview.filesCount }}</div>
              </div>
              <div>
                <div class="font-medium">将清理版本数</div>
                <div class="text-orange-500">{{ cleanupPreview.versionsCount }}</div>
              </div>
              <div>
                <div class="font-medium">释放空间</div>
                <div class="text-neon-green">{{ formatBytes(cleanupPreview.spaceFreed) }}</div>
              </div>
              <div>
                <div class="font-medium">预计用时</div>
                <div class="text-tech-blue">{{ cleanupPreview.estimatedTime }}</div>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>

    <!-- 版本历史对话框 -->
    <Dialog v-model:open="showVersionHistory">
      <DialogContent class="max-w-4xl max-h-[80vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle>版本历史</DialogTitle>
          <DialogDescription v-if="selectedFile">
            {{ selectedFile.name }} 的版本历史记录
          </DialogDescription>
        </DialogHeader>
        
        <div class="flex-1 overflow-auto">
          <div v-if="versionHistory" class="space-y-3">
            <div 
              v-for="version in versionHistory.versions" 
              :key="version.id"
              class="flex items-center justify-between p-3 border rounded-lg"
              :class="version.version === versionHistory.file.currentVersion ? 'border-primary bg-primary/5' : ''"
            >
              <div class="flex items-center gap-3">
                <div class="p-2 bg-muted rounded-full">
                  <GitBranch class="w-4 h-4" />
                </div>
                <div>
                  <div class="flex items-center gap-2">
                    <span class="font-medium">v{{ version.version }}</span>
                    <Badge v-if="version.version === versionHistory.file.currentVersion" variant="default">
                      当前版本
                    </Badge>
                  </div>
                  <div class="text-sm text-muted-foreground">
                    {{ formatBytes(version.size) }} • {{ formatDateTime(version.createdAt) }}
                  </div>
                  <div class="text-sm text-muted-foreground">
                    创建者: {{ version.createdBy }}
                  </div>
                  <div v-if="version.comment" class="text-sm mt-1">
                    {{ version.comment }}
                  </div>
                </div>
              </div>
              <div class="flex gap-2">
                <Button size="sm" variant="outline">
                  <Download class="w-4 h-4 mr-1" />
                  下载
                </Button>
                <Button size="sm" variant="outline">
                  <RotateCcw class="w-4 h-4 mr-1" />
                  恢复
                </Button>
                <Button 
                  v-if="version.version !== versionHistory.file.currentVersion"
                  size="sm" 
                  variant="destructive"
                >
                  <Trash2 class="w-4 h-4 mr-1" />
                  删除
                </Button>
              </div>
            </div>
          </div>
        </div>
        
        <DialogFooter>
          <Button @click="showVersionHistory = false">关闭</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { adminApi } from '@/apis/admin-mock'
import type { IVersionControlSettings, IVersionHistory } from '@/types/admin'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { 
  Dialog, 
  DialogContent, 
  DialogDescription, 
  DialogFooter, 
  DialogHeader, 
  DialogTitle 
} from '@/components/ui/dialog'

import {
  Settings,
  Save,
  Search,
  FileText,
  History,
  Trash2,
  Eye,
  GitBranch,
  Download,
  RotateCcw,
  FileX
} from 'lucide-vue-next'

// 响应式数据
const saving = ref(false)
const searching = ref(false)
const cleanupRunning = ref(false)
const searchKeyword = ref('')
const showVersionHistory = ref(false)
const selectedFile = ref<any>(null)
const versionHistory = ref<IVersionHistory | null>(null)
const fileList = ref<any[]>([])
const cleanupPreview = ref<any>(null)

// 版本控制设置
const settings = reactive<IVersionControlSettings>({
  enabled: true,
  maxVersions: 10,
  retentionDays: 365,
  compressionEnabled: true,
  autoCleanup: true
})

// 版本统计
const versionStats = reactive({
  totalVersions: 0,
  totalFiles: 0,
  totalSize: 0,
  avgVersionsPerFile: 0
})

// 清理选项
const cleanupOptions = reactive({
  type: 'old',
  olderThanDays: 30,
  keepVersions: 5,
  maxSizeGB: '10'
})

// 方法
const loadSettings = async () => {
  try {
    const data = await adminApi.getVersionControlSettings()
    Object.assign(settings, data)
  } catch (error) {
    console.error('加载版本控制设置失败:', error)
  }
}

const saveSettings = async () => {
  saving.value = true
  try {
    await adminApi.updateVersionControlSettings(settings)
    // 显示成功提示
  } catch (error) {
    console.error('保存设置失败:', error)
  } finally {
    saving.value = false
  }
}

const searchFiles = async () => {
  if (!searchKeyword.value.trim()) {
    fileList.value = []
    return
  }
  
  searching.value = true
  try {
    // 这里应该调用搜索文件的API
    // const result = await adminApi.searchFiles(searchKeyword.value)
    // fileList.value = result
    
    // 模拟数据
    fileList.value = [
      {
        id: 1,
        name: 'example.pdf',
        path: '/documents/example.pdf',
        currentVersion: 3,
        totalVersions: 5
      },
      {
        id: 2,
        name: 'report.docx',
        path: '/reports/report.docx',
        currentVersion: 2,
        totalVersions: 3
      }
    ]
  } catch (error) {
    console.error('搜索文件失败:', error)
  } finally {
    searching.value = false
  }
}

const viewVersionHistory = async (file: any) => {
  selectedFile.value = file
  showVersionHistory.value = true
  
  try {
    versionHistory.value = await adminApi.getFileVersionHistory(file.id)
  } catch (error) {
    console.error('加载版本历史失败:', error)
  }
}

const cleanupVersions = async (file: any) => {
  if (confirm(`确定要清理文件 "${file.name}" 的旧版本吗？`)) {
    try {
      await adminApi.cleanupFileVersions(file.id, 5) // 保留5个版本
      // 刷新文件列表
      searchFiles()
    } catch (error) {
      console.error('清理版本失败:', error)
    }
  }
}

const previewCleanup = async () => {
  try {
    // 这里应该调用预览清理的API
    // const preview = await adminApi.previewVersionCleanup(cleanupOptions)
    // cleanupPreview.value = preview
    
    // 模拟预览数据
    cleanupPreview.value = {
      filesCount: 150,
      versionsCount: 430,
      spaceFreed: 2.5 * 1024 * 1024 * 1024, // 2.5GB
      estimatedTime: '约 5 分钟'
    }
  } catch (error) {
    console.error('预览清理失败:', error)
  }
}

const runCleanup = async () => {
  if (!confirm('确定要执行批量清理吗？此操作不可逆。')) return
  
  cleanupRunning.value = true
  try {
    // 这里应该调用批量清理的API
    // await adminApi.runVersionCleanup(cleanupOptions)
    
    // 模拟清理过程
    await new Promise(resolve => setTimeout(resolve, 3000))
    
    cleanupPreview.value = null
    // 显示成功提示
  } catch (error) {
    console.error('批量清理失败:', error)
  } finally {
    cleanupRunning.value = false
  }
}

const formatBytes = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDateTime = (dateString: string) => {
  return new Date(dateString).toLocaleString('zh-CN')
}

// 生命周期
onMounted(() => {
  loadSettings()
  
  // 加载版本统计数据（模拟）
  Object.assign(versionStats, {
    totalVersions: 1250,
    totalFiles: 450,
    totalSize: 15.8 * 1024 * 1024 * 1024, // 15.8GB
    avgVersionsPerFile: 2.8
  })
})
</script>

<style scoped>
/* 对话框样式优化 */
.overflow-auto::-webkit-scrollbar {
  width: 8px;
}

.overflow-auto::-webkit-scrollbar-track {
  background: hsl(var(--muted));
  border-radius: 4px;
}

.overflow-auto::-webkit-scrollbar-thumb {
  background: hsl(var(--muted-foreground) / 0.3);
  border-radius: 4px;
}

.overflow-auto::-webkit-scrollbar-thumb:hover {
  background: hsl(var(--muted-foreground) / 0.5);
}

/* 卡片悬停效果 */
.hover\:bg-muted\/50:hover {
  background-color: hsl(var(--muted) / 0.5);
}

/* 响应式优化 */
@media (max-width: 768px) {
  .grid-cols-1.md\:grid-cols-3 {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }
  
  .grid-cols-1.md\:grid-cols-4 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  
  .max-w-4xl {
    max-width: 95vw;
  }
}
</style> 