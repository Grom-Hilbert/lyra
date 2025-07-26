<template>
  <div class="min-h-screen bg-gradient-to-br from-background via-muted/30 to-accent/5">
    <div class="container mx-auto p-6 space-y-6">
      <!-- 页面标题 -->
      <div class="flex items-center justify-between">
        <div>
          <h1 class="text-3xl font-bold bg-gradient-to-r from-tech-blue to-tech-purple bg-clip-text text-transparent">
            用户管理
          </h1>
          <p class="text-muted-foreground mt-1">管理系统中的所有用户账户</p>
        </div>
        <Button @click="openCreateDialog" class="bg-gradient-to-r from-tech-blue to-tech-purple hover:opacity-90">
          <UserPlus class="w-4 h-4 mr-2" />
          创建用户
        </Button>
      </div>

      <!-- 搜索和筛选 -->
      <Card class="border-border/50 shadow-lg">
        <CardHeader>
          <CardTitle class="flex items-center">
            <Search class="w-5 h-5 mr-2 text-tech-blue" />
            搜索和筛选
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
            <!-- 关键词搜索 -->
            <div>
              <Label for="search">搜索用户</Label>
              <Input
                id="search"
                v-model="searchParams.keyword"
                placeholder="用户名、邮箱或显示名称"
                @input="debouncedSearch"
                class="mt-1"
              />
            </div>
            
            <!-- 状态筛选 -->
            <div>
              <Label for="status">用户状态</Label>
              <select
                id="status"
                v-model="searchParams.status"
                @change="handleSearch"
                class="mt-1 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus:ring-2 focus:ring-ring focus:ring-offset-2"
              >
                <option value="">全部状态</option>
                <option value="ACTIVE">活跃</option>
                <option value="INACTIVE">非活跃</option>
                <option value="LOCKED">锁定</option>
                <option value="PENDING">待审核</option>
              </select>
            </div>
            
            <!-- 角色筛选 -->
            <div>
              <Label for="role">用户角色</Label>
              <select
                id="role"
                v-model="searchParams.role"
                @change="handleSearch"
                class="mt-1 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus:ring-2 focus:ring-ring focus:ring-offset-2"
              >
                <option value="">全部角色</option>
                <option value="ADMIN">管理员</option>
                <option value="USER">普通用户</option>
              </select>
            </div>
            
            <!-- 邮箱验证状态 -->
            <div>
              <Label for="emailVerified">邮箱验证</Label>
              <select
                id="emailVerified"
                v-model="searchParams.emailVerified"
                @change="handleSearch"
                class="mt-1 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus:ring-2 focus:ring-ring focus:ring-offset-2"
              >
                <option value="">全部状态</option>
                <option value="true">已验证</option>
                <option value="false">未验证</option>
              </select>
            </div>
          </div>
          
          <!-- 批量操作 -->
          <div class="flex items-center justify-between mt-4 pt-4 border-t">
            <div class="flex items-center gap-2">
              <input
                type="checkbox"
                id="selectAll"
                :checked="isAllSelected"
                @change="toggleSelectAll"
                class="rounded border-border"
              />
              <Label for="selectAll" class="text-sm">
                已选择 {{ selectedUsers.length }} 个用户
              </Label>
            </div>
            
            <div class="flex gap-2" v-if="selectedUsers.length > 0">
              <Button 
                variant="outline" 
                size="sm"
                @click="batchOperation('ACTIVATE')"
              >
                <CheckCircle class="w-4 h-4 mr-1" />
                批量激活
              </Button>
              <Button 
                variant="outline" 
                size="sm"
                @click="batchOperation('DEACTIVATE')"
              >
                <XCircle class="w-4 h-4 mr-1" />
                批量禁用
              </Button>
              <Button 
                variant="destructive" 
                size="sm"
                @click="batchOperation('DELETE')"
              >
                <Trash2 class="w-4 h-4 mr-1" />
                批量删除
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- 用户列表 -->
      <Card class="border-border/50 shadow-lg">
        <CardHeader>
          <CardTitle class="flex items-center justify-between">
            <span class="flex items-center">
              <Users class="w-5 h-5 mr-2 text-tech-purple" />
              用户列表
            </span>
            <Badge variant="secondary">
              共 {{ userList?.pagination.totalElements || 0 }} 个用户
            </Badge>
          </CardTitle>
        </CardHeader>
        <CardContent class="p-0">
          <div class="overflow-x-auto">
            <table class="w-full">
              <thead class="bg-muted/50 border-b">
                <tr>
                  <th class="text-left p-4 font-medium">
                    <input
                      type="checkbox"
                      :checked="isAllSelected"
                      @change="toggleSelectAll"
                      class="rounded border-border"
                    />
                  </th>
                  <th class="text-left p-4 font-medium">用户信息</th>
                  <th class="text-left p-4 font-medium">状态</th>
                  <th class="text-left p-4 font-medium">角色</th>
                  <th class="text-left p-4 font-medium">存储使用</th>
                  <th class="text-left p-4 font-medium">最后登录</th>
                  <th class="text-left p-4 font-medium">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="loading" class="border-b">
                  <td colspan="7" class="p-8 text-center">
                    <div class="flex items-center justify-center">
                      <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-tech-blue"></div>
                      <span class="ml-2 text-muted-foreground">加载中...</span>
                    </div>
                  </td>
                </tr>
                <tr v-else-if="!userList?.users.length" class="border-b">
                  <td colspan="7" class="p-8 text-center text-muted-foreground">
                    暂无用户数据
                  </td>
                </tr>
                <tr 
                  v-else
                  v-for="user in userList.users" 
                  :key="user.id"
                  class="border-b hover:bg-muted/30 transition-colors"
                >
                  <td class="p-4">
                    <input
                      type="checkbox"
                      :value="user.id"
                      v-model="selectedUsers"
                      class="rounded border-border"
                    />
                  </td>
                  <td class="p-4">
                    <div class="flex items-center gap-3">
                      <Avatar class="w-10 h-10">
                        <AvatarImage :src="user.avatar || ''" />
                        <AvatarFallback class="bg-gradient-to-br from-tech-blue to-tech-purple text-white font-semibold">
                          {{ user.displayName.charAt(0) }}
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <div class="font-medium">{{ user.displayName }}</div>
                        <div class="text-sm text-muted-foreground">{{ user.email }}</div>
                        <div class="text-xs text-muted-foreground">@{{ user.username }}</div>
                      </div>
                    </div>
                  </td>
                  <td class="p-4">
                    <Badge :variant="getStatusVariant(user.status)">
                      {{ getStatusLabel(user.status) }}
                    </Badge>
                    <div class="text-xs text-muted-foreground mt-1" v-if="user.emailVerified">
                      <CheckCircle class="w-3 h-3 inline mr-1" />
                      邮箱已验证
                    </div>
                  </td>
                  <td class="p-4">
                    <div class="flex flex-wrap gap-1">
                      <Badge 
                        v-for="role in user.roles" 
                        :key="role.id"
                        variant="outline"
                        class="text-xs"
                      >
                        {{ role.name }}
                      </Badge>
                    </div>
                  </td>
                  <td class="p-4">
                    <div class="space-y-1">
                      <div class="text-sm">
                        {{ formatBytes(user.storageUsed) }} / {{ formatBytes(user.storageQuota) }}
                      </div>
                      <div class="w-full bg-muted rounded-full h-2">
                        <div 
                          class="bg-gradient-to-r from-tech-blue to-tech-purple h-2 rounded-full transition-all"
                          :style="{ width: `${(user.storageUsed / user.storageQuota) * 100}%` }"
                        ></div>
                      </div>
                      <div class="text-xs text-muted-foreground">
                        {{ ((user.storageUsed / user.storageQuota) * 100).toFixed(1) }}%
                      </div>
                    </div>
                  </td>
                  <td class="p-4">
                    <div class="text-sm" v-if="user.lastLoginAt">
                      {{ formatDateTime(user.lastLoginAt) }}
                    </div>
                    <div class="text-xs text-muted-foreground" v-if="user.lastLoginIp">
                      {{ user.lastLoginIp }}
                    </div>
                    <div class="text-sm text-muted-foreground" v-else>
                      从未登录
                    </div>
                  </td>
                  <td class="p-4">
                    <DropdownMenu>
                      <DropdownMenuTrigger as-child>
                        <Button variant="ghost" size="sm">
                          <MoreVertical class="w-4 h-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem @click="viewUser(user)">
                          <Eye class="w-4 h-4 mr-2" />
                          查看详情
                        </DropdownMenuItem>
                        <DropdownMenuItem @click="editUser(user)">
                          <Edit class="w-4 h-4 mr-2" />
                          编辑用户
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem 
                          v-if="user.status === 'ACTIVE'"
                          @click="toggleUserStatus(user, 'INACTIVE')"
                        >
                          <UserX class="w-4 h-4 mr-2" />
                          禁用用户
                        </DropdownMenuItem>
                        <DropdownMenuItem 
                          v-else-if="user.status === 'INACTIVE'"
                          @click="toggleUserStatus(user, 'ACTIVE')"
                        >
                          <UserCheck class="w-4 h-4 mr-2" />
                          激活用户
                        </DropdownMenuItem>
                        <DropdownMenuItem 
                          v-if="user.status !== 'LOCKED'"
                          @click="toggleUserStatus(user, 'LOCKED')"
                        >
                          <Lock class="w-4 h-4 mr-2" />
                          锁定用户
                        </DropdownMenuItem>
                        <DropdownMenuItem 
                          v-else
                          @click="toggleUserStatus(user, 'ACTIVE')"
                        >
                          <Unlock class="w-4 h-4 mr-2" />
                          解锁用户
                        </DropdownMenuItem>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem 
                          @click="deleteUser(user)"
                          class="text-destructive"
                        >
                          <Trash2 class="w-4 h-4 mr-2" />
                          删除用户
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          
          <!-- 分页 -->
          <div class="flex items-center justify-between p-4 border-t" v-if="userList?.pagination">
            <div class="text-sm text-muted-foreground">
              共 {{ userList.pagination.totalElements }} 个用户，
              第 {{ userList.pagination.page + 1 }} / {{ userList.pagination.totalPages }} 页
            </div>
            <div class="flex gap-2">
              <Button 
                variant="outline" 
                size="sm"
                :disabled="userList.pagination.page === 0"
                @click="goToPage(userList.pagination.page - 1)"
              >
                <ChevronLeft class="w-4 h-4" />
                上一页
              </Button>
              <Button 
                variant="outline" 
                size="sm"
                :disabled="userList.pagination.page >= userList.pagination.totalPages - 1"
                @click="goToPage(userList.pagination.page + 1)"
              >
                下一页
                <ChevronRight class="w-4 h-4" />
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>

    <!-- 创建用户对话框 -->
    <Dialog v-model:open="showCreateDialog">
      <DialogContent class="max-w-md">
        <DialogHeader>
          <DialogTitle>创建新用户</DialogTitle>
          <DialogDescription>
            填写下面的信息来创建新的用户账户
          </DialogDescription>
        </DialogHeader>
        
        <form @submit.prevent="handleCreateUser" class="space-y-4">
          <div class="grid grid-cols-2 gap-4">
            <div>
              <Label for="username">用户名 *</Label>
              <Input
                id="username"
                v-model="createForm.username"
                placeholder="输入用户名"
                required
              />
            </div>
            <div>
              <Label for="displayName">显示名称 *</Label>
              <Input
                id="displayName"
                v-model="createForm.displayName"
                placeholder="输入显示名称"
                required
              />
            </div>
          </div>
          
          <div>
            <Label for="email">邮箱地址 *</Label>
            <Input
              id="email"
              v-model="createForm.email"
              type="email"
              placeholder="输入邮箱地址"
              required
            />
          </div>
          
          <div>
            <Label for="password">密码 *</Label>
            <Input
              id="password"
              v-model="createForm.password"
              type="password"
              placeholder="输入密码"
              required
            />
          </div>
          
          <div class="grid grid-cols-2 gap-4">
            <div>
              <Label for="status">初始状态</Label>
              <select
                id="status"
                v-model="createForm.status"
                class="mt-1 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
              >
                <option value="ACTIVE">活跃</option>
                <option value="INACTIVE">非活跃</option>
                <option value="PENDING">待审核</option>
              </select>
            </div>
            <div>
              <Label for="storageQuota">存储配额 (GB)</Label>
              <Input
                id="storageQuota"
                v-model.number="createForm.storageQuotaGB"
                type="number"
                min="1"
                max="1000"
                placeholder="10"
              />
            </div>
          </div>
          
          <div class="flex items-center space-x-2">
            <input
              type="checkbox"
              id="sendWelcomeEmail"
              v-model="createForm.sendWelcomeEmail"
              class="rounded border-border"
            />
            <Label for="sendWelcomeEmail" class="text-sm">
              发送欢迎邮件
            </Label>
          </div>
          
          <DialogFooter>
            <Button type="button" variant="outline" @click="showCreateDialog = false">
              取消
            </Button>
            <Button type="submit" :disabled="createLoading">
              <UserPlus class="w-4 h-4 mr-2" v-if="!createLoading" />
              <div class="w-4 h-4 mr-2 animate-spin rounded-full border-2 border-current border-t-transparent" v-else />
              {{ createLoading ? '创建中...' : '创建用户' }}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { debounce } from 'lodash-es'
