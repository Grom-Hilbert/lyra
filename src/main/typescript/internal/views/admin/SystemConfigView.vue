<template>
  <div class="min-h-screen bg-gradient-to-br from-background via-muted/30 to-accent/5">
    <div class="container mx-auto p-6 space-y-6">
      <!-- 页面标题 -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-3xl font-bold bg-gradient-to-r from-tech-blue to-tech-purple bg-clip-text text-transparent">
            系统配置
          </h1>
          <p class="text-muted-foreground mt-1">管理系统的各项设置和配置</p>
        </div>
        <div class="flex gap-2">
          <Button @click="resetToDefaults" variant="outline">
            <RotateCcw class="w-4 h-4 mr-2" />
            重置为默认
          </Button>
          <Button @click="saveAllConfigs" :disabled="saving">
            <Save class="w-4 h-4 mr-2" v-if="!saving" />
            <div class="w-4 h-4 mr-2 animate-spin rounded-full border-2 border-current border-t-transparent" v-else />
            {{ saving ? '保存中...' : '保存配置' }}
          </Button>
        </div>
      </div>

      <!-- 配置选项卡 -->
      <div class="flex space-x-1 border-b">
        <button
          v-for="tab in configTabs"
          :key="tab.id"
          @click="activeTab = tab.id"
          :class="[
            'px-4 py-2 text-sm font-medium rounded-t-lg transition-colors',
            activeTab === tab.id
              ? 'bg-primary text-primary-foreground'
              : 'text-muted-foreground hover:text-foreground hover:bg-muted'
          ]"
        >
          <component :is="tab.icon" class="w-4 h-4 mr-2 inline" />
          {{ tab.label }}
        </button>
      </div>

      <!-- 配置内容 -->
      <div class="space-y-6">
        <!-- 通用设置 -->
        <Card v-show="activeTab === 'general'" class="border-border/50 shadow-lg">
          <CardHeader>
            <CardTitle class="flex items-center">
              <Globe class="w-5 h-5 mr-2 text-tech-blue" />
              通用设置
            </CardTitle>
            <CardDescription>
              系统的基本配置信息
            </CardDescription>
          </CardHeader>
          <CardContent class="space-y-6">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div class="space-y-2">
                <Label for="siteName">站点名称</Label>
                <Input
                  id="siteName"
                  v-model="configData.general.siteName"
                  placeholder="Lyra 云盘系统"
                />
              </div>
              <div class="space-y-2">
                <Label for="adminEmail">管理员邮箱</Label>
                <Input
                  id="adminEmail"
                  v-model="configData.general.adminEmail"
                  type="email"
                  placeholder="admin@lyra.com"
                />
              </div>
              <div class="space-y-2">
                <Label for="timezone">时区</Label>
                <select
                  id="timezone"
                  v-model="configData.general.timezone"
                  class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus:ring-2 focus:ring-ring focus:ring-offset-2"
                >
                  <option value="Asia/Shanghai">Asia/Shanghai</option>
                  <option value="Asia/Tokyo">Asia/Tokyo</option>
                  <option value="America/New_York">America/New_York</option>
                  <option value="Europe/London">Europe/London</option>
                  <option value="UTC">UTC</option>
                </select>
              </div>
              <div class="space-y-2">
                <Label for="language">系统语言</Label>
                <select
                  id="language"
                  v-model="configData.general.language"
                  class="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus:ring-2 focus:ring-ring focus:ring-offset-2"
                >
                  <option value="zh-CN">简体中文</option>
                  <option value="en-US">English</option>
                  <option value="ja-JP">日本語</option>
                </select>
              </div>
            </div>
            
            <div class="space-y-2">
              <Label for="siteDescription">站点描述</Label>
              <Textarea
                id="siteDescription"
                v-model="configData.general.siteDescription"
                placeholder="企业级文档管理系统"
                rows="3"
              />
            </div>
          </CardContent>
        </Card>

        <!-- 存储配置 -->
        <Card v-show="activeTab === 'storage'" class="border-border/50 shadow-lg">
          <CardHeader>
            <CardTitle class="flex items-center">
              <HardDrive class="w-5 h-5 mr-2 text-tech-purple" />
              存储配置
            </CardTitle>
            <CardDescription>
              文件存储相关的配置选项
            </CardDescription>
          </CardHeader>
          <CardContent class="space-y-6">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div class="space-y-2">
                <Label for="maxFileSize">单文件最大大小</Label>
                <Input
                  id="maxFileSize"
                  v-model="configData.storage.maxFileSize"
                  placeholder="100MB"
                />
                <p class="text-sm text-muted-foreground">
                  支持的单位：B, KB, MB, GB
                </p>
              </div>
              <div class="space-y-2">
                <Label for="defaultQuota">默认用户配额</Label>
                <Input
                  id="defaultQuota"
                  v-model="configData.storage.defaultQuota"
                  placeholder="10GB"
                />
                <p class="text-sm text-muted-foreground">
                  新用户的默认存储配额
                </p>
              </div>
            </div>
            
            <div class="space-y-2">
              <Label>允许的文件类型</Label>
              <div class="space-y-2">
                <div class="flex items-center space-x-2">
                  <Input
                    v-model="newFileType"
                    placeholder="输入文件扩展名，如: pdf"
                    @keyup.enter="addFileType"
                  />
                  <Button @click="addFileType" size="sm">
                    <Plus class="w-4 h-4" />
                  </Button>
                </div>
                <div class="flex flex-wrap gap-2">
                  <Badge
                    v-for="(type, index) in configData.storage.allowedFileTypes"
                    :key="index"
                    variant="outline"
                    class="cursor-pointer hover:bg-destructive hover:text-destructive-foreground"
                    @click="removeFileType(index)"
                  >
                    {{ type }}
                    <X class="w-3 h-3 ml-1" />
                  </Badge>
                </div>
              </div>
            </div>
            
            <div class="flex items-center space-x-2">
              <input
                type="checkbox"
                id="enableVersioning"
                v-model="configData.storage.enableVersioning"
                class="rounded border-border"
              />
              <Label for="enableVersioning">启用文件版本控制</Label>
            </div>
          </CardContent>
        </Card>

        <!-- 安全配置 -->
        <Card v-show="activeTab === 'security'" class="border-border/50 shadow-lg">
          <CardHeader>
            <CardTitle class="flex items-center">
              <Shield class="w-5 h-5 mr-2 text-neon-green" />
              安全配置
            </CardTitle>
            <CardDescription>
              系统安全相关的配置选项
            </CardDescription>
          </CardHeader>
          <CardContent class="space-y-6">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div class="space-y-2">
                <Label for="jwtExpiration">JWT Token 过期时间</Label>
                <Input
                  id="jwtExpiration"
                  v-model="configData.security.jwtExpiration"
                  placeholder="24h"
                />
                <p class="text-sm text-muted-foreground">
                  格式：1h, 24h, 7d
                </p>
              </div>
              <div class="space-y-2">
                <Label for="sessionTimeout">会话超时时间</Label>
                <Input
                  id="sessionTimeout"
                  v-model="configData.security.sessionTimeout"
                  placeholder="30m"
                />
                <p class="text-sm text-muted-foreground">
                  无操作后的会话超时时间
                </p>
              </div>
              <div class="space-y-2">
                <Label for="passwordMinLength">密码最小长度</Label>
                <Input
                  id="passwordMinLength"
                  v-model.number="configData.security.passwordMinLength"
                  type="number"
                  min="6"
                  max="32"
                />
              </div>
            </div>
            
            <div class="flex items-center space-x-2">
              <input
                type="checkbox"
                id="enableTwoFactor"
                v-model="configData.security.enableTwoFactor"
                class="rounded border-border"
              />
              <Label for="enableTwoFactor">启用双因素认证</Label>
            </div>
          </CardContent>
        </Card>

        <!-- 邮件配置 -->
        <Card v-show="activeTab === 'email'" class="border-border/50 shadow-lg">
          <CardHeader>
            <CardTitle class="flex items-center">
              <Mail class="w-5 h-5 mr-2 text-orange-500" />
              邮件配置
            </CardTitle>
            <CardDescription>
              SMTP 邮件服务器配置
            </CardDescription>
          </CardHeader>
          <CardContent class="space-y-6">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div class="space-y-2">
                <Label for="smtpHost">SMTP 服务器</Label>
                <Input
                  id="smtpHost"
                  v-model="configData.email.smtpHost"
                  placeholder="smtp.example.com"
                />
              </div>
              <div class="space-y-2">
                <Label for="smtpPort">SMTP 端口</Label>
                <Input
                  id="smtpPort"
                  v-model.number="configData.email.smtpPort"
                  type="number"
                  placeholder="587"
                />
              </div>
              <div class="space-y-2">
                <Label for="fromAddress">发送方邮箱</Label>
                <Input
                  id="fromAddress"
                  v-model="configData.email.fromAddress"
                  type="email"
                  placeholder="noreply@lyra.com"
                />
              </div>
            </div>
            
            <div class="flex items-center space-x-2">
              <input
                type="checkbox"
                id="enableTLS"
                v-model="configData.email.enableTLS"
                class="rounded border-border"
              />
              <Label for="enableTLS">启用 TLS 加密</Label>
            </div>
            
            <div class="p-4 bg-muted/50 rounded-lg">
              <h4 class="font-medium mb-2 flex items-center">
                <TestTube class="w-4 h-4 mr-2" />
                邮件测试
              </h4>
              <div class="flex gap-2">
                <Input
                  v-model="testEmail"
                  type="email"
                  placeholder="输入测试邮箱"
                  class="flex-1"
                />
                <Button @click="sendTestEmail" :disabled="testingEmail">
                  <Send class="w-4 h-4 mr-2" v-if="!testingEmail" />
                  <div class="w-4 h-4 mr-2 animate-spin rounded-full border-2 border-current border-t-transparent" v-else />
                  发送测试邮件
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>

        <!-- 缓存配置 -->
        <Card v-show="activeTab === 'cache'" class="border-border/50 shadow-lg">
          <CardHeader>
            <CardTitle class="flex items-center">
              <Database class="w-5 h-5 mr-2 text-blue-500" />
              缓存配置
            </CardTitle>
            <CardDescription>
              系统缓存相关的配置和管理
            </CardDescription>
          </CardHeader>
          <CardContent class="space-y-6">
            <!-- 缓存统计 -->
            <div v-if="cacheStats" class="grid grid-cols-1 md:grid-cols-4 gap-4">
              <div class="p-4 bg-muted/50 rounded-lg text-center">
                <div class="text-2xl font-bold text-tech-blue">{{ cacheStats.totalKeys }}</div>
                <div class="text-sm text-muted-foreground">缓存键总数</div>
              </div>
              <div class="p-4 bg-muted/50 rounded-lg text-center">
                <div class="text-2xl font-bold text-tech-purple">{{ formatBytes(cacheStats.memoryUsage) }}</div>
                <div class="text-sm text-muted-foreground">内存使用</div>
              </div>
              <div class="p-4 bg-muted/50 rounded-lg text-center">
                <div class="text-2xl font-bold text-neon-green">{{ (cacheStats.hitRate * 100).toFixed(1) }}%</div>
                <div class="text-sm text-muted-foreground">命中率</div>
              </div>
              <div class="p-4 bg-muted/50 rounded-lg text-center">
                <div class="text-2xl font-bold text-orange-500">{{ (cacheStats.missRate * 100).toFixed(1) }}%</div>
                <div class="text-sm text-muted-foreground">未命中率</div>
              </div>
            </div>
            
            <!-- 缓存操作 -->
            <div class="space-y-4">
              <h4 class="font-medium">缓存管理</h4>
              <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
                <Button @click="clearUserCache" variant="outline">
                  <Users class="w-4 h-4 mr-2" />
                  清理用户缓存
                </Button>
                <Button @click="clearFileCache" variant="outline">
                  <FileText class="w-4 h-4 mr-2" />
                  清理文件缓存
                </Button>
                <Button @click="clearAllCache" variant="destructive">
                  <Trash2 class="w-4 h-4 mr-2" />
                  清理所有缓存
                </Button>
              </div>
            </div>
            
            <!-- 具体缓存详情 -->
            <div v-if="cacheStats?.cachesByName" class="space-y-4">
              <h4 class="font-medium">缓存详情</h4>
              <div class="space-y-2">
                <div 
                  v-for="(cache, name) in cacheStats.cachesByName" 
                  :key="name"
                  class="flex items-center justify-between p-3 border rounded-lg"
                >
                  <div>
                    <div class="font-medium">{{ name }}</div>
                    <div class="text-sm text-muted-foreground">
                      {{ cache.keys }} 个键 • {{ formatBytes(cache.size) }}
                    </div>
                  </div>
                  <div class="flex items-center gap-2">
                    <div class="text-sm">
                      命中: {{ cache.hits }} | 未命中: {{ cache.misses }}
                    </div>
                    <Button @click="clearSpecificCache(String(name))" size="sm" variant="outline">
                      <Trash2 class="w-3 h-3" />
                    </Button>
                  </div>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { adminApi } from '@/apis/admin-mock'
