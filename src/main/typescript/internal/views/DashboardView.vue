<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <h1>仪表板</h1>
      <p>欢迎使用 Lyra 文档管理系统</p>
    </div>
    
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon files">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-info">
            <h3>{{ stats.totalFiles }}</h3>
            <p>总文件数</p>
          </div>
        </div>
      </el-card>
      
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon storage">
            <el-icon><FolderOpened /></el-icon>
          </div>
          <div class="stat-info">
            <h3>{{ formatFileSize(stats.totalSize) }}</h3>
            <p>总存储空间</p>
          </div>
        </div>
      </el-card>
      
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon spaces">
            <el-icon><OfficeBuilding /></el-icon>
          </div>
          <div class="stat-info">
            <h3>{{ stats.totalSpaces }}</h3>
            <p>空间数量</p>
          </div>
        </div>
      </el-card>
      
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon users">
            <el-icon><User /></el-icon>
          </div>
          <div class="stat-info">
            <h3>{{ stats.totalUsers }}</h3>
            <p>用户数量</p>
          </div>
        </div>
      </el-card>
    </div>
    
    <!-- 内容区域 -->
    <div class="dashboard-content">
      <!-- 最近文件 -->
      <el-card class="content-card">
        <template #header>
          <div class="card-header">
            <h3>最近文件</h3>
            <el-button text @click="$router.push('/files')">查看全部</el-button>
          </div>
        </template>
        
        <div class="recent-files">
          <div v-if="recentFiles.length === 0" class="empty-state">
            <el-icon><Document /></el-icon>
            <p>暂无最近文件</p>
          </div>
          
          <div v-else class="file-list">
            <div 
              v-for="file in recentFiles" 
              :key="file.id" 
              class="file-item"
              @click="openFile(file)"
            >
              <div class="file-icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="file-info">
                <h4>{{ file.name }}</h4>
                <p>{{ formatFileSize(file.size) }} · {{ formatDate(file.updatedAt) }}</p>
              </div>
              <div class="file-actions">
                <el-button text size="small">打开</el-button>
              </div>
            </div>
          </div>
        </div>
      </el-card>
      
      <!-- 我的空间 -->
      <el-card class="content-card">
        <template #header>
          <div class="card-header">
            <h3>我的空间</h3>
            <el-button text @click="$router.push('/spaces')">管理空间</el-button>
          </div>
        </template>
        
        <div class="my-spaces">
          <div v-if="mySpaces.length === 0" class="empty-state">
            <el-icon><OfficeBuilding /></el-icon>
            <p>暂无空间</p>
          </div>
          
          <div v-else class="space-list">
            <div 
              v-for="space in mySpaces" 
              :key="space.id" 
              class="space-item"
              @click="openSpace(space)"
            >
              <div class="space-icon">
                <el-icon><OfficeBuilding /></el-icon>
              </div>
              <div class="space-info">
                <h4>{{ space.name }}</h4>
                <p>{{ space.description || '暂无描述' }}</p>
              </div>
              <div class="space-meta">
                <el-tag size="small">{{ space.type }}</el-tag>
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { IFileInfo, ISpace } from '@/types/index.ts'

const router = useRouter()

// 统计数据
const stats = ref({
  totalFiles: 0,
  totalSize: 0,
  totalSpaces: 0,
  totalUsers: 0
})

// 最近文件
const recentFiles = ref<IFileInfo[]>([])

// 我的空间
const mySpaces = ref<ISpace[]>([])

// 格式化文件大小
const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 格式化日期
const formatDate = (dateString: string): string => {
  const date = new Date(dateString)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  const minutes = Math.floor(diff / (1000 * 60))
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (minutes < 60) {
    return `${minutes} 分钟前`
  } else if (hours < 24) {
    return `${hours} 小时前`
  } else if (days < 7) {
    return `${days} 天前`
  } else {
    return date.toLocaleDateString()
  }
}

// 打开文件
const openFile = (file: IFileInfo) => {
  router.push(`/files/${file.path}`)
}

// 打开空间
const openSpace = (space: ISpace) => {
  router.push(`/spaces/${space.id}`)
}

// 加载数据
const loadData = async () => {
  try {
    // TODO: 实现API调用
    // const statsData = await dashboardApi.getStats()
    // const recentFilesData = await fileApi.getRecentFiles()
    // const mySpacesData = await spaceApi.getMySpaces()
    
    // 模拟数据
    stats.value = {
      totalFiles: 156,
      totalSize: 2048000000, // 2GB
      totalSpaces: 3,
      totalUsers: 12
    }
    
    recentFiles.value = [
      {
        id: '1',
        name: '项目需求文档.docx',
        path: '/personal/documents/项目需求文档.docx',
        size: 2048000,
        type: 'file',
        mimeType: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        isVersioned: true,
        permissions: [],
        createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
        updatedAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
        createdBy: 'user1',
        modifiedBy: 'user1'
      },
      {
        id: '2',
        name: '系统架构图.png',
        path: '/shared/diagrams/系统架构图.png',
        size: 512000,
        type: 'file',
        mimeType: 'image/png',
        isVersioned: false,
        permissions: [],
        createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
        updatedAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
        createdBy: 'user1',
        modifiedBy: 'user1'
      }
    ]
    
    mySpaces.value = [
      {
        id: '1',
        name: '个人空间',
        description: '我的个人文档存储空间',
        type: 'personal',
        ownerId: 'user1',
        members: [],
        settings: {
          isPublic: false,
          allowGuest: false,
          maxFileSize: 100 * 1024 * 1024,
          quota: 10 * 1024 * 1024 * 1024,
          versionControl: 'basic'
        },
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      },
      {
        id: '2',
        name: '团队协作',
        description: '团队共享文档空间',
        type: 'shared',
        ownerId: 'user1',
        members: [],
        settings: {
          isPublic: false,
          allowGuest: true,
          maxFileSize: 500 * 1024 * 1024,
          quota: 50 * 1024 * 1024 * 1024,
          versionControl: 'git'
        },
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      }
    ]
  } catch (error) {
    console.error('加载仪表板数据失败:', error)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-header {
  margin-bottom: 32px;
}

.dashboard-header h1 {
  margin: 0 0 8px;
  font-size: 32px;
  font-weight: 600;
  color: #2c3e50;
}

.dashboard-header p {
  margin: 0;
  color: #606266;
  font-size: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 32px;
}

.stat-card {
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.stat-icon.files {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.storage {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.spaces {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.users {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-info h3 {
  margin: 0 0 4px;
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
}

.stat-info p {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.dashboard-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.content-card {
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #909399;
}

.empty-state .el-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.file-list,
.space-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.file-item,
.space-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.file-item:hover,
.space-item:hover {
  background-color: #f5f7fa;
}

.file-icon,
.space-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background-color: #e1f5fe;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0288d1;
  font-size: 20px;
}

.file-info,
.space-info {
  flex: 1;
}

.file-info h4,
.space-info h4 {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 500;
  color: #2c3e50;
}

.file-info p,
.space-info p {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

.space-meta {
  display: flex;
  align-items: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .dashboard-content {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .stat-content {
    gap: 12px;
  }
  
  .stat-icon {
    width: 40px;
    height: 40px;
    font-size: 20px;
  }
  
  .stat-info h3 {
    font-size: 20px;
  }
}
</style> 