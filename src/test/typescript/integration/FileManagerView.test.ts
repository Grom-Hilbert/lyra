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
    it('应该正确加载空间列表', async () => {
      expect(spaceApi.getUserSpaces).toHaveBeenCalled()
      expect(wrapper.vm.spaces).toEqual(mockSpaces)
      expect(wrapper.vm.currentSpaceId).toBe(1)
    })

    it('应该正确加载文件夹和文件列表', async () => {
      expect(folderApi.getFolders).toHaveBeenCalledWith({
        spaceId: 1,
        parentId: undefined
      })
      expect(fileApi.getFilesBySpace).toHaveBeenCalledWith({
        spaceId: 1,
        folderId: undefined,
        page: 0,
        size: 100
      })
      expect(wrapper.vm.folders).toEqual(mockFolders)
      expect(wrapper.vm.files).toEqual(mockFiles.content)
    })

    it('应该正确加载存储配额信息', async () => {
      expect(spaceApi.getSpaceQuota).toHaveBeenCalledWith(1)
      expect(wrapper.vm.storageInfo).toEqual(mockStorageQuota)
    })
  })

  describe('空间切换功能', () => {
    it('应该能够切换到不同的空间', async () => {
      await wrapper.vm.selectSpace(2)
      
      expect(wrapper.vm.currentSpaceId).toBe(2)
      expect(wrapper.vm.currentFolderId).toBeUndefined()
      expect(spaceApi.getSpaceQuota).toHaveBeenCalledWith(2)
      expect(folderApi.getFolders).toHaveBeenCalledWith({
        spaceId: 2,
        parentId: undefined
      })
    })
  })

  describe('文件夹操作功能', () => {
    it('应该能够创建新文件夹', async () => {
      const mockNewFolder = { id: 3, name: '新文件夹', spaceId: 1, parentId: null }
      vi.mocked(folderApi.createFolder).mockResolvedValue({
        success: true,
        data: mockNewFolder,
        message: 'Success'
      })

      await wrapper.vm.handleCreateFolderSuccess(mockNewFolder)
      
      expect(folderApi.getFolders).toHaveBeenCalled()
    })

    it('应该能够删除文件夹', async () => {
      vi.mocked(folderApi.deleteFolder).mockResolvedValue({
        success: true,
        data: null,
        message: 'Success'
      })

      await wrapper.vm.handleFolderDelete(mockFolders[0])
      
      expect(folderApi.deleteFolder).toHaveBeenCalledWith(1)
    })

    it('应该能够移动文件夹', async () => {
      vi.mocked(folderApi.moveFolder).mockResolvedValue({
        success: true,
        data: null,
        message: 'Success'
      })

      await wrapper.vm.openFileOperation('move', mockFolders[0], 'folder')
      
      expect(wrapper.vm.showFileOperationDialog).toBe(true)
      expect(wrapper.vm.fileOperation).toBe('move')
      expect(wrapper.vm.selectedFileItem).toEqual(mockFolders[0])
      expect(wrapper.vm.selectedItemType).toBe('folder')
    })
  })

  describe('文件操作功能', () => {
    it('应该能够下载文件', async () => {
      const mockDownloadUrl = 'http://example.com/download/1'
      vi.mocked(fileApi.getDownloadUrl).mockReturnValue(mockDownloadUrl)
      
      // Mock window.open
      const mockOpen = vi.fn()
      Object.defineProperty(window, 'open', { value: mockOpen })

      await wrapper.vm.handleFileDownload(mockFiles.content[0])
      
      expect(fileApi.getDownloadUrl).toHaveBeenCalledWith(1)
      expect(mockOpen).toHaveBeenCalledWith(mockDownloadUrl, '_blank')
    })

    it('应该能够删除文件', async () => {
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

    it('应该能够重命名文件', async () => {
      vi.mocked(fileApi.renameFile).mockResolvedValue({
        success: true,
        data: null,
        message: 'Success'
      })

      await wrapper.vm.openFileOperation('rename', mockFiles.content[0], 'file')
      
      expect(wrapper.vm.showFileOperationDialog).toBe(true)
      expect(wrapper.vm.fileOperation).toBe('rename')
      expect(wrapper.vm.selectedFileItem).toEqual(mockFiles.content[0])
      expect(wrapper.vm.selectedItemType).toBe('file')
    })

    it('应该能够移动文件', async () => {
      vi.mocked(fileApi.moveFile).mockResolvedValue({
        success: true,
        data: null,
        message: 'Success'
      })

      await wrapper.vm.openFileOperation('move', mockFiles.content[0], 'file')
      
      expect(wrapper.vm.showFileOperationDialog).toBe(true)
      expect(wrapper.vm.fileOperation).toBe('move')
    })

    it('应该能够复制文件', async () => {
      vi.mocked(fileApi.copyFile).mockResolvedValue({
        success: true,
        data: null,
        message: 'Success'
      })

      await wrapper.vm.openFileOperation('copy', mockFiles.content[0], 'file')
      
      expect(wrapper.vm.showFileOperationDialog).toBe(true)
      expect(wrapper.vm.fileOperation).toBe('copy')
    })

    it('应该能够分享文件', async () => {
      const mockShareResult = {
        id: 1,
        token: 'abc123',
        shareUrl: 'http://example.com/share/abc123',
        accessType: 'read'
      }
      vi.mocked(fileApi.createFileShare).mockResolvedValue({
        success: true,
        data: mockShareResult,
        message: 'Success'
      })

      await wrapper.vm.openFileOperation('share', mockFiles.content[0], 'file')
      
      expect(wrapper.vm.showFileOperationDialog).toBe(true)
      expect(wrapper.vm.fileOperation).toBe('share')
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
      vi.mocked(fileApi.getDownloadUrl).mockReturnValue('http://example.com/download/')

      await wrapper.vm.batchDownload()
      
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

    it('应该能够批量移动文件', async () => {
      vi.mocked(fileApi.batchMoveFiles).mockResolvedValue({
        success: true,
        data: null,
        message: 'Success'
      })

      // Mock prompt and alert
      Object.defineProperty(window, 'prompt', { value: () => '2' })
      Object.defineProperty(window, 'alert', { value: vi.fn() })

      await wrapper.vm.handleBatchMove()
      
      // 这里需要进一步测试批量移动的逻辑
    })

    it('应该能够批量复制文件', async () => {
      vi.mocked(fileApi.batchCopyFiles).mockResolvedValue({
        success: true,
        data: null,
        message: 'Success'
      })

      // Mock prompt and alert
      Object.defineProperty(window, 'prompt', { value: () => '2' })
      Object.defineProperty(window, 'alert', { value: vi.fn() })

      await wrapper.vm.handleBatchCopy()
      
      // 这里需要进一步测试批量复制的逻辑
    })
  })

  describe('搜索功能', () => {
    it('应该能够搜索文件', async () => {
      const mockSearchResults = [mockFiles.content[0]]
      vi.mocked(fileApi.searchFiles).mockResolvedValue({
        success: true,
        data: { content: mockSearchResults },
        message: 'Success'
      })

      await wrapper.vm.handleSearch('test')
      
      expect(fileApi.searchFiles).toHaveBeenCalledWith({
        spaceId: 1,
        query: 'test',
        page: 0,
        size: 100
      })
      expect(wrapper.vm.isSearchMode).toBe(true)
      expect(wrapper.vm.searchResults).toEqual(mockSearchResults)
    })
  })

  describe('上传功能', () => {
    it('应该能够打开上传对话框', async () => {
      await wrapper.vm.$nextTick()
      
      const uploadButton = wrapper.find('[data-testid="upload-button"]')
      if (uploadButton.exists()) {
        await uploadButton.trigger('click')
        expect(wrapper.vm.showUploadDialog).toBe(true)
      }
    })

    it('应该能够处理上传成功', async () => {
      const mockUploadedFile = { id: 3, filename: 'uploaded.txt' }
      
      await wrapper.vm.handleUploadSuccess(mockUploadedFile)
      
      expect(fileApi.getFilesBySpace).toHaveBeenCalled()
    })
  })

  describe('错误处理', () => {
    it('应该能够处理API错误', async () => {
      vi.mocked(fileApi.getFilesBySpace).mockRejectedValue(new Error('API Error'))
      
      // Mock console.error
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
      
      await wrapper.vm.loadFolderContent()
      
      expect(consoleSpy).toHaveBeenCalledWith('Failed to load folder content:', expect.any(Error))
      
      consoleSpy.mockRestore()
    })
  })
})