import type { ISystemConfiguration } from '@/types/admin'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Textarea } from '@/components/ui/textarea'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'

import {
  Globe,
  HardDrive,
  Shield,
  Mail,
  Database,
  Save,
  RotateCcw,
  Plus,
  X,
  TestTube,
  Send,
  Users,
  FileText,
  Trash2
} from 'lucide-vue-next'

// 响应式数据
const loading = ref(false)
const saving = ref(false)
const testingEmail = ref(false)
const activeTab = ref('general')
const testEmail = ref('')
const newFileType = ref('')
const cacheStats = ref<any>(null)

// 配置选项卡
const configTabs = [
  { id: 'general', label: '通用设置', icon: Globe },
  { id: 'storage', label: '存储配置', icon: HardDrive },
  { id: 'security', label: '安全配置', icon: Shield },
  { id: 'email', label: '邮件配置', icon: Mail },
  { id: 'cache', label: '缓存管理', icon: Database }
]

// 配置数据
const configData = reactive<ISystemConfiguration>({
  general: {
    siteName: '',
    siteDescription: '',
    adminEmail: '',
    timezone: 'Asia/Shanghai',
    language: 'zh-CN'
  },
  storage: {
    maxFileSize: '100MB',
    allowedFileTypes: ['pdf', 'docx', 'xlsx', 'jpg', 'png', 'txt'],
    defaultQuota: '10GB',
    enableVersioning: true
  },
  security: {
    jwtExpiration: '24h',
    passwordMinLength: 8,
    enableTwoFactor: false,
    sessionTimeout: '30m'
  },
  email: {
    smtpHost: '',
    smtpPort: 587,
    enableTLS: true,
    fromAddress: ''
  }
})

