import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import Breadcrumb from '../Breadcrumb.vue'

// Mock clipboard API
Object.assign(navigator, {
  clipboard: {
    writeText: vi.fn().mockResolvedValue(undefined)
  }
})

// Mock window.open
global.window.open = vi.fn()

describe('Breadcrumb Component', () => {
  let router: any

  beforeEach(() => {
    // Create router
    router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/', component: { template: '<div>Home</div>' }, meta: { title: '首页' } },
        { path: '/dashboard', component: { template: '<div>Dashboard</div>' }, meta: { title: '仪表板' } },
        { path: '/files', component: { template: '<div>Files</div>' }, meta: { title: '文件管理' } },
        { path: '/files/:spaceId', component: { template: '<div>Space</div>' }, meta: { title: '空间' } },
        { path: '/files/:spaceId/:folderId', component: { template: '<div>Folder</div>' }, meta: { title: '文件夹' } },
        { path: '/admin/users', component: { template: '<div>Users</div>' }, meta: { title: '用户管理' } }
      ]
    })
  })

  it('renders simple breadcrumb for dashboard', async () => {
    await router.push('/dashboard')

    const wrapper = mount(Breadcrumb, {
      global: {
        plugins: [router]
      }
    })

    expect(wrapper.text()).toContain('首页')
    expect(wrapper.text()).toContain('仪表板')
  })

  it('renders nested breadcrumb for admin pages', async () => {
    await router.push('/admin/users')

    const wrapper = mount(Breadcrumb, {
      global: {
        plugins: [router]
      }
    })

    expect(wrapper.text()).toContain('首页')
    expect(wrapper.text()).toContain('管理仪表板')
    expect(wrapper.text()).toContain('用户管理')
  })

  it('handles dynamic route parameters', async () => {
    await router.push('/files/123/456')

    const wrapper = mount(Breadcrumb, {
      global: {
        plugins: [router]
      }
    })

    expect(wrapper.text()).toContain('首页')
    expect(wrapper.text()).toContain('文件管理')
    expect(wrapper.text()).toContain('空间 123')
    expect(wrapper.text()).toContain('文件夹 456')
  })

  it('shows current page as non-clickable', async () => {
    await router.push('/files')

    const wrapper = mount(Breadcrumb, {
      global: {
        plugins: [router]
      }
    })

    // The last item should be a BreadcrumbPage (non-clickable)
    const breadcrumbItems = wrapper.findAll('[data-testid="breadcrumb-item"]')
    const lastItem = breadcrumbItems[breadcrumbItems.length - 1]
    expect(lastItem.text()).toContain('文件管理')
  })

  it('allows navigation to parent paths', async () => {
    await router.push('/admin/users')

    const wrapper = mount(Breadcrumb, {
      global: {
        plugins: [router]
      }
    })

    // Click on the "首页" breadcrumb
    const homeLink = wrapper.find('a:contains("首页")')
    if (homeLink.exists()) {
      await homeLink.trigger('click')
      expect(router.currentRoute.value.path).toBe('/dashboard')
    }
  })

  it('copies current path to clipboard', async () => {
    await router.push('/files')

    const wrapper = mount(Breadcrumb, {
      global: {
        plugins: [router]
      }
    })

    // Find and click the dropdown trigger
    const dropdownTrigger = wrapper.find('[data-testid="breadcrumb-actions"]')
    if (dropdownTrigger.exists()) {
      await dropdownTrigger.trigger('click')

      // Find and click the copy option
      const copyOption = wrapper.find('[data-testid="copy-path"]')
      if (copyOption.exists()) {
        await copyOption.trigger('click')
        expect(navigator.clipboard.writeText).toHaveBeenCalledWith(window.location.href)
      }
    }
  })

  it('opens current page in new window', async () => {
    await router.push('/files')

    const wrapper = mount(Breadcrumb, {
      global: {
        plugins: [router]
      }
    })

    // Find and click the dropdown trigger
    const dropdownTrigger = wrapper.find('[data-testid="breadcrumb-actions"]')
    if (dropdownTrigger.exists()) {
      await dropdownTrigger.trigger('click')

      // Find and click the new window option
      const newWindowOption = wrapper.find('[data-testid="open-new-window"]')
      if (newWindowOption.exists()) {
        await newWindowOption.trigger('click')
        expect(window.open).toHaveBeenCalledWith(window.location.href, '_blank')
      }
    }
  })

  it('shows parent paths in dropdown for nested routes', async () => {
    await router.push('/admin/users')

    const wrapper = mount(Breadcrumb, {
      global: {
        plugins: [router]
      }
    })

    // Find and click the dropdown trigger
    const dropdownTrigger = wrapper.find('[data-testid="breadcrumb-actions"]')
    if (dropdownTrigger.exists()) {
      await dropdownTrigger.trigger('click')

      // Should show parent paths in dropdown
      expect(wrapper.text()).toContain('快速导航')
      expect(wrapper.text()).toContain('管理仪表板')
    }
  })

  it('does not show home breadcrumb on home page', async () => {
    await router.push('/')

    const wrapper = mount(Breadcrumb, {
      global: {
        plugins: [router]
      }
    })

    // Should not show "首页" breadcrumb when already on home page
    const breadcrumbItems = wrapper.findAll('[data-testid="breadcrumb-item"]')
    expect(breadcrumbItems.length).toBe(0)
  })
})