import { adminApi } from '@/apis/admin-mock'
import type { 
  IUserDetail, 
  IUserListResponse, 
  ICreateUserRequest,
  IUserSearchRequest 
} from '@/types/admin'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu'

import {
  Search,
  Users,
  UserPlus,
  Eye,
  Edit,
  Trash2,
  MoreVertical,
  CheckCircle,
  XCircle,
  UserX,
  UserCheck,
  Lock,
  Unlock,
  ChevronLeft,
  ChevronRight
} from 'lucide-vue-next'

// 响应式数据
const loading = ref(false)
const createLoading = ref(false)
const userList = ref<IUserListResponse | null>(null)
const selectedUsers = ref<number[]>([])
const showCreateDialog = ref(false)

// 搜索参数
const searchParams = reactive<IUserSearchRequest & { page: number; size: number }>({
  keyword: '',
  status: undefined,
  role: '',
  emailVerified: undefined,
  page: 0,
  size: 20
})

// 创建用户表单
const createForm = reactive<ICreateUserRequest & { storageQuotaGB: number }>({
  username: '',
  email: '',
  password: '',
  displayName: '',
  roles: ['USER'],
  storageQuota: 10 * 1024 * 1024 * 1024, // 10GB
  storageQuotaGB: 10,
  status: 'ACTIVE',
  sendWelcomeEmail: true
})