// 方法
const loadConfiguration = async () => {
  loading.value = true
  try {
    const config = await adminApi.getSystemConfiguration()
    Object.assign(configData, config)
  } catch (error) {
    console.error('加载配置失败:', error)
  } finally {
    loading.value = false
  }
}

const loadCacheStats = async () => {
  try {
    cacheStats.value = await adminApi.getCacheStatistics()
  } catch (error) {
    console.error('加载缓存统计失败:', error)
  }
}

const saveAllConfigs = async () => {
  saving.value = true
  try {
    await adminApi.updateSystemConfiguration(configData)
    // 显示成功提示
  } catch (error) {
    console.error('保存配置失败:', error)
  } finally {
    saving.value = false
  }
}

const resetToDefaults = () => {
  if (confirm('确定要重置所有配置为默认值吗？此操作不可逆。')) {
    Object.assign(configData, {
      general: {
        siteName: 'Lyra 云盘系统',
        siteDescription: '企业级文档管理系统',
        adminEmail: 'admin@lyra.com',
        timezone: 'Asia/Shanghai',
        language: 'zh-CN'
      },
      storage: {
        maxFileSize: '100MB',
        allowedFileTypes: ['pdf', 'docx', 'xlsx', 'jpg', 'png', 'txt'],
        defaultQuota: '10GB',
        enableVersioning: true
      },
      security: {
        jwtExpiration: '24h',
        passwordMinLength: 8,
        enableTwoFactor: false,
        sessionTimeout: '30m'
      },
      email: {
        smtpHost: '',
        smtpPort: 587,
        enableTLS: true,
        fromAddress: ''
      }
    })
  }
}

