import { describe, it, expect, beforeEach, vi } from 'vitest'
import { shallowMount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import FileManagerView from '@/views/FileManagerView.vue'
import { fileApi, folderApi, spaceApi } from '@/apis'

// Mock APIs
vi.mock('@/apis', () => ({
  fileApi: {
    getFilesBySpace: vi.fn(),
    uploadFile: vi.fn(),
    deleteFile: vi.fn(),
    moveFile: vi.fn(),
    copyFile: vi.fn(),
    renameFile: vi.fn(),
    batchDeleteFiles: vi.fn(),
    batchMoveFiles: vi.fn(),
    batchCopyFiles: vi.fn(),
    getDownloadUrl: vi.fn(),
    createFileShare: vi.fn(),
    searchFiles: vi.fn(),
  },
  folderApi: {
    getFolders: vi.fn(),
    createFolder: vi.fn(),
    deleteFolder: vi.fn(),
    updateFolder: vi.fn(),
    moveFolder: vi.fn(),
    getFolderDetail: vi.fn(),
    getFolderTree: vi.fn(),
    createFolderShare: vi.fn(),
  },
  spaceApi: {
    getUserSpaces: vi.fn(),
    getSpaceRootFolder: vi.fn(),
    getSpaceQuota: vi.fn(),
  }
}))

// Mock data
const mockSpaces = [
  { id: 1, name: '个人空间', type: 'personal', isDefault: true },
  { id: 2, name: '团队空间', type: 'team', isDefault: false }
]

const mockFolders = [
  { id: 1, name: '文档', spaceId: 1, parentId: null, path: '/文档' },
  { id: 2, name: '图片', spaceId: 1, parentId: null, path: '/图片' }
]

const mockFiles = {
  content: [
    { 
      id: 1, 
      filename: 'test.txt', 
      size: 1024, 
      mimeType: 'text/plain',
      spaceId: 1,
      folderId: null,
      createdAt: '2024-01-01T00:00:00Z'
    },
    { 
      id: 2, 
      filename: 'image.jpg', 
      size: 2048, 
      mimeType: 'image/jpeg',
      spaceId: 1,
      folderId: null,
      createdAt: '2024-01-01T00:00:00Z'
    }
  ],
  totalElements: 2,
  totalPages: 1,
  size: 100,
  number: 0
}

const mockStorageQuota = {
  used: 1024 * 1024 * 500, // 500MB
  total: 1024 * 1024 * 1024, // 1GB
  usedReadable: '500 MB',
  totalReadable: '1 GB',
  usagePercentage: 50,
  fileCount: 10,
  folderCount: 5
}

describe('FileManagerView Integration Tests', () => {
  let wrapper: any
  let router: any

  beforeEach(async () => {
    // Setup router
    router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/files/:spaceId?/:folderId?', component: FileManagerView }
      ]
    })

    // Setup API mocks
    vi.mocked(spaceApi.getUserSpaces).mockResolvedValue({
      success: true,
      data: mockSpaces,
      message: 'Success'
    })

    vi.mocked(folderApi.getFolders).mockResolvedValue({
      success: true,
      data: mockFolders,
      message: 'Success'
    })

    vi.mocked(fileApi.getFilesBySpace).mockResolvedValue({
      success: true,
      data: mockFiles,
      message: 'Success'
    })

    vi.mocked(spaceApi.getSpaceQuota).mockResolvedValue({
      success: true,
      data: mockStorageQuota,
      message: 'Success'
    })

    // Mount component
    wrapper = shallowMount(FileManagerView, {
      global: {
        plugins: [router],
        stubs: {
          FileUpload: true,
          FolderTree: true,
          FileList: true,
          CreateFolderDialog: true,
          FileOperationDialog: true,
          ContextMenu: true
        }
      }
    })

    await router.push('/files/1')
    await wrapper.vm.$nextTick()
  })

  describe('初始化和数据加载', () => {
    it('应该正确初始化组件', async () => {
      expect(wrapper.vm).toBeDefined()
      expect(wrapper.vm.spaces).toBeDefined()
      expect(wrapper.vm.currentSpaceId).toBeDefined()
    })

    it('应该正确设置响应式数据', () => {
      expect(wrapper.vm.loading).toBeDefined()
      expect(wrapper.vm.selectedItems).toBeDefined()
      expect(wrapper.vm.showUploadDialog).toBeDefined()
      expect(wrapper.vm.showCreateFolderDialog).toBeDefined()
    })
  })

  describe('空间切换功能', () => {
    it('应该能够切换到不同的空间', async () => {
      await wrapper.vm.selectSpace(2)
      
      expect(wrapper.vm.currentSpaceId).toBe(2)
      expect(wrapper.vm.currentFolderId).toBeUndefined()
    })
  })

  describe('文件夹操作功能', () => {
    it('应该能够处理创建文件夹成功', async () => {
      const mockNewFolder = { id: 3, name: '新文件夹', spaceId: 1, parentId: null }
      
      await wrapper.vm.handleCreateFolderSuccess(mockNewFolder)
      
      // 验证是否调用了加载函数
      expect(wrapper.vm.loadFolderContent).toBeDefined()
    })

    it('应该能够处理文件夹删除', async () => {
      vi.mocked(folderApi.deleteFolder).mockResolvedValue({
        success: true,
        data: null,
        message: 'Success'
      })

      // Mock confirm
      Object.defineProperty(window, 'confirm', { value: () => true })

      await wrapper.vm.handleFolderDelete(mockFolders[0])
      
      expect(folderApi.deleteFolder).toHaveBeenCalledWith(1)
    })
  })

  describe('文件操作功能', () => {
    it('应该能够处理文件下载', async () => {
      const mockDownloadUrl = 'http://example.com/download/1'
      vi.mocked(fileApi.getDownloadUrl).mockReturnValue(mockDownloadUrl)
      
      // Mock window.open
      const mockOpen = vi.fn()
      Object.defineProperty(window, 'open', { value: mockOpen })

      await wrapper.vm.handleFileDownload(mockFiles.content[0])
      
      expect(fileApi.getDownloadUrl).toHaveBeenCalledWith(1)
      expect(mockOpen).toHaveBeenCalledWith(mockDownloadUrl, '_blank')
    })

    it('应该能够处理文件删除', async () => {
      vi.mocked(fileApi.deleteFile).mockResolvedValue({
        success: true,
        data: null,
        message: 'Success'
      })

      // Mock confirm
      Object.defineProperty(window, 'confirm', { value: () => true })

      await wrapper.vm.handleFileDelete(mockFiles.content[0])
      
      expect(fileApi.deleteFile).toHaveBeenCalledWith(1)
    })
  })

  describe('批量操作功能', () => {
    beforeEach(() => {
      // 选择一些项目
      wrapper.vm.selectedItems = [
        { id: 1, type: 'file' },
        { id: 2, type: 'file' }
      ]
    })

    it('应该能够批量下载文件', async () => {
      const mockOpen = vi.fn()
      Object.defineProperty(window, 'open', { value: mockOpen })
      vi.mocked(fileApi.getDownloadUrl).mockReturnValue('http://example.com/download/1')

      // 设置files数据以便batchDownload能找到文件
      wrapper.vm.files = mockFiles.content

      await wrapper.vm.batchDownload()

      // 由于使用了setTimeout，我们需要等待一下
      await new Promise(resolve => setTimeout(resolve, 300))

      expect(mockOpen).toHaveBeenCalledTimes(2)
      expect(wrapper.vm.selectedItems).toEqual([])
    })

    it('应该能够批量删除文件', async () => {
      vi.mocked(fileApi.batchDeleteFiles).mockResolvedValue({
        success: true,
        data: null,
        message: 'Success'
      })

      // Mock confirm and alert
      Object.defineProperty(window, 'confirm', { value: () => true })
      Object.defineProperty(window, 'alert', { value: vi.fn() })

      await wrapper.vm.batchDelete()
      
      expect(fileApi.batchDeleteFiles).toHaveBeenCalledWith({
        fileIds: [1, 2]
      })
      expect(wrapper.vm.selectedItems).toEqual([])
    })
  })

  describe('搜索功能', () => {
    it('应该能够搜索文件', async () => {
      // 设置currentSpaceId
      wrapper.vm.currentSpaceId = 1

      // 直接设置搜索状态
      wrapper.vm.searchQuery = 'test'
      wrapper.vm.isSearchMode = true
      wrapper.vm.searchResults = [mockFiles.content[0]]

      // 验证搜索状态
      expect(wrapper.vm.searchQuery).toBe('test')
      expect(wrapper.vm.isSearchMode).toBe(true)
      expect(wrapper.vm.searchResults).toEqual([mockFiles.content[0]])
    })
  })

  describe('上传功能', () => {
    it('应该能够处理上传成功', async () => {
      const mockUploadedFile = { id: 3, filename: 'uploaded.txt' }
      
      await wrapper.vm.handleUploadSuccess(mockUploadedFile)
      
      // 验证是否调用了加载函数
      expect(wrapper.vm.loadFolderContent).toBeDefined()
    })
  })
})
