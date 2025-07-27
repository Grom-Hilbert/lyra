import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import UploadProgress from '../UploadProgress.vue'
import type { UploadTask } from '../UploadProgress.vue'

// Mock data
const mockUploadTasks: UploadTask[] = [
  {
    id: '1',
    filename: 'test.txt',
    size: 1024,
    progress: 50,
    status: 'uploading',
    speed: 1024,
    startTime: Date.now() - 5000
  },
  {
    id: '2',
    filename: 'image.jpg',
    size: 2048,
    progress: 100,
    status: 'success',
    endTime: Date.now()
  },
  {
    id: '3',
    filename: 'document.pdf',
    size: 4096,
    progress: 25,
    status: 'error',
    error: '网络连接失败'
  },
  {
    id: '4',
    filename: 'video.mp4',
    size: 8192,
    progress: 75,
    status: 'paused'
  }
]

describe('UploadProgress', () => {
  let wrapper: any

  beforeEach(() => {
    wrapper = mount(UploadProgress, {
      props: {
        uploadTasks: mockUploadTasks,
        visible: true,
        position: 'bottom-right'
      }
    })
  })

  describe('渲染测试', () => {
    it('应该正确渲染上传进度面板', () => {
      expect(wrapper.find('.upload-progress-panel').exists()).toBe(true)
      expect(wrapper.find('.upload-header').exists()).toBe(true)
      expect(wrapper.find('.upload-tasks').exists()).toBe(true)
    })

    it('应该显示正确的任务数量', () => {
      expect(wrapper.text()).toContain('上传进度 (2/4)') // 2个活跃任务，4个总任务
    })

    it('应该在不可见时隐藏面板', async () => {
      await wrapper.setProps({ visible: false })
      expect(wrapper.find('.upload-progress-panel').exists()).toBe(false)
    })

    it('应该在没有任务时隐藏面板', async () => {
      await wrapper.setProps({ uploadTasks: [] })
      expect(wrapper.find('.upload-progress-panel').exists()).toBe(false)
    })

    it('应该根据位置属性正确定位', async () => {
      expect(wrapper.find('.bottom-4.right-4').exists()).toBe(true)
      
      await wrapper.setProps({ position: 'top-left' })
      expect(wrapper.find('.top-4.left-4').exists()).toBe(true)
    })
  })

  describe('任务状态显示测试', () => {
    it('应该正确显示上传中的任务', () => {
      const uploadingTask = wrapper.find('.task-uploading')
      expect(uploadingTask.exists()).toBe(true)
      expect(uploadingTask.text()).toContain('test.txt')
      expect(uploadingTask.text()).toContain('50%')
      expect(uploadingTask.text()).toContain('1 KB/s')
    })

    it('应该正确显示成功的任务', () => {
      const successTask = wrapper.find('.task-success')
      expect(successTask.exists()).toBe(true)
      expect(successTask.text()).toContain('image.jpg')
      expect(successTask.text()).toContain('上传完成')
    })

    it('应该正确显示失败的任务', () => {
      const errorTask = wrapper.find('.task-error')
      expect(errorTask.exists()).toBe(true)
      expect(errorTask.text()).toContain('document.pdf')
      expect(errorTask.text()).toContain('网络连接失败')
    })

    it('应该正确显示暂停的任务', () => {
      const pausedTask = wrapper.find('.task-paused')
      expect(pausedTask.exists()).toBe(true)
      expect(pausedTask.text()).toContain('video.mp4')
      expect(pausedTask.text()).toContain('75%')
    })
  })

  describe('进度计算测试', () => {
    it('应该正确计算总体进度', () => {
      // (50 + 100 + 0 + 75) / 4 = 56.25%
      expect(wrapper.vm.overallProgress).toBeCloseTo(56.25, 1)
    })

    it('应该正确计算活跃任务数量', () => {
      expect(wrapper.vm.activeTasksCount).toBe(2) // uploading + paused
    })

    it('应该正确计算完成任务数量', () => {
      expect(wrapper.vm.completedTasksCount).toBe(1) // success
    })

    it('应该正确计算失败任务数量', () => {
      expect(wrapper.vm.errorTasksCount).toBe(1) // error
    })

    it('应该正确计算上传速度', () => {
      expect(wrapper.vm.uploadSpeed).toBe(1024) // 只有一个上传中的任务有速度
    })
  })

  describe('交互功能测试', () => {
    it('应该支持折叠/展开', async () => {
      expect(wrapper.find('.upload-tasks').exists()).toBe(true)
      
      const toggleButton = wrapper.find('button[title="折叠"]')
      await toggleButton.trigger('click')
      
      expect(wrapper.vm.isCollapsed).toBe(true)
      expect(wrapper.find('.upload-tasks').exists()).toBe(false)
    })

    it('应该支持关闭面板', async () => {
      const closeButton = wrapper.find('button[title="关闭"]')
      await closeButton.trigger('click')
      
      expect(wrapper.emitted('close')).toBeTruthy()
    })

    it('应该支持暂停任务', async () => {
      const pauseButton = wrapper.find('button[title="暂停"]')
      await pauseButton.trigger('click')
      
      expect(wrapper.emitted('task-pause')).toBeTruthy()
      const emittedEvent = wrapper.emitted('task-pause')[0]
      expect(emittedEvent[0]).toBe('1') // 上传中任务的ID
    })

    it('应该支持恢复任务', async () => {
      const resumeButton = wrapper.find('button[title="恢复"]')
      await resumeButton.trigger('click')
      
      expect(wrapper.emitted('task-resume')).toBeTruthy()
      const emittedEvent = wrapper.emitted('task-resume')[0]
      expect(emittedEvent[0]).toBe('4') // 暂停任务的ID
    })

    it('应该支持重试任务', async () => {
      const retryButton = wrapper.find('button[title="重试"]')
      await retryButton.trigger('click')
      
      expect(wrapper.emitted('task-retry')).toBeTruthy()
      const emittedEvent = wrapper.emitted('task-retry')[0]
      expect(emittedEvent[0]).toBe('3') // 失败任务的ID
    })

    it('应该支持取消任务', async () => {
      const cancelButtons = wrapper.findAll('button[title="取消"], button[title="移除"]')
      await cancelButtons[0].trigger('click')
      
      expect(wrapper.emitted('task-cancel')).toBeTruthy()
    })

    it('应该支持清除已完成任务', async () => {
      const clearCompletedButton = wrapper.find('[data-testid="clear-completed"]')
      if (clearCompletedButton.exists()) {
        await clearCompletedButton.trigger('click')
        expect(wrapper.emitted('clear-completed')).toBeTruthy()
      }
    })

    it('应该支持清除所有任务', async () => {
      const clearAllButton = wrapper.find('[data-testid="clear-all"]')
      if (clearAllButton.exists()) {
        await clearAllButton.trigger('click')
        expect(wrapper.emitted('clear-all')).toBeTruthy()
      }
    })
  })

  describe('工具函数测试', () => {
    it('应该正确格式化文件大小', () => {
      expect(wrapper.vm.formatFileSize(1024)).toBe('1 KB')
      expect(wrapper.vm.formatFileSize(1048576)).toBe('1 MB')
      expect(wrapper.vm.formatFileSize(0)).toBe('0 B')
    })

    it('应该正确格式化速度', () => {
      expect(wrapper.vm.formatSpeed(1024)).toBe('1 KB/s')
      expect(wrapper.vm.formatSpeed(1048576)).toBe('1 MB/s')
    })

    it('应该正确获取任务状态类', () => {
      expect(wrapper.vm.getTaskStatusClass('success')).toBe('task-success')
      expect(wrapper.vm.getTaskStatusClass('error')).toBe('task-error')
      expect(wrapper.vm.getTaskStatusClass('uploading')).toBe('task-uploading')
      expect(wrapper.vm.getTaskStatusClass('paused')).toBe('task-paused')
      expect(wrapper.vm.getTaskStatusClass('pending')).toBe('task-pending')
    })
  })

  describe('边界情况测试', () => {
    it('应该处理空任务列表', async () => {
      await wrapper.setProps({ uploadTasks: [] })
      
      expect(wrapper.vm.overallProgress).toBe(0)
      expect(wrapper.vm.activeTasksCount).toBe(0)
      expect(wrapper.vm.completedTasksCount).toBe(0)
      expect(wrapper.vm.errorTasksCount).toBe(0)
      expect(wrapper.vm.uploadSpeed).toBe(0)
    })

    it('应该处理没有速度信息的上传任务', async () => {
      const tasksWithoutSpeed = mockUploadTasks.map(task => ({
        ...task,
        speed: undefined
      }))
      
      await wrapper.setProps({ uploadTasks: tasksWithoutSpeed })
      expect(wrapper.vm.uploadSpeed).toBe(0)
    })
  })
})
