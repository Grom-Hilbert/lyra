import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import FileList from '../FileList.vue'
import type { IFileInfo, IFolderInfo } from '@/types/index'

// Mock data
const mockFiles: IFileInfo[] = [
  {
    id: 1,
    filename: 'test.txt',
    originalName: 'test.txt',
    path: '/test.txt',
    sizeBytes: 1024,
    mimeType: 'text/plain',
    fileHash: 'hash1',
    version: 1,
    isPublic: false,
    downloadCount: 0,
    spaceId: 1,
    uploaderId: 1,
    status: 'ACTIVE',
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z'
  },
  {
    id: 2,
    filename: 'image.jpg',
    originalName: 'image.jpg',
    path: '/image.jpg',
    sizeBytes: 2048,
    mimeType: 'image/jpeg',
    fileHash: 'hash2',
    version: 1,
    isPublic: false,
    downloadCount: 5,
    spaceId: 1,
    uploaderId: 1,
    status: 'ACTIVE',
    createdAt: '2024-01-02T00:00:00Z',
    updatedAt: '2024-01-02T00:00:00Z'
  }
]

const mockFolders: IFolderInfo[] = [
  {
    id: 1,
    name: 'Documents',
    path: '/Documents',
    spaceId: 1,
    level: 1,
    isRoot: false,
    fileCount: 5,
    sizeBytes: 10240,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z'
  },
  {
    id: 2,
    name: 'Images',
    path: '/Images',
    spaceId: 1,
    level: 1,
    isRoot: false,
    fileCount: 3,
    sizeBytes: 5120,
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z'
  }
]