const addFileType = () => {
  if (newFileType.value.trim() && !configData.storage.allowedFileTypes.includes(newFileType.value.trim())) {
    configData.storage.allowedFileTypes.push(newFileType.value.trim())
    newFileType.value = ''
  }
}

const removeFileType = (index: number) => {
  configData.storage.allowedFileTypes.splice(index, 1)
}

const sendTestEmail = async () => {
  if (!testEmail.value) return
  
  testingEmail.value = true
  try {
    // 发送测试邮件的API调用
    // await adminApi.sendTestEmail(testEmail.value)
    console.log('发送测试邮件到:', testEmail.value)
    // 显示成功提示
  } catch (error) {
    console.error('发送测试邮件失败:', error)
  } finally {
    testingEmail.value = false
  }
}

const clearUserCache = async () => {
  try {
    await adminApi.clearCache('users')
    loadCacheStats()
  } catch (error) {
    console.error('清理用户缓存失败:', error)
  }
}

const clearFileCache = async () => {
  try {
    await adminApi.clearCache('files')
    loadCacheStats()
  } catch (error) {
    console.error('清理文件缓存失败:', error)
  }
}

const clearAllCache = async () => {
  if (confirm('确定要清理所有缓存吗？这可能会暂时影响系统性能。')) {
    try {
      await adminApi.clearAllCache()
      loadCacheStats()
    } catch (error) {
      console.error('清理所有缓存失败:', error)
    }
  }
}

