import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia } from 'pinia'
import LoginView from '../LoginView.vue'

describe('LoginView Checkbox Behavior', () => {
  let router: any
  let pinia: any

  beforeEach(() => {
    // Create router
    router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/login', component: LoginView },
        { path: '/dashboard', component: { template: '<div>Dashboard</div>' } }
      ]
    })

    // Create pinia
    pinia = createPinia()

    // Clear all mocks
    vi.clearAllMocks()
  })

  it('should handle remember me checkbox clicks correctly', async () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Find the remember me checkbox
    const checkbox = wrapper.find('input[type="checkbox"]#remember')
    expect(checkbox.exists()).toBe(true)

    // Initial state should be unchecked
    expect(checkbox.element.checked).toBe(false)

    // First click - should check the checkbox
    await checkbox.trigger('change')
    await wrapper.vm.$nextTick()
    
    // Verify checkbox is now checked
    expect(checkbox.element.checked).toBe(true)

    // Second click - should uncheck the checkbox
    await checkbox.trigger('change')
    await wrapper.vm.$nextTick()
    
    // Verify checkbox is now unchecked
    expect(checkbox.element.checked).toBe(false)

    // Third click - should check again
    await checkbox.trigger('change')
    await wrapper.vm.$nextTick()
    
    // Verify checkbox is checked again
    expect(checkbox.element.checked).toBe(true)
  })

  it('should disable checkbox when loading', async () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router, pinia]
      }
    })

    // Set loading state
    wrapper.vm.loading = true
    await wrapper.vm.$nextTick()

    const checkbox = wrapper.find('input[type="checkbox"]#remember')
    expect(checkbox.element.disabled).toBe(true)
    expect(checkbox.classes()).toContain('disabled:opacity-50')
  })

  it('should have proper styling classes', async () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router, pinia]
      }
    })

    const checkbox = wrapper.find('input[type="checkbox"]#remember')
    const label = wrapper.find('label[for="remember"]')

    // Check checkbox classes
    expect(checkbox.classes()).toContain('w-4')
    expect(checkbox.classes()).toContain('h-4')
    expect(checkbox.classes()).toContain('transition-all')
    expect(checkbox.classes()).toContain('duration-200')

    // Check label classes
    expect(label.classes()).toContain('text-sm')
    expect(label.classes()).toContain('cursor-pointer')
    expect(label.classes()).toContain('transition-colors')
  })

  it('should update label styling when loading', async () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router, pinia]
      }
    })

    const label = wrapper.find('label[for="remember"]')

    // Normal state
    expect(label.classes()).toContain('text-muted-foreground')
    expect(label.classes()).toContain('hover:text-foreground')

    // Loading state
    wrapper.vm.loading = true
    await wrapper.vm.$nextTick()

    expect(label.classes()).toContain('text-muted-foreground/50')
    expect(label.classes()).toContain('cursor-not-allowed')
  })

  it('should handle label click correctly', async () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router, pinia]
      }
    })

    const checkbox = wrapper.find('input[type="checkbox"]#remember')
    const label = wrapper.find('label[for="remember"]')

    // Initial state
    expect(checkbox.element.checked).toBe(false)

    // Click label should toggle checkbox
    await label.trigger('click')
    await wrapper.vm.$nextTick()

    expect(checkbox.element.checked).toBe(true)

    // Click label again should toggle back
    await label.trigger('click')
    await wrapper.vm.$nextTick()

    expect(checkbox.element.checked).toBe(false)
  })

  it('should maintain checkbox state during form interaction', async () => {
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router, pinia]
      }
    })

    const checkbox = wrapper.find('input[type="checkbox"]#remember')
    const usernameInput = wrapper.find('input[type="text"]')

    // Check the remember me checkbox
    await checkbox.trigger('change')
    await wrapper.vm.$nextTick()
    expect(checkbox.element.checked).toBe(true)

    // Interact with other form elements
    await usernameInput.setValue('testuser')
    await wrapper.vm.$nextTick()

    // Checkbox should still be checked
    expect(checkbox.element.checked).toBe(true)

    // Uncheck the checkbox
    await checkbox.trigger('change')
    await wrapper.vm.$nextTick()
    expect(checkbox.element.checked).toBe(false)

    // Interact with form again
    await usernameInput.setValue('anotheruser')
    await wrapper.vm.$nextTick()

    // Checkbox should still be unchecked
    expect(checkbox.element.checked).toBe(false)
  })
})
