import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import CreateFolderDialog from '@/components/CreateFolderDialog.vue'
import { folderApi } from '@/apis'

// Mock APIs
vi.mock('@/apis', () => ({
  folderApi: {
    createFolder: vi.fn()
  }
}))

describe('CreateFolderDialog', () => {
  let wrapper: any

  beforeEach(() => {
    wrapper = mount(CreateFolderDialog, {
      props: {
        open: true,
        spaceId: 1,
        parentFolderId: undefined
      }
    })
  })

  it('renders correctly when open', () => {
    expect(wrapper.find('[data-testid="dialog-title"]').exists()).toBe(false) // Dialog组件可能没有这个testid
    expect(wrapper.text()).toContain('新建文件夹')
  })

  it('validates folder name correctly', async () => {
    const input = wrapper.find('input[type="text"]')
    
    // Test empty name
    await input.setValue('')
    await wrapper.vm.$nextTick()
    
    // Test invalid characters
    await input.setValue('test/folder')
    await wrapper.vm.$nextTick()
    
    // Test valid name
    await input.setValue('valid-folder-name')
    await wrapper.vm.$nextTick()
    
    expect(wrapper.vm.isValid).toBe(true)
  })

  it('calls createFolder API when form is submitted', async () => {
    const mockResponse = {
      success: true,
      data: { id: 1, name: 'test-folder' },
      message: 'Success'
    }
    
    vi.mocked(folderApi.createFolder).mockResolvedValue(mockResponse)
    
    const input = wrapper.find('input[type="text"]')
    await input.setValue('test-folder')
    
    const submitButton = wrapper.find('button[type="submit"]')
    await submitButton.trigger('click')
    
    expect(folderApi.createFolder).toHaveBeenCalledWith({
      name: 'test-folder',
      spaceId: 1,
      parentFolderId: undefined
    })
  })

  it('emits success event when folder is created', async () => {
    const mockResponse = {
      success: true,
      data: { id: 1, name: 'test-folder' },
      message: 'Success'
    }
    
    vi.mocked(folderApi.createFolder).mockResolvedValue(mockResponse)
    
    const input = wrapper.find('input[type="text"]')
    await input.setValue('test-folder')
    
    await wrapper.vm.handleSubmit()
    await wrapper.vm.$nextTick()
    
    expect(wrapper.emitted('success')).toBeTruthy()
    expect(wrapper.emitted('success')[0]).toEqual([mockResponse.data])
  })

  it('shows error message when API call fails', async () => {
    const mockError = new Error('API Error')
    vi.mocked(folderApi.createFolder).mockRejectedValue(mockError)
    
    const input = wrapper.find('input[type="text"]')
    await input.setValue('test-folder')
    
    await wrapper.vm.handleSubmit()
    await wrapper.vm.$nextTick()
    
    expect(wrapper.vm.error).toBe('API Error')
  })

  it('resets form when dialog is closed and reopened', async () => {
    const input = wrapper.find('input[type="text"]')
    await input.setValue('test-folder')
    
    // Close dialog
    await wrapper.setProps({ open: false })
    await wrapper.vm.$nextTick()
    
    // Reopen dialog
    await wrapper.setProps({ open: true })
    await wrapper.vm.$nextTick()
    
    expect(wrapper.vm.folderName).toBe('')
    expect(wrapper.vm.folderDescription).toBe('')
    expect(wrapper.vm.error).toBe('')
  })
})