const clearSpecificCache = async (cacheName: string) => {
  if (confirm(`确定要清理 ${cacheName} 缓存吗？`)) {
    try {
      await adminApi.clearCache(cacheName)
      loadCacheStats()
    } catch (error) {
      console.error('清理缓存失败:', error)
    }
  }
}

const formatBytes = (bytes: number) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 生命周期
onMounted(async () => {
  await Promise.all([
    loadConfiguration(),
    loadCacheStats()
  ])
})
</script>

<style scoped>
/* 选项卡过渡动画 */
.transition-colors {
  transition: color 0.2s ease, background-color 0.2s ease;
}

/* 自定义滚动条 */
.overflow-y-auto::-webkit-scrollbar {
  width: 6px;
}

.overflow-y-auto::-webkit-scrollbar-track {
  background: hsl(var(--muted));
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb {
  background: hsl(var(--muted-foreground) / 0.3);
  border-radius: 3px;
}

.overflow-y-auto::-webkit-scrollbar-thumb:hover {
  background: hsl(var(--muted-foreground) / 0.5);
}

/* 表单样式优化 */
.space-y-6 > * + * {
  margin-top: 1.5rem;
}

.space-y-4 > * + * {
  margin-top: 1rem;
}

/* 响应式优化 */
@media (max-width: 768px) {
  .grid-cols-1.md\:grid-cols-2 {
    grid-template-columns: repeat(1, minmax(0, 1fr));
  }
  
  .grid-cols-1.md\:grid-cols-4 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style> 