describe('FileList', () => {
  let wrapper: any

  beforeEach(() => {
    wrapper = mount(FileList, {
      props: {
        files: mockFiles,
        folders: mockFolders,
        viewMode: 'grid',
        selectable: true,
        loading: false
      }
    })
  })

  describe('渲染测试', () => {
    it('应该正确渲染文件和文件夹', () => {
      expect(wrapper.find('.file-list').exists()).toBe(true)

      // 检查文件夹
      const folderItems = wrapper.findAll('.folder-item')
      expect(folderItems).toHaveLength(2)
      expect(folderItems[0].text()).toContain('Documents')
      expect(folderItems[1].text()).toContain('Images')

      // 检查文件（按字母顺序排序，image.jpg在前）
      const fileItems = wrapper.findAll('.file-item-file')
      expect(fileItems).toHaveLength(2)
      expect(fileItems[0].text()).toContain('image.jpg')
      expect(fileItems[1].text()).toContain('test.txt')
    })

    it('应该在网格视图中正确显示', () => {
      expect(wrapper.find('.grid').exists()).toBe(true)
      expect(wrapper.find('.list-view').exists()).toBe(false)
    })

    it('应该在列表视图中正确显示', async () => {
      await wrapper.setProps({ viewMode: 'list' })
      
      expect(wrapper.find('.grid').exists()).toBe(false)
      expect(wrapper.find('.list-view').exists()).toBe(true)
      expect(wrapper.find('table').exists()).toBe(true)
    })

    it('应该显示加载状态', async () => {
      await wrapper.setProps({ loading: true })
      
      expect(wrapper.find('.loading-state').exists()).toBe(true)
      expect(wrapper.text()).toContain('加载中...')
    })

    it('应该显示空状态', async () => {
      await wrapper.setProps({ files: [], folders: [], loading: false })
      
      expect(wrapper.find('.empty-state').exists()).toBe(true)
      expect(wrapper.text()).toContain('暂无文件')
    })
  })

  describe('选择功能测试', () => {
    it('应该支持单个文件选择', async () => {
      const fileCheckbox = wrapper.find('.file-item-file input[type="checkbox"]')
      await fileCheckbox.setChecked(true)

      expect(wrapper.emitted('selection-change')).toBeTruthy()
      const emittedEvent = wrapper.emitted('selection-change')[0][0]
      expect(emittedEvent).toHaveLength(1)
      expect(emittedEvent[0].type).toBe('file')
      expect(emittedEvent[0].id).toBe(2) // image.jpg的ID是2
    })

    it('应该支持全选功能', async () => {
      const selectAllCheckbox = wrapper.find('input[type="checkbox"]')
      await selectAllCheckbox.setChecked(true)
      
      expect(wrapper.emitted('selection-change')).toBeTruthy()
      const emittedEvent = wrapper.emitted('selection-change')[0][0]
      expect(emittedEvent).toHaveLength(4) // 2 folders + 2 files
    })
  })

  describe('排序功能测试', () => {
    it('应该支持按名称排序', async () => {
      const sortSelect = wrapper.find('select')
      await sortSelect.setValue('name')
      
      expect(wrapper.emitted('sort-change')).toBeTruthy()
      const emittedEvent = wrapper.emitted('sort-change')[0]
      expect(emittedEvent[0]).toBe('name')
      expect(emittedEvent[1]).toBe('asc')
    })

    it('应该支持切换排序方向', async () => {
      const sortDirectionButton = wrapper.find('button[title="升序"]')
      await sortDirectionButton.trigger('click')
      
      expect(wrapper.emitted('sort-change')).toBeTruthy()
    })
  })

  describe('视图模式切换测试', () => {
    it('应该支持切换到列表视图', async () => {
      const listViewButton = wrapper.find('button[title="列表视图"]')
      await listViewButton.trigger('click')
      
      expect(wrapper.emitted('view-mode-change')).toBeTruthy()
      const emittedEvent = wrapper.emitted('view-mode-change')[0]
      expect(emittedEvent[0]).toBe('list')
    })

    it('应该支持切换到网格视图', async () => {
      await wrapper.setProps({ viewMode: 'list' })
      
      const gridViewButton = wrapper.find('button[title="网格视图"]')
      await gridViewButton.trigger('click')
      
      expect(wrapper.emitted('view-mode-change')).toBeTruthy()
      const emittedEvent = wrapper.emitted('view-mode-change')[0]
      expect(emittedEvent[0]).toBe('grid')
    })
  })

  describe('文件操作测试', () => {
    it('应该支持文件双击打开', async () => {
      const fileItem = wrapper.find('.file-item-file')
      await fileItem.trigger('dblclick')

      expect(wrapper.emitted('file-open')).toBeTruthy()
      const emittedEvent = wrapper.emitted('file-open')[0]
      expect(emittedEvent[0].id).toBe(2) // image.jpg的ID是2
    })

    it('应该支持文件夹双击打开', async () => {
      const folderItem = wrapper.find('.folder-item')
      await folderItem.trigger('dblclick')
      
      expect(wrapper.emitted('folder-open')).toBeTruthy()
      const emittedEvent = wrapper.emitted('folder-open')[0]
      expect(emittedEvent[0].id).toBe(1)
    })
  })

  describe('右键菜单测试', () => {
    it('应该在文件右键时显示菜单', async () => {
      const fileItem = wrapper.find('.file-item-file')
      await fileItem.trigger('contextmenu')
      
      expect(wrapper.find('.context-menu').exists()).toBe(true)
      expect(wrapper.text()).toContain('预览')
      expect(wrapper.text()).toContain('下载')
      expect(wrapper.text()).toContain('重命名')
      expect(wrapper.text()).toContain('删除')
    })

    it('应该在文件夹右键时显示菜单', async () => {
      const folderItem = wrapper.find('.folder-item')
      await folderItem.trigger('contextmenu')
      
      expect(wrapper.find('.context-menu').exists()).toBe(true)
      expect(wrapper.text()).toContain('打开')
      expect(wrapper.text()).toContain('重命名')
      expect(wrapper.text()).toContain('删除')
    })
  })

  describe('工具函数测试', () => {
    it('应该正确格式化文件大小', () => {
      // 这里需要访问组件内部的方法，在实际实现中可能需要导出这些工具函数
      expect(wrapper.vm.formatFileSize).toBeDefined()
    })

    it('应该正确识别图片文件', () => {
      expect(wrapper.vm.isImageFile).toBeDefined()
    })
  })
})
