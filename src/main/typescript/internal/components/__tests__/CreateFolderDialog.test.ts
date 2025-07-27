import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, shallowMount } from '@vue/test-utils'
import CreateFolderDialog from '@/components/CreateFolderDialog.vue'
import { folderApi } from '@/apis'

// Mock APIs
vi.mock('@/apis', () => ({
  folderApi: {
    createFolder: vi.fn()
  }
}))

// Mock Dialog components to avoid reka-ui issues
const MockDialog = {
  name: 'Dialog',
  template: '<div data-testid="dialog" v-if="open"><slot /></div>',
  props: ['open'],
  emits: ['update:open']
}

const MockDialogContent = {
  name: 'DialogContent',
  template: '<div data-testid="dialog-content"><slot /></div>'
}

const MockDialogDescription = {
  name: 'DialogDescription',
  template: '<div data-testid="dialog-description"><slot /></div>'
}

const MockDialogFooter = {
  name: 'DialogFooter',
  template: '<div data-testid="dialog-footer"><slot /></div>'
}

const MockDialogHeader = {
  name: 'DialogHeader',
  template: '<div data-testid="dialog-header"><slot /></div>'
}

const MockDialogTitle = {
  name: 'DialogTitle',
  template: '<div data-testid="dialog-title"><slot /></div>'
}

describe('CreateFolderDialog', () => {
  let wrapper: any

  beforeEach(() => {
    wrapper = shallowMount(CreateFolderDialog, {
      props: {
        open: true,
        spaceId: 1,
        parentFolderId: undefined
      },
      global: {
        components: {
          Dialog: MockDialog,
          DialogContent: MockDialogContent,
          DialogDescription: MockDialogDescription,
          DialogFooter: MockDialogFooter,
          DialogHeader: MockDialogHeader,
          DialogTitle: MockDialogTitle
        }
      }
    })
  })

  it('renders correctly when open', () => {
    // Since we're using shallowMount, check for component existence instead
    expect(wrapper.vm.folderName).toBeDefined()
    expect(wrapper.vm.folderDescription).toBeDefined()
    expect(wrapper.vm.isValid).toBeDefined()
  })

  it('validates folder name correctly', async () => {
    // Test valid name
    wrapper.vm.folderName = 'valid-folder-name'
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.isValid).toBe(true)

    // Test invalid name with special characters
    wrapper.vm.folderName = 'invalid/name'
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.isValid).toBe(false)
  })

  it('calls createFolder API when form is submitted', async () => {
    const mockResponse = {
      success: true,
      data: { id: 1, name: 'test-folder' },
      message: 'Success'
    }

    vi.mocked(folderApi.createFolder).mockResolvedValue(mockResponse)

    wrapper.vm.folderName = 'test-folder'
    await wrapper.vm.handleSubmit()

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

    wrapper.vm.folderName = 'test-folder'
    await wrapper.vm.handleSubmit()
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('success')).toBeTruthy()
    expect(wrapper.emitted('success')[0]).toEqual([mockResponse.data])
  })

  it('shows error message when API call fails', async () => {
    const mockError = new Error('API Error')
    vi.mocked(folderApi.createFolder).mockRejectedValue(mockError)

    wrapper.vm.folderName = 'test-folder'
    await wrapper.vm.handleSubmit()
    await wrapper.vm.$nextTick()

    expect(wrapper.vm.error).toBe('API Error')
  })

  it('validates reserved folder names', () => {
    wrapper.vm.folderName = 'CON'
    expect(wrapper.vm.validateFolderName()).toBe(false)
    expect(wrapper.vm.error).toContain('保留名称')
  })

  it('validates folder name length', () => {
    wrapper.vm.folderName = 'a'.repeat(256)
    expect(wrapper.vm.validateFolderName()).toBe(false)
    expect(wrapper.vm.error).toContain('不能超过255个字符')
  })
})