// 计算属性
const isAllSelected = computed(() => {
  return !!(userList.value?.users?.length && userList.value.users.length > 0 && selectedUsers.value.length === userList.value.users.length)
})

// 方法
const loadUsers = async () => {
  loading.value = true
  try {
    const params = { ...searchParams }
    // 此处不需要处理emailVerified，因为它已经是正确的类型
    
    userList.value = await adminApi.getUserList(params)
  } catch (error) {
    console.error('加载用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

const debouncedSearch = debounce(() => {
  searchParams.page = 0
  loadUsers()
}, 300)

const handleSearch = () => {
  searchParams.page = 0
  loadUsers()
}

const goToPage = (page: number) => {
  searchParams.page = page
  loadUsers()
}

const toggleSelectAll = () => {
  if (isAllSelected.value) {
    selectedUsers.value = []
  } else {
    selectedUsers.value = userList.value?.users.map(user => user.id) || []
  }
}

const getStatusVariant = (status: string) => {
  switch (status) {
    case 'ACTIVE': return 'default'
    case 'INACTIVE': return 'secondary'
    case 'LOCKED': return 'destructive'
    case 'PENDING': return 'outline'
    default: return 'secondary'
  }
}

const getStatusLabel = (status: string) => {
  switch (status) {
    case 'ACTIVE': return '活跃'
    case 'INACTIVE': return '非活跃'
    case 'LOCKED': return '锁定'
    case 'PENDING': return '待审核'
    default: return status
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

const openCreateDialog = () => {
  showCreateDialog.value = true
  // 重置表单
  Object.assign(createForm, {
    username: '',
    email: '',
    password: '',
    displayName: '',
    roles: ['USER'],
    storageQuota: 10 * 1024 * 1024 * 1024,
    storageQuotaGB: 10,
    status: 'ACTIVE',
    sendWelcomeEmail: true
  })
}

const handleCreateUser = async () => {
  createLoading.value = true
  try {
    // 转换存储配额
    createForm.storageQuota = createForm.storageQuotaGB * 1024 * 1024 * 1024
    
    await adminApi.createUser(createForm)
    showCreateDialog.value = false
    loadUsers()
  } catch (error) {
    console.error('创建用户失败:', error)
  } finally {
    createLoading.value = false
  }
}

const viewUser = (user: IUserDetail) => {
  // TODO: 实现查看用户详情
  console.log('View user:', user)
}

const editUser = (user: IUserDetail) => {
  // TODO: 实现编辑用户
  console.log('Edit user:', user)
}

const toggleUserStatus = async (user: IUserDetail, newStatus: string) => {
  try {
    await adminApi.updateUser(user.id, { status: newStatus as any })
    loadUsers()
  } catch (error) {
    console.error('更新用户状态失败:', error)
  }
}

const deleteUser = async (user: IUserDetail) => {
  if (confirm(`确定要删除用户 "${user.displayName}" 吗？此操作不可逆。`)) {
    try {
      await adminApi.deleteUser(user.id)
      loadUsers()
    } catch (error) {
      console.error('删除用户失败:', error)
    }
  }
}

const batchOperation = async (operation: string) => {
  if (selectedUsers.value.length === 0) return
  
  const operationNames = {
    ACTIVATE: '激活',
    DEACTIVATE: '禁用',
    LOCK: '锁定',
    UNLOCK: '解锁',
    DELETE: '删除'
  }
  
  const operationName = operationNames[operation as keyof typeof operationNames]
  
  if (confirm(`确定要${operationName} ${selectedUsers.value.length} 个用户吗？`)) {
    try {
      await adminApi.batchUserOperation({
        userIds: selectedUsers.value,
        operation: operation as any,
        reason: `批量${operationName}操作`
      })
      selectedUsers.value = []
      loadUsers()
    } catch (error) {
      console.error('批量操作失败:', error)
    }
  }
}

// 生命周期
onMounted(() => {
  loadUsers()
})
</script>

<style scoped>
/* 自定义滚动条 */
.overflow-x-auto::-webkit-scrollbar {
  height: 8px;
}

.overflow-x-auto::-webkit-scrollbar-track {
  background: hsl(var(--muted));
  border-radius: 4px;
}

.overflow-x-auto::-webkit-scrollbar-thumb {
  background: hsl(var(--muted-foreground) / 0.3);
  border-radius: 4px;
}

.overflow-x-auto::-webkit-scrollbar-thumb:hover {
  background: hsl(var(--muted-foreground) / 0.5);
}

/* 表格样式优化 */
table {
  border-collapse: separate;
  border-spacing: 0;
}

table th:first-child {
  border-top-left-radius: 8px;
}

table th:last-child {
  border-top-right-radius: 8px;
}
</style> 